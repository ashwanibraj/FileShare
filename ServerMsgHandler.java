package FileShare;

import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.io.*;


public class ServerMsgHandler implements Serializable {

	private static final long serialVersionUID = -8270502409155375127L;
	// private static final long serialVersionUID = -174333938837408245L;
	public final int CHOKE = 0;
	public final int UNCHOKE = 1;
	public final int INTERESTED = 2;
	public final int NOTINTERESTED = 3;
	public final int HAVE = 4;
	public final int BITFIELD = 5;
	public final int REQUEST = 6;
	public final int PIECE = 7;
	
	public ServerMsgHandler () {}
	
	public Object listenForMessages (Socket soc, serverConnect es) throws IOException {
		
		try {
			InputStream is = soc.getInputStream();  
			ObjectInputStream ois = new ObjectInputStream(is);
			
			Object obj = ois.readObject();
			es.nm = (NormalMsg)obj;
			
			return obj;
			
			
		}
		catch (ClassNotFoundException ex) {
			System.out.println(ex);
		}
		
		return null;
		
	}
	
	public void HandleMessages (int MsgType, Object obj, serverConnect es, HashSet<Integer> localReceivedByteIndex) throws IOException {
		
		System.out.println("Message handler type: "+MsgType);
		
		switch (MsgType) {
		
		case UNCHOKE:
			break;
		
		case CHOKE:
			break;
		
		case INTERESTED:
			InterestedMsg fromClientIntMsg = (InterestedMsg)obj;
			es.cPeerID = fromClientIntMsg.clientPeerID;
							
			es.interested = true;
			es.pObj.log.receivedInterested(es.cPeerID);
			es.pObj.ListofInterestedPeers.add(es.cPeerID);			
			
			while ( !(es.pObj.PreferredNeighbors.contains(es.cPeerID)) || (es.pObj.optPeerID != es.cPeerID) );
			ChokeUnchoke c = new ChokeUnchoke(0, UNCHOKE);
			//c.SendUnchokeMsg(es.connectionSocket);	
			es.pObj.log.Unchoked(es.cPeerID);
						
			break;
			
		case NOTINTERESTED:
			
			NotInterestedMsg ntIm = (NotInterestedMsg)obj;
			es.cPeerID = ntIm.clientPeerID;
			es.notInterested = true;
			es.pObj.log.receivedNotInterested(es.cPeerID);
			
			if (ntIm.finished == true && es.pObj.ListofInterestedPeers.contains(es.cPeerID)) {
				es.pObj.ListofInterestedPeers.remove(es.cPeerID);
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			/*
			boolean test = false;
			while (test==false){
				if(!es.pObj.receivedByteIndex.isEmpty()){
					test = true;
					ArrayList<Integer> list = new ArrayList<Integer>(es.pObj.receivedByteIndex);
					int testpiece = list.get(list.size()-1);
					//HaveMsg hm = new HaveMsg();
					System.out.println("Sending have to: "+es.cPeerID);
					HaveMsg hmsg = new HaveMsg(4, 4, testpiece);
					hmsg.SendHaveMsg(es.connectionSocket);
					
				}
			}
			*/
			break;
		
		case HAVE:			
			break;
		
		case BITFIELD:
			break;
			
		case REQUEST:
			//Receive the request message.
			ReqMsg rm = (ReqMsg)obj;
			int pieceIndex = rm.msgByteIndex;
			
			if (pieceIndex != -1) {
				//Send piece message.
				FileHandler f = new FileHandler();
				ArrayList<Integer> filePiece = f.readPiece(pieceIndex, es.PeerID);
				
				if ((es.pObj.PreferredNeighbors.contains(es.cPeerID)) || (es.pObj.optPeerID == es.cPeerID)) {
					//System.out.println("PRESENT inside ||");
					//TODO: if (have == false)
					localReceivedByteIndex = es.pObj.receivedByteIndex;
					
					HaveMsg hm = new HaveMsg();
					ArrayList<Integer> haveList = hm.prepareHaveList(es.pObj.receivedByteIndex, localReceivedByteIndex);
					for (int i = 0; i < haveList.size(); i++) {
						HaveMsg hmsg = new HaveMsg(4, HAVE, haveList.get(i));
						//hmsg.SendHaveMsg(es.connectionSocket);
						localReceivedByteIndex.add(haveList.get(i));
					}
					
					//Send piece msg.
					PieceMsg pm = new PieceMsg(4, PIECE, pieceIndex, filePiece);
					//pm.SendPieceMsg(es.connectionSocket);
					System.out.println("Index of sent piece:"+pieceIndex);
					System.out.println("end of piece msg transfer");
				}
				else {
					//send choke message.
					ChokeUnchoke cm = new ChokeUnchoke(0, CHOKE);
					//cm.SendChokeMsg(es.connectionSocket);
					es.pObj.log.Choked(es.cPeerID);
					
					while ( !(es.pObj.PreferredNeighbors.contains(es.cPeerID)) || (es.pObj.optPeerID != es.cPeerID) );
					ChokeUnchoke c1 = new ChokeUnchoke(0, UNCHOKE);
					//c1.SendUnchokeMsg(es.connectionSocket);
					es.pObj.log.Unchoked(es.cPeerID);
				}					
			}
			
			break;
			
		case PIECE:
			break;
			
		default: 
			System.exit(0);
		
		}
		
	}
	
}