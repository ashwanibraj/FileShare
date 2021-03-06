package FileShare;

import java.util.*;

public class ScheduledTasks {
	
	Timer timer1;
	Timer timer2;
	
	PeerProcess pp;
	int p;
	int m;
	
    public ScheduledTasks (PeerProcess pp, int p, int m) {
       
    	this.pp = pp;
    	this.p = p;
    	this.m = m;
    	
        timer1 = new Timer();
        timer1.scheduleAtFixedRate(new SelectPreferredNeighbors(this.pp), 0, p*1000);
        
        timer2 = new Timer();
        timer2.scheduleAtFixedRate(new SelectOptNeighbors(this.pp), 0, m*1000);
    }

    class SelectPreferredNeighbors extends TimerTask {
        
		PeerProcess pp;
    	public SelectPreferredNeighbors (PeerProcess pp) {
    		this.pp = pp;
    	}    	
    	
        public void run() {
            
        	try {
    			 
    			FileHandler f = new FileHandler();
    			int K=0;
    			System.out.println("Selecting neighbors!");
    			//If the peer has the entire file, it has to randomly select K preferred neighbors.
    			if (f.CheckHasFile(this.pp.ServerPeerID)) {
    				
    				System.out.println("Peer has entire file. Start timer.");
    				//Don't send a unchoke message if the interested list is empty. Keep waiting.
    				//synchronized (this.pp.ListofInterestedPeers) {
    					if (this.pp.ListofInterestedPeers.isEmpty()) {
    						System.out.println("Return timer");
    						return;
    					}
    				//}
    				
    				ChokeUnchoke c = new ChokeUnchoke(0, 1);
    				K = f.GetNumberOfPreferredNeighbors();
    				
    				
    				//Interested peers are less than the preferred neighbors. Initially.	
    				System.out.println("Synchronising list of interested peers.");
    				synchronized (this.pp.ListofInterestedPeers) {
    					if (this.pp.ListofInterestedPeers.size() < K) {
        					K = this.pp.ListofInterestedPeers.size();
        				}	
					}
    				
    				
    				System.out.println("Preferred neighbors updated successfully");
    				//select preferred neighbors.
    				
    				ArrayList<Integer> preferredList;
    				
    				synchronized (this.pp.PreferredNeighbors) {
    					this.pp.PreferredNeighbors = c.SelectPreferredNeighbors(this.pp.ListofInterestedPeers, K);
    					if (this.pp.PreferredNeighbors.isEmpty()) {
    						System.out.println("No preferred neighbors to choose from.");
    					}
    					preferredList = new ArrayList<Integer>(this.pp.PreferredNeighbors);
					}
    				
    				
    				int []peer_IDs = new int[this.pp.PreferredNeighbors.size()];
    				
    				ChokeUnchoke cm = new ChokeUnchoke(0, 1);
    				clientConnect ec ;//= new clientConnect();
    				for (int i=0; i < preferredList.size(); i++) {
    					peer_IDs[i] = preferredList.get(i);
    					System.out.println("peer ids"+peer_IDs[i]);
    					synchronized (PeerProcess.esclientmap) {
							
						}
    					
    					Set ids = PeerProcess.esclientmap.keySet();
    					System.out.println("Peer ids : " + ids);
    					 ec = PeerProcess.esclientmap.get(peer_IDs[i]);
    					 if (ec != null)
    					 {
    						 cm.SendUnchokeMsg(ec.out);
    						 System.out.println("Unchoking peer " + peer_IDs[i]);
    					 } else {
    						 System.out.println("No client to connect");
    					 }
    				}
    			
    				
    				//this.pp.log.changePrefNeighbor(peer_IDs, K);
    			}
    			
    			//Else: Choose the neighbors who provide the server with the highest download rate.
    			else {
    				System.out.println("Choosing from multiple neighbors.");
    				//Don't send a unchoke message if the interested list is empty. Keep waiting.
    				
    				synchronized (this.pp.ListofInterestedPeers) {
    					while (this.pp.ListofInterestedPeers.isEmpty());
					}
    				
    				boolean isEmpty = false;
    				
    				synchronized (this.pp.PreferredNeighbors) {
    					isEmpty = this.pp.PreferredNeighbors.isEmpty();
					}
    				
    				
    				if (!isEmpty) {
    				
    					synchronized (this.pp.PreferredNeighbors) {
    						this.pp.PreferredNeighbors.clear();
						}
    									
    					ChokeUnchoke c = new ChokeUnchoke(0, 1);
	    				K = f.GetNumberOfPreferredNeighbors();
	    				
	    				synchronized (this.pp.ListofInterestedPeers) {
	    					//Interested peers are less than the preferred neighbors. Initially.					
		    				if (this.pp.ListofInterestedPeers.size() < K) {
		    					K = this.pp.ListofInterestedPeers.size();
		    				}	
						}
	    				
	    				
	    				System.out.println("Preferred neighbors updated.");
	    				
	    				ArrayList<Integer> preferredList;
	    				//select preferred neighbors.
	    				synchronized (this.pp.PreferredNeighbors) {
	    					this.pp.PreferredNeighbors = c.SelectPreferredNeighbors(this.pp.ListofInterestedPeers, K);	
	    					preferredList = new ArrayList<Integer>(this.pp.PreferredNeighbors);
						}
	    				
	    				int []peer_IDs = new int[this.pp.PreferredNeighbors.size()];
	    				
	    				
	    				ChokeUnchoke cm = new ChokeUnchoke(0, 1);
						
	    				synchronized (this.pp.esclientmap) {
	    					for (int i=0; i < preferredList.size(); i++) {
	    						peer_IDs[i] = preferredList.get(i);
	    						System.out.println("Transfer file to peer " + peer_IDs[i]);
	    	    				
		    					clientConnect ec = this.pp.esclientmap.get(peer_IDs[i]);
		    					cm.SendUnchokeMsg(ec.out);
		    					System.out.println("Unchoking peer " + peer_IDs[i]);
		    				}	
						}
	    				
	    				ChokeUnchoke cm1 = new ChokeUnchoke(0, 0);
	    				ArrayList<Integer> chokeList = new ArrayList<Integer>();
	    				
	    				synchronized (this.pp.PreferredNeighbors) {
	    					synchronized (this.pp.ListofInterestedPeers) {
	    						clientConnect ec1 = new clientConnect();
	    						
	    						chokeList = c.prepareChokeList(this.pp.PreferredNeighbors, this.pp.ListofInterestedPeers);
	    	    				for (int i=0; i < chokeList.size(); i++) {
	    	    					if (chokeList.get(i) != this.pp.optPeerID) {
	    	    						 synchronized (this.pp.esclientmap) {
	    	    							 ec1 = this.pp.esclientmap.get(chokeList.get(i));
										}
	    	    						
	    	    						if (ec1 != null) {
	    	    							cm1.SendChokeMsg(ec1.out);
	    	    						}
	    	    					}
	    	    				}
	    					}
	    				}
	    				
	    				//this.pp.choke = true;

    				}	
    			}
    		}
        	catch(Exception ex) {
    			ex.printStackTrace();
    		}
        	System.out.println("Exit.");
        }    
    }
    
    class SelectOptNeighbors extends TimerTask {
        
    	PeerProcess pp;
		
    	public SelectOptNeighbors (PeerProcess pp) {
    		this.pp = pp;
    	}    	
    	
        public void run(){
            
        	try {

        		System.out.println("Wait for list of interested peers");
        		//Don't send a choke message if the interested list is empty. Keep waiting.
        		synchronized (this.pp.ListofInterestedPeers) {
        			if (this.pp.ListofInterestedPeers.isEmpty()){
        				return;
        			}
        		}
        		
    			//If the peer has the entire file, it has to randomly select K preferred neighbors. 
				ChokeUnchoke c = new ChokeUnchoke(0, 1);
				int optPeerID=0;
				
				//select opt neighbors.
				synchronized (this.pp.ListofInterestedPeers) {
					synchronized (this.pp.PreferredNeighbors) {
						optPeerID = c.SelectOptNeighbors(this.pp.ListofInterestedPeers, this.pp.PreferredNeighbors);
					}
				}
				
    			
				System.out.println("Selected opt unchoked neighbor "+optPeerID);
				this.pp.optPeerID = optPeerID;
			
				this.pp.log.changeOptimUnchokedNeighbor(optPeerID);
				clientConnect ec3 = this.pp.esclientmap.get(optPeerID);
				
				if (ec3 != null && optPeerID != 0) {
				c.SendUnchokeMsg(ec3.out);
				}
				
    		}
        	
    		catch(Exception ex) {
    			System.out.println(ex);
    		}
        }

    }
}
