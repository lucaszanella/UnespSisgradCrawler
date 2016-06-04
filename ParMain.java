import com.lucaszanella.UnespSisgradCrawler.*;
import com.lucaszanella.ParthenonCrawler.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ParMain {
    public static void main(String[] args) throws Exception{
      final String login_data = readFile("parthenon.txt");
      String[] parts = login_data.split("\\r?\\n");
      final String username = parts[0].split("=")[1];
      final String password = parts[1].split("=")[1];
      //SimpleRequest parthenonRequest = new SimpleRequest();
      ParthenonCrawler myCrawler = new ParthenonCrawler();
        myCrawler.doLogin(username, password);
        
    }
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
