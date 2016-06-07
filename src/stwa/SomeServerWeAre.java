package stwa;

import java.util.Iterator;
import java.util.Set;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

public class SomeServerWeAre {

	protected SocketIOServer server;
	protected SomeThingWeAre player;
	
	public SomeServerWeAre(SomeThingWeAre stwa, int port) {
		
		this.player = stwa;

		Configuration config = new Configuration();
		config.setPort(port);

		server = new SocketIOServer(config);
		
		server.addEventListener("uievent", UIEvent.class, new DataListener<UIEvent>() {
			
			public void onData(SocketIOClient client, UIEvent evt, AckRequest ack)
					throws Exception  
			{
				//System.out.println("  SomeServer.event "+evt);
				
				if (evt.getCommand().equals("generate")) {

					String[] parts = ((String)evt.getData()).split(",");
					String keyword = parts[0], color = parts[1];
					player.startMovie(keyword, color, false);
					// no reply here
				}
			}
		});

		// TODO: should send list of keywords and colors
		server.addConnectListener(new ConnectListener() {
			
			public void onConnect(SocketIOClient client) {
				sendConnectEvent();
			}
		});

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("  Server shutting down...");
				server.stop();
			}
		});
		
		server.start();
	}

	public void sendConnectEvent() {

		UIEvent uie = new UIEvent("config", formatParams());
		uie.setPosition(0);
		uie.setDuration(player.totalTime());
		
		//System.out.println("SomeServerWeAre.sendConnectEvent: "+uie);
    server.getBroadcastOperations().sendEvent("uievent", uie);
	}
	
	// send search result with position
	public void sendSearchResult(SearchResult ir) {
		
		updateSearchResult(ir);
		//System.out.println("SomeServerWeAre.sendSearchResult: "+ir.position);
    server.getBroadcastOperations().sendEvent("srevent", ir);
	}
	
	// -----------------------------------------------------------------

	private SearchResult updateSearchResult(SearchResult ir) {
		for (Iterator it = ir.images.iterator(); it.hasNext();) {
			ImageLoc pair = (ImageLoc) it.next();
			if (pair.thumbUrl.length()<1) {
				it.remove();
			}
		}
		ir.setPosition(player.position()); // add movie-time
		return ir;
	}
	
//	public void sendMovieTime() {
//		UIEvent uiEvent = new UIEvent("time", player.position());
//		server.getBroadcastOperations().sendEvent("uievent", uiEvent);
//	}

	private String formatParams() {
		
		String s = "";
		String[] keywords = SomeThingWeAre.keywords;
		for (int i = 0; i < keywords.length; i++) {
			s += keywords[i] + ((i < keywords.length-1) ? ":" : ",");
		}
		
		Set<String> colors = SomeThingWeAre.colorTable.keySet();
		for (Iterator it = colors.iterator(); it.hasNext();) {
			s +=  (String) it.next();
			if (it.hasNext()) s += ":";
		}
		
		return s;
	}
	
	public void stop() {
		
		this.server.stop();
	}


}
