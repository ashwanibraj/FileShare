package FileShare;
import java.io.*;
import java.net.*;

public class HandShake implements Serializable {
	
	private static final long serialVersionUID = -849530622076337158L;
	String HeaderMsg;
	int PeerID;
	
	public HandShake (int peerID) {
		PeerID = peerID;
	}
	
	public HandShake (String HeaderNote, int peerID) {
		HeaderMsg = HeaderNote;
		PeerID = peerID;
	}
	
	public void PrintMessage () {
		System.out.println("Header message: "+HeaderMsg);
		System.out.println("Peer ID: "+PeerID);
	}
	
	public void SendHandShake (OutputStream out) throws IOException {
		
		  
		ObjectOutputStream oos = new ObjectOutputStream(out);  			  
		oos.writeObject(this);
		System.out.println("Initiating handshake with peer" + this.PeerID);
	}
	
	public int ReceiveHandShake (InputStream in) throws IOException {
		try {
			ObjectInputStream ois = new ObjectInputStream(in);  
			HandShake RespMsg = (HandShake)ois.readObject();  
			if (RespMsg != null) {
			
				
				return RespMsg.PeerID;
				
			}
			else {
				return -1;
			}
			
		} 
		catch (ClassNotFoundException ex) {
			System.out.println(ex);
		}
		finally {
			//is.close();
			//ois.close();
		}
		return -1;
	}
}