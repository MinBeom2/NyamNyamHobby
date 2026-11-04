# 프론트엔드 빌드 전용 이미지 (실행용이 아님)
# Jenkins가 이 이미지에서 /app/dist 를 꺼내 서버의 /var/www/nyamnyam 에 배포하고, 호스트 Nginx가 정적 파일로 서빙한다
# 빌드: FE 폴더를 컨텍스트로 `docker build -t nyam-frontend-build FE`

FROM node:22-alpine AS build
WORKDIR /app

# 의존성 먼저 설치 (package*.json이 안 바뀌면 캐시 재사용)
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# API는 같은 도메인의 /api/v1 로 호출 (Nginx가 백엔드로 전달)
ENV VITE_API_BASE_URL=/api/v1
ARG VITE_KAKAO_MAP_KEY
ENV VITE_KAKAO_MAP_KEY=$VITE_KAKAO_MAP_KEY
RUN npm run build
