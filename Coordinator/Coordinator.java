import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
//refactor later.

/*
 * TODO: implement SSL support.
 * http://stilius.net/java/java_ssl.php
 * 
 * TODO: periodically prune the database. <- in another subfile.
 * TODO: test changes to protocol (basically i put in the fact that it says cmd, ok)
 */


/*
 * This unit listens for JSON commands via SSL commands.
 * This unit will have 2 functionalities:
 * 	Firstly, you can ask it for work. Which it will give, quite happily.
 * 		-It will only give u things which are neither "recently allocated" nor "already crawled"
 *  Secondly, you can tell it things you have discovered, mainly new targets
 *  	-It will add it to the list of things to crawl, if not already crawled.
 *  
 *  This unit will ensure that every URL is crawled only once.
 *  	-Subsequent modifications will allow crawled URLs to expire, allowing recrawl.
 */

/*
 * Credits: Structure of program taken from 
 * http://www.javaworld.com/article/2073344/core-java/use-select-for-high-speed-networking.html
 * Note: "significant code reuse"
 */

/*
 * PROTOCOL:
 * Query format: {cmd: "PUT_URLS"|"GET_URLS", URLS: ["string",...], limit: (int, >0)}
 * URLS is not used for GET_URLS.
 * limit is optional for GET_URLS. Not used for PUT_URLS.
 */
public class Coordinator {
	
	private static class ChannelState
	{
		public StringBuilder input;
		public ByteBuffer output;
		public String mode;
		
		ChannelState(int sz)
		{
			input = new StringBuilder();
			output = ByteBuffer.allocate(sz);
			mode = "";
		}
		ChannelState(int sz, String initmode)
		{
			input = new StringBuilder();
			output = ByteBuffer.allocate(sz);
			mode = initmode;
		}
		
	}
	
	//i think i'm using too many globals =(
	private static final ByteBuffer buffer = ByteBuffer.allocate( 60000 );
	static private Charset cs = Charset.forName("UTF-8");
    static CharsetDecoder decoder = cs.newDecoder();
    static CharsetEncoder encoder = cs.newEncoder();
    private static DBHelper db = new DBHelper();
    private static ArrayList<String> JSONStrings = new ArrayList<String>();
    private static DBManager DBMgr = new DBManager();
    private static class callback implements DBManager.InsertDocumentsCallback{

		@Override
		public void onFinish(List<String> failedUrls) {
			System.out.println("Could not process: ");
			for(String s: failedUrls)
				System.out.print(s+",");
			System.out.println("");
			
		}
    
    }
    
    //configuration
    static int port = 15001;
    private static int crawltimeout = 60 * 60 * 3; // 3 hours
    private static int defaultlimit = 100;
	private static int OutBufferSize = 32768;
	/*
	 * This looks scary, but this basically grabs any connection runs the function process
	 */
	public static void main(String[] args) {
		
		try{
			Selector selector = Selector.open();
		
			//init connections
			ServerSocketChannel ssc = ServerSocketChannel.open();
			ssc.bind(new InetSocketAddress(port));
			ssc.configureBlocking(false);
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("Listening on port "+port);
			while(true)
			{
				int num = selector.select();
				if (num == 0) { //no activity
					continue;
				}
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> it = keys.iterator();
				while (it.hasNext()) {
					// Get a key representing one of bits of I/O activity.
					SelectionKey key = (SelectionKey)it.next();
					// ... deal with SelectionKey ...
					if ((key.readyOps() & SelectionKey.OP_ACCEPT) ==
				            SelectionKey.OP_ACCEPT) {
						System.out.println("accept");
						ServerSocketChannel sc = (ServerSocketChannel)key.channel();
						SocketChannel newsocket = sc.accept();
						newsocket.configureBlocking(false);
						SelectionKey newkey = newsocket.register(selector, SelectionKey.OP_READ);
						//i need to attach a input and output buffer to this.
						newkey.attach(new ChannelState(OutBufferSize, "READ"));
					}else if ((key.readyOps() & SelectionKey.OP_READ) ==
				            SelectionKey.OP_READ) {
						SocketChannel sc = null;
						try{
							sc = (SocketChannel)key.channel();
							//process input here.
							boolean ok = process(sc, (ChannelState)key.attachment()); //this always rips all data from the buffer.
						    if(!ok)
						    {
						    	key.cancel();
						    	Socket s = null;
						    	try{
						    		s = sc.socket();
						    		s.close();
						    		
						    	}catch(IOException ie)
						    	{
						    		System.err.println("Error closing socket "+s+": "+ie);
						    	}
						    	System.out.println("Connection closed");
						    }
						}catch(IOException e) //TODO: tidy exception handling.
						{
							Socket s = null;
							try{
								
					    		s = sc.socket();
					    		s.close();
					    		
					    	}catch(IOException ie)
					    	{
					    		System.err.println("Error closing socket "+s+": "+ie);
					    	}
							
							e.printStackTrace();
						}
					}
				}
				keys.clear(); //current keys have been dealt with.
			}
		}
		catch(Exception e)
		{e.printStackTrace();}
	}
	
	/*
	 * See top for PROTOCOL
	 * 
	 */
	static boolean process(SocketChannel sc, ChannelState cs) throws Exception
	{
		//TODO: check correctness of write commands in non-blocking mode.
		buffer.clear();
	    int t = sc.read(buffer);
	    buffer.flip();
	    
	    // If no data, close the connection
	    if (buffer.limit()==0) {
	    	return false;
	    }
	    //do processing here.
	    CharBuffer cbuf;
	    DBObject msg;
	    
	    try{
	    cbuf = decoder.decode(buffer);
	    cs.input.append(cbuf.toString());
	    //System.out.println(s);
	    
	   
	    	msg = (DBObject)JSON.parse(cs.input.toString());
	    }catch(Exception e)
	    {
	    	System.out.println(e.getMessage());
	    	return true; //there is more (to be done), clearly.
	    }
	    //Check message type, there are 2 types of messages.
	    //System.out.println(msg);
	    if(msg.get("cmd").toString().equals("PUT_URLS") )
	    {
	    	BasicDBList urls = ((BasicDBList )msg.get("URLS"));
	    	DBCollection papers_db = db.GetCollection("PaperNodes");
	    	for(int i = 0;i<urls.size();i++)
	    	{
	    		//TODO: RF URLs
	    		//add if not already inside.
	    		DBCursor papers = db.GetPaperByUrl((String)urls.get(i));
	    		if(papers.count() == 1)
	    			continue;
	    		if(papers.count() == 0)
	    		{
	    			//create and insert.
	    			DBObject obj = new BasicDBObject();
	    			obj.put("URL", (String)urls.get(i));
	    			obj.put("Crawl_status", "Uncrawled");
	    			obj.put("Crawl_date", new Date(0)); //this is basically, much long time ago. So we can do comparisons
	    			//actually save into database.
	    			papers_db.insert(obj);
	    		}
	    		else
	    			assert(false); //impossible to have more than 1 identical URL.
	    	}
	    	
	    	cs.output = encoder.encode(CharBuffer.wrap("{cmd: \"OK\""));
	    	
	    }else if(msg.get("cmd").toString().equals("GET_URLS")){
	    	//find results, update the date to now and status to pending, send results.
	    	DBCollection papers_db = db.GetCollection("PaperNodes");
	    	
	    	int limit;
	    	try{
	    	limit = Integer.parseInt((String) msg.get("limit"));
	    	} catch(NumberFormatException e)
	    	{
	    		limit = defaultlimit;
	    	}
	    	DBObject query = (DBObject)JSON.parse("{ $or: [{Crawl_status: \"Uncrawled\"}, {Crawl_status: \"Pending\"}] }");
	    	//anything created before now-crawltimeout is acceptable.
	    	query.put("Crawl_date", new BasicDBObject("$lt", new Date((new Date()).getTime() - crawltimeout*1000)));
	    	List<DBObject> papers = papers_db.find(query).limit(limit).toArray();
	    	//update the database
	    	BasicDBList urls = new BasicDBList();
	    	for(DBObject paper: papers)
	    	{
	    		paper.put("Crawl_status", "Pending");
	    		paper.put("Crawl_date", new Date()); //now
	    		urls.add((String)paper.get("URL"));
	    		papers_db.save(paper);
	    	}
	    	DBObject result = new BasicDBObject("URLS", urls);
	    	result.put("cmd", "OK");
	    	String os = JSON.serialize(result);
	    	cs.output = encoder.encode(CharBuffer.wrap(os));
	    }else if(msg.get("cmd").toString().equals("PUT_JSONS"))
	    {
	    	System.out.println(msg);
	    	BasicDBList data = ((BasicDBList )msg.get("data"));
	    	for(int i = 0;i<data.size();i++)
	    	{
	    		JSONStrings.add((String)data.get(i));
	    	}
	    	
	    	DBMgr.insertDocuments((ArrayList<String>)JSONStrings.clone(), new callback());
	    	JSONStrings.clear();
	    	cs.output = encoder.encode(CharBuffer.wrap("{cmd: \"OK\""));
	    }
	    else
	    {
	    	System.out.println("Invalid command: '"+msg.get("cmd")+"'");
	    	return false;
	    }
	    //TODO: make non-blocking. But i'm lazeee
	    while(cs.output.hasRemaining())
    		sc.write(cs.output);
	    return false; //i don't need the connection anymore
	}
	
	/*static void printByteBuffer(ByteBuffer buffer)
	{
		 while (buffer.hasRemaining()) {
		    	System.out.print((char) buffer.get());
		    }
	}*/
}