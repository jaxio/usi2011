#!/bin/bash
#
# description: reset the game and cassandra on all the nodes
#
# This should be run on the front node (vfabric1)
#

# List the nodes
for (( node = 1 ;  node < 20;  node++ ))
do
  nodes[$node]="vfabric$(($node +1))"
done

echo "------------- NODES -----------------"
echo "Nodes list (excluding the front node)"
for node in ${nodes[*]}
do
     echo "$node"
done
echo "Number of nodes: ${#nodes[*]}"
echo "-------------------------------------"
echo

echo "Stopping all game servers"
/etc/init.d/game stop
for node in ${nodes[*]}
do
  ssh root@$node '/etc/init.d/game stop'
done
echo
echo "Stopping all cassandra servers"
/etc/init.d/cassandra stop
rm -rf /opt/usi2011_jaxio/cassandra-data
killall java > /dev/null
for node in ${nodes[*]}
do
  ssh root@$node '/etc/init.d/cassandra stop'
  ssh root@$node 'rm -rf /opt/usi2011_jaxio/cassandra-data'
  ssh root@$node '/usr/bin/killall java > /dev/null'
done
echo

echo "Updating node #1"
/opt/usi2011_jaxio/game/src/main/vfabric/cloud/./game_update.sh

echo "Synchronizing ${#nodes[*]} nodes..."

for node in ${nodes[*]}
do
  echo "Synchronizing node ${node}"
  rsync -e ssh -avz --delete-after --exclude-from "/opt/usi2011_jaxio/game/src/main/vfabric/cloud/game_update_all_nodes_exclude.txt" /opt/usi2011_jaxio root@${node}:/opt/ > /dev/null
done
echo

# List the nodes
for (( node = 2 ;  node < 7;  node++ ))
do
  nodescas[$node]="vfabric$(($node))"
done
index=0
for node in ${nodescas[*]}
do
  echo "print 2 ** 127 / (${#nodescas[*]}) * $index" > tmp.py
  ((index=$index+1))
  cat tmp.py
  new_token=$(python tmp.py)
  echo "New token for node ${node} : "$new_token
  eval "sed 's/initial_token: 0/initial_token: $new_token/g' /opt/usi2011_jaxio/game/src/main/vfabric/config/cassandra/cassandra.yaml > cassandra.yaml.tmp"
  scp cassandra.yaml.tmp root@${node}:/opt/usi2011_jaxio/apache-cassandra-0.7.4/conf/cassandra.yaml
  rm tmp.py
  rm cassandra.yaml.tmp
done
echo

echo "Removing cassandra data and logs"
echo "Removing on `hostname`" 
rm -rf /opt/usi2011_jaxio/log/*
rm -rf /opt/usi2011_jaxio/cassandra-data
for node in ${nodes[*]}
do
  echo "Removing on $node"
  ssh root@$node 'rm -rf /opt/usi2011_jaxio/log/*'
  ssh root@$node 'rm -rf /opt/usi2011_jaxio/cassandra-data'
done
echo

echo "Starting all cassandra servers"

for node in ${nodescas[*]}
do
  ssh root@$node '/etc/init.d/cassandra start'
done
echo
echo "Waiting 5 seconds for the cassandra cluster to gossip and be fully operational"
sleep 5
echo

/opt/usi2011_jaxio/apache-cassandra-0.7.4/bin/nodetool -h vfabric2 ring
echo
echo "Creating keyspace"
/opt/usi2011_jaxio/apache-cassandra-0.7.4/bin/cassandra-cli -host vfabric2 -port 9160 --batch --file /opt/usi2011_jaxio/game/src/main/cassandra/init-with-replication-factor-3.script
echo "Waiting 2 seconds for the cassandra cluster to replicate"
sleep 2
echo

echo "Starting all game servers"
/etc/init.d/game start
for node in ${nodes[*]}
do
  ssh root@$node '/etc/init.d/game start'
done
echo


echo "Finished"
