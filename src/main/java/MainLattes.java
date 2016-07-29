import com.lucaszanella.LattesCrawler.LattesCrawler;

public class MainLattes {
 static Boolean debugMode = true;

 public static void main(String[] args) throws Exception {
  LattesCrawler lattes = new LattesCrawler();
  String lattesPage = lattes.getSearchPage();
  LattesCrawler.requestObject lattesSearch = lattes.search("Suzete Maria Silva Afonso");
  System.out.println(lattesSearch.teacherURLImage);
  //System.out.println(lattesSearch.substring(8000));
 }
}
