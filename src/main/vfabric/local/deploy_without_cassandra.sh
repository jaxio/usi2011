#!/bin/sh
#
# description: Deploy the application without cassandra
#
echo "Syncing files with vfabric, please wait ..."
echo "Cassandra will NOT be restarted"
cd ../../../..
rsync -e ssh -avz --delete-after --exclude="target/" . root@vfabric:/opt/usi2011_jaxio/game
cd -
echo "Connecting to vfabric"
ssh root@vfabric '/opt/usi2011_jaxio/game/src/main/vfabric/cloud/./game_update_without_cassandra_all_nodes.sh'
