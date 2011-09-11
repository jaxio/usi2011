#!/bin/bash
for (( i = 5 ;  i < 11;  i++ ))
do
  echo adding vfabric$i
  nodes[$i]="vfabric$i"
done
echo --------  netty -------- 
echo se lance sur vfabric1 après un sync depuis le poste de dev 
echo -------- -------- 
for node in ${nodes[*]}
do
  echo --------  syncing ${node} -------- 
  rsync -e ssh -avz --delete-after --exclude-from "/opt/usi2011_jaxio/game/src/main/vfabric/cloud/game_update_all_nodes_exclude.txt" /opt/usi2011_jaxio root@${node}:/opt/ > /dev/null 
done

echo -------- restarting netty -------- 
for node in ${nodes[*]}
do
  echo --------  netty ${node} -------- 
  ssh root@$node "
    cp /opt/usi2011_jaxio/game/src/main/vfabric/config/game/* /opt/usi2011_jaxio/game/src/main/resources
    /etc/init.d/game stop; 
    cd /opt/usi2011_jaxio/game;
    export M2_HOME=/opt/usi2011_jaxio/apache-maven-3.0.3;
    export PATH=$M2_HOME/bin:$PATH;
    mvn clean package -DskipTests
    /etc/init.d/game start"
    echo sleeping 7s for the user cache load to occur
done

echo -------- checking VIP logs -------- 
for i in {1..10}
do 
  tail -10 /var/log/messages
  echo "sleeping a bit ..."
  sleep 1
done

echo -------- checking VIP -------- 
ipvsadm -L -n