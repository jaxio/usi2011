#!/bin/bash
#
# description: Install the game on all the nodes
#
# This should be run on the front node (vfabric1)
#

# List the nodes
for (( node = 1 ;  node < 19;  node++ ))
do
  nodes[$node]="vfabric$(($node +1))"
done
nodes[20]="usi1"

/opt/usi2011_jaxio/game/src/main/vfabric/cloud/./game_install.sh
echo "------------- NODES -----------------"
echo "Nodes list (excluding the front node)"
for node in ${nodes[*]}
do
  rsync -e ssh -avz --delete-after --exclude-from "/opt/usi2011_jaxio/game/src/main/vfabric/cloud/game_update_all_nodes_exclude.txt" /opt/usi2011_jaxio root@${node}:/opt/ > /dev/null
  echo ssh root@$node '/opt/usi2011_jaxio/game/src/main/vfabric/cloud/./game_install.sh'
  ssh root@$node '/opt/usi2011_jaxio/game/src/main/vfabric/cloud/./game_install.sh'
done
echo "Number of nodes: ${#nodes[*]}"
echo "-------------------------------------"
echo

