import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

public class DBManager {
	
	private MongoClient mongo;
	private DB database;
	DBCollection authorCollection, paperCollection, institutionCollection;
	
	public DBManager() {
		connectToDB();
	}
	
	private void connectToDB() {
		try {
			mongo = new MongoClient("localhost", 27017);
			database = mongo.getDB("cs3103project");
			authorCollection = database.getCollection("author");
			paperCollection = database.getCollection("paper");
			institutionCollection = database.getCollection("institution");
			System.out.println("CONNECTED TO DB");
		} catch (UnknownHostException e) {
			System.out.println("UNKNOWN HOST" + e.getLocalizedMessage());
		} catch (MongoException e) {
			System.out.println("DB CONNECTION ERROR" + e.getLocalizedMessage());
		}
	}
	
	public void insertDocuments(List<String> paperJSONStrings, InsertDocumentsCallback callback) {
		List<String> failedURLs = new ArrayList<String>();
		for(int i = 0; i < paperJSONStrings.size(); i++) {
			try {
				JSONObject doc = new JSONObject(paperJSONStrings.get(i));
				ObjectId id = insert(doc);
				if(id == null) failedURLs.add(doc.getString("url"));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		callback.onFinish(failedURLs);
	}

	private ObjectId findAuthorIdByName(String authorName) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", authorName);
		DBObject author = authorCollection.findOne(searchQuery);
		if (author != null) {
			return (ObjectId)author.get("_id");
		}
		return null;
	}
	
	private ObjectId findPaperIdByField(String field, String value) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put(field, value);
		DBObject paper = paperCollection.findOne(searchQuery);
		if (paper != null) {
			return (ObjectId)paper.get("_id");
		}
		return null;
	}
	
	private ObjectId insertTempAuthor(String authorName) {
		BasicDBObject authorDoc = new BasicDBObject();
		authorDoc.put("name", authorName);
		authorDoc.put("createdDate", new Date());
		authorCollection.insert(authorDoc);
		
		return (ObjectId)authorDoc.get( "_id" );
	}
	
	private ObjectId insertTempPaper(String title) {
		BasicDBObject paperDoc = new BasicDBObject();
		paperDoc.put("title", title);
		paperCollection.insert(paperDoc);
		
		return (ObjectId)paperDoc.get( "_id" );
	}
		
	private ObjectId insert(JSONObject data) {
		
		try {
			// Get author IDs.
			List<ObjectId> authorIds = new ArrayList<ObjectId>();
			JSONArray authors = data.getJSONArray("authors");
			for(int i = 0; i < authors.length(); i++) {
				String authorName = authors.getString(i);
				ObjectId id = findAuthorIdByName(authorName);
				if(id == null) {
					id = insertTempAuthor(authorName);
				}
				authorIds.add(id);
			}
			
			// System.out.println("AuthorIds: " + authorIds.toString());
			
			// Get citation paper IDs.
			List<ObjectId> citationIds = new ArrayList<ObjectId>();
			JSONArray citations = data.getJSONArray("citations"); 
			for(int i = 0; i < citations.length(); i++) {
				String title = citations.getJSONObject(i).getString("title");
				ObjectId id = findPaperIdByField("title", title);
				if(id == null) {
					id = insertTempPaper(title);
				}
				citationIds.add(id);
			}
			
			// System.out.println("CitationIds: " + authorIds.toString());
			
			ObjectId paperId = findPaperIdByField("doi", data.getString("doi"));
			if(paperId == null) {
				// If paper doesn't exit, create a new paper node
				BasicDBObject paperDoc = new BasicDBObject();
				paperDoc.put("title", data.getString("title"));
				paperDoc.put("doi", data.getString("doi"));
				paperDoc.put("url", data.getString("url"));		
				JSONArray array = data.getJSONArray("downloadlinks");
				if(array == null) array = new JSONArray();
				List<String> downloadLinks = new ArrayList<String>();
				for(int i = 0; i < array.length(); i++) 
					downloadLinks.add(array.getString(i));
				paperDoc.put("downloadlinks", downloadLinks);
				paperDoc.put("last_crawled", new Date());
				paperDoc.put("abstract", data.getString("abstract"));
				paperDoc.put("year", data.getInt("year"));
				paperDoc.put("authors", authorIds);
				paperDoc.put("citations", citationIds);
				paperCollection.insert(paperDoc);
				
				paperId = (ObjectId)paperDoc.get( "_id" );
			} else {
				// If paper already exists, update last_crawled field
				BasicDBObject newPaper = new BasicDBObject();
				newPaper.append("$set", new BasicDBObject().append("last_crawled", new Date()));
				BasicDBObject searchQuery = new BasicDBObject().append("_id", paperId);
				paperCollection.update(searchQuery, newPaper);
			}
			
			// Update authors' papers field and citations' cited_by field
			for(int i = 0; i < authorIds.size(); i++) {
				addPaperToAuthor(paperId, authorIds.get(i));
			}
			
			for(int i = 0; i < citationIds.size(); i++) {
				addPaperToPaper(paperId, citationIds.get(i));
			}
			
			return paperId;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param id1 ObjectId of paper1 
	 * @param id2 ObjectId of paper2 (cited by paper1)
	 * Add paper1 to the cited_by field of paper2
	 */
	private void addPaperToPaper(ObjectId id1, ObjectId id2) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", id2);
		DBObject paper = paperCollection.findOne(searchQuery);
		BasicDBList citedBys = (BasicDBList) paper.get("cited_by");
		if(citedBys == null) citedBys = new BasicDBList();
		if(citedBys.contains(id1)) return;
		
		citedBys.add(id1);
		BasicDBObject newPaper = new BasicDBObject();
		newPaper.append("$set", new BasicDBObject().append("cited_by", citedBys));
		searchQuery = new BasicDBObject().append("_id", id2);
		paperCollection.update(searchQuery, newPaper);
	}

	private void addPaperToAuthor(ObjectId paperId, ObjectId authorId) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", authorId);
		DBObject author = authorCollection.findOne(searchQuery);
		BasicDBList papers = (BasicDBList) author.get("papers");
		if(papers == null) papers = new BasicDBList();
		if(papers.contains(paperId)) return;
		papers.add(paperId);
		
		BasicDBObject newAuthor = new BasicDBObject();
		newAuthor.append("$set", new BasicDBObject().append("papers", papers));
		searchQuery = new BasicDBObject().append("_id", authorId);
		authorCollection.update(searchQuery, newAuthor);
	}
	
	public static interface InsertDocumentsCallback {
		public void onFinish(List<String> failedUrls);
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
            JSONObject json = p.getPaperJson(html, uri);
            System.out.println(json.toString());
            
            DBManager manager = new DBManager();
            List<String> jsonStrings = new ArrayList<String>();
            jsonStrings.add(json.toString());
            manager.insertDocuments(jsonStrings, new InsertDocumentsCallback(){

				@Override
				public void onFinish(List<String> failedUrls) {
					System.out.println("Finished with failed Urls " + failedUrls.size());
				}
            });
            
        } catch (URISyntaxException e) {
            System.err.println("URISyntaxException when adding link: " + url);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}