resource "google_compute_instance_template" "tp_provenir" {

 name    ="${var.tp_name}-${formatdate("YYYYMMDDHHmmss", timestamp())}"
 machine_type = var.machine_type
 can_ip_forward = false
 project  =var.project_id
 
 
 disk {
 
	source_image = "projects/${var.image_project_id}/global/images/family/${var.image_family}"
	disk_size_gb = var.root_vol_size'
	disk_encryption_key {
		kms_key_self_link =
		"projects/${var.cmek_project_project_id}/locations/${var.cmek_ring_gce}/cryptoKeys/${var.cmek_name_gce}"
		}
	}
	
	
	network_interface {
		subnetwork = local.subnetwork1.subnetwork
		network_ip = local.subnetwork1.network_ip
		}
		
		service_account {
			email = format ("%s@$s.iam.gserviceaccount.com", var.service_account, var.project_id)
			scopes = [
			"cloud-platform"
			]
		}
		
lifecycle {
 create_before_destroy = "true"
 }
 
 tags = var.tags
 labels = var.labels
 metadata = var.metadatas
		
}

resource "google_compute_health_check" "hc_provenir" {
 project    = var.project_id
 name       = var. hc_name
 timeout_sec=3
 check_interval =10
 healthy_threshold =4
 unhealthy_theshold = 5
 
 tcp_health_check {
port = "8161"
}
}

 version {
	name = var.tp_names
	instance_templte = google_compute_instance_template.tp_provenir.self_link
	
	}
auto_healing_policies {
	health_check = google_compute_health_check.hc_provenir.id
	initial_delay_sec=90
	}
	
resource "google_compute_health_check" "hc_provenir" {
 project    = var.project_id
 name       = var. mig_name
 base_instance_name =var.mig_vm_name
 region = var.region