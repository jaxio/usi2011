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
echo "Waiting 2 seconds"
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
