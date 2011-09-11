#!/bin/sh
#
# description: reset the application
#
# This script reset the application from a local computer to vfabric
#
echo "Do you really want to reset the cluster ?"
echo "That means all cassandra data, all logs will be wiped out"
echo "Really want to do that ? (yes/no)"
read yesno

if [ "yes" == "$yesno" ]; then
 echo "Syncing files with vfabric, please wait ..."
 cd ../../../..
 rsync -e ssh -avz --delete-after --exclude="target/" . root@vfabric:/opt/usi2011_jaxio/game
 cd -
 echo "Connecting to vfabric"
 ssh root@vfabric '/opt/usi2011_jaxio/game/src/main/vfabric/cloud/./game_reset_all_nodes.sh'
else
  echo "nothing was done"
fi
