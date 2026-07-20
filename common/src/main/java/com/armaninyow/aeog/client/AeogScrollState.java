package com.armaninyow.aeog.client;

public final class AeogScrollState {
	private static double pending = 0;

	public static void add(double v) { pending += v; }

	public static double consume() {
		double v = pending;
		pending  = 0;
		return v;
	}

	private AeogScrollState() {}
}