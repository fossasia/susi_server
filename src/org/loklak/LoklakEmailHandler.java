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

package org.loklak;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.eclipse.jetty.util.log.Log;
import org.loklak.data.DAO;
import org.loklak.server.Authorization;
import org.loklak.server.ClientCredential;

public class LoklakEmailHandler {

	private static Pattern pattern;
	private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

	public static void sendEmail(String addressTo, String subject, String text, Authorization rights)
			throws Exception {
		
		if (!rights.isAdmin() && !"true".equals(DAO.getConfig("smtp.mails.enabled", "false"))) {
			throw new Exception("Mail sending disabled");
		}
		
		pattern = Pattern.compile(EMAIL_PATTERN);
		
		ClientCredential credential = new ClientCredential(ClientCredential.Type.passwd_login, addressTo);
		String sender = DAO.getConfig("smtp.host.senderid", "server@loklak.org");
		String pass = DAO.getConfig("smtp.host.senderpass", "randomxyz");
		String hostname = DAO.getConfig("smtp.host.name", "smtp.gmail.com");
		String type = DAO.getConfig("smtp.host.encryption", "tls");
		String port = DAO.getConfig("smtp.host.port", "465");

		if (!pattern.matcher(addressTo).matches()) {
			throw new Exception("Invalid email ID");
		}
		if (!pattern.matcher(sender).matches()) {
			throw new Exception("Invalid sender ID");
		}

		if (DAO.authentication.has(credential.toString())
				&& ("none".equals(type) || "tls".equals(type) || "starttls".equals(type)))

		{
			java.util.Properties props;

			if ("none".equals(type)) {
				props = System.getProperties();
			} else {
				props = new Properties();
				props.put("mail.smtp.auth", true);
				props.put("mail.smtp.port", port);
			}

			props.put("mail.smtp.host", hostname);
			props.put("mail.debug", true);

			if ("starttls".equals(type)) {
				props.put("mail.smtp.starttls.enable", true);
			} else if ("tls".equals(type)) {
				props.put("mail.smtp.socketFactory.port", port);
				props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			}

			Session session;
			if ("none".equals(type)) {
				session = Session.getInstance(props, null);
			} else {
				session = Session.getInstance(props, new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(sender, pass);
					}
				});
			}

			try {

				MimeMessage message = new MimeMessage(session);
				message.addHeader("Content-type", "text/HTML; charset=UTF-8");
				message.addHeader("format", "flowed");
				message.addHeader("Content-Transfer-Encoding", "8bit");
				message.setSentDate(new Date());
				message.setFrom(new InternetAddress(sender));
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(addressTo, false));
				message.setSubject(subject, "UTF-8");
				message.setText(text, "UTF-8");
				Transport.send(message);
				Log.getLog().debug("status: ok", "reason: ok");

			} catch (MessagingException mex) {
				throw mex;
			}

		} else {
			throw new Exception("Receiver email does not exist or invalid encryption type");
		}
	}
}
