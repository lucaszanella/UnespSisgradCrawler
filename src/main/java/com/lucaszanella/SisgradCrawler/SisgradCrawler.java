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
//TODO: prepare this and other web crawlers to load all components in the page in order to perfectly simulate a computer access

public class SisgradCrawler {
    private Boolean debugMode = false;

    public String username;
    private String password;

    private static String protocol = "https";
    private static String domain = "sistemas.unesp.br";

    /**
     * Crazy number that I don't know the utility but won't work without it. Each page button
     * in the messages page has a link to the page, and in this link there is this magicalNumber.
     * I tried removing it and loading but it won't work, even tough the magicalNumber is the same
     * for all page buttons, except the first page button.
     */
    private String magicalNumber;
    private Boolean alreadyLoadedMagicalNumber = false;

    private SimpleHTTPSRequest sisgradRequest = new SimpleHTTPSRequest();

    public long lastAcademicoSuccess = 0;//0 as its initial value, then the first access will assign the current unix date to it
    public long lastLoginSuccess = 0;//same as above

    public static int ONE_MINUTE = 60; //1 minute in seconds
    public static int LOGIN_TIMEOUT = ONE_MINUTE*8;//will try to login if last login was 8 minutes ago or more
    public static int ACADEMICO_TIMEOUT = LOGIN_TIMEOUT;//time to decide that the academico session is still open

    public static int MAX_RECURSIVITY = 3;//number of times a function can call itself to resolve session timeout

    //Result codes
    public static final int OK = 0;
    public static final int WRONG_EMAIL = 1;
    public static final int WRONG_PASSWORD = 2;
    public static final int PAGE_ERROR = 3;
    public static final int TIMEOUT = 4;
    public static final int NOT_FOUND = 5;
    public static final int GENERIC_ERROR = 6;
    public static final int NOT_CONNECTED = 7;
    public static final int RESUMED = 8;

    /**
     * Sisgrad initialization stores the username and password. Later, we just call loginToSentinela().
     */
    public SisgradCrawler(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Page error used by all the response objects returned by any of the methods below
     */
    public class PageError {
        public String errorCode;
        public String errorMessage;
        public PageError(String errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
    }

    //---Sentinela Login and its response object

    //Result object to be sent back. 'error' property is null if no errors detected.
    public class SentinelaLoginObject {
        public String locationRedirect;
        public LoginError loginError;
        public PageError pageError;

        public SentinelaLoginObject(PageError pageError, String locationRedirect, LoginError loginError) {
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
    }
    //Logs to the 'sentinela' module inside Sisgrad's system. It's responsible to load messages.
    public SentinelaLoginObject loginToSentinela() throws Exception {
        //Mounts POST query that's gonna be sent to the login page
        String postQuery = "txt_usuario=" + URLEncoder.encode(this.username, "UTF-8") + "&" + "txt_senha=" + URLEncoder.encode(this.password, "UTF-8");
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
        /*if there's no location http command, then the login didn't succeed and we're back at the same page
         *this is a signal that the information was wrong
         */
        if (locationRedirect==null || (locationRedirect.equals("") || locationRedirect.length()==0)) {
            //if the login didn't succeed, it could be wrong password or any other thing, so let's detect it!
            Element doc = Jsoup.parse(response);
            Elements errors = doc.getElementsByClass("errormsg");
            String errorMsg = errors.first().text().toLowerCase();

            if (errorMsg.contains("senha")) {wrongPassword = true;}
            if (errorMsg.contains("email") || errorMsg.contains("e-mail")) {wrongEmail = true;}

            SentinelaLoginObject.LoginError loginError =
                    new SentinelaLoginObject(null, null, null).new LoginError(wrongPassword, wrongEmail);
            PageError pageError = new PageError(responseCode, responseMessage);
            return new SentinelaLoginObject(pageError,locationRedirect, loginError);
        } else if (locationRedirect.contains("sistemas.unesp.br/sentinela/sentinela.showDesktop.action")) {//login done because it redirected to this page
            return new SentinelaLoginObject(null, locationRedirect, null);
        }
        //If any http error happened, sent it back
        if (!responseCode.equals("302")) {
            PageError pageError =
                    new PageError(responseCode, responseMessage);
            return new SentinelaLoginObject(pageError, locationRedirect, null);
        }
        return new SentinelaLoginObject(null, locationRedirect, null);
    }

    //---Academico Login and its response object
    //Result object to be sent back. 'error' property is null if no errors detected.
    public class AcademicoAccessObject {
        public String locationRedirect;
        public PageError pageError;

        public AcademicoAccessObject(PageError pageError, String locationRedirect) {
            this.locationRedirect = locationRedirect;
            this.pageError = pageError;
        }
    }
    /**
     * Academico module is responsible to load things related to the student information like classes and so
     * It's not a real login like loginToSentinela() but it's needed in order to gather /academico/ pages
     */
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
                    //TODO: limit the recursive calls or it'll loop forever
                    accessAcademico();//Calls itself, now that it did login again
                }
            }
        }
        PageError pageError = new PageError(pageafterLogin.responseCode, pageafterLogin.responseMessage);
        return new AcademicoAccessObject(pageError, pageafterLogin.location);
    }

    /**
     * Whenever a method calls the Sisgrad's server, it can be surprised by some page redirections that are not
     * intended to be part of the experience. These redirections are mainly caused by session timeouts. It turns
     * out that each URL path of the Sisgrad's system can point to a different system, like /sentinela and /academico,
     * each one with its own timeout limit. So, the method below answers if the call successfully fixed the redirection
     * problem by reopening a session. If true, the method that called fixedRedirection() will call itself again to
     * deliver the requested information by the app. The recursivity number is passed in and decreased every time so
     * when it reaches -1, the function stops calling itself, to prevent infinite recursion, in case the system
     * is unable to fix itself.
     */
    public Boolean fixedRedirection(Integer recursivity, String location, String responseCode) {
        if (location!=null && responseCode.equals("302")) {//session probably timed out, server issued redirection to login page
            if (location.contains("sistemas.unesp.br/sentinela")) {//if location is login page, it means login timed out
                try {
                    SentinelaLoginObject reLogin = loginToSentinela();
                    if (reLogin.pageError==null) {
                        System.out.println("redirection to "+location+", should call itself recursively now");
                        if (recursivity>=0){
                            return true;
                        } else {
                            return false;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            } else if (location.contains("sistemas.unesp.br/academico")) {//if location is academico page, academico session timed out
                //TODO: support academico redirection
                return false;
            } else {
                System.out.println("unrecognized redirection: "+ location);
                return false;
            }
        } else {
            return false;
        }
    }
    /**
     * Synchronization is reaaaally important here. Every method of SisgradCrawler will
     * call this to know if they must reLogin (because it times out after like, 10 minutes), or if
     * they can just proceed with the access of the Sisgrad elements. Every method has a built in
     * functionality that tries to reLogin if they get redirected to the login page when trying to
     * access anything, but it's better not to trust it and instead, do the login again when we feel
     * it's going to timeout.
     * So, since every fragment uses this method, synchronization means that only one call will be
     * active in the app. So if the login timed out and 2 fragments call doOrResumeLogin, one will
     * actually log in, and the other will wait. When the first finished, the second will start
     * running, but it will see that a login has just been made and will resume the login (that is,
     * will return a 'resumed' value to the caller, which means that the login is healthy and he can
     * continue with the calls)
     */
    public class DoOrResumeLoginResponseObject {
        public Integer code;
        public PageError pageError;
        public SisgradCrawler.SentinelaLoginObject loginObject;
        public DoOrResumeLoginResponseObject(Integer code, PageError pageError, SisgradCrawler.SentinelaLoginObject loginObject) {
            this.code = code;
            this.pageError = pageError;
            this.loginObject = loginObject;
        }

    }
    //Just wrappers to call doOrResumeLoginWrapped with or without forceRelogin option
    public synchronized DoOrResumeLoginResponseObject doOrResumeLogin() {
        return doOrResumeLoginWrapped(false);
    }
    public synchronized DoOrResumeLoginResponseObject forceLogin() {
        return doOrResumeLoginWrapped(true);
    }

    private synchronized DoOrResumeLoginResponseObject doOrResumeLoginWrapped(Boolean forceRelogin) {
        //TODO: identify if never logged in, or if login is about to timeout. Differentiate between timed out, about to time out, and session open
        System.out.println("DoOrResumeLogin called");
        Long currentUnix = new Date().getTime()/1000;
        /*
        * Just re-login, don't bother deciding if it's a necessity. Normally will be called when
        * a request got a LoginTimedOut exception because it was redirected by a location HTTP header.
        */
        if (forceRelogin) {
            System.out.println("forcing relogin");
            try {
                SisgradCrawler.SentinelaLoginObject loginObject = this.loginToSentinela();
                //TODO: verify force reLogin result
                System.out.println("force login successful");
                return new DoOrResumeLoginResponseObject(OK, null, loginObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
            //TODO: break this if in the right way
        }
        /*
        * Now we'll verify if the login timed out or not and fix this if it did, or just return RESUMED if it didn't
        */
        if ((currentUnix - this.lastLoginSuccess) >= LOGIN_TIMEOUT || this.lastLoginSuccess == 0) {//lastLoginSuccess==0 means it was just created, never assigned a value
            System.out.println("logging in...");
            try {
                SisgradCrawler.SentinelaLoginObject loginObject = this.loginToSentinela();
                if (loginObject.loginError != null) {
                    System.out.println("something wrong with login information:");
                    if (loginObject.loginError.wrongEmail) {
                        System.out.println("wrong email");
                        return new DoOrResumeLoginResponseObject(WRONG_EMAIL, null, loginObject);
                    }
                    if (loginObject.loginError.wrongPassword) {
                        System.out.println("wrong password");
                        return new DoOrResumeLoginResponseObject(WRONG_PASSWORD, null, loginObject);
                    }
                } else if (loginObject.pageError != null) {
                    System.out.println("error with the page loading, code is: "
                            + loginObject.pageError.errorCode + " message is " +
                            loginObject.pageError.errorMessage
                    );
                    return new DoOrResumeLoginResponseObject(PAGE_ERROR, loginObject.pageError, loginObject);
                } else {
                    this.lastLoginSuccess = new Date().getTime() / 1000;//current unix time
                    return new DoOrResumeLoginResponseObject(OK, null, loginObject);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if ((currentUnix - this.lastLoginSuccess) < LOGIN_TIMEOUT) {
            //Didn't timeout, let's just resume it
            System.out.println("login resumed");
            return new DoOrResumeLoginResponseObject(RESUMED, null, null);
        }
        return null;
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
    }
    public GetMessagesResponse getMessages(int page) throws Exception {
        return getMessagesWrapped(page, MAX_RECURSIVITY);
    }
    //Gets the messages from the system.
    private GetMessagesResponse getMessagesWrapped(int page, int recursivity) throws Exception {
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
        //If the HTTP code is 200, there was no login timeout, else if it's 302, there was and we must deal with it.
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

        } else if (fixedRedirection(recursivity, locationRedirect, responseCode)) {
            /* If fixedRedirection() returned true, it means it successfully fixed the redirection, which was
             * probably caused by a session timeout, so in order to not interrupt the method call, we return
             * the result of the new getMessagesWrapped, which will PROBABLY not suffer from another redirection,
             * but in case it happens indefinitely, recursivity will decrease until fixedRedirection will return false
             * and everything will stop.
             */
            return getMessagesWrapped(page, (recursivity-1));
        } else {
            return new GetMessagesResponse(new PageError(responseCode, responseMessage), null);
        }
    }
    //---GetMessage and its response object
    public class GetMessageResponse{
        public String author;
        public String title;
        public String message;
        public Map<String, String> attachments;
        public PageError pageError;
        public GetMessageResponse(PageError pageError,String author, String title, String message, Map<String, String> attachments) {
            this.author = author;
            this.title = title;
            this.message = message;
            this.attachments = attachments;
            this.pageError = pageError;
        }
    }
    public GetMessageResponse getMessage (String messageId, Boolean html) throws Exception {
        return getMessageWrapped(messageId, html, -1);
    }
    public GetMessageResponse getMessageWrapped (String messageId, Boolean html, Integer recursivity) throws Exception {//this method is a mess. TODO: make it better
        //System.out.println("hi, i'm getting message for id "+ messageId );
        URL getMessagesURL = new URL(protocol + "://" + domain + "/" + "sentinela" + "/" + "sentinela.viewMessage.action?txt_id="+messageId+"&emailTipo=recebidas");
        if (debugMode) {System.out.println(" the url is "+ getMessagesURL.toString());}
        SimpleHTTPSRequest.requestObject messageRequest = sisgradRequest.SimpleHTTPSRequest(getMessagesURL, null);
        String locationRedirect = messageRequest.location;
        String responseCode = messageRequest.responseCode;
        String response = messageRequest.response;
        String responseMessage = messageRequest.responseMessage;
        if (responseCode.equals("200")) {//HTTP OK with no redirection
            Document doc = Jsoup.parse(response);
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
                        /*
                        System.out.println("SELECTION: ");
                        System.out.println("first: "+(!tags.get(0).getTag().select("td").isEmpty()
                        && tags.get(0).getTag().select("td").attr("bgcolor").equals("white")
                        && tags.size()==1));
                        System.out.println("second: "+(tags.get(0).getTag().getElementsByTag("br").size()>2 && tags.size()==1));
                        */

                        /*
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
            return new GetMessageResponse(null, from, title, message, attachmentsList);
        } else if (fixedRedirection(recursivity, locationRedirect, responseCode)) {
            /* If fixedRedirection() returned true, it means it successfully fixed the redirection, which was
             * probably caused by a session timeout, so in order to not interrupt the method call, we return
             * the result of the new getMessagesWrapped, which will PROBABLY not suffer from another redirection,
             * but in case it happens indefinitely, recursivity will decrease until fixedRedirection will return false
             * and everything will stop.
             */
            return getMessageWrapped(messageId, html, (recursivity-1));
        } else {
            return new GetMessageResponse(new PageError(responseCode, responseMessage), null, null, null, null);
        }
    }
    //---getClasses
    public class GetClassesResponse{
        public PageError pageError;
        public Map<String, Map<String, ClassInfo>> week;
        public GetClassesResponse(PageError pageError, Map<String, Map<String, ClassInfo>> week) {
            this.pageError = pageError;
            this.week = week;
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
    public GetClassesResponse getClasses() throws Exception {
        return getClassesWrapped(MAX_RECURSIVITY);
    }
    //Gets all the 'classes' (by classes I mean, the classes the student must go)
    private GetClassesResponse getClassesWrapped(int recursivity) throws Exception {
        URL getClassesURL = new URL(protocol + "://" + domain + "/" + "academico" + "/aluno/cadastro.horarioAulas.action");
        SimpleHTTPSRequest.requestObject classesRequest = sisgradRequest.SimpleHTTPSRequest(getClassesURL, null);

        SimpleHTTPSRequest.requestObject classesRequestRedirected = sisgradRequest.SimpleHTTPSRequest(new URL(classesRequest.location), null);
        SimpleHTTPSRequest.requestObject classesRequestRedirectedAgain = sisgradRequest.SimpleHTTPSRequest(new URL(classesRequestRedirected.location), null);
        String locationRedirect = classesRequestRedirectedAgain.location;
        String responseCode = classesRequestRedirectedAgain.responseCode;
        String response = classesRequestRedirectedAgain.response;
        String responseMessage = classesRequestRedirectedAgain.responseMessage;
        if (responseCode.equals("200")) {
            Document doc = Jsoup.parse(response);
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
                    if (days.isRow(i) && days.getRowTags(i).get(days.getColumnIndex(day, 0)).hasText()) {//it could be a header
                        String hourOfClass = days.getRowTags(i).get(0).text();
                        //TODO: tolerate ç as c and á, é, í, ... as a, e, i.
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
            return new GetClassesResponse(null, week);
        } else if (fixedRedirection(recursivity, locationRedirect, responseCode)) {
            /* If fixedRedirection() returned true, it means it successfully fixed the redirection, which was
             * probably caused by a session timeout, so in order to not interrupt the method call, we return
             * the result of the new getMessagesWrapped, which will PROBABLY not suffer from another redirection,
             * but in case it happens indefinitely, recursivity will decrease until fixedRedirection will return false
             * and everything will stop.
             */
            return getClassesWrapped(recursivity-1);
        } else {
            return new GetClassesResponse(new PageError(responseCode, responseMessage), null);
        }
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

    //mounts the URL to load a message page in the Sisgrad's web page
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

    //TODO: add safe splitting to magicalNumber
    public String getMagicalNumber(SimpleHTTPSRequest.requestObject page) {
        Document doc = Jsoup.parse(page.response);
        Elements pageNumbers = doc.getElementsByClass("listagemTopo");
        Elements pageLinks = pageNumbers.select("a");
        //if (debugMode) {System.out.println("pageLinks: "+ pageLinks);}
        String magicalNumber = "";
        for (Element pageLink: pageLinks) {
            magicalNumber = pageLink.attr("href").split("d-")[1].split("-p")[0];
            break; //They're all the same, so I break here, but if something change in the future, here's the loop so I can use :)
        }
        return magicalNumber;
    }
}