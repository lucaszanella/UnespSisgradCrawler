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
//TODO: prepare this and other Crawlers to load all components in the page in order to perfectly simulate a computer access

public class SisgradCrawler {
    private Boolean debugMode = false;
    public String username;
    private String password;
    private static String protocol = "https";
    private static String domain = "sistemas.unesp.br";
    private String magicalNumber;
    private Boolean alreadyLoadedMagicalNumber = false;
    private SimpleHTTPSRequest sisgradRequest = new SimpleHTTPSRequest();

    //mounts the URL to load a message page in the Sisgrad's webpage
    private URL mountMessagePage(int page, String magicalNumber) throws IOException {
        String thingsInTheEnd;
        if (page != 0) {
            thingsInTheEnd = "&p=" + page + "&d-" + magicalNumber + "-p=" + 2;
        } else {
            thingsInTheEnd = "";
        }
        return (new URL(protocol + "://" + domain + "/" + "sentinela" + "/" + "sentinela.openMessage.action?emailTipo=recebidas" + thingsInTheEnd));
    }

    private URL mountMessageLink(String id, int page) throws IOException {
        return (new URL(protocol + "://" + domain + "/" + "sentinela" + "/" + "sentinela.viewMessage.action?txt_id=" + id + "&emailTipo=recebidas"));
    }

    public SisgradCrawler(String username, String password) {
        this.username = username;
        this.password = password;
    }

    //---Sentinela Login and its response object

    //Result object to be sent back. 'error' property is null if no errors detected.
    public class SentinelaLoginObject {
        public String locationRedirect;
        public LoginError loginError;
        public PageError pageError;

        public SentinelaLoginObject(String locationRedirect, LoginError loginError, PageError pageError) {
            this.locationRedirect = locationRedirect;
            this.loginError = loginError;
            this.pageError = pageError;
        }
        //class to hold errors related to login
        public class LoginError {
            public Boolean wrongPassword;
            public Boolean wrongEmail;
            public LoginError(Boolean wrongPassword, Boolean wrongEmail) {
                this.wrongPassword = wrongPassword;
                this.wrongEmail = wrongEmail;
            }
        }
        //class to hold errors related to page loading
        public class PageError {
            public String errorCode;
            public String errorMessage;
            public PageError(String errorCode, String errorMessage) {
                this.errorCode = errorCode;
                this.errorMessage = errorMessage;
            }
        }
    }
    //Logs to the 'sentinela' module inside Sisgrad's system. It's responsible to load messages.
    public SentinelaLoginObject loginToSentinela() throws Exception {
        //Mounts POST query that's gonna be sent to the login page
        String postQuery = "txt_usuario=" + URLEncoder.encode(username, "UTF-8") + "&" + "txt_senha=" + URLEncoder.encode(password, "UTF-8");
        if (debugMode) {
            System.out.println("logging in to sentinela");
        }
        URL sentinelaLogin = new URL(protocol + "://" + domain + "/" + "sentinela" + "/" + "login.action");
        SimpleHTTPSRequest.requestObject loginRequest = sisgradRequest.SimpleHTTPSRequest(sentinelaLogin, postQuery); //calls the login url, POSTing the query with user and password

        String locationRedirect = loginRequest.location;
        String responseCode = loginRequest.responseCode;
        String response = loginRequest.response;
        String responseMessage = loginRequest.responseMessage;
        Boolean wrongPassword = false;
        Boolean wrongEmail = false;
        //if there's no location http command, then the login didn't succeed and we're back at the same page
        //this is a signal that the information was wrong
        if (locationRedirect==null || (locationRedirect.equals("") || locationRedirect.length()==0)) {
            //if the login didn't succeed, it could be wrong password or any other thing, so let's detect it!
            Element doc = Jsoup.parse(response);
            Elements errors = doc.getElementsByClass("errormsg");
            String errorMsg = errors.first().text().toLowerCase();

            if (errorMsg.contains("senha")) {wrongPassword = true;}
            if (errorMsg.contains("email") || errorMsg.contains("e-mail")) {wrongEmail = true;}

            SentinelaLoginObject.LoginError loginError =
                    new SentinelaLoginObject(null, null, null).new LoginError(wrongPassword, wrongEmail);
            SentinelaLoginObject.PageError pageError =
                    new SentinelaLoginObject(null, null, null).new PageError(responseCode, responseMessage);
            return new SentinelaLoginObject(locationRedirect, loginError, pageError);
        } else if (locationRedirect.contains("sistemas.unesp.br/sentinela/sentinela.showDesktop.action")) {//login done because it redirected to this page
            return new SentinelaLoginObject(locationRedirect, null, null);
        }
        //If any http error happened, sent it back
        if (!responseCode.equals("302")) {
            SentinelaLoginObject.PageError pageError =
                    new SentinelaLoginObject(null, null, null).new PageError(responseCode, responseMessage);
            return new SentinelaLoginObject(locationRedirect, null, pageError);
        }
        return new SentinelaLoginObject(locationRedirect, null, null);
    }
    //---Academico Login and its response object
    //Result object to be sent back. 'error' property is null if no errors detected.
    public class AcademicoAccessObject {
        public String locationRedirect;
        public PageError pageError;

        public AcademicoAccessObject(String locationRedirect, PageError pageError) {
            this.locationRedirect = locationRedirect;
            this.pageError = pageError;
        }
        //class to hold errors related to page loading
        public class PageError {
            public String errorCode;
            public String errorMessage;
            public PageError(String errorCode, String errorMessage) {
                this.errorCode = errorCode;
                this.errorMessage = errorMessage;
            }
        }
    }
    //Academico module is responsible to load things related to the student information like classes and so
    //It's not a real login like loginToSentinela() but it's needed in order to gather /academico/ pages
    public AcademicoAccessObject accessAcademico() throws Exception {
        if (debugMode) {
            System.out.println("logging in to academico");
        }
        //The academico access process, as I tested, requires me to access the page it redirected me to, so we do it below
        URL academicoLogin = new URL(protocol + "://" + domain + "/" + "sentinela" + "/" + "sentinela.acessarSistema.action?id=3");
        SimpleHTTPSRequest.requestObject loginRequest = sisgradRequest.SimpleHTTPSRequest(academicoLogin, null); //calls the login url from academico's page
        URL locationRedirect = new URL(loginRequest.location);//the login process requires HTTP redirection, which is disabled at SimpleHTTPSRequest
        SimpleHTTPSRequest.requestObject pageafterLogin = sisgradRequest.SimpleHTTPSRequest(locationRedirect, null); //calls the next page just to simulate a computer access
        if (pageafterLogin.responseCode.equals("200")) {
            return new AcademicoAccessObject(null, null);
        } else if (pageafterLogin.location!=null && pageafterLogin.responseCode.equals("302")) {//login probably timed out, server issued redirection to login page
            if (pageafterLogin.location.contains("sistemas.unesp.br/sentinela/login.open.action")) {//if location is login page...
                SentinelaLoginObject relogin = loginToSentinela();
                if (relogin.pageError==null) {//if everything went ok
                    accessAcademico();//Calls itself, now that it did login again
                }
            }
        }
        AcademicoAccessObject.PageError pageError = new AcademicoAccessObject(null, null).new PageError(pageafterLogin.responseCode, pageafterLogin.responseMessage);
        return new AcademicoAccessObject(pageafterLogin.location, pageError);

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

    //---GetMessage and its response object

    //Object to return from GetMessagesResponse
    public class GetMessagesResponse{
        public PageError pageError;
        public List < Map < String, String >> messages;
        public GetMessagesResponse(PageError pageError,  List < Map < String, String >> messages) {
            this.pageError = pageError;
            this.messages = messages;
        }

        //class to hold errors related to page loading
        public class PageError {
            public String errorCode;
            public String errorMessage;
            public PageError(String errorCode, String errorMessage) {
                this.errorCode = errorCode;
                this.errorMessage = errorMessage;
            }
        }
    }
    //Gets the messages from the system.
    public GetMessagesResponse getMessages(int page) throws Exception {
        /*
            Getting messages for pages above 0 is tricky, the link has some number that I couldn't figure out
            the pattern for its existence, so I just extract it from the '2, 3, 4, ...' buttons in the page,
            and save it as 'magicalNumber'. Turns out the magicalNumber is the same for all buttons.
            Then, the page URL is mounted from this number by mountMessagePage(). The entire process is debugged/commented
            because it would be hard to understand what's happening. Basically it extracts the number if it hadn't been
            extracted before, mounts the link, then loads the messages from this link.
        */
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
        String locationRedirect = pageToReadMessages.location;
        String responseCode = pageToReadMessages.responseCode;
        String response = pageToReadMessages.response;
        String responseMessage = pageToReadMessages.responseMessage;
        //System.out.println("responseCode: "+responseCode+" responseMessage: "+responseMessage);
        //if there's no locationRedirect, there was no login timeout
        if (responseCode.equals("200")) {//HTTP OK with no redirection
            Document doc = Jsoup.parse(response);
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
                if (c > 0) {//TODO: explain what the fuck is this c
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
            return new GetMessagesResponse(null, messagesList);

        } else if (locationRedirect!=null && responseCode.equals("302")) {//login probably timed out, server issued redirection to login page
            if (locationRedirect.contains("sistemas.unesp.br/sentinela/login.open.action")) {//if location is login page...
                SentinelaLoginObject relogin = loginToSentinela();
                if (relogin.pageError==null) {
                    getMessages(page);//Calls itself, now that it did login again
                }
            }
        } else {
            GetMessagesResponse.PageError pageError =
                    new GetMessagesResponse(null, null).new PageError(responseCode, responseMessage);
            return new GetMessagesResponse(pageError, null);
        }
        //System.out.println(messagesList);
        return new GetMessagesResponse(null, null);
        //System.out.println(pageToReadMessages.response);
    }

    //---GetMessage and its response object
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
        URL getMessagesURL = new URL(protocol + "://" + domain + "/" + "sentinela" + "/" + "sentinela.viewMessage.action?txt_id="+messageId+"&emailTipo=recebidas");
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
    //---getClasses
    //Gets all the 'classes' (by classes I mean, the classes the student must go)
    public Map<String, List<Map<String, String>>> getClasses() throws Exception {
        List < Map < String, String >> a = new ArrayList < Map < String, String >> ();
        URL getClassesURL = new URL(protocol + "://" + domain + "/" + "academico" + "/aluno/cadastro.horarioAulas.action");
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

                } else {
                }
            } else {
                System.out.println("selecionou empty: "+line);
            }
        }
        return (classesData);
    }
    public class GetGradesResponse{
        public PageError pageError;
        public List < Map < String, String >> grades;
        public GetGradesResponse(PageError pageError,  List < Map < String, String >> grades) {
            this.pageError = pageError;
            this.grades = grades;
        }

        //class to hold errors related to page loading
        public class PageError {
            public String errorCode;
            public String errorMessage;
            public PageError(String errorCode, String errorMessage) {
                this.errorCode = errorCode;
                this.errorMessage = errorMessage;
            }
        }
    }
    public GetGradesResponse getGrades() throws Exception {
        URL getGradesURL = new URL(protocol + "://" + domain + "/" + "academico" + "/selecionar.aluno.action?url=FREQUENCIAS_NOTAS  ");
        SimpleHTTPSRequest.requestObject gradesRequest = sisgradRequest.SimpleHTTPSRequest(getGradesURL, null);
        System.out.println("response code: "+gradesRequest.responseCode);
        System.out.println("location: "+gradesRequest.location);
        System.out.println("response: "+gradesRequest.response);

        System.out.println("ok, let's access"+ gradesRequest.location+"...");
        SimpleHTTPSRequest.requestObject gradesRequestRedirected = sisgradRequest.SimpleHTTPSRequest(new URL(gradesRequest.location), null);
        System.out.println("response code: "+gradesRequestRedirected.responseCode);
        System.out.println("location: "+gradesRequestRedirected.location);
        System.out.println("response: "+gradesRequestRedirected.response);
        //System.out.println("ok, let's access"+ gradesRequest.location+"...");
        return null;
    }
}