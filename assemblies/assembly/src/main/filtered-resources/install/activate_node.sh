set -e

echo "Stopping running instances..."
bin/stop > /dev/null 2>&1 || true

echo "Starting Reef..."
bin/start

bin/client -p `cat etc/karaf_pfile` -r 10 -d 5  "start-level 90; features:install reef"
bin/client -p `cat etc/karaf_pfile` "start-level 100"
