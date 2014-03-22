import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by benedict on 22/3/14.
 */
public class Parser {

    private ArrayList<String> linksToCrawl = new ArrayList<String>();

    public JSONObject getPaperJson(String html, String uriHost, String uriQuery) {
        JSONObject json = new JSONObject();

        String doi = uriQuery.split("=")[1];

        Document doc = Jsoup.parse(html);
        String paperAbstract = doc.select("div#abstract p").html();
        Elements citations = doc.select("div#citations tr a");

        ArrayList<Map<String, String>> citationsMapList = new ArrayList<Map<String, String>>();

        for (Element c : citations) {
            String citationLink = uriHost + c.attr("href");
            String citationTitle = c.html();

            Map<String, String> citationMap = new HashMap<String, String>();
            citationMap.put("url", citationLink);
            citationMap.put("title", citationTitle);
            citationsMapList.add(citationMap);

            System.out.println(citationTitle);
            if (!c.hasClass("citation_only")) {
                // Add to links to crawl array.
                linksToCrawl.add(citationLink);
            }
        }

        json.put("doi", doi);
        json.put("abstract", paperAbstract);
        json.put("citations", citationsMapList);

        System.out.println(json);
        return json;
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
            p.getPaperJson(html, uri.getHost(), uri.getRawQuery());
        } catch (URISyntaxException e) {
            System.err.println("URISyntaxException when adding link: " + url);
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
