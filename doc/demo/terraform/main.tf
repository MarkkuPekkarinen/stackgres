provider "google" {
  credentials = var.credentials
  project     = var.project
  region      = var.region_gcp
}

provider "aws" {
  region = var.region_aws
  profile = var.profile
}


// Module GCP
module "gcp" {
source                  ="./modules/cloud/gcp"
project_vpc             = "stackgres-demo-256115"
network_name            = "vpc-k8s"
subnet_name             = "gke-subnet"
subnet_ip               = "10.0.0.0/17"
subnet_region           = "us-central1"
range_name_pods         = "us-central1-01-gke-01-pods"
ip_cidr_range_pods      = "192.168.0.0/18"
range_name_services     = "us-central1-01-gke-01-services"
ip_cidr_range_services  = "192.168.64.0/18"
project_gke             = var.project
name_gke                = "gke-test-1"
region_gke              = "us-central1"
zones_gke               = ["us-central1-a", "us-central1-b", "us-central1-f"]
name_np                 = "default-node-pool"
machine_type_np         = "n1-standard-2"
min_count_np            = "1"
max_count_np            = "3"
disk_size_gb_np         = "20"
service_account_np      = var.service_account
initial_node_count_np   = "1"
value_np                = "my-node-pool"
}


// Module AWS
module "aws" {
source                   ="./modules/cloud/aws"
name_vpc                 = "eks-k8s"
cidr_vpc                 = "10.0.0.0/16"
azs_vpc                  = ["us-west-2a", "us-west-2b", "us-west-2c"]
private_subnets_vpc      = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
public_subnets_vpc       = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]
enable_nat_gateway_vpc   = true
enable_vpn_gateway_vpc   = false
enable_dns_hostnames_vpc = true
cluster_name_eks         = "aws-cluster"
cluster_version_eks      = "1.14"
instance_type_eks        = "m4.large"
asg_max_size_eks         = 5
}
