package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * This message is sent to all Bidders at the very beginning
 * of all the auctions. Bidders should reply with Register.
 * 
 * Not to be confused with StartAuction
 */
public class StartAuctions implements IFact {

	private static final long serialVersionUID = -3738971099847743500L;

	/** ID of this auction; to identify duplicate StartAuctions messages */
	private final Integer auctionsId;

	/** description to distinguish different StartAuction messages */
	private final String message;

	
	public StartAuctions(Integer auctionsId, String message) {
		this.auctionsId = auctionsId;
		this.message = message;
	}
	
	public Integer getAuctionsId() {
		return auctionsId;
	}
	
	public String getMessage() {
		return message;
	}
	
	@Override
	public String toString() {
		return String.format("StartAuctions(%d, %s)", auctionsId, message);
	}
	
}
