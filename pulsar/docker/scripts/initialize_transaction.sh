#!/usr/bin/env bash

bin/pulsar initialize-transaction-coordinator-metadata \
  --cluster pulsar-cluster-1 \
  --configuration-store zookeeper1:2181,zookeeper2:2181,zookeeper3:2181
