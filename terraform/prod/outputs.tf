output "app_public_ip" {
  description = "Cloudflare A 레코드가 가리킬 고정 공인 IP"
  value       = oci_core_public_ip.app.ip_address
}

output "ssh_command" {
  value = "ssh ubuntu@${oci_core_public_ip.app.ip_address}"
}

output "mysql_host" {
  description = ".env 의 MYSQL_HOST 에 넣을 값 (VCN 내부 전용 주소)"
  value       = "mysql.db.trypto.oraclevcn.com"
}

output "mysql_ip" {
  description = "호스트명이 안 풀릴 때를 대비한 프라이빗 IP"
  value       = oci_mysql_mysql_db_system.trypto.ip_address
}
