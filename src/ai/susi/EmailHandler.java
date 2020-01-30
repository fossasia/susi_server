/**
 *  LoklakEmailHandler
 *  Copyright 25.05.2016 by Shiven Mian, @shivenmian
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package ai.susi;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ai.susi.tools.TimeoutMatcher;

public class EmailHandler {

	public static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    /**
     * Send an email
     * @param addressTo Email address to send to
     * @param subject The email subject
     * @param text The content
     * @throws Exception on errors
     */
	public static void sendEmail(@Nonnull String addressTo, @Nonnull String subject, @Nonnull String text) throws Exception {
		String senderEmail = DAO.getConfig("smtp.sender.email", null);
        String displayname = DAO.getConfig("smtp.sender.displayname", null);
        sendEmail(senderEmail, displayname, addressTo, subject, text);
	}

   public static void sendEmail(String senderEmail, String displayname, @Nonnull String addressTo, @Nonnull String subject, @Nonnull String text) throws Exception {
        
        if (!"SMTP".equals(DAO.getConfig("mail.type", "false"))) {
            throw new Exception("Mail sending disabled");
        }

        String username = DAO.getConfig("smtp.sender.username", null);
        String password = DAO.getConfig("smtp.sender.password", null);
        String hostname = DAO.getConfig("smtp.host.name", null);
        String encryption = DAO.getConfig("smtp.host.encryption", null);
        Long port = DAO.getConfig("smtp.host.port", 0);
        boolean disableCertChecking = DAO.getConfig("smtp.trustselfsignedcerts", false);

        if(senderEmail == null || password == null || hostname == null){
            throw new Exception("Invalid SMTP configuration");
        }

        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        if (!new TimeoutMatcher(pattern.matcher(addressTo)).matches()) {
            throw new Exception("Invalid email ID");
        }
        if (!new TimeoutMatcher(pattern.matcher(senderEmail)).matches()) {
            throw new Exception("Invalid sender ID");
        }

        Properties props = createProperties(hostname, port.intValue(), encryption, disableCertChecking);

        Session session;
        if ("none".equals(encryption)) {
            session = Session.getInstance(props, null);
        } else {
            session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        }

        MimeMessage message = new MimeMessage(session);
        message.addHeader("Content-type", "text/HTML; charset=UTF-8");
        message.addHeader("format", "flowed");
        message.addHeader("Content-Transfer-Encoding", "8bit");
        message.setSentDate(new Date());
        message.setReplyTo(new Address[]{new InternetAddress(senderEmail, displayname)});
        message.setFrom(new InternetAddress(senderEmail, displayname));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(addressTo, false));
        message.setSubject(subject, "UTF-8");
        message.setText(text, "UTF-8");
        Transport.send(message);
        DAO.log("Successfully send mail to " + addressTo);
    }
	
    /**
     * Check SMTP login credentials
     * @param hostname the host address
     * @param username the username/login
     * @param password the password
     * @param encryption encryption type (must be none, starttls or tls)
     * @param port the port number
     * @param disableCertificateChecking disable certificate checking (behind a ssl-proxy or when the server has a self signed certificate)
     * @throws MessagingException on error
     */
	public static void checkConnection(@Nonnull String hostname, @Nonnull String username,
                                       @Nonnull String password, @Nonnull String encryption,
                                       int port, boolean disableCertificateChecking) throws MessagingException{

        Properties props = createProperties(hostname, port, encryption, disableCertificateChecking);

        Session session = Session.getInstance(props, null);
        Transport transport = session.getTransport("smtp");
        transport.connect(username, password);
        transport.close();
    }

    /**
     * Shared property creation
     * @param port the port number
     * @param encryption encryption type (must be none, starttls or tls)
     * @param disableCertificateChecking disable certificate checking (behind a ssl-proxy or when the server has a self signed certificate)
     * @return a Properties object
     * @throws MessagingException on error
     */
    static private Properties createProperties(@Nonnull String hostname, int port, @Nonnull String encryption, boolean disableCertificateChecking) throws MessagingException{

        if(port <= 0 || port > 65535 || !("none".equals(encryption) || "tls".equals(encryption) || "starttls".equals(encryption))){
            throw new MessagingException("Invalid Port or Encryption scheme");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.host", hostname);

        if ("starttls".equals(encryption)) {
            props.put("mail.smtp.starttls.enable", true);
        } else if ("tls".equals(encryption)) {
            props.put("mail.smtp.ssl.enable", true);
        }
        props.put("mail.smtp.connectiontimeout", 20000);

        if(disableCertificateChecking){
            props.put("mail.smtp.ssl.trust", "*");
        }

        return props;
    }
}
