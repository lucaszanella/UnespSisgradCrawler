package com.lucaszanella.SisgradCrawler;

import com.lucaszanella.SimpleRequest.SimpleHTTPSRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SisgradCrawler {
    private Boolean debugMode = false;
    public String username;
    private String password;
    private static String protocol = "https";
    private static String domain = "sistemas.unesp.br";
    private String magicalNumber;
    private Boolean alreadyLoadedMagicalNumber = false;
    private SimpleHTTPSRequest sisgradRequest = new SimpleHTTPSRequest();

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
    public SisgradCrawler(String username, String password) {
        this.username = username;
        this.password = password;
    }
    public void loginToSentinela() throws Exception {
        //Mounts POST query that's gonna be sent to the login page
        this.domain = domain;
        String postQuery = "txt_usuario=" + URLEncoder.encode(username, "UTF-8") + "&" + "txt_senha=" + URLEncoder.encode(password, "UTF-8");
        if (debugMode) {
            System.out.println("logging in to sentinela");
        }
        URL sentinelaLogin = new URL(this.protocol + "://" + this.domain + "/" + "sentinela" + "/" + "login.action");
        SimpleHTTPSRequest.requestObject loginRequest = sisgradRequest.SimpleHTTPSRequest(sentinelaLogin, postQuery); //calls the login url, POSTing the query with user and password
        String locationRedirect = loginRequest.location;
    }
    public void loginToAcademico() throws Exception {
        if (debugMode) {
            System.out.println("logging in to academico");
        }
        URL academicoLogin = new URL(this.protocol + "://" + this.domain + "/" + "sentinela" + "/" + "sentinela.acessarSistema.action?id=3");
        SimpleHTTPSRequest.requestObject loginRequest = sisgradRequest.SimpleHTTPSRequest(academicoLogin, null); //calls the login url from academico's page
        URL locationRedirect = new URL(loginRequest.location);
        SimpleHTTPSRequest.requestObject pageafterLogin = sisgradRequest.SimpleHTTPSRequest(locationRedirect, new String()); //calls the login url from academico's page
    }
    public String getMagicalNumber(SimpleHTTPSRequest.requestObject page) {
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
        //SimpleHTTPSRequest firstScreenAfterLoginRequest = new SimpleHTTPSRequest(locationRedirect, new String(), this.cookies);
        SimpleHTTPSRequest.requestObject pageToReadMessages;
        if (page == 0) {
            if (debugMode) {
                System.out.println("getting page 0");
            }
            pageToReadMessages = sisgradRequest.SimpleHTTPSRequest(mountMessagePage(0, ""), null);
            this.magicalNumber = getMagicalNumber(pageToReadMessages);
            this.alreadyLoadedMagicalNumber = true;
        } else if (this.alreadyLoadedMagicalNumber) {
            if (debugMode) {
                System.out.println("already loaded page magicalNumber before, now getting page " + page);
            }
            pageToReadMessages = sisgradRequest.SimpleHTTPSRequest(mountMessagePage(page, this.magicalNumber), null);
        } else {
            if (debugMode) {
                System.out.println("didn't load magicalNumber before, gonna get the first page to get magicalNumber and then load the page " + page);
            }
            SimpleHTTPSRequest.requestObject magicalNumberRequest = sisgradRequest.SimpleHTTPSRequest(mountMessagePage(0, ""), null); //Yes, I really need to load this page first just to get the magicalNumber that ables me to get the other pages
            if (debugMode) {
                System.out.println("Setting up magical number: " + getMagicalNumber(magicalNumberRequest));
            }
            this.magicalNumber = getMagicalNumber(magicalNumberRequest);

            if (debugMode) {
                System.out.println("already loaded magicalNumber, now gonna get new page: " + page);
            }
            pageToReadMessages = sisgradRequest.SimpleHTTPSRequest(mountMessagePage(page, this.magicalNumber), null);
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
                messageRow.put("messageId", messageId.replace("'", ""));
                messageRow.put("sentDate", sentDate);
                messageRow.put("readDate", readDate);
                String dateString = sentDate.split("\\.")[0];
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Date date = dateFormat.parse(dateString);
                long unixTime = (long) date.getTime()/1000;
                messageRow.put("sentDateUnix", String.valueOf(unixTime));
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
    public class GetMessageResponse{
        public String author;
        public String title;
        public String message;
        public Map<String, String> attachments;
        public GetMessageResponse(String author, String title, String message, Map<String, String> attachments) {
            this.author = author;
            this.title = title;
            this.message = message;
            this.attachments = attachments;
        }
    }
    public GetMessageResponse getMessage(String messageId, Boolean html) throws Exception {//this method is a mess. TODO: make it better
        //System.out.println("hi, i'm getting message for id "+ messageId );
        URL getMessagesURL = new URL(this.protocol + "://" + this.domain + "/" + "sentinela" + "/" + "sentinela.viewMessage.action?txt_id="+messageId+"&emailTipo=recebidas");
        if (debugMode) {System.out.println(" the url is "+ getMessagesURL.toString());}
        SimpleHTTPSRequest.requestObject messageRequest = sisgradRequest.SimpleHTTPSRequest(getMessagesURL, null);
        Document doc = Jsoup.parse(messageRequest.response);
        Element messageForm = doc.select("form").get(0);//gets the first form. TODO: change this to get the largest form or something like that
        Element messageTable = messageForm.select("table").get(0);
        Elements messageTableRows = messageForm.select("tr");
        Element attachments = null;
        //System.out.println("messageTableRows: "+messageTableRows.html());
        Boolean containsAttachments = false;
        Map<String, String> attachmentsList = new HashMap<String, String>();
        for (Element messageTableRow: messageTableRows) {
            //System.out.println("messageTableRow being analyzed: "+messageTableRow.html());
            Elements linksOfAttachments = messageTableRow.select("td").select("a");
            if (messageTableRow.select("td").text().toLowerCase().contains("anexo")) {
                containsAttachments = true;
                //System.out.println("contains link");
                //a.put("attachments", )
                //Elements attachmentLinks = linksOfAttachments.select("a");
                for (Element linkOfAttachment:linksOfAttachments) {
                    System.out.println("linkOfAttachment: "+linkOfAttachment.html()+"attr: "+linkOfAttachment.attr("href"));
                    attachmentsList.put(linkOfAttachment.text(),linkOfAttachment.attr("href"));
                }
            }
        }
        String message;
        int trNumber;//number of the <tr> tag in which message is contained
        if (containsAttachments) {
            trNumber = 4;
        } else {
            trNumber = 3;
            attachmentsList = null;
        }
        if (!html) {
            message = messageTable.select("tr").get(trNumber).text();
        } else {
            message = messageTable.select("tr").get(trNumber).html();
        }
        //Map<String, String> a = new HashMap<String,String>();
        //a.put("message", message);

        if (debugMode) {System.out.println("the message is "+ message);}
        String author = "";
        String title = "";
        return new GetMessageResponse(author, title, message, attachmentsList);
    }

    public Map<String, List<Map<String, String>>> getClasses() throws Exception {
        List < Map < String, String >> a = new ArrayList < Map < String, String >> ();
        URL getClassesURL = new URL(this.protocol + "://" + this.domain + "/" + "academico" + "/aluno/cadastro.horarioAulas.action");
        SimpleHTTPSRequest.requestObject classesRequest = sisgradRequest.SimpleHTTPSRequest(getClassesURL, null);

        SimpleHTTPSRequest.requestObject classesRequestRedirected = sisgradRequest.SimpleHTTPSRequest(new URL(classesRequest.location), null);
        SimpleHTTPSRequest.requestObject classesRequestRedirectedAgain = sisgradRequest.SimpleHTTPSRequest(new URL(classesRequestRedirected.location), null);

        Document doc = Jsoup.parse(classesRequestRedirectedAgain.response);
        Elements tableOriginal = doc.getElementsByClass("listagem quadro");
        Element table = doc.select("table").get(1);
        Elements lines = table.select("td");
        List<String> daysOfWeek = new ArrayList<String>(Arrays.asList("segunda", "terca", "quarta", "quinta", "sexta", "sabado"));//just a list of days of the week
        //Map<String, Map<String,Map<String, String>>> classesData = new HashMap<String, Map<String,Map<String, String>> >();
        Map<String, List<Map<String, String>>> classesData = new HashMap<String, List<Map<String, String>> >();
        for (String day:daysOfWeek) {classesData.put(day, new ArrayList<Map<String,String>>());}
        //p<String,Map<String, String>> dayAndHourData = new HashMap<String,Map<String, String>>();
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
                    classesData.get(trueDayName).add(hourData);
        /*
        hourData.put("className", className);
        hourData.put("classText", classText);
        hourData.put("hour", hourOfThisDay);
        dayAndHourData.put(hourOfThisDay, hourData);
        */
                    //System.out.println("added "+ hourData);
                } else {
                    //System.out.println("selected empty: "+line);
                }
            } else {
                System.out.println("selecionou empty: "+line);
            }
        }
        return (classesData);
    }
}