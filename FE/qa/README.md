# 내 취미 관리 화면 검증

실제 화면: `/my-hobbies` → 첫 진행 중 계획(없으면 첫 일시중지 계획)을 선택합니다. 로그인 후 기존 API를 사용합니다. `/my-hobbies/:hobbyId/plan?planId=:planId&week=:weekNo`와 기존 `/todos` 링크를 지원합니다.

## 로컬 실행

```powershell
# FE 폴더, 로컬 BE 사용 시 이 환경변수는 생략합니다.
$env:VITE_API_PROXY_TARGET='https://k15d104.p.ssafy.io'
npm run dev -- --host 127.0.0.1 --port 5174 --strictPort
```

- 실제 앱: http://127.0.0.1:5174/my-hobbies
- 디자인 예시: http://127.0.0.1:5174/qa/hobby-management.html

디자인 예시는 로그인 없이 배치를 확인하는 별도 개발용 진입점입니다. 예시 데이터임을 상단에 표시하며, 변경 요청과 가짜 저장 성공을 제공하지 않습니다. 일반 앱 진입점은 이 파일이나 fixture를 import하지 않으며 프로덕션 빌드에도 포함하지 않습니다.

## 검증 결과 — 2026-11-06

- 최신 `dev`의 `e35b70e`를 fast-forward pull한 다음 구현했습니다.
- 변경 범위는 FE입니다. BE 코드, DB, 공통 API 문서는 변경하지 않았습니다.
- `npm test`: 26개 파일, 173개 테스트 통과.
- `npm run build`: TypeScript 및 프로덕션 빌드 통과.
- `git diff --check`: 통과.
- 별도 lint 설정·스크립트는 기존 프로젝트에 없습니다.
- UI 동작 테스트는 React Testing Library + jsdom + MSW로 기존 응답 봉투와 API 계약을 사용합니다. 테스트 요청은 운영 서버로 전송하지 않습니다.
- 확인 항목: 3/8주 계획, 16개 중 완료 5·건너뜀 1 → 31%, 메모 명시 저장·수정·삭제·취소, 초안과 상태 한 번에 저장, 실패 후 초안 유지·재시도, 이동 확인, 500자 제한, 키보드 주차 이동, 미래 주차·일시중지 잠금, 재개 3개 제한, 활동 추가·이동·시간 변경·204 삭제 후 재조회, 관심 해제 실패, 잘못된 취미/계획 연결, 빈 목록·조회 오류·기존 todos 경로.
- Chrome에서 디자인 예시의 320px·768px·데스크톱 배치, 가로 넘침, 모바일 취미 변경 패널, 포커스 복원을 확인했습니다. 페이지 가로 넘침은 `scrollWidth === clientWidth`로 확인했습니다.
- 실제 로컬 앱의 기존 API 연결과 로그인 필요 상태를 확인했습니다. 로그인 계정이 제공되지 않아 실제 계정의 저장·새로고침 왕복 검증은 실행하지 않았습니다. 인증이 필요한 저장 동작은 위 MSW 통합 테스트로 확인했습니다.

## 구현 구성

- `src/features/activity/model/managementAdapter.ts`: API DTO → 표시 모델, 날짜·시간·진행률·상태 처리.
- `src/features/activity/hooks/`: 목록·주차 조회, 기록·편집 mutation, 기존 Query Key 무효화.
- `src/features/activity/ui/`: 취미 목록, 계획 헤더, 주차 탭, 활동·메모, 계획 편집, 초안 이동 확인 패널.
- `src/pages/activities/hobbyManagement.css`: 화면 범위 테마·반응형 스타일.
- `src/shared/assets/hobbyThumbnails.ts`: 장소 찾기에 있던 기존 30종 취미 대표 이미지를 공용 매핑으로 재사용합니다. 내 계획·관심 목록은 해당 이미지를 사용하며, 매핑에 없는 취미는 API의 `thumbnailUrl`을 유지합니다.
- 데이터 라우터를 사용하여 링크·주차 이동·브라우저 뒤로가기에서 미저장 메모를 보호합니다. 새로고침·탭 닫기는 브라우저의 기본 이탈 확인을 사용합니다.
- 인증이 필요한 내 취미 URL은 로그인 이후 복귀할 주소를 유지합니다.
- 새 개발 의존성: `@testing-library/react`, `@testing-library/user-event`, `jsdom`, `msw`. 기존 Vitest를 재사용합니다.

빌드의 기존 단일 번들 크기 경고 및 Zod 주석 경고는 유지됩니다. 의존성 감사의 기존 Vitest 계열 경고는 이번 화면 변경 범위에서 메이저 업그레이드하지 않았습니다.
