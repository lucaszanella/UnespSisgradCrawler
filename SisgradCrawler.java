import java.net.URL;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SisgradCrawler {
  String username; String password; String baseurl;
  List<String> cookies;
  //private static String baseurl = "https://sistemas.unesp.br/sentinela/";
  //private static String baseurl = "https://sistemas.bauru.unesp.br/sentinela/";
  private String login_action;
  private String login_url;
  //private static String login = "https://httpbin.org/post";
  private String getMessagesAction;
  private String messages;
  private String viewMessagesAction1;
  private String viewMessagesAction2;
  private String mountMessagePage(int page, String magicalNumber) {
    String thingsInTheEnd;
    if (page==0) {
      thingsInTheEnd = "&p="+page+"&d-"+magicalNumber+"-p="+2;
    } else {
      thingsInTheEnd = "";
    }
    return(this.baseurl+"sentinela.openMessage.action?emailTipo=recebidas"+thingsInTheEnd);
  }
  private String mountMessageLink(String id, int page) {
    return(baseurl+viewMessagesAction1+id+viewMessagesAction2);
  }
  public SisgradCrawler(String username, String password, String baseurl) throws Exception {
    this.username = username;
    this.password = password;
    this.baseurl = baseurl;
    this.login_action = "login.action";
    this.login_url = baseurl+login_action;
  //private static String login = "https://httpbin.org/post";
    this.getMessagesAction = "sentinela.openMessage.action?emailTipo=recebidas";
    this.messages = baseurl+getMessagesAction;
    this.viewMessagesAction1 = "sentinela.viewMessage.action?txt_id=";
    this.viewMessagesAction2 = "&emailTipo=recebidas";
  }
  public void connect() throws Exception {
    //Mounts POST query that's gonna be sent to the login page
    String query = "txt_usuario="+URLEncoder.encode(username,"UTF-8"); 
    query += "&";
    query += "txt_senha="+URLEncoder.encode(password,"UTF-8");
    //SimpleRequest mySimpleRequest(url, postQueryEncoded, listOfCookies) //basic usage of SimpleRequest
    System.out.println("DEBUG: "+this.login_url+" : "+ query);
    SimpleRequest loginRequest = new SimpleRequest(this.login_url,query,null);//calls the login url, POSTing the query with user and password
    String locationRedirect = loginRequest.location;//.toString();
    this.cookies = loginRequest.cookies;
  }
  public List<Map<String,String>> getMessages(int page)  throws Exception {
    //SimpleRequest firstScreenAfterLoginRequest = new SimpleRequest(locationRedirect, new String(), this.cookies);
    SimpleRequest pageToReadMessages = new SimpleRequest(mountMessagePage(0, ""), new String(), this.cookies);
    System.out.println("-----------------------------");
    //System.out.println(pageToReadMessages.response.substring(25000,50000));
    Document doc = Jsoup.parse(pageToReadMessages.response);
    Element table = doc.getElementById("destinatario");
    Elements pageNumbers = doc.getElementsByClass("listagemTopo");
    Elements pageLinks = pageNumbers.select("a");
    for (Element pageLink : pageLinks) {
      String pageBaseLink = pageLink.attr("href").split("&d-")[1].split("-p")[0];//Crazy number that I don't know the utility but won't work without it
      break;//They're all the same, so I break here, but if something change in the future, here's the loop so I can use :)
    }
    //ystem.out.println(pageLinks);
    Elements messages = table.getElementsByTag("tr");
    String title; String author;String subject; String messageId; String messageIdString; String sentDate; String readDate;
    int c = 0;
    List<Map<String,String>> messagesList = new ArrayList<Map<String,String>>();
    for (Element message : messages) {
      Elements rowOfMessageTable = message.getElementsByTag("td");
      if (c>0 && c<3) {
        author = rowOfMessageTable.get(2).text();
        title = rowOfMessageTable.get(3).select("a").text();
        messageIdString = rowOfMessageTable.get(3).select("a").first().attr("href");
        sentDate = rowOfMessageTable.get(4).text();
        readDate = rowOfMessageTable.get(5).text();
        messageId = messageIdString.split("\\(")[1].split("\\)")[0];
        Map<String, String> messageRow = new HashMap<String, String>();
        messageRow.put("title",title);
        messageRow.put("author",author);
        messageRow.put("messageId",messageId);
        messageRow.put("sentDate",sentDate);
        messageRow.put("readDate",readDate);
        messagesList.add(messageRow);
        //System.out.println();
      } else if (c>=5){
        break;
      }
      c+=1;
    }
    System.out.println(messagesList);
    return messagesList;
    //System.out.println(pageToReadMessages.response);
  }
}