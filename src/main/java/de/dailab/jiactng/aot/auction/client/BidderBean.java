package de.dailab.jiactng.aot.auction.client;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.comm.CommunicationAddressFactory;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.IGroupAddress;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.IActionDescription;
import de.dailab.jiactng.aot.auction.onto.Register;
import de.dailab.jiactng.aot.auction.onto.StartAuction;
import de.dailab.jiactng.aot.auction.onto.StartAuctions;
import de.dailab.jiactng.aot.auction.onto.Wallet;
import de.dailab.jiactng.aot.auction.onto.Bid;
import de.dailab.jiactng.aot.auction.onto.Offer;
import de.dailab.jiactng.aot.auction.onto.Resource;
import de.dailab.jiactng.aot.auction.onto.CallForBids;
import de.dailab.jiactng.aot.auction.onto.InitializeBidder;
import de.dailab.jiactng.aot.auction.onto.InformBuy;
import de.dailab.jiactng.aot.auction.onto.InformSell;
import de.dailab.jiactng.aot.auction.server.AuctioneerMetaBean;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

/**
 * TODO Implement this class.
 * 
 * You might also decide to split the logic of your bidder up onto several
 * agent beans, e.g. one for each type of auction. In this case, remember
 * to keep the agent's `Wallet` in synch between the different roles, e.g.
 * using the agent's memory, as seen for the auctioneer beans.
 */
public class BidderBean extends AbstractAgentBean {

	enum Phase { STARTING, REGISTRATION, AUCTIONS, WALLETING, WAITING}
	//List of items in auction
	//private List<String> items = Arrays.asList(new String[]{"A", "B", "C", "D", "E", "F", "J", "K"});
	private String bidderID;
	private String groupToken;
	private String messageGroup;
	private Wallet wallet;
	private Phase phase = Phase.STARTING;
	private Integer auctioneer_A;
	private Integer auctioneer_B;
	private Integer auctioneer_C;
	private ICommunicationAddress c_addr;


	public void execute() {
		log.info("Messages in Memory: " + memory.readAll(new JiacMessage()).size());

		switch (phase) {
			case STARTING:
				// Join the message group
				invokeSimple(ICommunicationBean.ACTION_JOIN_GROUP, CommunicationAddressFactory.createGroupAddress(messageGroup));
				phase = Phase.REGISTRATION;
				break;

			case REGISTRATION:
				for (JiacMessage message : memory.removeAll(new JiacMessage(new StartAuctions(null, null)))) {
					System.out.println("Got message: " + message.getPayload());
					send(new Register(bidderID, groupToken), message.getSender());
					phase = Phase.WALLETING;
				}
				break;

			case WALLETING:
				for (JiacMessage message : memory.removeAll(new JiacMessage(new InitializeBidder(null, null)))) {
					System.out.println("Got message: " + message.getPayload());
					wallet = ((InitializeBidder) message.getPayload()).getWallet();
					System.out.println("Current wallet: " + wallet.getCredits());
				}
				phase = Phase.AUCTIONS;
				break;

			case AUCTIONS:
				for (JiacMessage message : memory.removeAll(new JiacMessage(new StartAuction(null, null, null, null)))) {
					System.out.println("Got message: " + message.getPayload());
					switch (((StartAuction) message.getPayload()).getMode()) {
						case A:
							auctioneer_A = ((StartAuction) message.getPayload()).getAuctioneerId();
						case B:
							auctioneer_B = ((StartAuction) message.getPayload()).getAuctioneerId();
						case C:
							auctioneer_C = ((StartAuction) message.getPayload()).getAuctioneerId();
							c_addr = message.getSender();
					}
				}

				for (JiacMessage message : memory.removeAll(new JiacMessage(new CallForBids(null, null, null, null, null, null)))) {
					System.out.println("Got message: " + message.getPayload());
					ProcessCall((CallForBids) message.getPayload(), message.getSender());
				}
				for (JiacMessage message : memory.removeAll(new JiacMessage(new InformBuy(null, null, null, null)))) {
					System.out.println("We got bundle: " + message.getPayload());
					InformBuy inform = (InformBuy) message.getPayload();
					if (inform.getType() == InformBuy.BuyType.WON) {
						wallet.updateCredits(-inform.getPrice());
						wallet.add(inform.getBundle());
						System.out.println("Remaining credits in wallet: " + wallet.getCredits());
					}
				}
				for (JiacMessage message : memory.removeAll(new JiacMessage(new InformSell(null, null, null, null)))) {
					System.out.println("We sold bundle: " + message.getPayload());
					InformSell inform = (InformSell) message.getPayload();
					if (inform.getType() == InformSell.SellType.SOLD) {
						wallet.updateCredits(inform.getPrice());
						wallet.remove(inform.getBundle());
						System.out.println("Remaining credits in wallet: " + wallet.getCredits());
					}
				}
				for (Resource item: Resource.values() ) {
					if (wallet.get(item) > 10) {
						System.out.println("Having more than 10 of " + item + ", offering to other agents");
						send(new Offer(auctioneer_C, bidderID, Arrays.asList(item), 10.0), c_addr);
					}
				}
				break;

		}
		log.info("Messages in Memory: " + memory.readAll(new JiacMessage()).size());



	}

	private void ProcessCall(CallForBids call, ICommunicationAddress address) {
		//System.out.println("Auctioneer is: " + call.getAuctioneerId() + " our registerd auctioneer A is " + auctioneer_A);
		if (call.getMode() == CallForBids.CfBMode.BUY) {
			Double offer = call.getMinOffer() + 1;
			List<Resource> bundle = call.getBundle();
			System.out.println("Bundle is: " + bundle + " were offering " + offer);
			if (wallet.getCredits() > offer) {
				send(new Bid(call.getAuctioneerId(), bidderID, call.getCallId(), offer), address);
			} else {
				System.out.println("Cant afford bundle");
			}
		} else if (call.getMode() == CallForBids.CfBMode.SELL) {

			if (wallet.contains(call.getBundle())) {
				System.out.println("Bundle is possible, selling");
				System.out.println("Items required for Bundle: " + call.getBundle());
				send(new Bid(call.getAuctioneerId(), bidderID, call.getCallId(), call.getMinOffer()), address);
			}
		}
	}
	/*
	 * GETTERS AND SETTERS
	 */

	public void setBidderId(String bidderId) {
		this.bidderID = bidderId;
	}
	public void setGroupToken(String groupToken) {
		this.groupToken = groupToken;
	}
	public void setMessageGroup(String messageGroup) {
		this.messageGroup = messageGroup;
	}


	// Helper functions from auctioneer
	private void invokeSimple(String actionName, Serializable... params) {
		invoke(retrieveAction(actionName), params);
	}

	/**
	 * send message to given communication address
	 */
	protected void send(IFact payload, ICommunicationAddress address) {
		JiacMessage message = new JiacMessage(payload);
		IActionDescription sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		invoke(sendAction, new Serializable[] {message, address});
	}
}

