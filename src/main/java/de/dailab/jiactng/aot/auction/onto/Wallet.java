package de.dailab.jiactng.aot.auction.onto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Current "wallet", or "inventory" of a bidder. Bidders have to keep track
 * of their own wallet, but the auctioneer does the same, to prevent cheating.
 */
public class Wallet implements IFact {

	private static final long serialVersionUID = -7219909278211999621L;

	/** the ID of the bidder whom this wallet belongs to */
	private String bidderId;
	
	/** remaining credits */
	private Double credits;
	
	/** resources currently belonging to the bidder */
	private final Map<Resource, Integer> resources;
	
	
	public Wallet(String bidderId, Double credits) {
		this.bidderId = bidderId;
		this.credits = credits;
		this.resources = new HashMap<>();
	}
	
	public String getBidderId() {
		return bidderId;
	}
	
	public Double getCredits() {
		return credits;
	}

	/*
	 * HELPER METHODS
	 */
	
	public Double updateCredits(double delta) {
		this.credits += delta;
		return this.credits;
	}

	public void add(Resource res, int count) {
		this.resources.put(res, get(res) + count);
	}
	
	public void add(List<Resource> bundle) {
		for (Resource res : bundle) {
			add(res, +1);
		}
	}
	
	public void remove(List<Resource> bundle) {
		for (Resource res : bundle) {
			add(res, -1);
		}
	}

	/**
	 * check whether all the resources in the bundle are present in the wallet
	 * in the given quantities or more.
	 */
	public boolean contains(List<Resource> bundle) {
		return bundle.stream()
				.collect(Collectors.groupingBy(k -> k, Collectors.counting())).entrySet().stream()
				.allMatch(e -> get(e.getKey()) >= e.getValue());
	}
	
	public Integer get(Resource resource) {
		return this.resources.computeIfAbsent(resource, r -> 0);
	}

	/**
	 * Get total value of wallet, including penalties for leftover or missing resources.
	 */
	public double getValue() {
		int resourcesLeft = resources.values().stream().mapToInt(n -> n).filter(n -> n > 0).sum();
		int resourcesMsng = resources.values().stream().mapToInt(n -> n).filter(n -> n < 0).sum();
		return credits - 20 * resourcesLeft - 10000 * -resourcesMsng;
	}
	
	/**
	 * check whether credits and resources are not negative
	 */
	public boolean check() {
		return credits >= 0 && resources.values().stream().allMatch(i -> i >= 0);
	}
	
	@Override
	public String toString() {
		return String.format("Wallet(bidderId=%s, credits=%.2f, resources=%s, value=%.2f)",
				bidderId, credits, resources, getValue());
	}
	
}
