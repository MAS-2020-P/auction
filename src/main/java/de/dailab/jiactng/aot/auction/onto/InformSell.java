package de.dailab.jiactng.aot.auction.onto;

import java.util.List;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Inform the seller of the result, i.e. whether the item was sold,
 * and if so at what price.
 */
public class InformSell implements IFact {

	private static final long serialVersionUID = -6410327995358801993L;

	public enum SellType { SOLD, NOT_SOLD, INVALID }

	/** the outcome of the sale */
	private final SellType type;
	
	/** the ID of the call when the item was sold */
	private final Integer callId;
	
	/** the type of resource sold */
	private final List<Resource> bundle;
	
	/** the price, if sold, else null */
	private final Double price;
	
	
	public InformSell(SellType type, Integer callId, List<Resource> bundle, Double price) {
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
	
	public SellType getType() {
		return type;
	}
	
	public Double getPrice() {
		return price;
	}
	
	@Override
	public String toString() {
		return String.format("InformSell(%s, %d, %s, %.2f)", type, callId, bundle, price);
	}
	
}
