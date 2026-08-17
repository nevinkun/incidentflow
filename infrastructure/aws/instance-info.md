# IncidentFlow EC2 Instance

- **Instance ID:** i-0b6d8aad5fb8eceb8
- **Public IP (static/Elastic):** 100.30.13.164
- **Region:** us-east-1
- **Instance type:** t4g.medium (2 vCPU, 4GB RAM, Graviton/arm64)
- **AMI:** ami-09317ccfac89b432d (Amazon Linux 2023, arm64)
- **Security group:** sg-0aedc24753d0b364d - allows SSH (22) from dev IP only, HTTP (80) from anywhere; all internal ports additionally bound to 127.0.0.1-only at the Docker level
- **IAM instance profile:** incidentflow-ec2-profile (role: incidentflow-ec2-role, CloudWatchAgentServerPolicy)
- **Key pair:** incidentflow-key (private key at ~/.ssh/incidentflow-key.pem, not committed)
- **SSH:** ssh -i ~/.ssh/incidentflow-key.pem ec2-user@100.30.13.164
- **GitHub deploy key:** incidentflow-ec2 (read-only), private half lives at ~/.ssh/incidentflow-deploy-key on the instance itself
- **App directory on instance:** ~/incidentflow
- **CloudWatch log groups:** /incidentflow/api, /incidentflow/worker, /incidentflow/nginx (14-day retention)

## Notes
- Elastic IP allocated so this address survives stop/start.
- Full deployment/shutdown procedures: see DEPLOYMENT.md in this same directory.
