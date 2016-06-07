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

public class SomeServer {

	protected SocketIOServer server;
	protected SomePlayer moviePlayer;
	
	public SomeServer(SomePlayer mp, int port) {
		
		this.moviePlayer = mp;

		Configuration config = new Configuration();
		config.setPort(port);

		server = new SocketIOServer(config);
		
		server.addEventListener("uievent", UIEvent.class, new DataListener<UIEvent>() {
			
			public void onData(SocketIOClient client, UIEvent evt, AckRequest ack)
					throws Exception  
			{
				
				System.out.println("  SomeServer.event "+evt);
				
				if (evt.getCommand().equals("generate")) {

					String[] parts = ((String)evt.getData()).split(",");
					String keyword = parts[0], color = parts[1];
					moviePlayer.startMovie(keyword, color);
					sendSystemState();
				}
				
				else if (evt.getCommand().equals("toggle-pause")) {
					
					moviePlayer.togglePause();
					sendSystemState();
				}
			}
		});

		// TODO: should send list of keywords and colors
		server.addConnectListener(new ConnectListener() {
			
			public void onConnect(SocketIOClient client) {
				sendConfigInfo();
				sendSystemState();
			}
		});

		server.addDisconnectListener(new DisconnectListener() {
			public void onDisconnect(SocketIOClient client) {
				//System.out.println("SocketIOTest.onDisconnect()");
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

	public void sendSystemState() {
		sendPauseState();
		sendMovieTime();
	}
	
	public void sendSearchResult(SearchResult ir) {
		
		//System.out.println("sendSearchResult: "+ir.images.size()+" images");
		for (Iterator it = ir.images.iterator(); it.hasNext();) {
			ImageLoc pair = (ImageLoc) it.next();
			//System.out.println("'"+pair.thumbUrl+"' "+pair.thumbUrl.length());
			if (pair.thumbUrl.length()<1) {
				it.remove();
			}
		}
    server.getBroadcastOperations().sendEvent("srevent", ir);
    sendMovieTime();
	}
	
	public void sendMovieTime() {
		UIEvent uiEvent = new UIEvent("time", new float[] { 
				moviePlayer.playhead(), moviePlayer.movieDuration()	
		});
		server.getBroadcastOperations().sendEvent("uievent", uiEvent);
	}
	
	public void sendPauseState() {
		UIEvent uiEvent = new UIEvent("pause", new Boolean(moviePlayer.playbackPaused));
		server.getBroadcastOperations().sendEvent("uievent", uiEvent);
	}
	
	public void sendConfigInfo() {
		UIEvent uie1 = new UIEvent("config", formatParams());
    server.getBroadcastOperations().sendEvent("uievent", uie1);
		UIEvent uie2 = new UIEvent("params", moviePlayer.keyword+","+moviePlayer.colorName);
    server.getBroadcastOperations().sendEvent("uievent", uie2);
	}
	
	private String formatParams() {
		
		String s = "";
		for (int i = 0; i < SomeSuper.keywords.length; i++) {
			s += SomeSuper.keywords[i];
			s += (i < SomeSuper.keywords.length-1) ? ":" : ",";
		}
		
		Set<String> colors = SomeSuper.colorTable.keySet();
		for (Iterator it = colors.iterator(); it.hasNext();) {
			s +=  (String) it.next();
			if (it.hasNext()) s += ":";
		}
		
		return s;
	}
	
	public void stop() {
		
		this.server.stop();
	}
	
	public static void main(String[] args) {
		final SomeServer server = new SomeServer(null, SomeSuper.serverPort);
		new Thread() {
			public void run() {
				while (true) {
					try {
						int fidx = (int) (Math.random() * 1000);
            //server.sendPhrase(fidx+"");
						sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
		}.start();	
	}
}