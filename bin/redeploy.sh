set -ex

reefdir=totalgrid-reef-*

$reefdir/bin/stop > /dev/null 2>&1 || true

rm -rf $reefdir
mkdir -p assembly/target/temp
mv assembly/target/totalgrid-reef-console* assembly/target/temp
tar -xvf assembly/target/totalgrid-reef*.tar.gz
mv assembly/target/temp/* assembly/target

cd $reefdir && ./install_config.sh samples/integration/config.xml
