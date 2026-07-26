package net.foliaboard.internal.packet;

public enum DisplaySlotType {
    SIDEBAR("SIDEBAR"),

    BELOW_NAME("BELOW_NAME"),

    PLAYER_LIST("LIST");

    private final String nmsName;

    DisplaySlotType(String nmsName) {
        this.nmsName = nmsName;
    }

    public String nmsName() {
        return nmsName;
    }
}
