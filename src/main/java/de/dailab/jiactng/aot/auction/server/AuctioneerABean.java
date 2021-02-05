package de.dailab.jiactng.aot.auction.server;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.aot.auction.onto.Bid;
import de.dailab.jiactng.aot.auction.onto.CallForBids;
import de.dailab.jiactng.aot.auction.onto.InformBuy;
import de.dailab.jiactng.aot.auction.onto.InformBuy.BuyType;
import de.dailab.jiactng.aot.auction.server.AuctionRunnerBean.Mode;
import de.dailab.jiactng.aot.auction.onto.Item;
import de.dailab.jiactng.aot.auction.onto.Resource;
import de.dailab.jiactng.aot.auction.onto.StartAuction;
import de.dailab.jiactng.aot.auction.onto.Wallet;
import de.dailab.jiactng.aot.auction.onto.CallForBids.CfBMode;

/**
 * This is the Auctioneer A. It is the most important of the auctioneers,
 * offering items from a predefined list of items, collecting bids and
 * finally awarding the item to the bidder with the highest offer, at the
 * price of the second highest offer.
 */
public class AuctioneerABean extends AbstractAuctioneerBean {

	/** the current state of the auction */
	enum Phase { STARTING, BIDDING, EVALUATION, IDLE }
	
	/*
	 * CONFIGURATION	 
	 * those can be set in the Spring configuration file
	 */

	/** string holding pre-set items for "fixed" mode, e.g. "AAA ABC EF" */
	private String presetItems;
	
	/** number of bundles for "random" modes */
	private Integer randomItemsBundleNum;
	
	/** minimum items per bundle in "random" modes */
	private Integer randomItemsMinBundleSize;
	
	/** maximum items per bundle in "random" modes */
	private Integer randomItemsMaxBundleSize;
	
	/** reservation price (minimum bid) for all bundles */
	private Double reservationPrice;
	
	/*
	 * STATE
	 */

	/** the mode of the current auction, i.e. fixed, public, etc. */
	private Mode mode;
	
	/** the items to be sold */
	private LinkedList<Item> items;
	
	/** the current item */
	private Item current;
	
	/** provider for unique call IDs */
	private AtomicInteger callIdProvider = new AtomicInteger(100000);
	
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
			List<List<Resource>> rawResources = mode == Mode.FIXED
					? Resource.parseBundleSequence(presetItems)
					: Resource.generateRandomBundles(randomItemsBundleNum, randomItemsMinBundleSize, randomItemsMaxBundleSize, random);
			items = new LinkedList<>(rawResources.stream()
					.map(bundle -> new Item(callIdProvider.incrementAndGet(), bundle, null, reservationPrice))
					.collect(Collectors.toList()));
			
			sendRegistered(new StartAuction(StartAuction.Mode.A, auctioneerId, items.size(), items));
			phase = Phase.BIDDING;
			break;
		
		case BIDDING:
			log.info("Items: " + items.stream().map(i -> i.getBundle()).collect(Collectors.toList()));

			// get next item to sell from stack
			if (! items.isEmpty()) {
				current = items.pop();

				// start new round, send request to message group
				sendRegistered(new CallForBids(auctioneerId, current.getCallId(), current.getBundle(), current.getPrice(), CfBMode.BUY, null));

				phase = Phase.EVALUATION;
			} else {
				log.info("No more items to sell, stopping.");
				phase = Phase.IDLE;
			}
			break;

		case EVALUATION:
			Map<String, Bid> bids = new HashMap<>(); 
			
			// get all messages from memory, inspect bids
			for (JiacMessage message : memory.removeAll(new JiacMessage(new Bid(auctioneerId, null, null, null)))) {
				Bid bid = (Bid) message.getPayload();

				if (checkMessage(message, bid.getBidderId())) {
					if (current.getCallId().equals(bid.getCallId()) && 
							bid.getOffer() != null &&
							bid.getOffer() >= current.getPrice()) {

						// XXX check wallet
						if (bid.getOffer() > getBidder(bid.getBidderId()).getWallet().getCredits()) {
							log.warn("Invalid Bid: Bidder does not have enough credits.");
							send(new InformBuy(BuyType.INVALID, bid.getCallId(), null, bid.getOffer()), bid.getBidderId());
							continue;
						}
						
						if (! bids.containsKey(bid.getBidderId()) || 
								bids.get(bid.getBidderId()).getOffer() < bid.getOffer()) {
							bids.put(bid.getBidderId(), bid);
						} else {
							log.info("Ignoring new lower bid " + bid);
						}
					} else {
						log.warn("Bid was: " + bid.getOffer() + " price is " + current.getPrice());
						log.warn("Invalid Bid: Bid was null or too low " + bid);
						send(new InformBuy(BuyType.INVALID, bid.getCallId(), null, null), bid.getBidderId());
					}
				} else {
					log.warn("Discarding Bid with mismatched bidderID " + bid);
				}
			}
			
			// sort bids by offer to determine winner
			List<Bid> sortedBids = bids.values().stream()
					.sorted(Comparator.comparing(Bid::getOffer).reversed()
							.thenComparing(b -> random.nextDouble())) // random for tie-breaks
					.collect(Collectors.toList());
			
			for (int i = 0; i < sortedBids.size(); i++) {
				Bid bid = sortedBids.get(i);
				double price = sortedBids.size() > 1 ? sortedBids.get(1).getOffer() : current.getPrice();
				if (i == 0) {
					// update wallet
					Wallet w = getBidder(bid.getBidderId()).getWallet();
					synchronized (LOCK) {
						// XXX check wallet
						boolean creditsOkay = w.getCredits() >= price;
						if (creditsOkay) {
							w.add(current.getBundle());
							w.updateCredits(-price);
						}
						// send out inform messages
						BuyType type = creditsOkay ? BuyType.WON : BuyType.INVALID;
						send(new InformBuy(type,  current.getCallId(), current.getBundle(), price), bid.getBidderId());
					}
				} else {
					send(new InformBuy(BuyType.LOST, current.getCallId(), current.getBundle(), price), bid.getBidderId());
				}
			}

			phase = Phase.BIDDING;
			break;
	
		case IDLE:
			break;
		}
	}
	
	/*
	 * ACTIONS
	 */

	public static final String ACTION_START_AUCTION = "AuctioneerA#startAuction";
	public static final String ACTION_IS_FINISHED   = "AuctioneerA#isFinished";
	
	@Expose(name = ACTION_START_AUCTION, scope = ActionScope.NODE)
	public synchronized void startAuction(Mode mode, Long randomSeed) {
		this.phase = Phase.STARTING;
		this.mode = mode;
		this.random = new Random(randomSeed);
	}
	
	@Expose(name = ACTION_IS_FINISHED, scope = ActionScope.NODE)
	public boolean isFinished() {
		return this.phase == Phase.IDLE;
	}
	

	/*
	 * GETTERS AND SETTERS
	 */

	public void setPresetItems(String presetItems) {
		this.presetItems = presetItems;
	}
	
	public void setRandomItemsBundleNum(Integer randomItemsBundleNum) {
		this.randomItemsBundleNum = randomItemsBundleNum;
	}
	
	public void setRandomItemsMaxBundleSize(Integer randomItemsMaxBundleSize) {
		this.randomItemsMaxBundleSize = randomItemsMaxBundleSize;
	}

	public void setRandomItemsMinBundleSize(Integer randomItemsMinBundleSize) {
		this.randomItemsMinBundleSize = randomItemsMinBundleSize;
	}
	
	public void setReservationPrice(Double reservationPrice) {
		this.reservationPrice = reservationPrice;
	}
}
