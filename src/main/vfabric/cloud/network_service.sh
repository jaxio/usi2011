#!/bin/bash
#
# description: setup network
#

hostname=`hostname`
ip=`ifconfig -a | grep addr | grep 192.168 | cut -d ":" -f2 | cut -d " " -f1`
ethtool -K eth0 tso off
ethtool -K eth0 tx off
ethtool -K eth0 rx off
ethtool -K eth0 sg off
ethtool -K eth0 gso off

# vip
if [ "$hostname" == "vfabric1" ] ; then 
echo install keepalived on master
/etc/init.d/keepalived start
fi;
if [ "$hostname" == "vfabric1" ] ; then 
echo install keepalived on slave
/etc/init.d/keepalived start
fi;


# real server
ifconfig lo:21 192.168.1.21 netmask 255.255.255.255
route del -net 192.168.1.0 netmask 255.255.255.0
ifconfig $ip netmask 255.255.255.224
