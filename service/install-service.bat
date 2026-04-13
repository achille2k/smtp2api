@echo off
setlocal
cd /d "%~dp0"

if not exist smtp2api.xml (
  echo Missing smtp2api.xml
  exit /b 1
)

if not exist smtp2api.jar (
  echo Copying built jar from target\smtp2api.jar
  copy /Y "%~dp0..\target\smtp2api.jar" "%~dp0" > nul 2>&1
)

if not exist smtp2api.jar (
  echo Build not found: service\smtp2api.jar
  exit /b 1
)

if not exist smtp2api.exe (
  echo Missing smtp2api.exe wrapper in service folder.
  echo Please ensure smtp2api.exe is present in the service directory.
  exit /b 1
)

echo Installing Windows service smtp2api...
smtp2api.exe install
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%

echo Service installed. Use "smtp2api.exe start" to start the service.
