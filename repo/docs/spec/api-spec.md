# 냠냠취미 REST API 상세 명세서 (v0.4)

> **기준 문서**: 프로젝트 명세서 v0.4 ([project-spec-v0.3.md](./project-spec-v0.3.md), 파일 이름 유지) · 기능 요구사항 CSV(10/21) 반영 ([feature-requirements.csv](./feature-requirements.csv)) · DB 스키마 (PostgreSQL 17 + pgvector, 이미지 `pgvector/pgvector:pg17`; 확장·임베딩 스키마는 후속 마이그레이션, [V1__init.sql](../db/V1__init.sql))
> **최종 수정일**: 2026-10-22  
> **작성자**: 김재현  
> **단일 진실 공급원(SSOT)**: 런타임 상세 스펙은 Spring Boot 서버의 Swagger UI (`/swagger-ui/index.html`)를 기준으로 하며, 본 문서는 프론트엔드-백엔드 간 계약 및 기능 구현을 위한 표준 상세 명세서입니다.

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
| **409** | `SHORT_SESSION_ENDED` | 이미 완료되었거나 만료된 숏폼 탐색 세션 |
| **409** | `SHORT_NOT_ACTIVE` | 비공개, 삭제, 자동 숨김 처리된 숏폼에 반응 시도 |
| **409** | `MY_HOBBY_LIMIT` | 동시 진행 중인 취미가 최대 허용 수치(5개) 초과 [P2 도입 시 일시중지는 제외] |
| **409** | `MY_PLAN_NOT_READY` | 해당 취미/숙련도에 공개된 TODO 원본 템플릿 부재 |
| **409** | `MY_HOBBY_IN_PROGRESS` | 진행 중(`IN_PROGRESS`)인 취미는 관심 해제 불가 |
| **409** | `TODO_PLAN_NOT_ACTIVE` | `[P2·MVP 제외]` 일시중지(`PAUSED`) 또는 종료(`STOPPED`)된 취미의 TODO 수정 시도 (취미 상태 변경이 P2라 MVP에서는 발생하지 않음) |
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
      "hobbyId": 3,
      "skillLevel": "BEGINNER"
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
*(`maxCount` = 설정값 N(기본안 12). `isSaved`는 숏폼 보관 `[P2·MVP 제외]` 도입 전까지 항상 `false`. 비회원 첫 호출 시 Set-Cookie `guest_key` 발급은 `[P2·MVP 제외]`)*

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
*(P0 선택 규칙: `ACTIVE` 영상 중 이미 본 영상·관련 없음·숨김 영상을 제외하고 무작위. 첫 진입 분류별 분산과 선호 70%·탐색 30%는 `[P1]` 개선)*

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

#### 6) 숏폼 영상 보관 / 보관 해제 `[P2·MVP 제외]`
- **Endpoint**: `PUT /api/v1/shorts/{shortId}/save` (보관)
- **Endpoint**: `DELETE /api/v1/shorts/{shortId}/save` (해제)
- **Permission**: `회원 (USER)`
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

#### 7) 보관한 숏폼 목록 조회 `[P2·MVP 제외]`
- **Endpoint**: `GET /api/v1/users/me/saved-shorts?hobbyId={hobbyId}&page=0&size=20`
- **Permission**: `회원 (USER)`
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "saveId": 10,
        "shortId": 41,
        "youtubeVideoId": "aBcD123xYz0",
        "title": "퇴근 후 1시간, 볼더링 첫 도전기",
        "hobby": { "id": 3, "name": "클라이밍" },
        "status": "ACTIVE",
        "savedAt": "2026-10-20T14:20:00+09:00"
      }
    ],
    "page": { "number": 0, "size": 20, "totalElements": 1, "totalPages": 1, "hasNext": false }
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

#### 9) 탐색 종료 및 추천 생성
- **Endpoint**: `POST /api/v1/explorations/{sessionId}/finish`
- **Permission**: `회원 (USER)` (비회원 `GUEST` 체험은 `[P2·MVP 제외]`)
- **Response (201 Created)**: (추천 결과 반환 - 3.4절 `POST /recommendations` 응답과 동일)

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
*(myStatus: `NONE`, `SAVED`, `IN_PROGRESS`, `PAUSED`, `COMPLETED`, `STOPPED` — `PAUSED`·`COMPLETED`·`STOPPED`는 취미 상태 변경(FR-MY-03) `[P2·MVP 제외]` 도입 시에만 나온다)*

#### 3) 취미 상세 관련 숏폼 목록 `[P1]`
- **Endpoint**: `GET /api/v1/hobbies/{hobbyId}/shorts?limit=6`
- **Permission**: `공개`
- **Response (200 OK)**: 최대 6개의 추천 숏폼 카드 목록

#### 4) 취미 TODO 플랜 미리보기 `[P1]`
- **Endpoint**: `GET /api/v1/hobbies/{hobbyId}/plan-preview?skillLevel=BEGINNER`
- **Permission**: `공개`
- **Response (200 OK)**: 1~2주차 활동 예시 데이터

#### 5) 추천 생성 (수동 또는 직접 선택 기반)
- **Endpoint**: `POST /api/v1/recommendations`
- **Permission**: `회원 (USER)` (비회원 `GUEST` 체험은 `[P2·MVP 제외]`)
- **Request Body**:
```json
{
  "basis": "DIRECT_SELECTION",
  "sessionId": null,
  "hobbyIds": [3, 7, 12]
}
```
*(basis: `EXPLORATION_SESSION`, `DIRECT_SELECTION` — `DIRECT_SELECTION`(관심사 직접 선택, FR-SHORT-05)은 `[P2·MVP 제외]`. MVP는 탐색 세션 기반만 사용)*  
*(정보 부족(LIKE 3개 미만) 시 `isExploratory: true` 탐색용 제안은 `[P1]`. 이용 상황 반영 정렬·`conditionWarning`은 `[P1]`. `basisText`(추천 기준 문구)와 AI 추천 이유 `reason`(60자)은 `[P1]` — AI 이유 도입 전에는 `fallbackReason`을 표시)*
- **Response (201 Created)**:
```json
{
  "success": true,
  "data": {
    "recommendationId": 88,
    "isExploratory": false,
    "basisText": "선택하신 관심 취미(클라이밍, 도예)와 이용 상황을 바탕으로 추천했어요",
    "items": [
      {
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
- **Response (200 OK)**:
  - RabbitMQ 소비자가 GMS 추천 사유 생성을 마치면 `reasonStatus: "COMPLETED"`, `reason: "평소 활동적인 실내 운동을 선호하시고 주 3시간 여유에 적합한 취미예요"`로 응답.

#### 7) 취미 별로예요 (추천 제외) 등록 / 취소
- **Endpoint**: `PUT /api/v1/hobbies/{hobbyId}/dislike`
- **Endpoint**: `DELETE /api/v1/hobbies/{hobbyId}/dislike`
- **Permission**: `회원 (USER)`
- **Response (200 OK)**: 등록 시 향후 추천 및 연관 취미에서 영구 제외

---

### 3.5 내 취미 및 주차별 TODO (`/api/v1/users/me/hobbies`, `/api/v1/plans`)

#### 1) 내 취미 목록 조회
- **Endpoint**: `GET /api/v1/users/me/hobbies?status={status}`
- **Permission**: `회원 (USER)`
- **Query Parameter**: `status` (`SAVED`, `IN_PROGRESS`, `PAUSED`, `COMPLETED`, `STOPPED` — `PAUSED`·`COMPLETED`·`STOPPED`는 `[P2·MVP 제외]`)
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": [
    {
      "userHobbyId": 12,
      "hobbyId": 3,
      "name": "클라이밍",
      "thumbnailUrl": "https://cdn.example.com/hobbies/3.jpg",
      "status": "IN_PROGRESS",
      "skillLevel": "BEGINNER",
      "planId": 55,
      "progressRate": 0.375,
      "startedAt": "2026-10-15T09:00:00+09:00"
    }
  ],
  "error": null
}
```

#### 2) 관심 취미 저장 / 해제
- **Endpoint**: `PUT /api/v1/users/me/hobbies/{hobbyId}/save` (저장)
- **Endpoint**: `DELETE /api/v1/users/me/hobbies/{hobbyId}/save` (해제)
- **Permission**: `회원 (USER)`
- **Response (200 OK)**

#### 3) 취미 시작하기 (TODO 인스턴스 생성)
- **Endpoint**: `POST /api/v1/users/me/hobbies/{hobbyId}/start`
- **Permission**: `회원 (USER)`
- **Request Body**:
```json
{
  "skillLevel": "BEGINNER"
}
```
- **Response (201 Created)**:
```json
{
  "success": true,
  "data": {
    "userHobbyId": 12,
    "planId": 55,
    "status": "IN_PROGRESS",
    "title": "클라이밍 · 입문 8주 계획"
  },
  "error": null
}
```
*(동시 진행 5개 초과 시 409 `MY_HOBBY_LIMIT` [P2 도입 시 일시중지는 제외], 원본 템플릿 부재 시 409 `MY_PLAN_NOT_READY`)*

#### 4) 취미 상태 변경 (일시중지 / 재개 / 그만두기) `[P2·MVP 제외]`
> 취미 상태 변경(FR-MY-03: 일시중지·다시 시작, 그만두기, 완료 처리·축하)은 P2다. `COMPLETED` 자동 전환도 P2.

- **Endpoint**: `PATCH /api/v1/users/me/hobbies/{hobbyId}/status`
- **Permission**: `회원 (USER)`
- **Request Body**:
```json
{
  "status": "PAUSED"
}
```
*(status: `IN_PROGRESS`, `PAUSED`, `STOPPED`)*
- **Response (200 OK)**

#### 5) 주차별 TODO 계획 조회
- **Endpoint**: `GET /api/v1/plans/{planId}?week={weekNo}`
- **Permission**: `본인 소유자`
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "planId": 55,
    "title": "클라이밍 · 입문 8주 계획",
    "hobbyStatus": "IN_PROGRESS",
    "totalWeeks": 8,
    "currentWeek": 2,
    "progress": {
      "done": 5,
      "skipped": 1,
      "total": 16,
      "rate": 0.3125
    },
    "weeks": [
      { "weekNo": 1, "overdueCount": 0 },
      { "weekNo": 2, "overdueCount": 0 },
      { "weekNo": 3, "overdueCount": 0 }
    ],
    "items": [
      {
        "itemId": 901,
        "weekNo": 2,
        "seq": 1,
        "title": "기본 홀드 잡기 연습",
        "description": "오픈 그립과 클림프 그립의 차이를 이해하고 실습합니다.",
        "expectedMinutes": 40,
        "supplies": "암벽화, 초크",
        "doneCriteria": "기초 밸런스로 V0 문제 3개 완등",
        "status": "TODO",
        "memo": null,
        "isCustom": false,
        "completedAt": null
      }
    ]
  },
  "error": null
}
```
*(`weeks[].overdueCount`(밀린 활동 표시)는 `[P1]`. `hobbyStatus`의 `PAUSED`·`STOPPED`·`COMPLETED`는 `[P2·MVP 제외]`)*

#### 6) TODO 활동 상태 / 메모 변경
- **Endpoint**: `PATCH /api/v1/plans/{planId}/items/{itemId}`
- **Permission**: `본인 소유자`
- **Request Body**:
```json
{
  "status": "DONE",
  "memo": "오른손 그립 시 엄지 손가락 지지 연습 완료"
}
```
*(status: `TODO`, `DONE`, `SKIPPED` — 완료·건너뛰기·취소는 P0, `memo`(500자)는 `[P1]`. 일시중지·종료 취미의 수정 거부(409 `TODO_PLAN_NOT_ACTIVE`)는 `[P2·MVP 제외]`. `planCompleted`에 따른 `COMPLETED` 자동 전환·축하는 `[P2·MVP 제외]`)*
- **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "itemId": 901,
    "status": "DONE",
    "memo": "오른손 그립 시 엄지 손가락 지지 연습 완료",
    "completedAt": "2026-10-20T14:30:00+09:00",
    "progress": {
      "done": 6,
      "skipped": 1,
      "total": 16,
      "rate": 0.375
    },
    "planCompleted": false
  },
  "error": null
}
```

#### 7) 사용자 정의 TODO 활동 추가 `[P2·MVP 제외]`
> 내 계획 수정(FR-TODO-03: 활동 추가·삭제, 주차·시간 변경)은 P2다.

- **Endpoint**: `POST /api/v1/plans/{planId}/items`
- **Permission**: `본인 소유자`
- **Request Body**:
```json
{
  "weekNo": 2,
  "title": "보조 악력기 트레이닝 15분",
  "expectedMinutes": 15,
  "supplies": "악력기",
  "doneCriteria": "양손 3세트 완수"
}
```
- **Response (201 Created)**

#### 8) TODO 활동 삭제 (Soft Delete) `[P2·MVP 제외]`
- **Endpoint**: `DELETE /api/v1/plans/{planId}/items/{itemId}`
- **Permission**: `본인 소유자`
- **Response (204 No Content)**

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
   - **P0 (11/04 마감)**: 이메일 회원가입·인증·로그인·로그아웃·토큰 재발급·비밀번호 재설정(3.1), 회원 탈퇴 `DELETE /users/me`, 내 정보·마이페이지 요약(3.2 1)·5)), 숏폼 탐색 세션·다음 영상(무작위)·시청 기록·반응·진행 현황·종료(3.3, 보관 제외), 취미 검색·상세·추천 생성/조회·별로예요(3.4, 관련 숏폼·미리보기·직접 선택 제외), 관심 저장·시작하기(동시 진행 5개)·주차별 TODO 조회·완료/건너뛰기(3.5 1)~3)·5)·6)), 설정값 `exploration.session-size`(기본안 12)
   - **P1 (11/09~11/12)**: Google 소셜 로그인(`/oauth2/**`), 비밀번호 변경, 온보딩·이용 상황 수정(3.2 3)·4)), 다음 영상 분류 분산·선호 70%/탐색 30%, 추천 기준 문구·AI 추천 이유·이용 상황 정렬·탐색용 제안, 취미 상세 관련 숏폼·TODO 미리보기, 밀린 활동·TODO 메모, 지역·장소·정보 피드(3.6), 게시판·댓글·좋아요(3.7), 이미지 업로드(3.8 2)), 탈퇴 시 개인정보 익명화
   - **P3 (여유 시)**: 신고(3.8 1)), 고객 문의·공지(3.9 3)·4)), 관리자 신고 검토·문의 답변·공지 관리·숏폼 검수(채널·영상 승인·복구)(3.10 `[P3]` 행), 요청량 제한(Rate Limit)
   - **P2 (MVP 제외)**: 비회원 체험(`guest_key`, `/explorations/merge`), 프로필 닉네임·이미지 수정, 숏폼 보관·보관함, 관심사 직접 선택(`DIRECT_SELECTION`), 취미 상태 변경(`PAUSED`·`STOPPED`·`COMPLETED`), 내 계획 수정(TODO 추가·삭제), 알림 전체(`/notifications/**`, `notification.create`), 서비스 피드백(`/feedbacks`, `feedback.analyze`), 관리자 콘텐츠·수동 반영(지금 반영)·배치 이력·피드백 API(3.10 `[P2·MVP 제외]` 행)
