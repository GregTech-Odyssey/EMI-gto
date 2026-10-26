package dev.emi.emi.platform;

import dev.emi.emi.network.PingS2CPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmiClientCompatibilityTest {

    @AfterEach
    void disconnect() {
        dev.emi.emi.EmiClient.onServer = false;
    }

    @Test
    void legacyPackageSharesServerHandshakeAndDisconnectState() throws Exception {
        dev.emi.emi.EmiClient.onServer = false;
        assertFalse(EmiClient.onServer);
        new PingS2CPacket().apply(null);
        assertTrue(EmiClient.onServer);
        EmiClient.onServer = false;
        assertFalse(dev.emi.emi.EmiClient.onServer);
        assertSame(dev.emi.emi.EmiClient.class, EmiClient.class.getField("onServer").getDeclaringClass());
        assertSame(dev.emi.emi.EmiClient.HOE_ACTIONS, EmiClient.HOE_ACTIONS);
    }
}
