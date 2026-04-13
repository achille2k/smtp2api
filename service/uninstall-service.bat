@echo off
cd /d "%~dp0"

if exist smtp2api.exe (
  echo Stopping service if running...
  smtp2api.exe stop > nul 2>&1
  echo Uninstalling service...
  smtp2api.exe uninstall
) else (
  echo smtp2api.exe not found in service folder.
)

echo Service uninstall complete.
