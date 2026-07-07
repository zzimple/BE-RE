## 🏫 학교·기업 연계 프로그램 🏫  

- 🧾 **프로젝트명**: 찜플 (zzimple) - 이사 플랫폼 서비스  
- ⏳ **진행 기간**: 2025.02.17 ~ 2025.07.01  
- 🏢 **참여 기업**: 헥토 (멘토링 제공)

<br>

## 📝 개요  

Spring Boot 백엔드와 React 프론트엔드를 연동한 **이사 플랫폼 서비스**로, 사용자 유형별 맞춤 기능을 제공

### 🧑‍🤝‍🧑 사용자별 주요 기능

- 👤 **고객**  
  • 이사 견적서를 단계별로 작성  
  • Gpt를 통한 업체 견적 비교 및 요약  
  • Google Vision 기반 물품 이미지 분석


- 👨‍💼 **사장님**  
  • 고객 견적 요청에 회신  
  • 매출 및 직원 스케줄 관리  

- 👷 **직원**  
  • 근무 일정 확인  
  • 휴무 신청 기능 제공  


### 🔗 외부 서비스 연동

- 📆 공공데이터 API (공휴일, 음력, 도로명 주소)  
- 🗺 Kakao API (지도, 경/위도 변환, 거리 계산)  
- 🧠 ChatGPT API (견적 비교/요약)  
- 📷 Google Vision API (짐 이미지 인식)

💡 이사 산업의 아날로그한 흐름을 디지털 방식으로 전환하는 데 초점

<br>

## 🧩 MSA 아키텍처 (모놀리스 → 마이크로서비스 전환)

단일 Spring Boot 모놀리스를 **strangler fig 패턴**으로 5개 도메인 서비스 + 인프라 3종으로 점진 분리했습니다.

```
                        ┌──────────────┐
   Client ───────────▶  │   Gateway    │  JWT 검증 → X-User-* 헤더 주입 (위조 헤더 제거)
                        │    :8080     │
                        └──────┬───────┘        ┌ Eureka(:8761) 서비스 디스커버리
        ┌──────────┬───────────┼──────────┬─────┤ Config Server(:8888) 중앙 설정
        ▼          ▼           ▼          ▼     ▼
   ┌─────────┐┌─────────┐┌─────────┐┌─────────┐┌─────────────┐
   │  auth   ││  owner  ││  staff  ││estimate ││ integration │
   │  :8084  ││  :8082  ││  :8083  ││  :8086  ││    :8085    │
   │ JWT발급 ││사장/매장││직원/배정││  견적   ││ 외부API 프록시│
   └────┬────┘└────┬────┘└────┬────┘└────┬────┘└─────────────┘
     MySQL      MySQL      MySQL      MySQL      (stateless)
    auth_db    owner_db   staff_db  estimate_db   Kakao/GPT/주소
                Redis(0)             Redis(1)
```

**서비스 간 통신 (하이브리드)**
- **동기 Feign + Eureka**: "지금 이 순간의 값"이 필요한 호출 — 로그인 시 storeId/ownerId 클레임 해석, 직원 배정 시 견적 확정 상태 확인 등
- **비동기 Kafka 이벤트 + CQRS read model**: 자주 읽히고 드물게 바뀌는 표시용 데이터

| 토픽 | 발행 | 소비 | 용도 |
|---|---|---|---|
| `estimate.confirmed.v1` | estimate | owner, staff | 매출 read model 적재, 배정 준비 |
| `estimate.status-changed.v1` | estimate | owner | 매출 완료/진행 구분 |
| `owner.store.upserted.v1` | owner | estimate | store_view (목록 매장명 N+1 제거) |
| `user.updated.v1` | auth | estimate | user_view (목록 사용자명 N+1 제거) |

**실행**: `docker compose up` 하나로 전체 스택 기동 (Kafka는 KRaft 모드, 서비스별 독립 MySQL, 외부 노출은 Gateway 뿐)

<br>  
                                                                                                                                                        
## 🛠 기술 스택

| **Category**  | **Tech** |
|---------------|----------|
| **Language**  | <img src="https://img.shields.io/badge/Java-007396?style=flat-square&logo=java&logoColor=white"> |
| **Framework** | <img src="https://img.shields.io/badge/Spring-6DB33F?style=flat-square&logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20Cloud-6DB33F?style=flat-square&logo=spring&logoColor=white"> |
| **MSA**       | Spring Cloud Gateway · Eureka · Config Server · OpenFeign · <img src="https://img.shields.io/badge/Apache%20Kafka-231F20?style=flat-square&logo=apachekafka&logoColor=white"> <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white"> |
| **Security**  | <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white"> <img src="https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white"> |
| **Database**  | <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white"> <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white"> |
| **CI/CD**     | <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white"> |
| **Deploy**    | <img src="https://img.shields.io/badge/KT%20Cloud-000000?style=flat-square&logoColor=white"> |
| **Version Control** | <img src="https://img.shields.io/badge/Git-F05032?style=flat-square&logo=git&logoColor=white"> <img src="https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white"> |
| **AI API**    | <img src="https://img.shields.io/badge/ChatGPT-00A67E?style=flat-square&logo=openai&logoColor=white"> <img src="https://img.shields.io/badge/Google%20Vision%20API-4285F4?style=flat-square&logo=googlecloud&logoColor=white"> |
| **Docs & Team** | <img src="https://img.shields.io/badge/Notion-000000?style=flat-square&logo=notion&logoColor=white"> |


<br>

## 🏆 수상 내역

- 🥉 **학교·기업 연계 프로젝트 동상 수상**  
- 🥉 **헥토 기업 자체 평가 브론즈 수상**

<br>
  
## 🚀 서비스 구현 결과
![기능1](https://github.com/user-attachments/assets/1b6ef3b8-58c2-4075-a057-136e8432cb96)

![기능 2](https://github.com/user-attachments/assets/aa12c930-9a84-484a-89a7-f07f2d043749)

![기능 3](https://github.com/user-attachments/assets/36131db8-1a09-4942-9ffa-f1c40867e991)

![기능 4](https://github.com/user-attachments/assets/cfbab371-a8c5-41b5-bd85-96820e6e54fa)

![기능 5](https://github.com/user-attachments/assets/1d3f325f-445a-4641-bf50-7f5e6c7b981e)

![기능 9](https://github.com/user-attachments/assets/47014e23-c2cf-4f9f-bdd0-67fa38c5938a)

![기능 6](https://github.com/user-attachments/assets/1c57e3c9-721d-48ae-b83a-3f1b1e092e0a)

![기능 7](https://github.com/user-attachments/assets/56fbe52d-186c-4e4e-8a63-7b91cdd5fe1c)

![rlsmd 8](https://github.com/user-attachments/assets/0e0776c6-7b63-4459-b8e6-043f7f2057c1)

![기능 10](https://github.com/user-attachments/assets/c04f040f-260e-40b5-a7a1-a227588f7887)

<br>

