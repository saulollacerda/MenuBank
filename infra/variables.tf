variable "tenancy_ocid" {
  description = "OCI tenancy OCID"
  type        = string
}

variable "user_ocid" {
  description = "OCI user OCID"
  type        = string
}

variable "fingerprint" {
  description = "OCI API key fingerprint"
  type        = string
}

variable "private_key_path" {
  description = "Path to OCI API private key (.pem)"
  type        = string
}

variable "region" {
  description = "OCI region (e.g. us-ashburn-1, sa-saopaulo-1)"
  type        = string
}

variable "compartment_id" {
  description = "OCI compartment OCID (use tenancy_ocid for root)"
  type        = string
}

variable "ssh_public_key" {
  description = "SSH public key to authorize on the VM"
  type        = string
}

variable "repo_url" {
  description = "Git repository URL to clone on the VM"
  type        = string
}

variable "db_user" {
  description = "PostgreSQL username"
  type        = string
  default     = "menubank"
}

variable "db_password" {
  description = "PostgreSQL password"
  type        = string
  sensitive   = true
}

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "menubank_prod"
}

variable "ad_index" {
  description = "Availability Domain index (0, 1 or 2). Change if you get 'Out of host capacity'."
  type        = number
  default     = 0
}
