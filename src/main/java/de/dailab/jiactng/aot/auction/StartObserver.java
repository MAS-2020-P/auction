package de.dailab.jiactng.aot.auction;

import de.dailab.jiactng.agentcore.SimpleAgentNode;

public class StartObserver {

	public static void main(String[] args) {
		final String config = "observer.xml";
		SimpleAgentNode.startAgentNode(config, null);
	}
	
}
