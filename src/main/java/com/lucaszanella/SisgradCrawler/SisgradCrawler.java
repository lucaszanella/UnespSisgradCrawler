package com.lucaszanella.SisgradCrawler;

import com.lucaszanella.SimpleRequest.SimpleHTTPSRequest;
import com.lucaszanella.jSoupTable;
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
            jSoupTable messagesTable = new jSoupTable(table);
            int tableSize = messagesTable.getAllRows().size();
            List < Map < String, String >> messagesList = new ArrayList <> ();
            for (int i = 0; i<tableSize; i++) {//Start at 1 because 0 is a header tag
                Map < String, String > messageRow = new HashMap <> ();
                if (messagesTable.isRow(i)) {
                    String title = messagesTable.getRowTags(i).get(messagesTable.getColumnIndex("assunto", 0)).text();
                    String author = messagesTable.getRowTags(i).get(messagesTable.getColumnIndex("enviado por", 0)).text();
                    //TODO: deal with nullPointerException when the page doesn't appear as intended
                    String messageIdString = messagesTable.getRowTags(i).
                            get(messagesTable.getColumnIndex("assunto", 0)).
                            getElementsByTag("a").first().attr("href");
                    String sentDate = messagesTable.getRowTags(i).get(messagesTable.getColumnIndex("enviado em", 0)).text();
                    String readDate = messagesTable.getRowTags(i).get(messagesTable.getColumnIndex("lido em", 0)).text();
                    String messageId = messageIdString.split("\\(")[1].split("\\)")[0];

                    messageRow.put("title", title);
                    messageRow.put("author", author);
                    messageRow.put("messageId", messageId.replace("'", ""));
                    messageRow.put("sentDate", sentDate);
                    messageRow.put("readDate", readDate);

                    String dateString = sentDate.split("\\.")[0];
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    Date date = dateFormat.parse(dateString);
                    long unixTime = (long) date.getTime() / 1000;
                    messageRow.put("sentDateUnix", String.valueOf(unixTime));
                    messagesList.add(messageRow);
                } else {
                    //System.out.println(messagesTable.getRowTags(i).get(2).getTag());
                }
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

    public GetMessageResponse getMessage (String messageId, Boolean html) throws Exception {//this method is a mess. TODO: make it better
        //System.out.println("hi, i'm getting message for id "+ messageId );
        URL getMessagesURL = new URL(protocol + "://" + domain + "/" + "sentinela" + "/" + "sentinela.viewMessage.action?txt_id="+messageId+"&emailTipo=recebidas");
        if (debugMode) {System.out.println(" the url is "+ getMessagesURL.toString());}
        SimpleHTTPSRequest.requestObject messageRequest = sisgradRequest.SimpleHTTPSRequest(getMessagesURL, null);
        Document doc = Jsoup.parse(messageRequest.response);
        Element messageForm = doc.select("form").get(0);//gets the first form. TODO: change this to get the largest form or something like that
        Element messageTable = messageForm.select("table").get(0);
        jSoupTable table = new jSoupTable(messageTable);
        //Since this is not a table with header, we can't get columns indexes, so we'll need to iterate through each row,
        //then in each row, we're gonna search the column values that match our needs.
        String from = null;
        String title = null;
        String attachments = null;
        String message = null;
        Map<String, String> attachmentsList = new HashMap<>();

        for (int k = 0; k<table.getAllRows().size(); k++) {
            List<Element> tags = table.getAllRows().get(k);
            //for (List<jSoupTable.Tag> tags: table.getAllRows()) {
            for (int i=0; i<tags.size(); i++) {
                //Identify the sender of the message
                if (tags.get(0).text().toLowerCase().contains("de") && tags.size()==2) {
                    from = tags.get(1).text();
                }
                //Identify the subject or title of the message
                if (tags.get(0).text().toLowerCase().contains("assunto") && tags.size()==2) {
                    title = tags.get(1).text();
                }
                //Identify the attachments of the message
                if (tags.get(0).text().toLowerCase().contains("anexo") && tags.size()==2) {
                    Elements linksOfAttachments  = tags.get(1).select("a");
                    //containsAttachments = true;
                    for (Element linkOfAttachment:linksOfAttachments) {
                        //System.out.println("linkOfAttachment: "+linkOfAttachment.html()+"attr: "+linkOfAttachment.attr("href"));
                        attachmentsList.put(linkOfAttachment.text(),linkOfAttachment.attr("href"));
                    }

                }
                /**
                 * This is the most important part, the message content. Several techniques were
                 * used to identify the message, but attention, web crawling is never perfect :(.
                 * The only unique characteristics of the messages row was having bgcolor=white or
                 * having a lots of <br> (in case of multiline messages). In both cases, the column size
                 * was 1.
                 */
                if      (
                                (!tags.get(0).select("td").isEmpty()
                                && tags.get(0).select("td").attr("bgcolor").equals("white")
                                && tags.size()==1)
                                ||
                                (tags.get(0).getElementsByTag("br").size()>2 && tags.size()==1)
                        )
                {
                    //System.out.println("SELECTION: ");
                    //System.out.println("first: "+(!tags.get(0).getTag().select("td").isEmpty()
                            //&& tags.get(0).getTag().select("td").attr("bgcolor").equals("white")
                            //&& tags.size()==1));
                    //System.out.println("second: "+(tags.get(0).getTag().getElementsByTag("br").size()>2 && tags.size()==1));
                    /**
                     * Sometimes is useful to request for the HTML content of the message, other times for the text content.
                     * I could always return the HTML and extract the text with code but it requires a minimum API greater
                     * than the one I am supporting in Android, so let's parse the HTML or text content here and send it.
                     * PS: I could have used jSoup in the Android app to extract the text, but I wanted to use here since
                     * it's already loaded in memory.
                     */
                    if (html) {
                        message = tags.get(0).html();
                    } else {
                        message = tags.get(0).text();
                    }
                }
            }
        }
        /*
        System.out.println("title: "+title);
        System.out.println("from: "+from);
        System.out.println("attachments: "+attachmentsList);
        System.out.println("message: "+message);
        */

        return new GetMessageResponse(from, title, message, attachmentsList);
    }
    //---getClasses
    public class GetClassesResponse{
        public PageError pageError;
        public Map<String, Map<String, ClassInfo>> week;
        public GetClassesResponse(PageError pageError, Map<String, Map<String, ClassInfo>> week) {
            this.pageError = pageError;
            this.week = week;
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
    public class ClassInfo {
        String name;
        String code;
        String place;
        public ClassInfo(String name, String code, String place) {
            this.name = name;
            this.code = code;
            this.place = place;
        }
        public String toString() {
            return("Class name: "+this.name+", Class code: "+this.code+" Class location:" +this.place);
        }
    }

    //Gets all the 'classes' (by classes I mean, the classes the student must go)
    public GetClassesResponse getClasses() throws Exception {
        URL getClassesURL = new URL(protocol + "://" + domain + "/" + "academico" + "/aluno/cadastro.horarioAulas.action");
        SimpleHTTPSRequest.requestObject classesRequest = sisgradRequest.SimpleHTTPSRequest(getClassesURL, null);

        SimpleHTTPSRequest.requestObject classesRequestRedirected = sisgradRequest.SimpleHTTPSRequest(new URL(classesRequest.location), null);
        SimpleHTTPSRequest.requestObject classesRequestRedirectedAgain = sisgradRequest.SimpleHTTPSRequest(new URL(classesRequestRedirected.location), null);
        if (classesRequestRedirectedAgain.responseCode.equals("200")) {
            Document doc = Jsoup.parse(classesRequestRedirectedAgain.response);
            //Tables
            Element classesInfo = doc.getElementsByClass("listagem").first();
            Element daysTable = doc.select("table").get(1);
            //Interpret tables with jSoupTable class
            jSoupTable days = new jSoupTable(daysTable);
            jSoupTable classes = new jSoupTable(classesInfo);

            Map<String, Map<String, ClassInfo>> week = new LinkedHashMap<>();//Map<Day, Map<Hour, Class>>, the table represented as a map
            List<String> daysOfWeek = new ArrayList<>(Arrays.asList("segunda", "terça", "quarta", "quinta", "sexta", "sábado"));//just a list of days of the week

            //Iterates through the table of classes information (like teachers, name of the class, total of hours...)

            //Iterates through the table of classes per each day and hour
            for (String day : daysOfWeek) {//For each day, we're gonna add an entry in the 'week' Map
                Map<String, ClassInfo> dayColumn = new LinkedHashMap<>();//This is the Map<Hour, Class> which will be mounted for each day of the week
                for (int i = 0; i < days.getAllRows().size(); i++) {
                    if (days.isRow(i)) {//it could be a header
                        String hourOfClass = days.getRowTags(i).get(0).text();
                        //TODO: tolerate ç as c and á, é, í, ... as a, e, i.
                        //System.out.println("day "+day+" has index "+days.getColumnIndex(day, 0));
                        //TODO: VERIFY IF IT'S NOT EMPTY AND IF THE SPLIT IS POSSIBLE
                        //System.out.println("DEBUG SPLIT: "+days.getRowTags(i).get(days.getColumnIndex(day, 0)).text());
                        String classTitle = "";
                        String classCode = "";
                        String classLocation = "";
                        if (!days.getRowTags(i).get(days.getColumnIndex(day, 0)).getElementsByTag("div").isEmpty()) {
                            classTitle = days.getRowTags(i).get(days.getColumnIndex(day, 0)).getElementsByTag("div").first().attr("title");
                        }
                        if (days.getRowTags(i).get(days.getColumnIndex(day, 0)).text().contains("/")) {
                            try {
                                classCode = days.getRowTags(i).get(days.getColumnIndex(day, 0)).text().split("/")[0];
                                classLocation = days.getRowTags(i).get(days.getColumnIndex(day, 0)).text().split("/")[1];
                            } catch (Exception e) {

                            }
                        }
                        ClassInfo aboutClass = new ClassInfo(
                                classTitle,
                                classCode,
                                classLocation
                        );
                        dayColumn.put(hourOfClass, aboutClass);
                    }
                }
                week.put(day, dayColumn);
            }
            //System.out.println(week);
            return new GetClassesResponse(null, week);
        } else if (classesRequestRedirectedAgain.location!=null && classesRequestRedirectedAgain.responseCode.equals("302")) {//login probably timed out, server issued redirection to login page
            if (classesRequestRedirectedAgain.location.contains("sistemas.unesp.br/sentinela/login.open.action")) {//if location is login page...
                SentinelaLoginObject relogin = loginToSentinela();
                if (relogin.pageError==null) {
                    getClasses();//Calls itself, now that it did login again
                }
            }
        } else {
            GetClassesResponse.PageError pageError =
                    new GetClassesResponse(null, null).new PageError(classesRequestRedirectedAgain.responseCode, classesRequestRedirectedAgain.responseMessage);
            return new GetClassesResponse(pageError, null);
        }
        return (null);
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

        System.out.println("ok, let's access "+ gradesRequest.location+"...");
        SimpleHTTPSRequest.requestObject gradesRequestRedirected = sisgradRequest.SimpleHTTPSRequest(new URL(gradesRequest.location), null);
        System.out.println("response code: "+gradesRequestRedirected.responseCode);
        System.out.println("location: "+gradesRequestRedirected.location);
        //System.out.println("response: "+gradesRequestRedirected.response);
        Document doc = Jsoup.parse(gradesRequestRedirected.response);
        Element gradesTable = doc.getElementById("tabelaNotas");
        jSoupTable gradlesJsoupTable = new jSoupTable(gradesTable);
        //System.out.println(gradlesJsoupTable.getAllRowStrings());
        //System.out.println("ok, let's access"+ gradesRequest.location+"...");
        return null;
    }
}