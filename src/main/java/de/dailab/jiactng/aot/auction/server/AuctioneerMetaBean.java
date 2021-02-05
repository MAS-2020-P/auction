package de.dailab.jiactng.aot.auction.server;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.aot.auction.onto.Bidder;
import de.dailab.jiactng.aot.auction.onto.EndAuction;
import de.dailab.jiactng.aot.auction.onto.InitializeBidder;
import de.dailab.jiactng.aot.auction.onto.Register;
import de.dailab.jiactng.aot.auction.onto.Resource;
import de.dailab.jiactng.aot.auction.onto.StartAuctions;
import de.dailab.jiactng.aot.auction.onto.Wallet;
import de.dailab.jiactng.aot.auction.server.AuctionRunnerBean.Mode;

/**
 * This is the "Meta", or "Master" Auctioneer. It is responsible for the
 * registration of bidders and for starting and stopping the actual auctions.
 * In the end, it will check the wallets of the different bidders and
 * announce the winner.
 */
public class AuctioneerMetaBean extends AbstractAuctioneerBean {

	/** the current state of the auction */
	enum Phase { STARTING, REGISTRATION, STARTING_ABC, WAITING, STOPPING, EVALUATION, IDLE }
	
	/*
	 * CONFIGURATION
	 * those can be set in the Spring configuration file
	 */

	/** quick&dirty authentication for action invocations */
	protected String secretToken;

	/** initial balance for each bidder */
	private Double initialBalance;
	
	/** initial number of J or K resources to give to each bidder */
	private Integer initialJKcount;
	
	/** number of rounds to wait before stopping B */
	private Integer extraRoundsB = 0;
	
	/** number of rounds to wait before stopping C */
	private Integer extraRoundsC = 0;
	

	/** random number generator seed */
	private Long randomSeed;
	
	/** message to be sent with StartAuctions message */
	private String startMessage;
	
	/** counter for current auctions' ID; because of duplicate messages via Gateway+Multicast */
	private AtomicInteger auctionsId = new AtomicInteger();
	
	/** list of known group tokens, or null to allow any token; is read from file when agent starts */
	private List<String> knownGroupTokens = null;
	
	/*
	 * STATE
	 */
	
	/** the current phase */
	private Phase phase = Phase.IDLE;
	
	/** the mode of the current auction, i.e. fixed, public, etc. */
	private Mode mode;
	
	/** when the last auction started */
	private LocalTime started;

	/** number of rounds remaining after auction A ended before stopping B */
	private AtomicInteger roundsRemainingB; 
	
	/** number of rounds remaining after auction A ended before stopping C */
	private AtomicInteger roundsRemainingC; 
	
	/*
	 * LIFECYCLE METHODS
	 */

	@Override
	public void doStart() throws Exception {
		super.doStart();

		// get allowed group tokens from file in user home directory
		Path file = Paths.get(System.getProperty("user.home"), "aotgrouptokens.txt");
		log.info("Reading group tokens from file " + file);
		if (file.toFile().exists()) {
			this.knownGroupTokens = Files.readAllLines(file);
			log.info("Known Tokens: " + this.knownGroupTokens);
		} else {
			log.warn("File not found! Allowing any group token.");
		}
	}
	
	@Override
	public void execute() {
		if (phase == Phase.IDLE) return;
		log.info("Current Phase: " + phase);
		log.info("Messages in Memory: " + memory.readAll(new JiacMessage()).size());
		
		switch (phase) {
		case STARTING:
			// discard old bidders, if any
			clearBidders();
			
			// send initial start auction message
			sendGroup(new StartAuctions(auctionsId.incrementAndGet(), startMessage));
			phase = Phase.REGISTRATION;
			break;
		
		case REGISTRATION:
			Set<String> groupTokens = new HashSet<>(); // ensure unique group tokens
			
			// get all registration messages from memory
			for (JiacMessage message : memory.removeAll(new JiacMessage(new Register(null, null)))) {
				Register register = (Register) message.getPayload();

				if (register.getBidderId() == null || register.getGroupToken() == null) {
					log.warn("Discarding Register without bidder id or group token");
					send(new InitializeBidder(register.getBidderId(), null), message.getSender());
				}else if (! groupTokens.add(register.getGroupToken())) {
					log.warn("More than one bidder trying to register with group token " + register.getGroupToken());
					send(new InitializeBidder(register.getBidderId(), null), message.getSender());
				} else if (getBidder(register.getBidderId()) != null) {
					log.warn("More than one bidder trying to register with bidderID " + register.getBidderId());
					send(new InitializeBidder(register.getBidderId(), null), message.getSender());
				} else if (knownGroupTokens != null && ! knownGroupTokens.contains(register.getGroupToken())) {
					log.warn("Trying to register with unknown group tokenID " + register.getBidderId());
					send(new InitializeBidder(register.getBidderId(), null), message.getSender());
				} else {
					// create new bidder and initialize wallet
					Wallet wallet = new Wallet(register.getBidderId(), initialBalance);
					wallet.add(random.nextBoolean() ? Resource.J : Resource.K, initialJKcount);
					Bidder bidder = new Bidder(register.getBidderId(), register.getGroupToken(), message.getSender(), wallet);
					addBidder(bidder);
					
					send(new InitializeBidder(bidder.getBidderId(), wallet), bidder.getBidderId());
				}
			}

			// next phase: starting the auctions
			phase = Phase.STARTING_ABC;
			// ... except if no-one showed up for the auction
			if (getBidders().isEmpty()) {
				phase = Phase.IDLE;
				log.info("No one showed up for the auction. :-(");
			}
			break;
			
		case STARTING_ABC:
			log.info("Starting new Round of Auctions, mode " + mode);

			// invoke services to start auctioneers A, B, C
			try {
				invokeSimple(AuctioneerABean.ACTION_START_AUCTION, mode, randomSeed);
				invokeSimple(AuctioneerBBean.ACTION_START_AUCTION, mode, randomSeed);
				invokeSimple(AuctioneerCBean.ACTION_START_AUCTION, mode, randomSeed);
			} catch (Exception e) {
				log.error("Could not start Auctions", e);
			}

			roundsRemainingB = new AtomicInteger(extraRoundsB);
			roundsRemainingC = new AtomicInteger(extraRoundsC);
			phase = Phase.WAITING;
			break;

		case WAITING:
			log.info("Waiting for auction A to end");
			
			// auction running, wait for auctions to be over, then advance to next stage
			try {
				Serializable[] res = invokeAction(AuctioneerABean.ACTION_IS_FINISHED, -1);
				if (Boolean.TRUE.equals(res[0])) {
					phase = Phase.STOPPING;
				}
			} catch (Exception e) {
				log.error("Could not check Auction A state", e);
			}
			break;

		case STOPPING:
			if (roundsRemainingB.getAndDecrement() == 0) {
				try {
					log.info("Stopping auction B...");
					invokeSimple(AuctioneerBBean.ACTION_STOP_AUCTION);
				} catch (Exception e) {
					log.error("Could not Stop Auctions B", e);
				}
			}
			if (roundsRemainingC.getAndDecrement() == 0) {
				try {
					log.info("Stopping auction C...");
					invokeSimple(AuctioneerCBean.ACTION_STOP_AUCTION);
				} catch (Exception e) {
					log.error("Could not Stop Auctions C", e);
				}
			}
			if (roundsRemainingB.get() < 0 && roundsRemainingC.get() < 0) {
				phase = Phase.EVALUATION;
			}
			break;
			
		case EVALUATION:
			log.info("Evaluating auction results");

			log.info("Final Wallets");
			getBidders().stream().map(Bidder::getWallet).forEach(log::info);

			// check wallets & determine winner
			getBidders().stream().map(Bidder::getWallet)
					.filter(w -> ! w.check())
					.forEach(w -> log.warn("Invalid Wallet: " + w));
			
			Optional<Wallet> winner = getBidders().stream().map(Bidder::getWallet)
					.filter(Wallet::check)
					.max(Comparator.comparing(Wallet::getValue));
			if (winner.isPresent()) {
				log.info("Winner: " + winner.get());
				sendRegistered(new EndAuction(winner.get().getBidderId(), winner.get()));
			} else {
				sendRegistered(new EndAuction("No valid winning wallet", null));
			}
			
			phase = Phase.IDLE;
			break;
	
		case IDLE:
			break;
		}
	}

	/*
	 * ACTIONS
	 * used by AuctionRunner and AuctionObserver beans
	 */

	public static final String ACTION_START_AUCTION = "Auctioneer#startAuction";
	public static final String ACTION_GET_LAST_RESULTS = "Auctioneer#getLastResults";
	public static final String ACTION_GET_PHASE = "Auctioneer#getPhase";
	public static final String ACTION_GET_STATE = "Auctioneer#getState";
	
	@Expose(name=ACTION_START_AUCTION, scope=ActionScope.GLOBAL)
	public void startAuction(String token, Mode mode, Long randomSeed) {
		checkToken(token);
		if (phase != Phase.IDLE)
			throw new IllegalStateException("Can not start new Auction in Phase " + phase);

		this.started = LocalTime.now();
		this.phase = Phase.STARTING;
		this.mode = mode;
		this.random = new Random(randomSeed);
		this.randomSeed = randomSeed;
	}

	@Expose(name=ACTION_GET_LAST_RESULTS, scope=ActionScope.GLOBAL)
	public String getLastResults(String token) {
		checkToken(token);
		return getBidders().stream()
				.sorted(Comparator.comparing((Bidder b) -> b.getWallet().getValue()).reversed())
				.map(Bidder::toString)
				.collect(Collectors.joining("\n"));
	}
	
	@Expose(name=ACTION_GET_PHASE, scope=ActionScope.GLOBAL)
	public String getPhase() {
		return phase.toString();
	}

	@Expose(name=ACTION_GET_STATE, scope=ActionScope.GLOBAL)
	public Map<String, Map<String, Object>> getState(String token) {
		// no need to check the token here, all this is public information
		// also allows to see the info of all running auctioneers if more than one
//		checkToken(token);
		
		Map<String, Object> config = new HashMap<>();
		config.put("initialBalance", initialBalance);
		config.put("initialJKcount", initialJKcount);
		config.put("messageGroup", messageGroup);
		config.put("startMessage", startMessage);
		config.put("owner", thisAgent.getOwner());
		
		Map<String, Object> state = new HashMap<>();
		state.put("phase", phase);
		state.put("mode", mode);
		state.put("randomSeed", randomSeed);
		state.put("participants", getBidders().stream().map(Bidder::getBidderId).collect(Collectors.toList()));
		state.put("last_started", started);
		
		Map<String, Map<String, Object>> res = new HashMap<>();
		res.put("config", config);
		res.put("state", state);
		return res;
	}
	
	private void checkToken(String token) {
		if (! Objects.equals(secretToken, token)) 
			throw new IllegalArgumentException("Invalid Action Invocation Token: " + token);
	}
	
	private void invokeSimple(String actionName, Serializable... params) {
		invoke(retrieveAction(actionName), params);
	}
	
	/*
	 * GETTERS AND SETTERS
	 * needed for setting properties via Spring configuration file
	 */

	public void setInitialBalance(Double initialBalance) {
		this.initialBalance = initialBalance;
	}
	
	public void setInitialJKcount(Integer initialJKcount) {
		this.initialJKcount = initialJKcount;
	}
	
	public void setSecretToken(String secretToken) {
		this.secretToken = secretToken;
	}

	public void setExtraRoundsB(Integer extraRoundsB) {
		this.extraRoundsB = extraRoundsB;
	}
	
	public void setExtraRoundsC(Integer extraRoundsC) {
		this.extraRoundsC = extraRoundsC;
	}
	
	public void setStartMessage(String startMessage) {
		this.startMessage = startMessage;
	}

}
