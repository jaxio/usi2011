id=${1:-1}
echo "Launching jmeter script in session_$id.jmx on `hostname`"
/opt/usi2011_jaxio/jakarta-jmeter-2.4/bin/jmeter -n -t /opt/usi2011_jaxio/game/src/main/vfabric/jmeter/session_thinktime_$id.jmx -l /opt/usi2011_jaxio/log/session_thinktime_$id.log > /dev/null &
