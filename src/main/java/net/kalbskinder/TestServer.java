package net.kalbskinder;

import net.kalbskinder.eyeliner.Menu;
import net.kyori.adventure.text.Component;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;

public class TestServer {

    private static GlobalEventHandler globalEventHandler;

    void main() {
        MinecraftServer server = MinecraftServer.init(new Auth.Online());

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();
        Instance instance = instanceManager.createSharedInstance(instanceContainer);

        instance.setGenerator(unit -> {
            final Point start = unit.absoluteStart();
            final Point size = unit.size();
            for (int x = 0; x < size.blockX(); x++) {
                for (int z = 0; z < size.blockZ(); z++) {
                    for (int y = 0; y < Math.min(40 - start.blockY(), size.blockY()); y++) {
                        unit.modifier().setBlock(start.add(x, y, z), Block.STONE);
                    }
                }
            }
        });

        globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 40, 0));
        });

        // Eyeliner: bind the onClick keys used in .eyeliner files to code, load the menu once, open it per player
        Eyeliner.init();
        Eyeliner.registerAction("joinGame", ctx -> {
            ctx.player().sendMessage(Component.text("Joining game..."));
            ctx.close();
        });
        Menu mainMenu = Eyeliner.loadResource("example.eyeliner");

        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) mainMenu.open(event.getPlayer());
        });

        Command menuCommand = new Command("menu");
        menuCommand.setDefaultExecutor((sender, _) -> {
            if (!(sender instanceof Player player)) return;
            Eyeliner.closeAll(player);
            mainMenu.open(player);
        });
        MinecraftServer.getCommandManager().register(menuCommand);

        server.start("0.0.0.0", 25565);
    }
}
