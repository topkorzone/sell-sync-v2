# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드 문서입니다.

## 프로젝트 개요

**Marketplace Hub (marketplace-hub)** — 한국 마켓플레이스(쿠팡, 네이버 스마트스토어, 11번가, G마켓 등)의 주문을 통합 수집하고, 배송/출고를 관리하며, ERP 시스템과 연동하는 멀티테넌트 이커머스 주문 관리 플랫폼.

## 빌드 및 실행 명령어

### 백엔드 (Java 21 / Spring Boot 3.3.5 / Gradle)

```bash
# 전체 모듈 빌드
cd backend && ./gradlew build

# 테스트 제외 빌드
cd backend && ./gradlew build -x test

# API 서버 실행 (Spring 프로필 기본값: 'local')
cd backend && ./gradlew :mh-api:bootRun

# 전체 테스트 실행
cd backend && ./gradlew test

# 단일 모듈 테스트
cd backend && ./gradlew :mh-core:test

# 단일 테스트 클래스 실행
cd backend && ./gradlew :mh-core:test --tests "com.mhub.core.service.OrderServiceTest"

# 로컬 인프라 실행 (PostgreSQL, Redis, LocalStack)
docker compose -f backend/docker/docker-compose.yml up -d
```

### 프론트엔드 (React 18 / Vite / TypeScript)

```bash
cd frontend && npm install
cd frontend && npm run dev       # 개발 서버: http://localhost:5173
cd frontend && npm run build     # 타입 체크 + 프로덕션 빌드
cd frontend && npm run lint      # ESLint 실행
```

## 아키텍처

### 모노레포 구조

```
backend/                        # Gradle 멀티모듈 Java 프로젝트
├── mh-api/                     # Spring Boot 앱 진입점, REST 컨트롤러
├── mh-common/                  # 공통 DTO, 에러 코드, 예외 클래스
├── mh-core/                    # 도메인 엔티티, 리포지토리, 보안, 테넌트 로직
├── mh-marketplace/             # 마켓플레이스 어댑터 구현체
├── mh-scheduler/               # 크론 작업 (ShedLock) + SQS 워커
├── mh-shipping/                # 택배사 연동
└── mh-erp/                     # ERP 연동
frontend/                       # React SPA (Vite + Tailwind + Radix UI)
```

### 모듈 의존성 체인

`mh-api` → `mh-core`, `mh-marketplace`, `mh-shipping`, `mh-erp`, `mh-scheduler`
`mh-marketplace` / `mh-shipping` / `mh-erp` / `mh-scheduler` → `mh-core` → `mh-common`

### 멀티테넌시

모든 요청은 테넌트 범위로 격리된다. `TenantFilter`가 JWT의 `app_metadata` 클레임에서 `tenant_id`를 추출하여 `TenantContext`(ThreadLocal)에 저장한다. PostgreSQL RLS 정책이 커넥션마다 설정되는 `app.current_tenant_id`를 기반으로 행 수준 격리를 수행한다. `TenantAwareInterceptor`가 JPA 쿼리 실행 전에 이 PostgreSQL 변수를 설정한다.

### 인증

Supabase 기반 OAuth2 Resource Server. JWT는 Supabase JWKS 엔드포인트를 통해 검증된다. `SupabaseAuthClient`가 Supabase Auth REST API를 호출하여 로그인/토큰 갱신/로그아웃을 처리한다. `SupabaseJwtAuthenticationConverter`가 JWT 클레임을 Spring Security 권한으로 변환한다.

### 마켓플레이스 어댑터 패턴

`MarketplaceAdapter` 인터페이스 → `AbstractMarketplaceAdapter` → 구체 어댑터(`CoupangAdapter`, `NaverSmartStoreAdapter`). 각 어댑터는 `collectOrders()`, `getChangedOrders()`, `confirmShipment()`, `refreshToken()`, `testConnection()`과 마켓플레이스별 주문 상태 매핑을 구현한다.

### 비동기 처리

`OrderSyncScheduler`가 ShedLock 기반 분산 잠금으로 매시간 크론 실행된다. AWS SQS(로컬 개발 시 LocalStack)가 주문 동기화 메시지를 큐잉하고, `OrderSyncWorker`가 큐에서 메시지를 소비하여 처리한다.

### 데이터베이스

Supabase 위의 PostgreSQL에 Flyway 마이그레이션 적용(`mh-core/src/main/resources/db/migration/`). 주문(orders) 테이블은 월별 파티셔닝되어 있다. Hibernate 배치 처리 설정(batch_size: 100). 마켓플레이스 인증 정보는 `EncryptedStringConverter`를 통해 AES 암호화 저장된다.

### 프론트엔드

`react-router-dom` 기반 React SPA. 경로 별칭 `@/`는 `src/`에 매핑된다. `src/lib/api.ts`의 Axios 클라이언트가 Bearer 토큰 자동 첨부 및 401 응답 시 토큰 갱신/로그인 리다이렉트를 처리한다. `VITE_MOCK_AUTH=true` 환경변수로 오프라인 개발용 목(mock) 인증 모드를 활성화할 수 있다.

## 주요 컨벤션

- **API 응답 형식**: 모든 엔드포인트는 `ApiResponse<T>` — `{ success, data, errorCode, message }` 구조로 반환
- **에러 코드**: `mh-common/.../ErrorCodes.java`에 정의, 접두사: AUTH_, TENANT_, ORDER_, MKT_, SHIP_, ERP_
- **로깅**: MDC에 `tenantId` 포함, 로그 패턴: `[%X{tenantId}]`
- **Spring 프로필**: 기본값 `local` (`application-local.yml`), 로컬 프로필에서 SQS 자동 설정 제외
- **Swagger UI**: 백엔드 실행 시 `/swagger-ui.html`에서 접근 가능
- **포트**: 백엔드 8080, 프론트엔드 개발 서버 5173
