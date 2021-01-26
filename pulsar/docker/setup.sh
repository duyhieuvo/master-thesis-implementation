#!/usr/bin/env bash

echo "Start the infra"
echo "Start zookeeper cluster"
docker-compose up -d zookeeper1 zookeeper2 zookeeper3
echo "Set up the metadata"
docker-compose up setup
echo "Start Bookkeeper and broker and database"
docker-compose up -d bookie1 bookie2 bookie3 broker1 broker2 cli postgres_db adminer
while [[ $(docker inspect -f {{.State.Health.Status}} broker1) != *healthy* ]] || [[ $(docker inspect -f {{.State.Health.Status}} broker2) != *healthy* ]]; do
    echo -ne "\r\033[0KWaiting for brokers to be healthy";
    sleep 1
    echo -n "."
    sleep 1
    echo -n "."
    sleep 1
    echo -n "."
done
echo "Brokers are now fully started"

echo "Set the Bookkeeper quorum for the namespace"
winpty docker exec cli bin/pulsar-admin namespaces set-persistence public/default --bookkeeper-ack-quorum 2 --bookkeeper-ensemble 3 --bookkeeper-write-quorum 3 --ml-mark-delete-max-rate 0
echo "Set the retention period for messages on a topic to 1 week"
winpty docker exec cli bin/pulsar-admin namespaces set-retention public/default --size -1 --time 1w
#echo "Enable message deduplication to allow idempotent producer"
#winpty docker exec cli bin/pulsar-admin namespaces set-deduplication public/default --enable

echo "Create necessary topics"
winpty docker exec cli bin/pulsar-admin topics create persistent://public/default/reading-position
winpty docker exec cli bin/pulsar-admin topics create-partitioned-topic persistent://public/default/raw-event --partitions 2
winpty docker exec cli bin/pulsar-admin topics create-partitioned-topic persistent://public/default/transformed-event --partitions 2

