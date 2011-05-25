@ECHO OFF
IF "%1"=="" GOTO Usage
echo Using file %1

echo Stopping any running instances...
call bin\stop

echo Booting Reef...
call bin\start

call bin\client -r 10 -d 5 "start-level 90; features:install reef"
call bin\client "reef:resetdb; start-level 91"
call bin\client "reef:login -p core core; reef:load %1"
call bin\client "start-level 100"

GOTO End


:Usage
echo Usage: install_reef.bat (config file)

:End