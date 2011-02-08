chmod +x bin/*

bin/stop > /dev/null 2>&1 || true

bin/start

sleep 5

bin/client   "features:addurl file:reef-feature.xml"
bin/client   "start-level 90; features:install reef"
bin/client   "reef:resetdb; start-level 91"

bin/client   "reef:login core core; reef:load -benchmark samples/integration/config.xml"
bin/client   "start-level 100"

