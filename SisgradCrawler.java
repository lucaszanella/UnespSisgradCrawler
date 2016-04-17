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
  private String magicalNumber;
  private Boolean alreadyLoadedMagicalNumber = false;
  private Boolean debugMode = true;
  private String mountMessagePage(int page, String magicalNumber) {
    String thingsInTheEnd;
    if (page!=0) {
      thingsInTheEnd = "&p="+page+"&d-"+magicalNumber+"-p="+2;
    } else {
      thingsInTheEnd = "";
    }
    return(this.baseurl+"sentinela.openMessage.action?emailTipo=recebidas"+thingsInTheEnd);
  }
  private String mountMessageLink(String id, int page) {
    return(baseurl+viewMessagesAction1+id+viewMessagesAction2);
  }
  public SisgradCrawler(String username, String password, String baseurl, String magicalNumber) throws Exception {
    this.username = username;
    this.password = password;
    if (magicalNumber!=null) {
      this.magicalNumber = magicalNumber;
      this.alreadyLoadedMagicalNumber = true;
    }
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
    //System.out.println("DEBUG: "+this.login_url+" : "+ query);
    if (debugMode) {System.out.println("logging in");}
    SimpleRequest loginRequest = new SimpleRequest(this.login_url,query,null);//calls the login url, POSTing the query with user and password
    String locationRedirect = loginRequest.location;//.toString();
    this.cookies = loginRequest.cookies;
  }
  public String getMagicalNumber(SimpleRequest page) {
    Document doc = Jsoup.parse(page.response);
    Elements pageNumbers = doc.getElementsByClass("listagemTopo");
    Elements pageLinks = pageNumbers.select("a");
    //if (debugMode) {System.out.println("pageLinks: "+ pageLinks);}
    String magicalNumber="";
    for (Element pageLink : pageLinks) {
      //if (debugMode) {System.out.println("MagicalNumber requested, pageLink is: "+pageLink);}
      magicalNumber = pageLink.attr("href").split("d-")[1].split("-p")[0];//Crazy number that I don't know the utility but won't work without it
      //if (debugMode) {System.out.println("MagicalNumber requested, it is: "+magicalNumber);}
      break;//They're all the same, so I break here, but if something change in the future, here's the loop so I can use :)
    }
    return magicalNumber;
  }
  public List<Map<String,String>> getMessages(int page)  throws Exception {
    //SimpleRequest firstScreenAfterLoginRequest = new SimpleRequest(locationRedirect, new String(), this.cookies);
    SimpleRequest pageToReadMessages;
    if (page==0) {
      if (debugMode) {System.out.println("getting page 0");}
      pageToReadMessages = new SimpleRequest(mountMessagePage(0, ""), new String(), this.cookies);
      this.magicalNumber = getMagicalNumber(pageToReadMessages);
      this.alreadyLoadedMagicalNumber = true;
    } else if (this.alreadyLoadedMagicalNumber) {
      if (debugMode) {System.out.println("already loaded page magicalNumber before, now getting page "+page);}
      pageToReadMessages = new SimpleRequest(mountMessagePage(page, this.magicalNumber), new String(), this.cookies);
    } else {
      if (debugMode) {System.out.println("didn't load magicalNumber before, gonna get the first page to get magicalNumber and then load the page "+page);}
      SimpleRequest magicalNumberRequest = new SimpleRequest(mountMessagePage(0, ""), new String(), this.cookies);//Yes, I really need to load this page first just to get the magicalNumber that ables me to get the other pages  
      if (debugMode) {System.out.println("Setting up magical number: "+getMagicalNumber(magicalNumberRequest));}      
      this.magicalNumber = getMagicalNumber(magicalNumberRequest);
      
      if (debugMode) {System.out.println("already loaded magicalNumber, now gonna get new page: "+page);}  
      pageToReadMessages = new SimpleRequest(mountMessagePage(page,this.magicalNumber), new String(), this.cookies);
      this.alreadyLoadedMagicalNumber = true;
    }
    //System.out.println("-----------------------------");
    //System.out.println(pageToReadMessages.response.substring(25000,50000));
    Document doc = Jsoup.parse(pageToReadMessages.response);
    Element table = doc.getElementById("destinatario");
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
    //System.out.println(messagesList);
    return messagesList;
    //System.out.println(pageToReadMessages.response);
  }
}