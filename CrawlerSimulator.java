import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;


/**
 * This simulates the web crawler. The web crawler is given an array of seed
 * URLs to begin with. The user can also set the maximum number of URLs to
 * crawl and the maximum number of crawlers to call at any point in time.
 * @author benedict
 *
 */
public class CrawlerSimulator {

	/**
	 * The main method that executes the web crawler via the Master.
	 * @param args
	 */
	public static void main(String[] args) {
		String[] seedUrls = {
				"http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.248.5252"
				};
		int maxURLs = 100;
		int maxCrawlers = 128;
		
		try {
			Master master = new Master(seedUrls, maxURLs, maxCrawlers);
			String[] res = master.startCrawl();
			
			System.out.println(Arrays.toString(res));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
