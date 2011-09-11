echo "Create known users using jmeter"
					     /opt/usi2011_jaxio/jakarta-jmeter-2.4/bin/jmeter -n -t /opt/usi2011_jaxio/game/src/main/vfabric/jmeter/known_users.jmx -l /opt/usi2011_jaxio/log/insert_known_users.log 
echo "Create the users using all servers"
				   nohup /opt/usi2011_jaxio/jakarta-jmeter-2.4/bin/jmeter -n -t /opt/usi2011_jaxio/game/src/main/vfabric/jmeter/insert_users_1.jmx -l /opt/usi2011_jaxio/log/insert_user_1.log > /dev/null &
ssh root@vfabric2 "nohup /opt/usi2011_jaxio/jakarta-jmeter-2.4/bin/jmeter -n -t /opt/usi2011_jaxio/game/src/main/vfabric/jmeter/insert_users_2.jmx -l /opt/usi2011_jaxio/log/insert_user_2.log > /dev/null &"
ssh root@vfabric3 "nohup /opt/usi2011_jaxio/jakarta-jmeter-2.4/bin/jmeter -n -t /opt/usi2011_jaxio/game/src/main/vfabric/jmeter/insert_users_3.jmx -l /opt/usi2011_jaxio/log/insert_user_3.log > /dev/null &"
ssh root@vfabric4 "nohup /opt/usi2011_jaxio/jakarta-jmeter-2.4/bin/jmeter -n -t /opt/usi2011_jaxio/game/src/main/vfabric/jmeter/insert_users_4.jmx -l /opt/usi2011_jaxio/log/insert_user_4.log > /dev/null &"
ssh root@usi1     "nohup /opt/usi2011_jaxio/jakarta-jmeter-2.4/bin/jmeter -n -t /opt/usi2011_jaxio/game/src/main/vfabric/jmeter/insert_users_5.jmx -l /opt/usi2011_jaxio/log/insert_user_5.log > /dev/null &"
