# 2026-10-30 취미 검색·상세·플랜 미리보기 API 프론트 연결 안내

기준: Figma `05 · 취미 검색 · 추천 · 상세 · 마이페이지`(04-1, 04-2, 04-4, 04-4B, 04-5). 정식 계약은 [api-spec.md 3.4](api-spec.md). 모든 경로의 접두사는 `/api/v1`이고, 응답은 공통 봉투 `{ success, data, error }`다.

현재 [HobbyPages.tsx](../../../FE/src/pages/hobbies/HobbyPages.tsx)의 검색 카드·상세·상세 가이드는 클라이밍 고정 문구다. 아래 API로 바꾼다. 백엔드는 운영 서버에 배포되어 있다.

## 구현 범위

| API | 로그인 | 화면 |
|---|---|---|
| GET /hobbies | 선택 | 04-1 취미 찾기, 04-2 결과 없음 |
| GET /hobbies/{hobbyId} | 선택 | 04-4 상세 상단, 04-4B 상세 가이드(취미 알아보기·시작 전 준비) |
| GET /hobbies/{hobbyId}/plan-preview | 불필요 | 04-4B 활동 계획, 04-5 시작 모달의 숙련도별 준비 여부 |
| GET /hobbies/{hobbyId}/shorts?limit=3 | 불필요 | 04-4B 콘텐츠·장소의 관련 숏폼 (기존 API) |

로그인 상태면 `Authorization` 헤더를 그대로 보낸다. 검색 정렬 기본값과 상세 `myStatus`가 로그인 사용자 기준으로 바뀐다.

## 1. 취미 검색 `GET /hobbies`

쿼리(모두 선택). 화면의 필터 칩 값을 그대로 쿼리에 넣으면 된다(뒤로가기 복원용으로 URL 쿼리와 맞춘다).

| 파라미터 | 값 | 화면 |
|---|---|---|
| keyword | 2자 이상. 취미명·태그 부분 일치. 빈 값이면 전체 | 검색어 |
| time | `1H_LESS` · `1H_TO_2H` · `2H_MORE` | 시간 |
| cost | `30K_LESS` · `30K_TO_100K` · `100K_MORE` (월 비용) | 비용 |
| difficulty | `EASY` · `NORMAL` · `HARD` | 난이도 |
| place | `INDOOR` · `OUTDOOR` · `ANY` | 실내/실외 |
| social | `ALONE` · `TOGETHER` · `ANY` | 혼자/함께 |
| sort | `RECOMMEND` · `POPULAR` · `NAME` · `COST_ASC` | 정렬 |
| page, size | 0부터, 기본 20(최대 100) | 페이지네이션 |

- 정렬 생략 시 로그인은 `RECOMMEND`(숏폼 반응 기반), 비회원은 `POPULAR`.
- 시간·비용은 취미의 범위가 구간과 **겹치면** 포함한다. `INDOOR`/`ALONE` 등은 `BOTH` 취미도 포함한다.
- 1자 검색어나 목록에 없는 값은 `400 COMMON_INVALID_INPUT`. 칩 선택지만 보내면 생기지 않는다.
- 디자인의 "비용 · 월 5만 원 이하"는 API 구간과 다르다. 칩 문구를 위 3구간(~3만 / 3~10만 / 10만 이상)에 맞춘다.

응답 `data`:
```json
{
  "content": [
    {
      "id": 2, "name": "러닝",
      "category": { "id": 1, "name": "운동" },
      "summary": "신발 한 켤레로 시작하는 일상 달리기",
      "thumbnailUrl": null,
      "difficulty": "EASY", "placeType": "OUTDOOR", "socialType": "ALONE",
      "sessionMinutes": { "min": 30, "max": 60 },
      "monthlyCost": { "min": 0, "max": 30000 },
      "saveCount": 12,
      "tags": ["실외", "혼자", "활동적", "스포츠", "체력·건강"]
    }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 24, "totalPages": 2, "hasNext": true }
}
```
- "총 24개"는 `page.totalElements`. `content`가 빈 배열이면 04-2(결과 없음)를 보여 준다.
- 카드의 "관심 저장/저장됨"은 검색 응답에 없다. 로그인 시 `GET /users/me/hobbies`의 `hobbyId`로 표시하고, 버튼은 `PUT/DELETE /users/me/hobbies/{hobbyId}/save`(3.5)를 쓴다.
- `thumbnailUrl`은 아직 모든 취미가 `null`이다. 기본 이미지를 보여 준다.

## 2. 취미 상세 `GET /hobbies/{hobbyId}`

응답 `data`:
```json
{
  "id": 2, "name": "러닝",
  "category": { "id": 1, "name": "운동" },
  "summary": "신발 한 켤레로 시작하는 일상 달리기",
  "description": "러닝은 운동화 한 켤레만 있으면 ...",
  "thumbnailUrl": null,
  "placeType": "OUTDOOR", "socialType": "ALONE", "difficulty": "EASY",
  "sessionMinutes": { "min": 30, "max": 60 },
  "initialCost": { "min": 0, "max": 50000 },
  "monthlyCost": { "min": 0, "max": 30000 },
  "supplies": "러닝화, 통풍이 잘 되는 운동복, 물, 스마트폰(거리 기록용)",
  "environment": "동네 공원이나 하천변 산책로처럼 ...",
  "firstActivity": "집 근처 평평한 길에서 1분 뛰고 2분 걷기를 ...",
  "cautions": "처음부터 오래 달리면 무릎과 정강이에 ...",
  "tags": ["실외", "혼자", "활동적", "스포츠", "체력·건강"],
  "saveCount": 12,
  "myStatus": "NONE"
}
```
- 없거나 비공개 취미는 `404 COMMON_NOT_FOUND`.
- `myStatus`: `NONE`(비회원 포함) · `SAVED` · `IN_PROGRESS` · `PAUSED` · `STOPPED` · `COMPLETED`. `IN_PROGRESS`·`PAUSED`면 "이 취미 시작하기" 대신 "TODO 이어하기"(FR-MY-02).

화면 매핑:

| 화면 | 필드 |
|---|---|
| 브레드크럼 "취미 찾기 › 운동 › 러닝" | `category.name`, `name` |
| 태그 칩 | `tags` |
| 한 줄 소개 | `summary` |
| 소요 시간 · 초기 비용 · 난이도 | `sessionMinutes`, `initialCost`, `difficulty` |
| 상세 가이드 › 소개 | `description` |
| 상세 가이드 › 활동 환경 (장소·활동 방식) | `placeType`, `socialType`, `environment` |
| 시작 전 준비 › 준비물 | `supplies` (한 줄) |
| 시작 전 준비 › 예상 비용 (첫 방문·월 예상) | `initialCost`, `monthlyCost` |
| 시작 전 준비 › 첫 활동과 유의 사항 | `firstActivity`, `cautions` |

명세에 없어 API가 주지 않는 항목(디자인 04-4B에만 있음):
- **이런 사람에게 추천**: 빼거나 `tags`로 대체한다.
- **준비물 필수/선택 구분**: `supplies` 한 줄로 표시한다.
- **비용 계산하기, 첫 활동 계획에 추가**: 대응 기능이 없다. 버튼을 두지 않거나 "이 취미 시작하기"로 연결한다.

## 3. 플랜 미리보기 `GET /hobbies/{hobbyId}/plan-preview?skillLevel=BEGINNER&weeks=3`

- `skillLevel` 생략 시 `BEGINNER`, `weeks` 생략 시 2(1~3).
- 공개 원본이 없으면 404가 아니라 **200 + `ready:false`**. 이때 "준비 중인 계획이에요"를 보여 주고 시작하기를 비활성화한다.

응답 `data`:
```json
{
  "ready": true,
  "skillLevel": "BEGINNER",
  "title": "러닝 · 입문 3주 계획",
  "totalWeeks": 3,
  "previewWeeks": 2,
  "weeks": [
    {
      "weekNo": 1, "title": "걷기와 뛰기로 몸 적응시키기",
      "items": [
        { "seq": 1, "title": "1분 뛰고 2분 걷기 20분 하기", "description": "...",
          "expectedMinutes": 30, "supplies": "러닝화, 운동복, 물", "doneCriteria": "걷기·뛰기 반복 20분 완료하기" }
      ]
    }
  ]
}
```

화면 적용:
- **04-4B 활동 계획**: `weeks=3`으로 호출한다. 계획명은 `title`, 주차 목록은 `weeks[].title`, "활동 n개"는 `weeks[].items.length`. 현재 모든 원본은 **3주 × 주당 2개**라 1~3주차가 전체 계획이다. 디자인의 "입문 8주 계획 · 총 16개 활동", "4~8주차" 행은 쓰지 않는다.
- **04-5 시작 모달**: 숙련도마다 이 API를 호출해 `ready`가 false인 숙련도는 비활성화하고, 옆 문구는 `totalWeeks`로 "N주 계획"을 만든다. 현재 원본은 **BEGINNER(입문)만** 있다. 진행 중 개수 "2 / 3"은 `GET /users/me/todos/summary`(3.5)의 진행 수·최대 수를 쓴다.
- 시작은 `POST /users/me/hobbies/{hobbyId}/start` `{ "skillLevel": "BEGINNER" }`(3.5).

## 데이터 현황

- 취미 30개 모두 상세 글(소개·환경·준비물·첫 활동·유의 사항)과 BEGINNER 3주 계획이 들어 있다(V11, V12).
- 대표 이미지(`thumbnailUrl`)는 아직 없다.
- 상세 가이드의 관련 숏폼은 기존 `GET /hobbies/{hobbyId}/shorts?limit=3`. 주변 장소·입문 클래스·관련 자료는 장소·정보 피드 API(3.6, P1)에서 다룬다.
