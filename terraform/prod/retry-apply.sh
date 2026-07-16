#!/usr/bin/env bash
# A1 무료 인스턴스가 "Out of host capacity" 로 실패할 때 용량이 풀릴 때까지 apply 를 재시도한다.
# 성공하면 outputs 를 로그에 남기고 종료한다.
#
#   nohup bash terraform/prod/retry-apply.sh &   (레포 루트에서)
#   진행 확인: tail terraform/prod/apply-retry.log
cd "$(dirname "$0")"
LOG=apply-retry.log
MAX=72 # 10분 간격 x 72회 = 12시간

for i in $(seq 1 "$MAX"); do
  echo "=== 시도 $i/$MAX $(date '+%F %T') ===" >>"$LOG"
  if terraform apply -input=false -auto-approve -no-color >>"$LOG" 2>&1; then
    echo "=== 성공 $(date '+%F %T') ===" >>"$LOG"
    terraform output -no-color >>"$LOG" 2>&1
    exit 0
  fi
  echo "--- 실패, 10분 뒤 재시도 ---" >>"$LOG"
  sleep 600
done

echo "=== $MAX 회 모두 실패. 셰이프 축소(1코어/6GB)나 PAYG 전환을 검토한다 ===" >>"$LOG"
exit 1
