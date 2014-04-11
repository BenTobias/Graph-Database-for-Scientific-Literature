import java.net.UnknownHostException;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.util.JSON;

import org.bson.types.ObjectId;
/*
 * Note that database names are hardcoded into the query functions.
 *
 */

public class DBHelper {
	Mongo mongo;
	DB db;
	DBHelper()
	{
		try{
            mongo = new Mongo("localhost", 27017);
            db = mongo.getDB("PCrawl");
		}catch(Exception e)
		{
			System.out.println("IMPWOSSIBLEEEE");
			e.printStackTrace();
		}
	}
	
	/*
	 * Get <collection> by <params>
	 * 	returns you a cursor
	 * Implement as necessary.
	 */
	DBCursor GetPaperByUrl(String URL)
	{
		DBCollection collection = db.getCollection("PaperNodes");
		BasicDBObject q = new BasicDBObject("URL", URL);
		DBCursor matching = collection.find(q);
		return matching;
	}
	
	//this is for fast inserts
	DBCollection GetCollection(String collname)
	{
		return db.getCollection(collname);
	}
}