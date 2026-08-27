# Tabla principal: single-table design, un item por entidad (Franchise,
# Branch, Product), todos compartiendo la misma partition key para poder
# leer el aggregate completo en un solo Query.
#
#   PK              | SK                          | tipo
#   FRANCHISE#{id}   | METADATA                    | Franchise
#   FRANCHISE#{id}   | BRANCH#{id}                 | Branch
#   FRANCHISE#{id}   | BRANCH#{id}#PRODUCT#{id}    | Product
#
# Motivo: evita el limite de 400KB por item que un modelo anidado
# (franquicia con sucursales/productos embebidos) alcanzaria con escala.
resource "aws_dynamodb_table" "franchise_management" {
  name         = var.project_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "PK"
  range_key    = "SK"

  attribute {
    name = "PK"
    type = "S"
  }

  attribute {
    name = "SK"
    type = "S"
  }

  tags = {
    Name = var.project_name
  }
}

# Tabla de idempotencia para los endpoints POST (crear franchise, y las
# que se agreguen despues para branch/product). TTL nativo limpia las
# keys vencidas solas, sin job de limpieza aparte.
resource "aws_dynamodb_table" "idempotency" {
  name         = "${var.project_name}-idempotency"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "idempotency_key"

  attribute {
    name = "idempotency_key"
    type = "S"
  }

  ttl {
    attribute_name = "expires_at"
    enabled        = true
  }

  tags = {
    Name = "${var.project_name}-idempotency"
  }
}
