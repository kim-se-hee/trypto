variable "region" {
  type    = string
  default = "ap-northeast-2"
}

variable "my_ip_cidr" {
  type        = string
  default     = ""
  description = "Override for source CIDR. If empty, current public IP is auto-detected."
}

variable "key_pair_name" {
  type    = string
  default = "trypto-key-pair"
}

variable "loadgen_instance_type" {
  type    = string
  default = "t3.medium"
}

variable "sut_instance_type" {
  type    = string
  default = "m5.2xlarge"
}

variable "sut_ebs_size_gb" {
  type    = number
  default = 40
}

variable "infra_repo_url" {
  type    = string
  default = "https://github.com/kim-se-hee/trypto.git"
}

variable "tags" {
  type = map(string)
  default = {
    Project = "trypto-loadtest"
    Owner   = "kimsehee98"
  }
}
