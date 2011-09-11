#!/bin/sh
#
echo "Syncing files with vfabric, please wait ..."
cd ../../../..
rsync -e ssh -avz --delete-after --exclude="target/" . root@vfabric:/opt/usi2011_jaxio/game
