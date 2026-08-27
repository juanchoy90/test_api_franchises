variable "project_name" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "security_group_id" {
  type = string
}

variable "container_port" {
  type = number
}

variable "ecr_repository_url" {
  type = string
}

variable "container_image_tag" {
  type    = string
  default = "latest"
}

variable "dynamodb_table_name" {
  type = string
}

variable "dynamodb_table_arn" {
  type = string
}

variable "dynamodb_idempotency_table_name" {
  type = string
}

variable "dynamodb_idempotency_table_arn" {
  type = string
}
