# 보관한 숏폼 API 구현 요청

## 현재 상태

- 숏폼 보관과 해제는 `PUT`, `DELETE /api/v1/shorts/{shortId}/save`로 구현되어 있다.
- 보관 데이터는 `short_saves(user_id, short_id, created_at)`에 저장된다.
- `GET /api/v1/users/me/saved-shorts`는 API 명세에 있지만 백엔드 구현은 없다.
- `SavedShortsPage.tsx`는 fixture를 표시하고, 보관 해제도 화면 상태에서만 처리한다.
- `MyPage.tsx`의 보관한 숏폼 수는 `12`로 고정되어 있다.

이 문서는 변경 요청만 정리한다. 백엔드와 공통 API 명세는 수정하지 않았다.

## 필수 구현

### 로그인한 사용자의 보관 목록 조회

기존 명세 경로:

```http
GET /api/v1/users/me/saved-shorts?hobbyId={hobbyId}&page=0&size=20
Authorization: Bearer <accessToken>
```

- 사용자 ID는 인증 정보에서 가져오고, 해당 사용자의 보관 항목만 조회한다.
- `hobbyId`는 선택 필터이며, 미지정 시 전체 보관 목록을 조회한다.
- `short_saves`와 `shorts`를 조인하고 취미 정보는 LEFT JOIN한다. 취미가 없는 영상도 누락하지 않는다.
- 기본 정렬은 보관 시각 내림차순으로 한다. 시각이 같으면 `shortId`로 순서를 고정하여 페이지 순서를 안정적으로 유지한다.
- `page`, `size`, `hobbyId`의 허용 범위를 검증한다.
- 결과가 없으면 빈 `content`와 0건의 페이지 정보를 반환한다.

필요한 항목 필드는 `shortId`, `youtubeVideoId`, `title`, nullable `hobby: { id, name }`, `status`, `savedAt`이다. 응답은 기존 `ApiResponse`와 `PageResponse` 형식을 사용한다.

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "shortId": 41,
        "youtubeVideoId": "aBcD123xYz0",
        "title": "퇴근 후 1시간, 볼더링 첫 도전기",
        "hobby": { "id": 3, "name": "클라이밍" },
        "status": "ACTIVE",
        "savedAt": "2026-10-20T05:20:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1,
      "hasNext": false
    }
  },
  "error": null
}
```

위 예시는 아래 `saveId` 불일치의 해결안으로 해당 필드를 제외한 제안이다. 최종 응답 계약 확정이 필요하다.

### 명세와 DB의 불일치 해결

- 기존 명세에는 `saveId`가 있으나 DB는 `(user_id, short_id)` 복합 기본 키이며 별도 보관 ID가 없다.
- 권장안은 명세에서 `saveId`를 제거하고 사용자별 항목 식별에 `shortId`를 사용하는 것이다. 이를 위해 DB 구조를 바꿀 필요는 없다.
- `savedAt`은 `short_saves.created_at`에서 가져온다. DB의 UTC 저장 규칙에 맞춰 `Z` 또는 명시적 오프셋이 있는 ISO 8601 형식으로 반환한다.
- 보관 후 비활성화된 영상도 목록에 포함하고 실제 `status`를 반환한다. 프런트는 `ACTIVE`만 재생 가능하게 표시하며 나머지도 보관 해제할 수 있어야 한다.

## 현재 화면 기능을 유지하기 위해 협의할 계약

- **정렬:** 화면에 최근 보관순과 오래된 보관순이 있다. 페이지 단위 데이터를 프런트에서 뒤집으면 전체 목록 정렬이 되지 않으므로, 보관 시각 오름차순/내림차순을 선택할 서버 정렬 파라미터가 필요하다. 파라미터 이름과 허용 값은 명세에 추가해야 한다.
- **취미 필터와 건수:** 현재의 고정 취미 목록과 건수를 대체하려면 사용자 보관 목록 전체를 기준으로 한 취미별 `id`, `name`, `count` 집계가 필요하다. 목록 응답 확장 또는 별도 집계 API 중 방식을 확정한다. 한 페이지의 항목만 집계해서 전체 건수처럼 표시하면 안 된다.
- **마이페이지 보관 수:** 전체 목록 조회의 `page.totalElements`를 활용할 수 있다. 이 방식이면 보관 수만을 위한 API는 필수가 아니다.
- **썸네일:** 기존 명세에는 `thumbnailUrl`이 없다. DB의 썸네일 주소를 반환할지, 프런트에서 `youtubeVideoId`를 이용할지 확정한다. 별도 동영상 재생 API는 필요하지 않다.

## 수정 대상과 검증 범위

- `BE/.../domain/user/controller/UserController.java`: 목록 조회 엔드포인트 추가. 별도 보관 목록 컨트롤러로 분리해도 된다.
- `BE/.../domain/shortform/service/ShortSaveService.java`: 사용자별 조회·입력 검증 추가 또는 목록 조회 서비스 분리.
- `BE/.../domain/shortform/repository/ShortSaveRepository.java`: 사용자별 목록·건수·취미 필터·정렬 쿼리 추가.
- 보관 목록 응답 DTO 추가. 기존 공통 `PageResponse` 재사용.
- 백엔드 검증: 인증 없는 요청, 다른 사용자 데이터 차단, 빈 목록, 페이지 이동, 취미 필터, 정렬 동률, nullable 취미, 비활성 영상, 보관·해제 후 목록과 총건수 반영, 잘못된 조회 파라미터.

API 구현과 계약 확정 후 프런트에서는 fixture를 Query hook으로 교체하고, 보관 해제를 기존 DELETE API에 연결한다. 탐색 화면의 보관·해제와 보관함의 해제 성공 시 목록·집계 캐시를 갱신하고, 로딩·빈 목록·오류·재시도 상태를 연결한다.

## 확인한 소스

- [현재 보관 API 명세](../../repo/docs/spec/api-spec.md)
- [현재 보관 목록 화면](../src/pages/mypage/SavedShortsPage.tsx)
- [현재 마이페이지](../src/pages/mypage/MyPage.tsx)
- 저장소 루트 기준 `BE/src/main/java/com/ssafy/nyamnyam/domain/shortform/repository/ShortSaveRepository.java`
- 저장소 루트 기준 `BE/src/main/resources/db/migration/V1__init.sql`의 `short_saves` 정의
