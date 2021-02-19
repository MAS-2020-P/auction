package de.dailab.jiactng.aot.auction.client;
import java.io.Serializable;
import java.util.*;

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

	static private class Bundle {
		public Bundle(List<Resource> parts, double value) {
			this.parts = parts;
			this.value = value;
			this.buildable = false;
		}
		List<Resource> parts;
		double value;
		boolean buildable;
		public String toString() {
			return "(" + parts.toString() + ": " + value + "" + buildable + ")";
		}
	}

	enum Phase { STARTING, STARTED}
	//List of items in auction
	//private List<String> items = Arrays.asList(new String[]{"A", "B", "C", "D", "E", "F", "J", "K"});
	private String bidderID;
	private String groupToken;
	private String messageGroup;
	private Wallet wallet;
	private Phase phase = Phase.STARTING;
	private Boolean outOfMoney = false;
	private Boolean peakReached = false;
	private Boolean noBundlesPossible = false;
	private Integer auctioneer_C;
	private ICommunicationAddress c_addr;
	private HashMap<Resource, Integer> ItemPrices;
	private List<Bundle> bundles;
	double initialAAvalue = 0;


	public void execute() {

		switch (phase) {
			case STARTING:
				// Join the message group
				invokeSimple(ICommunicationBean.ACTION_JOIN_GROUP, CommunicationAddressFactory.createGroupAddress(messageGroup));
				this.memory.attach(new MessageObserver(this), new JiacMessage());
				ItemPrices = new HashMap<>();
				ItemPrices.put(Resource.A, (int) 200);
				ItemPrices.put(Resource.B, (int) 50);
				ItemPrices.put(Resource.C, (int) 300);
				ItemPrices.put(Resource.D, (int) 300);
				ItemPrices.put(Resource.E, (int) 200);
				ItemPrices.put(Resource.F, (int) 100);
				ItemPrices.put(Resource.G, (int) 0);
				ItemPrices.put(Resource.J, (int) 200);
				ItemPrices.put(Resource.K, (int) 200);


				/*
				AA (200), AAA (300), AAAA (400), AAB (200), AJK (200),
				BB (50),
				CCCDDD (1200), CCDDAA (800), CCDDBB (600),
				EEEEEF (1600), EEEEF (800), EEEF (400), EEF (200),
				FF (100), FJK (300),
				ABCDEFJK (1400).

				 */



				bundles = new LinkedList<Bundle>() {{
						// 50
						add(new Bundle(Arrays.asList(Resource.B, Resource.B), 50));
						//100
						add(new Bundle(Arrays.asList(Resource.F, Resource.F), 100));
						//200
						add(new Bundle(Arrays.asList(Resource.A, Resource.A), 200));
						add(new Bundle(Arrays.asList(Resource.E, Resource.E, Resource.F), 200));
						add(new Bundle(Arrays.asList(Resource.A, Resource.A, Resource.B), 200));
						add(new Bundle(Arrays.asList(Resource.A, Resource.J, Resource.K), 200));
						//300
						add(new Bundle(Arrays.asList(Resource.F, Resource.J, Resource.K), 300));
						add(new Bundle(Arrays.asList(Resource.A, Resource.A, Resource.A), 300));
						//400
						add(new Bundle(Arrays.asList(Resource.E, Resource.E, Resource.E, Resource.F), 400));
						add(new Bundle(Arrays.asList(Resource.A, Resource.A, Resource.A, Resource.A), 400));
						//600
						add(new Bundle(Arrays.asList(Resource.C, Resource.C, Resource.D, Resource.D, Resource.B, Resource.B), 600));
						//800
						add(new Bundle(Arrays.asList(Resource.C, Resource.C, Resource.D, Resource.D, Resource.A, Resource.A), 800));
						add(new Bundle(Arrays.asList(Resource.E, Resource.E, Resource.E, Resource.E, Resource.F), 800));
						//1200
						add(new Bundle(Arrays.asList(Resource.C, Resource.C, Resource.C, Resource.D, Resource.D, Resource.D), 1200));
						//1400
						add(new Bundle(Arrays.asList(Resource.A, Resource.B, Resource.C, Resource.D, Resource.E, Resource.F, Resource.J, Resource.K), 1400));
						//1600
						add(new Bundle(Arrays.asList(Resource.E, Resource.E, Resource.E, Resource.E, Resource.E, Resource.F), 1600));

					}};

				Collections.reverse(bundles);

				break;

			case STARTED:

				noBundlesPossible = true;
				for (Bundle b: bundles) {
					if(wallet.contains(b.parts)) {
						b.buildable = true;
						noBundlesPossible = false;
					} else {
						b.buildable = false;
					}
				}
				if(noBundlesPossible && outOfMoney && peakReached) {
					for (Resource item: Resource.values() ) {
						if (wallet.get(item) > 0) {
							System.out.println("Having more than 10 of " + item + ", offering to other agents");
							send(new Offer(auctioneer_C, bidderID, Arrays.asList(item), (double) ItemPrices.get(item)), c_addr);
							break;
						}

					}
				}
				/*
				for (Resource item: Resource.values() ) {
					if (wallet.get(item) > 10) {
						System.out.println("Having more than 10 of " + item + ", offering to other agents");
						send(new Offer(auctioneer_C, bidderID, Arrays.asList(item), 10.0), c_addr);
					}
				} */
				break;

		}


	}

	public void ProcessMessage(JiacMessage message) {
		if (message.getPayload() instanceof StartAuctions) {
			System.out.println("Got message: " + message.getPayload());
			send(new Register(bidderID, groupToken), message.getSender());
		} else if(message.getPayload() instanceof InitializeBidder) {
			System.out.println("Got message: " + message.getPayload());
			wallet = ((InitializeBidder) message.getPayload()).getWallet();
			phase = Phase.STARTED;
		} else if (message.getPayload() instanceof StartAuction) {
			System.out.println("Got message: " + message.getPayload());
			switch (((StartAuction) message.getPayload()).getMode()) {
				case C:
					auctioneer_C = ((StartAuction) message.getPayload()).getAuctioneerId();
					c_addr = message.getSender();
			}
		} else if(message.getPayload() instanceof CallForBids) {
			System.out.println("Got message: " + message.getPayload());
			ProcessCall((CallForBids) message.getPayload(), message.getSender());
		} else if(message.getPayload() instanceof InformBuy) {
			System.out.println("We got bundle: " + message.getPayload());
			InformBuy inform = (InformBuy) message.getPayload();
			if (inform.getType() == InformBuy.BuyType.WON) {
				wallet.updateCredits(-inform.getPrice());
				wallet.add(inform.getBundle());
				System.out.println("Remaining credits in wallet: " + wallet.getCredits());
			}
		} else if(message.getPayload() instanceof InformSell) {
			System.out.println("We sold bundle: " + message.getPayload());
			InformSell inform = (InformSell) message.getPayload();
			if (inform.getType() == InformSell.SellType.SOLD) {
				wallet.updateCredits(inform.getPrice());
				wallet.remove(inform.getBundle());
				System.out.println("Remaining credits in wallet: " + wallet.getCredits());
			}
		}
	}
	public void RemoveMsg(JiacMessage message) {
		memory.remove(message);
	}
	private void ProcessCall(CallForBids call, ICommunicationAddress address) {
		//System.out.println("Auctioneer is: " + call.getAuctioneerId() + " our registerd auctioneer A is " + auctioneer_A);
		if (call.getMode() == CallForBids.CfBMode.BUY) {
			Double offer = 0.0;
			List<Resource> bundle = call.getBundle();
			for (Resource item: bundle) {
				offer += ItemPrices.get(item);
			}
			System.out.println("Bundle is: " + bundle + " were offering " + offer);
			if (wallet.getCredits() > offer) {
				send(new Bid(call.getAuctioneerId(), bidderID, call.getCallId(), offer), address);
			} else {
				outOfMoney = true;
				System.out.println("Cant afford bundle");
			}
		} else if (call.getMode() == CallForBids.CfBMode.SELL) {
			if(!peakReached) {
				if (call.getBundle().equals(new LinkedList<>(Arrays.asList(Resource.A, Resource.A)))) {
					if(initialAAvalue == 0) {
						initialAAvalue = call.getMinOffer();
					} else if (call.getMinOffer() >= initialAAvalue + 500) {
						peakReached = true;
					}
				}
				for (Bundle b: bundles) {
					if (b.parts.equals(call.getBundle())) {
						if( b.value < call.getMinOffer()) {
							b.value = call.getMinOffer();
							System.out.println("Updated price: " + b.value + " for bundle " + b.parts);
						} else {
							peakReached = true;
						}
						break;
					}
				}
			}
			else {
				// TODO: add out of money check
				if(wallet.contains(call.getBundle()) && outOfMoney) {
					Boolean betterBundle = true;
					for (Bundle b : bundles) {
						if (b.parts.equals(call.getBundle())) {
							betterBundle = false;
							break;
						}
						if (b.buildable) {
							break;
						}
					}
					if (betterBundle == false) {
						System.out.println("Bundle is possible, selling");
						System.out.println("Items required for Bundle: " + call.getBundle());
						send(new Bid(call.getAuctioneerId(), bidderID, call.getCallId(), call.getMinOffer()), address);
					}
				}
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

class MessageObserver implements SpaceObserver<IFact> {

	private BidderBean agent;

	public MessageObserver(BidderBean bidderBean) {
		this.agent = bidderBean;
	}

	public void notify(SpaceEvent<? extends IFact> event) {
		if (event instanceof WriteCallEvent) {
			JiacMessage message = (JiacMessage) ((WriteCallEvent) event).getObject();
			agent.RemoveMsg(message);
			agent.ProcessMessage(message);
		}
	}
}
