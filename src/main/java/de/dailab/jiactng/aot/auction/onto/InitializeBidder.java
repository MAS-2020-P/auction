package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Sent in response to Register message. Contains bidder's
 * initial Wallet, i.e. their starting resources, and credits.
 * 
 * Note: If another bidder has already registered with the same
 * bidderId, then this message will return a "null" Wallet.
 */
public class InitializeBidder implements IFact {

	private static final long serialVersionUID = 1061663542225958995L;
	
	/** the ID of the bidder to whom this message is sent */
	private final String bidderId;
	
	/** the bidder's initial wallet */
	private final Wallet wallet;

	
	public InitializeBidder(String bidderId, Wallet wallet) {
		this.bidderId = bidderId;
		this.wallet = wallet;
	}
	
	public String getBidderId() {
		return bidderId;
	}
	
	public Wallet getWallet() {
		return wallet;
	}
	
	@Override
	public String toString() {
		return String.format("InitializeBidder(%s, %s)", bidderId, wallet);
	}
	
}
