package net.foliaboard.api;

public record FoliaBoardStats(long totalPackets, long providerRefreshes,
                              int activeSidebars, int activeNametags) {
    @Override
    public String toString() {
        return "FoliaBoardStats[packets=" + totalPackets + ", refreshes=" + providerRefreshes
                + ", sidebars=" + activeSidebars + ", nametags=" + activeNametags + "]";
    }
}
