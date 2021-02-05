package de.dailab.jiactng.aot.auction;

import de.dailab.jiactng.agentcore.SimpleAgentNode;

public class StartGateway {

	public static void main(String[] args) {
		final String config = "GatewayNode.xml";
		SimpleAgentNode.startAgentNode(config, null);
	}
	
}
