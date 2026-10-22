package dev.emi.emi.config;

import dev.emi.emi.config.SidebarSettings;
import dev.emi.emi.config.SidebarType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public class SidebarSubpanels {
    public final dev.emi.emi.config.SidebarSettings settings;
    public List<Subpanel> subpanels = new ObjectArrayList<>();

    public SidebarSubpanels(List<Subpanel> subpanels, SidebarSettings settings) {
        this.subpanels.addAll(subpanels);
        this.settings = settings;
        unique();
    }

    public void unique() {
        settings.pages().unique();
    }

    public static class Subpanel {
        public SidebarType type;
        public int rows;

        public Subpanel(SidebarType type, int rows) {
            this.type = type;
            this.rows = rows;
        }

        public int rows() {
            return Math.max(1, rows);
        }
    }
}
