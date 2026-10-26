package dev.emi.emi.config;

import dev.emi.emi.config.SidebarSettings;
import dev.emi.emi.config.SidebarSubpanels;
import dev.emi.emi.config.SidebarTheme;
import dev.emi.emi.config.SidebarType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.EnumSet;
import java.util.List;

public class SidebarPages {
    public final dev.emi.emi.config.SidebarSettings settings;
    public List<SidebarPage> pages = new ObjectArrayList<>();

    public SidebarPages(List<SidebarPage> pages, SidebarSettings settings) {
        this.pages.addAll(pages);
        this.settings = settings;
        unique();
    }

    public void unique() {
        var types = EnumSet.noneOf(SidebarType.class);
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).type == SidebarType.NONE || !types.add(pages.get(i).type)) {
                pages.remove(i--);
            }
        }
        if (settings.subpanels() != null) {
            List<SidebarSubpanels.Subpanel> subpanels = settings.subpanels().subpanels;
            for (int i = 0; i < subpanels.size(); i++) {
                if (subpanels.get(i).type == SidebarType.NONE || !types.add(subpanels.get(i).type)) {
                    subpanels.remove(i--);
                }
            }
        }
    }

    public boolean canShowChess() {
        return settings.size().values.getInt(0) == 8
            && settings.size().values.getInt(1) == 8
            && settings.theme() == SidebarTheme.MODERN;
    }

    public static class SidebarPage {
        public SidebarType type;

        public SidebarPage(SidebarType type) {
            this.type = type;
        }
    }
}
