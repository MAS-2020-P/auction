package de.dailab.jiactng.aot.auction;

import java.util.Arrays;

import de.dailab.jiactng.agentcore.SimpleAgentNode;

/**
 * For starting any node, so we don't have to add a runner for each node in the pom.xml.
 * Get name of config file from command line arguments
 */
public class StartNode {

	public static void main(String[] args) {
		System.out.println("ARGUMENTS: " + Arrays.toString(args));
		if (args.length >= 1) {
			SimpleAgentNode.startAgentNode(args[0], null);
		} else {
			System.err.println("Please specify the name of the config file to start "
					+ "as command line parameter!");
		}
	}
}
