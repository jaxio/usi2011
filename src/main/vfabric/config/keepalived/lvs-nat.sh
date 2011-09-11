# lvs-NAT
net.ipv4.conf.all.send_redirects = 0
net.ipv4.conf.default.send_redirects = 0
net.ipv4.conf.eth0.send_redirects = 0


route add default gw 192.168.1.1
route del default gw 192.168.1.254
route del -net 192.168.1.0 netmask 255.255.255.0 dev eth0

root@vfabric11:~# route 
Kernel IP routing table
Destination     Gateway         Genmask         Flags Metric Ref    Use Iface
default         vfabric1.locald 0.0.0.0         UG    0      0        0 eth0
