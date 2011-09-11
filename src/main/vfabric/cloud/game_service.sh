#!/bin/bash
#
# description: Game service for installation in init.d (auto start at server boot)
#
# see http://java.sun.com/performance/reference/whitepapers/tuning.html#section4.2.6

JVM_OPTS="-verbose:gc"
JVM_OPTS="$JVM_OPTS -Xloggc:/opt/usi2011_jaxio/log/gc-game.log"
JVM_OPTS="$JVM_OPTS -XX:+UseParNewGC" 
JVM_OPTS="$JVM_OPTS -XX:+UseConcMarkSweepGC" 
JVM_OPTS="$JVM_OPTS -XX:+CMSParallelRemarkEnabled" 
JVM_OPTS="$JVM_OPTS -XX:SurvivorRatio=8" 
JVM_OPTS="$JVM_OPTS -XX:MaxTenuringThreshold=1"
JVM_OPTS="$JVM_OPTS -XX:CMSInitiatingOccupancyFraction=75"
JVM_OPTS="$JVM_OPTS -XX:+UseCMSInitiatingOccupancyOnly"
JVM_OPTS="$JVM_OPTS -Xms3500m -Xmx3500m"

# LVS conf real server - along with /etc/sysctl/
# works only if you have a server on port 80
ifconfig lo:21 192.168.1.21 netmask 255.255.255.255


# perf
ethtool -K eth0 tso off;ethtool -K eth0 tx off;ethtool -K eth0 rx off;ethtool -K eth0 sg off;ethtool -K eth0 gso off;

case $1 in
start)

echo "Starting game service on $hostname..."
nohup java $JVM_OPTS -jar /opt/usi2011_jaxio/game/target/usi2011-jaxio-0.0.1-SNAPSHOT.jar  > /dev/null &
;;
stop)
echo "Stopping game service on $hostname..."
pkill -f usi2011-jaxio-0.0.1-SNAPSHOT.jar
;;
restart)
echo "Restarting game service on $hostname..."
pkill -f usi2011-jaxio-0.0.1-SNAPSHOT.jar
nohup java $JVM_OPTS -jar /opt/usi2011_jaxio/game/target/usi2011-jaxio-0.0.1-SNAPSHOT.jar $JVM_OPTS > /dev/null &
;;
esac
exit 0