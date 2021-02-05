package de.dailab.jiactng.aot.auction.server;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.Collection;
import java.util.Random;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

import de.dailab.jiactng.agentcore.action.AbstractMethodExposingBean;
import de.dailab.jiactng.agentcore.comm.CommunicationAddressFactory;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.IActionDescription;
import de.dailab.jiactng.aot.auction.onto.Bidder;

/**
 * Abstract Superclass for the other AuctioneerBeans, providing some helper methods
 * for sending messages to different groups of bidders, and for accessing known bidders
 * stored in the agent's memory. The latter will only work if all the auctioneer beans
 * are part of the same agent, sharing the same memory.
 */
public class AbstractAuctioneerBean extends AbstractMethodExposingBean {

	/*
	 * CONFIGURATION
	 * those can be set in the Spring configuration file
	 */
	
	/** message group where to send multicast messages to; this address is
	 * used by the auctioneer for sending CallForBid messages, or informing
	 * about a new auction; Bidders should listen at this group, but not reply
	 * to it, but to the auctioneer itself. The auctioneer does _not_ listen
	 * to this group. */
	protected String messageGroup;

	/** ID of this auctioneer, used for knowing who should handle which bids and offers */
	protected Integer auctioneerId;

	/*
	 * MANAGEMENT STUFF
	 */

	/** random number generator */
	protected Random random;
	
	/** for synchronization */
	protected static final Object LOCK = new Object();
	
	/** quick&dirty log appender shared by all the auctioneers for persisting more output */
	public static final ByteArrayOutputStream LOG_STREAM = new ByteArrayOutputStream();
	public static final String LOG_FORMAT = "%d{yy-MM-dd HH:mm:ss} %p [%c] %m%n";

	@Override
	public void doStart() throws Exception {
		log.addAppender(new WriterAppender(new PatternLayout(LOG_FORMAT), new OutputStreamWriter(LOG_STREAM)));
	}
	
	/*
	 * BIDDER MANAGEMENT
	 * current bidders are stored in agent's memory to share them among beans
	 */
	
	/**
	 * remove all bidders from memory; should be done before starting a new auction
	 */
	protected void clearBidders() {
		memory.removeAll(new Bidder(null, null, null, null));
	}
	
	/**
	 * add a new bidder to the memory
	 */
	protected void addBidder(Bidder bidder) {
		memory.write(bidder);
	}

	/**
	 * get bidder matching the given template, e.g. with same bidderId,
	 * but returns null if no bidder id is given, and not any random bidder
	 */
	protected Bidder getBidder(String bidderId) {
		return bidderId == null ? null : memory.read(new Bidder(bidderId, null, null, null));
	}

	/**
	 * get all bidders from memory
	 */
	protected Collection<Bidder> getBidders() {
		return memory.readAll(new Bidder(null, null, null, null));
	}
	
	
	/*
	 * SENDING MESSAGES TO BIDDERS
	 */

	/**
	 * send message to bidder with given bidder ID
	 */
	protected void send(IFact payload, String bidderId) {
		log.info(String.format("Sending %s to %s", payload, bidderId));
		send(payload, getBidder(bidderId).getAddress());
	}

	/**
	 * send message to multicast communication group
	 */
	protected void sendGroup(IFact payload) {
		log.info(String.format("Sending %s to group", payload));
		send(payload, CommunicationAddressFactory.createGroupAddress(messageGroup));
	}
	
	/**
	 * send message to all registered bidders
	 */
	protected void sendRegistered(IFact payload) {
		log.info(String.format("Sending %s to registered bidders", payload));
		getBidders().forEach(b -> send(payload, b.getAddress()));
	}
	
	/**
	 * send message to given communication address
	 */
	protected void send(IFact payload, ICommunicationAddress address) {
		JiacMessage message = new JiacMessage(payload);
		IActionDescription sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		invoke(sendAction, new Serializable[] {message, address});
	}
	
	/**
	 * Fraud prevention. Check whether the message was really sent by that bidder.
	 */
	protected boolean checkMessage(JiacMessage msg, String bidderId) {
		Bidder bidder = getBidder(bidderId);
		return bidder != null && msg.getSender().equals(bidder.getAddress());
	}

	
	/*
	 * GETTERS AND SETTERS
	 * needed for setting properties via Spring configuration file
	 */

	public void setMessageGroup(String messageGroup) {
		this.messageGroup = messageGroup;
	}
	
	public void setAuctioneerId(Integer auctioneerId) {
		this.auctioneerId = auctioneerId;
	}
}
