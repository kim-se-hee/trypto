# 프로덕션 인프라 (오라클 클라우드, Always Free 한도 내).
# 부하테스트 스택(terraform/)과 완전히 분리된 별도 상태를 가진다.
#
#   terraform -chdir=terraform/prod init
#   terraform -chdir=terraform/prod apply
#
# A1 인스턴스 생성이 "Out of host capacity" 로 실패하면 시간을 두고 apply 를 재시도한다.
# 자세한 절차는 README.md 참고.

terraform {
  required_version = ">= 1.5"
  required_providers {
    oci = {
      source  = "oracle/oci"
      version = ">= 6.0"
    }
  }
}

provider "oci" {
  tenancy_ocid     = var.tenancy_ocid
  user_ocid        = var.user_ocid
  fingerprint      = var.fingerprint
  private_key_path = var.private_key_path
  region           = var.region
}

locals {
  # 개인 계정이라 루트 컴파트먼트(=테넌시)를 그대로 쓴다.
  compartment_id = var.compartment_ocid != "" ? var.compartment_ocid : var.tenancy_ocid
  tags           = { project = "trypto-prod" }
}

# ---------- 네트워크 ----------

resource "oci_core_vcn" "trypto" {
  compartment_id = local.compartment_id
  display_name   = "trypto-vcn"
  cidr_blocks    = ["10.0.0.0/16"]
  dns_label      = "trypto"
  freeform_tags  = local.tags
}

resource "oci_core_internet_gateway" "igw" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.trypto.id
  display_name   = "trypto-igw"
  freeform_tags  = local.tags
}

resource "oci_core_route_table" "public" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.trypto.id
  display_name   = "trypto-public-rt"
  freeform_tags  = local.tags

  route_rules {
    destination       = "0.0.0.0/0"
    network_entity_id = oci_core_internet_gateway.igw.id
  }
}

# 앱 VM 이 사는 퍼블릭 서브넷. 바깥에서 80/443, 관리자 IP 에서 22 만 받는다.
resource "oci_core_security_list" "public" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.trypto.id
  display_name   = "trypto-public-sl"
  freeform_tags  = local.tags

  ingress_security_rules {
    protocol = "6" # TCP
    source   = var.admin_cidr
    tcp_options {
      min = 22
      max = 22
    }
  }
  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"
    tcp_options {
      min = 80
      max = 80
    }
  }
  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"
    tcp_options {
      min = 443
      max = 443
    }
  }

  egress_security_rules {
    protocol    = "all"
    destination = "0.0.0.0/0"
  }
}

# 관리형 MySQL 이 사는 프라이빗 서브넷. 퍼블릭 서브넷에서 오는 3306/33060 만 받는다.
resource "oci_core_security_list" "private" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.trypto.id
  display_name   = "trypto-private-sl"
  freeform_tags  = local.tags

  ingress_security_rules {
    protocol = "6"
    source   = "10.0.0.0/24"
    tcp_options {
      min = 3306
      max = 3306
    }
  }
  ingress_security_rules {
    protocol = "6"
    source   = "10.0.0.0/24"
    tcp_options {
      min = 33060
      max = 33060
    }
  }

  egress_security_rules {
    protocol    = "all"
    destination = "0.0.0.0/0"
  }
}

resource "oci_core_subnet" "public" {
  compartment_id    = local.compartment_id
  vcn_id            = oci_core_vcn.trypto.id
  display_name      = "trypto-public"
  cidr_block        = "10.0.0.0/24"
  dns_label         = "pub"
  route_table_id    = oci_core_route_table.public.id
  security_list_ids = [oci_core_security_list.public.id]
  freeform_tags     = local.tags
}

resource "oci_core_subnet" "private" {
  compartment_id             = local.compartment_id
  vcn_id                     = oci_core_vcn.trypto.id
  display_name               = "trypto-private"
  cidr_block                 = "10.0.1.0/24"
  dns_label                  = "db"
  prohibit_public_ip_on_vnic = true
  security_list_ids          = [oci_core_security_list.private.id]
  freeform_tags              = local.tags
}
