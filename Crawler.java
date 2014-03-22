import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


/**
 * The Crawler class represents one worker in the ThreadPoolExecutor (mentioned
 * in Master.java). This class handles the GET request of one URI (link).
 * Exceptions will not cause the other executing threads to terminate.
 * 
 * The Crawler aims to retrieve all outgoing links from the given URI and its
 * response time.
 * 
 * The remaining descriptions and optimizations are listed in Master.java.
 * @author benedict
 *
 */
public class Crawler implements Runnable {
	
	private final String GET_REQUEST_STRING = "GET %s HTTP/1.1\r\nHost: %s" +
			"\r\nConnection: close\r\n\r\n";
	private URI m_uri;
	private Master m_master;
    private String m_paperJson;


	/**
	 * The Crawler constructor.
	 * @param uri the URI to crawl.
	 * @param master the Master handle to respond to.
	 */
	public Crawler(URI uri, Master master) {
		this.m_uri = uri;
		this.m_master = master;
	}

	/**
	 * Gets the outgoing links present in the request URI. Only absolute links
	 * present in the response HTML page will be returned.
	 * @param host the URI host.
	 * @param requestPath the URI request path relative to the host.
	 * @param port the port number to run the request on.
	 * @return the array of absolute URLs obtained from the response page.
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private String[] getSiteLinks(String host, String requestPath, int port) 
			throws IOException {
		
		if (port == -1) {
			throw new PortUnreachableException("Invalid port.");
		}
		
		String html = "";
		if (port == 80) {
			Socket socket = new Socket(host, port);
			sendGETRequest(host, requestPath, socket);
			html = readDocumentStringFromSocketBuffer(socket);
			socket.close();
		}
		else {
			SSLSocketFactory sslsocketfactory =
                    (SSLSocketFactory) SSLSocketFactory.getDefault();
	        SSLSocket socket = (SSLSocket) sslsocketfactory.createSocket(host, port);
	        sendGETRequest(host, requestPath, socket);
			html = readDocumentStringFromSocketBuffer(socket);
			socket.close();
		}

        // The HTML string is a url.
        if (html.indexOf("http") == 0) {
            try {
                m_uri = new URI(html);
                return getSiteLinks(m_uri.getHost(), m_uri.getPath() + "?" +
                        m_uri.getRawQuery(), getPort(m_uri));
            } catch (URISyntaxException e) {
                System.err.println("URISyntaxException when adding link: "
                        + html);
                return new String[0];
            }
        }

        Parser parser = new Parser();
        m_paperJson = parser.parseHtml(html, m_uri);

		return parser.getLinksToCrawl();
	}

    /**
     * Gets the outgoing links present in the request URI. Only absolute links
     * present in the response HTML page will be returned.
     * @param host the URI host.
     * @param requestPath the URI request path relative to the host.
     * @param port the port number to run the request on.
     * @return the array of absolute URLs obtained from the response page.
     * @throws UnknownHostException
     * @throws IOException
     */
    public String getHTML(String host, String requestPath, int port)
            throws IOException {

        if (port == -1) {
            throw new PortUnreachableException("Invalid port.");
        }

        String html;
        if (port == 80) {
            Socket socket = new Socket(host, port);
            sendGETRequest(host, requestPath, socket);
            html = readDocumentStringFromSocketBuffer(socket);
            socket.close();
        }
        else {
            SSLSocketFactory sslsocketfactory =
                    (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) sslsocketfactory.createSocket(host, port);
            sendGETRequest(host, requestPath, socket);
            html = readDocumentStringFromSocketBuffer(socket);
            socket.close();
        }

        return html;
    }

	/**
	 * Strip out all the links from the given HTML page.
	 * @param html the HTML document string.
	 * @return the array of absolute URLs obtained from the response page. 
	 * @throws IOException
	 */
	private ArrayList<String> getLinksFromHTMLPage(String html) 
			throws IOException {
		Document doc = Jsoup.parse(html);
		Elements links = doc.select("a[href]");
		
		return getLinksFromAnchorTags(links);
	}

	/**
	 * Get the absolute links from a given list of anchor tags.
	 * @param links the array of anchor tags.
	 * @return the filtered absolute links.
	 */
	private ArrayList<String> getLinksFromAnchorTags(Elements links) {
		ArrayList<String> absLinks = new ArrayList<String>();
		
		for (Element l : links) {
			String link = l.absUrl("href");
			if (link != "") {
				absLinks.add(link);
			}
		}
		return absLinks;
	}

	/**
	 * Obtain the HTML document string from the socket buffer.
	 * @param socket the socket connecting to the request URI.
	 * @return the HTML document string. If a redirect url exists, return the
     *      redirect url instead.
	 * @throws IOException
	 */
	private String readDocumentStringFromSocketBuffer(Socket socket)
			throws IOException {
		InputStream inputStream = socket.getInputStream();
		BufferedReader serverReader = new BufferedReader(
		        new InputStreamReader(inputStream));
		
		StringBuffer sb = new StringBuffer();

		sb.append(serverReader.readLine());

		String line;
        int lineCount = 0;
        String redirectUrl = null;

		while ((line = serverReader.readLine()) != null) {
            if (lineCount < 6) {
                if (line.indexOf("Location: http") != -1) {
                    int index = line.indexOf("http://");
                    redirectUrl = line.substring(index);
                    break;
                }
                lineCount += 1;
            }
		    sb.append(line);
		}

        if (redirectUrl != null) {
            return redirectUrl;
        }

		return sb.toString();
	}

	/**
	 * Sends the GET request to the request host via the socket object. 
	 * @param host the URI host.
	 * @param requestPath the URI request path relative to the host.
	 * @param socket the socket connecting to the request URI.
	 * @throws IOException
	 */
	private void sendGETRequest(String host, String requestPath,
			Socket socket) throws IOException {
		DataOutputStream request = new DataOutputStream(
				socket.getOutputStream());
		String formattedGetRequestString = String.format(GET_REQUEST_STRING,
				requestPath, host);

		request.writeBytes(formattedGetRequestString);
	}

	/**
	 * Gets the port of the URI based on the URI's protocol.
	 * @param uri the URI to crawl.
	 * @return 80 or 443 if the protocol is HTTP or HTTPS respectively.
	 * 		Return -1 for an invalid protocol.
	 */
	private int getPort(URI uri) {
		int port = uri.getPort( );
		String protocol = uri.getScheme( ); 
		if (port == -1) {
		    if (protocol.equals("http")) { 
		        return 80;
		    }
		    else if (protocol.equals("https")) {
		        return 443;
		    }
		}
		
		return -1;
	}
	
	/**
	 * The entry point for the thread. The thread will get the outgoing links
	 * from the input URI and update the Master. If any exception is
	 * encountered, the thread will not update the Master.
	 */
	@Override
	public void run() {
		if (m_uri == null) {
			return;
		}
		String[] links = null;

		try {
            String getRequestPath = m_uri.getPath() + "?" + m_uri.getQuery();
			links = getSiteLinks(m_uri.getHost(), getRequestPath,
                    getPort(m_uri));
		} catch (UnknownHostException e) {
			System.err.println("UnknownHostException during GET request: " + 
					m_uri.toString());
			return;
		} catch (PortUnreachableException e) {
			System.err.println("PortUnreachableException during GET request: " +
					m_uri.toString());
			return;
		} catch (IOException e) {
			System.err.println("IOException during GET request: " +
					m_uri.toString());
			return;
		}
		
		if (links != null && (m_paperJson != null)) {
			m_master.addCrawledDataCallback(links, m_paperJson,
                    m_uri.toString());
		}
		
		return;
	}	
}
