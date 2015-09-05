package FileShare;
import java.io.Serializable;

public class NormalMsg implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8361597158056482861L;
	int MessageLength;
	int MessageType;
	
	public NormalMsg () {
		
	}
	
	public NormalMsg (int MsgLen,int MsgType) {
		this.MessageLength = MsgLen;
		this.MessageType = MsgType;
	}	
}