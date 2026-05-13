resource "oci_core_vcn" "menubank_vcn" {
  compartment_id = var.compartment_id
  cidr_block     = "10.0.0.0/16"
  display_name   = "menubank-vcn"
}

resource "oci_core_internet_gateway" "menubank_igw" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.menubank_vcn.id
  display_name   = "menubank-igw"
  enabled        = true
}

resource "oci_core_route_table" "menubank_rt" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.menubank_vcn.id
  display_name   = "menubank-rt"

  route_rules {
    destination       = "0.0.0.0/0"
    network_entity_id = oci_core_internet_gateway.menubank_igw.id
  }
}

resource "oci_core_security_list" "menubank_sl" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.menubank_vcn.id
  display_name   = "menubank-sl"

  egress_security_rules {
    destination = "0.0.0.0/0"
    protocol    = "all"
  }

  ingress_security_rules {
    protocol = "6" # TCP
    source   = "0.0.0.0/0"

    tcp_options {
      min = 22
      max = 22
    }
  }

  ingress_security_rules {
    protocol = "6" # TCP
    source   = "0.0.0.0/0"

    tcp_options {
      min = 80
      max = 80
    }
  }
}

resource "oci_core_subnet" "menubank_subnet" {
  compartment_id    = var.compartment_id
  vcn_id            = oci_core_vcn.menubank_vcn.id
  cidr_block        = "10.0.1.0/24"
  display_name      = "menubank-subnet"
  route_table_id    = oci_core_route_table.menubank_rt.id
  security_list_ids = [oci_core_security_list.menubank_sl.id]
}
