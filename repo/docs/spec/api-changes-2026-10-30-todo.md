# 2026-10-30 TODO API 구현 및 프론트 연결 안내

기준: [todo-work.md](../../../todo-work.md), Figma 00-2/06-1/06-2/06-3A·B. 정식 계약은 [api-spec.md 3.5](api-spec.md#35-내-취미-및-todo-apiv1usersmehobbies-apiv1plans). 모든 경로의 접두사는 `/api/v1`, 로그인 필요.

## 구현 범위

| API | 결과 |
|---|---|
| GET /users/me/todos/summary | 홈 현재 주차 남은 TODO·진행 중 카드·밀린 수·상태별 개수·최대 진행 수 |
| GET /users/me/hobbies?status= | 기존 배열 유지, 실제 planId·전체/주차 진행·주차명·총 주차·원본 숙련도 목록 추가 |
| POST /users/me/hobbies/{hobbyId}/start | 공개 숙련도 원본을 사용자 계획으로 복사, 201 |
| PATCH /users/me/hobbies/{hobbyId}/status | 일시중지·같은 계획 재개·그만두기, 200 |
| PUT/DELETE /users/me/hobbies/{hobbyId}/save | SAVED만 저장/해제, 진행·중지·종료 상태 보호 |
| GET /plans/{planId}?week= | 현재/선택 주차 분리, 진행·활동·밀림·준비물·권한 |
| GET /plans/{planId}/overview | 모든 주차 요약·밀린/건너뛴 활동, 06-3A/B 공통 |
| PATCH /plans/{planId}/items/{itemId} | 상태·메모 단독 저장·주차 이동·예상 시간 변경 |
| POST /plans/{planId}/items | 사용자 활동 추가 및 갱신 집계, 201 |
| DELETE /plans/{planId}/items/{itemId} | 원본/사용자 활동 soft delete, 204 |

## 확정 정책

사용자가 이번 구현에서 승인한 정책:

- 주차 완료는 비어 있지 않은 주차의 **DONE+SKIPPED=전체**. 전체 처리 완료도 같은 계산이나 자동 COMPLETED 전환·계획 축하 기능은 추가하지 않는다.
- 준비물 체크는 **프론트 임시 상태**. 서버에는 준비물 표시 배열과 활동별 원문 문자열만 제공한다.
- 미래 주차는 조회·계획 편집(추가/삭제/주차/시간) 가능. 완료·건너뛰기·메모 변경은 현재/과거만 가능하다. 이동과 동시에 보내도 미래 주차 제한을 우회하지 못한다.

그 밖의 구현 계약:

- 동시 IN_PROGRESS **3개**, PAUSED 제외. 사용자 행 잠금을 시작·재개·상태·활동·관심 변경에 공통 적용한다. 같은 상태 재요청은 멱등이다.
- 시간은 UTC 저장, 응답은 +09:00. 현재 주차는 서울 날짜 기준, 정지 기간을 초 단위로 제외한다. PAUSED·STOPPED 중 주차는 동결하고 재개해도 원래 시작일을 유지한다.
- 홈의 remainingThisWeekCount는 진행 중 현재 주차 TODO만 합산. overdueCount는 과거 TODO만 별도 합산하며 SKIPPED·PAUSED를 포함하지 않는다.
- 진행률은 DONE/삭제 제외 전체(0~1), total=0이면 0. 전부 건너뛰면 처리 완료여도 완료율은 0이다. 예상 시간은 완료·건너뜀을 포함한 삭제 제외 활동의 합계다.
- 메모 생략=유지, null=삭제, 빈 문자열=빈 메모. DONE 재시도는 최초 completedAt 유지, TODO/SKIPPED 전환은 제거한다.
- 새 활동은 주차 마지막. 이동도 도착 주차 마지막. 삭제/이동 후 출발 주차 순서 압축. 완료/건너뜀 활동도 삭제 가능하며 집계에서 즉시 제외한다. 주차·시간은 정수 JSON 숫자만 허용하며 소수 절삭·숫자 문자열 변환은 하지 않는다.
- 원본 활동과 사용자 추가 활동 모두 주차/시간/상태/메모 수정 및 삭제 가능. 기존 활동의 제목·설명·준비물·완료 기준 수정과 임의 순서 변경/일괄 편집은 지원하지 않는다.
- 일시중지는 같은 planId 재개. STOPPED/COMPLETED의 새 계획 생성·ARCHIVED로 재시작·지난 취미 UI는 이번 범위에 추가하지 않는다. 기존 ARCHIVED 계획은 조회만 가능하다.
- 선택한 숙련도의 공개 원본만 사용한다. 이전 기획의 자동 하위 숙련도 대체는 사용하지 않는다. availableSkillLevels를 이용해 선택하고, 미준비는 MY_PLAN_NOT_READY로 처리한다.
- 목록/홈 카드 정렬은 savedAt·userHobbyId 내림차순. 프레임의 카드 2개나 8주를 API 제한으로 고정하지 않는다.
- 한 조회 요청 내 집계가 같은 데이터 스냅샷을 보도록 읽기 트랜잭션은 REPEATABLE_READ를 사용한다.

## 응답 호환 및 갱신

- 내 취미의 기존 `data: []`, `name`, `thumbnailUrl`, `progressRate` 유지. `progressRate`는 전체 계획 비율이다.
- 계획 조회와 활동 mutation의 기존 `progress`는 `planProgress`와 같은 전체 집계 호환 별칭이다. 조회 주차는 `weekProgress`를 사용한다.
- `weekCompleted`는 조회 주차 또는 mutation 후 활동이 속한 주차의 값. `nextWeek`는 해당 주차+1이며 마지막은 null이다. 다음 주차 보기는 시간상 currentWeek를 변경하지 않는다.
- 전체 계획 `overview`의 selectedWeek/weekTitle/weekProgress/nextWeek는 null, items/supplies는 빈 배열이다. 펼칠 주차는 `GET /plans/{id}?week=N`으로 조회한다.
- 06-3 펼침은 읽기 전용 UI. canEditPlan은 별도 계획 편집 기능의 권한이며, 펼침에 완료 버튼을 두라는 뜻이 아니다.
- mutation 성공 후 **홈 summary·내 취미·현재/선택 주차·overview** 캐시 갱신. 이동은 출발·도착 주차 모두 갱신한다. DELETE는 204이므로 다시 조회한다.
- 프론트 fixture/상수 교체와 캐시 연결은 이번 백엔드 작업에서 실행하지 않았으며 [FRONTEND_TASKS.md](../tasks/FRONTEND_TASKS.md)에 미완료로 남긴다.

## DB 및 원본 데이터

- [V10](../../../BE/src/main/resources/db/migration/V10__todo_plan_weeks_and_pause_time.sql): plan_template_weeks/user_plan_weeks(복합 PK, 주차 제목 50자), user_hobbies.paused_seconds, 같은 관계의 ACTIVE 계획 1개 부분 UNIQUE 인덱스. V1 직접 수정 없음.
- 기존 계획 주차 제목은 중립적인 `N주차`로 backfill한다. 활동 제목으로 추정하지 않는다. 기존 paused_days는 초로 변환하며 이후 초 단위 계산을 기준으로 한다.
- 기존 ACTIVE 계획 중복은 V10 적용 전에 확인해야 한다. 인덱스 생성 시 중복 데이터를 지우지 않고 실패한다.
- [V11](../../../BE/src/main/resources/db/migration/V11__seed_beginner_todo_templates.sql): **클라이밍·드로잉·사진 촬영의 BEGINNER**, 3주·주당 2개 활동. 이름으로 취미를 찾아 신규 원본만 생성하므로 기존 수기 원본을 덮지 않는다. 다른 취미·숙련도는 공개 원본을 준비해야 시작 가능하다.
- 관리자 원본 편집 API·화면은 이번 구현에 포함하지 않는다. 원본/사본의 주차 제목과 템플릿 버전을 분리해 사용자 기록을 보존한다.

## 오류 처리

| HTTP / 코드 | 조건 |
|---|---|
| 400 COMMON_INVALID_INPUT | 잘못된 상태/숙련도, 빈 PATCH, 허용하지 않는 null, 길이·주차·시간 범위 오류 |
| 401 COMMON_UNAUTHORIZED | 로그인 없음 |
| 403 COMMON_FORBIDDEN | 타인 계획 |
| 404 COMMON_NOT_FOUND | 없는 계획/취미 관계/활동, 다른 계획 소속 itemId, 삭제된 활동 PATCH |
| 404 HOBBY_NOT_FOUND | 시작/관심 저장 대상 취미 없음·비공개 |
| 409 MY_HOBBY_LIMIT | 시작/재개 시 진행 중 3개 |
| 409 MY_PLAN_NOT_READY | 선택 숙련도 공개 원본 없음·주차/활동 불완전 |
| 409 MY_HOBBY_IN_PROGRESS | 진행·일시중지 취미 중복 시작 또는 관심 저장/해제 |
| 409 MY_HOBBY_INVALID_TRANSITION | 허용되지 않는 상태 전이·종료 후 새 시작·종료 이력 관심 변경 |
| 409 TODO_PLAN_NOT_ACTIVE | PAUSED/STOPPED/COMPLETED 취미·ARCHIVED 계획 수정 |
| 409 TODO_FUTURE_WEEK | 미래 주차 상태·메모 변경 |

## 검증

2026-10-30 최종 실행 결과: **총 230개 중 226개 통과, 실패 0, 오류 0, PostgreSQL 통합 테스트 4개 건너뜀**. `git diff --check` 통과. 기본 JSON 숫자 변환이 소수를 절삭하던 경우를 발견해 계획 요청에 정수 전용 역직렬화를 적용했으며 관련 거부 테스트도 통과했다.

계획 도메인·MVC·기존 사용자/온보딩·탐색·보관·보안 테스트를 실행한다. 주차 경계, 정지 반복/초 누적, 동일 계획 재개, 메모 500자/생략/null, 완료 재시도, 집계·빈 주차·전부 건너뜀·소유자·다른 계획 itemId·soft delete·미래 주차 우회를 검증한다.

실제 PostgreSQL 통합 테스트는 `PlanPostgresIntegrationTest` 4개로 별도 제공한다. 기본적으로 `TODO_POSTGRES_TESTS=true`가 없으면 건너뛴다. DB 행 잠금 기반 동시 시작/재개·중복 시작과 JPA 사본/변경의 실제 DB 왕복을 검증한다. 실행 환경에는 Flyway 전체 마이그레이션을 적용할 수 있는 PostgreSQL+pgvector가 필요하다.

```powershell
# BE 디렉터리: 단위/MVC 및 관련 회귀 테스트
.\gradlew.bat test --tests 'com.ssafy.nyamnyam.domain.plan.*' --tests 'com.ssafy.nyamnyam.domain.user.*' --tests 'com.ssafy.nyamnyam.domain.exploration.service.*' --tests 'com.ssafy.nyamnyam.domain.exploration.ExplorationControllerTest' --tests 'com.ssafy.nyamnyam.domain.shortform.service.ShortSaveServiceTest' --tests 'com.ssafy.nyamnyam.global.security.SecurityConfigTest'

# 별도 테스트 DB의 DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD를 설정한 후
$env:TODO_POSTGRES_TESTS='true'
.\gradlew.bat test --tests 'com.ssafy.nyamnyam.domain.plan.PlanPostgresIntegrationTest'
Remove-Item Env:TODO_POSTGRES_TESTS
```

로컬 PostgreSQL 포트와 Docker 엔진이 실행되지 않아 이 환경에서 실제 DB 마이그레이션/동시성 통합 검증은 실행하지 않았다. 배포 전 테스트 DB에서 위 4개 테스트와 V10/V11 적용을 확인한다.
