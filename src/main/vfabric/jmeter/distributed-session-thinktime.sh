echo "Launching jmeter users scripts with thinktime on all vfabric and usi, they will hit only vfabric1"
                   nohup /opt/usi2011_jaxio/jakarta-jmeter-2.4/bin/jmeter -n -t /opt/usi2011_jaxio/game/src/main/vfabric/jmeter/session_thinktime_1.jmx -l /opt/usi2011_jaxio/log/session_thinktime_1.log > /dev/null &
ssh root@vfabric2 "nohup /opt/usi2011_jaxio/jakarta-jmeter-2.4/bin/jmeter -n -t /opt/usi2011_jaxio/game/src/main/vfabric/jmeter/session_thinktime_2.jmx -l /opt/usi2011_jaxio/log/session_thinktime_2.log > /dev/null &"
ssh root@vfabric3 "nohup /opt/usi2011_jaxio/jakarta-jmeter-2.4/bin/jmeter -n -t /opt/usi2011_jaxio/game/src/main/vfabric/jmeter/session_thinktime_3.jmx -l /opt/usi2011_jaxio/log/session_thinktime_3.log > /dev/null &"
ssh root@vfabric4 "nohup /opt/usi2011_jaxio/jakarta-jmeter-2.4/bin/jmeter -n -t /opt/usi2011_jaxio/game/src/main/vfabric/jmeter/session_thinktime_4.jmx -l /opt/usi2011_jaxio/log/session_thinktime_4.log > /dev/null &"
ssh root@usi1     "nohup /opt/usi2011_jaxio/jakarta-jmeter-2.4/bin/jmeter -n -t /opt/usi2011_jaxio/game/src/main/vfabric/jmeter/session_thinktime_5.jmx -l /opt/usi2011_jaxio/log/session_thinktime_5.log > /dev/null &"

