package dev.no.name.bdadvance.modes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;
import java.util.Set;

public final class BloodstoneServer {
    private static final Set<String> HOSTS = Set.of("bloodstone.boo", "extra.bloodstone.boo");

    private BloodstoneServer() {
    }

    public static boolean isConnected() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hasSingleplayerServer() || minecraft.getConnection() == null) {
            return false;
        }
        ServerData server = minecraft.getCurrentServer();
        return server != null && matches(server.ip);
    }

    public static boolean matches(String address) {
        if (address == null) {
            return false;
        }
        String host = address.strip().toLowerCase(Locale.ROOT);
        int colon = host.lastIndexOf(':');
        if (colon >= 0) {
            host = host.substring(0, colon);
        }
        return HOSTS.contains(host);
    }
}
