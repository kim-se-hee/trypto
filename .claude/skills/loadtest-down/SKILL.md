---
name: loadtest-down
description: Panic button. Terminates the trypto load-test EC2 instances (SUT + loadgen) via AWS CLI by tag. EIPs are preserved (prevent_destroy). Use whenever the user wants to stop / clean up / tear down the load test infra (e.g. "/loadtest-down", "테스트 끝났어 정리해줘", "이상해 멈춰").
---

# /loadtest-down

부하테스트 인프라 즉시 정리. 인스턴스 종료(루트 EBS 자동 삭제 포함) → EIP/보안그룹/IAM은 그대로 남음 → 다음 `/loadtest`에서 같은 EIP로 재사용 가능.

## 절차

1. **태그로 인스턴스 ID 조회**
   ```bash
   IDS=$(aws ec2 describe-instances \
     --region ap-northeast-2 \
     --filters "Name=tag:Project,Values=trypto-loadtest" \
               "Name=instance-state-name,Values=pending,running,stopping,stopped" \
     --query 'Reservations[].Instances[].InstanceId' \
     --output text)
   ```

2. **결과 확인**
   - 비어있으면 사용자에게 "정리할 인스턴스 없음" 알리고 종료
   - 있으면 ID 목록을 사용자에게 보여주고 terminate

3. **종료**
   ```bash
   aws ec2 terminate-instances --region ap-northeast-2 --instance-ids $IDS
   ```

4. **결과 출력** — 종료 요청한 인스턴스 ID와, 보존된 EIP가 있으면 안내:
   ```
   terminated: i-xxx, i-yyy
   EIP는 보존됨. 다음 /loadtest 시 같은 IP로 재생성.
   ```

## 주의

- terraform state는 약간 stale 해지지만 다음 `terraform apply`(== `/loadtest`)에서 자동 reconcile됩니다. 별도 조치 불필요.
- region은 `ap-northeast-2` 고정 (terraform default와 동일).
- 확인 프롬프트 없이 즉시 실행 — 패닉 버튼이라는 본분에 충실하게.