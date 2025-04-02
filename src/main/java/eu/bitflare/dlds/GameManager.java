package eu.bitflare.dlds;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import eu.bitflare.dlds.exceptions.PlayerAlreadyInTeamException;
import eu.bitflare.dlds.exceptions.PlayerNotInTeamException;
import eu.bitflare.dlds.exceptions.TeamAlreadyExistsException;
import eu.bitflare.dlds.exceptions.TeamNotFoundException;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.boss.DragonBattle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.OminousBottleMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

import static eu.bitflare.dlds.DLDSColor.DARK_GREY;
import static eu.bitflare.dlds.DLDSColor.LIGHT_GREY;
import static net.kyori.adventure.text.Component.text;

public class GameManager implements Listener {

    private final DLDSPlugin plugin;
    private Map<UUID, PlayerData> players;
    private Set<DLDSTeam> teams;
    private boolean isGameRunning;
    private boolean isTimerRunning;
    private boolean isCountdownRunning;
    private long dragonRespawnTime;

    private final int totalAdvancementPoints;
    private final int totalAdvancementCount;

    public GameManager(DLDSPlugin plugin) {
        this.plugin = plugin;
        this.players = new HashMap<>();
        this.teams = new HashSet<>();
        this.isGameRunning = false;
        this.isTimerRunning = false;
        this.isCountdownRunning = false;
        this.dragonRespawnTime = Long.MAX_VALUE;

        this.totalAdvancementPoints = computeTotalAdvancementPoints();
        this.totalAdvancementCount = computeTotalAdvancementCount();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void createTeam(String teamName) throws TeamAlreadyExistsException {
        // Check if team already exists
        Optional<DLDSTeam> team = getTeam(teamName);
        if (team.isPresent()) {
            throw new TeamAlreadyExistsException(team.get());
        }

        teams.add(new DLDSTeam(this, teamName));
    }

    public void removeTeam(String teamName) throws TeamNotFoundException {
        // Check if team even exists
        Optional<DLDSTeam> team = getTeam(teamName);
        if(team.isEmpty()) {
            throw new TeamNotFoundException(teamName);
        }

        teams.remove(team.get());
    }

    public void addPlayerToTeam(Player player, String teamName) throws TeamNotFoundException, PlayerAlreadyInTeamException {
        // Check if team exists
        Optional<DLDSTeam> targetTeam = getTeam(teamName);
        if(targetTeam.isEmpty()) {
            throw new TeamNotFoundException(teamName);
        }

        // Check if player is currently in another team
        Optional<DLDSTeam> currentTeam = getTeam(player);
        if(currentTeam.isPresent()) {
            throw new PlayerAlreadyInTeamException(player, currentTeam.get());
        }

        // Add player to team
        targetTeam.get().addPlayer(player);
    }

    public void removePlayerFromTeam(Player player, String teamName) throws TeamNotFoundException, PlayerNotInTeamException {
        // Check if team even exists
        Optional<DLDSTeam> targetTeam = getTeam(teamName);
        if(targetTeam.isEmpty()) {
            throw new TeamNotFoundException(teamName);
        }

        // Check if player even is in the team
        if(!targetTeam.get().containsPlayer(player)) {
            throw new PlayerNotInTeamException(player, targetTeam.get());
        }

        // Remove player from team
        targetTeam.get().removePlayer(player);
    }

    public boolean startGame() {
        if(isGameRunning) {
            return false;
        }
        isGameRunning = true;

        World overworld = plugin.getServer().getWorlds().getFirst();
        int worldborderSize = plugin.getConfig().getInt("worldborder");


        // Get random spawn location and generate chunks
        Location location = getRandomSpawnLocation(worldborderSize == 0 ? 10000 : worldborderSize/2.0);
        int chunkX = location.getChunk().getX();
        int chunkZ = location.getChunk().getZ();
        for(int x = chunkX - 5; x < chunkX + 5; x++) {
            for(int z = chunkZ - 5; z < chunkZ + 5; z++) {
                overworld.getChunkAtAsync(x, z, true);
            }
        }


        // Give blindness, slowness and play sound
        for(Player player : getOnlineRegisteredPlayers()) {
            player.setWalkSpeed(0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20*13, 1, false, false));
        }

        // Show loading message
        Component message = Component.text("The game will start soon!").color(DLDSColor.LIGHT_BLUE);
        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1));
        broadcastTitleToRegisteredPlayers(message, Component.empty(), times);


        // Start countdown and teleport players
        isCountdownRunning = true;
        new BukkitRunnable() {

            private int countdown = 12;
            private final Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(250));

            @Override
            public void run() {

                switch (countdown) {
                    case 11 -> {
                        // Player sound shortly after countdown started
                        for(Player player : getOnlineRegisteredPlayers()) {
                            player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSER, 1f, 1f);
                        }
                    }
                    case 1, 2, 3, 4, 5 -> {
                        // Teleport and reset players once countdown reaches 5
                        if(countdown == 5) {
                            teleportPlayers(location);
                        }

                        // Only show countdown and play notes for the last five seconds

                        TextColor color = switch (countdown) {
                            case 3 -> DLDSColor.ORANGE;
                            case 2 -> DLDSColor.YELLOW;
                            case 1 -> DLDSColor.LIGHT_GREEN;
                            default -> DLDSColor.WHITE;
                        };
                        broadcastTitleToRegisteredPlayers(Component.text(countdown).color(color), Component.empty(), times);

                        // Play note
                        for (Player player : getOnlineRegisteredPlayers()) {
                            player.playNote(location, Instrument.PIANO, Note.natural(0, Note.Tone.F));
                        }
                    }
                    case 0 -> {

                        // Set world border
                        if(worldborderSize > 0) {
                            WorldBorder border = overworld.getWorldBorder();
                            border.setCenter(0, 0);
                            border.setSize(worldborderSize);
                        }

                        // Set difficulty
                        Difficulty difficulty = Difficulty.valueOf(plugin.getConfig().getString("difficulty"));
                        for(World world : plugin.getServer().getWorlds()) {
                            world.setDifficulty(difficulty);
                        }

                        // Set time
                        overworld.setTime(0);

                        // Send final title and sound
                        broadcastTitleToRegisteredPlayers(Component.text("0").color(DLDSColor.RED), Component.empty(), times);
                        for(Player player : getOnlineRegisteredPlayers()) {
                            player.playNote(location, Instrument.PIANO, Note.natural(1, Note.Tone.F));
                            player.playSound(location, Sound.BLOCK_NOTE_BLOCK_IMITATE_ENDER_DRAGON, 1.0F, 1.0F);
                        }

                        // Create Scoreboards for all registered players, clear their inventory, set gamemode to survival, and fill hunger / HP, ...
                        for(Player player : getOnlineRegisteredPlayers()) {
                            PlayerData playerData = players.get(player.getUniqueId());
                            playerData.setRemainingTime(plugin.getConfig().getLong("playtime"));

                            resetPlayer(player);

                            // Create scoreboard
                            plugin.getScoreboardManager().createBoardForPlayers(player);
                        }
                        isCountdownRunning = false;
                        cancel();
                    }
                }

                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    public void resetPlayer(Player player) {
        // Revoke all advancements
        Iterator<Advancement> it = Bukkit.getServer().advancementIterator();
        while(it.hasNext()) {
            Advancement advancement = it.next();
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            for(String criteria : advancement.getCriteria()) {
                progress.revokeCriteria(criteria);
            }
        }

        // Set gamemode
        player.setGameMode(GameMode.SURVIVAL);

        // Reset player to clean state
        player.getInventory().clear();
        player.getInventory().setItem(4, new ItemStack(Material.CLOCK, 1));
        player.updateInventory();
        player.setHealth(20D);
        player.setFoodLevel(20);
        player.setSaturation(5);
        player.setExperienceLevelAndProgress(0);
        player.clearActivePotionEffects();
        player.setWalkSpeed(0.2f);
        player.setFireTicks(0);

    }

    public void startTimers() {
        if(isTimerRunning) {
            return;
        }
        isTimerRunning = true;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            if(isGameRunning) {
                for (PlayerData playerData : players.values()) {
                    Player player = plugin.getServer().getPlayer(playerData.getUuid());
                    if (player != null && player.isOnline()) {

                        if(playerData.getRemainingTime() == 0L && plugin.getConfig().getBoolean("timeout_kick")) {
                            plugin.getComponentLogger().info("{} has no time left and is kicked", player.getName());

                            int currentPoints = getCurrentPoints();
                            int maxPoints = getMaxPoints();

                            int currentAdvancements = getCurrentAdvancementAmount();
                            int maxAdvancements = getMaxAdvancementAmount();

                            player.kick(
                                    DLDSComponents.getPlayerTimeoutKickMessage(currentPoints, maxPoints, currentAdvancements, maxAdvancements)
                            );
                        }

                        long remainingTime = playerData.getRemainingTime();
                        playerData.setRemainingTime(remainingTime - 1);

                    }
                }
            }
        }, 0L, 20L);
    }

    public boolean stopGame() {
        if(!isGameRunning) {
            return false;
        }
        isGameRunning = false;

        // Reset world border
        World overworld = plugin.getServer().getWorlds().getFirst();
        overworld.getWorldBorder().reset();

        broadcastMessageToRegisteredPlayers(DLDSComponents.stopSuccess());

        // Reset registered players
        for(PlayerData playerData : players.values()) {
            Player player = plugin.getServer().getPlayer(playerData.getUuid());
            if(player != null) {
                plugin.getScoreboardManager().deleteBoardForPlayers(player);
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1F, 1F);
            }
        }
        this.players.clear();
        return true;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if(event.getEntity() instanceof Player player) {
            if(players.containsKey(player.getUniqueId()) && isCountdownRunning) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        if(players.containsKey(event.getPlayer().getUniqueId()) && isCountdownRunning) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if(players.containsKey(event.getPlayer().getUniqueId()) && isCountdownRunning) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.renderer(((source, sourceDisplayName, message, viewer) ->
                Component.empty()
                                .append(sourceDisplayName.style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                                .append(text(" > ", DARK_GREY))
                                .append(message.color(LIGHT_GREY))
        ));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.joinMessage(DLDSComponents.playerJoinMessage(player));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = players.get(player.getUniqueId());

        // If the player is registered and kicked, check if we need to alter the quit message
        if(playerData != null && event.getReason().equals(PlayerQuitEvent.QuitReason.KICKED)) {

            // If the player just ran out of time, send a custom quit message
            if(plugin.getConfig().getBoolean("timeout_kick")
                    && (playerData.getRemainingTime() == 0L || playerData.getRemainingTime() == -1L)) {
                event.quitMessage(DLDSComponents.playerTimeoutQuitMessage(player));
                return;
            }

            // If the player is dead, do not send a kick message at all (normal death message displayed instead)
            if(plugin.getConfig().getBoolean("permadeath")
                    && playerData.isDead()) {
                event.quitMessage(null);
                return;
            }
        }

        // Normal quit message
        event.quitMessage(DLDSComponents.playerQuitMessage(player));
    }

    @EventHandler
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        handleAdvancement(event.getPlayer(), event.getAdvancement());
    }

    public void handleAdvancement(Player player, Advancement advancement) {
        PlayerData playerData = players.get(player.getUniqueId());
        NamespacedKey advancementKey = advancement.getKey();

        // Ignore recipes
        if(isRecipe(advancement)) {
            return;
        }

        // Ignore if player is not registered or game has not started yet
        if(playerData == null || !isGameRunning) {
            return;
        }

        // Check if anyone else has this advancement
        boolean isFirst = true;
        for(PlayerData pd : players.values()) {
            if(pd.getEarnedAdvancements().contains(advancementKey)) {
                isFirst = false;
                break;
            }
        }


        // Read rewards from configuration file
        ConfigurationSection rewardSection = getRewardsSection(advancement);
        if(rewardSection == null) {
            plugin.getComponentLogger().info("Player {} just received the advancement {} ({}), but there exists no reward section for it in the rewards.yml file!",
                    player.displayName(), advancement.getDisplay().title(), advancement.getKey().asString());
            return;
        }

        // Add advancement to player's list
        playerData.getEarnedAdvancements().add(advancementKey);

        // Give rewards if first
        if(isFirst) {
            List<ItemStack> rewards = getAdvancementRewards(rewardSection);
            int experience = getAdvancementExperience(rewardSection);
            awardItems(player, rewards);
            awardExperience(player, experience);

            broadcastMessageToRegisteredPlayers(DLDSComponents.newAdvancementMessage(player));
        }

        player.playNote(player.getLocation(), Instrument.PIANO, Note.natural(1, Note.Tone.A));

        // Send point notification to all registered players
        int points = getAdvancementPoints(rewardSection);
        broadCastActionBar(Component.text("+" + points + " ").color(DLDSColor.LIGHT_GREEN)
                .append(Component.text(points == 1 ? "Point" : "Points").color(DLDSColor.LIGHT_GREY)));

        // Update Scoreboards
        plugin.getScoreboardManager().updateBoards();
    }

    public void broadcastTitleToRegisteredPlayers(Component title, Component subtitle, Title.Times times) {
        for(Player player : getOnlineRegisteredPlayers()) {
            player.showTitle(Title.title(title, subtitle, times));
        }
    }

    public void broadcastMessageToRegisteredPlayers(Component... components) {
        for(Player player : getOnlineRegisteredPlayers()) {
            for(Component component : components) {
                player.sendMessage(component);
            }
        }
    }

    public List<Player> getOnlineRegisteredPlayers() {
        LinkedList<Player> res = new LinkedList<>();
        for(PlayerData playerData : players.values()) {
            Player player = plugin.getServer().getPlayer(playerData.getUuid());
            if (player != null && player.isOnline()) {
                res.add(player);
            }
        }
        return res;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        // Ignore if entity is not EnderDragon
        if(!(entity instanceof EnderDragon)) {
            return;
        }

        event.setDroppedExp(12000);

        long dragonRespawnDelay = plugin.getConfig().getLong("dragon_respawn_delay");
        plugin.getComponentLogger().info("Ender Dragon has been killed! Next spawn in {} minutes.", dragonRespawnDelay);
        Bukkit.getScheduler().runTaskLater(plugin, this::respawnEnderDragon, 20L * 60L * dragonRespawnDelay);
        Bukkit.getScheduler().runTaskLater(plugin, this::spawnDragonEgg, 20L * 15L);

        this.dragonRespawnTime = dragonRespawnDelay * 60L * 1000L + System.currentTimeMillis();
    }

    private void spawnDragonEgg() {
        World mainWorld = Bukkit.getWorlds().getFirst();
        World endWorld = Bukkit.getWorld(mainWorld.getName() + "_the_end");

        if(endWorld != null) {
            DragonBattle battle = endWorld.getEnderDragonBattle();
            if(battle != null) {
                Location endPortalLocation = battle.getEndPortalLocation();
                if(endPortalLocation != null) {
                    Block topBlock = endPortalLocation.clone().add(0, 4, 0).getBlock();
                    if(topBlock.getType() != Material.DRAGON_EGG) {
                        topBlock.setType(Material.DRAGON_EGG);
                    }
                }
            }
        }

    }

    public void respawnEnderDragon() {
        plugin.getComponentLogger().info("Respawning Ender Dragon!");

        World mainWorld = Bukkit.getWorlds().getFirst();
        String endWorldName = mainWorld.getName() + "_the_end";
        World endWorld = Bukkit.getWorld(endWorldName);

        final DragonBattle dragonBattle;
        if (endWorld != null) {
            dragonBattle = endWorld.getEnderDragonBattle();
            final Location endPortalLoc;
            if (dragonBattle != null) {
                endPortalLoc = dragonBattle.getEndPortalLocation();
                endWorld.spawnEntity(endPortalLoc.clone().add(3.5d, 1, 0.5), EntityType.END_CRYSTAL);
                endWorld.spawnEntity(endPortalLoc.clone().add(-2.5d, 1, 0.5), EntityType.END_CRYSTAL);
                endWorld.spawnEntity(endPortalLoc.clone().add(0.5, 1, 3.5d), EntityType.END_CRYSTAL);
                endWorld.spawnEntity(endPortalLoc.clone().add(0.5, 1, -2.5d), EntityType.END_CRYSTAL);
                dragonBattle.initiateRespawn();

                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        Location blockLocation = new Location(endWorld, endPortalLoc.getX() + x, endPortalLoc.getY(), endPortalLoc.getZ() + z);

                        if ((x == 0 && z == 0) || (Math.abs(x) == 2 && Math.abs(z) == 2)) {
                            continue;
                        }

                        Block block = blockLocation.getBlock();
                        block.setType(Material.END_PORTAL);
                    }
                }
            }

            new BukkitRunnable() {
                public void run() {
                    EnderDragon dragon = dragonBattle.getEnderDragon();

                    if (dragon != null) {
                        plugin.getComponentLogger().info("Ender Dragon respawned successfully!");
                        dragonRespawnTime = Long.MAX_VALUE;
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }



    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        PlayerData playerData = players.get(event.getPlayerProfile().getId());

        // Ignore OP player
        if(Bukkit.getServer().getOperators().stream().map(OfflinePlayer::getUniqueId).toList().contains(event.getPlayerProfile().getId())){
            return;
        }

        // Ignore if player is not registered or game is not running
        if(playerData == null || !isGameRunning) {
            return;
        }

        int currentPoints = getCurrentPoints();
        int maxPoints = getMaxPoints();

        int currentAdvancements = getCurrentAdvancementAmount();
        int maxAdvancements = getMaxAdvancementAmount();

        if(playerData.getRemainingTime() <= 0L && plugin.getConfig().getBoolean("timeout_kick")){
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    DLDSComponents.getPlayerTimeoutKickMessage(currentPoints, maxPoints, currentAdvancements, maxAdvancements)
            );
        } else if(playerData.isDead() && plugin.getConfig().getBoolean("permadeath")) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    DLDSComponents.getPlayerDeathKickMessage(currentPoints, maxPoints, currentAdvancements, maxAdvancements)
            );
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData playerData = players.get(player.getUniqueId());

        event.deathMessage(DLDSComponents.playerDeathMessage(player));

        // Play thunder sound for all other players
        for(Player otherplayer : getOnlineRegisteredPlayers()) {
            if(!otherplayer.getUniqueId().equals(player.getUniqueId())) {
                otherplayer.playSound(otherplayer.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
            }
        }

        // Ignore if permadeath is off
        if(!plugin.getConfig().getBoolean("permadeath")) {
            return;
        }

        // Ignore if player is not registered or game is not running
        if(playerData == null || !isGameRunning) {
            return;
        }

        int currentPoints = getCurrentPoints();
        int maxPoints = getMaxPoints();

        int currentAdvancements = getCurrentAdvancementAmount();
        int maxAdvancements = getMaxAdvancementAmount();

        playerData.setDead(true);
        player.kick(
                DLDSComponents.getPlayerDeathKickMessage(currentPoints, maxPoints, currentAdvancements, maxAdvancements)
        );


    }

    private void broadCastActionBar(Component component) {
        for(PlayerData pd : players.values()) {
            Player player = plugin.getServer().getPlayer(pd.getUuid());
            if(player != null) {
                player.sendActionBar(component);
            }
        }
    }

    private void teleportPlayers(Location location) {
        World overworld = plugin.getServer().getWorlds().getFirst();

        for(Player player : getOnlineRegisteredPlayers()) {
            int deltaX, deltaZ;
            int tries = 0;
            Block highestBlock;
            do {
                tries++;
                deltaX = (int) (Math.random() * 10 - 5);
                deltaZ = (int) (Math.random() * 10 - 5);
                highestBlock = overworld.getHighestBlockAt(location.getBlockX() + deltaX, location.getBlockZ() + deltaZ);
            } while(highestBlock.isLiquid() && tries < 5);

            // Teleport player to original random position if we could not find a suitable random offset
            if(tries == 5) {
                player.teleport(location);
                player.setRespawnLocation(location, true);
                continue;
            }

            // Teleport player to random offset location
            Location randomOffsetLoc = highestBlock.getLocation().add(0, 1, 0);
            player.teleport(randomOffsetLoc);
            player.setRespawnLocation(randomOffsetLoc, true);
        }
    }

    private Location getRandomSpawnLocation(double size) {
        World overworld = plugin.getServer().getWorlds().getFirst();

        Location randomLoc;
        do {
            randomLoc = getRandomLocation(overworld, size).add(0, 1, 0);
        } while (overworld.getBiome(randomLoc).getKey().asString().contains("ocean") || overworld.getHighestBlockAt(randomLoc).isLiquid());

        return randomLoc;
    }

    private Location getRandomLocation(World world, double size) {
        double x = (Math.random() * size) - (size / 2);
        double z = (Math.random() * size) - (size / 2);
        int y = world.getHighestBlockYAt((int) x, (int) z);
        return new Location(world, x, y, z);
    }

    private boolean isRecipe(Advancement advancement) {
        String key = advancement.getKey().getKey();
        return key.split("/")[0].equals("recipes");
    }

    private ConfigurationSection getRewardsSection(Advancement advancement) {
        String path = advancement.getKey().getNamespace() + "." + advancement.getKey().getKey().replace('/', '.');
        return plugin.getRewardConfig().getConfigurationSection(path);
    }

    public ConfigurationSection getRewardsSection(NamespacedKey key) {
        String path = key.getNamespace() + "." + key.getKey().replace('/', '.');
        return plugin.getRewardConfig().getConfigurationSection(path);
    }

    public int getAdvancementPoints(ConfigurationSection rewardSection) {
        return rewardSection.getInt("points", 0);
    }

    public int getAdvancementExperience(ConfigurationSection rewardsSection) {
        return rewardsSection.getInt("exp", 0);
    }

    public List<ItemStack> getAdvancementRewards(ConfigurationSection rewardSection) {
        List<ItemStack> rewards = new LinkedList<>();

        for(String rewardType : rewardSection.getKeys(false)) {

            switch (rewardType) {
                case "items" -> {
                    ConfigurationSection itemRewards = rewardSection.getConfigurationSection(rewardType);

                    for(String item : itemRewards.getKeys(false)) {
                        int amount = itemRewards.getInt(item);
                        Material material = Material.getMaterial(item);

                        if(material == null) {
                            plugin.getComponentLogger().warn("Material for item {} not found! Advancement: {}", item, rewardSection.getCurrentPath());
                            break;
                        }
                        rewards.add(new ItemStack(material, amount));
                    }
                }
                case "potion" -> {
                    ItemStack itemStack = new ItemStack(Material.POTION);
                    PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
                    meta.setBasePotionType(PotionType.valueOf(rewardSection.getString(rewardType)));
                    itemStack.setItemMeta(meta);

                    rewards.add(itemStack);
                }
                case "book" -> {
                    ConfigurationSection bookReward = rewardSection.getConfigurationSection(rewardType);

                    String enchantmentName = bookReward.getString("enchantment");
                    int level = bookReward.getInt("level", 1);

                    if(enchantmentName == null) {
                        plugin.getComponentLogger().warn("No enchantment specified for book in advancement: {}", rewardSection.getCurrentPath());
                        break;
                    }

                    ItemStack itemStack = new ItemStack(Material.ENCHANTED_BOOK);
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemStack.getItemMeta();
                    Enchantment enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                            .get(NamespacedKey.minecraft(enchantmentName.toLowerCase()));

                    if(enchantment == null) {
                        plugin.getComponentLogger().warn("Enchantment {} not found! Advancement: {}", rewardSection.getString(rewardType), rewardSection.getCurrentPath());
                        break;
                    }

                    meta.addStoredEnchant(enchantment, level, true);
                    itemStack.setItemMeta(meta);

                    rewards.add(itemStack);
                }
                case "bad_omen" -> {
                    ItemStack itemStack = new ItemStack(Material.OMINOUS_BOTTLE);
                    OminousBottleMeta meta = (OminousBottleMeta) itemStack.getItemMeta();
                    meta.setAmplifier(rewardSection.getInt(rewardType)-1);
                    itemStack.setItemMeta(meta);

                    rewards.add(itemStack);
                }
            }
        }
        return rewards;
    }

    private void awardItems(Player player, ItemStack... items) {
        awardItems(player, Arrays.asList(items));
    }

    private void awardItems(Player player, List<ItemStack> items) {
        List<ItemStack> remainder = new LinkedList<>();

        for(ItemStack item : items) {
            Map<Integer, ItemStack> didNotFit = player.getInventory().addItem(item);
            remainder.addAll(didNotFit.values());
        }
        player.updateInventory();

        if(!remainder.isEmpty()) {
            player.sendMessage(DLDSComponents.rewardInventoryFull());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);

            for(ItemStack item : remainder) {
                // Drop on the ground
                Item drop = player.getWorld().dropItem(player.getLocation(), item);
                drop.setVelocity(new Vector(0, 0.2, 0));
            }
        }
    }

    private void awardExperience(Player player, int experience) {
        if(experience <= 0) {
            return;
        }

        player.giveExp(experience);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f, 1f);
    }

    public void playErrorSound(Entity entity) {
        if(entity instanceof Player player) {
            playErrorSound(player);
        }
    }

    public void playErrorSound(Player player) {
        player.playNote(player.getLocation(), Instrument.DIDGERIDOO, Note.sharp(0, Note.Tone.F));
    }

    public Set<UUID> getRegisteredUUIDs(){
        return players.keySet();
    }

    public Map<UUID, PlayerData> getPlayers() {
        return players;
    }

    public boolean isGameRunning() {
        return isGameRunning;
    }

    public void setGameRunning(boolean gameRunning) {
        isGameRunning = gameRunning;
    }

    public void setPlayers(Map<UUID, PlayerData> players) {
        this.players = players;
    }

    public int getCurrentPoints() {
        int res = 0;
        for(PlayerData data : players.values()) {
            for(NamespacedKey key : data.getEarnedAdvancements()) {
                res += getAdvancementPoints(getRewardsSection(key));
            }
        }
        return res;
    }

    public int getMaxPoints() {
        int res = 0;
        List<String> configKey = plugin.getRewardConfig().getKeys(true).stream().filter(s -> s.endsWith(".points")).toList();

        for (String key : configKey) {
            res += plugin.getRewardConfig().getInt(key);
        }
        return res * players.size();
    }

    public int getCurrentAdvancementAmount() {
        int res = 0;
        for(PlayerData data : players.values()) {
            res += data.getEarnedAdvancements().size();
        }
        return res;
    }

    public int getMaxAdvancementAmount() {
        //TODO: replace hardcoded value
        return 118*players.size();
    }

    private int computeTotalAdvancementPoints() {
        int res = 0;
        List<String> configKeys = plugin.getRewardConfig().getKeys(true).stream().filter(s -> s.endsWith(".points")).toList();

        for (String key : configKeys) {
            res += plugin.getRewardConfig().getInt(key);
        }
        return res;
    }

    public Set<DLDSTeam> getTeams() {
        return teams;
    }

    public Optional<DLDSTeam> getTeam(Player player) {
        return teams.stream().filter(team -> team.containsPlayer(player)).findFirst();
    }

    public Optional<DLDSTeam> getTeam(String teamName) {
        return teams.stream().filter(team -> team.getName().equalsIgnoreCase(teamName)).findFirst();
    }

    private int computeTotalAdvancementCount() {
        List<String> configKeys = plugin.getRewardConfig().getKeys(true).stream().filter(s -> s.endsWith(".points")).toList();
        return configKeys.size();
    }

    public int getTotalAdvancementCount() {
        return totalAdvancementCount;
    }

    public int getTotalAdvancementPoints() {
        return totalAdvancementPoints;
    }

    public long getDragonRespawnTime() {
        return dragonRespawnTime;
    }

    public void setDragonRespawnTime(long dragonRespawnTime) {
        this.dragonRespawnTime = dragonRespawnTime;
    }

    public boolean setTimeForPlayer(Player player, int hours, int minutes, int seconds) {
        PlayerData playerData = players.get(player.getUniqueId());

        if(playerData == null) {
            // Player not registered
            return false;
        }

        // Set remaining time
        long totalSeconds = (hours * 3600L) + (minutes * 60L) + seconds;
        playerData.setRemainingTime(totalSeconds);
        return true;
    }
}