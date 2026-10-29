# 냠냠취미 기획 · 설계 문서

> 기준: 기능 요구사항 CSV(10/21) 반영 · 프로젝트 명세서 v0.4 · 디자인 컨셉북 v1.0 · 2026-10-21 · 작성 김시후(PM)
>
> 명세서 md는 v0.4(파일 이름은 링크 유지를 위해 `project-spec-v0.3.md` 그대로)이고, **PDF·Claude Docs 원본은 v0.3에 머물러 있어 이 md가 최신**입니다. 기능·우선순위가 문서끼리 다르면 [`spec/feature-requirements.csv`](spec/feature-requirements.csv)를 따릅니다.

## 문서 목록

| 경로 | 내용 | 원본 |
|---|---|---|
| [spec/feature-requirements.csv](spec/feature-requirements.csv) | **기능 요구사항 목록(단일 기준)**: 상세기능 138줄 × 대분류 · 중분류 · 주기능 · 기능 ID · 우선순위(P0 필수 · P1 권장 · P3 후순위 · P2 MVP 제외) · 설명 · 조정 메모 | 팀 확정(10/21) |
| [spec/project-spec-v0.3.md](spec/project-spec-v0.3.md) | 프로젝트 명세서 **v0.4**(CSV 반영, 최신): 기능 · 비기능 요구사항, 화면, 데이터, API, 배치, 일정, 미결 사항 | [Claude Docs](https://claude.ai/code/artifact/e1912c90-ddd5-4f14-aebd-708d8c42ba35)(v0.3) |
| [spec/project-spec-v0.3.pdf](spec/project-spec-v0.3.pdf) | 같은 명세서의 PDF 판 **v0.3**(ERD · 화면 흐름 · 시스템 구성 · 일정 도식 포함, 81쪽). 우선순위·규칙은 md(v0.4)를 따름 | 〃 |
| [spec/취미태그_유사도기준표.xlsx](spec/취미태그_유사도기준표.xlsx) | **취미 태그 기준표**: 태그 결정 기록, 분류 목록, 성향(장소·사교·활동성)·주제(소재·얻는 것) 태그 목록, 취미 30개 × 분류·태그 입력표(드롭다운), 취미 간 유사도 미리보기(가중치 조정 가능) | 팀 결정(10/23~) |
| [spec/api-spec.md](spec/api-spec.md) | REST API 상세 명세서: 공통 봉투, 에러 코드, 도메인별 요청·응답 DTO 및 예시 | 명세서 8장 |
| [db/ERD.md](db/ERD.md) | 데이터베이스 ERD: V1 + V2(`tags.tag_group` 추가) 기준 전체 도메인 관계도 및 7개 도메인별 상세 Mermaid 다이어그램 | Flyway V1 + V2 기반 |
| [db/V1__init.sql](db/V1__init.sql) | PostgreSQL 17 초기 스키마(pgvector 포함 이미지 사용, 확장·임베딩 스키마는 후속 마이그레이션), 테이블 41개. 백엔드 Flyway 마이그레이션 `BE/src/main/resources/db/migration/V1__init.sql`의 사본이며 명세서 7.5와 같은 내용입니다 | 명세서 7.5 |
| [tasks/README.md](tasks/README.md) | 작업 목록 안내: 일정 기준, 영역 × 파트 ID, 먼저 시작할 것, 확인 필요 사항 | — |
| [tasks/FRONTEND_TASKS.md](tasks/FRONTEND_TASKS.md) | 프론트엔드 작업 72건 (필수 32 · 권장 19 · 후순위 8 · 제외 13) | — |
| [tasks/BACKEND_TASKS.md](tasks/BACKEND_TASKS.md) | 백엔드 작업 91건 (필수 36 · 권장 27 · 후순위 9 · 제외 19) | — |
| [tasks/PLANNING_TASKS.md](tasks/PLANNING_TASKS.md) | 기획 · 운영 작업 15건 (필수 11 · 권장 4), 부록 기본안 수치 36개 | — |
| [tasks/requirements-progress.xlsx](tasks/requirements-progress.xlsx) | 요구사항 진척현황 시트. 담당자 · 진행표기를 채우면 요약이 자동 집계됩니다 | — |
| [design/figma-plugin/](design/figma-plugin/) | 디자인 컨셉북 화면 가이드를 Figma에 적용한 플러그인 | — |
| [Figma 와이어프레임](https://www.figma.com/design/9ExlElBFJhsqSCUQaT6lLP) | 화면 38개(사용자 32 · 관리자 4 · 공통 2), 컴포넌트 · 색 변수 · 텍스트 스타일 | Figma |

## 파트별로 먼저 볼 곳

- **프론트엔드**: 명세서 6장(화면 · 디자인 토큰 · MUI 테마 · 문구 규칙) → Figma 프레임 아래 `개발 메모` 띠 → `tasks/FRONTEND_TASKS.md`
- **백엔드**: 명세서 7 · 8 · 9장(데이터 · API · 배치) → `db/V1__init.sql` → `tasks/BACKEND_TASKS.md`
- **모두**: `spec/feature-requirements.csv`(기능·우선순위 기준) → 명세서 4장 우선순위 정의와 11장 미결 사항 · 11.1 확인 필요 사항 → `tasks/README.md`의 확인 필요 사항

## 고치는 방법

- **기능·우선순위**는 `spec/feature-requirements.csv`를 먼저 고치고, 명세서 · API 명세 · 작업 목록을 거기에 맞춥니다.
- **명세서**는 원래 Claude Docs 원본을 고친 뒤 Markdown · PDF로 다시 내보내 `spec/`의 두 파일을 바꿉니다. v0.4는 md만 직접 고쳤으므로(PDF·원본은 v0.3), 원본을 다시 내보낼 때는 md의 v0.4 변경을 원본에 먼저 옮깁니다. 링크 유지를 위해 md 파일 이름은 v0.3 그대로 둡니다.
- **DDL**은 백엔드 `BE/src/main/resources/db/migration/`이 기준입니다. V1은 이미 적용됐으므로 고치지 않고 Flyway 마이그레이션(`V2__...sql`)을 추가하며, 바뀐 내용은 명세서 7장 · `db/` 문서에도 반영합니다.
- **진척**은 `requirements-progress.xlsx`의 진행표기가 기준입니다. MD 체크박스는 MR을 올릴 때 같이 체크합니다.
