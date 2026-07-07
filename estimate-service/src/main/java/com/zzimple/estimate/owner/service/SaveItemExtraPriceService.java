package com.zzimple.estimate.owner.service;

import com.zzimple.estimate.client.OwnerServiceClient;
import java.util.Map;
import com.zzimple.estimate.guest.entity.MoveItems;
import com.zzimple.estimate.guest.repository.MoveItemsRepository;
import com.zzimple.estimate.owner.dto.request.SaveEstimatePriceRequest;
import com.zzimple.estimate.owner.dto.response.ItemTotalResponse;
import com.zzimple.estimate.owner.dto.response.ItemTotalResultResponse;
import com.zzimple.estimate.owner.entity.EstimateCalculation;
import com.zzimple.estimate.owner.entity.MoveItemExtraCharge;
import com.zzimple.estimate.owner.entity.MoveItemPriceByStore;
import com.zzimple.estimate.owner.repository.EstimateCalculationRepository;
import com.zzimple.estimate.owner.repository.MoveItemExtraChargeRepository;
import com.zzimple.estimate.owner.repository.MoveItemPriceByStoreRepository;
import com.zzimple.common.exception.CustomException;
import com.zzimple.common.exception.GlobalErrorCode;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaveItemExtraPriceService {

  private final MoveItemsRepository moveItemsRepository;
  private final MoveItemExtraChargeRepository moveItemExtraChargeRepository;
  private final EstimateCalculationRepository estimateCalculationRepository;
  private final MoveItemPriceByStoreRepository moveItemPriceByStoreRepository;
  private final OwnerServiceClient ownerServiceClient;

  @Transactional
  public void saveEstimateItems(Long estimateNo, Long userId, List<SaveEstimatePriceRequest> itemRequests) {

    Map<String, Object> store = ownerServiceClient.getStoreByOwnerUserId(userId)
        .orElseThrow(() -> new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    Long storeId = OwnerServiceClient.asLong(store, "storeId");

    // moveitem의 기본금을 update하기.
    for (SaveEstimatePriceRequest req : itemRequests) {
      Long itemTypeId = req.getItemTypeId();
      Integer basePrice = req.getBasePrice();

      log.debug("💰 기본금 처리 시작: estimateNo={}, storeId={}, itemTypeId={}, basePrice={}",
          estimateNo, storeId, itemTypeId, basePrice);

      // 기본금 저장 (업데이트 or 신규 등록)
      MoveItemPriceByStore base = moveItemPriceByStoreRepository
          .findByEstimateNoAndStoreIdAndItemTypeId(estimateNo, storeId, itemTypeId)
          .orElseGet(() -> MoveItemPriceByStore.builder()
              .estimateNo(estimateNo)
              .storeId(storeId)
              .itemTypeId(itemTypeId)
              .build());

      base.setBasePrice(basePrice);
      moveItemPriceByStoreRepository.save(base);
      log.debug("✅ 기본금 저장 완료: itemTypeId={}, basePrice={}", itemTypeId, basePrice);
    }

    // 2) item-level 추가금 저장: 추가 항목마다 직접 itemId 조회 후 처리
    for (SaveEstimatePriceRequest request : itemRequests) {
      log.debug("옵션 처리 시작: estimateNo={}, storeId={}, itemTypeId={}",
          estimateNo, storeId, request.getItemTypeId());
      // 2-1) estimateNo + storeId + itemTypeId 로 DB PK(itemId)만 조회 (findByEstimateNoAndItemTypeId 커스텀 메서드 필요)
      Long itemId = request.getItemTypeId();

      moveItemExtraChargeRepository.deleteByEstimateNoAndStoreIdAndItemTypeId(estimateNo, storeId, itemId);

      // 2-3) 새 옵션 리스트 insert
      if (request.getExtraCharges() != null) {
        List<MoveItemExtraCharge> toSave = request.getExtraCharges().stream()
            .map(ec -> MoveItemExtraCharge.builder()
                .estimateNo(estimateNo)
                .storeId(storeId)
                .itemTypeId(itemId)
                .amount(ec.getAmount())
                .reason(ec.getReason())
                .build())
            .collect(Collectors.toList());
        moveItemExtraChargeRepository.saveAll(toSave);
        log.debug("새 옵션 저장 완료: itemId={}, count={}", itemId, toSave.size());
      }
    }

    int itemsTotal = moveItemPriceByStoreRepository
        .findByEstimateNoAndStoreId(estimateNo, storeId)
        .stream()
        .mapToInt(MoveItemPriceByStore::getBasePrice)
        .sum();

    // 4) 추가금 합계 계산
    int extrasTotal = moveItemExtraChargeRepository
        .findByEstimateNoAndStoreId(estimateNo, storeId)
        .stream()
        .mapToInt(MoveItemExtraCharge::getAmount)
        .sum();

    // 5) EstimateCalculation 조회/생성 및 업데이트
    EstimateCalculation calc = estimateCalculationRepository
        .findByEstimateNoAndStoreId(estimateNo, storeId)
        .orElseGet(() -> EstimateCalculation.builder()
            .estimateNo(estimateNo)
            .storeId(storeId)
            .build());

    calc.setItemsTotalPrice(itemsTotal+extrasTotal);
    estimateCalculationRepository.save(calc);

    log.info("견적 저장 완료: estimateNo={}", estimateNo);
  }

  @Transactional
  public ItemTotalResultResponse calculateAndSaveItemTotalPrices(Long estimateNo, Long userId) {

    Map<String, Object> store = ownerServiceClient.getStoreByOwnerUserId(userId)
        .orElseThrow(() -> new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    Long storeId = OwnerServiceClient.asLong(store, "storeId");
    log.info("조회 중인 ownerUserId={}, 조회된 storeId={}", userId, OwnerServiceClient.asLong(store, "storeId"));

    List<MoveItems> items = moveItemsRepository.findByEstimateNo(estimateNo);
    List<ItemTotalResponse> responses = new ArrayList<>();

    int totalPrice = 0;

    for (MoveItems item : items) {
      Long itemTypeId = item.getItemTypeId();

      // 1. 기본금 조회 (가게 기준)
// 수정: estimateNo 조건을 추가한 단건 조회로 교체    ← 수정
      int basePrice = moveItemPriceByStoreRepository
          .findByEstimateNoAndStoreIdAndItemTypeId(estimateNo, storeId, itemTypeId)  // ← 수정
          .map(MoveItemPriceByStore::getBasePrice)                                   // ← 수정
          .orElse(0);


      // 2. 추가금 계산 (가게 기준)
      int extraPrice = moveItemExtraChargeRepository
          .findByEstimateNoAndStoreIdAndItemTypeId(estimateNo, storeId, itemTypeId)
          .stream()
          .mapToInt(MoveItemExtraCharge::getAmount)
          .sum();

      // 3. 총합 계산 및 저장
      int itemTotalPrice = basePrice + extraPrice;

      item.setPer_item_totalPrice(itemTotalPrice); // 저장 가능할 경우

      totalPrice += itemTotalPrice;

      responses.add(new ItemTotalResponse(
          item.getId(),
          itemTypeId,
          item.getItemTypeName(),
          basePrice,
          extraPrice,
          itemTotalPrice
      ));

      log.info("🧮 계산 완료: itemId={}, base={}, extra={}, total={}", item.getId(), basePrice, extraPrice, itemTotalPrice);
    }

    moveItemsRepository.saveAll(items);

    // ← 수정: orElseThrow로만 불러오고, 새로 생성 안 함
    EstimateCalculation calculation = estimateCalculationRepository
        .findByEstimateNoAndStoreId(estimateNo, storeId)
        .orElseThrow(() -> new EntityNotFoundException(
            "계산 레코드가 없습니다. estimateNo=" + estimateNo + ", storeId=" + storeId));

    calculation.setItemsTotalPrice(totalPrice);

    estimateCalculationRepository.save(calculation);

    return new ItemTotalResultResponse(totalPrice, storeId, responses);
  }
}
