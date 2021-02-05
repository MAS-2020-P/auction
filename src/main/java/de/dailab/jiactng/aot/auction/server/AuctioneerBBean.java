package de.dailab.jiactng.aot.auction.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.aot.auction.onto.Bid;
import de.dailab.jiactng.aot.auction.onto.CallForBids;
import de.dailab.jiactng.aot.auction.onto.CallForBids.CfBMode;
import de.dailab.jiactng.aot.auction.onto.InformSell;
import de.dailab.jiactng.aot.auction.onto.InformSell.SellType;
import de.dailab.jiactng.aot.auction.server.AuctionRunnerBean.Mode;
import de.dailab.jiactng.aot.auction.onto.Item;
import de.dailab.jiactng.aot.auction.onto.Resource;
import de.dailab.jiactng.aot.auction.onto.StartAuction;
import de.dailab.jiactng.aot.auction.onto.Wallet;

/**
 * This is the Auctioneer B. It will offer a fixed set of items and
 * will pay increasing amounts of money for those items. When an item
 * is bought, the demand for this item goes down and its price decreases.
 */
public class AuctioneerBBean extends AbstractAuctioneerBean {

	/** the current state of the auction */
	enum Phase { STARTING, BIDDING, EVALUATION, IDLE }
	
	/*
	 * CONFIGURATION
	 * those can be set in the Spring configuration file
	 */

	/** how much to increase the price each round */
	private double priceIncrease;
	
	/** how much to reduce the price when sold */
	private double priceDecrease;
	
	/** maximum random deviation of initial prices */
	private double maxInitPriceDeviation;
	
	/*
	 * STATE
	 */

	/** the mode of the current auction, i.e. fixed, public, etc. */
	private Mode mode;
	
	/** the items to be sold */
	private Map<Integer, Item> items;
	
	/** provider for unique call IDs */
	private AtomicInteger callIdProvider = new AtomicInteger(200000);
	
	/** the current phase */
	private Phase phase = Phase.IDLE;
	
	/*
	 * LIFECYCLE METHODS
	 */

	@Override
	public synchronized void execute() {
		if (phase == Phase.IDLE) return;
		log.info("Current Phase: " + phase);
		
		switch (phase) {
		case STARTING:
			this.items = initializeItems().stream().collect(Collectors.toMap(
					item -> callIdProvider.incrementAndGet(),
					item -> new Item(callIdProvider.get(), item.getBundle(), null, deviatePrice(item.getPrice()))));
			
			sendRegistered(new StartAuction(StartAuction.Mode.B, auctioneerId, items.size(), new ArrayList<>(items.values())));
			phase = Phase.BIDDING;
			break;
		
		case BIDDING:
			// update item prices
			items = items.values().stream().collect(Collectors.toMap(
					item -> callIdProvider.incrementAndGet(),
					item -> new Item(callIdProvider.get(), item.getBundle(), null, item.getPrice() + priceIncrease)));
			
			// send out new call-for-offers
			for (Item item : items.values()) {
				sendRegistered(new CallForBids(auctioneerId, item.getCallId(), item.getBundle(), item.getPrice(), CfBMode.SELL, item.getSeller()));
			}
			phase = Phase.EVALUATION;
			break;
			
		case EVALUATION:
			// get all messages from memory, inspect bids
			Map<Integer, List<Bid>> bids = items.keySet().stream()
					.collect(Collectors.toMap(i -> i, i -> new ArrayList<>()));

			for (JiacMessage message : memory.removeAll(new JiacMessage(new Bid(auctioneerId, null, null, null)))) {
				Bid bid = (Bid) message.getPayload();

				if (checkMessage(message, bid.getBidderId())) {

					if (items.containsKey(bid.getCallId())) {
						
						// XXX check wallet
						if (! getBidder(bid.getBidderId()).getWallet().contains(items.get(bid.getCallId()).getBundle())) {
							log.warn("Invalid Bid: Bidder does not have the resources.");
							send(new InformSell(SellType.INVALID, bid.getCallId(), items.get(bid.getCallId()).getBundle(), bid.getOffer()), bid.getBidderId());
							continue;
						}
						
						bids.get(bid.getCallId()).add(bid);

					} else {
						log.warn("Current known callIDs: " + items);
						log.warn("Invalid Bid: Unknown callId: " + bid.getCallId());
						send(new InformSell(SellType.INVALID, bid.getCallId(), null, bid.getOffer()), bid.getBidderId());
					}
				} else {
					log.warn("Discarding Bid with mismatched bidderID");
				}
			}
				
			// update prices, update wallets, send inform-sell messages
			for (Item item : new ArrayList<>(items.values())) {
				int numBids = bids.get(item.getCallId()).size();
				if (numBids > 0) {
					// calculate new price end effective selling price
					double newPrice = item.getPrice();
					double effectivePrice = 0;
					for (int i = 0; i < numBids; i++) {
						effectivePrice += newPrice;
						newPrice -= priceDecrease;
					}
					effectivePrice /= numBids;
					
					for (Bid bid : bids.get(item.getCallId())) {
						// update wallet
						Wallet wallet = getBidder(bid.getBidderId()).getWallet();
						synchronized (LOCK) {
							// XXX check wallet
							boolean hasBundle = wallet.contains(item.getBundle());
							if (hasBundle) {
								wallet.updateCredits(effectivePrice);
								wallet.remove(item.getBundle());
							}
							SellType type = hasBundle ? SellType.SOLD : SellType.INVALID;
							send(new InformSell(type, bid.getCallId(), item.getBundle(), effectivePrice), bid.getBidderId());
						}
					}
					
					// update item price
					items.put(item.getCallId(), new Item(item.getCallId(), item.getBundle(), null, newPrice));
				}
			}
			
			phase = Phase.BIDDING;
			break;
			
		case IDLE:
			break;
		}
	}
	
	private List<Item> initializeItems() {
		return Arrays.asList(
				new Item(0, Resource.parseBundle("AA"),       null,  200.),
				new Item(0, Resource.parseBundle("AAA"),      null,  300.),
				new Item(0, Resource.parseBundle("AAAA"),     null,  400.),
				new Item(0, Resource.parseBundle("AAB"),      null,  200.),
				new Item(0, Resource.parseBundle("AJK"),      null,  200.),
				new Item(0, Resource.parseBundle("BB"),       null,   50.),
				new Item(0, Resource.parseBundle("CCCDDD"),   null, 1200.),
				new Item(0, Resource.parseBundle("CCDDAA"),   null,  800.),
				new Item(0, Resource.parseBundle("CCDDBB"),   null,  600.),
				new Item(0, Resource.parseBundle("EEEEEF"),   null, 1600.),
				new Item(0, Resource.parseBundle("EEEEF"),    null,  800.),
				new Item(0, Resource.parseBundle("EEEF"),     null,  400.),
				new Item(0, Resource.parseBundle("EEF"),      null,  200.),
				new Item(0, Resource.parseBundle("FF"),       null,  100.),
				new Item(0, Resource.parseBundle("FJK"),      null,  300.),
				new Item(0, Resource.parseBundle("ABCDEFJK"), null, 1400.)
				);
	}
	
	private double deviatePrice(double price) {
		if (mode == Mode.FIXED) {
			return price;
		} else {
			double delta = (price * maxInitPriceDeviation);
			return price - delta + random.nextDouble() * 2 * delta;
		}
	}
	
	/*
	 * ACTIONS
	 */

	public static final String ACTION_START_AUCTION = "AuctioneerB#startAuction";
	public static final String ACTION_STOP_AUCTION  = "AuctioneerB#stopAuction";
	
	@Expose(name = ACTION_START_AUCTION, scope = ActionScope.NODE)
	public synchronized void startAuction(Mode mode, Long randomSeed) {
		this.phase = Phase.STARTING;
		this.mode = mode;
		this.random = new Random(randomSeed);
	}
	
	@Expose(name = ACTION_STOP_AUCTION, scope = ActionScope.NODE)
	public synchronized void stopAuction() {
		this.phase = Phase.IDLE;
	}
	
	/*
	 * GETTERS AND SETTERS
	 */

	public void setPriceIncrease(double priceIncrease) {
		this.priceIncrease = priceIncrease;
	}
	
	public void setPriceDecrease(double priceDecrease) {
		this.priceDecrease = priceDecrease;
	}
	
	public void setMaxInitPriceDeviation(double maxInitPriceDeviation) {
		this.maxInitPriceDeviation = maxInitPriceDeviation;
	}

}
