# Subnets publicas, sin NAT Gateway. Fargate (cuando se agregue en un PR
# posterior) correra con IP publica. Decision de costo: el NAT Gateway es
# el recurso mas caro y menos justificable para una prueba tecnica de
# corta duracion (~$1.10/dia solo por existir).

data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.project_name}-vpc"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-igw"
  }
}

resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.${count.index}.0/24"
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.project_name}-public-${count.index}"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${var.project_name}-public-rt"
  }
}

resource "aws_route_table_association" "public" {
  count          = length(aws_subnet.public)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Security group para las futuras tareas de Fargate (se usa cuando se
# agregue el modulo de ECS en un PR posterior). El ingress se deja listo
# desde ya para no tener que retocar networking mas adelante.
resource "aws_security_group" "app_tasks" {
  name        = "${var.project_name}-app-tasks"
  description = "Trafico hacia las tareas de la app (via VPC Link, sin ALB)"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "Trafico interno de la VPC hacia el puerto de la app"
    from_port   = var.container_port
    to_port     = var.container_port
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.main.cidr_block]
  }

  # TEMPORAL: acceso publico directo, solo mientras no hay API Gateway
  # + VPC Link delante (recorte de alcance por tiempo, documentado en
  # CLAUDE.md). Cuando se agregue ese modulo, ELIMINAR este bloque de
  # ingress -- el trafico deberia entrar solo via VPC Link, nunca
  # directo desde internet.
  ingress {
    description = "TEMPORAL: acceso publico directo sin API Gateway delante"
    from_port   = var.container_port
    to_port     = var.container_port
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Salida libre (pull de ECR, llamadas a DynamoDB, etc.)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-app-tasks-sg"
  }
}
