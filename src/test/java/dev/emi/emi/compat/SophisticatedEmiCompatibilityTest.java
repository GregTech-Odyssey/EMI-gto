package dev.emi.emi.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.compat.recipeviewers.emi.BackpackEmiPlugin;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ICraftingContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.CoreEmiPlugin;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.EmiGridMenuInfo;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.EmiTransferRecipeMessage;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.compat.recipeviewers.emi.StorageEmiPlugin;

import dev.emi.emi.EmiClient;
import dev.emi.emi.TestBootstrap;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiCraftContext.Destination;
import dev.emi.emi.registry.EmiRecipeFiller;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SophisticatedEmiCompatibilityTest {

    private static SimpleChannel channel;

    @BeforeAll
    static void bootstrap() throws Exception {
        TestBootstrap.init();
        // The menu caches a Forge-remapped reflection method at startup; JUnit has no ModLauncher.
        try (var reflection = mockStatic(ObfuscationReflectionHelper.class)) {
            Class.forName(StorageContainerMenuBase.class.getName());
        }
        channel = mock(SimpleChannel.class);
        try (var core = mockStatic(SophisticatedCore.class); var network = mockStatic(NetworkRegistry.class)) {
            core.when(SophisticatedCore::getNetworkProtocolVersion).thenReturn("test");
            network.when(() -> NetworkRegistry.newSimpleChannel(any(), any(), any(), any())).thenReturn(channel);
            assertNotNull(PacketHandler.INSTANCE);
        }
    }

    @ParameterizedTest
    @ValueSource(classes = { CoreEmiPlugin.class, BackpackEmiPlugin.class, StorageEmiPlugin.class })
    void allEmiCallsInRealPluginsLink(Class<?> plugin) throws Exception {
        int checked = 0;
        try (var jar = new JarFile(plugin.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().contains("/compat/recipeviewers/emi/") || !entry.getName().endsWith(".class")) {
                    continue;
                }
                try (var input = jar.getInputStream(entry)) {
                    new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            return new MethodVisitor(Opcodes.ASM9) {

                                @Override
                                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                                    if (owner.startsWith("dev/emi/emi/")) {
                                        assertDoesNotThrow(() -> {
                                            var type = Class.forName(owner.replace('/', '.'), false, getClass().getClassLoader());
                                            var field = type.getField(name);
                                            assertEquals(descriptor, org.objectweb.asm.Type.getDescriptor(field.getType()));
                                        }, owner + "." + name);
                                    }
                                }

                                @Override
                                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                    if (owner.startsWith("dev/emi/emi/")) {
                                        assertDoesNotThrow(() -> {
                                            var loader = getClass().getClassLoader();
                                            var type = Class.forName(owner.replace('/', '.'), false, loader);
                                            var methodType = MethodType.fromMethodDescriptorString(descriptor, loader);
                                            var lookup = MethodHandles.publicLookup();
                                            if (name.equals("<init>")) {
                                                lookup.findConstructor(type, methodType);
                                            } else if (opcode == Opcodes.INVOKESTATIC) {
                                                lookup.findStatic(type, name, methodType);
                                            } else {
                                                lookup.findVirtual(type, name, methodType);
                                            }
                                        }, owner + "." + name + descriptor);
                                    }
                                }
                            };
                        }
                    }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
                checked++;
            }
        }
        assertTrue(checked > 0);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @ParameterizedTest
    @ValueSource(classes = { BackpackContainer.class, StorageContainerMenu.class })
    void realCraftHandlerReadsLegacyFlagForEveryDestination(Class<? extends StorageContainerMenuBase> menuType) {
        var menu = mock(menuType);
        var screen = mock(AbstractContainerScreen.class);
        when(screen.getMenu()).thenReturn(menu);
        var upgrade = mock(UpgradeContainerBase.class, withSettings().extraInterfaces(ICraftingContainer.class));
        when(upgrade.isOpen()).thenReturn(true);
        when(menu.getOpenOrFirstCraftingContainer(RecipeType.CRAFTING)).thenReturn(Optional.of(upgrade));
        var handler = spy(EmiGridMenuInfo.crafting());
        var source = mock(Slot.class);
        var input = mock(Slot.class);
        var output = mock(Slot.class);
        source.index = 5;
        input.index = 40;
        output.index = 49;
        doReturn(List.of(source)).when(handler).getInputSources(menu);
        when(((ICraftingContainer) upgrade).getRecipeSlots()).thenReturn(List.of(input));
        when(upgrade.getSlots()).thenReturn(List.of(input, output));
        var recipe = mock(EmiRecipe.class);
        var recipeId = new ResourceLocation("minecraft", "oak_planks");
        when(recipe.getId()).thenReturn(recipeId);
        var client = mock(Minecraft.class);
        List<ItemStack> stacks = Collections.emptyList();
        boolean previous = EmiClient.onServer;
        try (var minecraft = mockStatic(Minecraft.class); var filler = mockStatic(EmiRecipeFiller.class)) {
            minecraft.when(Minecraft::getInstance).thenReturn(client);
            filler.when(() -> EmiRecipeFiller.getStacks(handler, recipe, screen, 1)).thenReturn(stacks);
            EmiClient.onServer = false;
            for (var destination : Destination.values()) {
                var context = mock(EmiCraftContext.class);
                when(context.getScreen()).thenReturn(screen);
                when(context.getScreenHandler()).thenReturn(menu);
                when(context.getDestination()).thenReturn(destination);
                filler.when(() -> EmiRecipeFiller.clientFill(handler, recipe, screen, stacks, destination)).thenReturn(true);
                assertTrue(handler.craft(recipe, context));
                filler.verify(() -> EmiRecipeFiller.clientFill(handler, recipe, screen, stacks, destination));

                for (int amount : new int[] { 1, 64 }) {
                    clearInvocations(channel);
                    when(context.getAmount()).thenReturn(amount);
                    EmiClient.onServer = true;
                    assertTrue(handler.craft(recipe, context));
                    var sent = ArgumentCaptor.forClass(EmiTransferRecipeMessage.class);
                    verify(channel).sendToServer(sent.capture());
                    var packet = sent.getValue();
                    assertEquals(recipeId, packet.recipeId());
                    assertEquals(new ResourceLocation("minecraft", "crafting"), packet.recipeTypeId());
                    assertEquals(switch (destination) {
                        case NONE -> 0;
                        case CURSOR -> 1;
                        case INVENTORY -> 2;
                    }, packet.action());
                    assertEquals(List.of(5), packet.slots());
                    assertEquals(List.of(40), packet.crafting());
                    assertEquals(49, packet.output());
                    assertSame(stacks, packet.stacks());
                    assertEquals(amount > 1, packet.maxTransfer());
                    EmiClient.onServer = false;
                }
            }
        } finally {
            EmiClient.onServer = previous;
        }
    }
}
