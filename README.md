# 냠냠취미

냠냠

## DB 구성

로컬·운영 설정 원본은 PostgreSQL 17 + pgvector(`pgvector/pgvector:pg17`)를 사용합니다. 운영 반영은 Jenkins 백엔드 배포와 별도이며, 확장 활성화와 임베딩 스키마는 후속 Flyway 마이그레이션으로 추가합니다. 자세한 내용은 [인프라 안내](infra/README.md)를 참고하세요.

## 기획 · 설계 문서

> 기준: 기능 요구사항 CSV(10/21) 반영 · 프로젝트 명세서 v0.4 · 2026-10-21
>
> 명세서 md는 v0.4(파일 이름은 링크 유지를 위해 `project-spec-v0.3.md` 그대로)이고, **PDF·Claude Docs 원본은 v0.3이라 md가 최신**입니다. 기능·우선순위는 `feature-requirements.csv`가 단일 기준입니다.

| 문서 | 내용 |
|---|---|
| [repo/docs/spec/feature-requirements.csv](repo/docs/spec/feature-requirements.csv) | **기능 요구사항 목록(단일 기준)**: 상세기능 138줄, 기능 ID · 우선순위(P0 필수 · P1 권장 · P3 후순위 · P2 MVP 제외) |
| [repo/docs/spec/project-spec-v0.3.md](repo/docs/spec/project-spec-v0.3.md) | 프로젝트 명세서 v0.4(최신): 기능 · 화면 · 데이터 · API · 배치 · 일정 · 미결 사항 |
| [repo/docs/spec/project-spec-v0.3.pdf](repo/docs/spec/project-spec-v0.3.pdf) | 명세서 PDF 판(v0.3, 도식 포함) |
| [repo/docs/spec/api-spec.md](repo/docs/spec/api-spec.md) | REST API 상세 명세서 |
| [repo/docs/db/ERD.md](repo/docs/db/ERD.md) · [V1__init.sql](repo/docs/db/V1__init.sql) | 데이터베이스 ERD와 초기 스키마 |
| [repo/docs/tasks/README.md](repo/docs/tasks/README.md) | 파트별 작업 목록 안내 |
| [repo/docs/README.md](repo/docs/README.md) | 문서 전체 목록과 고치는 방법 |
