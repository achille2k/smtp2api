# SMTP to API - Logging Troubleshooting Guide

## Problem: Logs Not Writing to File

When running the application from the root directory, logs may not appear in the file, even though console output works.

## Root Cause

The issue typically occurs when:
1. The `logs/` directory doesn't exist and Logback can't create it
2. Directory creation happens after logging is initialized
3. Relative paths don't resolve correctly from the running directory

## Solution Implemented

### 1. **Automatic Directory Creation**
The application now creates the `logs/` directory before initializing loggers:

```java
// Create logs directory if it doesn't exist
Files.createDirectories(Paths.get("logs"));
```

This runs first in `main()`, ensuring the directory exists before any logging occurs.

### 2. **Console Feedback**
The application now prints the absolute log file path to console:

```
Log directory: C:\Users\Microsoft\Documents\GitHub\smtp2api\logs
Log file: C:\Users\Microsoft\Documents\GitHub\smtp2api\logs\smtp2api.log
```

This helps verify that logs are being written to the expected location.

### 3. **Updated Logback Configuration**
The `logback.xml` now uses property-based paths:

```xml
<property name="LOG_DIR" value="logs"/>
<property name="LOG_FILE" value="${LOG_DIR}/smtp2api.log"/>
```

This ensures consistent path resolution throughout the configuration.

## How to Use

### Build the Application
```bash
cd C:\Users\Microsoft\Documents\GitHub\smtp2api
mvn clean package
```

### Run from Root Directory
```bash
# Run from the root directory
cd C:\Users\Microsoft\Documents\GitHub\smtp2api
java -jar target/smtp2api.jar
```

### Verify Logs Are Working

1. **Console Output (before logger initialization):**
   ```
   Log directory: C:\Users\Microsoft\Documents\GitHub\smtp2api\logs
   Log file: C:\Users\Microsoft\Documents\GitHub\smtp2api\logs\smtp2api.log
   ```

2. **Application Logs (after logger initialization):**
   ```
   2024-04-12 15:30:45.123 [main] INFO  smtp2api.App - ========================================
   2024-04-12 15:30:45.124 [main] INFO  smtp2api.App - Starting SMTP to API v1.0.0
   2024-04-12 15:30:45.125 [main] INFO  smtp2api.App - ========================================
   2024-04-12 15:30:45.200 [main] INFO  smtp2api.App - Log directory: C:\Users\Microsoft\Documents\GitHub\smtp2api\logs
   2024-04-12 15:30:45.201 [main] INFO  smtp2api.App - Log file: C:\Users\Microsoft\Documents\GitHub\smtp2api\logs\smtp2api.log
   ```

3. **Check Log Files:**
   ```bash
   # Windows
   type logs\smtp2api.log
   
   # Unix/Linux
   cat logs/smtp2api.log
   ```

## File Locations

After running the application, you should see:

```
smtp2api/
├── logs/
│   ├── smtp2api.log          (current log file)
│   ├── smtp2api.2024-04-12.1.log
│   └── smtp2api.2024-04-13.1.log
├── target/
│   └── smtp2api.jar          (built JAR file)
├── app/
│   └── src/
│       └── main/
│           └── resources/
│               ├── application.properties   (configuration file in JAR)
│               └── logback.xml              (logging configuration in JAR)
└── application.properties    (optional root-level config override)
```

## Configuration Files

### Primary Configuration (in JAR)
**Location:** `app/src/main/resources/application.properties`

This file is packaged with the JAR and contains default settings.

### Optional Override (at runtime)
**Location:** `application.properties` (in root directory)

If this file exists when running the JAR, it overrides the built-in defaults.

## Log File Contents

### Log Rotation
- **Daily Rotation:** Logs rotate at midnight, creating datestamped files
- **Size-Based Rotation:** Files rotate when they exceed 10MB
- **Retention:** Keeps 30 days of logs before deletion

### Log Format
```
2024-04-12 15:30:45.123 [main] INFO  smtp2api.App - Message here
└─────────────────────┘ └────┘ └────┘ └──────────┘   └──────────┘
        Date/Time      Thread Level Logger Class    Log Message
```

## Common Issues & Solutions

### Issue: No logs directory created
**Solution:** Ensure write permissions in the application directory
```bash
# Windows
mkdir logs
icacls logs /grant:r %USERNAME%:F

# Unix/Linux
mkdir -p logs
chmod 755 logs
```

### Issue: "Permission denied" writing to logs
**Solution:** Check directory permissions
```bash
# Unix/Linux
ls -la logs/
chmod 777 logs/
```

### Issue: Log files in wrong location
**Solution:** Check running directory
```bash
# PowerShell
[System.IO.Path]::GetFullPath($PWD)

# Bash
pwd
```

Make sure you run the JAR from the project root directory.

### Issue: Async logs appearing after application exits
**Solution:** The async appender flushes logs when the application shuts down. This is normal behavior and ensures no logs are lost.

## Performance Considerations

The logging configuration uses:
- **Async Appender:** Non-blocking file writes for better performance
- **Queue Size:** 512 pending log entries before dropping (set to 0 to never drop)
- **Batch Flushing:** Logs are batched and written to disk periodically

This ensures the application responsiveness isn't affected by disk I/O.

## Advanced Configuration

To modify logging behavior, edit `app/src/main/resources/logback.xml`:

```xml
<!-- Change log level for specific loggers -->
<logger name="smtp2api" level="DEBUG"/>  <!-- More detailed logging -->

<!-- Change log retention policy -->
<maxHistory>7</maxHistory>  <!-- Keep 7 days instead of 30 -->

<!-- Change max file size -->
<maxFileSize>50MB</maxFileSize>  <!-- 50MB files instead of 10MB -->

<!-- Change async queue size -->
<queueSize>1024</queueSize>  <!-- Larger buffer for high-volume logging -->
```

Then rebuild:
```bash
mvn clean package
```
