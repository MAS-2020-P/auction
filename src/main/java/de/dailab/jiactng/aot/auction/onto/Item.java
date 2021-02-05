package de.dailab.jiactng.aot.auction.onto;

import java.util.List;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Wrapper for an item to be sold, including the types of Resources
 * bundled in this item, the seller, if any, and the reservation price.
 */
public class Item implements IFact {

	private static final long serialVersionUID = -537290120804212290L;

	/** the ID of this item */
	private final Integer callId;
	
	/** the resource being sold */
	private final List<Resource> bundle;
	
	/** the ID of the bidder selling the item, or null */
	private final String seller;
	
	/** the reservation price for this item */
	private final Double price;
	
	
	public Item(Integer callId, List<Resource> bundle, String seller, Double price) {
		this.callId = callId;
		this.bundle= bundle;
		this.seller = seller;
		this.price = price;
	}

	public Integer getCallId() {
		return callId;
	}

	public List<Resource> getBundle() {
		return bundle;
	}
	
	public String getSeller() {
		return seller;
	}
	
	public Double getPrice() {
		return price;
	}
	
	@Override
	public String toString() {
		return String.format("Item(%d, %s, %s, %.2f)", callId, bundle, seller, price);
	}
}
