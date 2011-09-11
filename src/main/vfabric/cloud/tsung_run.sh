#!/bin/sh
#
# description: Run Tsung on the cloud
#

cd /opt/usi2011_jaxio/tsung/injecteur/usi2011
git pull

echo "*************************************"
echo "Step 1: create users"
echo "*************************************"
cd create_users
tsung -f tsung_create_users.xml start

echo "*************************************"
echo "Step 2: launch the game"
echo "*************************************"
cd ../game_launch/
echo "this is sparta !" > authentication_key
tsung -f tsung_game_launch.xml start

echo "*************************************"
echo "Step 3: play the game"
echo "*************************************"
cd ../game_play/
tsung -f tsung_game_play.xml start

echo "*************************************"
echo "Step 4: audit"
echo "*************************************"
cd ../game_audit/
echo "this is sparta !" > authentication_key
tsung -f tsung_game_audit.xml start