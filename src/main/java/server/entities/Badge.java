package server.entities;

public class Badge {
    public enum BadgeType {
        RECENSORE("Recensore", 0, 10, "R"),
        RECENSORE_ESPERTO("Recensore Esperto", 11, 40, "RE"),
        CONTRIBUTORE("Contributore", 41, 70, "C"),
        CONTRIBUTORE_ESPERTO("Contributore Esperto", 71, 80, "CE"),
        CONTRIBUTORE_SUPER("Contributore Super", 81, 9999, "CS");

        private final String description;
        private final int boundFrom;
        private final int boundTo;
        private final String initials;

        BadgeType(String description, int boundFrom, int boundTo, String initials) {
            this.description = description;
            this.boundFrom = boundFrom;
            this.boundTo = boundTo;
            this.initials = initials;
        }

        public String getDescription() {
            return description;
        }

        public int getBoundFrom() {
            return boundFrom;
        }

        public int getBoundTo() {
            return boundTo;
        }

        public String getInitials() {
            return initials;
        }
    }

    private BadgeType badge;

    public Badge(BadgeType badgeType) {
        this.badge = badgeType;
    }

    public Badge(int value) {
        for (BadgeType type : BadgeType.values()) {
            if (value >= type.getBoundFrom() && value <= type.getBoundTo()) {
                this.badge = type;
                break;
            }
        }
        if (this.badge == null) {
            throw new IllegalArgumentException("Value not in badge range!");
        }
    }

    public String getBadge() {
        return this.badge.getDescription();
    }


    public String getInitials() {
        return this.badge.getInitials();
    }
}
