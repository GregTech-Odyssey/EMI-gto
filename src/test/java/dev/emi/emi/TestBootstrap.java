package dev.emi.emi;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

public final class TestBootstrap {
    private TestBootstrap() {
    }

    public static void init() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError forgeNetworkingNeedsFml) {
        }
    }
}
