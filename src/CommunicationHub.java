import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.TimerTask;


// TODO: Auto-generated Javadoc
/**
 * The Class CommunicationHub.
 */
public abstract class CommunicationHub {
	
	/** The waiting for heartbeat. */
	private boolean waitingForHeartbeat;
	
	/** The socket. */
	private Socket socket;
    
    /** The Constant STILL_CONNECTED_REQUEST. */
    public static final String STILL_CONNECTED_REQUEST = "AreYouStillThere";
    
    /** The Constant STILL_CONNECTED_RESPONSE. */
    public static final String STILL_CONNECTED_RESPONSE = "YesImHere";
	
	/**
	 * Sets the login info.
	 *
	 * @param credentials the new login info
	 */
	public abstract void setLoginInfo (String credentials);
	
	/**
	 * Sets the iP.
	 *
	 * @param ipAddress the new iP
	 */
	public abstract void setIP(String ipAddress);
	
	/**
	 * Send message.
	 *
	 * @param message the message
	 */
	public abstract void sendMessage(String message);
	
	/**
	 * Read message.
	 *
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public abstract String readMessage() throws IOException;
	
	/**
	 * Got disconnected.
	 */
	public abstract void gotDisconnected();
	
	/**
	 * Close connections.
	 */
	public abstract void closeConnections();
	
	/**
	 * Received heart beat.
	 */
	public abstract void receivedHeartBeat();
	
	/**
	 * Start listening.
	 */
	public abstract void startListening();
	
	/**
	 * Alert user.
	 *
	 * @param message the message
	 */
	public abstract void alertUser(String message);
	
	/**
	 * The Class HeartbeatTask.
	 */
	private class HeartbeatTask extends TimerTask {
        
        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        public void run() {
            if (waitingForHeartbeat) {
                try {
                    socket.close();
                } catch (IOException e) {}
                gotDisconnected();
            } else {
                waitingForHeartbeat = true;
                sendMessage(STILL_CONNECTED_REQUEST);
            }
        }
    }
	
	/**
	 * The Class InputManagementThread.
	 */
	private class InputManagementThread extends Thread {
		
	}
	
}
