package dev.emi.emi.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.emi.emi.TestBootstrap;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import static org.mockito.Mockito.*;

class FillRecipeC2SPacketTest {
    @BeforeAll
    static void bootstrap() {
        TestBootstrap.init();
    }

    @Test
    void compressedRangesAndMissingCraftingSlotsRoundTripWithoutChangingWireFormat() {
        var input = new FriendlyByteBuf(Unpooled.buffer());
        var encoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            input.writeInt(42);
            input.writeByte(2);
            input.writeVarInt(3);
            input.writeVarInt(0);
            input.writeVarInt(127);
            input.writeVarInt(255);
            input.writeVarInt(257);
            input.writeVarInt(300);
            input.writeVarInt(300);
            input.writeVarInt(3);
            input.writeVarInt(9);
            input.writeVarInt(-1);
            input.writeVarInt(11);
            input.writeBoolean(true);
            input.writeVarInt(12);
            input.writeVarInt(0);

            new FillRecipeC2SPacket(input).write(encoded);
            assertEquals(input.writerIndex(), input.readerIndex());
            input.readerIndex(0);
            assertEquals(input, encoded);
        } finally {
            input.release();
            encoded.release();
        }
    }

    @Test
    void menuSlotEncodingPreservesOrderDuplicatesGapsAndNullCraftingTargets() {
        var menu = mock(AbstractContainerMenu.class);
        var first = slot(5);
        var next = slot(6);
        var distant = slot(10);
        var encoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new FillRecipeC2SPacket(menu, 1, List.of(first, next, distant, distant),
                    java.util.Arrays.asList(first, null, next), distant, List.of()).write(encoded);

            assertEquals(menu.containerId, encoded.readInt());
            assertEquals(1, encoded.readByte());
            assertEquals(3, encoded.readVarInt());
            assertEquals(5, encoded.readVarInt());
            assertEquals(6, encoded.readVarInt());
            assertEquals(10, encoded.readVarInt());
            assertEquals(10, encoded.readVarInt());
            assertEquals(10, encoded.readVarInt());
            assertEquals(10, encoded.readVarInt());
            assertEquals(3, encoded.readVarInt());
            assertEquals(5, encoded.readVarInt());
            assertEquals(-1, encoded.readVarInt());
            assertEquals(6, encoded.readVarInt());
            assertTrue(encoded.readBoolean());
            assertEquals(10, encoded.readVarInt());
            assertEquals(0, encoded.readVarInt());
            assertFalse(encoded.isReadable());
        } finally {
            encoded.release();
        }
    }

    private static Slot slot(int index) {
        var slot = mock(Slot.class);
        slot.index = index;
        return slot;
    }
}
