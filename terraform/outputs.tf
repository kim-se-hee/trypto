output "sut_public_ip" {
  value       = data.aws_eip.sut.public_ip
  description = "SUT EIP — 사용자가 브라우저에서 대시보드 띄울 때 쓰는 주소"
}

output "sut_private_ip" {
  value       = aws_instance.sut.private_ip
  description = "SUT 사설 IP — loadgen(같은 VPC, 다른 서브넷) 이 k6 트래픽을 보낼 때 이 주소를 써야 SG sg-ref 가 매칭된다. 공인 IP 로 가면 인터넷 게이트웨이로 나갔다 들어와 source IP 가 NAT 되어 SG 가 막는다."
}

output "loadgen_public_ips" {
  value       = aws_instance.loadgen[*].public_ip
  description = "loadgen 인스턴스들의 ephemeral 공인 IP 목록. 인스턴스마다 EIP 안 붙였으므로 apply 마다 바뀐다."
}

output "loadgen_private_ips" {
  value       = aws_instance.loadgen[*].private_ip
  description = "loadgen 사설 IP 목록 — 디버깅용."
}

output "sut_instance_id" {
  value = aws_instance.sut.id
}

output "loadgen_instance_ids" {
  value = aws_instance.loadgen[*].id
}
