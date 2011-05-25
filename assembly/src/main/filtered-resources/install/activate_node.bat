@ECHO OFF

echo Stopping any running instances...
call bin\stop

echo Starting Reef...
call bin\start

call bin\client -r 10 -d 5 "start-level 90; features:install reef"
call bin\client "start-level 100"