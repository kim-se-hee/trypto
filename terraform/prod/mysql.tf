# 관리형 MySQL (HeatWave Always Free). 계정당 1개, 홈 리전 전용.
#
# 기존 MySQL 컨테이너가 command 로 강제하던 값과의 동등성:
#   - time_zone=+09:00        → 아래 구성(configuration)에서 지정
#   - utf8mb4_unicode_ci      → 서버 전역이 아니라 데이터베이스 생성 시 지정한다.
#     최초 1회 VM 에서:  CREATE DATABASE trypto CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

resource "oci_mysql_mysql_configuration" "kst" {
  compartment_id = local.compartment_id
  display_name   = "trypto-kst"
  shape_name     = "MySQL.Free"

  # 참고: provider 가 variables 대신 options 를 권하는 경고를 내지만,
  # options 블록에는 time_zone 항목이 없어 여기서는 variables 가 맞다.
  variables {
    time_zone = "+09:00"
  }
}

resource "oci_mysql_mysql_db_system" "trypto" {
  compartment_id      = local.compartment_id
  display_name        = "trypto-mysql"
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[0].name

  # Always Free 전용 shape. 스토리지 50GB 고정, 자동 백업 포함.
  shape_name              = "MySQL.Free"
  configuration_id        = oci_mysql_mysql_configuration.kst.id
  data_storage_size_in_gb = 50

  subnet_id      = oci_core_subnet.private.id
  hostname_label = "mysql" # → mysql.db.trypto.oraclevcn.com (VCN 안에서만 풀린다)
  port           = 3306
  port_x         = 33060

  admin_username = var.mysql_admin_username
  admin_password = var.mysql_admin_password

  crash_recovery = "ENABLED"

  # Always Free DB 시스템은 backup_policy 블록을 지정하면 생성이 거부된다.
  # 대신 일일 자동 백업이 기본으로 켜진 채 제공된다.

  deletion_policy {
    is_delete_protected        = true
    final_backup               = "REQUIRE_FINAL_BACKUP"
    automatic_backup_retention = "RETAIN"
  }

  freeform_tags = local.tags
}
