package rtx.heave.api.modules;

public enum Category {
    COMBAT("Combat", "a", "Категория имеет в себе модули для PvP контента"),
    MOVEMENT("Movement", "b", "Категория имеет в себе модули, отвечающие за мувмент клиента"),
    VISUALS("Visuals", "c", "Категория имеет в себе модули, отвечающие за визуализацию клиента"),
    DISPLAY("Display", "d", "Категория имеет в себе элементы интерфейса и HUD"),
    PLAYER("Player", "d", "Категория имеет в себе модули, отвечающие за PvE контент"),
    UTILS("Utils", "e", "Категория имеет в себе вспомогательные модули клиента"),
    EVENTS("Events", "b", "Категория имеет в себе маркеры и трекеры событий"),
    CONFIGS("Configs", "f", "Категория имеет в себе все сохранённые конфигурации клиента"),
    THEMES("Themes", "g", "Категория имеет в себе все элементы для кастомизации клиента");

    private final String displayName;
    private final String icon;
    private final String description;

    Category(String displayName) {
        this(displayName, "c", "");
    }

    Category(String displayName, String icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String toString() {
        return this.displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getIcon() {
        return this.icon;
    }

    public String getDescription() {
        return this.description;
    }
}
