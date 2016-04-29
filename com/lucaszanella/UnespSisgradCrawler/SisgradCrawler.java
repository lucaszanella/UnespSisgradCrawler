package com.lucaszanella.UnespSisgradCrawler;

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SisgradCrawler {
 private Boolean debugMode = true;
 public String username;
 private String password;
 public List < List < String >> cookies = new ArrayList < List < String >> ();;
 private static String protocol = "https";
 private static String domain = "sistemas.unesp.br";
 private String magicalNumber;
 private Boolean alreadyLoadedMagicalNumber = false;

 private URL mountMessagePage(int page, String magicalNumber) throws IOException {
  String thingsInTheEnd;
  if (page != 0) {
   thingsInTheEnd = "&p=" + page + "&d-" + magicalNumber + "-p=" + 2;
  } else {
   thingsInTheEnd = "";
  }
  return (new URL(this.protocol + "://" + this.domain + "/" + "sentinela" + "/" + "sentinela.openMessage.action?emailTipo=recebidas" + thingsInTheEnd));
 }
 private URL mountMessageLink(String id, int page) throws IOException {
  return (new URL(this.protocol + "://" + this.domain + "/" + "sentinela" + "/" + "sentinela.viewMessage.action?txt_id=" + id + "&emailTipo=recebidas"));
 }
 public SisgradCrawler() throws Exception {
  if (magicalNumber != null) {
   this.magicalNumber = magicalNumber;
   this.alreadyLoadedMagicalNumber = true;
  }
 }
 public void loginToSentinela(String username, String password) throws Exception {
  //Mounts POST query that's gonna be sent to the login page
  this.username = username;
  this.password = password;
  this.domain = domain;
  String postQuery = "txt_usuario=" + URLEncoder.encode(username, "UTF-8") + "&" + "txt_senha=" + URLEncoder.encode(password, "UTF-8");
  if (debugMode) {
   System.out.println("logging in to sentinela");
  }
  URL sentinelaLogin = new URL(this.protocol + "://" + this.domain + "/" + "sentinela" + "/" + "login.action");
  SimpleRequest loginRequest = new SimpleRequest(sentinelaLogin, postQuery, null); //calls the login url, POSTing the query with user and password
  String locationRedirect = loginRequest.location;
  //System.out.println("cookies now: "+loginRequest.cookies);
  if (loginRequest.cookies != null) {
   this.cookies.add(loginRequest.cookies);
  }
 }
 public void loginToAcademico() throws Exception {
  if (debugMode) {
   System.out.println("logging in to academico");
  }
  URL academicoLogin = new URL(this.protocol + "://" + this.domain + "/" + "sentinela" + "/" + "sentinela.acessarSistema.action?id=3");
  SimpleRequest loginRequest = new SimpleRequest(academicoLogin, new String(), this.cookies); //calls the login url from academico's page
  URL locationRedirect = new URL(loginRequest.location);
  if (loginRequest.cookies != null) {
   System.out.println("adding following cookies: "+loginRequest.cookies);
   this.cookies.add(loginRequest.cookies);
  }
  SimpleRequest pageafterLogin = new SimpleRequest(locationRedirect, new String(), this.cookies); //calls the login url from academico's page
  if (pageafterLogin.cookies != null) {
   System.out.println("adding following cookies: "+pageafterLogin.cookies);
    this.cookies.add(pageafterLogin.cookies);
  } 
 }
 public String getMagicalNumber(SimpleRequest page) {
  Document doc = Jsoup.parse(page.response);
  Elements pageNumbers = doc.getElementsByClass("listagemTopo");
  Elements pageLinks = pageNumbers.select("a");
  //if (debugMode) {System.out.println("pageLinks: "+ pageLinks);}
  String magicalNumber = "";
  for (Element pageLink: pageLinks) {
   magicalNumber = pageLink.attr("href").split("d-")[1].split("-p")[0]; //Crazy number that I don't know the utility but won't work without it
   break; //They're all the same, so I break here, but if something change in the future, here's the loop so I can use :)
  }
  return magicalNumber;
 }
 public List < Map < String, String >> getMessages(int page) throws Exception {
  //SimpleRequest firstScreenAfterLoginRequest = new SimpleRequest(locationRedirect, new String(), this.cookies);
  SimpleRequest pageToReadMessages;
  if (page == 0) {
   if (debugMode) {
    System.out.println("getting page 0");
   }
   pageToReadMessages = new SimpleRequest(mountMessagePage(0, ""), new String(), this.cookies);
   this.magicalNumber = getMagicalNumber(pageToReadMessages);
   this.alreadyLoadedMagicalNumber = true;
  } else if (this.alreadyLoadedMagicalNumber) {
   if (debugMode) {
    System.out.println("already loaded page magicalNumber before, now getting page " + page);
   }
   pageToReadMessages = new SimpleRequest(mountMessagePage(page, this.magicalNumber), new String(), this.cookies);
  } else {
   if (debugMode) {
    System.out.println("didn't load magicalNumber before, gonna get the first page to get magicalNumber and then load the page " + page);
   }
   SimpleRequest magicalNumberRequest = new SimpleRequest(mountMessagePage(0, ""), new String(), this.cookies); //Yes, I really need to load this page first just to get the magicalNumber that ables me to get the other pages  
   if (debugMode) {
    System.out.println("Setting up magical number: " + getMagicalNumber(magicalNumberRequest));
   }
   this.magicalNumber = getMagicalNumber(magicalNumberRequest);

   if (debugMode) {
    System.out.println("already loaded magicalNumber, now gonna get new page: " + page);
   }
   pageToReadMessages = new SimpleRequest(mountMessagePage(page, this.magicalNumber), new String(), this.cookies);
   this.alreadyLoadedMagicalNumber = true;
  }
  //System.out.println("-----------------------------");
  //System.out.println(pageToReadMessages.response.substring(25000,50000));
  Document doc = Jsoup.parse(pageToReadMessages.response);
  Element table = doc.getElementById("destinatario");
  Elements messages = table.getElementsByTag("tr");
  String title;
  String author;
  String subject;
  String messageId;
  String messageIdString;
  String sentDate;
  String readDate;
  int c = 0;
  List < Map < String, String >> messagesList = new ArrayList < Map < String, String >> ();
  for (Element message: messages) {
   Elements rowOfMessageTable = message.getElementsByTag("td");
   if (c > 0) {
    author = rowOfMessageTable.get(2).text();
    title = rowOfMessageTable.get(3).select("a").text();
    messageIdString = rowOfMessageTable.get(3).select("a").first().attr("href");
    sentDate = rowOfMessageTable.get(4).text();
    readDate = rowOfMessageTable.get(5).text();
    messageId = messageIdString.split("\\(")[1].split("\\)")[0];
    Map < String, String > messageRow = new HashMap < String, String > ();
    messageRow.put("title", title);
    messageRow.put("author", author);
    messageRow.put("messageId", messageId);
    messageRow.put("sentDate", sentDate);
    messageRow.put("readDate", readDate);
    messagesList.add(messageRow);
    //System.out.println();
   } else {
    //
   }
   c += 1;
  }
  //System.out.println(messagesList);
  return messagesList;
  //System.out.println(pageToReadMessages.response);
 }
 public List < Map < String, String >> getClasses() throws Exception {
  List < Map < String, String >> a = new ArrayList < Map < String, String >> ();
  URL getClassesURL = new URL(this.protocol + "://" + this.domain + "/" + "academico" + "/aluno/cadastro.horarioAulas.action");
  SimpleRequest classesRequest = new SimpleRequest(getClassesURL, new String(), this.cookies);
  /*
  System.out.println("getClasses() response: "+classesRequest.response);
  System.out.println("getClasses() code: "+classesRequest.responseMessage);
  System.out.println("getClasses() location: "+classesRequest.location);
  */
  SimpleRequest classesRequestRedirected = new SimpleRequest(new URL(classesRequest.location), new String(), this.cookies);
  /*
  System.out.println("getClasses() response: "+classesRequestRedirected.response);
  System.out.println("getClasses() code: "+classesRequestRedirected.responseMessage);
  System.out.println("getClasses() location: "+classesRequestRedirected.location);
  */
  SimpleRequest classesRequestRedirectedAgain = new SimpleRequest(new URL(classesRequestRedirected.location), new String(), this.cookies);
  /*
  System.out.println("getClasses() response: "+classesRequestRedirectedAgain.response);
  System.out.println("getClasses() code: "+classesRequestRedirectedAgain.responseMessage);
  System.out.println("getClasses() location: "+classesRequestRedirectedAgain.location);
  */
  Document doc = Jsoup.parse(classesRequestRedirectedAgain.response);
  Elements tableOriginal = doc.getElementsByClass("listagem quadro");
  Element table = doc.select("table").get(1);
  Elements lines = table.select("td");
  Map<String, Map<String,Map<String, String>>> classesData = new HashMap<String, Map<String,Map<String, String>> >();
  List<String> daysOfWeek = new ArrayList<String>(Arrays.asList("segunda", "terca", "quarta", "quinta", "sexta", "sabado")); 
  Map<String,Map<String, String>> dayAndHourData = new HashMap<String,Map<String, String>>();
  int c = 0;
  for (Element line: lines) {
    //System.out.println(line);
     
    Map<String, String> hourData = new HashMap<String, String>();
    String dayName = line.attr("id");
    Pattern r = Pattern.compile("[A-Za-z]*");
    Matcher m = r.matcher(dayName);
    String trueDayName = "";
    if (m.find()) {trueDayName = m.group().toLowerCase();} else {System.out.println("Didn't find anything at: "+dayName);}
    Element parentTag = line.parent();
    String parentTagName = parentTag.tagName();
    if (daysOfWeek.contains(trueDayName) && parentTagName.equals("tr")) {
      if (!line.select("div").isEmpty()) {
        String lineText = line.text();
        String className = line.select("div").attr("title");//Not 'java class', here I mean, the name of the class of the university
        String classText = line.select("div").text();
        String nonsenseId = line.select("div").attr("id");//Don't know what this id means, but gonna store it for future usage
        String hourOfThisDay = parentTag.select("th").text();
        hourData.put("className", className);
        hourData.put("classText", classText);
        hourData.put("hour", hourOfThisDay);
        hourData.put("id", nonsenseId);
        dayAndHourData.put(hourOfThisDay, hourData);
        System.out.println("added "+ hourData);
      } else {
        System.out.println("selected empty: "+line);
      }
    } else {
      System.out.println("selecionou empty: "+line);
    }
    if (c<50 && debugMode) {
      //if (!hourData.isEmpty()) {System.out.println(dayAndHourData);}
      //System.out.println(trueDayName);
      //System.out.println(line.parent().tag());
    } else {
      System.out.println("----------------------");
      System.out.println(dayAndHourData);
      break;
    }
    c+=1;
  }
  
  /*
  Map<String, String> segundaFeira = new HashMap<String, String>();
  Map<String, String> tercaFeira = new HashMap<String, String>();
  Map<String, String> quartaFeira = new HashMap<String, String>();
  Map<String, String> quintaFeira = new HashMap<String, String>();
  Map<String, String> sextaFeira = new HashMap<String, String>();
  Map<String, String> sabado = new HashMap<String, String>(); 
  Elements hours = table.select("tr");
  int c = 0;
  for (Element hour: hours) {
    String SingleHour = hour.select("th").get(0).select("center").html();
    if (c<4 && debugMode) {
      //System.out.println(SingleHour);
    } else {
      break;
    }
    c+=1;
    Elements days = hour.select("td");
    //System.out.println("days "+days);
    for (Element day : days) {
      String dayName = day.attr("id");
      Pattern r = Pattern.compile("[A-Za-z]*");
      Matcher m = r.matcher(dayName);
      String trueDayName = "";
      if (m.find()) {trueDayName = m.group();} else {System.out.println("Didn't find anything at: "+dayName);}
      switch (trueDayName) {
        case "SEGUNDA":
          
      }
      //if (Pattern.compile(Pattern.quote("segunda"), Pattern.CASE_INSENSITIVE).matcher(singleDay).find()){System.out.println("contém segunda");}
      //if (singleDay.contains("SEGUNDA")) {System.out.println("contém segunda");}
      //System.out.println(dayName);
    }
  }
  */
  
  //Elements table2 = doc.getElementsByClass("listagem");
  //Elements tableElements = table.select("tbody");
  //System.out.println(classesRequestRedirectedAgain.response);
  //System.out.println(table);
  //System.out.println(tables); 
  //System.out.println(hours.get(1));
  //a.add(classesRequest.response);
  return (a);
 }
}