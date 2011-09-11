# lvs-DR
net.ipv4.conf.eth0.arp_ignore = 1
net.ipv4.conf.eth0.arp_announce = 2
net.ipv4.conf.all.arp_ignore = 1
net.ipv4.conf.all.arp_announce = 2


ifconfig lo:210 192.168.1.210 netmask 255.255.255.255 network 192.168.1.210 broadcast 192.168.1.210