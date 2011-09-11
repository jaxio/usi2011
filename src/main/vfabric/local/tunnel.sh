#!/bin/sh
#
# description: Creates a tunnel to the vfabric1 server
#

echo "Tunneling vfabric game server"
echo "Wait for the log to appear then by point your browser to: http://127.0.0.1:9091/"
ssh -L 9091:localhost:80 root@vfabric 'tail -f /opt/usi2011_jaxio/log/game.log'
