import com.lucaszanella.SimpleRequest.*;
import com.lucaszanella.LattesCrawler.*;

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

public class MainLattes {
 static Boolean debugMode = true;

 public static void main(String[] args) throws Exception {
   LattesCrawler lattes = new LattesCrawler();
   String lattesPage = lattes.getSearchPage();
   LattesCrawler.requestObject lattesSearch = lattes.search("Suzete Maria Silva Afonso");
   System.out.println(lattesSearch.teacherURLImage);
   //System.out.println(lattesSearch.substring(8000));
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
