# Infraestructura 

## Orden de despliegue

### 1. Bootstrap del bucket de state (una sola vez)

```bash
cd infra/bootstrap
terraform init
terraform apply
```

Copia el `bucket_name` resultante y reemplaza el placeholder en
`infra/backend.tf`.

### 2. Apply de este PR

```bash
cd infra
terraform init
terraform validate
terraform plan
terraform apply
```

### 3. Usa los outputs en tu app real (no en local — local sigue usando LocalStack)

```bash
terraform output dynamodb_table_name
terraform output dynamodb_idempotency_table_name
```

## Para destruir

```bash
cd infra
terraform destroy
```

El bucket de `bootstrap/` no se destruye con esto (vive aparte a
propósito).
