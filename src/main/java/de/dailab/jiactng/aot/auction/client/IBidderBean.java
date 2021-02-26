package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.action.ActionResult;
import de.dailab.jiactng.agentcore.comm.CommunicationAddressFactory;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.IGroupAddress;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.environment.ResultReceiver;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.aot.auction.onto.*;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import java.io.Serializable;
import java.util.*;


/**
 * TODO Implement this class.
 * <p>
 * You might also decide to split the logic of your bidder up onto several
 * agent beans, e.g. one for each type of auction. In this case, remember
 * to keep the agent's `Wallet` in synch between the different roles, e.g.
 * using the agent's memory, as seen for the auctioneer beans.
 */
public class IBidderBean extends AbstractAgentBean implements ResultReceiver {

    private Action sendAction = null;

    private Wallet wallet;

    private Wallet finalWallet;

    private Repository repo = new Repository();

    private OfferHelperA offerHelperA = new OfferHelperA();

    private OfferHelperB offerHelperB = new OfferHelperB();

    private OfferHelperC offerHelperC = new OfferHelperC();

    private SellingHelper sellingHelper = new SellingHelper();

    private IGroupAddress groupAddress;


    /*
     * TODO
     * add properties for e.g. the multicast message group, or the bidderID
     * add getter methods for those properties so they can be set in the
     * Spring configuration file
     */
    private String bidderId;

    private String groupToken;

    private String messageGroup;

    /*
     * TODO
     * when the agent starts,
     *
     * use the action ICommunicationBean.ACTION_JOIN_GROUP
     * to join the multicast message group "de.dailab.jiactng.aot.auction"
     * for the final competition, or a group of your choosing for testing
     * make sure to use the same message group as the auctioneer!
     *
     * create a message observer and attach it to the
     * agent's memory. that message observer should then handle the different
     * messages and send a suitable Bid in reply. see the readme and the
     * sequence diagram for the expected order of messages.
     */

    /**
     * This is called once when the agent starts. Here, we attach an observer
     * to the agent's memory, listening for incoming JIAC messages.
     */
    @Override
    public void doStart() throws Exception {
        super.doStart();
        //log.info("Bidder - starting....");
        //log.info("Bidder - my ID: " + this.thisAgent.getAgentId());
        //log.info("Bidder - my Name: " + this.thisAgent.getAgentName());
        //log.info("Bidder - my Node: " + this.thisAgent.getAgentNode().getName());

        groupAddress = CommunicationAddressFactory.createGroupAddress(messageGroup);
        // Retrieve send action provided by CommunicationBean
        sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
        Action joinGroup = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);

        // Join Group
        invoke(joinGroup, new Serializable[]{groupAddress});
        //log.info("joinGroup");

        // TODO attach your memory observer to the agent's memory, subscribe for JiacMessage
        memory.attach(new MessageObserver(), new JiacMessage());
        //log.info("memory Attach");
    }

    class MessageObserver implements SpaceObserver<IFact> {

        @Override
        public void notify(SpaceEvent<? extends IFact> event) {
            // check the type of the event
            //System.out.println("MessageObserver notify ...");
            if (event instanceof WriteCallEvent) {
                // we know it's a message due to the template, but we have to check the content
                JiacMessage message = (JiacMessage) ((WriteCallEvent) event).getObject();

                if (message.getPayload() instanceof StartAuctions) {
                    handleStartAuctions(message);
                }
                if (message.getPayload() instanceof StartAuction) {
                    handleStartAuction(message);
                }
                if (message.getPayload() instanceof CallForBids) {
                    handleCallForBids(message);
                }
                if (message.getPayload() instanceof InformBuy) {
                    handleInformBuy(message);
                }
                if (message.getPayload() instanceof InformSell) {
                    handleInformSell(message);
                }

                if (message.getPayload() instanceof InitializeBidder) {
                    handleInitialize((InitializeBidder) message.getPayload());
                }

                if (message.getPayload() instanceof EndAuction) {
                    handleEndAuction((EndAuction) message.getPayload());
                }
                memory.remove(message);
            }
        }
    }//[message observer]

    private void handleEndAuction(EndAuction message) {
        finalWallet = message.getWallet();
    }

    private void handleInitialize(InitializeBidder message) {
        wallet = message.getWallet();
    }

    private void handleCallForBids(JiacMessage message) {

        CallForBids payload = (CallForBids) message.getPayload();

        // save the call for bid for later processing
        repo.addCallForBid(payload);

    }

    private void handleInformBuy(JiacMessage message) {

        InformBuy payload = (InformBuy) message.getPayload();

        repo.removeCallForBid(payload);
        try {
            updateWallet(payload);
        } catch (Exception ignored) {

        }
    }

    private void handleInformSell(JiacMessage message) {

        InformSell payload = (InformSell) message.getPayload();
        try {
            repo.removeCallForBid(payload);

        } catch (Exception ignored) {

        }
        updateWallet(payload);
    }


    private void handleStartAuctions(JiacMessage message) {
        // register to the auctions
        Register register = new Register(bidderId, groupToken);
        JiacMessage content = new JiacMessage(register);
        invoke(sendAction, new Serializable[]{content, message.getSender()});
    }

    private void handleStartAuction(JiacMessage message) {
        StartAuction payload = (StartAuction) message.getPayload();
        repo.updateAddressBook(payload.getAuctioneerId(), payload.getMode(), message.getSender());

        //System.out.println(payload.getAuctioneerId().toString() + "  " + message.getSender());

        if(StartAuction.Mode.A == payload.getMode())
        {
            if(payload.getInitialItems()!=null)
            {
                repo.setAuctionInitialItems( Repository.AuctionType.A, payload.getInitialItems());
            }
        }

    }


    /*
     * TODO You will receive your initial "Wallet" from the auctioneer, but
     * afterwards you will have to keep track of your spendings and acquisitions
     * yourself. The Auctioneer will do so, as well.
     */

    private void updateWallet(InformSell message) {
        if (InformSell.SellType.SOLD == message.getType()) {
            wallet.remove(message.getBundle());
            //add the amount to the credits
            wallet.updateCredits(message.getPrice());
        }

    }

    /**
     * TODO
     * if the item is NOT_SOLD update the logic to lower down the selling price
     *
     * @param message
     */
    private void updateWallet(InformBuy message) throws Exception{
        if (InformBuy.BuyType.WON == message.getType()) {
            wallet.add(message.getBundle());

            double price = repo.removeBid(message.getCallId());
            //subtract the amount to the credits
            //not working wallet.updateCredits(-message.getPrice());
            wallet.updateCredits(-price);
        }

    }


    private void sendBids(CallForBids call) throws Exception
    {
        double offer = 0;

        if (CallForBids.CfBMode.BUY == call.getMode()) {
            offer = offerHelperA.generateOffer(call, wallet, repo);
        }
        if (CallForBids.CfBMode.SELL == call.getMode()) {
            offer = offerHelperB.generateOffer(call, wallet, repo);
        }
        if ((CallForBids.CfBMode.BUY == call.getMode()) && call.getOfferingBidder() != null) {
            offer = offerHelperC.generateOffer(call, wallet, repo);
        }

        //no bid will be sent
        if (offer == -1)
            return;

        Bid bid = new Bid(call.getAuctioneerId(), this.bidderId, call.getCallId(), offer);

        repo.addBid(bid);

        JiacMessage message = new JiacMessage(bid);

        invoke(sendAction, new Serializable[]{message, repo.getAddress(call.getAuctioneerId())});
    }


    // TODO logic to sell items
    private void sellItem(int auctionerId) {

        List<Resource> bundle = sellingHelper.getItemToSell(wallet, repo);

        if(null==bundle)
            return;

        if(sellingHelper.getPrice(bundle, repo) <= 0)
            return;

        Offer offer = new Offer(auctionerId,bidderId, bundle, sellingHelper.getPrice(bundle, repo));

        JiacMessage message = new JiacMessage(offer);

        invoke(sendAction, new Serializable[]{message, repo.getAddress(auctionerId)});
    }


    @Override
    public void execute() {

        // generate bids
        for (CallForBids call : repo.getCallForBid()) {
                try {

                    sendBids(call);
                } catch (Exception ignored) {

                }
        }

        //TODO logic to sell items
        if(repo.isAuctionCrunning())
        {
            sellItem(repo.getAuctionerId(StartAuction.Mode.C));
        }
    }

    @Override
    public void receiveResult(ActionResult result) {
        String resultActionName = result.getAction().getName();
        if (ICommunicationBean.ACTION_JOIN_GROUP.equals(resultActionName)) {
            if (result.getFailure() != null) {
                this.log.error("could not join chat group address: "
                        + result.getFailure());
            }
        }
        if (ICommunicationBean.ACTION_SEND.equals(resultActionName)) {
            if (result.getFailure() != null) {
                this.log.error("could not send msg " + result.getFailure());
            }
        }
    }

    /*
     * GETTERS AND SETTERS
     * needed for setting properties via Spring configuration file
     */
    public void setMessageGroup(String messageGroup) {
        this.messageGroup = messageGroup;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public void setGroupToken(String groupToken) {
        this.groupToken = groupToken;
    }

    public String getBidderId() {
        return bidderId;
    }

    public String getGroupToken() {
        return groupToken;
    }

    public String getMessageGroup() {
        return messageGroup;
    }

    private static class Repository {

        LinkedList<StartAuction> auctionInfos;
        HashMap<Integer, CallForBids> callForBids;
        AuctionData auctionA;
        AuctionData auctionB;
        AuctionData auctionC;

        HashMap<Integer, Bid> bids;
        Collection<ItemValue> priceList;

        public Repository()
        {
            auctionInfos = new LinkedList<>();
            callForBids = new HashMap<>();
            bids = new HashMap<>();
            priceList = new ArrayList<>();
            createPricingList();

        }

        public synchronized void addCallForBid(CallForBids c)
        {
            callForBids.put(c.getCallId(), c);
        }

        public synchronized Collection<CallForBids> getCallForBid()
        {
            return callForBids.values();
        }

        public synchronized void removeCallForBid(InformSell message) throws Exception
        {
            int callId = message.getCallId();

            callForBids.remove(callId);

        }

        public synchronized void removeCallForBid(InformBuy message)
        {
            int callId = message.getCallId();

            callForBids.remove(callId);
        }

        public void updateAddressBook(Integer auctioneer,
                                      StartAuction.Mode mode,
                                      ICommunicationAddress address)
        {
            switch (mode)
            {
                case A:
                    auctionA = new AuctionData(auctioneer, mode, address);
                    break;
                case B:
                    auctionB = new AuctionData(auctioneer, mode, address);
                    break;
                case C:
                    auctionC = new AuctionData(auctioneer, mode, address);
                    break;
            }
        }

        public ICommunicationAddress getAddress(Integer auctioneer)
        {
            if(auctioneer == auctionA.id)
                return auctionA.address;

            if(auctioneer == auctionB.id)
                return auctionB.address;

            return auctionC.address;
        }

        public ICommunicationAddress getAddress(StartAuction.Mode mode)
        {
            if(mode == auctionA.mode)
                return auctionA.address;

            if(mode == auctionB.mode)
                return auctionB.address;

            return auctionC.address;
        }

        public int getAuctionerId(StartAuction.Mode mode)
        {
            if(auctionA!=null && mode == auctionA.mode)
                return auctionA.id;

            if(auctionB!=null && mode == auctionB.mode)
                return auctionB.id;

            if(auctionC!=null && mode == auctionC.mode)
                return auctionC.id;

            return -1;
        }


        public void setAuctionInitialItems(AuctionType type, Collection<Item> items)
        {
            switch ( type )
            {
                case A:
                    auctionA.items = items;
                    break;
                case B:
                    auctionB.items = items;
                    break;
                case C:
                    break;
            }

        }

        public synchronized void addBid(Bid bid) {
            this.bids.put(bid.getCallId(), bid);
        }

        public synchronized double removeBid(int callId) {

            Bid ret = this.bids.remove(callId);
            return ret.getOffer();
        }


        /**
         * AA (200), AAA (300), AAAA (400), AAB (200), AJK (200),
         * BB (50),
         * CCCDDD (1200), CCDDAA (800), CCDDBB (600),
         * EEEEEF (1600), EEEEF (800), EEEF (400), EEF (200),
         * FF (100), FJK (300),
         * ABCDEFJK (1400).
         */
        private void createPricingList()
        {
            priceList.add(new ItemValue(
                    Arrays.asList(Resource.A),
                    100
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.A, Resource.A),
                    200
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.A, Resource.A, Resource.A),
                    300
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.A, Resource.A, Resource.A, Resource.A),
                    400
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.A, Resource.A,  Resource.B),
                    200
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.A, Resource.J, Resource.K),
                    200
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.B),
                    25
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.B, Resource.B),
                    50
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.C, Resource.C, Resource.C, Resource.D, Resource.D, Resource.D),
                    1200
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.C, Resource.C, Resource.D, Resource.D, Resource.A, Resource.A),
                    800
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.C, Resource.C, Resource.D, Resource.D, Resource.B, Resource.B),
                    600
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.E),
                    50
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.E, Resource.E, Resource.E, Resource.E, Resource.E, Resource.F),
                    1600
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.E, Resource.E, Resource.E, Resource.E, Resource.F),
                    800
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.E, Resource.E, Resource.E, Resource.F),
                    400
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.E, Resource.E, Resource.F),
                    200
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.F),
                    100
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.F, Resource.F),
                    100
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.F, Resource.J, Resource.K),
                    300
            ));

            priceList.add(new ItemValue(
                    Arrays.asList(Resource.A, Resource.B,Resource.C, Resource.D,Resource.E, Resource.F, Resource.J, Resource.K),
                    1400
            ));


            priceList.add(new ItemValue(
                    Arrays.asList(Resource.G),
                    80
            ));

        }

        public double getBundleValue(List<Resource> bundle)
        {
            for(ItemValue itemValue : priceList)
            {
                if( (new HashSet<>(bundle)).equals(new HashSet<>(itemValue.bundle)))
                {
                    return itemValue.price;
                }
            }

            return -1;
        }

        public Boolean isAuctionCrunning(){

            return auctionC != null;
        }

        private class ItemValue{
            List<Resource> bundle;
            double price;

            public ItemValue(List<Resource> bundle, double price) {
                this.bundle = bundle;
                this.price = price;
            }
        }

        private class AuctionData
        {
            int id;
            StartAuction.Mode mode;
            ICommunicationAddress address;
            Collection<Item> items;

            AuctionData(int id, StartAuction.Mode mode, ICommunicationAddress address)
            {
                this.id = id;
                this.address = address;
                this.mode = mode;
            }

            AuctionData(){ this.id = -1;}

        }

        public enum AuctionType{A, B , C}

    }//[c]

    private class SellingHelper {

        /**
         * agent will place a selling order for an item, either G or one chosen randomly among A and B,
         * and consider the reservation price as two thirds of it’s predetermined value.
         * @param wallet
         * @param repo
         * @return null if no there are no resource to sell
         */
        public List<Resource> getItemToSell(Wallet wallet, Repository repo)
        {
            List<Resource> bundle = getRes(wallet);

            return bundle;
        }


        private List<Resource> getRes(Wallet wallet)
        {
            List<Resource> res = new ArrayList<>();

            res.add(Resource.G);

            if(wallet.contains(res))
            {
                return res;
            }

            res.clear();

            Random random = new Random();

            if( random.nextInt(2) % 2 == 0)
            {
                res.add(Resource.A);
            }
            else
            {
                res.add(Resource.B);
            }

            if(wallet.contains(res))
            {
                return res;
            }

            return null;
        }

        public double getPrice(List<Resource> bundle, Repository repo)
        {

            double value = repo.getBundleValue(bundle);

            return value;
        }
    }//[c]

    private class OfferHelperA {

        /*
         *
         * @param call
         * @param wallet
         * @return the bid to place
         * -1 means do not bid
         */
        public double generateOffer(CallForBids call, Wallet wallet, Repository repo) {

            double bid = 0;

            double minOffer = call.getMinOffer();

            double credits = wallet.getCredits();

            final double EXTRA_VALUE = minOffer > 0 ? 0.15*minOffer : 1500.0;

            bid = minOffer;

            List<Resource> resList = call.getBundle();

            // no offer if we do not need the resources
            if (wallet.contains(resList))
                return -1;

            Boolean isNeeded = isBundleNeeded(resList);

            if(isNeeded)
            {
                if(credits >= minOffer + EXTRA_VALUE)
                {
                    bid = minOffer + EXTRA_VALUE;
                }
            }
            else
            {
                bid = -1;
            }
            return bid;
        }


        private Boolean isBundleNeeded(List<Resource> bundle) {
            final int BUNDLE_SIZE = 2;

            if (bundle.size() >= BUNDLE_SIZE)
                return true;


            return false;
        }
    }//[c]

    private class OfferHelperB {

        /**
         * Minimum starting price for bundles
         * AA (200), AAA (300), AAAA (400), AAB (200), AJK (200),
         * BB (50),
         * CCCDDD (1200), CCDDAA (800), CCDDBB (600),
         * EEEEEF (1600), EEEEF (800), EEEF (400), EEF (200),
         * FF (100), FJK (300),
         * ABCDEFJK (1400).
         *
         * The created offer is what we ask for the resource bundle
         *
         * @param call
         * @param wallet
         * @param repo
         * @return
         * -1 if we do not generate an offer
         * otherwise offer for the bid
         */
        public double generateOffer(CallForBids call, Wallet wallet, Repository repo) {

            double offer = -1;

            double minOffer = call.getMinOffer();

            final double AMOUNT = minOffer > 0 ? 1.25*minOffer : 500.0;

            List<Resource> required = call.getBundle();

            Boolean canSell = wallet.contains(required);

            if( canSell )
            {
                offer = AMOUNT;
            }

            return offer;
        }

    }//[c]

    private class OfferHelperC {

        /**
         * The Agent then will place bids only for the items less likely to appear, excluded G, E and F.
         *
         * @param call
         * @param wallet
         * @param repo
         * @return
         */
        public double generateOffer(CallForBids call, Wallet wallet, Repository repo) {

            double offer = -1;

            double minOffer = call.getMinOffer();

            double credits = wallet.getCredits();

            final double AMOUNT = minOffer > 0 ? 0.15*minOffer : 25.0;

            List<Resource> required = call.getBundle();

            Boolean needed = !wallet.contains(required);

            if( needed )
            {
                offer = AMOUNT;
            }

            if(credits < offer)
            {
                offer = -1;
            }

            return offer;
        }
    }//[c]


}//[c]
