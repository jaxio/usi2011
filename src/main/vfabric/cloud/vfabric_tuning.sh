#!/bin/sh
#
# description: Performance tuning of the standard vFabric servers
#

# Remove vfabric installations
cd /root/rc
./vfabric-stopall
cd -
update-rc.d -f ers-apache remove
update-rc.d -f gemfire-jmx remove
update-rc.d -f hostname-vfabric remove
update-rc.d -f hyperic-server remove
update-rc.d -f tcserver-tc01 remove
update-rc.d -f gemfire-gf01 remove
update-rc.d -f gemfire-locator remove
update-rc.d -f hyperic-agent remove
update-rc.d -f rabbitmq-server remove
update-rc.d -f vfabric-stopall remove
rm -rf /home/user/vfabric
rm -rf /var/spool/cron/crontabs/user

# Update the system
export http_proxy=http://10.200.1.44:8080
apt-get update
apt-get upgrade -y --force-yes
apt-get install htop dstat tcptrack ethtool netperf nmon -y --force-yes

# Clean up
apt-get autoremove -y
apt-get clean

# Add NTP
echo "ntpdate -u 10.50.80.7" > /etc/cron.hourly/ntpdate
chmod 755 /etc/cron.hourly/ntpdate
crontab -l | (cat;echo "* * * * * /usr/sbin/ntpdate -u 10.50.80.7 >> /opt/usi2011_jaxio/log/ntp.log") | crontab

# Tune the Linux Kernel
echo "#Added by Jaxio Team" >> /etc/sysctl.conf
echo "kernel.sem = 250 32000 100 128" >> /etc/sysctl.conf
echo "kernel.shmall = 2097152" >> /etc/sysctl.conf
echo "kernel.shmmax = 2147483648" >> /etc/sysctl.conf
echo "kernel.shmmni = 4096" >> /etc/sysctl.conf
echo "fs.file-max = 16777216" >> /etc/sysctl.conf
echo "vm.swappiness = 1" >> /etc/sysctl.conf
echo "vm.vfs_cache_pressure = 50" >> /etc/sysctl.conf
echo "net.core.rmem_default = 16777216" >> /etc/sysctl.conf
echo "net.core.rmem_max = 16777216" >> /etc/sysctl.conf
echo "net.core.wmem_default = 16777216" >> /etc/sysctl.conf
echo "net.core.wmem_max = 16777216" >> /etc/sysctl.conf
echo "net.ipv4.tcp_rmem = 10240 87380 16777216" >> /etc/sysctl.conf
echo "net.ipv4.tcp_wmem = 10240 87380 16777216" >> /etc/sysctl.conf
echo "net.ipv4.tcp_no_metrics_save = 1" >> /etc/sysctl.conf
echo "net.ipv4.tcp_window_scaling = 1" >> /etc/sysctl.conf
echo "net.ipv4.tcp_timestamps = 1" >> /etc/sysctl.conf
echo "net.ipv4.tcp_sack = 1" >> /etc/sysctl.conf
echo "net.core.netdev_max_backlog = 5000" >> /etc/sysctl.conf
echo "net.ipv4.tcp_fin_timeout=15" >> /etc/sysctl.conf
echo "net.core.netdev_max_backlog=10000" >> /etc/sysctl.conf
echo "net.core.somaxconn=10000" >> /etc/sysctl.conf

echo "# lvs-DR" >> /etc/sysctl.conf
echo "net.ipv4.conf.eth0.arp_ignore = 1" >> /etc/sysctl.conf
echo "net.ipv4.conf.eth0.arp_announce = 2" >> /etc/sysctl.conf
echo "net.ipv4.conf.all.arp_ignore = 1" >> /etc/sysctl.conf
echo "net.ipv4.conf.all.arp_announce = 2" >> /etc/sysctl.conf

# Remove limits for root
echo "root hard memlock unlimited" >> /etc/security/limits.conf
echo "root soft memlock unlimited" >> /etc/security/limits.conf
# 1048576 is the kernel maximum value
echo "root hard nofile 1048576" >> /etc/security/limits.conf
echo "root soft nofile 1048576" >> /etc/security/limits.conf

# Allow SSH access (the keys used are the one from the front server)
mkdir /root/.ssh
echo "ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAypEKG9ECs5olBv04RTak0FGpdqcVBJIBQClvpARWXyEnrbiufRep3qZvurm6PYJj+X61ostjtc/1e0rHDu6IPjpvI7hq3gZZKLxGmPK6Z+Poi7oWgWzji0m1POkr0If9Qu4EdEliWlaUt3lYrntW5HMBeN9fGmW4/FV3GcEQhcXgxNSs/5sKHPvXtoP0rYVf7uYA1ld58h4B+v0lzpeag9zpLijru/neDNcUIhRow1e0PB+FiPTQeKxpKfFAMwc1p1+Z+5wIu0SPgGZnhvEVhmC9GBC5qfKPt7j0p7IWvtsG31WWPTsyNg4g/yu9Vl5N1SCfoDFx0c6sxMkskToKYQ== root@vfabric1" >> /root/.ssh/authorized_keys
sed -i 's/PermitRootLogin no/PermitRootLogin yes/g' /etc/ssh/sshd_config 

# Prepare the startup scripts that will be added when the game is copied to the server
ln -s /opt/usi2011_jaxio/game/src/main/vfabric/cloud/cassandra_service.sh /etc/init.d/cassandra
ln -s /etc/init.d/cassandra /etc/rc1.d/K99cassandra
ln -s /etc/init.d/cassandra /etc/rc2.d/S99cassandra
ln -s /opt/usi2011_jaxio/game/src/main/vfabric/cloud/game_service.sh /etc/init.d/game
ln -s /etc/init.d/game /etc/rc1.d/K99game
ln -s /etc/init.d/game /etc/rc2.d/S99game

reboot
