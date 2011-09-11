#!/bin/sh
#
# description: Deploy the application
#
# This script deploy the application from a local computer to vfabric
#
echo "ATTENTION IL NE FAUT PAS LE LANCER"
echo "That means all cassandra data, all logs will be wiped out"
echo "Really want to do that ? (yes/no)"
read yesno


#echo "Syncing files with vfabric, please wait ..."
#cd ../../../..
#rsync -e ssh -avz --delete-after --exclude="target/" . root@vfabric:/opt/usi2011_jaxio/game
#cd -
#echo "Connecting to vfabric"
#ssh root@vfabric '/opt/usi2011_jaxio/game/src/main/vfabric/cloud/./game_update_all_nodes.sh'
