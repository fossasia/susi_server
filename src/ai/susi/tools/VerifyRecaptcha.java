package ai.susi.tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import org.json.JSONObject;

public class VerifyRecaptcha {

	/**
	 * Validates Google reCAPTCHA V2 or Invisible reCAPTCHA.
	 * 
	 * @param secretKey Secret key (key given for communication between your site
	 *                  and Google)
	 * @param response  reCAPTCHA response from client side. (g-recaptcha-response)
	 * @return true if validation successful, false otherwise.
	 */
	public static boolean verify(String response) {
		try {
			String url = "https://www.google.com/recaptcha/api/siteverify?"
					+ "secret=6LfPZGAUAAAAAAULjaq7Rt9-7IGJoLYoz2Di6yVV&response=" + response;
			InputStream res = new URL(url).openStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(res, Charset.forName("UTF-8")));

			StringBuilder sb = new StringBuilder();
			int cp;
			while ((cp = rd.read()) != -1) {
				sb.append((char) cp);
			}
			String jsonText = sb.toString();
			res.close();

			JSONObject json = new JSONObject(jsonText);
			return json.getBoolean("success");
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return false;
	}
}
