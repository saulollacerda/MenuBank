output "instance_public_ip" {
  description = "Public IP of the MenuBank demo VM — share this with the client"
  value       = oci_core_instance.menubank_vm.public_ip
}
