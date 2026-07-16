# 1단계: 프로덕션 번들 빌드
# 빌드 스테이지는 빌드 머신 아키텍처로 돈다 (번들은 아키텍처 무관). arm64 크로스 빌드 시 에뮬레이션을 피한다.
FROM --platform=$BUILDPLATFORM node:20-alpine AS build

WORKDIR /app

# VITE_ 값은 런타임 환경변수가 아니라 빌드 시점에 번들에 박힌다. 따라서 build arg 로 받는다.
# 클라이언트 ID 와 redirect URI 는 브라우저에 노출되는 공개값이며, 시크릿은 백엔드에만 둔다.
ARG VITE_KAKAO_CLIENT_ID
ARG VITE_KAKAO_REDIRECT_URI
ARG VITE_KAKAO_AUTH_URL
ARG VITE_GOOGLE_CLIENT_ID
ARG VITE_GOOGLE_REDIRECT_URI
ARG VITE_GOOGLE_AUTH_URL

ENV VITE_KAKAO_CLIENT_ID=$VITE_KAKAO_CLIENT_ID \
    VITE_KAKAO_REDIRECT_URI=$VITE_KAKAO_REDIRECT_URI \
    VITE_KAKAO_AUTH_URL=$VITE_KAKAO_AUTH_URL \
    VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID \
    VITE_GOOGLE_REDIRECT_URI=$VITE_GOOGLE_REDIRECT_URI \
    VITE_GOOGLE_AUTH_URL=$VITE_GOOGLE_AUTH_URL

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# 2단계: 정적 파일 서빙
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
