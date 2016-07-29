import com.lucaszanella.SisgradCrawler.SisgradCrawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainSisgrad {
    public static void main(String[] args) throws Exception {
        //Read username information from account.txt
        String login_data = ReadFile.Read("account.txt");
        String[] parts = login_data.split("\\r?\\n");
        String username = parts[0].split("=")[1];
        String password = parts[1].split("=")[1];

        //Creates the login object
        SisgradCrawler login = new SisgradCrawler(username, password);
        login.loginToSentinela();//logs in

        System.out.println("logged in, now gonna push content from server");

        //Creates request threads
        List < Thread > requestThreads = new ArrayList < Thread > ();
        //initializeMessageLoaderThread(requestThreads, login, 0);

        login.loginToAcademico();//logs to academico to gather student info
        Map<String, List<Map<String, String>>> getClassesRequest = login.getClasses();

        String mId = login.getMessages(0).get(0).get("messageId");
        //System.out.println(login.getMessage("20055868", true).attachments);
        //System.out.println(login.getMessage("20086397", true).attachments);
        System.out.println(getClassesRequest);
        //SimpleRequest classesRequest = new SimpleRequest(domain+"/"+classesPage, new String(), login.cookies);
        //System.out.println(classesRequest.response);
        //System.out.println(login.getClasses());
    }
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
}
