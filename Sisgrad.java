import java.net.URL;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;
//import org.jsoup.*;

public class Sisgrad {
    //System.setProperty("jsse.enableSNIExtension", "false");

    private static String baseurl = "https://sistemas.feg.unesp.br/sentinela/";
    private static String login_action = "login.action";
    private static String login = baseurl+login_action;
    private static String getMessagesAction = "sentinela.openMessage.action?emailTipo=recebidas";
    private static String messages = baseurl+getMessagesAction;
    //String viewMessagesAction = "sentinela.viewMessage.action?txt_id="+msgID+"&emailTipo=recebidas";
    //String viewMessages = baseurl+viewMessagesAction;
    public static void main(String[] args) throws Exception {
      String httpsURL = login;
      System.out.println(httpsURL);
      URL myurl = new URL(httpsURL);
      HttpsURLConnection con = (HttpsURLConnection)myurl.openConnection();
      InputStream ins = con.getInputStream();
      InputStreamReader isr = new InputStreamReader(ins);
      BufferedReader in = new BufferedReader(isr);

      String inputLine;

      while ((inputLine = in.readLine()) != null)
      {
        System.out.println(inputLine);
      }

      in.close();
    }

}
