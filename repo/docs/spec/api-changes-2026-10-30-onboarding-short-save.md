# 2026-10-30 API 변경: 온보딩 선택값 · 숏폼 보관

대상: [현재 Figma 와이어프레임 v2](https://www.figma.com/design/utbzAGrX0kTGuicP8M0ry2?node-id=4-2)의 **01-6A 관심 취미 설정 → 01-6B 이용 상황 설정**, **02-1 숏폼 재생의 보관 버튼**.

이번 요청에 따라 기존 우선순위 문서에서 P2로 제외했던 **숏폼 보관·해제만 구현 범위에 포함**했다. 보관함 목록, 다른 화면·버튼, 비회원 체험은 이번 변경 범위에 포함하지 않는다. 프론트 화면 연결 코드는 별도 작업이며, 아래는 연결할 수 있도록 구현한 백엔드 계약이다.

## 점검 결과와 변경점

| 항목 | 기존 명세 / 구현 | 이번 변경 |
|---|---|---|
| 온보딩 일괄 저장 | 명세에 `PUT /users/me/onboarding`만 있고 구현 없음 | 관심 취미와 이용 상황을 하나의 트랜잭션으로 저장 |
| 관심 취미 | `initialHobbies[].skillLevel` 예시 포함, 화면에는 숙련도 입력 없음 | `hobbyId`만 전송. 0~5개 허용, 새 관심 취미의 내부 숙련도는 `BEGINNER` |
| 이용 상황 | `/users/me/conditions`에 시간·예산·선호·지역 ID 지원 | 기존 규격 재사용. 전 항목 선택이며 미입력은 생략 또는 `null` |
| 선택지의 실제 ID | 온보딩에서 사용할 취미·지역 조회 구현 없음 | `GET /users/me/onboarding/options` 추가 |
| 숏폼 보관·해제 | 명세는 P2 제외, V1에 `short_saves` 테이블만 있음 | `PUT`, `DELETE /shorts/{shortId}/save` 구현 |
| 영상별 보관 여부 | 탐색 첫 영상·다음 영상의 `isSaved`가 항상 `false` | 로그인한 사용자의 실제 보관 상태 조회 |

모든 경로의 공통 접두사는 `/api/v1`이며, 아래 API는 `Authorization: Bearer <accessToken>`이 필요하다. 인증 실패는 `401 COMMON_UNAUTHORIZED`. DB 스키마 변경은 없으며 기존 V1 테이블을 사용한다.

## 1. 온보딩 선택지 조회

`GET /api/v1/users/me/onboarding/options`

화면의 취미·지역 표시 이름을 아래 응답과 연결한다. 취미는 공개 상태(`PUBLISHED`)만 반환한다. `categoryId`별로 취미를 묶고, 지역은 `sido`별로 묶어 `sigungu`를 선택한다. 표시 이름·프론트 배열 순번·행정구역 `code`를 DB ID로 보내지 않는다.

```json
{
  "success": true,
  "data": {
    "hobbies": [
      { "hobbyId": 3, "name": "클라이밍", "categoryId": 1, "categoryName": "운동·야외" }
    ],
    "regions": [
      { "id": 10, "code": "11440", "sido": "서울특별시", "sigungu": "마포구" }
    ]
  },
  "error": null
}
```

ID·명칭은 설명용 예시다. 실제 응답값을 사용해야 하며, 선택지는 DB에 등록된 취미·지역에 한정된다.

**지역 데이터 확인 필요**: 현재 레포의 Flyway 기본 시드에는 `regions` 데이터 INSERT가 없다. 배포 DB에도 지역이 없다면 `regions: []`가 반환된다. 지역 선택을 제공하려면 운영에서 사용하는 지역 데이터를 먼저 등록해야 한다. 임의 ID를 전송하면 `REGION_NOT_FOUND`가 발생한다.

## 2. 01-6A → 01-6B 선택값 저장

`PUT /api/v1/users/me/onboarding`

01-6A의 **다음**에서는 선택한 ID를 프론트 상태로 유지하고, 01-6B의 **저장하고 숏폼 탐색 시작**에서 아래 본문을 한 번 전송한다. 저장 성공 후 기존 `POST /api/v1/explorations`를 호출해 숏폼 탐색을 시작한다. 온보딩 저장 자체는 탐색 세션을 만들지 않는다.

```json
{
  "initialHobbies": [{ "hobbyId": 3 }, { "hobbyId": 4 }],
  "weeklyHours": 3,
  "monthlyBudget": 150000,
  "placePref": "INDOOR",
  "socialPref": "ANY",
  "regionId": 10
}
```

| 필드 | JSON 타입 / 허용값 | 화면 대응 |
|---|---|---|
| `initialHobbies` | 객체 배열, 최대 5개. 생략·`null`·`[]` = 선택 없음 | 01-6A 관심 취미 |
| `initialHobbies[].hobbyId` | 필수 양의 정수. 중복 불가, 공개 취미만 허용 | 선택지의 `hobbyId` |
| `weeklyHours` | 정수 0~40 또는 `null` | 활동 가능 시간 **시간/주** |
| `monthlyBudget` | 정수 0~2,147,483,647 또는 `null` | 예산 **원/월** |
| `placePref` | `INDOOR`, `OUTDOOR`, `ANY` 또는 `null` | 실내, 실외, 상관없음 |
| `socialPref` | `ALONE`, `TOGETHER`, `ANY` 또는 `null` | 혼자, 함께, 상관없음 |
| `regionId` | 양의 정수 또는 `null` | 시·도 + 시·군·구에 해당하는 지역의 `id` |

프론트 입력 변환:

- 시간·예산 입력이 빈 문자열이면 `null`로 보낸다. 예산의 표시용 쉼표를 제거하고 JSON 숫자로 전송한다. `0`은 미입력이 아니라 실제 조건이다.
- 선택하지 않은 장소·동행 선호는 `null`, **상관없음**을 누른 경우는 `ANY`다. 빈 문자열 `""`은 허용하지 않는다.
- 지역을 선택하지 않으면 `regionId: null`. 시·도와 시·군·구 문자열이나 GPS 좌표는 보내지 않는다. 시·도만 입력된 상태라면 시·군·구까지 선택하도록 안내하거나 지역 조건을 비운다.
- `skillLevel`은 이 화면에서 수집·전송하지 않는다. 관심 취미 저장은 실제 취미 시작·TODO 생성과 별개다.
- 두 화면을 모두 건너뛴 경우 `{}`도 허용한다. 빈 이용 상황 행을 저장하므로 이후 로그인 응답의 `user.onboarded`는 `true`가 된다.

응답: `200 OK`

```json
{
  "success": true,
  "data": { "onboarded": true, "savedHobbyCount": 2 },
  "error": null
}
```

저장 규칙:

- PUT은 온보딩 관심 목록과 이용 상황 전체를 교체한다. 미전송·`null`인 이용 상황 필드는 비워진다. 관심 취미가 빈 배열이면 기존 `SAVED` 관심 취미를 해제한다.
- 이미 저장한 취미는 중복 생성하지 않는다. 기존 진행 중·일시중지 등 `SAVED` 이외의 취미와 숙련도·진행 이력은 유지한다.
- `savedHobbyCount`는 반영 후 `SAVED` 상태인 관심 취미 수다. 이미 진행 중인 취미는 이 수에 포함하지 않는다.
- 잘못된 취미·지역이 있으면 두 화면 모두 저장하지 않는다. 같은 요청을 재시도해도 관심 취미 행·관심 저장 수가 중복 증가하지 않는다.

| 오류 | 조건 |
|---|---|
| `400 COMMON_INVALID_INPUT` | 5개 초과, 중복 취미 ID, null 배열 항목, 취미 ID 누락·0 이하, 시간·예산 범위 초과, 잘못된 선호값, 지역 ID 0 이하 |
| `404 HOBBY_NOT_FOUND` | 존재하지 않거나 공개되지 않은 취미 |
| `404 REGION_NOT_FOUND` | 존재하지 않는 지역 |

기존 `PUT /users/me/conditions`는 유지한다. 이 API는 01-6A 관심 취미까지 저장하지 않으므로 가입 온보딩에서는 `/onboarding`을 사용한다.

## 3. 02-1 숏폼 보관 버튼

| 버튼 상태 / 동작 | 요청 (본문 없음) | 응답 `data` |
|---|---|---|
| 미보관 → 보관 | `PUT /api/v1/shorts/{shortId}/save` | `{ "shortId": 41, "isSaved": true }` |
| 보관 → 해제 | `DELETE /api/v1/shorts/{shortId}/save` | `{ "shortId": 41, "isSaved": false }` |

둘 다 `200 OK`이며 공통 응답 봉투(`success`, `data`, `error`)를 사용한다.

- `shortId`는 탐색 응답의 `data.short.shortId`. YouTube 영상 ID가 아니다.
- 탐색 시작(`POST /explorations`)과 다음 영상(`GET /explorations/{sessionId}/next`) 응답의 `data.short.isSaved`로 버튼 초기 상태를 표시한다.
- 보관 API에는 `sessionId`, `reaction`, `userId`가 필요 없다. 사용자 ID는 로그인 정보에서 결정한다.
- 보관은 **좋아요와 독립적**이다. 좋아요/별로예요 반응, 추천 점수, 시청·탐색 진행 수, 다음 영상 전환을 변경하지 않는다.
- 요청 성공 응답의 `isSaved`로 버튼을 갱신한다. 중복 PUT은 보관 행을 추가하거나 최초 보관 시각을 갱신하지 않는다. 중복 DELETE도 성공한다.
- 보관은 `ACTIVE` 영상만 가능하다. 숨김·이용 불가로 바뀐 영상도 기존 보관은 해제할 수 있다.
- 없는 영상은 `404 COMMON_NOT_FOUND`, 비활성 영상 보관은 `409 SHORT_NOT_ACTIVE`다.
- `GET /users/me/saved-shorts`(보관함 목록)은 이번 구현에 포함하지 않는다. 현재 프론트의 임시 보관 상태를 위 API에 연결하는 작업은 별도다.

## 검증 범위

컨트롤러·서비스 테스트로 요청 역직렬화, 최대 5개·선택 입력 검증, 인증 요구, 두 화면 저장값, 기존 진행 상태 보존, 중복·잘못된 ID 거부, 보관/해제 JSON 계약, 비활성 영상 정책, 첫 영상·다음 영상의 `isSaved`와 저장소 조회 연동을 검증한다.

2026-10-30 실행 결과: 아래 범위의 **145개 테스트 통과**(실패 0, 건너뜀 0). `git diff --check`도 통과했다.

실제 PostgreSQL 통합 테스트는 로컬 DB·Docker가 실행되지 않은 환경이어서 수행하지 않았다. 따라서 실제 DB 저장·동시 요청과 운영 DB의 지역 데이터 보유 여부는 배포 환경에서 추가 확인해야 한다.

재실행 명령 (`BE` 디렉터리의 PowerShell):

```powershell
.\gradlew.bat test --tests 'com.ssafy.nyamnyam.domain.user.*' --tests 'com.ssafy.nyamnyam.domain.exploration.service.*' --tests 'com.ssafy.nyamnyam.domain.exploration.ExplorationControllerTest' --tests 'com.ssafy.nyamnyam.domain.shortform.service.ShortSaveServiceTest'
```
