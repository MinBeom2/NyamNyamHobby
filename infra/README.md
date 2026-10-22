# 냠냠취미 서버 구성 (EC2 k15d104.p.ssafy.io)

EC2 한 대에 Nginx(호스트)와 Docker 컨테이너들을 올린다. 외부에 열린 포트는 22·80·443뿐이고, 나머지는 모두 `127.0.0.1` 또는 Docker 내부 네트워크(`backend`)에서만 접근한다.

```
인터넷 ──443──► Nginx(호스트, Let's Encrypt)
                 ├─ /jenkins/ ─► jenkins   (127.0.0.1:19090)
                 └─ /api/     ─► backend   (127.0.0.1:18080)
                                  │  backend 네트워크
                                  ├─ postgres:5432 (호스트 127.0.0.1:15432)
                                  ├─ redis:6379    (호스트 127.0.0.1:16379)
                                  └─ rabbitmq:5672 (호스트 127.0.0.1:15673, 관리 화면 25672)
프론트엔드는 Vercel
```

## 서버 폴더와 이 저장소 파일
| 서버 경로 | 저장소 파일 | 내용 |
|---|---|---|
| `/home/ubuntu/infra/` | `server/infra/` | PostgreSQL · Redis · RabbitMQ |
| `/home/ubuntu/jenkins/` | `server/jenkins/` | Jenkins (Docker CLI 포함 이미지) |
| `/home/ubuntu/app/` | `server/app/` | 백엔드 컨테이너 (Jenkins가 배포) |
| `/etc/nginx/snippets/` | `server/nginx/` | `/jenkins/`, `/api/` 프록시 설정 |

`.env` 파일은 서버에만 있고 커밋하지 않는다(권한 600). 형식은 각 폴더의 `.env.example`을 본다.

## 배포 흐름
`dev`에 `BE/` 변경이 머지되면 GitLab 웹훅 → Jenkins(`BE/Jenkinsfile`) → 이미지 빌드(`BE/Dockerfile`) → `/home/ubuntu/app`에서 컨테이너 교체 → `/actuator/health` 확인 → Mattermost 알림.

## 보안 규칙
- UFW는 항상 켜 둔다. 22번 규칙은 지우지 않는다.
- Docker가 여는 포트는 UFW를 거치지 않으므로, 컨테이너 포트는 반드시 `127.0.0.1:`로 묶는다.
- 기본 포트를 그대로 쓰지 않는다(8080·5432·6379 등은 호스트에서 다른 번호로).
- 비밀값은 `.env`와 Jenkins Credentials에만 둔다.
