package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Class encapsulating a Bidder, used by the Auctioneer Beans
 */
public class Bidder implements IFact {

	private static final long serialVersionUID = 2813812910820582594L;

	/** this bidder's bidder ID */
	private final String bidderId;
	
	/** used to identify agents belonging to the same group of students */
	private final String groupToken;
	
	/** communication address, used for sending private messages */
	private final ICommunicationAddress address;
	
	/** the wallet */
	private final Wallet wallet;

	
	public Bidder(String bidderId, String groupToken, ICommunicationAddress address, Wallet wallet) {
		this.bidderId = bidderId;
		this.groupToken = groupToken;
		this.address = address;
		this.wallet = wallet;
	}
	
	public String getBidderId() {
		return bidderId;
	}
	
	// this method seems to be needed in order for the Memory to work properly
	public void setBidderId(String bidderId) {
		throw new UnsupportedOperationException();
	}
	
	public String getGroupToken() {
		return groupToken;
	}
	
	public ICommunicationAddress getAddress() {
		return address;
	}

	public Wallet getWallet() {
		return wallet;
	}
	
	@Override
	public String toString() {
		return String.format("Bidder(%s, %s, %s, %s)", bidderId, groupToken, address, wallet);
	}
	
}
