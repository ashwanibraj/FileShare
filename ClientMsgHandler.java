package FileShare;

import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.io.*;


public class ClientMsgHandler implements Serializable {

	private static final long serialVersionUID = -6801972033167530864L;
	public final int CHOKE = 0;
	public final int UNCHOKE = 1;
	public final int INTERESTED = 2;
	public final int NOTINTERESTED = 3;
	public final int HAVE = 4;
	public final int BITFIELD = 5;
	public final int REQUEST = 6;
	public final int PIECE = 7;
	
	public ClientMsgHandler () {}
	
	public Object listenForMessages (InputStream is, clientConnect ec) throws IOException {
		
		try {
			ObjectInputStream ois = new ObjectInputStream(is);
			Object obj = ois.readObject();
			ec.nm = (NormalMsg)obj;
			return obj;
		}
		catch (ClassNotFoundException ex) {
			System.out.println(ex);
		}
		
		return null;
		
	}
	
	public void HandleMessages (int MsgType, Object obj, clientConnect ec, HashSet<Integer> localReceivedByteIndex) throws IOException {
		
		System.out.println("Message handler type: "+MsgType);
		
		switch (MsgType) {
		
		case UNCHOKE:
			
			ReqMsg rm = new ReqMsg();
			int pieceIndex=0;
			synchronized (ec.pObj.neededByteIndex) {
				
				pieceIndex = rm.getPieceIndex(ec.pObj.neededByteIndex);
			}
			System.out.println("Requesting for peice index " + pieceIndex);
			ReqMsg rm1 = new ReqMsg(4, REQUEST, pieceIndex);
			rm1.SendRequestMsg(ec.out);
			
			break;
		
		case CHOKE:
			break;
		
		case INTERESTED:
			
			InterestedMsg fromClientIntMsg = (InterestedMsg)obj;
			//ec.clientPeerID = fromClientIntMsg.clientPeerID;
							
			//ec.interested = true;
			ec.pObj.log.receivedInterested(ec.clientPeerID);
			
			synchronized (ec.pObj.ListofInterestedPeers) {
				System.out.println("Add peer to interested peer list: " + ec.clientPeerID);
				ec.pObj.ListofInterestedPeers.add(ec.clientPeerID);
			}
						
			
			break;
			
		case NOTINTERESTED:
			
			NotInterestedMsg ntIm = (NotInterestedMsg)obj;
			ec.clientPeerID = ntIm.clientPeerID;
			//ec.notInterested = true;
			ec.pObj.log.receivedNotInterested(ec.clientPeerID);
			
			if (ntIm.finished == true && ec.pObj.ListofInterestedPeers.contains(ec.clientPeerID)) {
				ec.pObj.ListofInterestedPeers.remove(ec.clientPeerID);
			}
			
			break;
		
		case HAVE:			
				HaveMsg rxHvMsg = new HaveMsg();
				//int byteIndex = rxHvMsg.ReceiveHaveMsg(ec.in);
				rxHvMsg = (HaveMsg)obj;
				int byteIndex = rxHvMsg.msgByteIndex;
				if (byteIndex != -1) {
					//System.out.println("Received a have message from:"+ec.peerID);
					ec.pObj.log.receivedHave(ec.peerID, byteIndex);
					//System.out.println("and byteIndex:"+byteIndex);
					ec.serverPeerBitFieldMsg.UpdateBitFieldMsg(byteIndex);
					if (ec.myBitFields.bitFieldMsg[byteIndex] == false) {
						InterestedMsg im = new InterestedMsg(0,INTERESTED, ec.clientPeerID);
						im.SendInterestedMsg(ec.out);
					}
					else {
						NotInterestedMsg ntm = new NotInterestedMsg(0, NOTINTERESTED, ec.clientPeerID, false);
						ntm.SendNotInterestedMsg(ec.out);
					}					
				}
				else {
					System.out.println("Error in receiving have msg");
				}
			
			break;
		
		case BITFIELD:
			break;
			
		case REQUEST:
			//Get request and send piece
			ReqMsg reqMsg = (ReqMsg)obj;
			int pieceIndex1 = reqMsg.msgByteIndex;
			
			System.out.println("Piece request received for:"+pieceIndex1);
			
			if (pieceIndex1 != -1) {
				//Send piece message.
				FileHandler f = new FileHandler();
				ArrayList<Integer> filePiece = f.readPiece(pieceIndex1, ec.peerID);
				
				boolean check = false;
				
				synchronized (ec.pObj.PreferredNeighbors) {
					//check = ec.pObj.PreferredNeighbors.contains(ec.peerID);	
				}
				check = true;
				if ( check == true || (ec.pObj.optPeerID == ec.clientPeerID)) {
					
					//localReceivedByteIndex = ec.pObj.receivedByteIndex;
					
					/*
					HaveMsg hm = new HaveMsg();
					ArrayList<Integer> haveList = hm.prepareHaveList(ec.pObj.receivedByteIndex, localReceivedByteIndex);
					for (int i = 0; i < haveList.size(); i++) {
						HaveMsg hmsg = new HaveMsg(4, HAVE, haveList.get(i));
						hmsg.SendHaveMsg(ec.out);
						localReceivedByteIndex.add(haveList.get(i));
					}*/
					
					//Send piece msg.
					PieceMsg pm = new PieceMsg(4, PIECE, pieceIndex1, filePiece);
					pm.SendPieceMsg(ec.out);
					System.out.println("Sent piece index:"+pieceIndex1);
					System.out.println("End of piece msg transfer");
				}
				else {
					//send choke message.
					//ChokeUnchoke cm = new ChokeUnchoke(0, CHOKE);
					//cm.SendChokeMsg(ec.out);
				//	ec.pObj.log.Choked(ec.clientPeerID);
					
					//ec.pObj.log.Unchoked(ec.clientPeerID);
				}					
			}
			
			break;
			
		case PIECE:
			PieceMsg pm = (PieceMsg)obj;
			FileHandler f = new FileHandler();
			f.writePiece(pm.Filepiece, pm.msgByteIndex, ec.clientPeerID);
			
			System.out.println("Received piece index:"+pm.msgByteIndex);
			
			synchronized (ec.pObj.myBitFields) {
				if (!ec.pObj.myBitFields.contains(pm.msgByteIndex)) {
					ec.pObj.myBitFields.UpdateBitFieldMsg(pm.msgByteIndex);
				}
			}
			
			synchronized (ec.pObj.neededByteIndex) {
				ec.pObj.neededByteIndex.remove(pm.msgByteIndex);	
			}
			
			//ec.pObj.receivedByteIndex.add(pm.msgByteIndex);
			//ec.updateHashAndSendHaveMsg(pm.msgByteIndex);
			
			synchronized (ec.pObj.haveList) {
				ec.pObj.haveList.add(pm.msgByteIndex);	
			}
			
			int pieceIndex2=-1;
			synchronized (ec.pObj.neededByteIndex) {
				synchronized (ec.pObj.receivedByteIndex) {
					if (!ec.pObj.neededByteIndex.isEmpty()) {
						//System.out.println("neededbyteindex size"+ec.pObj.neededByteIndex.size());
						//System.out.println();
						
						ReqMsg rm2 = new ReqMsg();
						synchronized (ec.pObj.neededByteIndex) {
							pieceIndex2 = rm2.getPieceIndex(ec.pObj.neededByteIndex);
						}
						
						
						System.out.println("Sending request for: "+pieceIndex2);
						ec.pObj.log.downloadedPiece(ec.peerID, pieceIndex2, ec.pObj.receivedByteIndex.size());
						ReqMsg rm3 = new ReqMsg(4, REQUEST, pieceIndex2);
						rm3.SendRequestMsg(ec.out);
						
						System.out.println("Sent request for piece: "+pieceIndex2);
						synchronized (ec.pObj.esclientmap) {
							
							for (Integer key: ec.pObj.esclientmap.keySet()) {
								System.out.println("Sending have message to:"+key); 
								clientConnect tempEC = ec.pObj.esclientmap.get(key);
								HaveMsg hm = new HaveMsg(4, HAVE, pm.msgByteIndex);
								hm.SendHaveMsg(tempEC.out);
							}
						}
				
					}
					else {
						//Terminate once the client has received all the pieces.
						FileHandler f1 = new FileHandler();
						f1.ReadCommonConfigFile();
						f1.JoinFile(f1.inputFileName, f1.fileSize, f1.pieceSize, f1.pieceCount, ec.clientPeerID);
						ec.pObj.ListofInterestedPeers.remove(ec.clientPeerID);
						//send not interested message
						ec.pObj.log.completedDownload();
						ec.finished = true;
					}
				}
				
			}
			break;
			
		default: 
			System.exit(0);
		
		}
		
	}
	
}
