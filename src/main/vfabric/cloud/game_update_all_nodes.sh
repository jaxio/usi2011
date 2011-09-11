#!/bin/bash
#
# description: Updates the game on all the nodes
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
for node in ${nodes[*]}
do
  ssh root@$node '/etc/init.d/cassandra stop'
done
echo

echo "Updating node #1"
/opt/usi2011_jaxio/game/src/main/vfabric/cloud/./game_update.sh

echo "Synchronizing ${#nodes[*]} nodes..."
index=1
for node in ${nodes[*]}
do
  echo "Synchronizing node ${node}"
  rsync -e ssh -avz --delete-after --exclude-from "/opt/usi2011_jaxio/game/src/main/vfabric/cloud/game_update_all_nodes_exclude.txt" /opt/usi2011_jaxio root@${node}:/opt/ > /dev/null
  echo "print 2 ** 127 / (${#nodes[*]} + 1) * $index" > tmp.py
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

echo "Starting all cassandra servers"
/etc/init.d/cassandra start
for node in ${nodes[*]}
do
  ssh root@$node '/etc/init.d/cassandra start'
done
echo

echo "Waiting 5 seconds for the cassandra cluster to gossip and be fully operational"
sleep 5
/opt/usi2011_jaxio/apache-cassandra-0.7.4/bin/nodetool -h localhost ring
echo

echo "Starting all game servers"
/etc/init.d/game start
for node in ${nodes[*]}
do
  ssh root@$node '/etc/init.d/game start'
done
echo

echo "Finished"
