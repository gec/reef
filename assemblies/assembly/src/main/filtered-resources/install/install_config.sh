set -e

config_file=

load() {
    echo "Stopping running instances..."
    bin/stop > /dev/null 2>&1 || true

    echo "Starting Reef..."
    bin/start

    # it can take a long time to setup the ssh session the first time karaf is run on a slow machine
    bin/client -p `cat etc/karaf_pfile` -r 10 -d 5  "start-level 90; features:install reef"
    bin/client -p `cat etc/karaf_pfile`   "reef:resetdb; start-level 91"

    echo "Sleeping for 5 seconds before loading"
    sleep 5

    echo "Loading configuration..."
    bin/client -p `cat etc/karaf_pfile` "reef:login -p core core; reef:load $config_file"
    bin/client -p `cat etc/karaf_pfile` "start-level 100"
    # bin/stop
}

if [ -z $1 ]
then
echo "Usage: ./install_reef.sh (config file)"
else

config_file=$1
load

fi


