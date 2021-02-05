package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Message sent by the auctioneer when the auction is over.
 */
public class EndAuction implements IFact {

	private static final long serialVersionUID = -2924655499801795861L;

	/** ID of the bidder who won the auction as a whole */
	private final String winner;
	
	/** the winner's final wallet */
	private final Wallet wallet;
	
	
	public EndAuction(String winner, Wallet wallet) {
		this.winner = winner;
		this.wallet = wallet;
	}	
	
	public String getWinner() {
		return winner;
	}
	
	public Wallet getWallet() {
		return wallet;
	}

	@Override
	public String toString() {
		return String.format("EndAuction(%s, %s)", winner, wallet);
	}
	
}
