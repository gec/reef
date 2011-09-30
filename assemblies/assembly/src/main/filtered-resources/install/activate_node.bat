@ECHO OFF

echo Stopping any running instances...
call bin\stop

echo Starting Reef...
call bin\start

set /p KARAF_PASS= < etc/karaf_pfile

call bin\client -p %KARAF_PASS% -r 10 -d 5 "start-level 90; features:install reef"
call bin\client -p %KARAF_PASS% "start-level 100"