output "repository_url" {
  description = "URL para docker push / docker tag"
  value       = aws_ecr_repository.app.repository_url
}

output "repository_arn" {
  value = aws_ecr_repository.app.arn
}

output "repository_name" {
  value = aws_ecr_repository.app.name
}
