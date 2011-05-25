set -ex

reefdir=totalgrid-reef-*

$reefdir/bin/stop > /dev/null 2>&1 || true

rm -rf $reefdir
tar -xvf assembly/target/totalgrid-reef*.tar.gz

cd $reefdir && ./install_config.sh samples/integration/config.xml
