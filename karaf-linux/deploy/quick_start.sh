bin/start

sleep 5
	
bin/client   "features:addurl file:`pwd`/reef-feature.xml"
bin/client   "features:install reef-core"
bin/client   "reef:resetdb"
bin/client   "features:install reef"
	
bin/client   "reef:login core core; reef:load -benchmark samples/two_substations.xml"

