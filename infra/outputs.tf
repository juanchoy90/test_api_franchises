output "vpc_id" {
  value = module.networking.vpc_id
}

output "public_subnet_ids" {
  value = module.networking.public_subnet_ids
}

output "app_security_group_id" {
  value = module.networking.app_security_group_id
}

output "dynamodb_table_name" {
  description = "Usar como DYNAMODB_TABLE_NAME en la app"
  value       = module.dynamodb.table_name
}

output "dynamodb_idempotency_table_name" {
  description = "Usar como DYNAMODB_IDEMPOTENCY_TABLE_NAME en la app (SUPUESTO — confirmar nombre real de property)"
  value       = module.dynamodb.idempotency_table_name
}

output "ecr_repository_url" {
  description = "Usar para docker tag / docker push"
  value       = module.ecr.repository_url
}

output "ecs_cluster_name" {
  value = module.ecs.cluster_name
}

output "ecs_service_name" {
  value = module.ecs.service_name
}
