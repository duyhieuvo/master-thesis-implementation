.\setup.ps1

docker-compose up event-generator
docker-compose up -d stream-processor-2-partition-revoked
Start-Sleep -Milliseconds 3000
docker-compose up -d stream-processor-1
docker-compose up -d stream-aggregator