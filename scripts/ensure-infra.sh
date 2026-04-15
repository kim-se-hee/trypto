#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../terraform"

if [ ! -d .terraform ]; then
  echo "[ensure-infra] terraform init"
  terraform init -input=false
fi

if SUT_IP=$(terraform output -raw sut_public_ip 2>/dev/null) \
  && LG_IP=$(terraform output -raw loadgen_public_ip 2>/dev/null) \
  && [ -n "$SUT_IP" ] && [ -n "$LG_IP" ]; then

  if nc -z -w 5 "$SUT_IP" 22 2>/dev/null && nc -z -w 5 "$LG_IP" 22 2>/dev/null; then
    echo "[ensure-infra] already up: SUT=$SUT_IP loadgen=$LG_IP"
    exit 0
  fi
fi

echo "[ensure-infra] applying terraform..."
terraform apply -auto-approve -input=false
echo "[ensure-infra] apply done"
