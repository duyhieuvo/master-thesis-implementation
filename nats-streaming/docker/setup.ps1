Write-Host "Start the infra"
docker-compose up -d nats-streaming-1 nats-streaming-2 nats-streaming-3 postgres_db adminer
Write-Host "Waiting for stream servers to be healthy"
Start-Sleep -Milliseconds 5000