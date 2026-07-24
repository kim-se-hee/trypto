# 앱 서버. Always Free A1 한도(2 OCPU / 12GB) 를 통째로 한 대에 쓴다.
# "Out of host capacity" 로 실패하면 apply 를 재시도한다 (README 참고).
resource "oci_core_instance" "app" {
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[0].name
  compartment_id      = local.compartment_id
  display_name        = "trypto-app"
  shape               = "VM.Standard.A1.Flex"
  freeform_tags       = local.tags

  shape_config {
    ocpus         = var.instance_ocpus
    memory_in_gbs = var.instance_memory_gbs
  }

  source_details {
    source_type             = "image"
    source_id               = data.oci_core_images.ubuntu_arm.images[0].id
    boot_volume_size_in_gbs = var.boot_volume_gbs
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.public.id
    hostname_label   = "app"
    # 고정(RESERVED) 공인 IP 를 아래에서 따로 붙이므로 임시 IP 는 받지 않는다.
    assign_public_ip = false
  }

  metadata = {
    ssh_authorized_keys = file(pathexpand(var.ssh_public_key_path))
    user_data           = base64encode(file("${path.module}/cloud-init.yaml"))
  }

  lifecycle {
    # 아래 항목이 바뀌어도 인스턴스를 갈아치우지 않는다. 실행 중인 프로덕션 서버를 재생성하면
    # 부트볼륨(InfluxDB 시드·인증서·nginx·/opt/trypto)이 전부 날아가기 때문이다.
    # - source_details: 오라클이 최신 우분투 이미지를 새로 내면 data 소스가 갱신돼 재생성을 유발
    # - metadata: cloud-init(user_data)은 최초 부팅 때만 실행되므로 파일이 바뀌어도 running 인스턴스엔 무의미
    ignore_changes = [source_details, metadata]
  }
}

data "oci_core_vnic_attachments" "app" {
  compartment_id = local.compartment_id
  instance_id    = oci_core_instance.app.id
}

data "oci_core_private_ips" "app" {
  vnic_id = data.oci_core_vnic_attachments.app.vnic_attachments[0].vnic_id
}

# 고정 공인 IP. 인스턴스를 지웠다 다시 만들어도 이 IP 는 유지되므로
# Cloudflare A 레코드를 다시 만질 일이 없다. (오라클은 공인 IPv4 무료)
resource "oci_core_public_ip" "app" {
  compartment_id = local.compartment_id
  display_name   = "trypto-app-ip"
  lifetime       = "RESERVED"
  private_ip_id  = data.oci_core_private_ips.app.private_ips[0].id
  freeform_tags  = local.tags
}
