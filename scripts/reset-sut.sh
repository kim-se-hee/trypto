#!/usr/bin/env bash
set -euo pipefail

KEY="${SSH_KEY:-$HOME/.ssh/trypto-key-pair.pem}"
SSH_OPTS="-i $KEY -o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR -o ConnectTimeout=10"

cd "$(dirname "$0")/../terraform"
SUT_IP=$(terraform output -raw sut_public_ip)

echo "[reset-sut] SUT 컨테이너/볼륨 초기화 시작 ($SUT_IP)"
ssh $SSH_OPTS ubuntu@"$SUT_IP" 'bash /home/ubuntu/trypto/loadtest/reset.sh'
echo "[reset-sut] 완료"
