variable "region" {
  type    = string
  default = "ap-northeast-2"
}

variable "sut_instance_type" {
  type    = string
  default = "m5.2xlarge"
}

variable "loadgen_instance_type" {
  type    = string
  default = "r5.4xlarge"
}

variable "loadgen_count" {
  type        = number
  default     = 1
  description = "loadgen 인스턴스 수. /performance-test 의 두 번째 인자로 덮어쓴다."
}

variable "use_spot" {
  type        = bool
  default     = true
  description = "loadtest 는 ephemeral 이라 SUT/loadgen 둘 다 spot 으로 띄워 비용을 70%+ 절감한다. 중단되면 그냥 다시 돌리면 된다."
}

variable "key_pair_name" {
  type    = string
  default = "trypto-key-pair"
}

variable "sut_sg_id" {
  type    = string
  default = "sg-0b382d035f1af8797"
}

variable "loadgen_sg_id" {
  type    = string
  default = "sg-0acffea9e1179d5a0"
}

variable "sut_eip_allocation_id" {
  type    = string
  default = "eipalloc-0b6ba99aae704c843"
}

variable "loadgen_eip_allocation_id" {
  type    = string
  default = "eipalloc-0c74e5a847b8a1afb"
}

variable "root_volume_gb" {
  type    = number
  default = 30
}
