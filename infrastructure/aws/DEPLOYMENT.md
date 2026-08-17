# IncidentFlow - AWS Deployment & Operations

See instance-info.md in this same directory for instance ID, IP, and key locations.

## Routine redeploy (after pushing new backend/frontend/infra changes)

    ssh -i ~/.ssh/incidentflow-key.pem ec2-user@100.30.13.164 'bash -s' << 'REMOTE_SCRIPT'
    set -e
    cd ~/incidentflow
    git pull
    docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d
    sleep 20
    docker compose ps
    REMOTE_SCRIPT

The two -f flags are required every time - omitting docker-compose.prod.yml silently drops CloudWatch log shipping without any error.

## Pausing between work sessions (saves compute cost only)

    aws ec2 stop-instances --instance-ids i-0b6d8aad5fb8eceb8

Resume later - the Elastic IP means the address never changes, this just confirms it:

    aws ec2 start-instances --instance-ids i-0b6d8aad5fb8eceb8 && \
    aws ec2 wait instance-running --instance-ids i-0b6d8aad5fb8eceb8 && \
    echo "Back up at: $(aws ec2 describe-instances --instance-ids i-0b6d8aad5fb8eceb8 --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)"

**Stopping does not stop all billing.** Only compute (~$0.034/hr) stops. The Elastic IP (~$0.005/hr, ~$3.60/mo) and the 20GB EBS volume (a small continuous monthly charge) keep billing regardless of instance state - this is current AWS policy (since Feb 2024), not a mistake in this setup.

## Full teardown (only once the project is completely finished - not now)

1. Terminate the instance
2. Release the Elastic IP explicitly - terminating the instance alone does not stop this charge
3. Delete the security group
4. Detach and delete the IAM instance profile and role
5. Optionally delete the CloudWatch log groups if no longer needed for reference

## Known cost profile

| Item | Rate | Notes |
|---|---|---|
| Compute (t4g.medium, running) | ~$0.034/hr (~$24.53/mo if 24/7) | Stops when instance is stopped |
| Elastic IP | ~$0.005/hr (~$3.60/mo) | Bills continuously regardless of instance state |
| EBS (20GB gp3) | Small continuous monthly charge | Bills regardless of instance state |
| CloudWatch Logs, data transfer | Negligible at this project's traffic volume | - |
