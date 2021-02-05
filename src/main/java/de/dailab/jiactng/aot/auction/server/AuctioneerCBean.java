package de.dailab.jiactng.aot.auction.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.aot.auction.onto.Bid;
import de.dailab.jiactng.aot.auction.onto.CallForBids;
import de.dailab.jiactng.aot.auction.onto.InformBuy;
import de.dailab.jiactng.aot.auction.onto.InformBuy.BuyType;
import de.dailab.jiactng.aot.auction.onto.InformSell;
import de.dailab.jiactng.aot.auction.onto.InformSell.SellType;
import de.dailab.jiactng.aot.auction.server.AuctionRunnerBean.Mode;
import de.dailab.jiactng.aot.auction.onto.Item;
import de.dailab.jiactng.aot.auction.onto.Offer;
import de.dailab.jiactng.aot.auction.onto.StartAuction;
import de.dailab.jiactng.aot.auction.onto.Wallet;
import de.dailab.jiactng.aot.auction.onto.CallForBids.CfBMode;

/**
 * This is the Auctioneer C. It will take offers from Bidders and advertise
 * those to other bidders, selling them to the first bidder bidding on that
 * item with a sufficiently high offer.
 */
public class AuctioneerCBean extends AbstractAuctioneerBean {

	/** the current state of the auction */
	enum Phase { STARTING, BIDDING, EVALUATION, IDLE }
	
	/*
	 * STATE
	 */
	
	/** the items to be sold; key is the call ID */
	private Map<Integer, Item> items;
	
	/** provider for unique call IDs */
	private AtomicInteger callIdProvider = new AtomicInteger(300000);
	
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
			sendRegistered(new StartAuction(StartAuction.Mode.C, auctioneerId, 0, null));
			items = new HashMap<>();
			phase = Phase.BIDDING;
			break;
		
		case BIDDING:
			items.clear();
			// only one offer per bidder
			Set<String> bidders = new HashSet<>();
			
			for (JiacMessage message1 : memory.removeAll(new JiacMessage(new Offer(auctioneerId, null, null, null)))) {
				Offer offer = (Offer) message1.getPayload();
			
				if (checkMessage(message1, offer.getBidderId())) {
					if (offer.getPrice() != null &&
							offer.getBundle() != null &&
							bidders.add(offer.getBidderId())) {
						
						// XXX check wallet
						if (! getBidder(offer.getBidderId()).getWallet().contains(offer.getBundle())) {
							log.warn("Invalid Offer: Seller does not have the resources.");
							send(new InformSell(SellType.INVALID, null, offer.getBundle(), offer.getPrice()), offer.getBidderId());
							continue;
						}
						
						Item item = new Item(callIdProvider.incrementAndGet(),
								offer.getBundle(),
								offer.getBidderId(),
								offer.getPrice());
						items.put(item.getCallId(), item);
						
						sendRegistered(new CallForBids(auctioneerId, item.getCallId(), item.getBundle(), item.getPrice(), CfBMode.BUY, item.getSeller()));
					} else {
						log.warn("Invalid offer: contains null values, or more than one offer from same bidder");
						send(new InformSell(SellType.INVALID, null, offer.getBundle(), offer.getPrice()), offer.getBidderId());
					}
				} else {
					log.warn("Discarding Offer with mismatched bidderID");
				}
			}
			phase = Phase.EVALUATION;
			break;
			
		case EVALUATION:
			// get all messages from memory, inspect bids
			Map<Integer, List<Bid>> bids = items.keySet().stream()
					.collect(Collectors.toMap(k -> k, k -> new ArrayList<>()));
			
			for (JiacMessage message2 : memory.removeAll(new JiacMessage(new Bid(auctioneerId, null, null, null)))) {
				Bid bid = (Bid) message2.getPayload();
				
				if (checkMessage(message2, bid.getBidderId())) {
					if (items.containsKey(bid.getCallId()) &&
							bid.getOffer() != null &&
							bid.getOffer() >= items.get(bid.getCallId()).getPrice()) {
						
						// XXX check wallet
						if (bid.getOffer() > getBidder(bid.getBidderId()).getWallet().getCredits()) {
							log.warn("Invalid Bid: Bidder does not have enough credits.");
							send(new InformBuy(BuyType.INVALID, bid.getCallId(), null, bid.getOffer()), bid.getBidderId());
							continue;
						}
						
						bids.get(bid.getCallId()).add(bid);

					} else {
						log.warn("Invalid Bid: Unknown callId or bid was null or too low");
						send(new InformBuy(BuyType.INVALID, bid.getCallId(), null, bid.getOffer()), bid.getBidderId());
					}
				} else {
					log.warn("Discarding Bid with mismatched bidderID");
				}
			}

			for (Integer id : items.keySet()) {
				Item item = items.get(id);
				List<Bid> sortedBids = bids.get(id).stream()
						.sorted(Comparator.comparing(Bid::getOffer).reversed()
								.thenComparing(b -> random.nextDouble())) // random for tie-breaks
						.collect(Collectors.toList());

				if (sortedBids.isEmpty()) {
					send(new InformSell(SellType.NOT_SOLD, item.getCallId(), item.getBundle(), null), item.getSeller());
				} else {
					for (int i = 0; i < sortedBids.size(); i++) {
						Bid bid = sortedBids.get(i);
						double price = sortedBids.get(0).getOffer();
						if (i == 0) {
							synchronized (LOCK) {
								Wallet seller = getBidder(item.getSeller()).getWallet();
								Wallet buyer = getBidder(bid.getBidderId()).getWallet();

								// XXX check wallet OF BOTH buyer and seller
								boolean valid = seller.contains(item.getBundle()) && buyer.getCredits() >= price;
								if (valid) {
									// update wallets
									seller.updateCredits(price);
									seller.remove(item.getBundle());
									
									buyer.updateCredits(-price);
									buyer.add(item.getBundle());
								}
								send(new InformSell(valid ? SellType.SOLD : SellType.INVALID, item.getCallId(), item.getBundle(), price), item.getSeller());
								send(new InformBuy(valid ? BuyType.WON : BuyType.INVALID, item.getCallId(), item.getBundle(), price), bid.getBidderId());
							}
						} else {
							send(new InformBuy(BuyType.LOST, item.getCallId(), item.getBundle(), price), bid.getBidderId());
						}
					}
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

	public static final String ACTION_START_AUCTION = "AuctioneerC#startAuction";
	public static final String ACTION_STOP_AUCTION  = "AuctioneerC#stopAuction";
	
	@Expose(name = ACTION_START_AUCTION, scope = ActionScope.NODE)
	public synchronized void startAuction(Mode mode, Long randomSeed) {
		this.phase = Phase.STARTING;
		this.random = new Random(randomSeed);
	}
	
	@Expose(name = ACTION_STOP_AUCTION, scope = ActionScope.NODE)
	public synchronized void stopAuction() {
		this.phase = Phase.IDLE;
	}
	
}
