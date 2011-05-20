set -e

load() {
    echo "Stopping running instances..."
    bin/stop > /dev/null 2>&1 || true

    echo "Booting Reef..."
    bin/start

    # it can take a long time to setup the ssh session the first time karaf is run on a slow machine
    bin/client -r 10 -d 5  "start-level 90; features:install reef"
    bin/client   "reef:resetdb; start-level 91"

    echo "Loading configuration..."
    bin/client   "reef:login -p core core; reef:load $1"
    bin/client   "start-level 100"
}

if [ -z $1 ]
then
echo "Usage: ./install_reef.sh (config file)"
else
if [ -f $1 ]
then

load

else

echo "$1 not found."

fi
fi


