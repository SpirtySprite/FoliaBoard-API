package net.foliaboard.internal.packet;

import net.foliaboard.internal.packet.reflect.NmsPacketAdapter;
import net.foliaboard.internal.version.ServerVersion;

import java.util.logging.Logger;

public final class PacketAdapterFactory {
    private PacketAdapterFactory() {
    }

    public static PacketAdapter create(Logger logger) {
        ServerVersion version = ServerVersion.current();
        if (!version.isAtLeast(20, 6)) {
            throw new IllegalStateException("FoliaBoard requires Minecraft 1.20.6 or newer (found "
                    + version + "). The packet layer relies on modern per-score display components.");
        }
        try {
            NmsPacketAdapter adapter = new NmsPacketAdapter();
            logger.info("FoliaBoard packet adapter: " + adapter.describe() + " on MC " + version);
            return adapter;
        } catch (Throwable t) {
            throw new IllegalStateException("FoliaBoard: failed to initialise the packet adapter on MC "
                    + version + ". This build targets Mojang-mapped Paper/Folia 1.20.6+; the server's "
                    + "internals may have changed. See NmsPacketAdapter.", t);
        }
    }
}
