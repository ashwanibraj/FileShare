package FileShare;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

class clientConnect implements Runnable{
	
	//int peerCount;
	int peerID;
	int clientPeerID;
	int portNumber;
	String hostName;
	Socket clientSocket;
	Hashtable<Integer, clientConnect> esclientmap;
	BitFields serverPeerBitFieldMsg;
	BitFields myBitFields;
	PeerProcess pObj;
	Boolean finished = false;
	Boolean initialStage = false;
	NormalMsg nm = new NormalMsg();	
	OutputStream out;
    InputStream in;
	
    public clientConnect() {
    	
    }
    
    public clientConnect (Socket socket, int peer_id, int cpeer_id, PeerProcess pp) throws IOException {
		this.peerID = peer_id; 
		this.clientPeerID = cpeer_id; 
		//this.hostName = peer_address;
		//this.portNumber = Integer.parseInt(peer_port);
		this.myBitFields = pp.myBitFields;		
		this.pObj = pp;
		this.clientSocket = socket;
		
		this.out = socket.getOutputStream();
		this.in = socket.getInputStream();
	}
	
//>>>>>	synchronized (esclientmap){
//		
//		if (!esclientmap.containsKey(clientPeerID))  {
//			esclientmap.put(clientPeerID, this);
//			//peerProcess.log ("Peer " +  mypid + " is connected from peer " + peerid);
//		    } 
//		
//	}	<<<<<<<<
	
	public void run() {
		
		try {
			this.finished = false;
			
			
			//Sending handshake message
			
			
			HandShake HMsg = new HandShake("HELLO", peerID);
			HMsg.SendHandShake (this.out);			
			
			//wait till client handshake is received from server
			
			if ((this.clientPeerID = HMsg.ReceiveHandShake(this.in))!= -1){
				System.out.println("Handshake successful ");
				
				if (!PeerProcess.esclientmap.contains(this.clientPeerID)) {
					System.out.println("Server side: Mapping client " + clientPeerID);
					PeerProcess.esclientmap.put(this.clientPeerID, this);
				}
				
			}
			else {
				System.out.println("Handshake failure");
			}
			
			
			//Sending bitfield message.
			
			myBitFields.SendBitFieldMsg(this.out);	
			
			//Fetching bitfield message from server
			BitFields receiveBMsg = new BitFields();
			BitFields returnBMsg;
			
			returnBMsg = receiveBMsg.ReceiveBitFieldMsg(this.in); 
			
			this.serverPeerBitFieldMsg = returnBMsg;
			
			//>>>>	if ( (myBitFields.AnalyzeReceivedBitFieldMsg(returnBMsg)) != null) {
			
			HashSet<Integer> res = myBitFields.AnalyzeReceivedBitFieldMsg(returnBMsg);
			
			if (res.size() != 0) {
				//send interested msg.
				InterestedMsg nIMsg = new InterestedMsg(0,2, clientPeerID);
				nIMsg.SendInterestedMsg(this.out);
		
			}
			else {
				
				//send not interested msg.
				NotInterestedMsg nIMsg = new NotInterestedMsg(0,3, clientPeerID, false);
				nIMsg.SendNotInterestedMsg(this.out);
				
			}
		
			//this.initialStage = true;
			
			ClientMsgHandler cm = new ClientMsgHandler();
			Object readObj = null;
			HashSet<Integer> localReceivedByteIndex = new HashSet<Integer>();
			
			while (true) {
				System.out.println("Client: Waiting for messages");
				readObj = cm.listenForMessages(this.in, this);
				System.out.println("Message Received");
				int msgType = this.nm.MessageType;
//				System.out.println("Msg type:"+msgType);
				cm.HandleMessages(msgType, readObj, this, localReceivedByteIndex);
				readObj = null;
				
				//Thread.sleep(1000);
				
				if (this.finished == true) {
					NotInterestedMsg ntIm = new NotInterestedMsg(0, 3, clientPeerID, true);
					ntIm.SendNotInterestedMsg(this.out);
					break;
				}
			}
			
			System.out.println("Client: Closing socket...");
			clientSocket.close();
			
			return;
		}
		catch (IOException ex) {
			System.out.println("Error: IOException: "+ex);
		}
	}

//	@Override
//	public void updateHashAndSendHaveMsg(int msgByteIndex) throws IOException {
//		// TODO Auto-generated method stub
//		
//		if (this.initialStage == true) {
//			System.out.println("In client: update hash");
//			this.pObj.receivedByteIndex.add(msgByteIndex);
//			Iterator<serverConnect> it = this.pObj.es.iterator();
//			
//			while (it.hasNext()) {
//				it.next().updateHashAndSendHaveMsg(msgByteIndex);
//			}
//		}
//	}	
}