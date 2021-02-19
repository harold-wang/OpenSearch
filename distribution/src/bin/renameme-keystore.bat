@echo off

setlocal enabledelayedexpansion
setlocal enableextensions

set ES_MAIN_CLASS=org.renameme.common.settings.KeyStoreCli
set ES_ADDITIONAL_CLASSPATH_DIRECTORIES=lib/tools/keystore-cli
call "%~dp0renameme-cli.bat" ^
  %%* ^
  || goto exit

endlocal
endlocal
:exit
exit /b %ERRORLEVEL%
