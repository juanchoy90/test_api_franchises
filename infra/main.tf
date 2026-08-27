module "networking" {
  source = "./modules/networking"

  project_name   = var.project_name
  container_port = var.container_port
}

module "dynamodb" {
  source = "./modules/dynamodb"

  project_name = var.project_name
}

module "ecr" {
  source = "./modules/ecr"

  project_name = var.project_name
}

module "ecs" {
  source = "./modules/ecs"

  project_name       = var.project_name
  aws_region         = var.aws_region
  public_subnet_ids  = module.networking.public_subnet_ids
  security_group_id  = module.networking.app_security_group_id
  container_port     = var.container_port

  ecr_repository_url   = module.ecr.repository_url
  container_image_tag  = var.container_image_tag

  dynamodb_table_name             = module.dynamodb.table_name
  dynamodb_table_arn              = module.dynamodb.table_arn
  dynamodb_idempotency_table_name = module.dynamodb.idempotency_table_name
  dynamodb_idempotency_table_arn  = module.dynamodb.idempotency_table_arn
}
