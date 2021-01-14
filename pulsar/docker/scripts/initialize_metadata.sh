#!/bin/bash
bin/pulsar initialize-cluster-metadata \
  --cluster pulsar-cluster-1 \
  --zookeeper zookeeper1:2181 \
  --configuration-store zookeeper1:2181 \
  --web-service-url http://broker1:8080,broker2:8080,broker3:8080 \
  --broker-service-url pulsar://broker1:6650,broker2:6650,broker3:6650
