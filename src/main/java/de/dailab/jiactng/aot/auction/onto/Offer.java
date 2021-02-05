package de.dailab.jiactng.aot.auction.onto;

import java.util.List;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Sent by the Bidder to offer one or more of its own resources for sale.
 * If reservation price is not met, the Bidder gets the resource back.
 */
public class Offer implements IFact {

	private static final long serialVersionUID = 3456389482058944999L;

	/** ID of the responsible auctioneer */
	private final Integer auctioneerId;
	
	/** the ID of the bidder making the offer */
	private final String bidderId;
	
	/** the bundle of resources to sell */
	private final List<Resource> bundle;
	
	/** the price the seller is expecting */
	private final Double price;
	
	
	public Offer(Integer auctioneerId, String bidderId, List<Resource> bundle, Double price) {
		this.auctioneerId = auctioneerId;
		this.bidderId = bidderId;
		this.bundle = bundle;
		this.price = price;
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

	public List<Resource> getBundle() {
		return bundle;
	}
	
	public Double getPrice() {
		return price;
	}
	
	@Override
	public String toString() {
		return String.format("Offer(%d, %s, %s, %.2f)", auctioneerId, bidderId, bundle, price);
	}
	
}
