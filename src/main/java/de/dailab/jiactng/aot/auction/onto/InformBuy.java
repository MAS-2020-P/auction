package de.dailab.jiactng.aot.auction.onto;

import java.util.List;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Sent by auctioneer to bidder to inform about the result of the auction.
 * Contains whether the bidder has won the item or not, or whether its bid
 * was invalid, and the price the resource was finally sold. If the bidder
 * did not win, it does not tell who won instead, though.
 */
public class InformBuy implements IFact {

	private static final long serialVersionUID = -7597674901576948939L;

	public enum BuyType { WON, LOST, INVALID }
	
	/** the outcome of the call */
	private final BuyType type;
	
	/** the ID of the call in question */
	private final Integer callId;
	
	/** the resource being sold */
	private final List<Resource> bundle;
	
	/** the price the resource was sold at, or null */
	private final Double price;
	
	
	public InformBuy(BuyType type, Integer callId, List<Resource> bundle, Double price) {
		this.type = type;
		this.callId = callId;
		this.bundle = bundle;
		this.price = price;
	}
	
	public Integer getCallId() {
		return callId;
	}

	public void setCallId(Integer callId) {
		throw new UnsupportedOperationException();
	}

	public List<Resource> getBundle() {
		return bundle;
	}
	
	public BuyType getType() {
		return type;
	}
	
	public Double getPrice() {
		return price;
	}
	
	@Override
	public String toString() {
		return String.format("InformBuy(%s, %d, %s, %.2f)", type, callId, bundle, price);
	}
	
}
