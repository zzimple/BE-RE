package com.zzimple.internal;

import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * [임시 - strangler] legacy가 추출된 staff-service의 내부 API를 호출하는 클라이언트.
 * estimate-service 추출(Phase 5) 시 estimate-service의 Feign 클라이언트로 대체된다.
 */
@Component
@RequiredArgsConstructor
public class StaffServiceClient {

  private final RestTemplate restTemplate;

  @Value("${clients.staff-service.url:http://localhost:8083}")
  private String staffServiceUrl;

  /**
   * userId로 직원 정보 조회. 없으면 Optional.empty().
   * 응답: {staffId, userId, ownerId, storeId, status}
   */
  @SuppressWarnings("unchecked")
  public Optional<Map<String, Object>> findStaffByUserId(Long userId) {
    try {
      Map<String, Object> body = restTemplate.getForObject(
          staffServiceUrl + "/internal/staff/by-user/" + userId, Map.class);
      return Optional.ofNullable(body);
    } catch (HttpClientErrorException.NotFound e) {
      return Optional.empty();
    }
  }

  /** 해당 견적에 직원이 배정되어 있는지 확인 */
  @SuppressWarnings("unchecked")
  public boolean isAssigned(Long estimateNo, Long staffId) {
    Map<String, Object> body = restTemplate.getForObject(
        staffServiceUrl + "/internal/staff/assignments/check?estimateNo=" + estimateNo
            + "&staffId=" + staffId, Map.class);
    return body != null && Boolean.TRUE.equals(body.get("assigned"));
  }
}
