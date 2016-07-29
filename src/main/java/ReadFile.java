
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by lucaszanella on 7/29/16.
 */
public class ReadFile {
    //Just a method I borrowed from internet to open a simple text file
    //and convert it to a Sring
    public static String Read(String fileName) throws IOException {
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
