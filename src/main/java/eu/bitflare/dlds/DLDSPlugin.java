package eu.bitflare.dlds;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import eu.bitflare.dlds.exceptions.DLDSException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static net.kyori.adventure.text.Component.text;

public class DLDSPlugin extends JavaPlugin implements Listener {

    private FileConfiguration rewardConfig;

    private GameManager gameManager;
    private ScoreboardManager scoreboardManager;
    private FileManager fileManager;

    @SuppressWarnings("UnstableApiUsage")
    LiteralCommandNode<CommandSourceStack> dldsCommand = Commands.literal("dlds")
            .then(Commands.literal("start")
                    .executes(ctx -> {
                        if(gameManager.getPlayers().isEmpty()){
                            ctx.getSource().getSender().sendMessage(DLDSComponents.startNoPlayers());
                            gameManager.playErrorSound(ctx.getSource().getExecutor());
                            return Command.SINGLE_SUCCESS;
                        }

                        if(!gameManager.startGame()){
                            ctx.getSource().getSender().sendMessage(DLDSComponents.startAlreadyRunning());
                            gameManager.playErrorSound(ctx.getSource().getExecutor());
                        }
                        return Command.SINGLE_SUCCESS;
                    }))
            .then(Commands.literal("stop")
                    .executes(ctx -> {
                        if(!gameManager.stopGame()){
                            ctx.getSource().getSender().sendMessage(DLDSComponents.stopNotStarted());
                            gameManager.playErrorSound(ctx.getSource().getExecutor());
                        }
                        return Command.SINGLE_SUCCESS;
                    }))
            .then(Commands.literal("time")
                    .then(Commands.literal("set")
                            .then(Commands.argument("player", ArgumentTypes.player())
                                    .then(Commands.argument("hours", IntegerArgumentType.integer(0, 23))
                                            .then(Commands.argument("minutes", IntegerArgumentType.integer(0, 59))
                                                    .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 59))
                                                            .executes(ctx -> {
                                                                final PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
                                                                final Player player = targetResolver.resolve(ctx.getSource()).getFirst();


                                                                int hours = IntegerArgumentType.getInteger(ctx, "hours");
                                                                int minutes = IntegerArgumentType.getInteger(ctx, "minutes");
                                                                int seconds = IntegerArgumentType.getInteger(ctx, "seconds");

                                                                if (gameManager.setTimeForPlayer(player, hours, minutes, seconds)) {
                                                                    ctx.getSource().getSender().sendMessage(DLDSComponents.timeSuccess(player, hours, minutes, seconds));
                                                                    return Command.SINGLE_SUCCESS;
                                                                }
                                                                ctx.getSource().getSender().sendMessage(DLDSComponents.timePlayerNotFound(player));
                                                                gameManager.playErrorSound(ctx.getSource().getExecutor());
                                                                return Command.SINGLE_SUCCESS;
                                                            })
                                                    )
                                            )
                                    )
                            )
                    )
            )
            .then(Commands.literal("team")
                    .then(Commands.literal("create")
                            .then(Commands.argument("teamname", StringArgumentType.word())
                                    .executes(ctx -> {
                                        CommandSender sender = ctx.getSource().getSender();
                                        String teamName = StringArgumentType.getString(ctx, "teamname");

                                        try {
                                            gameManager.createTeam(teamName);
                                            sender.sendMessage(DLDSComponents.teamCreateSuccess(teamName));
                                        } catch (DLDSException e) {
                                            sender.sendMessage(e.errorMessage());
                                            gameManager.playErrorSound(ctx.getSource().getExecutor());
                                        }
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
                    .then(Commands.literal("delete")
                            .then(Commands.argument("teamname", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        gameManager.getTeams().stream().map(DLDSTeam::getName)
                                                .filter(entry -> entry.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                                .forEach(builder::suggest);
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        CommandSender sender = ctx.getSource().getSender();
                                        String teamName = StringArgumentType.getString(ctx, "teamname");

                                        try {
                                            gameManager.removeTeam(teamName);
                                            sender.sendMessage(DLDSComponents.teamRemoveSuccess(teamName));
                                        } catch (DLDSException e){
                                            sender.sendMessage(e.errorMessage());
                                            gameManager.playErrorSound(ctx.getSource().getExecutor());
                                        }
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
                    .then(Commands.literal("list")
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();

                                sender.sendMessage(DLDSComponents.teamList(gameManager.getTeams()));

                                return Command.SINGLE_SUCCESS;
                            })
                    )
                    .then(Commands.literal("addplayer")
                            .then(Commands.argument("player", ArgumentTypes.player())
                                    .then(Commands.argument("teamname", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                gameManager.getTeams().stream().map(DLDSTeam::getName)
                                                        .filter(entry -> entry.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                                        .forEach(builder::suggest);
                                                return builder.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                CommandSender sender = ctx.getSource().getSender();
                                                final PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
                                                final Player player = targetResolver.resolve(ctx.getSource()).getFirst();
                                                final String teamName = StringArgumentType.getString(ctx, "teamname");

                                                try {
                                                    gameManager.addPlayerToTeam(player, teamName);
                                                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                                                    player.sendMessage(DLDSComponents.youWereAdded(teamName));

                                                    // Send confirmation message to sender if the sender is not the player that was added to a team
                                                    if(ctx.getSource().getExecutor() instanceof Player playerExecutor
                                                        && !playerExecutor.getUniqueId().equals(player.getUniqueId())) {
                                                        sender.sendMessage(DLDSComponents.teamAddPlayerSuccess(player, teamName));
                                                    }

                                                } catch (DLDSException e) {
                                                    sender.sendMessage(e.errorMessage());
                                                    gameManager.playErrorSound(ctx.getSource().getExecutor());
                                                }

                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                    )
                    .then(Commands.literal("removeplayer")
                            .then(Commands.argument("player", ArgumentTypes.player())
                                    .then(Commands.argument("teamname", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                gameManager.getTeams().stream().map(DLDSTeam::getName)
                                                        .filter(entry -> entry.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                                        .forEach(builder::suggest);
                                                return builder.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                CommandSender sender = ctx.getSource().getSender();
                                                final PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
                                                final Player player = targetResolver.resolve(ctx.getSource()).getFirst();
                                                final String teamName = StringArgumentType.getString(ctx, "teamname");

                                                try {
                                                    gameManager.removePlayerFromTeam(player, teamName);
                                                    player.sendMessage(DLDSComponents.youWereRemoved(teamName));

                                                    // Send confirmation message to sender if the sender is not the player that was removed from the team
                                                    if(ctx.getSource().getExecutor() instanceof Player playerExecutor
                                                            && !playerExecutor.getUniqueId().equals(player.getUniqueId())) {
                                                        sender.sendMessage(DLDSComponents.teamRemovePlayerSuccess(player, teamName));
                                                    }

                                                } catch (DLDSException e) {
                                                    sender.sendMessage(e.errorMessage());
                                                    gameManager.playErrorSound(ctx.getSource().getExecutor());
                                                }

                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                    )
            )
            .executes(ctx -> {
                ctx.getSource().getSender().sendMessage(DLDSComponents.helpMessage());
                return Command.SINGLE_SUCCESS;
            }).build();


    @Override
    public void onEnable() {

        loadRewards();

        this.gameManager = new GameManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.fileManager = new FileManager(this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(dldsCommand);
        });

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);

        int worldborderSize = getConfig().getInt("worldborder");
        if(worldborderSize < 10000 && worldborderSize != 0) {
            getComponentLogger().warn(text().content("Invalid world border size! Resetting value to default of 10000...").build());
            getConfig().set("worldborder", 10000);
        }
        saveConfig();



        Bukkit.getPluginManager().registerEvents(this, this);
        fileManager.loadGameState();

        // Respawn ender dragon if server was offline during respawn time
        if(gameManager.getDragonRespawnTime() < System.currentTimeMillis()) {
            gameManager.respawnEnderDragon();
        } else {
            long ticks = (gameManager.getDragonRespawnTime() - System.currentTimeMillis()) / 50L;

            if(ticks / 20 <= getConfig().getLong("dragon_respawn_delay") * 60) {
                getComponentLogger().info("Respawning Ender Dragon in {}m{}s", (int) (ticks / 20 / 60), (int) ((ticks/20) % 60));
            }

            Bukkit.getScheduler().runTaskLater(this, () -> gameManager.respawnEnderDragon(), ticks);
        }

        // Custom day/night cycle
        World overworld = getServer().getWorlds().getFirst();
        overworld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);

        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            final World overworld = getServer().getWorlds().getFirst();
            long ticks = 0;

            private int sleepersNeeded(int onlinePlayers, int neededPercentage) {
                int res = (int)(neededPercentage / 100D * onlinePlayers);
                if(res <= 0) {
                    return 1;
                }
                return res;
            }

            @Override
            public void run() {
                long currentTime = overworld.getTime();

                // Set multiplier based on current time
                double multiplier;
                if(currentTime <= 12000L) {
                    // Time is day -> set multiplier to 2/3
                    multiplier = 10./15;
                } else {
                    //Time is night -> set multiplier to 2
                    multiplier = 2;
                }

                // Advance time according to multiplier
                if(multiplier < 1D) {
                    if(ticks > 1D / multiplier) {
                        ticks = 0;
                        overworld.setTime(currentTime + 1);
                    }
                    ticks++;
                } else {
                    overworld.setTime(currentTime + (long)multiplier);
                }

                // Check if players are sleeping
                if(currentTime > 12000L || overworld.hasStorm()) {
                    List<Player> onlinePlayers = overworld.getPlayers();
                    List<Player> sleepingPlayers = new LinkedList<>();

                    // Count how many players are currently trying to sleep
                    if(!onlinePlayers.isEmpty()) {
                        for(Player player : onlinePlayers) {
                            if(player.getSleepTicks() >= 100) {
                                sleepingPlayers.add(player);
                            }
                        }
                    }

                    // Skip night if necessary
                    int sleepersNeeded = sleepersNeeded(onlinePlayers.size(), overworld.getGameRuleValue(GameRule.PLAYERS_SLEEPING_PERCENTAGE));
                    if(sleepingPlayers.size() >= sleepersNeeded) {
                        overworld.setTime(0);
                        if(overworld.hasStorm()) {
                            overworld.setClearWeatherDuration((int)(Math.random() * 168000.0D + 12000.0D));
                        }
                    }


                }


            }
        }, 0L, 1L);

        gameManager.startTimers();
    }

    @Override
    public void onDisable() {
        fileManager.saveGameState();
    }

    private void loadRewards() {

        File rewardConfigFile = new File(getDataFolder(), "rewards.yml");
        if(!rewardConfigFile.exists()) {
            // Load from JAR if it does not exist already
            rewardConfigFile.getParentFile().mkdirs();
            saveResource("rewards.yml", false);
        }

        rewardConfig = YamlConfiguration.loadConfiguration(rewardConfigFile);
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public FileConfiguration getRewardConfig() {
        return rewardConfig;
    }

}
