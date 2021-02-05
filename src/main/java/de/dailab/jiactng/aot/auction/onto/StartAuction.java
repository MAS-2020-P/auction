package de.dailab.jiactng.aot.auction.onto;

import java.util.Collection;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * This message is sent to all Bidders at the very beginning
 * of an individual auction. Do NOT reply with Register to this message!
 * 
 * This message indicates the start of a new auction, and makes
 * the auctioneer agent known to the bidders. It MAY also contain
 * the number of items and the items themselves to be sold.
 * 
 * Not to be confused with StartAuctions.
 */
public class StartAuction implements IFact {

	public enum Mode {A, B, C}
	
	private static final long serialVersionUID = -3738971099847743500L;

	/** description to distinguish different StartAuction messages */
	private final Mode mode;
	
	/** ID of the responsible auctioneer */
	private final Integer auctioneerId;
	
	/** number of items to be sold */
	private final Integer numItems;
	
	/** the initial items to be sold by the auctioneer (can be null) */
	private final Collection<Item> initialItems;
	
	
	public StartAuction(Mode mode, Integer auctioneerId, Integer numItems, Collection<Item> initialItems) {
		this.mode = mode;
		this.auctioneerId = auctioneerId;
		this.numItems = numItems;
		this.initialItems = initialItems;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		throw new UnsupportedOperationException();
	}
	
	public Integer getAuctioneerId() {
		return auctioneerId;
	}
	
	public Integer getNumItems() {
		return numItems;
	}
	
	public Collection<Item> getInitialItems() {
		return initialItems;
	}
	
	@Override
	public String toString() {
		return String.format("StartAuction(%s, %s, %s, %s)", mode, auctioneerId, numItems, initialItems);
	}
	
}
