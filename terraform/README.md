## terraform — 부하테스트 인프라

`/performance-test` 와 `/performance-test-clear` 스킬이 사용한다.

### 관리하는 리소스

- `aws_instance.sut` — m5.2xlarge, SUT
- `aws_instance.loadgen` — c5.2xlarge, k6 실행기
- `aws_eip_association.{sut,loadgen}` — 기존 EIP 를 인스턴스에 붙임

### 참조만 하는 (data 소스, destroy 영향 없음)

- EIP × 2 (`eipalloc-0b6ba99aae704c843`, `eipalloc-0c74e5a847b8a1afb`)
- SG × 2 (`trypto-sut-sg`, `trypto-loadgen-sg`)
- 키 페어 `trypto-key-pair`
- AMI: Amazon Linux 2023 최신

### 명령

```bash
cd terraform
terraform init
terraform apply -auto-approve
terraform output
terraform destroy -auto-approve   # 인스턴스만 삭제, EIP/SG 보존
```
