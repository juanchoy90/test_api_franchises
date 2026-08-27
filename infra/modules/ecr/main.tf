resource "aws_ecr_repository" "app" {
  name                 = var.project_name
  image_tag_mutability = "MUTABLE"

  # force_delete permite que "terraform destroy" borre el repo aunque
  # todavia tenga imagenes adentro -- util en una prueba tecnica donde
  # vas a destruir todo despues de la demo, sin pasos manuales extra.
  force_delete = true

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = var.project_name
  }
}

# Se queda solo con las ultimas 5 imagenes, para no acumular storage sin
# limite en cada build/push que hagas mientras pruebas.
resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Mantener solo las ultimas 5 imagenes"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 5
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}
