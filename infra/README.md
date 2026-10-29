# 냠냠취미 서버 구성 (EC2 k15d104.p.ssafy.io)

EC2 한 대에 Nginx(호스트)와 Docker 컨테이너들을 올린다. 외부에 열린 포트는 22·80·443뿐이고, 나머지는 모두 `127.0.0.1` 또는 Docker 내부 네트워크(`backend`)에서만 접근한다.

```
인터넷 ──443──► Nginx(호스트, Let's Encrypt)
                 ├─ /           ─► 프론트엔드 정적 파일 (/var/www/nyamnyam/current)
                 ├─ /jenkins/   ─► jenkins   (127.0.0.1:19090)
                 └─ /api/, /oauth2/, /login/oauth2/ ─► backend (127.0.0.1:18080)
                                  │  backend 네트워크
                                  ├─ postgres:5432 (호스트 127.0.0.1:15432)
                                  ├─ redis:6379    (호스트 127.0.0.1:16379)
                                  └─ rabbitmq:5672 (호스트 127.0.0.1:15673, 관리 화면 25672)
프론트엔드도 같은 도메인에서 서빙하므로 CORS·쿠키 문제가 없다
```

## 서버 폴더와 이 저장소 파일
| 서버 경로 | 저장소 파일 | 내용 |
|---|---|---|
| `/home/ubuntu/infra/` | `server/infra/` | PostgreSQL 17 + pgvector · Redis · RabbitMQ |
| `/home/ubuntu/jenkins/` | `server/jenkins/` | Jenkins (Docker CLI 포함 이미지) |
| `/home/ubuntu/app/` | `server/app/` | 백엔드 컨테이너 (Jenkins가 배포) |
| `/etc/nginx/sites-available/default` | `server/nginx/site.conf` | 사이트 전체 설정 (HTTPS, 프론트 정적 파일, snippets include) |
| `/etc/nginx/snippets/` | `server/nginx/*.conf` | `/jenkins/`, `/api/`, `/oauth2/` 프록시 설정 |
| `/var/www/nyamnyam/` | — | 프론트엔드 릴리스 (`releases/<빌드번호>`, `current` 링크). Jenkins가 배포 |
| `/etc/docker/daemon.json` | `server/docker/daemon.json` | 컨테이너 로그 크기 제한 (파일당 10MB × 3개) |

`.env` 파일은 서버에만 있고 커밋하지 않는다(권한 600). 형식은 각 폴더의 `.env.example`을 본다.
저장소의 `server/` 파일을 고치면 서버의 같은 경로에도 직접 복사해야 반영된다(Jenkins는 서버의 `/home/ubuntu/app/docker-compose.yml`을 읽는다).

## 배포 흐름
`dev`에 `BE/` 변경이 MR로 머지되면 GitLab 웹훅 → Jenkins(`BE/Jenkinsfile`) → 테스트(전용 임시 네트워크의 PostgreSQL·RabbitMQ로 `./gradlew test`) → 이미지 빌드(`BE/Dockerfile`) → `/home/ubuntu/app`에서 컨테이너 교체 → `/actuator/health` 확인 → Mattermost 알림.
- 테스트는 DB와 RabbitMQ의 준비 완료를 확인한 후 실행하며, 성공·실패와 관계없이 두 컨테이너와 임시 네트워크를 정리한다. 테스트용 RabbitMQ는 호스트 포트를 공개하지 않는다.
- 테스트 중 자동 이유 발행 작업과 RabbitMQ 리스너는 중지한다. 브로커 통합 테스트가 직접 발행·수신·소비 처리를 실행하므로 다른 테스트 컨텍스트가 메시지를 먼저 가져가지 않는다. CI에서만 지정하는 Spring 설정이며 운영 환경변수를 추가할 필요는 없다.
- 테스트가 실패하면 이미지를 만들지 않고 서버는 그대로 둔다.
- 헬스 체크가 실패하면 직전에 돌던 이미지(`nyam-backend:previous`)로 되돌린다.
- 프론트엔드: `dev`에 `FE/` 변경이 머지되면 Jenkins(`FE/Jenkinsfile`) → `FE/Dockerfile`로 빌드(npm ci → 타입 검사 → vite build) → `/var/www/nyamnyam/releases/<빌드번호>`에 복사 → `current` 링크 교체 → Mattermost 알림. 최근 5개 릴리스를 남기며, 되돌릴 때는 서버에서 `ln -sfn releases/<번호> /var/www/nyamnyam/current`.
- `dev`는 보호 브랜치라 직접 푸시하지 않는다. `feature/{JIRA키}-{설명}` 브랜치에서 MR을 올리고, MR 설명에 `Closes {JIRA키}`를 적으면 머지할 때 Jira 이슈가 완료된다.

## 이미지 업로드 배포 설정 반영

커뮤니티 이미지 업로드를 처음 배포할 때 서버의 `/home/ubuntu/app/docker-compose.yml`에도 저장소의 `infra/server/app/docker-compose.yml`을 반영한다. Jenkins는 이 서버 폴더를 읽기 전용으로 마운트하므로 설정 파일 동기화는 서버에서 먼저 수행한다.

1. 기존 서버 Compose 파일을 백업한 뒤 최신 파일로 교체한다. 서버 `.env`는 유지하고 필요한 항목만 수정한다.
2. 최신 Compose에는 `UPLOAD_DIR=/app/uploads`와 `uploads:/app/uploads` 볼륨이 있다. 이후 컨테이너를 재생성해도 업로드 파일을 유지한다. 기존 컨테이너에 이미 저장한 이미지가 있으면 재생성 전에 해당 파일을 백업하고 볼륨으로 이관한다.
3. 공개 URL은 Compose의 `UPLOAD_PUBLIC_BASE_URL`로 지정한다. `.env`에 없거나 비어 있으면 `https://k15d104.p.ssafy.io`를 사용한다. 다른 도메인을 사용하면 서버 `.env`의 값을 변경한다.
4. 서버에서 `docker compose -f /home/ubuntu/app/docker-compose.yml config --quiet`로 구문을 확인한 뒤 Jenkins 배포를 실행한다.
5. 배포 후 `docker inspect backend --format '{{range .Mounts}}{{println .Type .Destination}}{{end}}'`에 `volume /app/uploads`가 있는지 확인한다. 이미지 업로드 응답 URL이 운영 도메인이고, 로그인하지 않은 브라우저에서도 해당 이미지를 조회할 수 있어야 한다.

이미지 경로는 기존 Nginx의 `/api/` 프록시를 사용한다. 저장소 설정 수정만으로 서버의 기존 Compose·`.env`가 갱신되지는 않는다.

## 로그 크기 제한 적용 (최초 1회)
```bash
sudo cp server/docker/daemon.json /etc/docker/daemon.json
sudo systemctl restart docker   # 모든 컨테이너가 잠깐 재시작됨 (restart: unless-stopped라 다시 올라옴)
```
설정은 새로 만드는 컨테이너부터 적용된다. 기존 컨테이너는 각 폴더에서 `docker compose up -d --force-recreate`로 한 번 다시 만든다.

## DB 구성 (2026-10-23 변경)

- 로컬(`BE/docker-compose.yml`)과 운영 설정 원본(`infra/server/infra/docker-compose.yml`)의 DB 이미지는 모두 `pgvector/pgvector:pg17`을 사용합니다. PostgreSQL 17에 pgvector 확장 파일을 포함한 이미지입니다.
- 기존 서비스 이름, 포트, 계정 설정, 볼륨과 데이터 경로는 유지합니다. 이미지 변경만으로 DB에 `vector` 확장이 활성화되지는 않습니다.
- 대상 DB의 확장 활성화(`CREATE EXTENSION IF NOT EXISTS vector;`)와 임베딩 테이블·인덱스는 이후 신규 Flyway 마이그레이션으로 추가합니다. 기존 `V1__init.sql`은 수정하지 않습니다.
- 이 변경은 저장소 설정 기준입니다. 현재 Jenkins는 백엔드만 배포하므로 운영 DB에는 자동 반영되지 않습니다. 운영 반영 시 서버 `/home/ubuntu/infra/docker-compose.yml`을 동기화한 뒤, 백업과 기존 볼륨을 확인하고 DB 컨테이너를 재생성해야 합니다. 볼륨을 삭제하지 않습니다.
- 벡터를 사용하는 백엔드 마이그레이션은 운영 DB 이미지 교체 후 배포합니다. 이미지 교체 중에는 DB 연결이 일시 중단됩니다.

## 보안 규칙
- UFW는 항상 켜 둔다. 22번 규칙은 지우지 않는다.
- Docker가 여는 포트는 UFW를 거치지 않으므로, 컨테이너 포트는 반드시 `127.0.0.1:`로 묶는다.
- 기본 포트를 그대로 쓰지 않는다(8080·5432·6379 등은 호스트에서 다른 번호로).
- 비밀값은 `.env`와 Jenkins Credentials에만 둔다.
