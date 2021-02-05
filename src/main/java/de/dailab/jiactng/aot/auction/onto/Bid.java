package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Message containing a Bid by a bidder for buying the Resource
 * in the current call.
 */
public class Bid implements IFact {

	private static final long serialVersionUID = -123356410215917626L;

	/** ID of the responsible auctioneer */
	private final Integer auctioneerId;
	
	/** ID of the bidder issuing this bid */
	private final String bidderId;
	
	/** the ID of the call-for-bids */
	private final Integer callId;
	
	/** the price offered for the resource */
	private final Double offer;
	
	
	public Bid(Integer auctioneerId, String bidderId, Integer callId, Double offer) {
		this.auctioneerId = auctioneerId;
		this.bidderId = bidderId;
		this.callId = callId;
		this.offer = offer;
	}

	public Integer getAuctioneerId() {
		return auctioneerId;
	}
	
	// this method seems to be needed in order for the Memory to work properly
	public void setAuctioneerId(Integer auctioneerId) {
		throw new UnsupportedOperationException();
	}

	public String getBidderId() {
		return bidderId;
	}
	
	public Integer getCallId() {
		return callId;
	}

	public void setCallId(Integer callId) {
		throw new UnsupportedOperationException();
	}
	
	public Double getOffer() {
		return offer;
	}
	
	public String toString() {
		return String.format("Bid(%d, %s, %d, %.2f)", auctioneerId, bidderId, callId, offer);
	}

}
