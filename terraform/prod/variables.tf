# 인증 4종은 오라클 콘솔에서 API 키를 만들면 "구성 파일 미리보기"에 그대로 나온다.
# README.md 의 사전 준비 절차 참고.

variable "tenancy_ocid" {
  description = "테넌시 OCID"
  type        = string
}

variable "user_ocid" {
  description = "사용자 OCID"
  type        = string
}

variable "fingerprint" {
  description = "API 키 지문"
  type        = string
}

variable "private_key_path" {
  description = "API 개인 키(.pem) 경로"
  type        = string
}

variable "region" {
  description = "홈 리전. Always Free 자원은 홈 리전에서만 만들 수 있다"
  type        = string
  default     = "ap-osaka-1"
}

variable "compartment_ocid" {
  description = "리소스를 담을 컴파트먼트. 비우면 루트(테넌시)를 쓴다"
  type        = string
  default     = ""
}

variable "ssh_public_key_path" {
  description = "VM 접속용 SSH 공개 키 경로"
  type        = string
}

variable "admin_cidr" {
  description = "SSH(22) 를 허용할 관리자 IP 대역 (예: 1.2.3.4/32)"
  type        = string
}

variable "instance_ocpus" {
  description = "A1 코어 수. Always Free 한도는 총 2 OCPU (2026-06 축소 이후)"
  type        = number
  default     = 2
}

variable "instance_memory_gbs" {
  description = "A1 메모리(GB). Always Free 한도는 총 12GB"
  type        = number
  default     = 12
}

variable "boot_volume_gbs" {
  description = "부트 볼륨 크기(GB). Always Free 블록 스토리지 총 한도는 200GB"
  type        = number
  default     = 100
}

variable "mysql_admin_username" {
  description = "관리형 MySQL 관리자 계정명 (.env 의 MYSQL_USERNAME 과 맞춘다)"
  type        = string
  default     = "trypto"
}

variable "mysql_admin_password" {
  description = "관리형 MySQL 관리자 비밀번호 (.env 의 MYSQL_PASSWORD 와 맞춘다)"
  type        = string
  sensitive   = true
}
