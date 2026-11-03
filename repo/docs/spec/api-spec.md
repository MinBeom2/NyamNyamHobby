# 냠냠취미 REST API 상세 명세서 (v0.4)

> **기준 문서**: 프로젝트 명세서 v0.4 ([project-spec-v0.3.md](./project-spec-v0.3.md), 파일 이름 유지) · 기능 요구사항 CSV(10/21) 반영 ([feature-requirements.csv](./feature-requirements.csv)) · DB 스키마 (PostgreSQL 17 + pgvector, 이미지 `pgvector/pgvector:pg17`; 확장·임베딩 스키마는 후속 마이그레이션, [V1__init.sql](../db/V1__init.sql))
> **최종 수정일**: 2026-10-30
> **작성자**: 김재현  
> **단일 진실 공급원(SSOT)**: 런타임 상세 스펙은 Spring Boot 서버의 Swagger UI (`/swagger-ui/index.html`)를 기준으로 하며, 본 문서는 프론트엔드-백엔드 간 계약 및 기능 구현을 위한 표준 상세 명세서입니다.

> **10/30 화면 기준 변경**: 01-6A·01-6B 온보딩과 02-1 숏폼 보관·해제는 현재 Figma v2에 맞춰 구현했다. 이 두 기능은 아래의 과거 우선순위 설명보다 [프론트 연동용 변경 문서](./api-changes-2026-10-30-onboarding-short-save.md)를 우선한다. 보관함 목록 등 다른 P2 기능은 범위에 추가하지 않는다.

> **우선순위 표기** (기능 요구사항 CSV 기준)
>
> | 등급 | 뜻 | 일정 | 작업 목록 표기 |
> |---|---|---|---|
> | P0 | 필수(MVP) | 11/04 개발 마감 | `필수` |
> | P1 | 권장(MVP) | 11/09~11/12 개선 기간 | `권장` |
> | P3 | 후순위 | P0·P1을 마친 뒤 시간이 남으면(개선 기간 안에 못 하면 발표 이후) | `후순위` |
> | P2 | MVP 제외 | 이번 개발 범위에서 뺌, 고도화 후보로만 남김 | `제외` |
>
> - 개발 순서는 **P0 → P1 → P3** 이고 P2는 개발하지 않는다(숫자 순서와 다름에 주의).
> - 이 문서에서는 API 제목(또는 표의 설명)에 `[P1]` `[P3]` `[P2·MVP 제외]`를 붙인다. **표시가 없으면 P0**이다. 한 API 안에서 일부 필드·동작만 등급이 다르면 그 줄에 따로 표시한다.

---

## 1. 공통 규약 및 아키텍처 규칙

### 1.1 Base URL & 헤더 규격
- **Base URL**: `https://k15d104.p.ssafy.io/api/v1` (EC2 Nginx가 `/api/`를 백엔드 컨테이너로 전달, 로컬은 `http://localhost:8080/api/v1`) (소셜 로그인의 경우 `/oauth2/**`, `/login/oauth2/**` — `[P1]`, 제공자는 **Google만**)
- **인증 헤더**: `Authorization: Bearer {accessToken}`
- **요청 추적 헤더**: `X-Request-Id` (UUID, 모든 응답에 포함)
- **요청량 제한 헤더 (Rate Limit)** (CSV에 없는 비기능 — 고도화(P3 수준, 여유 시)):
  - `X-RateLimit-Remaining`: 남은 요청 토큰 수
  - 초과 시 HTTP 429 반환 및 `Retry-After: {초}` 응답 헤더 제공
- **CORS 정책**: 허용 도메인(`localhost:5173`, 배포 Vercel 도메인), `credentials: true`

### 1.2 쿠키 정책
| 쿠키 이름 | 유효 기간 | 보안 플래그 | Path | 용도 |
|---|---|---|---|---|
| `refresh_token` | 14일 | HttpOnly; Secure; SameSite=Lax | `/api/v1/auth` | Access Token 재발급 전용 (DB 해시 저장 및 사용 시 교체) |
| `guest_key` `[P2·MVP 제외]` | 7일 | HttpOnly; Secure; SameSite=Lax | `/` | 비회원 숏폼 탐색 및 추천 체험 식별자 (UUID). 비회원 체험(FR-AUTH-05)이 P2라 MVP에서는 발급하지 않으며, 숏폼 탐색·추천은 로그인 후에만 사용한다 |

### 1.3 데이터 포맷 규칙
- **문자 인코딩**: `UTF-8` / `Content-Type: application/json`
- **필드 명명 규칙**: `camelCase`
- **Enum 값 표기**: `UPPER_SNAKE_CASE`
- **일시 (DateTime)**: ISO-8601 포맷 (`YYYY-MM-DDTHH:mm:ss+09:00`, 초 단위 기본, 밀리초 포함 가능)
- **날짜 (Date)**: `YYYY-MM-DD`
- **금액 / 시간**: 정수형 (금액: 원 단위 정수, 시간: 분 단위 정수)

---

### 1.4 공통 응답 봉투 (Response Envelope)

#### 성공 응답 (`success: true`)
```json
{
  "success": true,
  "data": {
    /* 실제 응답 DTO 데이터 (단일 객체, 목록, null 등) */
  },
  "error": null
}
```

#### 페이징 목록 응답 (목록 조회 시 `data` 구조)
- 0-indexed 기반 페이징 (`page=0&size=20`, size 최대 50)
```json
{
  "success": true,
  "data": {
    "content": [
      /* 항목 목록 배열 */
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 134,
      "totalPages": 7,
      "hasNext": true
    }
  },
  "error": null
}
```

#### 실패 응답 (`success: false`)
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "COMMON_INVALID_INPUT",
    "message": "입력값을 확인해 주세요.",
    "fields": [
      {
        "field": "nickname",
        "reason": "2~12자의 한글, 영문, 숫자만 사용 가능합니다."
      }
    ],
    "retryAfterSeconds": null
  }
}
```

---

## 2. 공통 에러 코드 총괄표

| HTTP Status | 에러 코드 | 설명 및 발생 상황 |
|---|---|---|
| **400** | `COMMON_INVALID_INPUT` | 요청 파라미터 또는 DTO 필드 Bean Validation 검증 실패 |
| **400** | `AUTH_LINK_INVALID` | 이메일 인증 또는 비밀번호 재설정 토큰 만료/기사용/유효하지 않음 |
| **400** | `BOARD_REPLY_DEPTH` | `[P1]` 대댓글에 추가 대댓글 작성 시도 (최대 1단계 제한) |
| **400** | `REPORT_SELF` | `[P3]` 본인이 작성한 콘텐츠(게시글/댓글)를 신고하려고 할 때 |
| **400** | `UPLOAD_INVALID_FILE` | 허용되지 않은 파일 확장자/MIME 타입 업로드 시도 |
| **400** | `ADMIN_CHANNEL_NOT_FOUND` | `[P2·MVP 제외]` 등록 시도한 유튜브 채널 URL/ID를 외부 API에서 찾을 수 없음 |
| **401** | `COMMON_UNAUTHORIZED` | 인증 토큰이 없거나 유효하지 않음 |
| **401** | `AUTH_TOKEN_EXPIRED` | Access Token 유효기간(30분) 만료 (`/auth/refresh` 필요) |
| **401** | `AUTH_REFRESH_INVALID` | Refresh Token 만료, 변조, 불일치 또는 이미 사용된 경우 |
| **401** | `AUTH_INVALID_CREDENTIALS` | 로그인 시 이메일 또는 비밀번호 불일치 |
| **403** | `COMMON_FORBIDDEN` | 요청 리소스에 대한 접근 권한 부족 |
| **403** | `AUTH_ACCOUNT_LOCKED` | 5회 연속 로그인 실패로 계정 임시 잠금 상태 |
| **403** | `AUTH_EMAIL_NOT_VERIFIED` | 이메일 인증이 완료되지 않은 계정 (`PENDING`) |
| **403** | `BOARD_FORBIDDEN` | `[P1]` 타인의 게시글/댓글에 대한 수정 또는 삭제 시도 |
| **404** | `COMMON_NOT_FOUND` | 요청 대상을 찾을 수 없음 (존재하지 않거나 삭제/숨김 처리됨) |
| **409** | `AUTH_EMAIL_DUPLICATED` | 회원가입 시 이미 등록된 이메일 주소 |
| **409** | `AUTH_NICKNAME_DUPLICATED` | 이미 사용 중인 닉네임 |
| **409** | `AUTH_SOCIAL_ACCOUNT` | `[P1]` 이메일 가입 계정인데 소셜(Google) 로그인으로 접근했거나 그 반대인 경우 (예: "Google로 가입된 이메일이에요", "Google 로그인을 이용해 주세요") |
| **409** | `SHORT_SESSION_ENDED` | 포기한 탐색 세션 또는 이미 추천이 생성된 세션의 시청·반응 변경. 자연 종료 후 최초 추천 생성 및 종료 재요청은 허용하며, 시간 만료는 현재 도입하지 않음 |
| **409** | `SHORT_NOT_ACTIVE` | 비공개, 삭제, 자동 숨김 처리된 숏폼에 반응 시도 |
| **409** | `MY_HOBBY_LIMIT` | 시작/재개 시 IN_PROGRESS 최대 3개 초과. PAUSED 제외 |
| **409** | `MY_PLAN_NOT_READY` | 해당 취미/숙련도에 공개된 TODO 원본 템플릿 부재 |
| **409** | `MY_HOBBY_IN_PROGRESS` | 진행 중(IN_PROGRESS)·일시중지(PAUSED) 취미의 중복 시작 또는 관심 저장/해제 |
| **409** | `MY_HOBBY_INVALID_TRANSITION` | 허용되지 않은 취미 상태 전환·종료 후 재시작·종료 이력 관심 변경 |
| **409** | `TODO_FUTURE_WEEK` | 미래 주차 활동의 상태·메모 변경 시도 |
| **409** | `TODO_PLAN_NOT_ACTIVE` | PAUSED·STOPPED·COMPLETED 취미 또는 ARCHIVED 계획 수정 시도 |
| **409** | `REPORT_DUPLICATED` | `[P3]` 동일한 사용자가 같은 대상을 중복 신고한 경우 |
| **409** | `INQUIRY_ALREADY_ANSWERED` | `[P3]` 관리자 답변이 완료된 문의에 대한 수정/삭제 시도 |
| **409** | `ADMIN_RELEASE_IN_PROGRESS` | `[P2·MVP 제외]` 숏폼 주간 반영 배치가 이미 실행 중인 경우 (수동 반영 API가 P2) |
| **413** | `UPLOAD_TOO_LARGE` | 파일 업로드 허용 용량 초과 (프로필 2MB, 일반 5MB) |
| **429** | `COMMON_RATE_LIMITED` | 단위 시간당 API 요청 허용량 초과 (요청량 제한은 고도화, P3 수준·여유 시) |

---

## 3. 도메인별 상세 API 명세

---

### 3.1 회원 및 인증 (`/api/v1/auth`, `/api/v1/users`)

#### 1) 이메일 회원가입
- **Endpoint**: `POST /api/v1/auth/signup`
- **Permission**: `공개 (GUEST/전체)`
- **Request Body**:
```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "nickname": "클라이머호호"
}
```
- **Validation**:
  - `email`: 필수, 최대 100자, 유효한 이메일 형식
  - `password`: 필수, 8~20자, 영문·숫자·특수문자 중 2종 이상 조합
  - `nickname`: 필수, 2~12자, 한글·영문·숫자만 허용, 공백 불가
- **Response (201 Created)**:
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "클라이머호호",
    "status": "PENDING",
    "message": "인증 메일이 발송되었습니다. 24시간 내에 링크를 확인해 주세요."
  },
  "error": null
}
```

#### 2) 이메일 중복 확인
- **Endpoint**: `GET /api/v1/auth/check-email?email={email}`
- **Permission**: `공개`
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "email": "user@example.com",
    "available": true
  },
  "error": null
}
```

#### 3) 닉네임 중복 확인
- **Endpoint**: `GET /api/v1/auth/check-nickname?nickname={nickname}`
- **Permission**: `공개`
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "nickname": "클라이머호호",
    "available": true
  },
  "error": null
}
```

#### 4) 이메일 인증 확인
- **Endpoint**: `POST /api/v1/auth/email/verify`
- **Permission**: `공개`
- **Request Body**:
```json
{
  "token": "a1b2c3d4e5f6..."
}
```
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "expiresIn": 1800,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "nickname": "클라이머호호",
      "role": "USER",
      "onboarded": false
    }
  },
  "error": null
}
```
*(쿠키: Set-Cookie `refresh_token`)*

#### 5) 이메일 로그인
- **Endpoint**: `POST /api/v1/auth/login`
- **Permission**: `공개`
- **Request Body**:
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "expiresIn": 1800,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "nickname": "클라이머호호",
      "role": "USER",
      "onboarded": true
    }
  },
  "error": null
}
```
*(성공 시 Set-Cookie `refresh_token=...; HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth; Max-Age=1209600`)*

#### 6) 토큰 재발급
- **Endpoint**: `POST /api/v1/auth/refresh`
- **Permission**: `공개` (단, 쿠키에 유효한 `refresh_token` 필요)
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "expiresIn": 1800
  },
  "error": null
}
```

#### 7) 로그아웃
- **Endpoint**: `POST /api/v1/auth/logout`
- **Permission**: `회원 (USER)`
- **Response (204 No Content)**:
*(Set-Cookie로 `refresh_token` Max-Age=0 만료 처리)*

#### 8) 비밀번호 재설정 메일 요청
- **Endpoint**: `POST /api/v1/auth/password/reset-request`
- **Permission**: `공개`
- **Request Body**:
```json
{
  "email": "user@example.com"
}
```
- **Response (200 OK)**: 보안을 위해 이메일 존재 여부와 무관하게 동일 응답
```json
{
  "success": true,
  "data": {
    "message": "입력하신 이메일로 비밀번호 재설정 링크를 발송했습니다."
  },
  "error": null
}
```

#### 9) 비밀번호 재설정 실행
- **Endpoint**: `POST /api/v1/auth/password/reset`
- **Permission**: `공개`
- **Request Body**:
```json
{
  "token": "reset-token-string...",
  "newPassword": "NewPassword123!"
}
```
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "message": "비밀번호가 성공적으로 변경되었습니다. 다시 로그인해 주세요."
  },
  "error": null
}
```

#### 10) 비회원 탐색 기록 이관 (Merge) `[P2·MVP 제외]`
> 비회원 체험(FR-AUTH-05)이 P2라 MVP에서는 구현하지 않는다. 고도화 후보로 계약만 남긴다.

- **Endpoint**: `POST /api/v1/explorations/merge`
- **Permission**: `회원 (USER)`
- **Request**: 요청 시 `guest_key` 쿠키 자동 전송
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "mergedReactionsCount": 8,
    "mergedSessionCount": 1
  },
  "error": null
}
```
*(응답 시 `guest_key` 쿠키 만료 처리)*

#### 11) 회원 탈퇴
- **Endpoint**: `DELETE /api/v1/users/me`
- **Permission**: `회원 (USER)`
- **Request Body**: 일반 회원은 비밀번호 확인 필수 (소셜 전용은 빈 객체 `{}`)
```json
{
  "password": "Password123!"
}
```
- **Response (204 No Content)**: 즉시 익명화 및 토큰 폐기 (탈퇴 처리·토큰 폐기는 P0, 개인정보 익명화는 `[P1]` — 탈퇴 시 개인정보 처리 방식(삭제·익명화)은 확인 필요)

---

### 3.2 사용자 프로필 및 온보딩 (`/api/v1/users/me`)

#### 1) 내 프로필 및 환경 정보 조회
- **Endpoint**: `GET /api/v1/users/me`
- **Permission**: `회원 (USER)`
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "클라이머호호",
    "profileImageUrl": "https://cdn.example.com/profiles/1.png",
    "role": "USER",
    "conditions": {
      "weeklyHours": 5,
      "monthlyBudget": 150000,
      "placePref": "INDOOR",
      "socialPref": "TOGETHER",
      "region": {
        "id": 1101,
        "code": "11110",
        "sido": "서울특별시",
        "sigungu": "종로구"
      }
    }
  },
  "error": null
}
```

#### 2) 프로필 기본 정보 수정 `[P2·MVP 제외]`
> FR-USER-02 중 닉네임·프로필 이미지 수정은 P2다. 관심 취미·이용 상황 수정(P1)은 아래 4) 및 3.5 관심 저장 API를 쓴다.

- **Endpoint**: `PATCH /api/v1/users/me`
- **Permission**: `회원 (USER)`
- **Request Body**:
```json
{
  "nickname": "새닉네임",
  "profileImageUrl": "https://cdn.example.com/profiles/new.png"
}
```
- **Response (200 OK)**: 수정된 사용자 정보 반환

#### 3) 가입 직후 온보딩 일괄 저장 `[P1]`
- **Endpoint**: `PUT /api/v1/users/me/onboarding`
- **Permission**: `회원 (USER)`
- **Request Body**:
```json
{
  "weeklyHours": 4,
  "monthlyBudget": 100000,
  "placePref": "ANY",
  "socialPref": "ALONE",
  "regionId": 1101,
  "initialHobbies": [
    {
      "hobbyId": 3
    }
  ]
}
```
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "onboarded": true,
    "savedHobbyCount": 1
  },
  "error": null
}
```

- **화면 규격 (10/30)**: `initialHobbies`는 최대 5개, 생략·`null`·빈 배열 허용. 배열 항목의 `hobbyId`는 필수 양수이며 중복 불가. 이 화면에는 숙련도 입력이 없어 `skillLevel`을 요청하지 않으며 새 관심 취미의 내부 기본값은 `BEGINNER`다.
- 이용 상황 전 항목은 선택이다. 미입력은 생략 또는 `null`, 상관없음은 `ANY`. 시간은 정수 0~40(시간/주), 예산은 0 이상의 32비트 정수(원/월), 지역은 선택한 시·도·시·군·구의 DB ID다.
- **선택지 조회 추가**: `GET /api/v1/users/me/onboarding/options` → `data.hobbies[{hobbyId,name,categoryId,categoryName}]`, `data.regions[{id,code,sido,sigungu}]`. 회원 인증 필요, 공개 취미만 반환. 프론트는 여기서 얻은 실제 ID를 전송한다.
- PUT은 두 화면 값을 원자적으로 전체 교체한다. 기존 `SAVED` 중 선택하지 않은 취미는 해제하며, 진행 중인 취미와 진행 이력은 유지한다. `savedHobbyCount`는 반영 후 `SAVED` 수다. 전체 건너뛰기 `{}`도 온보딩 완료로 기록한다. 상세 예시·오류·재시도 규칙은 [변경 문서](./api-changes-2026-10-30-onboarding-short-save.md) 참조.

#### 4) 이용 상황 및 선호 지역 수정 `[P1]`
- **Endpoint**: `PUT /api/v1/users/me/conditions`
- **Permission**: `회원 (USER)`
- **Request Body**:
```json
{
  "weeklyHours": 6,
  "monthlyBudget": 200000,
  "placePref": "OUTDOOR",
  "socialPref": "TOGETHER",
  "regionId": 1102
}
```
- **Response (200 OK)**: 갱신된 conditions 반환

#### 5) 마이페이지 요약 수치 조회
- **Endpoint**: `GET /api/v1/users/me/summary`
- **Permission**: `회원 (USER)`
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "savedHobbyCount": 4,
    "inProgressHobbyCount": 1,
    "savedShortCount": 12,
    "myPostCount": 3
  },
  "error": null
}
```
*(`savedShortCount`: 마이페이지 카드는 CSV상 P0이지만 숏폼 보관(FR-SHORT-06)이 P2라 보관 기능 도입 전에는 화면에서 숨긴다 — 확인 필요)*

---

### 3.3 숏폼 탐색 및 반응 (`/api/v1/explorations`, `/api/v1/shorts`)

> MVP에서 숏폼 탐색·추천은 **로그인 후에만** 쓴다(비회원 체험 FR-AUTH-05는 P2). 세션당 노출 수는 고정값이 아니라 설정값 `exploration.session-size`(N, 기본안 12)이며, 응답의 `maxCount`가 이 값이다. 진행 표시는 "n / N".

**탐색·추천 계약 결정 (2026-10-29)**

- 새 영상이 부족하면 **이전 세션에서 봤지만 현재 반응이 없는 영상**만 재사용한다. 같은 세션의 중복 영상, 관련 없음 반응 영상, 비활성 영상은 제외한다.
- 자연 종료 이후에도 최초 추천 생성은 허용한다. 같은 세션의 종료 요청을 다시 받아도 추천은 한 번만 생성하고 기존 결과를 반환한다. 마지막 시청 기록은 추천 생성 전까지 받고, 추천 생성 후에는 변경하지 않는다.
- 세션의 시간 만료 정책은 이번에 추가하지 않는다.
- 관심 주제 응답은 아래 명세의 `topInterests: [{hobbyName, percent}]` 형식을 유지한다.
- 최종 추천에는 이전 세션을 포함한 누적 탐색·반응 기록을 반영한다.
- 취미 유사도 계산은 현재 구현을 유지한다. 영상 수집 영역은 이번 작업 범위에서 제외하며, 수집 방식에 관한 기존 문서·구현의 차이는 여기서 변경하지 않는다.

위 항목은 구현 기준이며, 각 기능의 구현 완료 여부를 의미하지 않는다.

- **세션 크기 설정**: `exploration.session-size`는 기본 12이며 `EXPLORATION_SESSION_SIZE` 환경변수로 변경할 수 있다. 유효 범위는 DB `SMALLINT`에 맞춰 1~32767이고, 범위를 벗어나면 서버 기동 시 설정 검증에 실패한다. 세션 생성 시 `maxCount`에 저장하므로 설정 변경 후에도 기존 세션의 최대 노출 수·진행 현황·종료 기준은 유지된다.

**영상 선택 `[P1]`**

- 이전 세션에 양수 취미 점수가 없으면 현재 세션의 1~4번째 영상은 가능한 범위에서 서로 다른 분류로 분산한다. 분류를 먼저 균등 선택하고 그 안에서 영상을 무작위 선택한다. 분류가 부족하면 남은 후보를 사용한다.
- 이후에는 누적 점수가 양수인 상위 3개 선호 취미 중에서 70%, 누적 기록에서 덜 본 분류 중에서 30% 확률로 선택한다. 선호 후보가 없으면 탐색 후보를 사용한다. 확률은 영상마다 적용하므로 세션별 개수가 정확히 7:3으로 고정되지는 않는다.
- 설정은 `exploration.prefer-ratio`, 환경변수는 `EXPLORATION_PREFER_RATIO`(기본 `0.7`, 유효 범위 `0~1`)다.
- 현재 세션에서 `DISLIKE` 반응을 가진 영상의 **취미 전체**를 후보에서 제외한다. 반응 변경·취소 시 현재 상태를 반영한다. 추천 결과의 취미 제외(`hobby_dislikes`)와는 별도 정책이다.
- 새 영상 후보를 우선 사용하고, 모두 소진됐을 때만 이전 세션의 현재 무반응 영상을 재사용한다. 같은 세션 중복·비활성·현재 관련 없음 반응 제외는 모든 단계에 적용한다. 선택 후 잠금 대기 중 비활성화된 후보는 건너뛰고 다른 후보를 확인한다.

**고유 사용자 집계·영상 상태 전환**

- `view_user_count`는 해당 영상이 노출된 고유 회원 수다. 같은 회원의 반복 노출·여러 세션은 한 명으로 센다.
- `fail_user_count`는 해당 영상에 `playFailed: true`를 보고한 고유 회원 수다. 같은 회원의 반복 보고·여러 세션은 한 명으로 센다. 동일 세션·영상에서 한 번 보고된 실패는 뒤늦은 `false` 요청으로 지우지 않는다.
- `irrelevant_count`는 현재 `IRRELEVANT` 반응을 가진 고유 회원 수다. 같은 반응의 재전송은 중복 가산하지 않고, 다른 반응으로 변경하거나 허용된 취소를 하면 집계에서 빠진다.
- 성공한 노출·시청 기록·반응 변경은 저장된 원본 기록을 집계해 해당 영상의 카운터를 갱신한다. 요청 횟수를 단순 가산하지 않는다.

| 현재 상태 | 조건 | 변경 상태 |
|---|---|---|
| `ACTIVE` | 재생 실패 고유 회원 3명 이상 | `UNAVAILABLE` |
| `ACTIVE` | 관련 없음 고유 회원 5명 이상 **그리고** 고유 시청자 대비 20% 이상 | `AUTO_HIDDEN` |

- 두 조건을 동시에 만족하면 `UNAVAILABLE`을 우선한다. 이미 비활성인 영상은 집계 갱신으로 다른 상태로 덮어쓰거나 자동 복구하지 않는다. 관리자 복구·제외는 별도 P3 범위다.
- 영상별 행 잠금으로 서로 다른 회원·세션의 집계를 직렬화한다. 노출 직전에도 `ACTIVE` 상태를 확인하며, 후보 조회 후 숨김·이용 불가가 된 영상은 건너뛴다.

#### 1) 숏폼 탐색 세션 시작
- **Endpoint**: `POST /api/v1/explorations`
- **Permission**: `회원 (USER)` (비회원 `GUEST` 체험은 `[P2·MVP 제외]`)
- **Response (201 Created)**:
```json
{
  "success": true,
  "data": {
    "sessionId": 301,
    "maxCount": 12,
    "viewedCount": 1,
    "short": {
      "shortId": 41,
      "youtubeVideoId": "aBcD123xYz0",
      "title": "퇴근 후 1시간, 볼더링 첫 도전기",
      "duration": 58,
      "hobby": {
        "id": 3,
        "name": "클라이밍",
        "category": "운동·야외"
      },
      "myReaction": null,
      "isSaved": false
    }
  },
  "error": null
}
```
*(`maxCount` = 설정값 N(기본안 12). `isSaved`는 10/30부터 현재 사용자의 실제 보관 상태를 반환한다. 다음 영상 응답도 동일하다. 비회원 첫 호출 시 Set-Cookie `guest_key` 발급은 `[P2·MVP 제외]`)*

- **영상 선택**: 아래 다음 영상 조회와 동일한 P0 규칙을 적용한다. 새 영상이 없을 때만 과거 무반응 영상을 재사용한다.
- **후보 없음 (404 Not Found)**: 새 영상과 재사용 가능한 영상이 모두 없으면 `COMMON_NOT_FOUND`를 반환하며 세션·노출 기록을 생성하지 않는다.

#### 2) 다음 숏폼 영상 조회
- **Endpoint**: `GET /api/v1/explorations/{sessionId}/next`
- **Permission**: `회원 (USER)` (비회원 `GUEST` 체험은 `[P2·MVP 제외]`)
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "finished": false,
    "viewedCount": 2,
    "short": {
      "shortId": 57,
      "youtubeVideoId": "kLmnOP12345",
      "title": "물레로 나만의 감성 머그컵 만들기",
      "duration": 45,
      "hobby": {
        "id": 7,
        "name": "도예",
        "category": "공예·만들기"
      },
      "myReaction": null,
      "isSaved": false
    }
  },
  "error": null
}
```
*(세션 노출 수 N(`maxCount`) 도달 또는 영상 소진 시 `finished: true, short: null`. 사용자가 직접 종료하면 9) finish 호출)*  
*(P0 선택 규칙: `ACTIVE` 영상 중 현재 세션 노출 영상과 관련 없음 반응 영상을 제외한 뒤, 이전 세션을 포함해 아직 보지 않은 영상을 우선 무작위 선택한다. 새 후보가 없을 때만 이전 세션에서 봤지만 현재 반응이 없는 영상을 무작위로 재사용한다. `LIKE`·`DISLIKE`·`IRRELEVANT` 반응이 남아 있는 영상은 재사용하지 않는다. 반응을 취소해 현재 반응이 없으면 재사용할 수 있다. 재사용 가능한 영상까지 소진되면 종료한다. 첫 진입 분류별 분산과 선호 70%·탐색 30%는 `[P1]` 개선)*

- **종료 후 재조회**: `finished: true, short: null`을 다시 반환하며 기존 종료 상태·시각을 변경하지 않는다. 타인 또는 없는 세션은 `404 COMMON_NOT_FOUND`.

#### 3) 영상 노출/시청 시간/재생 실패 기록
- **Endpoint**: `POST /api/v1/explorations/{sessionId}/views`
- **Permission**: `회원 (USER)` (비회원 `GUEST` 체험은 `[P2·MVP 제외]`)
- **Request Body**:
```json
{
  "shortId": 41,
  "watchedSeconds": 38,
  "playFailed": false
}
```
- **Response (204 No Content)**

- **검증**: 본인 세션에 실제로 노출된 영상의 기록만 수정한다. 타인·없는 세션 또는 세션에 노출되지 않은 영상은 `404 COMMON_NOT_FOUND`이며 새 노출 기록을 만들지 않는다.
- **변경 가능 시점**: 자연 종료(`COMPLETED`) 후에도 추천 생성 전에는 마지막 기록을 받을 수 있다. 포기(`ABANDONED`)했거나 추천이 생성된 세션은 `409 SHORT_SESSION_ENDED`.
- **전송 순서**: 프론트엔드는 마지막 시청 기록과 반응 요청의 완료를 확인한 뒤 `finish`를 호출한다. 동시에 들어온 변경·종료 요청은 서버가 세션별로 직렬화하므로, 추천 생성 이후 도착한 변경은 거절될 수 있다.

#### 4) 숏폼 반응 등록 및 변경
- **Endpoint**: `PUT /api/v1/shorts/{shortId}/reaction`
- **Permission**: `회원 (USER)` (비회원 `GUEST` 체험은 `[P2·MVP 제외]`)
- **Request Body**:
```json
{
  "type": "LIKE",
  "sessionId": 301
}
```
*(type: `LIKE`, `DISLIKE`, `IRRELEVANT`)*
- **Validation**: `sessionId`는 MVP에서 필수인 양수다. 누락·0·음수는 `400 COMMON_INVALID_INPUT`.
- **세션·영상 검증**: 본인 세션에 노출된 영상이어야 한다. 타인·없는 세션 또는 노출 기록이 없으면 `404 COMMON_NOT_FOUND`. 포기했거나 추천이 생성된 세션은 `409 SHORT_SESSION_ENDED`, `ACTIVE`가 아닌 영상은 `409 SHORT_NOT_ACTIVE`.
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "shortId": 41,
    "type": "LIKE"
  },
  "error": null
}
```

#### 5) 숏폼 반응 취소
- **Endpoint**: `DELETE /api/v1/shorts/{shortId}/reaction`
- **Permission**: `회원 (USER)` (비회원 `GUEST` 체험은 `[P2·MVP 제외]`)
- **검증**: 본인 반응에 저장된 `sessionId`로 소유권·노출 기록·변경 가능 시점을 확인한다. 포기했거나 추천이 생성된 세션이면 `409 SHORT_SESSION_ENDED`, 비활성 영상이면 `409 SHORT_NOT_ACTIVE`. 세션 연결·노출 기록이 없으면 `404 COMMON_NOT_FOUND`. 세션 없는 반응은 MVP에서 지원하지 않는다.
- **반응 없음**: 존재하는 `ACTIVE` 영상에 본인 반응이 없으면 변경 없이 `200 OK`, `type: null`을 반환한다.
- **동시 변경**: 세션·영상 잠금 획득 후 현재 반응을 다시 확인한다. 그 사이 반응이 다른 세션으로 이동했다면 `409 SHORT_SESSION_ENDED`를 반환하며 변경하지 않는다. 프론트엔드는 현재 상태를 확인한 뒤 다시 요청할 수 있다.
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "shortId": 41,
    "type": null
  },
  "error": null
}
```

#### 6) 숏폼 영상 보관 / 보관 해제 `[10/30 화면 요구로 구현]`
- **Endpoint**: `PUT /api/v1/shorts/{shortId}/save` (보관)
- **Endpoint**: `DELETE /api/v1/shorts/{shortId}/save` (해제)
- **Permission**: `회원 (USER)`
- **Request Body**: 없음. 세션 ID 없이 현재 로그인 사용자에게 적용하며 좋아요·추천 점수·탐색 진행과 독립적이다.
- 중복 PUT/DELETE는 멱등적이다. 중복 PUT은 최초 보관 시각을 유지한다. 보관은 `ACTIVE` 영상만 가능(`409 SHORT_NOT_ACTIVE`), 해제는 숨김·이용 불가 영상에도 가능하다. 없는 영상은 `404 COMMON_NOT_FOUND`.
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "shortId": 41,
    "isSaved": true
  },
  "error": null
}
```

#### 7) 보관한 숏폼 목록 조회
- **Endpoint**: `GET /api/v1/users/me/saved-shorts?hobbyId={hobbyId}&page=0&size=20&sort=recent`
- **Permission**: `회원 (USER)`
- 인증된 본인의 보관 항목만 조회한다. `userId`를 요청으로 받지 않는다.
- `hobbyId`: 선택 양의 정수. 미지정 시 취미 미분류 영상을 포함한 전체 보관 목록.
- `page`: 0 이상의 정수(기본 0), `size`: 1~100(기본 20).
- `sort`: `recent`(기본, 보관 시각 내림차순), `oldest`(오름차순). 동일 시각은 `shortId`를 같은 방향으로 정렬한다. 페이지 간에 클라이언트가 별도로 정렬하지 않는다.
- 잘못된 파라미터는 `400 COMMON_INVALID_INPUT`, 미인증 요청은 `401 COMMON_UNAUTHORIZED`.
- 비활성 영상도 보관 목록과 건수에 포함한다. `status`는 실제 숏폼 상태이며 `ACTIVE`만 재생 가능하게 표시한다. 모든 상태에서 기존 보관 해제 API를 사용할 수 있다.
- `hobby`, `thumbnailUrl`은 nullable. `savedAt`은 UTC 기준 ISO 8601(오프셋 포함)이다.
- 보관 식별자는 사용자 내에서 `shortId`를 사용한다. 실제 DB는 `(user_id, short_id)` 복합 기본 키이며 별도 `saveId`는 없다.
- 결과가 없으면 `content: []`, `totalElements: 0`, `totalPages: 0`, `hasNext: false`를 반환한다.
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "shortId": 41,
        "youtubeVideoId": "aBcD123xYz0",
        "title": "퇴근 후 1시간, 볼더링 첫 도전기",
        "thumbnailUrl": "https://i.ytimg.com/vi/aBcD123xYz0/hqdefault.jpg",
        "hobby": { "id": 3, "name": "클라이밍" },
        "status": "ACTIVE",
        "savedAt": "2026-10-20T05:20:00Z"
      }
    ],
    "page": { "number": 0, "size": 20, "totalElements": 1, "totalPages": 1, "hasNext": false }
  },
  "error": null
}
```

#### 7-A) 보관한 숏폼 전체 건수·취미별 집계
- **Endpoint**: `GET /api/v1/users/me/saved-shorts/summary`
- **Permission**: `회원 (USER)`
- 본인의 전체 보관 항목을 기준으로 집계한다. 목록의 페이지·취미 필터와 독립적이며 비활성 영상도 포함한다.
- `totalCount`는 전체 보관 수, `hobbies`는 취미 이름·ID 오름차순(미분류는 마지막)의 취미별 건수다. 취미 미분류 그룹은 `id: null`, `name: null`이며 전체 건수에 포함한다.
- 보관 항목이 없으면 `totalCount: 0`, `hobbies: []`.
- 마이페이지 보관 수와 취미별 필터의 건수에 사용한다. 보관·해제 성공 후 목록과 이 집계를 다시 조회한다.
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "totalCount": 3,
    "hobbies": [
      { "id": 3, "name": "클라이밍", "count": 2 },
      { "id": null, "name": null, "count": 1 }
    ]
  },
  "error": null
}
```

#### 8) 탐색 세션 진행 현황 및 관심사 % 막대 조회
- **Endpoint**: `GET /api/v1/explorations/{sessionId}/progress`
- **Permission**: `회원 (USER)` (비회원 `GUEST` 체험은 `[P2·MVP 제외]`)
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "currentStep": 4,
    "maxCount": 12,
    "reactedHobbies": [
      { "id": 3, "name": "클라이밍", "reaction": "LIKE" },
      { "id": 5, "name": "크로스핏", "reaction": "DISLIKE" }
    ],
    "topInterests": [
      { "hobbyName": "클라이밍", "percent": 100 },
      { "hobbyName": "러닝", "percent": 75 },
      { "hobbyName": "도예", "percent": 40 }
    ]
  },
  "error": null
}
```
*(`topInterests`는 관심 주제 **상위 3개**까지 반환. 진행 표시는 `currentStep / maxCount`이며 `maxCount`는 설정값 N(기본안 12) — 예시의 12는 기본안 값)*

- **집계 범위**: 이 API는 현재 세션의 노출·시청·반응 기록만 사용한다. 최종 추천의 누적 기록 집계와 구분한다. `currentStep`은 해당 세션의 노출 수이며 관련 없음·재생 실패도 노출 수에는 포함한다. `maxCount`는 세션 생성 당시 값이다.
- **관심 주제 이름**: `topInterests[].hobbyName`에는 소재 태그명이 아닌 **취미명**을 반환한다. 소재 태그가 같아도 서로 다른 취미는 따로 집계한다.
- **점수·백분율**: 영상 점수 = 반응 점수(`LIKE` +3, `DISLIKE` -3, 무반응 0) + (시청 비율 × 2 - 1). 같은 취미의 영상 점수를 합산하고, 합계가 양수인 상위 3개까지 반환한다. `percent`는 취미 점수 ÷ 최고 점수 × 100을 정수로 반올림한다. 동점은 취미 ID 오름차순이다. 양수 점수가 없으면 `topInterests: []`.
- **제외 대상**: 현재 세션에서 관련 없음 반응을 받은 영상, 재생 실패 영상, 취미가 지정되지 않은 영상은 관심 점수와 `reactedHobbies` 모두에서 제외한다. 세션에 노출되지 않은 영상의 반응도 반환하지 않는다.
- **반응 목록**: `reactedHobbies`는 집계 대상 영상의 최초 노출 순서로 취미당 한 번 반환한다. 같은 취미에 여러 유효 반응이 있으면 수정 시각이 가장 최근인 반응을 사용하며, 시각이 같으면 반응 ID가 큰 것을 사용한다. 유효한 `DISLIKE`는 점수가 음수여도 목록에 표시한다.
- **변경·취소**: 반응 변경·취소는 다음 조회에 반영한다. 취소한 영상의 시청 점수는 유지하므로 반응을 취소해도 관심 주제가 남을 수 있다. 취소 후 해당 취미에 다른 유효 반응이 남아 있으면 그중 최신 반응을 표시한다.
- **일관성·권한**: 한 조회의 진행 수·시청·반응은 동일한 DB 스냅샷에서 읽는다. 타인 또는 없는 세션은 `404 COMMON_NOT_FOUND`. 반환 필드는 `currentStep`, `maxCount`, `reactedHobbies`, `topInterests`를 유지한다.

#### 9) 탐색 종료 및 추천 생성
- **Endpoint**: `POST /api/v1/explorations/{sessionId}/finish`
- **Permission**: `회원 (USER)` (비회원 `GUEST` 체험은 `[P2·MVP 제외]`)
- **Response (201 Created)**: (추천 결과 반환 - 3.4절 `POST /recommendations` 응답과 동일)
- **재요청 (200 OK)**: 이미 생성된 같은 세션의 추천 ID·추천 항목·순위를 반환한다. 점수 재계산·추천 추가 저장·AI 이유 이벤트 재발행은 하지 않는다. AI 이유는 기존 비동기 처리에 따라 갱신될 수 있다.
- **종료 조건**: 진행 중 세션의 직접 종료와 자연 종료 세션의 최초 추천 생성을 모두 허용한다. 포기한 세션의 최초 생성은 `409 SHORT_SESSION_ENDED`, 타인·없는 세션은 `404 COMMON_NOT_FOUND`.
- **동시 요청**: 세션 행 잠금을 사용해 동일 세션의 추천 생성과 다음 영상·시청·반응 변경을 직렬화한다. 동시에 종료를 요청해도 최초 한 건만 생성하고 나머지는 기존 추천을 반환한다.

---

### 3.4 취미 검색·상세 및 맞춤 추천 (`/api/v1/hobbies`, `/api/v1/recommendations`)

#### 1) 취미 다중 필터 검색
- **Endpoint**: `GET /api/v1/hobbies`
- **Permission**: `공개`
- **Query Parameters**:
  - `keyword`: 검색어 (2자 이상, 취미명/태그 부분일치)
  - `time`: 소요시간 범위 (`1H_LESS`, `1H_TO_2H`, `2H_MORE`)
  - `cost`: 월 비용 범위 (`30K_LESS`, `30K_TO_100K`, `100K_MORE`)
  - `difficulty`: 난이도 (`EASY`, `NORMAL`, `HARD`)
  - `place`: 장소 구분 (`INDOOR`, `OUTDOOR`, `ANY`)
  - `social`: 참여 형태 (`ALONE`, `TOGETHER`, `ANY`)
  - `sort`: 정렬 (`RECOMMEND` (로그인시 기본), `POPULAR` (비회원 기본), `NAME`, `COST_ASC`)
  - `page`: 페이지 번호 (0부터)
  - `size`: 크기 (기본 20)
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 3,
        "name": "클라이밍",
        "category": { "id": 1, "name": "운동·야외" },
        "summary": "온몸의 근육과 집중력을 깨우는 실내 암벽 등반",
        "thumbnailUrl": "https://cdn.example.com/hobbies/3.jpg",
        "difficulty": "NORMAL",
        "placeType": "INDOOR",
        "socialType": "BOTH",
        "sessionMinutes": { "min": 60, "max": 120 },
        "monthlyCost": { "min": 100000, "max": 150000 },
        "saveCount": 420,
        "tags": ["실내", "활동적", "집중력"]
      }
    ],
    "page": { "number": 0, "size": 20, "totalElements": 28, "totalPages": 2, "hasNext": true }
  },
  "error": null
}
```

#### 2) 취미 상세 조회
- **Endpoint**: `GET /api/v1/hobbies/{hobbyId}`
- **Permission**: `공개` (로그인 시 사용자 상태 `myStatus` 자동 포함)
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 3,
    "name": "클라이밍",
    "category": { "id": 1, "name": "운동·야외" },
    "summary": "온몸의 근육과 집중력을 깨우는 실내 암벽 등반",
    "description": "클라이밍은 손과 발을 이용해 인공 암벽의 홀드를 잡고 목표 지점에 도달하는 운동입니다...",
    "thumbnailUrl": "https://cdn.example.com/hobbies/3.jpg",
    "placeType": "INDOOR",
    "socialType": "BOTH",
    "difficulty": "NORMAL",
    "sessionMinutes": { "min": 60, "max": 120 },
    "initialCost": { "min": 30000, "max": 80000 },
    "monthlyCost": { "min": 100000, "max": 150000 },
    "supplies": "암벽화(초기 대여 가능), 초크백, 편안한 운동복",
    "environment": "실내 인공암벽장, 냉난방 시설 완비",
    "firstActivity": "1일 체험 강습으로 안전 낙하법과 기본 홀드 잡기 배우기",
    "cautions": "손목과 손가락 관절 부상 방지를 위한 충분한 스트레칭 필수",
    "tags": ["실내", "활동적", "전신운동", "성취감"],
    "saveCount": 420,
    "myStatus": "SAVED"
  },
  "error": null
}
```
*(myStatus: `NONE`, `SAVED`, `IN_PROGRESS`, `PAUSED`, `COMPLETED`, `STOPPED` — PAUSED·STOPPED는 현재 상태 변경 API에서 사용하며 COMPLETED 자동 전환은 제외)*

#### 3) 취미 상세 관련 숏폼 목록 `[P1]`
- **Endpoint**: `GET /api/v1/hobbies/{hobbyId}/shorts?limit=6`
- **Permission**: `공개`
- **Query Parameter**: `limit`은 생략 시 6, 정수 `1~6`. 범위를 벗어나거나 양수가 아닌 취미 ID는 `400 COMMON_INVALID_INPUT`.
- **필터·정렬**: 공개(`PUBLISHED`) 취미의 `ACTIVE` 영상만 반환한다. 공개 반영 시각(`released_at`, 없으면 `created_at`) 내림차순, 영상 ID 내림차순이다. 개인별 시청·반응 여부로 제외하지 않는다.
- **Response (200 OK)**: `data`는 최대 `limit`개의 카드 배열이며 영상이 없으면 `[]`. 없거나 비공개인 취미는 `404 COMMON_NOT_FOUND`.
```json
{
  "success": true,
  "data": [
    {
      "shortId": 41,
      "youtubeVideoId": "aBcD123xYz0",
      "title": "퇴근 후 1시간, 볼더링 첫 도전기",
      "duration": 58,
      "thumbnailUrl": "https://cdn.example.com/shorts/41.jpg"
    }
  ],
  "error": null
}
```

#### 4) 취미 TODO 플랜 미리보기 `[P1]`
- **Endpoint**: `GET /api/v1/hobbies/{hobbyId}/plan-preview?skillLevel=BEGINNER`
- **Permission**: `공개`
- **Response (200 OK)**: 1~2주차 활동 예시 데이터

#### 5) 추천 생성 (탐색 세션 기반)
- **Endpoint**: `POST /api/v1/recommendations`
- **Permission**: `회원 (USER)` (비회원 `GUEST` 체험은 `[P2·MVP 제외]`)
- **Request Body**:
```json
{
  "basis": "EXPLORATION_SESSION",
  "sessionId": 301
}
```
`basis`와 양수 `sessionId`는 필수다. `DIRECT_SELECTION`(관심사 직접 선택, FR-SHORT-05)은 `[P2·MVP 제외]`이며 MVP에서는 `400 COMMON_INVALID_INPUT`. `hobbyIds`는 생략·`null`·빈 배열만 허용한다.

- `POST /explorations/{sessionId}/finish`와 동일한 생성 처리를 사용한다. 최초 생성은 `201 Created`, 같은 세션의 재요청은 `200 OK`로 저장된 결과를 반환한다. 포기한 세션은 `409 SHORT_SESSION_ENDED`, 타인·없는 세션은 `404 COMMON_NOT_FOUND`.
- **누적 집계**: 본인의 모든 세션 기록에서 영상별 가장 최근 시청 기록(`viewed_at DESC, id DESC`) 하나와 현재 반응 하나를 사용한다. 반복 시청·반응 변경을 중복 가산하지 않는다. 반응 취소 시 시청 점수는 유지한다. 관심사 초기화 시각이 이미 저장되어 있으면 그 이후 기록만 사용하며, 초기화 API 자체는 P2다.
- **점수**: 영상별 `LIKE +3`, `DISLIKE -3`, 무반응 0에 `시청 비율 × 2 - 1`을 더한다. 취미별로 합산하고, 각 태그에는 영상 점수의 `0.3`을 합산한다. 최신 시청이 재생 실패인 영상, 현재 `IRRELEVANT`인 영상, 취미가 없는 영상은 점수·LIKE 수·`reactionCount`에서 제외한다. `reactionCount`는 집계 대상 영상의 현재 LIKE·DISLIKE 반응 수(최대 32767)다.
- **일반 추천**: 양수 취미 점수 상위 3개를 `INTEREST`, 관심 취미와의 최대 기존 유사도 × `0.7` + 정규화 태그 점수 × `0.3`의 상위 3개를 `RELATED`로 구성한다. 취미 제외는 양쪽에 적용하고, 연관 후보에서는 관심 취미·진행 중 취미도 제외한다. 기존 취미 유사도 계산 방식은 유지한다. 동점은 취미 ID 오름차순이다.
- **정보 부족 `[P1]`**: 집계 대상 LIKE 영상이 3개 미만이면 `isExploratory: true`. 관심 저장 수에 이용 상황 가중치를 적용한 순서로 서로 다른 분류에서 최대 6개를 선택한다. 추천 제외·진행 중 취미를 제외하며 분류가 부족하면 적게 반환한다. 앞 3개는 `INTEREST`, 나머지는 `RELATED`다.
- **이용 상황 `[P1]`**: 월 비용 최솟값이 월 예산을 넘거나 1회 시간 최솟값이 주간 가능 시간(분 환산)을 넘으면 점수에 `0.8`을 한 번 적용하고 `conditionWarning: true`로 표시한다. 장소·참여 형태가 맞으면 각각 `1.1`을 곱한다(`BOTH`는 양쪽에 일치). 미입력·`ANY`는 해당 가중치를 적용하지 않으며 후보를 필터링하지 않는다.
- **스냅샷**: 취미 ID·명칭·이미지·난이도·시간·비용·정렬 점수·순위·저장 여부(`saved`)·이용 상황·경고·추천 기준 문구는 생성 시점에 저장한다. 이후 취미 정보·저장 상태·반응·이용 상황을 바꿔도 기존 결과는 재계산하지 않는다. AI `reason`·`reasonStatus`만 비동기 변경된다. 과거 추천은 V9 마이그레이션 시점의 취미·저장 정보를 고정하며 원래 생성 시점의 정보를 복원하지는 않는다.
- API `basis`는 `EXPLORATION_SESSION`이며 기존 DB 표현 `REACTION`과 구분한다. `rank`는 전체 항목에서 1부터 순서대로 부여한다. 이유가 준비되지 않았거나 실패하면 항상 제공되는 `fallbackReason`을 표시한다.
- **Response (201 Created)**:
```json
{
  "success": true,
  "data": {
    "recommendationId": 88,
    "basis": "EXPLORATION_SESSION",
    "isExploratory": false,
    "reactionCount": 5,
    "basisText": "숏폼 누적 반응 5개와 이용 상황을 바탕으로 추천했어요",
    "items": [
      {
        "itemId": 201,
        "type": "INTEREST",
        "rank": 1,
        "hobby": {
          "id": 3,
          "name": "클라이밍",
          "thumbnailUrl": "https://cdn.example.com/hobbies/3.jpg",
          "difficulty": "NORMAL",
          "sessionMinutes": { "min": 60, "max": 120 },
          "monthlyCost": { "min": 100000, "max": 150000 }
        },
        "reason": null,
        "reasonStatus": "PENDING",
        "fallbackReason": "클라이밍 영상에 높은 관심을 보였어요",
        "conditionWarning": false,
        "saved": false
      },
      {
        "itemId": 202,
        "type": "RELATED",
        "rank": 2,
        "hobby": {
          "id": 14,
          "name": "볼더링",
          "thumbnailUrl": "https://cdn.example.com/hobbies/14.jpg",
          "difficulty": "NORMAL",
          "sessionMinutes": { "min": 60, "max": 90 },
          "monthlyCost": { "min": 90000, "max": 130000 }
        },
        "reason": null,
        "reasonStatus": "PENDING",
        "fallbackReason": "관심 취미인 클라이밍과 유사한 신체 활동이에요",
        "conditionWarning": false,
        "saved": false
      }
    ]
  },
  "error": null
}
```

#### 6) 추천 상세 조회 (비동기 사유 폴링용) — 비동기 AI 추천 이유는 `[P1]`
- **Endpoint**: `GET /api/v1/recommendations/{id}`
- **Permission**: `회원 (USER)` (비회원 `GUEST` 체험은 `[P2·MVP 제외]`)
- **Response (200 OK)**: 추천 생성과 같은 응답 구조. 본인 추천만 조회하며 타인·없는 추천은 `404 COMMON_NOT_FOUND`.
- **상태 흐름**: 생성 직후 `reasonStatus: "PENDING"`, `reason: null`. RabbitMQ 소비자가 GMS 이유 생성을 마치면 `COMPLETED`와 60자 이내 한 문장으로 응답한다. GMS 설정 없음·오류·빈 결과는 `FALLBACK`, `reason: null`이며 `fallbackReason`을 유지한다. 모든 항목이 `COMPLETED` 또는 `FALLBACK`이면 폴링을 종료한다. 기존 DB의 `DONE`도 응답에서는 `COMPLETED`로 변환한다.
- **발행 보장**: 추천과 이유 발행 요청(outbox)을 같은 DB 트랜잭션에 저장한다. 별도 작업자가 5초 주기로 커밋된 요청을 읽어 RabbitMQ 발행 확인 후 제거한다. 실패 시 10·20·40·80초 간격으로 재시도하며 5번째 실패 시 남은 `PENDING` 항목을 `FALLBACK`으로 전환한다. 중복 메시지는 완료된 항목의 이유를 재생성하지 않는다.
- **운영 설정**: `RECOMMENDATION_REASON_OUTBOX_ENABLED`는 기본 `true`. `false`이면 발행 작업만 중지하고 요청은 DB에 보존한다. 재활성화 전까지 해당 추천의 이유는 `PENDING`일 수 있다. 로컬·서버 `.env.example`에 예시를 함께 관리한다.

#### 7) 취미 별로예요 (추천 제외) 등록 / 취소
- **Endpoint**: `PUT /api/v1/hobbies/{hobbyId}/dislike`
- **Endpoint**: `DELETE /api/v1/hobbies/{hobbyId}/dislike`
- **Permission**: `회원 (USER)`
- **Response (200 OK)**: 등록은 `data: {"hobbyId": 3, "disliked": true}`, 취소는 `data: {"hobbyId": 3, "disliked": false}`. 같은 요청을 반복해도 성공하며 사용자별로 독립적이다.
- 등록 이후 새로 생성하는 관심·연관·탐색용 추천 후보에서 제외하고 취소하면 다시 후보가 될 수 있다. 이미 생성된 추천 스냅샷은 변경하지 않는다.
- 등록은 공개(`PUBLISHED`) 취미만 허용한다. 취소는 이미 비공개가 된 취미도 허용한다. 없는 취미는 `404 COMMON_NOT_FOUND`, 양수가 아닌 ID는 `400 COMMON_INVALID_INPUT`, 비로그인은 `401`이다.

---

### 3.5 내 취미 및 TODO (`/api/v1/users/me/hobbies`, `/api/v1/plans`)

> **2026-10-30 구현 계약**: 00-2 홈, 06-1 내 취미, 06-2 이번 주 TODO, 06-3A/B 전체 계획을 지원한다. 일시중지·재개·그만두기와 개인 계획 편집을 현재 범위에 포함한다. 전체 계획 자동 `COMPLETED` 전환·축하, 종료 후 새 계획 시작·지난 취미 UI는 제외한다. 상세 변경·연동 안내는 [TODO API 변경 기록](api-changes-2026-10-30-todo.md)을 따른다.

#### 공통 정책

- 모든 경로는 로그인 필요. 계획 조회·수정은 소유자만 가능하다. 타인 계획은 `403 COMMON_FORBIDDEN`, 없는 계획·다른 계획 소속 itemId·삭제된 활동 PATCH는 `404 COMMON_NOT_FOUND`.
- 동시 진행은 **IN_PROGRESS 최대 3개**. PAUSED는 제외한다. 시작·재개·상태 변경·관심 저장/해제·활동 변경은 같은 사용자 행을 잠근 후 상태를 읽고 변경한다.
- DB 날짜는 UTC. 이 절의 응답 날짜는 ISO 8601 `+09:00`. 현재 주차는 서울 달력 날짜 기준 `floor(시작 후 유효 경과 일수 / 7) + 1`, 최대 `totalWeeks`. 누적 일시중지 시간을 초 단위로 제외하며 중지 중에는 pausedAt에서 동결한다. STOPPED는 endedAt에서 동결한다. 열람·조기 완료는 현재 주차를 이동시키지 않는다.
- 모든 집계에서 soft delete 활동 제외. `Progress = {done, skipped, total, rate}`, `rate = DONE / total` (0~1, total=0이면 0). SKIPPED는 분모에 포함한다.
- `weekCompleted`는 **해당 주차가 비어 있지 않고 DONE+SKIPPED=total**일 때 true. `planCompleted`도 같은 규칙을 전체 계획에 적용한 처리 완료 신호다. 상태를 COMPLETED로 자동 변경하지 않는다.
- `ACTIVE` 계획이면서 취미가 `IN_PROGRESS`일 때만 계획 편집 가능. 상태·메모 변경은 현재/과거 주차만 허용한다. 미래 주차는 미리보기와 활동 추가·삭제·주차 이동·시간 변경만 허용한다.
- 06-3 전체 계획의 펼침은 읽기 전용 UI다. API의 `canEditPlan`은 별도 편집 기능의 권한이며, 전체 계획 화면에서 완료 버튼을 표시하라는 뜻이 아니다.

#### 1) 내 취미 목록

`GET /api/v1/users/me/hobbies?status=` → **200**, 기존 `data: []` 형식 유지.

- status 생략 시 모든 상태. `SAVED|IN_PROGRESS|PAUSED|COMPLETED|STOPPED` 필터 가능(기존과 같이 대소문자·주변 공백 허용), 잘못된 값은 400.
- 정렬은 savedAt 내림차순, userHobbyId 내림차순. 페이지 제한 없음. 06-1은 상태별로 묶어 사용하며 종료 이력 UI는 이번 범위에 포함하지 않는다.
- 기존 `userHobbyId, hobbyId, name, thumbnailUrl, status, skillLevel, planId, progressRate, startedAt` 유지. planId·progressRate는 실제 현재 계획에서 계산한다.
- 추가 필드: `title, totalWeeks, currentWeek, weekTitle, planProgress, weekProgress, remainingThisWeekCount, overdueCount, pausedAt, availableSkillLevels, planReady`.
- 현재 계획 없는 SAVED 항목의 planId·계획 정보·진행 집계는 null, progressRate·남은 수·밀린 수는 0.
- `availableSkillLevels`는 해당 취미의 공개 템플릿 숙련도 목록(문자열 오름차순). `planReady`는 저장된 skillLevel의 공개 템플릿 존재 여부이며, 시작 시 원본의 주차·활동 완전성을 다시 검사한다. 기본 skillLevel은 BEGINNER.
- 상태별 총 개수와 최대 진행 수는 9)의 summary에서 함께 제공한다.

#### 2) 관심 취미 저장 / 해제

- `PUT /api/v1/users/me/hobbies/{hobbyId}/save` → **200**, `data: {"saved": true}`.
- `DELETE /api/v1/users/me/hobbies/{hobbyId}/save` → **200**, `data: {"saved": false}`.
- PUT은 공개 취미만 허용(없는/비공개 취미는 404 HOBBY_NOT_FOUND). 기존 SAVED면 유지, 없으면 생성. DELETE는 SAVED만 삭제하며 이미 없으면 성공한다.
- IN_PROGRESS·PAUSED는 양쪽 모두 `409 MY_HOBBY_IN_PROGRESS`. STOPPED·COMPLETED는 `409 MY_HOBBY_INVALID_TRANSITION`. 계획이 있는 취미를 SAVED로 덮거나 삭제하지 않는다.
- 실제 SAVED 생성/삭제 때만 hobby.saveCount 증감. SAVED에서 시작하면 관심 수를 1 줄인다. 재시도는 중복 증감하지 않는다.

#### 3) 취미 시작하기

`POST /api/v1/users/me/hobbies/{hobbyId}/start`

요청: `{"skillLevel":"BEGINNER"}`. 필수, `BEGINNER|NOVICE|INTERMEDIATE`.

- 선택한 숙련도의 공개 원본만 사용하며 자동 하위 숙련도 대체는 하지 않는다. 선택 가능한 숙련도는 목록의 availableSkillLevels로 제공한다.
- 공개 원본의 제목·totalWeeks·버전·주차 제목·전체 활동을 사용자 사본으로 복사한다. 이후 원본 변경은 반영하지 않는다.
- 원본 없음·빈 활동·누락된 주차 제목·잘못된 주차/순서/시간은 `409 MY_PLAN_NOT_READY`. 시작에는 최소 한 활동이 필요하다. 빈 주차는 허용한다.
- 네 번째 진행 취미 시작은 `409 MY_HOBBY_LIMIT`. 관심 저장 없이도 바로 시작 가능하다.
- IN_PROGRESS·PAUSED 중복 시작은 `409 MY_HOBBY_IN_PROGRESS` (PAUSED 재개는 4) 사용). STOPPED·COMPLETED 새 계획 생성은 지원하지 않으며 `409 MY_HOBBY_INVALID_TRANSITION`.
- 성공 **201 Created**:

```json
{
  "success": true,
  "data": {"userHobbyId": 12, "planId": 55, "status": "IN_PROGRESS", "title": "클라이밍 · 입문 3주 계획"},
  "error": null
}
```

#### 4) 일시중지 / 재개 / 그만두기

`PATCH /api/v1/users/me/hobbies/{hobbyId}/status`, 요청 `{"status":"PAUSED"}`.

- 허용 전이: `IN_PROGRESS → PAUSED`, `PAUSED → IN_PROGRESS`, `IN_PROGRESS|PAUSED → STOPPED`. 같은 상태 요청은 성공하며 pausedAt·endedAt을 덮지 않는다.
- 재개 시 3개 제한 재검사. planId·startedAt·주차 제목·활동·메모·완료 기록 유지. 재개는 신규 시작이 아니다.
- 나머지 전이는 `409 MY_HOBBY_INVALID_TRANSITION`. 관계가 없으면 404.
- **200** data: `{userHobbyId, planId, status, currentWeek, startedAt, pausedAt}`. 날짜는 +09:00, 미중지 pausedAt은 null. 그만두기는 계획·활동을 삭제하지 않는다.

#### 5) 주차별 TODO 조회

`GET /api/v1/plans/{planId}?week={weekNo}` → **200**.

week 생략 시 currentWeek. 1~totalWeeks 범위를 벗어나면 400. 미래 주차도 내 사본을 조회한다.

| data 필드 | 의미 |
|---|---|
| planId, userHobbyId, hobbyId, title | 계획·취미 식별 및 제목 |
| hobbyStatus, planStatus, skillLevel, startedAt | 취미 상태, ACTIVE/ARCHIVED, 숙련도, 시작일 |
| totalWeeks, currentWeek, selectedWeek, weekTitle | 총 주차, 시간상 현재 주차, 조회 주차, 복사된 주차 제목 |
| progress, planProgress | 전체 계획 Progress. progress는 기존 계약의 호환 별칭 |
| weekProgress, weekExpectedMinutes | 조회 주차 Progress 및 삭제 제외 전체 활동 시간 합계(완료·건너뜀 포함) |
| overdueCount, overdueItems | 계획 전체의 currentWeek 이전 TODO 수·상세 목록. SKIPPED 제외 |
| skippedItems | 계획 전체 SKIPPED 상세 목록 |
| canEditPlan, canUpdateItems | 계획 편집 권한, 조회 주차 상태·메모 변경 권한 |
| weekCompleted, nextWeek, planCompleted | 주차 처리 완료, selectedWeek+1(마지막이면 null), 전체 처리 완료 |
| weeks | 1~totalWeeks 모든 주차 요약 |
| items | 조회 주차의 삭제되지 않은 활동 |
| supplies | 조회 주차 준비물 표시 문자열 배열. 쉼표·전각 쉼표·줄바꿈 분리, trim·빈 값 제거·등장순 중복 제거 |

`weeks[]` 필드:
`{weekNo, title, status, current, progress, expectedMinutes, overdueCount, weekCompleted, canUpdateItems, canEditPlan}`.
status는 시간상 `PAST|CURRENT|FUTURE`이며 완료 여부는 별도 weekCompleted로 판단한다. 진행이 멈춘 계획도 동결된 주차를 기준으로 분류한다.

`items[]`, `overdueItems[]`, `skippedItems[]` 공통:
`{itemId, weekNo, seq, title, description, expectedMinutes, supplies, doneCriteria, status, memo, isCustom, completedAt}`.
itemId는 사용자 사본 ID, status는 `TODO|DONE|SKIPPED`. 정렬은 weekNo·seq·itemId 오름차순이다.

준비물 체크는 **프론트 임시 상태**이며 서버 쓰기 API·영속 ID·체크 상태는 제공하지 않는다. 원문 items[].supplies도 유지한다.

#### 6) 활동 상태 / 메모 / 주차 / 시간 변경

`PATCH /api/v1/plans/{planId}/items/{itemId}`

```json
{"status":"DONE","memo":"활동 기록"}
```

각 필드는 선택 사항이지만 최소 하나 필요하다.

| 필드 | 계약 |
|---|---|
| status | TODO/DONE/SKIPPED. 생략=유지, null 불가 |
| memo | 500자 이하. 생략=유지, null=삭제, 빈 문자열=빈 메모 저장. 메모만 PATCH 가능 |
| weekNo | 정수 1~totalWeeks. 생략=유지, null 불가. 이동하면 도착 주차 마지막 순서에 삽입 |
| expectedMinutes | 정수 1~1440분. 생략=유지, null 불가 |

- 원본 활동·사용자 추가 활동 모두 위 필드 수정 가능. 기존 제목·설명·준비물·완료 기준과 임의 seq 재정렬은 PATCH 대상이 아니다.
- DONE 최초 전환에 completedAt 기록, 동일 DONE 재시도는 최초 시각 유지. TODO/SKIPPED 전환은 completedAt 제거.
- 미래 주차 활동의 status/memo 변경은 `409 TODO_FUTURE_WEEK`. 주차 이동을 같이 보내도 **이동 전·후 주차 중 하나라도 미래면** 상태·메모를 동시에 변경할 수 없다.
- PAUSED·STOPPED·COMPLETED·ARCHIVED 수정은 `409 TODO_PLAN_NOT_ACTIVE`.
- **200** data: `{itemId, status, memo, completedAt, item, progress, planProgress, weekProgress, weekCompleted, nextWeek, planCompleted}`. item은 위 활동 DTO, weekProgress는 **수정 후 활동 소속 주차**, progress는 planProgress 호환 별칭이다.

#### 7) 활동 추가

`POST /api/v1/plans/{planId}/items` → **201**, 6)과 같은 mutation data.

```json
{"weekNo":2,"title":"보조 연습","description":"연습 방법","expectedMinutes":15,"supplies":"연습 도구","doneCriteria":"한 번 수행"}
```

- weekNo·title·expectedMinutes 필수. title은 공백만 불가, 최대 50자(trim 저장). description 최대 500자, supplies·doneCriteria 각각 최대 200자, 선택 필드는 null 허용.
- weekNo는 1~totalWeeks, 시간은 정수 1~1440. 해당 주차 마지막에 추가, status=TODO, isCustom=true, templateItemId=null.
- 1주차 활동 최대 32767개. 추가·이동 시 순서는 마지막 seq+1이며 필요하면 압축한다.
- 현재 진행 중인 ACTIVE 사본만 편집, 미래 주차 추가도 가능. 추가 후 집계에 즉시 반영한다.

#### 8) 활동 삭제

`DELETE /api/v1/plans/{planId}/items/{itemId}` → **204 No Content**.

- 원본·사용자 추가·완료·건너뜀 활동 모두 soft delete. 이력 행 유지, 이후 모든 조회·집계에서 제외한다.
- 같은 활동 재삭제는 204. 삭제된 활동 PATCH·다른 계획 소속 활동 삭제는 404.
- 삭제·이동 후 출발 주차의 남은 활동은 기존 순서를 보존해 1부터 seq를 정리한다. 빈 주차/계획 조회는 허용하며 total=0·rate=0·완료=false.
- 재조회: 해당 주차 GET과 overview, 내 취미 목록, 홈 summary. 이동 시 원래 주차도 재조회한다.

#### 9) 홈 TODO 및 상태별 요약 (신규)

`GET /api/v1/users/me/todos/summary` → **200**.

```json
{
  "success": true,
  "data": {
    "remainingThisWeekCount": 1,
    "overdueCount": 1,
    "inProgressCount": 1,
    "maxInProgressCount": 3,
    "statusCounts": {"SAVED":0,"IN_PROGRESS":1,"PAUSED":0,"COMPLETED":0,"STOPPED":0},
    "plans": [
      {
        "userHobbyId":12,"hobbyId":3,"name":"클라이밍","thumbnailUrl":null,
        "status":"IN_PROGRESS","skillLevel":"BEGINNER","planId":55,"progressRate":0.0,
        "startedAt":"2026-10-02T00:00:00+09:00","title":"클라이밍 · 입문 3주 계획",
        "totalWeeks":3,"currentWeek":2,"weekTitle":"기본 동작 익히기",
        "planProgress":{"done":0,"skipped":0,"total":3,"rate":0.0},
        "weekProgress":{"done":0,"skipped":0,"total":1,"rate":0.0},
        "remainingThisWeekCount":1,"overdueCount":1,"pausedAt":null,
        "availableSkillLevels":["BEGINNER"],"planReady":true
      }
    ]
  },
  "error": null
}
```

- plans는 실제 계획이 있는 IN_PROGRESS 취미 전체이며 1)과 같은 DTO·정렬. 최대 카드 수를 별도 제한하지 않는다.
- remainingThisWeekCount는 진행 중 계획의 **currentWeek TODO만** 합산. overdueCount는 같은 계획들의 **과거 주차 TODO만** 합산하며 남은 수에 더하지 않는다. PAUSED 제외.
- inProgressCount·statusCounts는 사용자 취미 전체 범위이며 필터/노출 수와 무관하다. 데이터가 정상 연결된 경우 inProgressCount=plans.length.
- 기존 `GET /users/me/summary`의 마이페이지 수치 계약과 별도다. 홈에서 계획별 추가 GET 없이 카드 표시 가능.

#### 10) 전체 계획 조회 (신규)

`GET /api/v1/plans/{planId}/overview` → **200**, 5)의 View 공통 DTO 사용.

- weeks는 모든 주차, overdueItems·skippedItems는 계획 전체. 06-3A/B는 하나의 화면으로 totalWeeks를 따르며 8주로 고정하지 않는다.
- overview에서는 selectedWeek·weekTitle·weekProgress·nextWeek=null, weekExpectedMinutes=0, items·supplies=[], canUpdateItems·weekCompleted=false. 전체 집계·planCompleted·canEditPlan은 실제 값이다.
- 주차를 펼칠 때 5)의 `?week=N` 조회 재사용. 미래 주차도 사용자 사본을 사용한다.

---

### 3.6 활동 장소 및 정보 피드 (`/api/v1/regions`, `/api/v1/places`, `/api/v1/feeds`) `[P1]`

> 선호 지역·활동 장소 추천·장소 수집 배치(FR-PLACE-01~03)와 취미 정보 피드·피드 수집 배치(FR-FEED-01·02)는 모두 P1이다. 관리자 검토(FR-ADM-06)가 P2라 MVP에서는 규칙·GPT 분류를 통과한 피드·장소를 `PENDING_REVIEW` 없이 바로 `PUBLISHED`로 두고 이 API로 노출한다(검토 단계는 P2 도입 시) — 확인 필요.

#### 1) 행정구역 지역 목록 조회 `[P1]`
- **Endpoint**: `GET /api/v1/regions?sido={sido}`
- **Permission**: `공개`
- **Response (200 OK)**:
  - `sido` 생략 시 시·도 목록 반환 (MVP는 서울특별시 활성, 타 지역 `enabled: false`)
  - `sido="서울특별시"` 전달 시 서울시 25개 구 목록 반환

#### 2) 활동 장소 검색 (목록 + 지도 좌표) `[P1]`
- **Endpoint**: `GET /api/v1/places?regionId={regionId}&hobbyId={hobbyId}&category={category}`
- **Permission**: `공개`
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 105,
      "name": "서울볼더스 클라이밍 컴퍼니",
      "category": "STUDIO",
      "address": "서울특별시 마포구 월드컵북로 123",
      "lat": 37.5562145,
      "lng": 126.9213451,
      "openingHours": "평일 10:00~23:00 / 주말 10:00~20:00",
      "feeText": "1일 이용권 20,000원",
      "externalUrl": "https://place.map.kakao.com/123456",
      "source": "KAKAO_LOCAL",
      "needsInfoCheck": false
    }
  ],
  "error": null
}
```

#### 3) 취미 정보 피드 목록 조회 `[P1]`
- **Endpoint**: `GET /api/v1/feeds?hobbyId={hobbyId}&type={type}&excludeEnded=true&page=0&size=20`
- **Permission**: `공개`
- **Query Parameter**:
  - `type`: `EVENT`(축제·행사), `SHOW`(공연·전시), `CLASS`(강좌·클래스), `GUIDE`(입문 자료)
  - `excludeEnded`: 종료된 행사 숨김 여부 (기본 true)
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 501,
        "hobbyId": 3,
        "hobbyName": "클라이밍",
        "type": "EVENT",
        "title": "2026 전국 청소년 스포츠클라이밍 페스티벌",
        "summary": "스포츠클라이밍 꿈나무들을 위한 축제로 다양한 체험 부스와 이벤트 볼더링 경기가 열립니다.",
        "sourceName": "한국관광공사 TourAPI",
        "sourceUrl": "https://korean.visitkorea.or.kr/detail/...",
        "thumbnailUrl": "https://cdn.example.com/events/climbing.jpg",
        "eventStart": "2026-11-01",
        "eventEnd": "2026-11-03",
        "publishedAt": "2026-10-18T10:00:00+09:00",
        "isEnded": false
      }
    ],
    "page": { "number": 0, "size": 20, "totalElements": 15, "totalPages": 1, "hasNext": false }
  },
  "error": null
}
```

---

### 3.7 커뮤니티 게시판 및 댓글 (`/api/v1/posts`, `/api/v1/comments`) `[P1]`

> 게시판·작성·상세·댓글·좋아요·링크 공유(FR-BOARD-01~05)는 모두 P1이다. 아래 1)~7) 전부 `[P1]`.

#### 1) 취미별 게시글 목록 조회
- **Endpoint**: `GET /api/v1/hobbies/{hobbyId}/posts?type={type}&keyword={keyword}&sort={sort}&page=0&size=20`
- **Permission**: `공개`
- **Query Parameter**:
  - `type`: `QUESTION`, `INFO`, `REVIEW`, `SHARE`
  - `sort`: `LATEST`(기본), `LIKES`, `COMMENTS`
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 120,
        "type": "REVIEW",
        "title": "클라이밍 입문 한 달 차의 솔직한 후기",
        "contentSummary": "처음에는 악력도 약하고 힘들었지만 V1을 처음 깼을 때의 쾌감이 대단합니다...",
        "author": { "id": 15, "nickname": "벽타는펭귄", "profileImageUrl": "https://..." },
        "thumbnailUrl": "https://cdn.example.com/uploads/posts/120_1.jpg",
        "viewCount": 240,
        "likeCount": 18,
        "commentCount": 5,
        "createdAt": "2026-10-19T18:20:00+09:00"
      }
    ],
    "page": { "number": 0, "size": 20, "totalElements": 45, "totalPages": 3, "hasNext": true }
  },
  "error": null
}
```

#### 2) 게시글 등록
- **Endpoint**: `POST /api/v1/posts`
- **Permission**: `회원 (USER)`
- **Request Body**:
```json
{
  "hobbyId": 3,
  "type": "QUESTION",
  "title": "초보자용 암벽화 발볼 넓은 제품 추천 부탁드려요",
  "content": "발볼이 넓은 편이라 기본 대여화는 발가락이 너무 아프네요...",
  "imageUrls": [
    "https://api.example.com/uploads/images/uuid1.jpg"
  ]
}
```
- **Response (201 Created)**: 생성된 게시글 정보 및 ID 반환

#### 3) 게시글 상세 조회
- **Endpoint**: `GET /api/v1/posts/{postId}`
- **Permission**: `공개`
- **Response (200 OK)**: 상세 내용, 본문 이미지 전체 URL, 좋아요 여부(`isLiked`), 작성자 본인 여부(`isMine`) 포함

#### 4) 게시글 수정 / 삭제
- **Endpoint**: `PATCH /api/v1/posts/{postId}` (수정, 본인 소유자)
- **Endpoint**: `DELETE /api/v1/posts/{postId}` (삭제, 본인 소유자)
- **Response (200 OK / 204 No Content)**

#### 5) 댓글 목록 조회
- **Endpoint**: `GET /api/v1/posts/{postId}/comments`
- **Permission**: `공개`
- **Response (200 OK)**: 계층형 구조 (부모 댓글 + 하위 `replies` 1단계)

#### 6) 댓글 / 대댓글 작성
- **Endpoint**: `POST /api/v1/posts/{postId}/comments`
- **Permission**: `회원 (USER)`
- **Request Body**:
```json
{
  "content": "스카르파 베이퍼 V 추천드립니다. 볼이 넓게 나왔어요.",
  "parentId": null
}
```
*(대댓글 작성 시 `parentId`에 상위 댓글 ID 전달. 대댓글의 대댓글은 400 `BOARD_REPLY_DEPTH` 에러. 댓글 알림 발행(RabbitMQ `notification.create`)은 `[P2·MVP 제외]`)*
- **Response (201 Created)**

#### 7) 게시글 / 댓글 좋아요 토글
- **Endpoint**: `PUT /api/v1/posts/{postId}/like` / `DELETE /api/v1/posts/{postId}/like`
- **Endpoint**: `PUT /api/v1/comments/{commentId}/like` / `DELETE /api/v1/comments/{commentId}/like`
- **Permission**: `회원 (USER)`
- **Response (200 OK)**: 변경된 총 좋아요 수 반환

---

### 3.8 신고 및 이미지 업로드 (`/api/v1/reports`, `/api/v1/uploads`)

#### 1) 신고 접수 `[P3]`
- **Endpoint**: `POST /api/v1/reports`
- **Permission**: `회원 (USER)`
- **Request Body**:
```json
{
  "targetType": "POST",
  "targetId": 120,
  "reason": "SPAM_OR_AD",
  "detail": "게시글 하단에 불법 도박 사이트 홍보 링크가 삽입되어 있습니다."
}
```
*(targetType: `POST`, `COMMENT`, `SHORT` / reason: `SPAM_OR_AD`, `INAPPROPRIATE`, `OFFENSIVE`, `ETC`)*
- **Response (201 Created)**:
```json
{
  "success": true,
  "data": {
    "reportId": 45,
    "message": "신고가 정상적으로 접수되었습니다. 관리자 검토 후 조치됩니다."
  },
  "error": null
}
```

#### 2) 이미지 업로드 (Multipart) `[P1]`
- **Endpoint**: `POST /api/v1/uploads/images`
- **Permission**: `회원 (USER)`
- **Request**: `multipart/form-data`
  - `file`: 바이너리 파일 (JPG, PNG, WEBP)
  - `purpose`: `PROFILE`(2MB 이하, 프로필 이미지 수정 `[P2·MVP 제외]`), `POST`(5MB 이하, 게시판 `[P1]`), `INQUIRY`(5MB 이하, 고객 문의 `[P3]`)
- **Response (201 Created)**:
```json
{
  "success": true,
  "data": {
    "url": "https://api.example.com/uploads/images/2026/10/a8f192b0-9f12-4c6e.webp",
    "width": 1080,
    "height": 720
  },
  "error": null
}
```

---

### 3.9 알림, 고객센터 및 피드백 (`/api/v1/notifications`, `/api/v1/inquiries`, `/api/v1/notices`, `/api/v1/feedbacks`)

> 웹 알림(FR-NOTI-01)과 서비스 피드백(FR-FB-01·02)은 `[P2·MVP 제외]`, 고객 문의(FR-CS-01)와 서비스 공지(FR-CS-02)는 `[P3]`이다.

#### 1) 내 알림 목록 및 안 읽은 개수 조회 `[P2·MVP 제외]`
- **Endpoint**: `GET /api/v1/notifications?unreadOnly=false&page=0&size=20`
- **Endpoint**: `GET /api/v1/notifications/unread-count`
- **Permission**: `회원 (USER)`
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "unreadCount": 3
  },
  "error": null
}
```

#### 2) 알림 읽음 처리 `[P2·MVP 제외]`
- **Endpoint**: `PATCH /api/v1/notifications/{id}/read` (단건)
- **Endpoint**: `PATCH /api/v1/notifications/read-all` (전체)
- **Response (200 OK)**

#### 3) 1:1 고객 문의 등록 및 내역 조회 `[P3]`
- **Endpoint**: `GET /api/v1/inquiries`
- **Endpoint**: `POST /api/v1/inquiries`
- **Endpoint**: `GET /api/v1/inquiries/{id}`
- **Permission**: `회원 (USER)`
- **Request Body (POST)**:
```json
{
  "category": "ACCOUNT",
  "title": "비밀번호 재설정 메일이 오지 않습니다.",
  "content": "여러 번 요청했는데 스팸함에도 메일이 없습니다."
}
```
- **Response (201 Created)**: 상태 `RECEIVED` 반환

#### 4) 서비스 공지사항 목록 / 상세 `[P3]`
- **Endpoint**: `GET /api/v1/notices`
- **Endpoint**: `GET /api/v1/notices/{id}`
- **Permission**: `공개`
- **Response (200 OK)**: 고정 공지(`pinned: true`) 상단 배치 목록 반환

#### 5) 서비스 피드백 제출 (고도화 비동기 AI 분석) `[P2·MVP 제외]`
> 서비스 피드백은 CSV에서 빠져 MVP 제외다. `feedback.analyze` 큐·Mattermost 피드백 알림 포함, 고도화 후보로만 남긴다.

- **Endpoint**: `POST /api/v1/feedbacks`
- **Permission**: `회원 (USER)`
- **Request Body**:
```json
{
  "category": "BUG",
  "content": "추천 결과 화면에서 뒤로가기를 누르면 이전 탐색 영상이 다시 재생되지 않고 오류가 떠요.",
  "pagePath": "/recommendations/88",
  "clientInfo": {
    "browser": "Chrome 128.0",
    "os": "Windows 11"
  }
}
```
- **Response (202 Accepted)**:
```json
{
  "success": true,
  "data": {
    "feedbackId": 17,
    "status": "RECEIVED"
  },
  "error": null
}
```
*(RabbitMQ `feedback.analyze` 발행 → GMS GPT-4o가 우선순위(URGENT/HIGH 등) 분류 후 Mattermost 웹훅 통지)*

---

### 3.10 관리자 API (`/api/v1/admin/**`)

*모든 관리자 API는 ADMIN 권한(`role = 'ADMIN'`) 필수입니다.*

> **MVP 운영 방식** (관리자 화면 대부분이 P2): 취미 콘텐츠·분류·태그·TODO 원본·데이터 매핑표·신뢰 채널은 **시드 SQL(또는 DB 직접 수정)** 로 넣는다. 숏폼은 관리자 없이도 돈다: P0 수집 배치가 확실한 영상은 `CANDIDATE`, 불확실한 영상은 `NEEDS_REVIEW`로 저장하고, 주간 반영 배치(P0)가 `CANDIDATE`만 `ACTIVE`로 올리므로 관리자가 로그인해 있지 않아도 사용자에게 영상이 나간다. `NEEDS_REVIEW` 영상은 관리자가 승인할 때까지 노출하지 않는다(승인 → `CANDIDATE` → 다음 주간 반영 때 `ACTIVE`, 거절 → `REJECTED`). 신뢰 채널 발굴(P0)은 후보를 `youtube_channels`에 `CANDIDATE`로 저장까지 한다. 숏폼 검수 API(채널 등록·승인, 영상 목록·승인·취미 변경·제외·복구)는 팀 결정(10/21)으로 `[P3]`이고, 그 전에는 채널 승인·자동 숨김 복구를 DB에서 직접 한다. "지금 반영"과 배치 이력 API는 `[P2·MVP 제외]`. 개발 범위의 관리자 API는 P3(신고 검토·문의 답변·공지 관리·숏폼 검수)뿐이다.

| Method | URI | 설명 |
|---|---|---|
| `GET` / `POST` | `/api/v1/admin/hobbies` | `[P2·MVP 제외]` 취미 목록 조회 / 신규 취미 등록 (기본 DRAFT) |
| `GET` / `PUT` | `/api/v1/admin/hobbies/{id}` | `[P2·MVP 제외]` 취미 상세 조회 / 수정 |
| `PATCH` | `/api/v1/admin/hobbies/{id}/status` | `[P2·MVP 제외]` 취미 상태 변경 (`DRAFT` → `PUBLISHED`, `HIDDEN`) |
| `GET` / `PUT` | `/api/v1/admin/hobbies/{id}/source-mappings` | `[P2·MVP 제외]` 취미별 외부 검색 키워드 매핑 조회 / 갱신 |
| `GET` / `POST` | `/api/v1/admin/plan-templates?hobbyId={id}` | `[P2·MVP 제외]` TODO 원본 템플릿 목록 조회 / 신규 등록 |
| `PUT` | `/api/v1/admin/plan-templates/{id}` | `[P2·MVP 제외]` TODO 원본 전체 주차 및 활동 수정 (버전 +1) |
| `GET` / `POST` | `/api/v1/admin/channels?hobbyId=&status=` | `[P3]` 신뢰 유튜브 채널 목록 조회 / 신규 채널 등록 |
| `PATCH` | `/api/v1/admin/channels/{id}` | `[P3]` 신뢰 채널 상태 변경 (`CANDIDATE` 승인/거절, 수집 일시중지) |
| `GET` | `/api/v1/admin/shorts?status=&hobbyId=` | `[P3]` 영상 목록 (`CANDIDATE`, `NEEDS_REVIEW`, `AUTO_HIDDEN`, `UNAVAILABLE`) |
| `PATCH` | `/api/v1/admin/shorts/{id}` | `[P3]` 영상 취미 수동 지정, 승인(`NEEDS_REVIEW` → `CANDIDATE`, 다음 주간 반영 때 `ACTIVE`), 영구 제외(`REJECTED`), 복구 |
| `GET` / `POST` | `/api/v1/admin/shorts/release` | `[P2·MVP 제외]` 주간 반영 예정 건수 조회 / 즉시 수동 릴리즈 실행 ("지금 반영"). 주간 반영 배치 자체는 P0 스케줄로 동작 |
| `GET` | `/api/v1/admin/reports?status=&targetType=` | `[P3]` 신고 목록 대상별 그룹 조회 |
| `POST` | `/api/v1/admin/reports/targets/{targetType}/{targetId}/actions` | `[P3]` 신고 대상 일괄 조치 (`HIDE`, `RESTORE`, `DISMISS`) (신고 처리 알림은 `[P2·MVP 제외]`) |
| `GET` / `PUT` | `/api/v1/admin/inquiries/{id}/answer` | `[P3]` 문의 조회 및 답변 저장 (답변 알림 자동 발행은 `[P2·MVP 제외]`) |
| `GET` | `/api/v1/admin/batch-runs?job=` | `[P2·MVP 제외]` 스케줄 배치 실행 이력 및 외부 API 쿼터 사용량 확인 (MVP는 `batch_runs` 테이블·로그 직접 확인) |
| `GET` | `/api/v1/admin/feedbacks?priority=&status=` | `[P2·MVP 제외]` 사용자 피드백 분석 결과 목록 조회 |

---

## 4. 백엔드 구현 체크리스트 (Spring Boot)

1. **SYS-B02**: 공통 응답 클래스 `ApiResponse<T>`, 에러 DTO `ErrorResponse`, `@RestControllerAdvice` 구현
2. **SYS-B03 / B04**: Spring Security JWT Filter, HttpOnly Refresh 쿠키 파서, `@CurrentUserId` 커스텀 리졸버 (`GuestKeyInterceptor`·`@CurrentGuestKey`는 비회원 체험 `[P2·MVP 제외]`라 구현하지 않음 — MVP의 탐색·추천 API는 `USER` 권한 필수)
3. **Validation**: `@Valid`, `@NotBlank`, `@Size`, `@Min`, `@Max` 선언 및 `MethodArgumentNotValidException` 필드 매핑
4. **Swagger (springdoc)**: 모든 Controller와 DTO에 `@Tag`, `@Operation`, `@Schema` 어노테이션 적용하여 `/swagger-ui/index.html` 자동 문서화 동기화
5. **우선순위별 구현 범위** (개발 순서 P0 → P1 → P3, P2는 구현하지 않음)
   - **P0 (11/04 마감)**: 이메일 회원가입·인증·로그인·로그아웃·토큰 재발급·비밀번호 재설정(3.1), 회원 탈퇴 `DELETE /users/me`, 내 정보·마이페이지 요약(3.2 1)·5)), 숏폼 탐색 세션·다음 영상(무작위)·시청 기록·반응·진행 현황·종료(3.3, 보관 제외), 취미 검색·상세·추천 생성/조회·별로예요(3.4, 관련 숏폼·미리보기·직접 선택 제외), 관심 저장·시작하기(동시 진행 3개)·일시중지/재개/그만두기·주차/전체 계획/홈 TODO 요약·완료/건너뛰기·메모·내 계획 편집(3.5), 설정값 `exploration.session-size`(기본안 12)
   - **P1 (11/09~11/12)**: Google 소셜 로그인(`/oauth2/**`), 비밀번호 변경, 온보딩·이용 상황 수정(3.2 3)·4)), 다음 영상 분류 분산·선호 70%/탐색 30%, 추천 기준 문구·AI 추천 이유·이용 상황 정렬·탐색용 제안, 취미 상세 관련 숏폼·TODO 미리보기, 밀린 활동·TODO 메모, 지역·장소·정보 피드(3.6), 게시판·댓글·좋아요(3.7), 이미지 업로드(3.8 2)), 탈퇴 시 개인정보 익명화
   - **P3 (여유 시)**: 신고(3.8 1)), 고객 문의·공지(3.9 3)·4)), 관리자 신고 검토·문의 답변·공지 관리·숏폼 검수(채널·영상 승인·복구)(3.10 `[P3]` 행), 요청량 제한(Rate Limit)
   - **P2 (MVP 제외)**: 비회원 체험(`guest_key`, `/explorations/merge`), 프로필 닉네임·이미지 수정, 숏폼 보관·보관함, 관심사 직접 선택(`DIRECT_SELECTION`), 전체 계획 자동 COMPLETED 전환·축하, 종료 취미 재시작·지난 취미 UI, 알림 전체(`/notifications/**`, `notification.create`), 서비스 피드백(`/feedbacks`, `feedback.analyze`), 관리자 콘텐츠·수동 반영(지금 반영)·배치 이력·피드백 API(3.10 `[P2·MVP 제외]` 행)
