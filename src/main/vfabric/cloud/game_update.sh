#!/bin/bash
#
# description: Updates the game on the current node
#

echo "Update configuration files"
cp /opt/usi2011_jaxio/game/src/main/vfabric/config/cassandra/* /opt/usi2011_jaxio/apache-cassandra-0.7.4/conf
cp /opt/usi2011_jaxio/game/src/main/vfabric/config/maven/* /opt/usi2011_jaxio/apache-maven-3.0.3/conf
cp /opt/usi2011_jaxio/game/src/main/vfabric/config/game/* /opt/usi2011_jaxio/game/src/main/resources

echo "Build project"
cd /opt/usi2011_jaxio/game
export M2_HOME=/opt/usi2011_jaxio/apache-maven-3.0.3
export PATH=$M2_HOME/bin:$PATH
mvn clean package -DskipTests

echo "Finished"



