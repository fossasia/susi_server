package org.loklak.harvester;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.loklak.http.CookieRequestManager;

import java.util.regex.Matcher;

public class WeiboInfoScraper {
	/**
	 * This is a helper function that helps user to extract html nested inside of html script
	 * @param raw_html
	 * @return nested html String
	 */
	private static String getNestedHtml(String raw_html){
		String html = raw_html.replace("\\","");
		Document doc = Jsoup.parse(html);
		//get the script tag
		Elements scripts = doc.getElementsByTag("script");
		//pattern for extracting html
		Pattern pttrn = Pattern.compile("\"html\":\"");
		String nested_html = "";
		for (Element script:scripts){
			Matcher m =  pttrn.matcher(html = script.html());
			if(m.find()){
				nested_html += html.substring(m.end(), html.length() -3);
			}
		}
		return nested_html;
		}

	private CookieRequestManager manager = new CookieRequestManager();

	public WeiboInfoScraper(){}

	public void addCookie(String cookie){
		manager.addCookie(cookie);
	}

	public String get(String url){
		return getNestedHtml(manager.buildRequest(url).makeRequest().body());
	}

	public static void main(String args[]) {
		String test_url = "http://weibo.com/p/1005051666978981/info?mod=pedit_more";
		WeiboInfoScraper scraper = new WeiboInfoScraper();
		scraper.addCookie("SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9W5CpyuN-4ys57wHiW6l3Lk35JpX5KMhUgL.FoeE1h2c1Ke7ehM2dJLoI74WMJf0UGiadJUkxsHV97tt; SINAGLOBAL=7613128031177.77.1468064007468; ULV=1469247293505:4:4:2:6888389703798.142.1469247293469:1469195743689; SCF=AiBpidaOFHvAe4IdkfIvwMnQPbwC_X6-mWARH-VfeZgGBg2aI_9nTP3RooOHIZTUgc-HvQ3WJ0i3lJoxBuuV5MI.; SUHB=0u5hf-IIrTOdnN; wb_bub_hot_3281693007=1; UOR=,,developer.51cto.com; ALF=1500780807; _s_tentry=developer.51cto.com; SUB=_2A256lpXXDeTxGeVM41MX-S3MyzuIHXVZ5YAfrDV8PUNbmtBeLXflkW9PNY1IURQCXFeXkAya64Lev-0VGQ..; SSOLoginState=1469244807; YF-Ugrow-G0=56862bac2f6bf97368b95873bc687eef; wvr=6; YF-V5-G0=c99031715427fe982b79bf287ae448f6; Apache=6888389703798.142.1469247293469; YF-Page-G0=c6cf9d248b30287d0e884a20bac2c5ff");
		String nested = scraper.get(test_url);
		System.out.println(nested);
	}
}
