# SMTP2API Windows Service

This project can be run as a Windows service using WinSW.

## Steps

1. Build the application:
   ```bat
   mvn clean package
   ```
2. Copy the generated jar to the service folder:
   ```bat
   copy target\smtp2api.jar service\
   ```
3. Install the service:
   ```bat
   cd service
   install-service.bat
   ```

## Notes

- `install-service.bat` uses the pre-packaged `smtp2api.exe` WinSW wrapper in the service directory.
- The service config is defined in `service\smtp2api.xml`.
- To uninstall the service, run:
  ```bat
  cd service
  uninstall-service.bat
  ```

## Requirements

- Java must be available on `PATH` or configured in the service wrapper.
- `smtp2api.jar` must be present in the `service` folder before installation.
