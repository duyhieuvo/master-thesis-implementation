.\setup.ps1

#Start and wait until event generator publishes all 1000 events
docker-compose up event-generator

#Start the first stream processor instance, all 1000 events will be pushed to the queue on this instance
docker-compose up -d stream-processor-2-partition-revoked

Start-Sleep -Milliseconds 3000

#Start the second stream process instance, messages on the partition assigned to this instance which have not been processed and acknowledged by the other instance will be redelivered
#Therefore, there are temporarily two instances processing the same set of messages
docker-compose up -d stream-processor-1
docker-compose up -d stream-aggregator