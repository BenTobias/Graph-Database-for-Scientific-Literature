import java.net.UnknownHostException;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

//TODO: modify such that it can continue execution later.
public class GraphMetrics {
	private static double donationratio = 0.15;  
	private static double epsilon = 0.00000001;
	Mongo mongo;
	DB db;
	DBCollection papersdb;
	DBCollection authorsdb;
	GraphMetrics(){
		try {
			mongo = new MongoClient("localhost", 27017);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db = mongo.getDB("cs3103project");
		papersdb = db.getCollection("paper");
		authorsdb = db.getCollection("author");
	}
	
	void IteratePageRank() //this is a 3 phase algorithm which can be done 1 step at a time.
	{
		PageRank_markParticipating();
		PageRank_computeDeltas();
		PageRank_applyDeltas();
		double sum = 0;
		BasicDBObject q = new BasicDBObject("PageRank", new BasicDBObject("$exists", true));
		DBCursor papers = papersdb.find(q);
		for(DBObject paper: papers)
		{
			sum += (double)paper.get("PageRank");
		}
		System.out.println(sum);
		
	}
	
	void PageRank_markParticipating(){
		//select all nodes.
		//set delta field to zero
		BasicDBObject q = new BasicDBObject();
		BasicDBObject o = new BasicDBObject();
		o.append("$set", new BasicDBObject().append("PageRank_Delta", 0.0));
		o.append("$inc", new BasicDBObject().append("PageRank", 0.0)); //if there isn't a PageRank field, create it.
		papersdb.update(q, o, false, true);
	}
	
	//assume no isolated node.
		//Or rather, if it is an isolated node, its pagerank will hit 0.
	void PageRank_computeDeltas(){
		//select all nodes with a PageRank field.
		BasicDBObject q = new BasicDBObject("PageRank", new BasicDBObject("$exists", true));
		DBCursor papers = papersdb.find(q);
		for(DBObject paper : papers)
		{
			if((double)paper.get("PageRank") < epsilon) //bo lui.
				continue;
			//determine to who it should donate.
			BasicDBList incoming = (BasicDBList) paper.get("cited_by"); //incoming cited by
			BasicDBList outgoing = (BasicDBList) paper.get("citations");//outgoing citations
			if(incoming == null)
				incoming = new BasicDBList();
			if(outgoing == null)
				outgoing = new BasicDBList();
			incoming.addAll(outgoing); //assume symmetry.
			if(incoming.size() == 0)
			{
				continue;
			}
			BasicDBObject neighbourquery = new BasicDBObject("_id", new BasicDBObject("$in", incoming));
			//neighbourquery.put("PageRank", new BasicDBObject("$exists", true)); //also must include only participating nodes.
			DBCursor neighbours = papersdb.find(neighbourquery);
			if(neighbours.count() == 0)
				continue;
			//how much to donate?
			double donationamt = donationratio * (double)paper.get("PageRank") / (double)(int)neighbours.count();
			if(donationamt == 0)
				continue;
			//grab the id of all neighbours
			BasicDBList idlist = new BasicDBList();
			for(DBObject neighbour: neighbours)
				idlist.add(neighbour.get("_id"));
			//increment all by donationratio
			BasicDBObject incrementquery = new BasicDBObject("_id", new BasicDBObject("$in", idlist));
			BasicDBObject incrementaction = new BasicDBObject();
			incrementaction.append("$inc", new BasicDBObject().append("PageRank_Delta", donationamt));
			papersdb.update(incrementquery, incrementaction, false, true);
		}
	}
	
	/*void PageRank_computeDeltas()
	{
		
		
	}*/
	
	void PageRank_applyDeltas(){
		//select all nodes with a PageRank field.
		BasicDBObject q = new BasicDBObject("PageRank", new BasicDBObject("$exists", true));
		DBCursor papers = papersdb.find(q);
		for(DBObject paper : papers)
		{
			Double delta = (Double)paper.get("PageRank_Delta");
			paper.put("PageRank", (double)paper.get("PageRank") * (1-donationratio) + delta);
			/*if((double)paper.get("PageRank") != 0.0)
				System.out.println((double)paper.get("PageRank") );*/
			papersdb.save(paper);
		}
	}
}
