package de.dailab.jiactng.aot.auction;

import de.dailab.jiactng.agentcore.SimpleAgentNode;

/**
 * Entry point for starting the Auctioneer Node.
 * Start Bidders first, then Auctioneer.
 */
public class StartAuctioneer {

	public static void main(String[] args) {
		SimpleAgentNode.startAgentNode("auctioneer.xml", null);
	}
}
