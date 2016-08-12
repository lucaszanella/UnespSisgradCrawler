import com.lucaszanella.SisgradCrawler.SisgradCrawler;
import com.lucaszanella.jSoupTable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;

public class MainSisgrad {
    public static void main(String[] args) throws Exception {
        //TODO: if account.txt does not exists, prompt asking login information
        if (false) {
            //DEBUG TABLE PARSER
            String table = ReadFile.Read("table.txt");
            Document doc = Jsoup.parse(table);
            Element tableElement = doc.getElementById("tabelaNotas");
            jSoupTable myTable = new jSoupTable(tableElement);
            System.out.println(myTable.getRowTags(0).get(0).tagName());
            System.out.println(myTable.getRowStrings(0));
            System.out.println(myTable.getRowStrings(1));
            System.out.println(myTable.getColumnTags(0).get(0));
            System.out.println(myTable.getColumnStrings(0));
            System.out.println("header index:" + myTable.getColumnIndex("turma", 0));
            System.out.println("tag: " + myTable.getRowTags(1).get(myTable.getColumnIndex("turma", 0)));
        }

        /*
        System.out.println(myTable.getRowStrings(0));
        System.out.println(myTable.getRowStrings(1));
        System.out.println(myTable.getRowStrings(2));
        System.out.println("AllRowStrings: "+myTable.getAllRowStrings());
        System.out.println(myTable.getColumnElements(0));
        System.out.println(myTable.getColumnStrings(0));
        */
        //System.out.println("Column 0: "+myTable.getValuesByColumn(6));

        //Read username information from account.txt
        String login_data = ReadFile.Read("account.txt");
        String[] parts = login_data.split("\\r?\\n");
        String username = parts[0].split("=")[1];
        String password = parts[1].split("=")[1];
        if (true) {
            //Creates the login object
            SisgradCrawler sisgradCrawler = new SisgradCrawler(username, password);
            SisgradCrawler.SentinelaLoginObject loginObject = sisgradCrawler.loginToSentinela();//logs in
            if (loginObject.loginError != null) {
                System.out.println("something wrong with login information:");
                if (loginObject.loginError.wrongEmail) {
                    System.out.print(" wrong email");
                }
                if (loginObject.loginError.wrongPassword) {
                    System.out.print(" wrong password");
                }
            } else if (loginObject.pageError != null) {
                System.out.println("error with the page loading, code is: "
                        + loginObject.pageError.errorCode + " message is " +
                        loginObject.pageError.errorMessage
                );
            } else {
                System.out.println("logged in, location Redirect is: " + loginObject.locationRedirect);
                System.out.println("now gonna push content from server...");

                SisgradCrawler.GetMessagesResponse messages = sisgradCrawler.getMessages(0);//page 0
                System.out.println("first message, metadata: " + messages.messages.get(0));
                String mId = sisgradCrawler.getMessages(0).messages.get(0).get("messageId");
                System.out.println("first message, content: " + sisgradCrawler.getMessage(mId, true).message);//true means: gather message formatted in HTML
                System.out.println("first message, attachments: " + sisgradCrawler.getMessage(mId, true).attachments);//true means: gather message formatted in HTML

                System.out.println("accessing academico...");
                SisgradCrawler.AcademicoAccessObject academicoAccess = sisgradCrawler.accessAcademico();
                if (academicoAccess.pageError == null) {
                    System.out.println("academico accessed");
                } else {
                    //if we got redirected to https://sistemas.unesp.br/academico/common.home.action, then, according to my late tests,
                    //the academico access went fine.
                    if (academicoAccess.locationRedirect != null && academicoAccess.locationRedirect.contains("sistemas.unesp.br/academico/common.home.action")) {
                        System.out.println("academico access probably successful");
                    } else {
                        System.out.println("something went wrong. HTTP error: " + academicoAccess.pageError.errorCode + " error message: " + academicoAccess.pageError.errorMessage);
                    }
                }
                System.out.println("getting classes...");
                SisgradCrawler.GetClassesResponse classesResponse = sisgradCrawler.getClasses();
                System.out.println("first class at 'Segunda'");
                System.out.println(classesResponse.week.get("segunda").get(
                        new ArrayList<>(classesResponse.week.get("segunda").keySet()).get(0)));//ArrayList is needed because keySet() returns a set, in which we cannot iterate
                System.out.println("classes at 'Segunda'");
                System.out.println(classesResponse.week.get("segunda"));
                System.out.println("getting grades...");
                SisgradCrawler.GetGradesResponse gradesResponse = sisgradCrawler.getGrades();
            }
        }
        //Creates request threads
        //List < Thread > requestThreads = new ArrayList < Thread > ();
        //initializeMessageLoaderThread(requestThreads, login, 0);

        //login.loginToAcademico();//logs to academico to gather student info
        //Map<String, List<Map<String, String>>> getClassesRequest = login.getClasses();

        //String mId = login.getMessages(0).get(0).get("messageId");
        //System.out.println(login.getMessage("20055868", true).attachments);
        //System.out.println(login.getMessage("20086397", true).attachments);
        //System.out.println(getClassesRequest);
        //SimpleRequest classesRequest = new SimpleRequest(domain+"/"+classesPage, new String(), login.cookies);
        //System.out.println(classesRequest.response);
        //System.out.println(login.getClasses());
    }
    /*
    public static void initializeMessageLoaderThread(List < Thread > threads, final SisgradCrawler sisgrad, final int page) {
        Thread t = new Thread() {
            public void run() {
                try {
                    System.out.println("starting new thread to request page: " + page);

                    SisgradCrawler request = sisgrad;
                    List < Map < String, String >> messages = request.getMessages(page);

                    System.out.println(messages.get(0));
                    System.out.println("ended request of page " + page);

                } catch (Exception e) {

                }
            }
        };
        t.start();
        threads.add(t);
    }
    */
}
