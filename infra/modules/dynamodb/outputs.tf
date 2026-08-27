output "table_name" {
  description = "Nombre de la tabla principal -- coincide con DYNAMODB_TABLE_NAME en application.yml"
  value       = aws_dynamodb_table.franchise_management.name
}

output "table_arn" {
  value = aws_dynamodb_table.franchise_management.arn
}

output "idempotency_table_name" {
  description = "Coincide con DYNAMODB_IDEMPOTENCY_TABLE_NAME -- SUPUESTO, confirmar contra el codigo real de IdempotencyPort"
  value       = aws_dynamodb_table.idempotency.name
}

output "idempotency_table_arn" {
  value = aws_dynamodb_table.idempotency.arn
}
