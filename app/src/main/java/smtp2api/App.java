package smtp2api;

import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class App {

    public static void main(String[] args) {
        // Create a Listener to handle incoming emails
        SimpleMessageListener myListener = new SimpleMessageListener() {
            @Override
            public boolean accept(String from, String recipient) {
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

                    System.out.println("Received mail from: " + from);
                    System.out.println("Subject: " + subject);

                    // 2. Prepare JSON data
                    Map<String, String> mailMap = new HashMap<String, String>();
                    mailMap.put("from", from);
                    mailMap.put("to", recipient);
                    mailMap.put("subject", subject);
                    mailMap.put("body", body);

                    String jsonPayload = new ObjectMapper().writeValueAsString(mailMap);

                    // 3. Call REST API using HttpURLConnection (Java 8 compatible)
                    callRestApi(jsonPayload);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        // Start SMTP Server on port 1025
        SMTPServer smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(myListener));
        smtpServer.setPort(1025);
        smtpServer.start();
        
        System.out.println("SMTP Server is running on port 1025...");
    }

    private static void callRestApi(String json) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Json: " + json);
                    // URL url = new URL("https://your-api-endpoint.com/webhook");
                    // HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    // connection.setRequestMethod("POST");
                    // connection.setRequestProperty("Content-Type", "application/json");
                    // connection.setDoOutput(true);
                    
                    // OutputStream os = connection.getOutputStream();
                    // os.write(json.getBytes("UTF-8"));
                    // os.flush();
                    // os.close();
                    
                    // int status = connection.getResponseCode();
                    // System.out.println("API Response Status: " + status);
                    // connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}