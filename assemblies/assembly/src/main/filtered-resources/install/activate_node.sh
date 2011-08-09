set -e

echo "Stopping running instances..."
bin/stop > /dev/null 2>&1 || true

echo "Starting Reef..."
bin/start

bin/client -r 10 -d 5  "start-level 90; features:install reef"
bin/client   "start-level 100"
