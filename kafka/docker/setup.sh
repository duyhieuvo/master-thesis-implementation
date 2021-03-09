#!/usr/bin/env bash

echo "Start the infra"
docker-compose up -d zookeeper1 zookeeper2 zookeeper3 kafka1 kafka2 kafka3 cli postgres_db adminer
while [[ $(docker inspect -f {{.State.Health.Status}} kafka1) != *healthy* ]] || [[ $(docker inspect -f {{.State.Health.Status}} kafka2) != *healthy* ]] || [[ $(docker inspect -f {{.State.Health.Status}} kafka3) != *healthy* ]]; do
    echo -ne "\r\033[0KWaiting for brokers to be healthy";
    sleep 1
    echo -n "."
    sleep 1
    echo -n "."
    sleep 1
    echo -n "."
done
echo "Brokers are now fully started"

echo "Create necessary topics"
docker exec cli kafka-topics --create --topic raw-event --bootstrap-server kafka:9092 --partitions 2 --replication-factor 3
docker exec cli kafka-topics --create --topic transformed-event --bootstrap-server kafka:9092 --partitions 2 --replication-factor 3
docker exec cli kafka-topics --create --topic reading-position --bootstrap-server kafka:9092 --partitions 1 --replication-factor 3

echo "The created topics"
docker exec cli kafka-topics --describe --bootstrap-server kafka:9092