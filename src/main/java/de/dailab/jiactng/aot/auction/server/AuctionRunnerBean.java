package de.dailab.jiactng.aot.auction.server;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.ontology.IActionDescription;

/**
 * The sole purpose of this bean is to trigger a new auction in regular intervals
 * and to log the results of past auctions.
 * 
 * The first auction is started in the doStart() method; after that,
 * the auction runner's execute() method will regularly check the results of the
 * previous run and trigger a new auction via the corresponding actions.
 */
public class AuctionRunnerBean extends AbstractAgentBean {

	enum Mode { FIXED, RANDOM }

	/** auction counter */
	private AtomicLong counter = new AtomicLong();
	
	/** "secret" token for calling actions at auctioneer meta bean */
	private String secretToken;
	
	/** total number of auctions to run, of null for infinite */
	private Integer numberOfAuctions;
	
	/** the "mode" of the current auction */
	private Mode mode = Mode.FIXED;
	
	/** random seed to use for EACH auction, or -1 for random random seeds */
	private Long randomSeed;
	
	/** minutes when to start new auction; e.g. if 10, start auctions at 
	 * 10, 20, 30, 40 after full hour etc.; use 60 for "at full hours"!
	 * if null, 0, or 1, start new auction as soon as the last one ended */
	private Integer auctionStartMinutes;
	
	/** time when the next auction should start */
	private LocalDateTime nextAuction = null;
	
	/** whether the last auction still has to be handled (logged etc.) */
	private boolean lastAuctionHandled = true;
	
	
	@Override
	public void doStart() throws Exception {
		updateNextAuctionStart();
		if (nextAuction == null || nextAuction.isBefore(LocalDateTime.now())) {
			IActionDescription action = thisAgent.searchAction(new Action(AuctioneerMetaBean.ACTION_START_AUCTION));
			invoke(action, new Serializable[] {secretToken, mode, getSeed()});
			lastAuctionHandled = false;
			counter.incrementAndGet();
		}
	}
	
	@Override
	public void execute() {
		try {
			Serializable[] results = invokeAction(AuctioneerMetaBean.ACTION_GET_PHASE, -1);
			if ("IDLE".equals(results[0])) {
				
				if (! lastAuctionHandled) {
					// get results from past auction, write to file
					results = invokeAction(AuctioneerMetaBean.ACTION_GET_LAST_RESULTS, -1, secretToken);
					String res = (String) results[0];
					// XXX call action with default secret token; check is enabled on server, but not for students
					results = invokeAction(AuctioneerMetaBean.ACTION_GET_STATE, -1, "nottheactualsecrettoken");
					String conf = String.valueOf(results[0]);
					String logs = new String(AbstractAuctioneerBean.LOG_STREAM.toByteArray());
					writeToFile(res, conf, logs);
					AbstractAuctioneerBean.LOG_STREAM.reset();

					// determine starting time of next auction
					updateNextAuctionStart();
					lastAuctionHandled = true;
				}
				
				if (numberOfAuctions == null || counter.get() < numberOfAuctions) {
					if (nextAuction == null || nextAuction.isBefore(LocalDateTime.now())) {
						if      (mode == Mode.FIXED)  mode = Mode.RANDOM;
						else if (mode == Mode.RANDOM) mode = Mode.FIXED;

						// dump all old messages from memory (might be spammed with invalid stuff from bidders)
						memory.removeAll(new JiacMessage());
						
						// trigger next auction
						invokeAction(AuctioneerMetaBean.ACTION_START_AUCTION, -1, secretToken, mode, getSeed());
						lastAuctionHandled = false;
						counter.incrementAndGet();
					} else {
						log.info("Waiting for next auction to start at " + nextAuction);
					}
				} else {
					log.info("ALL AUCTIONS ARE OVER");
					setExecutionInterval(-1);
				}
			} else {
				log.info("Auction still running...");
			}
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	private void updateNextAuctionStart() {
		if (auctionStartMinutes != null && auctionStartMinutes > 0) {
			LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
			int mins = now.get(ChronoField.MINUTE_OF_DAY);
			nextAuction = now.plusSeconds((auctionStartMinutes - (mins % auctionStartMinutes)) * 60);
		}
	}
	
	/**
	 * Write the results and logs of the last auction to a new separate log file
	 */
	private void writeToFile(String content, String conf, String logs) throws IOException {
		// find first 'new' number suffix for log file 
		int c = 0;
		Path path = null;
		do {
			path = Paths.get("logs/auction_" + c++ + ".txt");
		} while (Files.exists(path));
		// save log file
		content = String.format("%s\nAuction in this session: %s\n%s\n%s\n\n%s\n", 
				Instant.now().toString(), counter.get(), content, conf, logs);
		log.info("Writing result to " + path.toAbsolutePath());
		Files.write(path, content.getBytes());
	}

	private long getSeed() {
		return randomSeed == -1 ? new Random().nextLong() : randomSeed;
	}
	
	public void setSecretToken(String secretToken) {
		this.secretToken = secretToken;
	}
	
	public void setNumberOfAuctions(Integer numberOfAuctions) {
		this.numberOfAuctions = numberOfAuctions;
	}
	
	public void setRandomSeed(Long randomSeed) {
		this.randomSeed = randomSeed;
	}
	
	public void setAuctionStartMinutes(Integer auctionStartMinutes) {
		this.auctionStartMinutes = auctionStartMinutes;
	}
}
