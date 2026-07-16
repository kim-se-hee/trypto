data "oci_identity_availability_domains" "ads" {
  compartment_id = local.compartment_id
}

# 우분투 24.04 ARM(aarch64) 최신 이미지
data "oci_core_images" "ubuntu_arm" {
  compartment_id           = local.compartment_id
  operating_system         = "Canonical Ubuntu"
  operating_system_version = "24.04"
  shape                    = "VM.Standard.A1.Flex"
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}
