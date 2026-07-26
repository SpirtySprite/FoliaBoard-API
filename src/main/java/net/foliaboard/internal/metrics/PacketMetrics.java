package net.foliaboard.internal.metrics;

import java.util.concurrent.atomic.LongAdder;

public final class PacketMetrics {
    private final LongAdder total = new LongAdder();
    private final LongAdder refreshes = new LongAdder();

    public void sent() {
        total.increment();
    }

    public void refresh() {
        refreshes.increment();
    }

    public long totalPackets() {
        return total.sum();
    }

    public long refreshCount() {
        return refreshes.sum();
    }
}
