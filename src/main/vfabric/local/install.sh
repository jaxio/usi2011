#!/bin/sh
#
# description: Deploy the application
#
# This script deploy the application from a local computer to vfabric
#
echo "Syncing files with vfabric, please wait ..."
cd ../../../..
rsync -e ssh -avz --delete-after --exclude="target/" . root@vfabric:/opt/usi2011_jaxio/game
cd -
echo "Connecting to vfabric"
ssh root@vfabric '/opt/usi2011_jaxio/game/src/main/vfabric/cloud/./game_install_all_nodes.sh'
