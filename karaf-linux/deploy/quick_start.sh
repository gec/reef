set -e

chmod +x bin/*

bin/stop > /dev/null 2>&1 || true

bin/start

# it can take a long time to setup the ssh session the first time karaf is run on a slow machine
bin/client -r 10 -d 5  "features:addurl file:reef-feature.xml"
bin/client   "start-level 90; features:install reef"
bin/client   "reef:resetdb; start-level 91"

bin/client   "reef:login core core; reef:load -benchmark samples/integration/config.xml"
bin/client   "start-level 100"

