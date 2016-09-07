/**
 * 
 */
package org.loklak.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;


/**
 * @author scott
 *
 */
@SuppressWarnings("deprecation")
public class CookieRequestManager {
	private ArrayList<String> cookies = new ArrayList<String>();
	private Random randomno = new Random();

	public void addCookie(String cookie){
		cookies.add(cookie);
		return;
	}
	
	public boolean delCookie(String cookie){
		return cookies.remove(cookie);
	}
	
	public int cookieCount(){
		return cookies.size();
	}
	
	public String randGetCookie(){
		int idx = randomno.nextInt(cookies.size());
		return cookies.get(idx);
	}
	
	
	// helper to convert inputstream to string
	public   static   String   inputStream2String(InputStream   is)   throws   IOException{
        ByteArrayOutputStream   baos   =   new   ByteArrayOutputStream();
        int   i=-1;
        while((i=is.read())!=-1){
        baos.write(i);
        }
       return   baos.toString();
    } 
	
	private class Request implements CookieRequest{
		private String cookie;
		private String url;
		private String response = "";
		HttpGet httpGet;
		
		
		
		
		
		public Request(String cookie_, String url_){
			cookie = cookie_;
			url = url_;
			httpGet = new HttpGet(url);
		}
		
		
		
		
		
		public CookieRequest makeRequest(){
			httpGet.addHeader("Cookie", cookie);
			try{
				HttpResponse httpResponse = new DefaultHttpClient().execute(httpGet);
				if(httpResponse.getStatusLine().getStatusCode() == 200){
					response = inputStream2String(httpResponse.getEntity().getContent());	
				}
				else{
					response = "";
				}
			}catch(Exception e){}
			return this;
		}
		
		
		public String body(){
			return response;
		}
		
		public String cookie(){
			return cookie;
		}
	}
	
	public CookieRequest buildRequest(String url){
		return new Request(this.randGetCookie(), url);
	}
	
	public static void main(String args[]){
		CookieRequestManager manager = new CookieRequestManager();
		
		manager.addCookie("SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9W5CpyuN-4ys57wHiW6l3Lk35JpX5KMhUgL.FoeE1h2c1Ke7ehM2dJLoI74WMJf0UGiadJUkxsHV97tt; SINAGLOBAL=7613128031177.77.1468064007468; ULV=1469247293505:4:4:2:6888389703798.142.1469247293469:1469195743689; SCF=AiBpidaOFHvAe4IdkfIvwMnQPbwC_X6-mWARH-VfeZgGBg2aI_9nTP3RooOHIZTUgc-HvQ3WJ0i3lJoxBuuV5MI.; SUHB=0u5hf-IIrTOdnN; wb_bub_hot_3281693007=1; UOR=,,developer.51cto.com; ALF=1500780807; _s_tentry=developer.51cto.com; SUB=_2A256lpXXDeTxGeVM41MX-S3MyzuIHXVZ5YAfrDV8PUNbmtBeLXflkW9PNY1IURQCXFeXkAya64Lev-0VGQ..; SSOLoginState=1469244807; YF-Ugrow-G0=56862bac2f6bf97368b95873bc687eef; wvr=6; YF-V5-G0=c99031715427fe982b79bf287ae448f6; Apache=6888389703798.142.1469247293469; YF-Page-G0=c6cf9d248b30287d0e884a20bac2c5ff");
		//don't use my cookie do some bad thing, thank you
		
		String body = manager.buildRequest("http://weibo.com/p/1005051666978981/info?mod=pedit_more").makeRequest().body();
		System.out.println(body);
	}
}
