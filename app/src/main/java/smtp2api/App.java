package smtp2api;

import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static Properties config;

    public static void main(String[] args) {
        try {
            // Create logs directory if it doesn't exist
            Files.createDirectories(Paths.get("logs"));
            
            // Log startup information
            String logPath = Paths.get("logs").toAbsolutePath().toString();
            System.out.println("Log directory: " + logPath);
            System.out.println("Log file: " + Paths.get("logs/smtp2api.log").toAbsolutePath().toString());
            
            // Load configuration
            config = loadConfig();
            
            int smtpPort = Integer.parseInt(config.getProperty("smtp.port", "1025"));
            String appName = config.getProperty("app.name", "SMTP to API");
            String appVersion = config.getProperty("app.version", "1.0.0");
            
            logger.info("========================================");
            logger.info("Starting {} v{}", appName, appVersion);
            logger.info("========================================");
            logger.info("Log directory: {}", Paths.get("logs").toAbsolutePath().toString());
            logger.info("Log file: {}", Paths.get("logs/smtp2api.log").toAbsolutePath().toString());
            
            // Create a Listener to handle incoming emails
            SimpleMessageListener myListener = new SimpleMessageListener() {
                @Override
                public boolean accept(String from, String recipient) {
                    logger.debug("Email received from: {} to: {}", from, recipient);
                    return true; // Accept all senders and recipients
                }

                @Override
                public void deliver(String from, String recipient, InputStream data) {
                    try {
                        // 1. Parse Email content
                        Session session = Session.getDefaultInstance(new Properties());
                        MimeMessage message = new MimeMessage(session, data);

                        String subject = message.getSubject();
                        Object content = message.getContent();
                        String body = extractText(content);

                        logger.info("Email received from: {}", from);
                        logger.info("Recipient: {}", recipient);
                        logger.info("Subject: {}", subject);
                        logger.info("Email body length: {}", body != null ? body.length() : 0);

                        // 2. Prepare JSON data
                        Map<String, String> mailMap = new HashMap<String, String>();
                        mailMap.put("from", from);
                        mailMap.put("to", recipient);
                        mailMap.put("subject", subject);
                        mailMap.put("body", body);

                        String jsonPayload = new ObjectMapper().writeValueAsString(mailMap);
                        logger.debug("Payload: {}", jsonPayload);
                        // 3. Call REST API using HttpURLConnection (Java 8 compatible)
                        ResponseData response = callRestApi(jsonPayload);
                        if (response != null && !"00".equals(response.responseCode)) {
                            logger.warn("API call failed: code={} errorCode={} message={}", response.responseCode, response.errorCode, response.errorMessage);
                        }

                    } catch (Exception e) {
                        logger.error("Error processing email", e);
                    }
                }
            };

            // Start SMTP Server
            SMTPServer smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(myListener));
            smtpServer.setPort(smtpPort);
            smtpServer.start();
            
            logger.info("SMTP Server is running on port {}...", smtpPort);
            logger.info("Listening for incoming emails...");
        } catch (Exception e) {
            logger.error("Failed to start SMTP Server", e);
            System.exit(1);
        }
    }

    /**
     * Load configuration from application.properties file
     */
    private static Properties loadConfig() {
        Properties properties = new Properties();
        try {
            // First, try to load from file in current directory
            String configPath = "application.properties";
            if (Files.exists(Paths.get(configPath))) {
                properties.load(Files.newInputStream(Paths.get(configPath)));
                logger.info("Configuration loaded from file: {}", new File(configPath).getAbsolutePath());
            } else {
                // If not found, try to load from classpath (inside JAR)
                try (InputStream is = App.class.getClassLoader().getResourceAsStream("application.properties")) {
                    if (is != null) {
                        properties.load(is);
                        logger.info("Configuration loaded from classpath (JAR)");
                    } else {
                        logger.warn("Configuration file not found in classpath");
                        logger.warn("Using default values");
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to load configuration file", e);
        }
        return properties;
    }

    private static String extractText(Object content) throws Exception {
        if (content == null) {
            return "";
        }
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof MimeMultipart) {
            return getTextFromMimeMultipart((MimeMultipart) content);
        }
        if (content instanceof Multipart) {
            return getTextFromMimeMultipart((MimeMultipart) content);
        }
        return content.toString();
    }

    private static String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws Exception {
        StringBuilder plainText = new StringBuilder();
        StringBuilder htmlText = new StringBuilder();
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                plainText.append(bodyPart.getContent().toString()).append('\n');
            } else if (bodyPart.isMimeType("text/html")) {
                htmlText.append(bodyPart.getContent().toString()).append('\n');
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                String nested = getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
                if (nested != null && !nested.isEmpty()) {
                    plainText.append(nested).append('\n');
                }
            }
        }
        if (plainText.length() > 0) {
            return plainText.toString().trim();
        }
        return htmlText.toString().trim();
    }

    private static class ResponseData {
        public String responseCode;
        public String errorCode;
        public String errorMessage;
        public Map<String, Object> data;
    }

    private static class ApiError {
        public String ErrorCode;
        public String Message;
    }

    private static ResponseData callRestApi(String json) {
        ResponseData responseData = new ResponseData();
        String apiEndpoint = config.getProperty("api.endpoint", "https://your-api-endpoint.com/webhook");
        int timeout = Integer.parseInt(config.getProperty("api.timeout.seconds", "30")) * 1000;
        boolean retryEnabled = Boolean.parseBoolean(config.getProperty("api.retry.enabled", "false"));
        int retryAttempts = 1;
        try {
            retryAttempts = Integer.parseInt(config.getProperty("api.retry.attempts", "3"));
        } catch (NumberFormatException ex) {
            logger.warn("Invalid api.retry.attempts value, using default=3");
            retryAttempts = 3;
        }
        if (retryAttempts < 1) {
            retryAttempts = 1;
        }

        int attempt = 1;
        while (attempt <= retryAttempts) {
            HttpURLConnection connection = null;
            try {
                logger.info("Calling REST API endpoint: {} (attempt {}/{})", apiEndpoint, attempt, retryAttempts);
                URL url = new URL(apiEndpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Accept", "application/json; charset=utf-8");

                String basicAuth = config.getProperty("api.auth", "");
                if (!basicAuth.isEmpty()) {
                    String encoded = java.util.Base64.getEncoder().encodeToString(basicAuth.getBytes(StandardCharsets.US_ASCII));
                    connection.setRequestProperty("Authorization", "Basic " + encoded);
                }

                // String sessionId = config.getProperty("api.session.id", "");
                // if (!sessionId.isEmpty()) {
                //     connection.setRequestProperty("X-Session-Id", sessionId);
                // }

                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int status = connection.getResponseCode();
                String responseText = readStream(status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream());

                Map<String, Object> responseMap = null;
                try {
                    responseMap = new ObjectMapper().readValue(responseText, Map.class);
                } catch (Exception ex) {
                    logger.warn("Unable to parse API response JSON", ex);
                }

                String errorCode = extractString(responseMap, "ErrorCode", "errorCode");
                String errorMessage = extractString(responseMap, "ErrorMessage", "Message", "message");
                responseData.data = responseMap;

                if (status >= 200 && status < 300) {
                    if ("000".equals(errorCode)) {
                        responseData.responseCode = "00";
                        responseData.errorCode = errorCode;
                        return responseData;
                    }
                    responseData.responseCode = "99";
                    responseData.errorCode = errorCode != null ? errorCode : "99";
                    responseData.errorMessage = errorMessage != null ? errorMessage : "Business error";
                    return responseData;
                }

                responseData.responseCode = Integer.toString(status);
                responseData.errorCode = errorCode != null ? errorCode : responseData.responseCode;
                responseData.errorMessage = errorMessage != null ? errorMessage : ("HTTP " + status);
                if (!retryEnabled) {
                    return responseData;
                }
            } catch (IOException ex) {
                logger.error("Error calling REST API on attempt {}/{}", attempt, retryAttempts, ex);
                responseData.responseCode = "99";
                responseData.errorCode = "NO_RESPONSE";
                responseData.errorMessage = "CAN NOT CALL API: " + ex.getMessage();
                if (!retryEnabled) {
                    return responseData;
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            if (attempt < retryAttempts) {
                try {
                    int sleepMs = 1000 * attempt;
                    logger.info("Retrying API call in {} ms", sleepMs);
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.warn("Retry sleep interrupted", ie);
                    break;
                }
            }
            attempt++;
        }

        if (responseData.responseCode == null) {
            responseData.responseCode = "99";
            responseData.errorCode = "NO_RESPONSE";
            responseData.errorMessage = "CAN NOT CALL API: No response received from API.";
        }
        return responseData;
    }

    private static String readStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    private static String extractString(Map<String, Object> data, String... keys) {
        if (data == null) {
            return null;
        }
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof String) {
                return (String) value;
            }
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }
}
