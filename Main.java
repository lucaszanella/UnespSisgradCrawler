import java.net.URL;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {
  //Loads login data from account.txt
  private static String baseurl = "https://sistemas.unesp.br/sentinela/";
  //private static String baseurl = "https://sistemas.bauru.unesp.br/sentinela/";
  private static String login_action = "login.action";
  private static String login_url = baseurl+login_action;
  //private static String login = "https://httpbin.org/post";
  private static String getMessagesAction = "sentinela.openMessage.action?emailTipo=recebidas";
  private static String messages = baseurl+getMessagesAction;
  private static String viewMessagesAction1 = "sentinela.viewMessage.action?txt_id=";
  private static String viewMessagesAction2 = "&emailTipo=recebidas";
  private static String mountMessageLink(String id, int page) {
    return(baseurl+viewMessagesAction1+id+viewMessagesAction2);
  }
  public static void main(String[] args) throws Exception {
    
    //opens file account.txt with username as password in the format:
    //user=my_username
    //password=my_password
    String login_data = readFile("account.txt");
    String[] parts = login_data.split("\\r?\\n");
    String username = parts[0].split("=")[1];
    String password = parts[1].split("=")[1];

    //Mounts POST query that's gonna be sent to the login page
    String query = "txt_usuario="+URLEncoder.encode(username,"UTF-8"); 
    query += "&";
    query += "txt_senha="+URLEncoder.encode(password,"UTF-8");
    //SimpleRequest mySimpleRequest(url, postQueryEncoded, listOfCookies) //basic usage of SimpleRequest
    SimpleRequest loginRequest = new SimpleRequest(login_url,query,null);//calls the login url, POSTing the query with user and password
    String locationRedirect = loginRequest.location;//.toString();
    List<String> cookies = loginRequest.cookies;
    SimpleRequest firstScreenAfterLoginRequest = new SimpleRequest(locationRedirect, new String(), cookies);
    SimpleRequest pageToReadMessages = new SimpleRequest(messages, new String(), cookies);
    
    //System.out.println(loginRequest.response);
    System.out.println("-----------------------------");
    //System.out.println(pageToReadMessages.response.substring(25000,50000));
    Document doc = Jsoup.parse(pageToReadMessages.response);
    Element table = doc.getElementById("destinatario");
    Elements messages = table.getElementsByTag("tr");
    String title; String subject; String messageIdString;String sentDate; String readDate;String messageId;
    int c = 0;
    for (Element message : messages) {
      Elements rowOfMessageTable = message.getElementsByTag("td");
      //System.out.println(pageToReadMessages.response);
      //System.out.println("--");
      //System.out.println(rowOfMessageTable);
      if (c>0 && c<3) {
        title = rowOfMessageTable.get(2).text();
        messageIdString = rowOfMessageTable.get(3).select("a").first().attr("href");
        sentDate = rowOfMessageTable.get(4).text();
        readDate = rowOfMessageTable.get(5).text();
        messageId = messageIdString.split("\\(")[1].split("\\)")[0];
        System.out.println(messageId);
        break;
      }
      c+=1;
    }
    //System.out.println(pageToReadMessages.response);
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