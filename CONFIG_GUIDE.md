# SMTP to API - Configuration & Logging Guide

## Configuration Files

### 1. `application.properties`
Location: `app/src/main/resources/application.properties`

This file contains all configuration parameters for the application:

```properties
# SMTP Server Configuration
smtp.port=1025                          # SMTP server listening port
smtp.host=localhost                    # SMTP server hostname

# REST API Configuration
api.endpoint=https://your-api-endpoint.com/webhook  # API endpoint URL
api.timeout.seconds=30                 # Request timeout in seconds
api.retry.enabled=false                # Enable/disable retry logic
api.retry.attempts=3                   # Number of retry attempts

# Application Configuration
app.name=SMTP to API                   # Application name
app.version=1.0.0                      # Application version
```

#### How to Customize:

1. **SMTP Port**: Change `smtp.port` to use a different port (default: 1025)
   - Note: Ports below 1024 require root/admin privileges on Unix systems

2. **API Endpoint**: Set `api.endpoint` to your actual API webhook URL
   - The application will POST JSON data to this endpoint

3. **Timeout**: Adjust `api.timeout.seconds` for your API response time requirements

### 2. `logback.xml`
Location: `app/src/main/resources/logback.xml`

This file configures logging behavior:

#### Features:
- **Console Output**: Logs displayed in the terminal
- **File Output**: Logs written to `logs/smtp2api.log`
- **Rolling Policy**: 
  - Daily rotation with date in filename
  - Maximum 10MB per file before rotation
  - Keeps 30 days of logs
- **Async Appender**: Non-blocking file writes for better performance

#### Log Levels:
- `DEBUG`: Detailed debug information
- `INFO`: General information messages
- `WARN`: Warning messages
- `ERROR`: Error messages

#### Log Format:
```
2024-04-12 15:30:45.123 [main] INFO  smtp2api.App - Application started
```

Format breakdown:
- `2024-04-12 15:30:45.123` - Timestamp
- `[main]` - Thread name
- `INFO` - Log level
- `smtp2api.App` - Logger name
- Message

## Log Files

### Location
Logs are stored in: `logs/` directory (relative to application working directory)

### Files Generated
- `smtp2api.log` - Current application log
- `smtp2api.2024-04-12.1.log` - Archived logs with date and sequence

### Log Content Examples

**Application Startup:**
```
2024-04-12 15:30:45.123 [main] INFO  smtp2api.App - ========================================
2024-04-12 15:30:45.124 [main] INFO  smtp2api.App - Starting SMTP to API v1.0.0
2024-04-12 15:30:45.125 [main] INFO  smtp2api.App - ========================================
2024-04-12 15:30:45.200 [main] INFO  smtp2api.App - Configuration loaded from: app/src/main/resources/application.properties
2024-04-12 15:30:45.300 [main] INFO  smtp2api.App - SMTP Server is running on port 1025...
2024-04-12 15:30:45.301 [main] INFO  smtp2api.App - Listening for incoming emails...
```

**Email Processing:**
```
2024-04-12 15:31:10.456 [pool-1-thread-1] INFO  smtp2api.App - Email received from: sender@example.com
2024-04-12 15:31:10.457 [pool-1-thread-1] INFO  smtp2api.App - Recipient: recipient@example.com
2024-04-12 15:31:10.458 [pool-1-thread-1] INFO  smtp2api.App - Subject: Test Email
2024-04-12 15:31:10.500 [pool-1-thread-1] DEBUG smtp2api.App - Payload: {"from":"sender@example.com","to":"recipient@example.com",...}
2024-04-12 15:31:10.501 [pool-1-thread-1] INFO  smtp2api.App - Calling REST API endpoint: https://your-api-endpoint.com/webhook
2024-04-12 15:31:11.123 [pool-1-thread-1] INFO  smtp2api.App - API Response Status: 200
2024-04-12 15:31:11.124 [pool-1-thread-1] INFO  smtp2api.App - Email successfully sent to API
```

**Error Handling:**
```
2024-04-12 15:35:45.789 [pool-1-thread-1] ERROR smtp2api.App - Error calling REST API
java.net.UnknownHostException: your-api-endpoint.com
    at java.net.InetAddress.getAllByName0(InetAddress.java:1260)
    ...
```

## Building and Running

### Build with Maven
```bash
mvn clean package
```

This will create: `target/smtp2api.jar`

### Run the Application
```bash
java -jar target/smtp2api.jar
```

The application will:
1. Load configuration from `application.properties`
2. Initialize logging with `logback.xml`
3. Start SMTP server on configured port
4. Begin logging all activities

### View Logs
```bash
# View current log file
tail -f logs/smtp2api.log

# View with timestamps
tail -f logs/smtp2api.log | grep "2024-04-12"

# Count log entries
wc -l logs/smtp2api.log

# Search for errors
grep ERROR logs/smtp2api.log
```

## Troubleshooting

### 1. "Address already in use" Error
- Another service is using the configured SMTP port
- **Solution**: Change port in `application.properties` (e.g., to 1026)

### 2. API Connection Errors
- Network connectivity issue or incorrect endpoint URL
- **Solution**: Verify `api.endpoint` in `application.properties`
- Check logs: `grep "Error calling REST API" logs/smtp2api.log`

### 3. High Memory Usage
- Async appender queue getting too large
- **Solution**: Adjust `queueSize` in `logback.xml`

### 4. Logs Not Being Generated
- Check logs directory permissions
- **Solution**: Ensure `logs/` directory is writable: `mkdir -p logs && chmod 755 logs`

## Dependencies

Required packages (automatically included via Maven):
- **subethasmtp**: SMTP server implementation
- **slf4j-api**: Logging API
- **logback-classic**: Logging implementation
- **jackson-databind**: JSON processing
