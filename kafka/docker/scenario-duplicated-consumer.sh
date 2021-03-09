#!/usr/bin/env bash

./setup.sh

docker-compose up event-generator
docker-compose up -d stream-processor-2-partition-revoked
sleep 3s
docker-compose up -d stream-processor-1
docker-compose up -d stream-aggregator