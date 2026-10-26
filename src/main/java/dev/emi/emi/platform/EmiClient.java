package dev.emi.emi.platform;

/**
 * Legacy package used by third-party EMI integrations. Inherit the client members so both package
 * names share the same server handshake state and recipe transfer implementation.
 */
public final class EmiClient extends dev.emi.emi.EmiClient {}
