Write-Host "Start the infra"
docker-compose up -d zookeeper1 zookeeper2 zookeeper3 kafka1 kafka2 kafka3 cli postgres_db adminer
[string]$status1 = $(docker inspect --format='{{.State.Health.Status}}' kafka1)
[string]$status2 = $(docker inspect --format='{{.State.Health.Status}}' kafka2)
[string]$status3 = $(docker inspect --format='{{.State.Health.Status}}' kafka3)
while (($status1 -ne "healthy") -or ($status2 -ne "healthy") -or ($status3 -ne "healthy"))
{
    Write-Host -NoNewLine "`r$a% Waiting for brokers to be healthy"
    Start-Sleep -Milliseconds 10
    $status1 = $(docker inspect --format='{{.State.Health.Status}}' kafka1)
    $status2 = $(docker inspect --format='{{.State.Health.Status}}' kafka2)
    $status3 = $(docker inspect --format='{{.State.Health.Status}}' kafka3)
}
Write-Host
Write-Host "Brokers are now fully started"

Write-Host "Create necessary topics"
docker exec cli kafka-topics --create --topic raw-event --bootstrap-server kafka:9092 --partitions 2 --replication-factor 3
docker exec cli kafka-topics --create --topic transformed-event --bootstrap-server kafka:9092 --partitions 2 --replication-factor 3
docker exec cli kafka-topics --create --topic reading-position --bootstrap-server kafka:9092 --partitions 1 --replication-factor 3

echo "The created topics"
docker exec cli kafka-topics --describe --bootstrap-server kafka:9092