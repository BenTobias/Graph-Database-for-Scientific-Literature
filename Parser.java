import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by benedict on 22/3/14.
 */
public class Parser {

    /**
     * The list of links to crawl next.
     */
    private ArrayList<String> linksToCrawl = new ArrayList<>();

    /**
     * Public API for classes to parse the HTML string to a JSON string.
     * @param html the html string.
     * @param uri the URI of the HTML page to crawl.
     * @return the JSON string containing the data according to the
     *      predetermined schema.
     */
    public String parseHtml(String html, URI uri) {
        JSONObject paperJson = getPaperJson(html, uri);

        // TODO(benedict): Pass links to controller to crawl.

        return paperJson.toString();
    }

    /**
     * Gets the JSON object for the Paper node.
     * @param html the html string.
     * @param uri the URI of the HTML page to crawl.
     * @return the JSON object of the Paper node.
     */
    public JSONObject getPaperJson(String html, URI uri) {
        String uriHost = uri.getHost();
        String uriQuery = uri.getRawQuery();

        JSONObject json = new JSONObject();

        String doi = uriQuery.split("=")[1];

        Document doc = Jsoup.parse(html);

        String paperAbstract = doc.select("div#abstract p").html();
        ArrayList<String> downloadLinksList = getDownloadLinks(doc);
        ArrayList<Map<String, String>> citationsMapList = getCitationURLMaps(
                uriHost, doc);

        // Add parsed data to JSON.
        addMetadataToJson(json, doc);
        json.put("url", uri.toString());
        json.put("doi", doi);
        json.put("abstract", paperAbstract);
        json.put("downloadlinks", downloadLinksList);
        json.put("citations", citationsMapList);

        return json;
    }

    /**
     * Adds the metadata information to the result JSON.
     * @param json the result JSON.
     * @param doc the document object for the HTML page.
     */
    private void addMetadataToJson(JSONObject json, Document doc) {
        for (Element meta : doc.select("meta")) {
            switch (meta.attr("name")) {
                case "citation_title":
                    json.put("title", meta.attr("content"));
                    break;
                case "citation_authors":
                    // Remove duplicates
                    String[] authors = meta.attr("content").trim().split(", ");
                    Set<String> authorsSet = new HashSet<>(
                            Arrays.asList(authors));

                    json.put("authors", authorsSet);
                    break;
                case "citation_year":
                    json.put("year", meta.attr("content"));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Get paper download links.
     * @param doc the document object for the HTML page.
     * @return the list of download links.
     */
    private ArrayList<String> getDownloadLinks(Document doc) {
        Elements downloadLinks = doc.select("ul#dlinks li a");
        ArrayList<String> downloadLinksList = new ArrayList<>();

        for (Element l : downloadLinks) {
            downloadLinksList.add(l.attr("href"));
        }
        return downloadLinksList;
    }

    /**
     * Gets the list of citations.
     *
     * The list of URLS to crawl will be added to the list of URLs to crawl
     * as well. URLs with the class that indicates that the page only has the
     * citation will not be crawled.
     *
     * The return format will be as follows:
     * [{"url": <citation URL>, "title": <citation title>}]
     *
     * @param uriHost the host of the page to crawl.
     * @param doc the document object for the HTML page.
     * @return the list of citation title and url maps.
     */
    private ArrayList<Map<String, String>> getCitationURLMaps(
            String uriHost, Document doc) {
        Elements citations = doc.select("div#citations tr a");

        ArrayList<Map<String, String>> citationsMapList = new ArrayList<>();

        for (Element c : citations) {
            String citationLink = uriHost + c.attr("href");
            String citationTitle = c.html();

            Map<String, String> citationMap = new HashMap<>();
            citationMap.put("url", citationLink);
            citationMap.put("title", citationTitle);
            citationsMapList.add(citationMap);

            if (!c.hasClass("citation_only")) {
                // Add to links to crawl array.
                linksToCrawl.add(citationLink);
            }
        }
        return citationsMapList;
    }

    public static void main(String[] args) {
        Parser p = new Parser();
        String url = "http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.248.5252";
        URI uri;
        try {
            uri = new URI(url);
            System.out.println(uri);
            Crawler c = new Crawler(uri, null);
            String html = c.getHTML(uri.getHost(), uri.getRawPath() + "?" + uri.getQuery(), 80);
            p.getPaperJson(html, uri);
        } catch (URISyntaxException e) {
            System.err.println("URISyntaxException when adding link: " + url);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
