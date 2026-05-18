# xk6-sse 확장이 포함된 k6 커스텀 이미지.
# grafana/k6:latest 공식 이미지엔 xk6-sse 가 없어 ticker_subscription.js 가 import 못 한다.
#
# /performance-test 스킬이 이 Dockerfile 의 콘텐츠 해시로 태그를 만들고
# Docker Hub (kimsehee98/trypto-k6:lt-<hash>) 에 push 한다.
# 해시가 같으면 빌드/푸시를 통째로 스킵한다.

FROM golang:1.25-alpine AS builder

RUN apk add --no-cache git

ENV CGO_ENABLED=0
RUN go install go.k6.io/xk6/cmd/xk6@latest

# xk6-sse 는 k6 v1 모듈(go.k6.io/k6) 에 묶여있어 default 가 된 k6 v2 로 빌드하면
# extension 이 비활성화된다. k6 v1.x 로 명시 빌드.
# Dockerfile 텍스트 해시로 태그되므로 같은 Dockerfile 이면 Hub 의 이미지가 재사용된다.
RUN xk6 build v1.0.0 \
    --with github.com/phymbert/xk6-sse@latest \
    --output /usr/local/bin/k6

FROM alpine:3.20

RUN apk add --no-cache ca-certificates tzdata

COPY --from=builder /usr/local/bin/k6 /usr/local/bin/k6

ENTRYPOINT ["k6"]
