package com.zzimple.owner.service;

import com.zzimple.common.exception.CustomException;
import com.zzimple.common.exception.GlobalErrorCode;
import com.zzimple.owner.client.UserClient;
import com.zzimple.owner.client.dto.CreateUserRequest;
import com.zzimple.owner.client.dto.UserSummaryResponse;
import com.zzimple.owner.dto.request.OwnerLoginIdCheckRequest;
import com.zzimple.owner.dto.request.OwnerSignUpRequest;
import com.zzimple.owner.dto.response.MonthlySalesAvgResponse;
import com.zzimple.owner.dto.response.MonthlySalesItemResponse;
import com.zzimple.owner.dto.response.OwnerLoginIdCheckResponse;
import com.zzimple.owner.dto.response.OwnerProfileResponse;
import com.zzimple.owner.dto.response.OwnerSignUpResponse;
import com.zzimple.owner.dto.response.WeeklySalesSimpleResponse;
import com.zzimple.owner.entity.Owner;
import com.zzimple.owner.entity.OwnerSalesRecord;
import com.zzimple.owner.event.OwnerEventPublisher;
import com.zzimple.owner.exception.BusinessErrorCode;
import com.zzimple.owner.repository.OwnerRepository;
import com.zzimple.owner.repository.OwnerSalesRecordRepository;
import com.zzimple.owner.repository.redis.BusinessRedisRepository;
import com.zzimple.owner.store.entity.Store;
import com.zzimple.owner.store.repository.StoreRepository;
import feign.FeignException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerService {

  private final OwnerRepository ownerRepository;
  private final PasswordEncoder passwordEncoder;
  private final BusinessRedisRepository businessRedisRepository;
  private final StoreRepository storeRepository;
  private final OwnerSalesRecordRepository ownerSalesRecordRepository;
  private final UserClient userClient;
  private final OwnerEventPublisher ownerEventPublisher;

  public OwnerLoginIdCheckResponse checkLoginIdDuplicate(OwnerLoginIdCheckRequest request) {
    boolean isDuplicate = ownerRepository.findByBusinessNumber(request.getLoginId()).isPresent();
    return OwnerLoginIdCheckResponse.builder()
        .isDuplicate(isDuplicate)
        .build();
  }

  @Transactional
  public OwnerSignUpResponse registerOwner(OwnerSignUpRequest request) {

    // 1. 중복 아이디
    if (ownerRepository.findByBusinessNumber(request.getB_no()).isPresent()) {
      log.warn("[회원가입 실패] 이미 존재하는 아이디 - ID: {}", request.getB_no());
      throw new CustomException(BusinessErrorCode.BUSINESS_NUMBER_ALREADY_EXISTS);
    }

    // 2. 인증된 사업자 번호 Redis에서 삭제
    businessRedisRepository.deleteById(request.getB_no());
    log.info("[회원가입] 사업자번호 인증정보 삭제 완료 - b_no: {}", request.getB_no());

    // 3. 비밀번호 암호화
    String encodedPassword = passwordEncoder.encode(request.getPassword());

    // 4. 기본 User 생성 (auth 도메인 - 동기 Feign)
    //    User 생성이 실패하면 회원가입 전체가 실패해야 하므로 이벤트가 아닌 동기 호출.
    //    (User 생성 후 Owner/Store 저장이 실패하면 고아 User가 남는다 -
    //     보상 트랜잭션(saga)이 정석이지만 데모 범위에서는 로그로 추적한다)
    UserSummaryResponse baseUser;
    try {
      baseUser = userClient.createUser(new CreateUserRequest(
          request.getB_no(),
          encodedPassword,
          request.getUserName(),
          request.getPhoneNumber(),
          request.getEmail(),
          "OWNER"
      ));
    } catch (FeignException e) {
      log.error("[회원가입] User 생성 실패 - ID: {}, status: {}", request.getB_no(), e.status());
      throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }

    try {
      // 5. Owner 저장
      Owner owner = Owner.builder()
          .userId(baseUser.getId())
          .businessNumber(request.getB_no())
          .insured(request.getInsured())
          .status("계속사업자")
          .roadFullAddr(request.getRoadFullAddr())
          .roadAddrPart1(request.getRoadAddrPart1())
          .addrDetail(request.getAddrDetail())
          .zipNo(request.getZipNo())
          .build();
      ownerRepository.save(owner);

      // 6. Store 저장
      Store store = Store.builder()
          .ownerId(owner.getId())
          .name(request.getStoreName())
          .address(request.getRoadFullAddr())
          .build();
      storeRepository.save(store);

      log.info("[회원가입] 회원가입 및 가게 등록 성공 - 사업자번호: {}, 가게: {}",
          request.getB_no(), store.getName());

      // 7. owner.store.upserted 이벤트 발행 (estimate-service 등의 read model 갱신용)
      ownerEventPublisher.publishOwnerStoreUpserted(owner, store);

      return OwnerSignUpResponse.builder()
          .isSuccess(true)
          .build();
    } catch (Exception e) {
      log.error("[회원가입] 회원가입 처리 중 예외 발생 - ID: {} (User {} 생성됨 - 보상 필요)",
          request.getB_no(), baseUser.getId(), e);
      throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  // 사장님 프로필 조회
  public OwnerProfileResponse getOwnerProfile(Long userId) {

    Owner owner = ownerRepository.findByUserId(userId)
        .orElseThrow(() -> new RuntimeException("Owner not found"));

    // 표시용 사용자 정보 (auth 도메인 - Feign)
    UserSummaryResponse user;
    try {
      user = userClient.getUser(userId);
    } catch (FeignException.NotFound e) {
      throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }

    Store store = storeRepository.findByOwnerUserId(userId)
        .orElseThrow(() -> new RuntimeException("Store not found"));

    return new OwnerProfileResponse(user.getUserName(), user.getLoginId(), user.getEmail(),
        owner.getRoadFullAddr(), owner.getRoadAddrPart1(), owner.getAddrDetail(),
        owner.getZipNo(), store.getId());
  }

  /**
   * 월별 평균 매출.
   * 모놀리스 시절 estimate 도메인의 3개 리포지토리 + 전체 유저 스캔으로 계산하던 것을,
   * Kafka 이벤트로 적재되는 owner_sales_record 로컬 read model 조회로 대체했다.
   */
  public List<MonthlySalesAvgResponse> getMonthlyAverageSales(Long storeId) {
    List<OwnerSalesRecord> records = ownerSalesRecordRepository.findByStoreId(storeId);

    DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    Map<String, List<MonthlySalesItemResponse>> grouped = new HashMap<>();

    for (OwnerSalesRecord record : records) {
      if (record.getMoveDate() == null) {
        continue;
      }
      String yearMonth = record.getMoveDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));

      MonthlySalesItemResponse item = new MonthlySalesItemResponse(
          record.getEstimateNo(),
          record.getMoveDate().format(displayFormatter),
          record.getGuestName() != null ? record.getGuestName() : "알 수 없음",
          record.getFinalTotalPrice() != null ? record.getFinalTotalPrice() : 0,
          record.getStatus()
      );
      grouped.computeIfAbsent(yearMonth, k -> new ArrayList<>()).add(item);
    }

    return grouped.entrySet().stream()
        .map(entry -> {
          List<MonthlySalesItemResponse> items = entry.getValue();
          double avg = items.stream()
              .mapToInt(MonthlySalesItemResponse::getAmount).average().orElse(0.0);
          return new MonthlySalesAvgResponse(entry.getKey(), avg, items);
        })
        .sorted(Comparator.comparing(MonthlySalesAvgResponse::getMonth))
        .collect(Collectors.toList());
  }

  // 주간 매출 조회 (평균 매출 기준, 월요일 시작)
  public List<WeeklySalesSimpleResponse> getWeeklySales(Long storeId) {
    List<OwnerSalesRecord> records = ownerSalesRecordRepository.findByStoreId(storeId);

    Map<LocalDate, List<Integer>> weeklyGroup = new HashMap<>();
    for (OwnerSalesRecord record : records) {
      if (record.getMoveDate() == null || record.getFinalTotalPrice() == null) {
        continue;
      }
      LocalDate weekStart = record.getMoveDate().with(DayOfWeek.MONDAY);
      weeklyGroup.computeIfAbsent(weekStart, k -> new ArrayList<>())
          .add(record.getFinalTotalPrice());
    }

    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return weeklyGroup.entrySet().stream()
        .map(entry -> {
          int avg = (int) entry.getValue().stream()
              .mapToInt(Integer::intValue).average().orElse(0.0);
          return new WeeklySalesSimpleResponse(entry.getKey().format(outputFormatter), avg);
        })
        .sorted(Comparator.comparing(WeeklySalesSimpleResponse::getWeekStartDate))
        .collect(Collectors.toList());
  }
}
