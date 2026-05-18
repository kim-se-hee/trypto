// trypto-loadtest-base AMI 가 docker / 인프라 이미지 / sysctl·limits / bootstrap 마커
// 까지 다 박은 상태로 부팅하므로 user_data 는 불필요.
resource "aws_instance" "sut" {
  ami                         = data.aws_ami.trypto_base.id
  instance_type               = var.sut_instance_type
  key_name                    = var.key_pair_name
  vpc_security_group_ids      = [var.sut_sg_id]
  associate_public_ip_address = true

  root_block_device {
    volume_size = var.root_volume_gb
    volume_type = "gp3"
    encrypted   = true
  }

  dynamic "instance_market_options" {
    for_each = var.use_spot ? [1] : []
    content {
      market_type = "spot"
      spot_options {
        spot_instance_type             = "one-time"
        instance_interruption_behavior = "terminate"
      }
    }
  }

  tags = {
    Name    = "trypto-sut"
    Role    = "sut"
    Project = "trypto-loadtest"
  }
}

// 분산을 위해 loadgen 은 N대로 띄운다. 각 인스턴스는 SSH/k6 dashboard 만 쓰면 되므로
// EIP 는 안 붙이고 ephemeral 공인 IP 를 그대로 쓴다 — 매번 IP 가 바뀌지만 어차피 spot 이라 무관.
resource "aws_instance" "loadgen" {
  count                       = var.loadgen_count
  ami                         = data.aws_ami.trypto_base.id
  instance_type               = var.loadgen_instance_type
  key_name                    = var.key_pair_name
  vpc_security_group_ids      = [var.loadgen_sg_id]
  associate_public_ip_address = true

  root_block_device {
    volume_size = var.root_volume_gb
    volume_type = "gp3"
    encrypted   = true
  }

  dynamic "instance_market_options" {
    for_each = var.use_spot ? [1] : []
    content {
      market_type = "spot"
      spot_options {
        spot_instance_type             = "one-time"
        instance_interruption_behavior = "terminate"
      }
    }
  }

  tags = {
    Name    = "trypto-loadgen-${count.index}"
    Role    = "loadgen"
    Project = "trypto-loadtest"
  }
}

resource "aws_eip_association" "sut" {
  instance_id   = aws_instance.sut.id
  allocation_id = data.aws_eip.sut.id
}
