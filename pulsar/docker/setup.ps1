Write-Host "Start the infra"
Write-Host "Start zookeeper cluster"
docker-compose up -d zookeeper1 zookeeper2 zookeeper3

Write-Host "Set up the metadata"
docker-compose up setup

Write-Host "Start Bookkeeper and broker and database"
docker-compose up -d bookie1 bookie2 bookie3 broker1 broker2 cli postgres_db adminer

[string]$status1 = $(docker inspect --format='{{.State.Health.Status}}' broker1)
[string]$status2 = $(docker inspect --format='{{.State.Health.Status}}' broker2)

while (($status1 -ne "healthy") -or ($status2 -ne "healthy"))
{
    Write-Host -NoNewLine "`r$a% Waiting for brokers to be healthy"
    Start-Sleep -Milliseconds 10
    $status1 = $(docker inspect --format='{{.State.Health.Status}}' broker1)
    $status2 = $(docker inspect --format='{{.State.Health.Status}}' broker2)
}
Write-Host
Write-Host "Brokers are now fully started"

Write-Host "Set the Bookkeeper quorum for the namespace"
docker exec cli bin/pulsar-admin namespaces set-persistence public/default --bookkeeper-ack-quorum 2 --bookkeeper-ensemble 3 --bookkeeper-write-quorum 3 --ml-mark-delete-max-rate 0
Write-Host "Set the retention period for messages on a topic to 1 week"
docker exec cli bin/pulsar-admin namespaces set-retention public/default --size -1 --time 1w


Write-Host "Create necessary topics"
docker exec cli bin/pulsar-admin topics create persistent://public/default/reading-position
docker exec cli bin/pulsar-admin topics create-partitioned-topic persistent://public/default/raw-event --partitions 2
docker exec cli bin/pulsar-admin topics create-partitioned-topic persistent://public/default/transformed-event --partitions 2


