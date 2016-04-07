import java.net.URL;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;
import org.jsoup.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class Sisgrad {
  //System.setProperty("jsse.enableSNIExtension", "false"); CONSERTAR ESTE PROBLEMA
  //private static String password = "blablabla7662";
  private static String baseurl = "https://sistemas.feg.unesp.br/sentinela/";
  //private static String baseurl = "https://sistemas.bauru.unesp.br/sentinela/";
  private static String login_action = "login.action";
  private static String login = baseurl+login_action;
  //private static String login = "https://httpbin.org/post";
  private static String getMessagesAction = "sentinela.openMessage.action?emailTipo=recebidas";
  private static String messages = baseurl+getMessagesAction;
  private static String viewMessagesAction1 = "sentinela.viewMessage.action?txt_id=";
  private static String viewMessagesAction2 = "&emailTipo=recebidas";
  private static String mountMessageLink(String id) {
    return(baseurl+viewMessagesAction1+id+viewMessagesAction2);
  }
  
  public static HashMap<String, String> simpleRequest(String url, String postQuery, List<String> cookies) throws Exception {
    URL myurl = new URL(url);
    HttpsURLConnection con = (HttpsURLConnection)myurl.openConnection();
    con.setInstanceFollowRedirects(false);
    con.setRequestProperty("Content-length", String.valueOf(postQuery.length())); 
    con.setRequestProperty("Content-Type","application/x-www-form-urlencoded"); 
    con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0;Windows98;DigExt)"); 
    con.setDoOutput(true); 
    con.setDoInput(true);
    if (cookies!=null){
      for (String cookie : cookies) {
        con.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
      }
    }
    if (postQuery!=null) {
      con.setRequestMethod("POST");
      DataOutputStream output = new DataOutputStream(con.getOutputStream());  
      output.writeBytes(postQuery);
      output.close();
    }

    DataInputStream input = new DataInputStream( con.getInputStream() ); 
    
    String response = "";
    for( int c = input.read(); c != -1; c = input.read() ) 
    response += c;
    input.close(); 
    
    List<String> rCookies = con.getHeaderFields().get("Set-Cookie");
    String responseCode = Integer.toString(con.getResponseCode());
    String responseMessage = con .getResponseMessage();
    System.out.println("Resp Code:"+responseCode); 
    System.out.println("Resp Message:"+ responseMessage); 
    System.out.println("Cookies:"+ rCookies);
    
    
    return(new HashMap<String,String>()); 
  }
  
  public static void main(String[] args) throws Exception {
    //Loads login data from account.txt
    String login_data = readFile("account.txt");
    String[] parts = login_data.split("\\r?\\n");
    String username = parts[0].split("=")[1];
    String password = parts[1].split("=")[1];
    
    //Mounts POST query that's gonna be sent to the login page
    String query = "txt_usuario="+URLEncoder.encode(username,"UTF-8"); 
    query += "&";
    query += "txt_senha="+URLEncoder.encode(password,"UTF-8");
    
    //simpleRequest(url, postQueryEncoded, listOfCookies)
    //Do the login to get the cookies and response
    simpleRequest(login, query, null);
    
    System.out.println(username+password);
  }
  
  //Just a method I borrowed from internet to open a simple text file
  //and convert it to a Sring
  private static String readFile(String fileName) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(fileName));
    try {
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
            sb.append(line);
            sb.append("\n");
            line = br.readLine();
        }
        return sb.toString();
    } finally {
        br.close();
    }
  }
}
