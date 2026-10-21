# 냠냠취미 기획 · 설계 문서

> 기준: 프로젝트 명세서 v0.3 · 디자인 컨셉북 v1.0 · 2026-10-19 · 작성 김시후(PM)

## 문서 목록

| 경로 | 내용 | 원본 |
|---|---|---|
| [spec/project-spec-v0.3.md](spec/project-spec-v0.3.md) | 프로젝트 명세서: 기능 · 비기능 요구사항, 화면, 데이터, API, 배치, 일정, 미결 사항 | [Claude Docs](https://claude.ai/code/artifact/e1912c90-ddd5-4f14-aebd-708d8c42ba35) |
| [spec/project-spec-v0.3.pdf](spec/project-spec-v0.3.pdf) | 같은 명세서의 PDF 판(ERD · 화면 흐름 · 시스템 구성 · 일정 도식 포함, 81쪽) | 〃 |
| [spec/api-spec.md](spec/api-spec.md) | REST API 상세 명세서: 공통 봉투, 에러 코드, 도메인별 요청·응답 DTO 및 예시 | 명세서 8장 |
| [db/ERD.md](db/ERD.md) | 데이터베이스 ERD: 전체 도메인 관계도 및 7개 도메인별 상세 Mermaid 다이어그램 | DDL 기반 |
| [db/V1__init.sql](db/V1__init.sql) | MySQL 8.0 초기 스키마, 테이블 41개. 명세서 7.5와 같은 내용이라 Flyway `V1__init.sql`로 그대로 씁니다 | 명세서 7.5 |
| [tasks/README.md](tasks/README.md) | 작업 목록 안내: 일정 기준, 영역 × 파트 ID, 먼저 시작할 것, 확인 필요 사항 | — |
| [tasks/FRONTEND_TASKS.md](tasks/FRONTEND_TASKS.md) | 프론트엔드 작업 72건 (필수 47 · 권장 25) | — |
| [tasks/BACKEND_TASKS.md](tasks/BACKEND_TASKS.md) | 백엔드 작업 91건 (필수 58 · 권장 33) | — |
| [tasks/PLANNING_TASKS.md](tasks/PLANNING_TASKS.md) | 기획 · 운영 작업 15건, 부록 기본안 수치 34개 | — |
| [tasks/requirements-progress.xlsx](tasks/requirements-progress.xlsx) | 요구사항 진척현황 시트. 담당자 · 진행표기를 채우면 요약이 자동 집계됩니다 | — |
| [design/figma-plugin/](design/figma-plugin/) | 디자인 컨셉북 화면 가이드를 Figma에 적용한 플러그인 | — |
| [Figma 와이어프레임](https://www.figma.com/design/9ExlElBFJhsqSCUQaT6lLP) | 화면 38개(사용자 32 · 관리자 4 · 공통 2), 컴포넌트 · 색 변수 · 텍스트 스타일 | Figma |

## 파트별로 먼저 볼 곳

- **프론트엔드**: 명세서 6장(화면 · 디자인 토큰 · MUI 테마 · 문구 규칙) → Figma 프레임 아래 `개발 메모` 띠 → `tasks/FRONTEND_TASKS.md`
- **백엔드**: 명세서 7 · 8 · 9장(데이터 · API · 배치) → `db/V1__init.sql` → `tasks/BACKEND_TASKS.md`
- **모두**: 명세서 11장 미결 사항(10/20 마감)과 `tasks/README.md`의 확인 필요 사항

## 고치는 방법

- **명세서**는 Claude Docs 원본을 고친 뒤 Markdown · PDF로 다시 내보내 `spec/`의 두 파일을 바꿉니다. 버전이 오르면 파일 이름의 버전도 올립니다.
- **DDL**은 명세서 7.5와 같이 고칩니다. 백엔드 저장소로 옮긴 뒤부터는 Flyway 마이그레이션(`V2__...sql`)을 추가하는 방식으로 바꿉니다.
- **진척**은 `requirements-progress.xlsx`의 진행표기가 기준입니다. MD 체크박스는 MR을 올릴 때 같이 체크합니다.
