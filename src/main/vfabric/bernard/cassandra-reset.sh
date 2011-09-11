#!/bin/bash
for (( i = 2 ;  i < 5;  i++ ))
do
  echo adding vfabric$i
  nodes[$i]="vfabric$i"
done

echo -------- Stopping cassandra -------- 
for node in ${nodes[*]}
do
  ssh root@$node '/etc/init.d/cassandra stop'
done

echo -------- rsyncing -------- 
for node in ${nodes[*]}
do
  rsync -e ssh -avz --delete-after --exclude-from "/opt/usi2011_jaxio/game/src/main/vfabric/cloud/game_update_all_nodes_exclude.txt" /opt/usi2011_jaxio root@${node}:/opt/ > /dev/null
  ssh root@$node 'cp /opt/usi2011_jaxio/game/src/main/vfabric/config/cassandra/* /opt/usi2011_jaxio/apache-cassandra-0.7.4/conf'
done

echo -------- destroying cassandra data -------- 
for node in ${nodes[*]}
do
  ssh root@$node 'rm -rf /opt/usi2011_jaxio/cassandra-data'
done

echo -------- configuring yaml -------- 
index=0
for node in ${nodes[*]}
do
  echo "print 2 ** 127 / (${#nodes[*]}) * $index" > tmp.py
  ((index=$index+1))
  cat tmp.py
  new_token=$(python tmp.py)
  echo "New token for node ${node} : "$new_token
  eval "sed 's/initial_token: 0/initial_token: $new_token/g' /opt/usi2011_jaxio/game/src/main/vfabric/config/cassandra/cassandra.yaml > cassandra.yaml.tmp"
  scp cassandra.yaml.tmp root@${node}:/opt/usi2011_jaxio/apache-cassandra-0.7.4/conf/cassandra.yaml
  rm tmp.py
  rm cassandra.yaml.tmp
done
echo

echo -------- starting cassandra -------- 
for node in ${nodes[*]}
do
  ssh root@$node '/etc/init.d/cassandra start'
done

echo -------- creating keyspace -------- 
/opt/usi2011_jaxio/apache-cassandra-0.7.4/bin/cassandra-cli -host vfabric2 -port 9160 --batch --file /opt/usi2011_jaxio/game/src/main/cassandra/init-with-replication-factor-3.script

nodetool -h vfabric2 ring

echo done