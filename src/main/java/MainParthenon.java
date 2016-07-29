import com.lucaszanella.ParthenonCrawler.ParthenonCrawler;

public class MainParthenon {
    public static void main(String[] args) throws Exception{
        String login_data = ReadFile.Read("parthenon.txt");
        String[] parts = login_data.split("\\r?\\n");
        String username = parts[0].split("=")[1];
        String password = parts[1].split("=")[1];
        //SimpleRequest parthenonRequest = new SimpleRequest();
        ParthenonCrawler myCrawler = new ParthenonCrawler();
        myCrawler.doLogin(username, password);

    }
}
