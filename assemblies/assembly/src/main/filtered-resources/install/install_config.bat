@ECHO OFF
IF "%1"=="" GOTO Usage
echo Using file %1

echo Stopping any running instances...
call bin\stop

echo Starting Reef...
call bin\start


set /p KARAF_PASS= < etc/karaf_pfile

call bin\client -p %KARAF_PASS% -r 10 -d 5 "start-level 90; features:install reef"
call bin\client -p %KARAF_PASS% "reef:resetdb; start-level 91"
call bin\client -p %KARAF_PASS% "reef:login -p core core; reef:load %1"
call bin\client -p %KARAF_PASS% "start-level 100"

GOTO End


:Usage
echo Usage: install_reef.bat (config file)

:End