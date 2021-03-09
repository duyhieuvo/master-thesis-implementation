#!/usr/bin/env bash

echo "Start the infra"
docker-compose up -d nats-streaming-1 nats-streaming-2 nats-streaming-3 postgres_db adminer
echo "Waiting for stream servers to be healthy"
sleep 5s



