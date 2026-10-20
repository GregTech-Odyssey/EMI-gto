package dev.emi.emi.screen.widget.config;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import dev.emi.emi.EmiPort;
import dev.emi.emi.screen.ConfigScreen.Mutator;

public class StringWidget extends ConfigEntryWidget {
	public final EditBox text;

	public StringWidget(Component name, List<ClientTooltipComponent> tooltip, Supplier<String> search, Mutator<String> mutator) {
		super(name, tooltip, search, 20);
		Minecraft client = Minecraft.getInstance();
		text = new EditBox(client.font, 0, 0, 150, 18, EmiPort.literal(""));
		text.setMaxLength(4096);
		text.setValue(mutator.get());
		text.setResponder(mutator::set);
		this.setChildren(List.of(text));
	}

	@Override
	public void update(int y, int x, int width, int height) {
		text.x = x + width - 150;
		text.y = y + 1;
	}
}
