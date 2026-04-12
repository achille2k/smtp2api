package smtp2api;

import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
                        // Note: Needs more robust handling if content is MultiPart
                        String body = message.getContent().toString();

                        logger.info("Email received from: {}", from);
                        logger.info("Recipient: {}", recipient);
                        logger.info("Subject: {}", subject);

                        // 2. Prepare JSON data
                        Map<String, String> mailMap = new HashMap<String, String>();
                        mailMap.put("from", from);
                        mailMap.put("to", recipient);
                        mailMap.put("subject", subject);
                        mailMap.put("body", body);

                        String jsonPayload = new ObjectMapper().writeValueAsString(mailMap);
                        
                        logger.debug("Payload: {}", jsonPayload);
                        
                        // 3. Call REST API using HttpURLConnection (Java 8 compatible)
                        callRestApi(jsonPayload);

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

    private static void callRestApi(String json) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String apiEndpoint = config.getProperty("api.endpoint", "https://your-api-endpoint.com/webhook");
                    int timeout = Integer.parseInt(config.getProperty("api.timeout.seconds", "30")) * 1000;
                    
                    logger.info("Calling REST API endpoint: {}", apiEndpoint);
                    
                    URL url = new URL(apiEndpoint);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setConnectTimeout(timeout);
                    connection.setReadTimeout(timeout);
                    connection.setDoOutput(true);
                    
                    OutputStream os = connection.getOutputStream();
                    os.write(json.getBytes("UTF-8"));
                    os.flush();
                    os.close();
                    
                    int status = connection.getResponseCode();
                    logger.info("API Response Status: {}", status);
                    
                    if (status >= 200 && status < 300) {
                        logger.info("Email successfully sent to API");
                    } else {
                        logger.warn("API returned status: {}", status);
                    }
                    
                    connection.disconnect();
                } catch (Exception e) {
                    logger.error("Error calling REST API", e);
                }
            }
        });
        thread.start();
    }
}