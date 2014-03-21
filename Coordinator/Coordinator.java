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
import java.util.Iterator;
import java.util.Set;




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
 */

/*
 * Credits: Structure of program taken from 
 * http://www.javaworld.com/article/2073344/core-java/use-select-for-high-speed-networking.html
 * Note: "significant code reuse"
 */
public class Coordinator {
	
	static int port = 15001;
	private static final ByteBuffer buffer = ByteBuffer.allocate( 60000 );
	static private Charset cs = Charset.forName("ASCII");
    static CharsetDecoder decoder = cs.newDecoder();
	
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
	
	static boolean process(SocketChannel sc) throws Exception
	{
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
	    
	    //printByteBuffer(buffer);
	    //Check message type, there are 2 types of messages.
	    	//PUT_URLS
	    		//TODO: RF URLs
	    		//Retrieve and add if not already inside.
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