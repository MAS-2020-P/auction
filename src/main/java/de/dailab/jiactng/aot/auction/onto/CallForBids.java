package de.dailab.jiactng.aot.auction.onto;

import java.util.List;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Sent by the auctioneer to advertise a new call for bids,
 * i.e. the auctioneer selling items to the bidders.
 */
public class CallForBids implements IFact {

	private static final long serialVersionUID = -2459640599311665642L;

	/** Whether the Bidder (!) is meant to buy or sell the item */
	public enum CfBMode { BUY, SELL};
	
	/** ID of the responsible auctioneer */
	private final Integer auctioneerId;
	
	/** the id of the call-for-bids */
	private final Integer callId;
	
	/** the type of resource */
	private final List<Resource> bundle;
	
	/** the minimum offer */
	private final Double minOffer;
	
	/** whether the bidder is buying or selling the item */
	private final CfBMode mode;
	
	/** id of the bidder making the offer, if any, or null */
	private final String offeringBidder;
	
	
	public CallForBids(Integer auctioneerId, Integer callId, List<Resource> bundle, Double minOffer, CfBMode mode, String offferingBidder) {
		this.auctioneerId = auctioneerId;
		this.callId = callId;
		this.bundle = bundle;
		this.minOffer = minOffer;
		this.mode = mode;
		this.offeringBidder = offferingBidder;
	}

	public Integer getAuctioneerId() {
		return auctioneerId;
	}

	public void setAuctioneerId(Integer auctioneerId) {
		throw new UnsupportedOperationException();
	}
	
	public Integer getCallId() {
		return callId;
	}

	public List<Resource> getBundle() {
		return bundle;
	}
	
	public Double getMinOffer() {
		return minOffer;
	}
	
	public CfBMode getMode() {
		return mode;
	}
	
	public String getOfferingBidder() {
		return offeringBidder;
	}
	
	@Override
	public String toString() {
		return String.format("CallForBids(%d, %d, %s, %.2f, %s, %s)", auctioneerId, callId, bundle, minOffer, mode, offeringBidder);
	}
	
}
