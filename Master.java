import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * This is the crawler handler for the concurrent web crawler. The following
 * are a few features of this handler:
 * - Users can set the maximum number of crawlers (threads). This provides
 * 		the user with control over how much resources to use for the crawler.
 * - Each thread request has a delay to prevent an "accidental" DOS attack.
 * - Threads are created and controlled by the ThreadPoolExecutor class. This
 * 		class acts as a pool and thread scheduler. The size of the pool is
 * 		bounded by the maximum number of threads set by the user.
 * - The crawlers terminate when encountering either conditions:
 * 		a) The crawler has reached a dead end, i.e. no links left to visit.
 * 		b) The crawler has reached the maximum number of links requested.
 * - Results are obtained and written to "results.txt" at the end of the crawl.
 * 
 * The following are some of the performance optimization techniques used. Some
 * optimizations are found in Crawler.java.
 * - Only HTML pages (links without extensions or ending with ".html"|".htm".
 * 		Reasons:		
 * 		1) As compared to other file types (.jpg|.pdf|.txt|etc), the html page
 * 		type will more likely result in more links. This is especially
 * 		important as each domain can only be visited once. (You will want to
 * 		pick the page in the domain that can yield links.)
 * 		2) Html pages will generally be smaller in size than the other mentioned
 * 		file types. This greatly reduces crawling time and does not compromise
 * 		the number of links found.
 * - Jsoup was used as opposed to pure regex for HTML parsing. Jsoup is a more
 * 		optimized parser with better results on obtaining links.
 * - The StringBuffer was used to obtain the HTML response page instead of
 * 		naive string concatenation. This cut down processing time by ~20% on
 * 		large webpages.
 * @author benedict
 *
 */
public class Master {

	private static final int REQUEST_DELAY = 0;
	private static final String RESULTS_FILENAME = "results.txt";
	private static final String WHITESPACE = "                               ";
	private int m_maxPagesToCrawl;
	private HashSet<String> m_seenUrls = new HashSet<String>();
	private ArrayList<URI> m_urisRepository = new ArrayList<>();
	private String[] m_seedUrls = null;
	private ThreadPoolExecutor m_executorPool;
	private int m_linkCounts = 0;
	private int m_maxCrawlers = 1;
	private ArrayList<String> m_results = new ArrayList<String>();
    private ArrayList<String> m_jsonRepository = new ArrayList<>();
    private static final Object m_jsonRepositoryLock = new Object();
	
	/**
	 * Constructor for Master.
	 * @param seedUrls The URLs to start crawling with.
	 * @param maxPagesToCrawl The maximum number of pages to crawl.
	 * @param numOfCrawlers The maximum number of crawlers to use.
	 * @throws URISyntaxException
	 */
	public Master(String[] seedUrls, int maxPagesToCrawl, int numOfCrawlers) 
			throws URISyntaxException {
		if (seedUrls.length < 1) {
			throw new IllegalArgumentException(
					"There must be at least one seed url.");
		}
		
		if (numOfCrawlers < 1) {
			throw new IllegalArgumentException(
					"Number of crawlers must be more than 0.");
		}

		this.m_seedUrls = seedUrls;
		this.m_maxPagesToCrawl = maxPagesToCrawl;
		this.m_maxCrawlers = numOfCrawlers;
		this.m_executorPool = new ThreadPoolExecutor(
				m_maxCrawlers,
				m_maxCrawlers, 
				Long.MAX_VALUE,
				TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(numOfCrawlers, true));

		addUrlListToRepository(this.m_seedUrls);
	}
	
	/**
	 * Add a list of URL strings to the URL Repository. The strings may
	 * not be validated at this stage. 
	 * @param urlList List of URL strings.
	 */
	private void addUrlListToRepository(String[] urlList) {
		for (String url : urlList) {
			addUrlToRepository(url);
		}
	}

	/**
	 * Add a single URL string to the repository if it is a valid URL. The URL
	 * string is convert to an URI object before being added to the repository.
	 * If the string is invalid, the program will print the invalid string and
	 * return but WILL NOT terminate subsequent executions.
	 * 
	 * Only HTML pages or pages with no extensions will be added to the
	 * repository. Details can be found in the class doc.
	 * 
	 * URL strings with host names that have been added into the repository
	 * will not be added.
	 * @param url The URL string to add into the repository.
	 */
	private void addUrlToRepository(String url) {
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			System.err.println("URISyntaxException when adding link: " + url);
			return;
		}

		if (!m_seenUrls.contains(uri.toString())) {
            m_seenUrls.add(uri.toString());
			m_urisRepository.add(uri);
		}
	}
	
	/**
	 * Starts the crawler and writes the results to a file.
	 * @return the results array. This consists of the visited hosts and their
	 * 		request times.
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public String[] startCrawl() throws IOException,
			URISyntaxException {
		executeCrawl();
		System.out.println("Found " + m_linkCounts + " links.");
		
		System.out.println(m_results.toString());
		
		System.out.println("Shutting down crawlers...");
		m_executorPool.shutdownNow();
		
        System.out.println("Writing results to file.");
        writeResultsToFile();
        System.out.println("Finished writing results to file.");
		
        while (!m_executorPool.isTerminated()) {
			// Wait till threads in the executor pool are stopped.
		}
        System.out.println("Stopped all crawlers.");
        
		return m_results.toArray(new String[0]);
	}

	/**
	 * Executes the crawling procedure. The crawler will terminate once the
	 * maximum number of pages to crawl has been reached or when there are no
	 * links left to visit.
	 * 
	 * A request delay is included as well.
	 */
	private void executeCrawl() {
		while ((m_linkCounts < m_maxPagesToCrawl) && !isDeadEnd()) {
			if (m_urisRepository.isEmpty() || m_executorPool.getActiveCount() >= m_maxCrawlers) {
				continue;
			}

			try {
				Thread.sleep(REQUEST_DELAY);
			} catch (InterruptedException e) {
				System.err.println("Crawling delay interrupted.");
			}
			
			URI uri = m_urisRepository.get(0);
			m_urisRepository.remove(0);
			
			if (uri != null) {
				m_executorPool.execute(new Crawler(uri, this));
			}
		}
	}
	
	/**
	 * Checks if there are any links left to be visited. If there are no
	 * links in the repository and there are no threads running, a dead end
	 * is reached.
	 * @return true if a dead end is reached. Return false otherwise.
	 */
	private boolean isDeadEnd() {
		return m_urisRepository.isEmpty() &&
				(m_executorPool.getActiveCount() == -1);
	}

	/**
	 * Writes the results array into the results file.
	 */
	private void writeResultsToFile() {
		try {
			File resultsFile = new File(RESULTS_FILENAME);
			if(!resultsFile.exists()) {
				resultsFile.createNewFile();
			} 
			
			PrintWriter writer = new PrintWriter(RESULTS_FILENAME);
			for (String r : m_results) {
				writer.println(r);
			}

			writer.close();
		} catch (FileNotFoundException e) {
			System.err.println("FileNotFoundException when writing results" +
					" to file.");
			return;
		} catch (IOException e) {
			System.err.println("IOException when writing results to file.");
			return;
		}
	}

	/**
	 * This is the callback function used by the crawler to update the master
	 * with the links obtained from its crawl job. Only one crawler thread can
	 * access this at any point in time to prevent an error from a race
	 * condition.
     * @param links the URL strings to add into the repository.
     * @param paperJson the paper node JSON string.
     * @param crawledURL the url that was visited.
     * @param crawled checks if the url was crawled successfully.
     */
	public synchronized void addCrawledDataCallback(String[] links,
                                                    String paperJson,
                                                    String crawledURL,
                                                    boolean crawled) {
		if (m_linkCounts >= m_maxPagesToCrawl) {
			return;
		}
		
		addUrlListToRepository(links);
        if (paperJson != "") {
            synchronized (m_jsonRepositoryLock) {
                m_jsonRepository.add(paperJson);
            }
        }

        if (crawled) {
            m_results.add(prettyFormatResultString(crawledURL));
            m_linkCounts += 1;
            System.out.println(m_linkCounts + " URLs crawled.");
        }
	}

    /**
     * Returns the length of the JSON repository. This locks on the
     * repository so that the length will be accurate.
     * @return the length of the JSON repository.
     */
    public int getPaperJsonArrayLength() {
        int length = 0;
        synchronized (m_jsonRepositoryLock) {
            length = m_jsonRepository.size();
        }

        return length;
    }

    /**
     * This returns an instance of the Paper JSON list and clears the list.
     * @return instance of the Paper JSON list.
     */
    public String[] getPaperJsonArray() {
        String[] res = null;
        synchronized (m_jsonRepositoryLock) {
            res = m_jsonRepository.toArray(new String[
                    m_jsonRepository.size()]);
            m_jsonRepository.clear();
        }

        return res;
    }

	/**
	 * Pretty formatter for the <url> string.
	 * @param crawledURL the url that was visited.
	 * @return the pretty formatted string.
	 */
	private String prettyFormatResultString(String crawledURL) {
		return crawledURL;
	}
}
