variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "environment" {
  type    = string
  default = "test"
}

variable "project_name" {
  type    = string
  default = "franchise-management"
}

variable "container_port" {
  description = "Puerto de la app Spring Boot"
  type        = number
  default     = 8080
}

variable "container_image_tag" {
  description = "Tag de la imagen en ECR que ECS debe correr"
  type        = string
  default     = "latest"
}
