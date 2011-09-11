#!/bin/bash
#
# description: Cassandra service for installation in init.d (auto start at server boot)
#
hostname=`hostname`
case $1 in
start)
        echo "Starting cassandra service on $hostname..."
        /opt/usi2011_jaxio/apache-cassandra-0.7.4/bin/./cassandra -p /var/tmp/cassandra.pid
        ;;
stop)
        echo "Stopping cassandra service on $hostname..."
        pkill -f CassandraDaemon
        ;;
restart)
        echo "Restarting cassandra service on $hostname..."
        pkill -f CassandraDaemon
        /opt/usi2011_jaxio/apache-cassandra-0.7.4/bin/./cassandra -p /var/tmp/cassandra.pid
        ;;
esac
exit 0
