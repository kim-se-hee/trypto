// 부하테스트용 베이스 AMI.
// packer/loadtest-base.pkr.hcl 로 빌드한 우리 전용 AMI 를 가리킨다.
// docker + 인프라 이미지(mysql/redis/...) 가 이미 박혀있어서 인스턴스 부팅 직후
// /performance-test 의 compose pull 이 사실상 0초로 통과한다.
//
// AMI 가 한 번도 빌드된 적 없으면 plan/apply 가 "no matching AMI found" 으로 실패.
// 그럴 때는:  cd packer && packer init . && packer build loadtest-base.pkr.hcl
data "aws_ami" "trypto_base" {
  most_recent = true
  owners      = ["self"]

  filter {
    name   = "name"
    values = ["trypto-loadtest-base-*"]
  }

  filter {
    name   = "tag:Project"
    values = ["trypto-loadtest"]
  }
}

data "aws_eip" "sut" {
  id = var.sut_eip_allocation_id
}

// loadgen 은 분산 후 EIP 를 쓰지 않는다 (인스턴스 N대에 EIP 1개 의미 없음).
// 기존 EIP allocation 은 보존만 — 단일 loadgen 으로 되돌릴 때를 위해 variable 도 그대로 둔다.
