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
  public static void main(String[] args) throws Exception{
    //Loads login data from account.txt
    String baseurl = "https://sistemas.unesp.br/sentinela/";
    //Read username information from account.txt
    String login_data = readFile("account.txt");
    String[] parts = login_data.split("\\r?\\n");
    String username = parts[0].split("=")[1];
    String password = parts[1].split("=")[1];
    String magicalNumber = "4827107";
    SisgradCrawler sisgrad = new SisgradCrawler(username, password, baseurl, magicalNumber);
    sisgrad.connect();
    List<Map<String,String>> messages = sisgrad.getMessages(0);
    List<Map<String,String>> messages1= sisgrad.getMessages(1);
    System.out.println(messages);
    System.out.println(messages1);
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