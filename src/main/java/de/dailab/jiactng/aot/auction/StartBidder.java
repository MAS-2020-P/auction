package de.dailab.jiactng.aot.auction;

import de.dailab.jiactng.agentcore.SimpleAgentNode;

/**
 * Entry point for starting the Bidder Node.
 * Start Bidders first, then Auctioneer.
 */
public class StartBidder {

	public static void main(String[] args) {
		SimpleAgentNode.startAgentNode("bidder.xml", null);
	}
}
