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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;








import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
//refactor later.
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.util.JSON;

/*
 * TODO: implement SSL support.
 * http://stilius.net/java/java_ssl.php
 * 
 * TODO: periodically prune the database.
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
	
	static int port = 15001;
	private static final ByteBuffer buffer = ByteBuffer.allocate( 60000 );
	static private Charset cs = Charset.forName("ASCII");
    static CharsetDecoder decoder = cs.newDecoder();
    private static DBhelper db = new DBhelper();
	
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
						//TODO: check this code.
						newsocket.configureBlocking(false);
						newsocket.register(selector, SelectionKey.OP_READ);
					}else if ((key.readyOps() & SelectionKey.OP_READ) ==
				            SelectionKey.OP_READ) {
						System.out.println("read");
						SocketChannel sc = null;
						try{
							sc = (SocketChannel)key.channel();
							//process input here.
							boolean ok = process(sc); //this always rips all data from the buffer.
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
						}catch(IOException e)
						{
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
	static boolean process(SocketChannel sc) throws Exception
	{
		//TODO: check correctness of write commands in non-blocking mode.
		buffer.clear();
	    int t = sc.read( buffer );
	    buffer.flip();
	    
	    // If no data, close the connection
	    if (buffer.limit()==0) {
	    	return false;
	    }
	    //do processing here.
	    
	    CharBuffer cbuf = decoder.decode(buffer);
	    String s = cbuf.toString();
	    DBObject msg = (DBObject)JSON.parse(s);
	    System.out.println(msg);
	    
	  //Check message type, there are 2 types of messages.
	    if(msg.get("cmd").toString().equals("PUT_URLS") )
	    {
	    	BasicDBList  urls = ((BasicDBList )msg.get("URLS")); //i hope this works.
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
	    			//actually insert into database.
	    			papers_db.insert(obj);
	    		}
	    		else
	    			assert(false); //impossible to have more than 1 identical URL.
	    	}
	    }else if(msg.get("cmd").toString().equals("GET_URLS")){
	    	
	    }else
	    {
	    	System.out.println("Invalid command: '"+msg.get("cmd")+"'");
	    	return false;
	    }
	    
	    		
	    		
	    	//GET_URLS
	    		//Retrieve N URLs from database, 
	    			//give it to them and mark as "Tentative"
		return true;
	}
	
	/*static void printByteBuffer(ByteBuffer buffer)
	{
		 while (buffer.hasRemaining()) {
		    	System.out.print((char) buffer.get());
		    }
	}*/
}