#!/bin/bash
#
# description: Updates the game on all the nodes but do not touch cassandra
#
# This should be run on the front node (vfabric1)
#

echo "Redeploy only game"
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

echo "Updating node #1"
/opt/usi2011_jaxio/game/src/main/vfabric/cloud/./game_update.sh

echo "Synchronizing ${#nodes[*]} nodes..."
index=1
for node in ${nodes[*]}
do
echo "Synchronizing node ${node}"
rsync -e ssh -avz --delete-after --exclude-from "/opt/usi2011_jaxio/game/src/main/vfabric/cloud/game_update_all_nodes_exclude.txt" /opt/usi2011_jaxio root@${node}:/opt/ > /dev/null
done
echo

echo "Starting all game servers"
/etc/init.d/game start
for node in ${nodes[*]}
do
ssh root@$node '/etc/init.d/game start'
done
echo

echo "Finished"