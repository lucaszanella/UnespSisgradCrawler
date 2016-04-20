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

public class Main {
  static Boolean debugMode = true;
  public static void initializeMessageLoaderThread(List<Thread> threads, final SisgradCrawler sisgrad, final int page) {
    Thread t = new Thread(){
      public void run(){
        try {
          if (debugMode) {System.out.println("starting new thread to request page: "+page);}
          SisgradCrawler request = sisgrad;
          List<Map<String,String>> messages = request.getMessages(page);
          System.out.println(messages.get(0));
          if (debugMode) {System.out.println("ended request of page "+page);}
        } catch (Exception e) {
          
        }
      }
    };
    t.start();
    threads.add(t);
  }
  public static void main(String[] args) throws Exception{
    //Loads login data from account.txt
    final String baseurl = "https://sistemas.unesp.br/sentinela/";
    //Read username information from account.txt
    final String login_data = readFile("account.txt");
    String[] parts = login_data.split("\\r?\\n");
    final String username = parts[0].split("=")[1];
    final String password = parts[1].split("=")[1];
    final String magicalNumber = "4827107";
    final SisgradCrawler login = new SisgradCrawler(username, password, baseurl, magicalNumber);
    login.connect();
    System.out.println("logged in, now gonna push content from server");
    List<Thread> requestThreads = new ArrayList<Thread>();
    initializeMessageLoaderThread(requestThreads, login, 0);
    initializeMessageLoaderThread(requestThreads, login, 1);
  }
  //Just a method I borrowed from internet to open a simple text file
  //and convert it to a Sring
   private static String readFile(String fileName) throws IOException {
     BufferedReader br = new BufferedReader(new FileReader(fileName));
     try {
          StringBuilder sb = new StringBuilder();
          String line = br.readLine();

          while (line != null) {
              sb.append(line);
              sb.append("\n");
              line = br.readLine();
          }
          return sb.toString();
     } finally {
          br.close();
     }
   }
}