#!/bin/sh
#
# description: Installs the game
#
mkdir -p /opt/usi2011_jaxio/
mkdir -p /opt/usi2011_jaxio/game
mkdir -p /opt/usi2011_jaxio/cassandra-data
mkdir -p /opt/usi2011_jaxio/log

cd /opt/usi2011_jaxio
export http_proxy=http://10.200.1.44:8080

#
# Cassandra Installation
#
wget http://archive.apache.org/dist/cassandra/0.7.4/apache-cassandra-0.7.4-bin.tar.gz
tar -xzvf apache-cassandra-0.7.4-bin.tar.gz
rm -f apache-cassandra-0.7.4-bin.tar.gz

export CASSANDRA_HOME=/opt/usi2011_jaxio/apache-cassandra-0.7.4
export PATH=$CASSANDRA_HOME/bin:$PATH
echo "export CASSANDRA_HOME=/opt/usi2011_jaxio/apache-cassandra-0.7.4" >> ~/.bashrc
echo "export PATH=$CASSANDRA_HOME/bin:$PATH" >> ~/.bashrc

# Install JNA for Cassandra
cd /opt/usi2011_jaxio/apache-cassandra-0.7.4/lib
wget http://java.net/projects/jna/sources/svn/content/trunk/jnalib/dist/jna.jar?rev=1193
mv jna.jar\?rev\=1193 jna.jar
cd -

#
# Maven Installation
#
wget http://mirror.ibcp.fr/pub/apache//maven/binaries/apache-maven-3.0.3-bin.tar.gz
tar -xzvf apache-maven-3.0.3-bin.tar.gz
rm -f apache-maven-3.0.3-bin.tar.gz
export M2_HOME=/opt/usi2011_jaxio/apache-maven-3.0.3
export PATH=$M2_HOME/bin:$PATH
echo "export M2_HOME=/opt/usi2011_jaxio/apache-maven-3.0.3" >> ~/.bashrc
echo "export PATH=$M2_HOME/bin:$PATH" >> ~/.bashrc

#
# JMeter Installation
#
wget http://mirror.ibcp.fr/pub/apache//jakarta/jmeter/binaries/jakarta-jmeter-2.4.tgz
tar -xzvf jakarta-jmeter-2.4.tgz
rm -f jakarta-jmeter-2.4.tgz
export JMETER_HOME=/opt/usi2011_jaxio/jakarta-jmeter-2.4
echo "export JMETER_HOME=/opt/usi2011_jaxio/jakarta-jmeter-2.4" >> ~/.bashrc

# Clean up
export http_proxy=
