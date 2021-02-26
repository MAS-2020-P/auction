package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.CommunicationAddressFactory;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.IGroupAddress;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.aot.auction.onto.*;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * TODO Implement this class.
 *
 * You might also decide to split the logic of your bidder up onto several
 * agent beans, e.g. one for each type of auction. In this case, remember
 * to keep the agent's `Wallet` in synch between the different roles, e.g.
 * using the agent's memory, as seen for the auctioneer beans.
 */
public class RBidderBean extends AbstractAgentBean {

	/*
	 * TODO
	 * add properties for e.g. the multicast message group, or the bidderID
	 * add getter methods for those properties so they can be set in the
	 * Spring configuration file
	 */
	private int MAX_RESOURCES_A  = 10;

	private String bidderId;
	private String groupToken;
	private String messageGroup;
	private IGroupAddress group;

	private Wallet wallet;
	private double initial_credits;
	private int auctionC_id;
	private  ICommunicationAddress auctioneer_C;
	private HashMap<List<Resource>,Double> collectionPriceList;
	private HashMap<Resource, Double> ressourcePriceList;
	//private BestOffer bestOfferB;
	private int offersOnA,offersOnB;
	private int gameTurn;
	private boolean offSync;

	@Override
	public void doStart() throws Exception {
		memory.attach(new MessageObserver(), new JiacMessage());

		// gruppe beitreten
		group = CommunicationAddressFactory.createGroupAddress(messageGroup);
		Action joinAction = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
		invoke(joinAction, new Serializable[]{group});

		//estimated values for each item
		collectionPriceList = new HashMap<>();
		ressourcePriceList = new HashMap<>();
		ressourcePriceList.put(Resource.A, (double)100);
		ressourcePriceList.put(Resource.B, (double) 25);
		ressourcePriceList.put(Resource.C, (double)200);
		ressourcePriceList.put(Resource.D, (double)200);
		ressourcePriceList.put(Resource.E, (double) 75);
		ressourcePriceList.put(Resource.F, (double) 50);
		ressourcePriceList.put(Resource.G, (double)-20);
		ressourcePriceList.put(Resource.J, (double) 60);
		ressourcePriceList.put(Resource.K, (double) 60);

		//most value giving collection to sell on auction b
		//bestOfferB = new BestOffer();
		gameTurn = 1;
		offSync = false;
	}

	@Override
	public void execute() {
		if(wallet == null) return;
		//log("################################################################################################");

		gameTurn++;

		// wait for incoming messages
		long start = System.currentTimeMillis();
		while(System.currentTimeMillis() - start < 500){
			;
		}
		if(offSync) {
			start = System.currentTimeMillis();
			while(System.currentTimeMillis() - start < 200){
				;
			}
			offSync = false;
		}


		// offer item A for auction c
		if((wallet.get(Resource.A) > MAX_RESOURCES_A)) {
			List<Resource> bundle_to_offer = Arrays.asList(
					Resource.A);
			Offer offer = new Offer(auctionC_id, bidderId, bundle_to_offer, calculateResourceValue(Resource.A));
			sendMessage(auctioneer_C, offer);
			//log("Offer A for sell: Auction C");
		}
		// offer item B for auction c
		if((wallet.get(Resource.B) > MAX_RESOURCES_A)) {
			List<Resource> bundle_to_offer = Arrays.asList(
					Resource.B);
			Offer offer = new Offer(auctionC_id, bidderId, bundle_to_offer, calculateResourceValue(Resource.B));
			sendMessage(auctioneer_C, offer);
			//log("Offer B for sell: Auction C");
		}

	}

	private void handleMessage(JiacMessage message) {
		Object payload = message.getPayload();

		if(payload instanceof StartAuctions){
			register(message.getSender());
		}

		if(payload instanceof StartAuction){
			StartAuction saMessage = (StartAuction) payload;
			handleStartAuction(saMessage, message.getSender());
		}

		if(payload instanceof InitializeBidder) {
			handleInitializeBidder((InitializeBidder) payload);
		}

		if(payload instanceof CallForBids){
			CallForBids cfbMessage = (CallForBids) payload;
			if(cfbMessage.getMode() == CallForBids.CfBMode.BUY) {
				if(cfbMessage.getOfferingBidder() == null) {
					offersOnA++;
					handleAuctionA(cfbMessage, message.getSender());
				}
				else {
					handleAuctionC(cfbMessage, message.getSender());
				}

			}
			else {
				handleAuctionB(cfbMessage, message.getSender());
			}

		}
		if(payload instanceof InformBuy) {
			handleInformBuy((InformBuy) payload, message.getSender());
		}
		if(payload instanceof InformSell) {
			handleInformSell((InformSell)payload, message.getSender());
		}
		if(payload instanceof EndAuction) {
			handleEndAuction((EndAuction)payload);
		}



	}

	private void handleInitializeBidder(InitializeBidder message) {
		if(message.getBidderId().equals(this.bidderId)) {
			this.wallet = message.getWallet();
			this.initial_credits = this.wallet.getCredits();
		}
	}

	private void handleAuctionA(CallForBids message, ICommunicationAddress sender) {
		double value = calculateBundleValue(message.getBundle());

		// if not in buying mode dont reply
		if(wallet.getCredits() < initial_credits/2){
			//log("Didn't bid for: " + message.toString()  + " because wallet has half credits left - MARKET A");
			return;
		}

		// if less then MAX_RESOURCES ressources available and value positive -> bid
		Bid bid = new Bid(message.getAuctioneerId(), bidderId, message.getCallId(), value);
		sendMessage(sender, bid);
		//log("Bid for: " + message.toString() + " with value: " + value  + " - AUCTION A");
	}


	private void handleAuctionB(CallForBids message, ICommunicationAddress sender) {
		offersOnB++;
		// wallet contains items
		if(wallet.contains(message.getBundle())){
			//calculate value gain
			double valueGain = message.getMinOffer();
			for(Resource res: message.getBundle()) {
				valueGain -= ressourcePriceList.get(res);
			}

			// sell if value gain >= 5 euros
			if(valueGain >= 5){
				sellCollection(message, sender);
				//log("Bid for: " + message.toString() + "with gain: " + valueGain + " - AUCTION B");
			}

		}
	}

	private void handleAuctionC(CallForBids message, ICommunicationAddress sender) {
		//calculate the value for the whole bundle
		double value = calculateBundleValue(message.getBundle());
		List<Resource> resrc = message.getBundle();

		// if not in buying mode dont reply
		if(resrc.contains(Resource.E) || resrc.contains(Resource.F)){

			//bid for bundle
			Bid myBid = new Bid(message.getAuctioneerId(),this.bidderId,message.getCallId(),value);
			sendMessage(sender,myBid);
			//log("Bid for: " + message.toString() + " with value: " + value  + " - Auction C");

		}
		else{
			//log("Didn't bid for: " + message.toString()  + " because wallet is still filled - MARKET C");

		}
		return;



	}

	private void handleStartAuction(StartAuction message, ICommunicationAddress sender) {

		// get auctioneer id for auction C
		if(message.getMode() == StartAuction.Mode.C){
			this.auctionC_id = message.getAuctioneerId();
			this.auctioneer_C = sender;
		}
	}

	//returns value of given bundle
	private double calculateBundleValue(List<Resource> bundle) {
		double value = 0;
		for(Resource res: bundle) {
			value += calculateResourceValue(res);
		}
		return value;
	}

	//returns value of given resource
	private double calculateResourceValue(Resource resource) {
		return ressourcePriceList.get(resource);
	}

	private void sellCollection(CallForBids cfbMessage, ICommunicationAddress sender) {
		//log("##########_Selling: " + cfbMessage.getCallId());
		Bid myBid = new Bid(cfbMessage.getAuctioneerId(),this.bidderId,cfbMessage.getCallId(),cfbMessage.getMinOffer());
		sendMessage(sender,myBid);
	}

	private class MessageObserver implements SpaceObserver<IFact> {
		@Override
		public void notify(SpaceEvent<? extends IFact> event) {
			if (event instanceof WriteCallEvent) {
				WriteCallEvent writeEvent = (WriteCallEvent) event;
				if (writeEvent.getObject() instanceof JiacMessage) {
					JiacMessage message = (JiacMessage) writeEvent.getObject();
					handleMessage(message);
					memory.remove(message);
				}
			}
		}
	}

	private void handleInformSell(InformSell payload, ICommunicationAddress sender) {
		//log("SELL MESSAGE " + payload.toString());
		if(payload.getType().equals(InformSell.SellType.SOLD)) {
			//sold collection: now remove resources from wallet add credits to wallet
			if(wallet.contains(payload.getBundle())) {
				wallet.remove(payload.getBundle());
				wallet.updateCredits(payload.getPrice());
				//log("sold: " + payload.getBundle() + " for: " + payload.getPrice());
			} else {
				return;
			}
		} else if(payload.getType().equals(InformSell.SellType.NOT_SOLD)) {

		} else if(payload.getType().equals(InformSell.SellType.INVALID)) {

		}
	}

	private void handleInformBuy(InformBuy payload, ICommunicationAddress sender) {
		if(payload.getType().equals(InformBuy.BuyType.WON)) {
			//won auction a: now add resources to wallet and remove credits from wallet
			wallet.add(payload.getBundle());
			wallet.updateCredits(payload.getPrice()*(-1));
			//log("bought: " + payload.getBundle() + " for: " + payload.getPrice());
		} else if(payload.getType().equals(InformBuy.BuyType.LOST)){

		} else if(payload.getType().equals(InformBuy.BuyType.INVALID)) {

			offSync = true;
		}
	}
	private void handleEndAuction(EndAuction message) {
		//log("Winner is: " + message.getWinner() + " he got: " + message.getWallet().getCredits() + "$");
	}


	private void register(ICommunicationAddress receiver){
		Register message = new Register(bidderId, groupToken);
		sendMessage(receiver, message);
	}

	private void log(String s) {
		log.info(gameTurn + ": " + s);
	}

	private void sendMessage (ICommunicationAddress receiver, IFact payload){
		Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		JiacMessage message = new JiacMessage(payload);
		invoke(sendAction, new Serializable[]{message, receiver});
	}


	public String getBidderId() {
		return bidderId;
	}

	public void setBidderId(String bidderId) {
		this.bidderId = bidderId;
	}

	public void setMessageGroup(String messGroup) {
		this.messageGroup = messGroup;
	}
	public String getMessageGroup() {
		return this.messageGroup;
	}
	public String getGroupToken() {
		return groupToken;
	}
	public void setGroupToken(String groupToken) {
		this.groupToken = groupToken;
	}


}
