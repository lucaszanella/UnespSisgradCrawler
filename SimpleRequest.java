import java.net.URL;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;
//import org.jsoup.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

//Just a Class designed to facilitate requests to Unesp's Sisgrad server
//

//System.setProperty("jsse.enableSNIExtension", "false"); CONSERTAR ESTE PROBLEMA
public class SimpleRequest {
 public String response;
 public String responseCode;
 public String responseMessage;
 public String location;
 public List < String > cookies;

 public SimpleRequest(String url, String postQuery, List < String > rcookies) throws Exception {
  //System.out.println("calling "+url);
  //System.out.println("query "+postQuery);
  //System.out.println("cookies "+rcookies);
  URL myurl = new URL(url);
  HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
  con.setInstanceFollowRedirects(false);
  con.setRequestProperty("Content-length", String.valueOf(postQuery.length()));
  con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
  con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0;Windows98;DigExt)");
  con.setDoOutput(true);
  con.setDoInput(true);
  if (rcookies != null) {
   for (String cookie: rcookies) {
    con.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
   }
  }
  if (postQuery != null) {
   con.setRequestMethod("POST");
   DataOutputStream output = new DataOutputStream(con.getOutputStream());
   output.writeBytes(postQuery);
   output.close();
  }

  //DataInputStream dis = new DataInputStream( con.getInputStream() ); 
  String charset = "ISO-8859-1";
  BufferedReader buff = new BufferedReader(
   new InputStreamReader(con.getInputStream(), charset));
  String response = "";
  String line;
  while ((line = buff.readLine()) != null) {
   this.response += line + "\n";
  }

  //use inputLine.toString(); here it would have whole source
  //String response = inputLine.toString();

  this.cookies = con.getHeaderFields().get("Set-Cookie");
  this.responseCode = Integer.toString(con.getResponseCode());
  //List<String> location = new List<String>();
  this.responseMessage = con.getResponseMessage();
  if (responseCode.equals("302")) {
   this.location = con.getHeaderFields().get("Location").get(0);
  }
 }
}