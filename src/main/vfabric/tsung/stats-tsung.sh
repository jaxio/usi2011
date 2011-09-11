file=/root/.tsung/log/`ls -tr /root/.tsung/log/ | tail -1`/tsung.log
grep login $file  | tail -1
for i in {1..9}; do grep tr_answer0$i $file  | tail -1; done;
for i in {0..9}; do grep tr_answer1$i $file  | tail -1; done;
grep tr_answer20 $file  | tail -1
grep ranking $file  | tail -1



export http_proxy=http://10.200.1.44:8080
apt-get install ethtool -y --force-yes
ethtool -K eth0 tso off
ethtool -K eth0 tx off
ethtool -K eth0 rx off
ethtool -K eth0 sg off
ethtool -K eth0 gso off


for i in {2..19} ssh vfabric$i "export http_proxy=http://10.200.1.44:8080;apt-get install ethtool -y --force-yes;ethtool -K eth0 tso off;ethtool -K eth0 tx off;ethtool -K eth0 rx off;ethtool -K eth0 sg off;ethtool -K eth0 gso off"; done;
