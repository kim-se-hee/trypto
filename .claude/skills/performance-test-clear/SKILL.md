---
description: >
  부하테스트용으로 띄워둔 EC2 인스턴스(SUT m5.2xlarge, loadgen r5.4xlarge × 4) 를 terraform destroy 로 정리한다.
  spot 인스턴스라 stop 이 안 되고 항상 destroy. EBS 도 같이 사라진다.
  EIP/SG/키페어/AMI 는 보존된다 — 다음 /performance-test 호출 때 같은 EIP 가 다시 붙는다.
---

`/performance-test` 가 띄운 인스턴스를 `terraform destroy` 로 정리한다. SUT/loadgen 둘 다 spot(`use_spot=true` 가 default) 으로 띄우기 때문에 stop 이 불가능하고 항상 destroy 만 가능하다. 다음 회차는 처음부터 (이미지 pull 50s 포함 ~80s).

## 사전 확인

```bash
terraform -chdir=terraform state list
```

`aws_instance.sut`, `aws_instance.loadgen`, `aws_eip_association.sut`, `aws_eip_association.loadgen` 만 있어야 한다. 다른 리소스가 보이면 사용자에게 보여주고 진행 여부를 확인한다.

데이터 소스(`data.aws_eip.*`, `data.aws_ami.*`) 는 state list 에 안 나오니 신경 쓸 필요 없다.

## 흐름

### 1. 현재 인스턴스 정보 캡처 (로그용)

```bash
terraform -chdir=terraform output 2>/dev/null || true
aws ec2 describe-instances \
  --region ap-northeast-2 \
  --filters "Name=tag:Project,Values=trypto-loadtest" "Name=instance-state-name,Values=running,stopped,stopping" \
  --query 'Reservations[].Instances[].{Id:InstanceId,Type:InstanceType,State:State.Name,Name:Tags[?Key==`Name`]|[0].Value}' \
  --output table
```

### 2. terraform destroy

```bash
terraform -chdir=terraform destroy -auto-approve
```

EIP/SG/키페어는 data 소스라 destroy 대상이 아니다 — 자동으로 보존된다.

### 3. 검증

EIP 가 살아있는지 + association 이 해제됐는지 확인.

```bash
aws ec2 describe-addresses \
  --region ap-northeast-2 \
  --allocation-ids eipalloc-0b6ba99aae704c843 eipalloc-0c74e5a847b8a1afb \
  --query 'Addresses[].{Alloc:AllocationId,Ip:PublicIp,Assoc:AssociationId,Tag:Tags[?Key==`Name`]|[0].Value}' \
  --output table
```

`AssociationId` 가 비어 있으면 깨끗히 해제된 상태. `PublicIp` 는 그대로다.

인스턴스가 정말 사라졌는지도 확인.

```bash
aws ec2 describe-instances \
  --region ap-northeast-2 \
  --filters "Name=tag:Project,Values=trypto-loadtest" "Name=instance-state-name,Values=running,pending,stopping,stopped" \
  --query 'Reservations[].Instances[].InstanceId' \
  --output text
```

빈 결과여야 한다.

### 4. 사용자 보고

| 항목 | 결과 |
|---|---|
| EC2 인스턴스 | destroy 완료 |
| EBS 볼륨 × 2 | 함께 제거됨 |
| EIP × 2 | 보존됨 — 다음 호출 시 재사용 |
| SG / 키페어 | 보존됨 |
| 다음 회차 | 처음부터 (이미지 pull 50s 포함 ~80s) |

⚠️ destroy 후 EIP 가 인스턴스에 연결돼 있지 않으면 시간당 ~$0.005/EIP 의 idle 요금이 부과된다 (월 $3.6/EIP × 2 = ~$7.2). 부하테스트를 며칠 안에 다시 안 할 거면 EIP release 도 안내한다.

```bash
# (선택) EIP 도 회수하려면 사용자 확인 후:
# aws ec2 release-address --region ap-northeast-2 --allocation-id eipalloc-0b6ba99aae704c843
# aws ec2 release-address --region ap-northeast-2 --allocation-id eipalloc-0c74e5a847b8a1afb
```

자동으로 release 하지 않는다. 사용자가 명시적으로 요청해야만 실행한다.

## 실패 처리

| 상황 | 대응 |
|---|---|
| terraform state 비어있음 | 이미 destroy 된 상태. 사용자에게 알리고 종료 |
| destroy 중 의존성 에러 | EIP association → instance 순서로 destroy. terraform 이 알아서 처리하지만 안 되면 `-target` 으로 association 부터 |
| terraform state 가 dirty | `terraform -chdir=terraform refresh` 후 재시도 |
| AWS rate limit | 1분 후 재시도 안내 |

부분 실패 상태로 두지 않는다. 두 인스턴스 모두 사라진 걸 확인하고 끝낸다.
