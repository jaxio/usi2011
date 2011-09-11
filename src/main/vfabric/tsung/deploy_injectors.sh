#!/bin/bash
#
# description: Updates innjectors
#
# This should be run on usi1
#

# List the nodes
for (( node = 5 ;  node < 10;  node++ ))
do
  nodes[$node]="vfabric$(($node))"
done

echo "------------- NODES -----------------"
echo "Nodes list"
for node in ${nodes[*]}
do
     echo "$node"
done
echo "Number of nodes: ${#nodes[*]}"
echo "-------------------------------------"
echo
echo "Synchronizing ${#nodes[*]} nodes..."
index=1
for node in ${nodes[*]}
do
  echo "Synchronizing node ${node}"
  rsync -e ssh -avh --delete /root/jaxio root@${node}:/root/
  ssh root@${node} "sed -i -e 's/injector_x/injector_${index}/' /root/jaxio/game_play/tsung_game_play_quick_stop_usi_team10.xml"
((index=$index+1))
done
echo "Finished" 