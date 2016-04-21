import java.net.URL;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;
//import org.jsoup.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

//Just a Class designed to facilitate HTTPs requests to Unesp's Sisgrad server, that will return HTML pages
//

//System.setProperty("jsse.enableSNIExtension", "false"); CONSERTAR ESTE PROBLEMA
public class SimpleRequest {
 public String response;
 public String responseCode;
 public String responseMessage;
 public String location;
 public List < String > cookies;

 public SimpleRequest(URL url, String postQuery, List < List < String > > listOfCookiesList) throws Exception {
  System.out.println("calling " + url);
  //System.out.println("query "+postQuery);
  System.out.println("cookies " + listOfCookiesList);
  List < String > _cookies = new ArrayList < String > ();
  if (listOfCookiesList != null) {
   //Choose the cookies that match the path being requested
   for (List < String > cookieList: listOfCookiesList) {
    for (String singleCookie: cookieList) {
     String search = singleCookie.split("Path")[1].split("/")[1];
     System.out.println("search "+search);
     if (url.getPath().split("/")[1].contains(search)) { //ANALISAR URL COM CUIDADO
      _cookies = cookieList;
      System.out.println("chose "+search);
      System.out.println("url.getPath() cutted: "+url.getPath().split("/")[1]);
     }
    }
   }
  }
  System.out.println("cookie being used: " + _cookies);
  HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
  con.setInstanceFollowRedirects(false);
  con.setRequestProperty("Content-length", String.valueOf(postQuery.length()));
  con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
  con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0;Windows98;DigExt)");
  con.setDoInput(true);
  if (_cookies != null) {
   for (String cookie: _cookies) {
    con.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
   }
  }
  if (postQuery != null) {
   con.setDoOutput(true);
   con.setRequestMethod("POST");
   DataOutputStream output = new DataOutputStream(con.getOutputStream());
   output.writeBytes(postQuery);
   output.close(); //VERIFICAR ISSO!
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
  System.out.println("Cookies got: " + this.cookies);
  this.responseCode = Integer.toString(con.getResponseCode());
  //List<String> location = new List<String>();
  this.responseMessage = con.getResponseMessage();
  if (responseCode.equals("302")) {
   this.location = con.getHeaderFields().get("Location").get(0);
  }
 }
}