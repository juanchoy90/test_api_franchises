# El bucket referenciado aqui NO lo crea este mismo codigo (problema del
# huevo y la gallina). Se crea una unica vez con infra/bootstrap/, con
# state local, antes de correr esto. Ver infra/README.md.
terraform {
  backend "s3" {
    bucket       = "franchise-management-tfstate-602983921520"
    key          = "franchise-management/terraform.tfstate"
    region       = "us-east-1"
    encrypt      = true
    use_lockfile = true
  }
}
