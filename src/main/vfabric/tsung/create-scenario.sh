#!/bin/bash

##
## Read input
##

read -p "Scenario name ? " name
read -p "How many users to simulate ? " nb_users
read -p "Which users_X.csv to use ? (1/2/3/4/5) ? " users_csv_id
read -p "Use tsung on all nodes but vfabric1 ? (true/false) " distributed_tsung
read -p "Send load only on vfabric1 ? (true/false) " load_on_vfabric1
read -p "Max user per tsung instance: " max_users_per_tsung
read -p "User arrival rate per second ? " arrival_rate_per_second
read -p "Duration of game in minute ? " duration_in_minutes
read -p "Are you okay ? (true/false) : " are_you_okay

if [ "false" == "$are_you_okay" ]; then
  echo "Exiting then ..."
  exit 0;
fi

##
## Building files
##
currentfolder=`dirname $0`
now=`date "+%Y-%m-%d_%H-%M-%S"`
scenario=$name-$now
templates=templates
scenariofolder=scenarios/$scenario

cd $currentfolder
echo "Creating scenario folder $scenariofolder"
mkdir -p $scenariofolder
echo

echo "Copying templates"
cp -r $templates/* $scenariofolder/
echo

echo "Creating data file for $nb_users users"
./prepare_data.py $nb_users
mv injector_1/data $scenariofolder
rm -rf injector_1
echo


##
## Build clients config
##
if [ "true" == "$distributed_tsung" ]; then
  clients="<client host=\"localhost\" maxusers=\"$max_users_per_tsung\"\\/><client host=\"vfabric2\" maxusers=\"$max_users_per_tsung\"\\/><client host=\"vfabric3\" maxusers=\"$max_users_per_tsung\"\\/><client host=\"vfabric4\" use_controller_vm=\"$use_controller_vm\"\\/>"
else
  clients="<clients><client host=\"localhost\" maxusers=\"$max_users_per_tsung\"/></clients>"
fi

##
## Build server config
##
if [ "true" == "$load_on_vfabric1" ]; then
 servers="<servers><server host=\"vfabric1\" port=\"80\" type=\"tcp\" \\/><\\/servers>"
else
 servers="<servers><server host=\"vfabric1\" port=\"80\" type=\"tcp\" \\/><server host=\"vfabric2\" port=\"80\" type=\"tcp\" \\/><server host=\"vfabric3\" port=\"80\" type=\"tcp\" \\/><server host=\"vfabric4\" port=\"80\" type=\"tcp\" \\/><\\/servers>"
fi


function replace {
 echo "  replacing $1 with $2 in $3"  
 /usr/bin/perl -p -i -e "s/$1/$2/g" $3
}

function update_config {
 file=$1
 echo "replacing in $1"
 replace "%%CLIENTS%%" "$clients" $file;
 replace "%%SERVERS%%" "$servers" $file;
 replace "%%USERS_CSV_FILE%%" "\.\.\\/\.\.\\/\.\.\\/\.\.\\/data\\/users_$users_csv_id\.csv", $file;
 replace "%%ARRIVAL_DURATION_IN_MINUTES%%" "$arrival_duration_in_minutes" $file;
 replace "%%ARRIVAL_RATE_PER_SECOND%%" "$arrival_rate_per_second" $file;
}

cd $scenariofolder
echo "Update the templates with configuration in folder $scenariofolder"
for file in `ls *.xml`; do
 update_config $file
done
echo "done"
echo

read -p "Launching scripts ? (true/false) " are_you_okay
if [ "false" == "$are_you_okay" ]; then
  echo tsung -f launch.xml -l launch start
  echo tsung -f launch.xml -l play start
  echo tsung -f audit.xml -l audit start
else
  echo "You can launch them manually like this"
  echo 
  echo cd $currentfolder/$scenariofolder
  echo tsung -f launch.xml -l launch start
  echo tsung -f launch.xml -l play start
  echo tsung -f audit.xml -l audit start
fi


