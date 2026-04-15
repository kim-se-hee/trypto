locals {
  sut_user_data = <<-EOT
    #!/bin/bash
    set -eux
    exec > >(tee /var/log/user-data.log) 2>&1

    export DEBIAN_FRONTEND=noninteractive
    apt-get update
    apt-get install -y ca-certificates curl git jq

    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu jammy stable" > /etc/apt/sources.list.d/docker.list
    apt-get update
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    systemctl enable --now docker
    usermod -aG docker ubuntu

    sudo -u ubuntu git clone ${var.infra_repo_url} /home/ubuntu/trypto
    cd /home/ubuntu/trypto
    docker compose pull || true
    docker compose up -d

    deadline=$(( $(date +%s) + 900 ))
    while :; do
      unhealthy=$(docker compose ps --format '{{.Service}}|{{.Health}}' \
        | awk -F'|' '$2 != "" && $2 != "healthy" {print $1}')
      if [ -z "$unhealthy" ]; then
        break
      fi
      if [ "$(date +%s)" -gt "$deadline" ]; then
        echo "healthy timeout" >&2
        break
      fi
      sleep 10
    done

    touch /home/ubuntu/READY
    chown ubuntu:ubuntu /home/ubuntu/READY
  EOT
}

resource "aws_instance" "sut" {
  ami                         = data.aws_ami.ubuntu_2204.id
  instance_type               = var.sut_instance_type
  key_name                    = var.key_pair_name
  vpc_security_group_ids      = [aws_security_group.sut.id]
  subnet_id                   = tolist(data.aws_subnets.default.ids)[0]
  associate_public_ip_address = true

  root_block_device {
    volume_size = var.sut_ebs_size_gb
    volume_type = "gp3"
  }

  user_data = local.sut_user_data

  tags = merge(var.tags, { Name = "trypto-sut" })
}
