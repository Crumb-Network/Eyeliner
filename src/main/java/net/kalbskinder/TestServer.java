package net.kalbskinder;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;

public class TestServer {

    private static GlobalEventHandler globalEventHandler;


    void main() {
        MinecraftServer server = MinecraftServer.init();
        System.setProperty("minestom.chunk-view-distance", "16");

        globalEventHandler = MinecraftServer.getGlobalEventHandler();


        server.start("0.0.0.0", 25565);
    }
}
