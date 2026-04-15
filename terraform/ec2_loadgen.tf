locals {
  loadgen_user_data = <<-EOT
    #!/bin/bash
    set -eux
    exec > >(tee /var/log/user-data.log) 2>&1

    export DEBIAN_FRONTEND=noninteractive
    apt-get update
    apt-get install -y gnupg2 ca-certificates curl git jq

    gpg -k
    gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
      --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
    echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" > /etc/apt/sources.list.d/k6.list
    apt-get update
    apt-get install -y k6

    sudo -u ubuntu git clone ${var.infra_repo_url} /home/ubuntu/trypto
    touch /home/ubuntu/READY
    chown ubuntu:ubuntu /home/ubuntu/READY
  EOT
}

resource "aws_instance" "loadgen" {
  ami                         = data.aws_ami.ubuntu_2204.id
  instance_type               = var.loadgen_instance_type
  key_name                    = var.key_pair_name
  vpc_security_group_ids      = [aws_security_group.loadgen.id]
  subnet_id                   = tolist(data.aws_subnets.default.ids)[0]
  associate_public_ip_address = true

  user_data = local.loadgen_user_data

  tags = merge(var.tags, { Name = "trypto-loadgen" })
}
