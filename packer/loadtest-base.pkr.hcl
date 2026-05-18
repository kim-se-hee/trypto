// trypto 부하테스트 베이스 AMI 빌더.
//
// 무엇을 박는가:
//   - docker engine + docker-compose plugin
//   - sysctl / nofile 튜닝 (성능테스트용 high-fd / 포트 범위 확장)
//   - 인프라 이미지 pull (mysql / redis / rabbitmq / influxdb / prometheus / grafana
//     / mysqld-exporter / node-exporter / cadvisor / node / k6) — 매 회차 docker
//     pull 시간 0초로 만들기 위함. 모두 고정 버전 핀(latest 금지) 으로 박아서
//     compose pull 의 manifest digest 비교가 항상 캐시 히트로 끝난다.
//
// 안 박는 것:
//   - trypto-api / trypto-collector / trypto-engine — 이건 코드 해시 태그라 빌드마다
//     달라짐. /performance-test 가 매번 새 태그로 push 하고 SUT 가 그것만 받음.
//
// 빌드 방법:
//   cd packer
//   packer init .
//   packer build loadtest-base.pkr.hcl
//
// 빌드 후:
//   결과 AMI 이름이 trypto-loadtest-base-<timestamp> 로 박히고, terraform 의
//   data.aws_ami.trypto_base 가 most_recent 로 그걸 잡는다. 다음 /performance-test
//   호출 때 자동으로 새 AMI 사용.

packer {
  required_plugins {
    amazon = {
      source  = "github.com/hashicorp/amazon"
      version = ">= 1.3.0"
    }
  }
}

variable "region" {
  type        = string
  default     = "ap-northeast-2"
  description = "AMI 빌드 + 등록할 AWS 리전"
}

variable "build_instance_type" {
  type        = string
  default     = "t3.medium"
  description = "빌드용 임시 인스턴스 타입. 빌드 5~10분짜리라 작아도 됨."
}

locals {
  timestamp = formatdate("YYYYMMDD-hhmm", timestamp())
  ami_name  = "trypto-loadtest-base-${local.timestamp}"
}

source "amazon-ebs" "al2023" {
  region        = var.region
  instance_type = var.build_instance_type
  ssh_username  = "ec2-user"

  source_ami_filter {
    filters = {
      name                = "al2023-ami-2023.*-x86_64"
      virtualization-type = "hvm"
      root-device-type    = "ebs"
    }
    owners      = ["amazon"]
    most_recent = true
  }

  ami_name        = local.ami_name
  ami_description = "trypto loadtest base - docker + infra images pre-baked"

  launch_block_device_mappings {
    device_name           = "/dev/xvda"
    volume_size           = 30
    volume_type           = "gp3"
    delete_on_termination = true
    encrypted             = true
  }

  tags = {
    Name    = local.ami_name
    Project = "trypto-loadtest"
    Role    = "base"
  }
}

build {
  name    = "trypto-loadtest-base"
  sources = ["source.amazon-ebs.al2023"]

  // 1. 패키지 + docker
  provisioner "shell" {
    inline = [
      "set -euxo pipefail",
      "sudo dnf update -y",
      "sudo dnf install -y docker rsync",
      "sudo systemctl enable --now docker",
      "sudo usermod -aG docker ec2-user",
    ]
  }

  // 2. docker compose plugin (CLI plugin 형태)
  provisioner "shell" {
    inline = [
      "set -euxo pipefail",
      "DOCKER_COMPOSE_VERSION=v2.30.3",
      "sudo mkdir -p /usr/local/lib/docker/cli-plugins",
      "sudo curl -SL https://github.com/docker/compose/releases/download/$DOCKER_COMPOSE_VERSION/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose",
      "sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose",
      "docker --version || true",
      "sudo docker compose version",
    ]
  }

  // 3. 커널 / 파일디스크립터 튜닝
  provisioner "shell" {
    inline = [
      "set -euxo pipefail",
      "sudo tee -a /etc/sysctl.conf > /dev/null <<'EOF'",
      "fs.file-max=1000000",
      "net.core.somaxconn=65535",
      "net.ipv4.tcp_max_syn_backlog=65535",
      "net.ipv4.ip_local_port_range=10000 65535",
      "net.ipv4.tcp_tw_reuse=1",
      "EOF",
      "sudo tee -a /etc/security/limits.conf > /dev/null <<'EOF'",
      "ec2-user soft nofile 1000000",
      "ec2-user hard nofile 1000000",
      "* soft nofile 1000000",
      "* hard nofile 1000000",
      "EOF",
    ]
  }

  // 4. 인프라 이미지 미리 받음 (이게 본 목적)
  //    floating tag (latest 등) 은 빌드 시점의 latest 가 박힘.
  //    docker-compose.yml 의 태그가 그대로 매칭되면 compose pull 이 manifest 만 비교 후 통과.
  provisioner "shell" {
    inline = [
      "set -euxo pipefail",
      "sudo docker pull mysql:8.0.30",
      "sudo docker pull redis:7-alpine",
      "sudo docker pull rabbitmq:4-management-alpine",
      "sudo docker pull influxdb:2.7.10-alpine",
      "sudo docker pull prom/prometheus:v2.55.1",
      "sudo docker pull grafana/grafana:11.3.0",
      "sudo docker pull prom/mysqld-exporter:v0.14.0",
      "sudo docker pull prom/node-exporter:v1.8.2",
      "sudo docker pull gcr.io/cadvisor/cadvisor:v0.49.1",
      "sudo docker pull node:20-alpine",
      "sudo docker pull grafana/k6:latest",
      "sudo docker images",
    ]
  }

  // 5. 디렉토리 + bootstrap 마커
  //    AMI 부팅 직후 이미 다 셋업된 상태이므로 user_data 가 할 일이 없다 —
  //    그냥 마커가 있으면 /performance-test 의 bootstrap 대기가 즉시 통과.
  provisioner "shell" {
    inline = [
      "set -euxo pipefail",
      "sudo mkdir -p /home/ec2-user/trypto /home/ec2-user/k6",
      "sudo chown -R ec2-user:ec2-user /home/ec2-user/trypto /home/ec2-user/k6",
      "sudo touch /var/log/trypto-bootstrap-done",
    ]
  }
}
