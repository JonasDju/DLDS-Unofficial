package eu.bitflare.dlds;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import eu.bitflare.dlds.exceptions.*;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static eu.bitflare.dlds.DLDSColor.DARK_GREY;
import static eu.bitflare.dlds.DLDSColor.LIGHT_GREY;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

public class GameManager implements Listener {

    private static GameManager instance;

    private DLDSPlugin plugin;
    private Set<DLDSTeam> teams;
    private boolean isTimerRunning;
    private long dragonRespawnTime;

    private int totalAdvancementPoints;
    private int totalAdvancementCount;

    private GameManager() {
        this.teams = new HashSet<>();
        this.isTimerRunning = false;
        this.dragonRespawnTime = Long.MAX_VALUE;
    }

    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public void init(DLDSPlugin plugin) {
        this.plugin = plugin;
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

        teams.add(new DLDSTeam(teamName));
    }

    public void removeTeam(String teamName) throws TeamNotFoundException {
        // Check if team even exists
        Optional<DLDSTeam> team = getTeam(teamName);
        if(team.isEmpty()) {
            throw new TeamNotFoundException(teamName);
        }

        // Check if the team is currently playing
        if(team.get().isPlaying()) {
            throw new TeamCurrentlyPlayingException(team.get());
        }

        teams.remove(team.get());
    }

    public void addPlayerToTeam(Player player, String teamName) throws TeamNotFoundException, TeamCurrentlyPlayingException {
        // Check if team exists
        Optional<DLDSTeam> targetTeam = getTeam(teamName);
        if(targetTeam.isEmpty()) {
            throw new TeamNotFoundException(teamName);
        }

        // Check if team is already playing
        if(targetTeam.get().isPlaying()) {
            throw new TeamCurrentlyPlayingException(targetTeam.get());
        }

        // Remove player from current team (if applicable)
        Optional<DLDSTeam> currentTeam = getTeam(player);
        if(currentTeam.isPresent()) {
            if(currentTeam.get().isPlaying()) {
                throw new TeamCurrentlyPlayingException(currentTeam.get());
            }
            currentTeam.get().removePlayer(player);
        }

        // Add player to team
        targetTeam.get().addPlayer(player);
    }

    public DLDSTeam removePlayerFromTeams(Player player) throws PlayerNotInTeamException, TeamCurrentlyPlayingException {
        Optional<DLDSTeam> team = getTeam(player);
        if(team.isPresent()) {
            if(team.get().isPlaying()) {
                throw new TeamCurrentlyPlayingException(team.get());
            } else {
                team.get().removePlayer(player);
                return team.get();
            }
        } else {
            throw new PlayerNotInTeamException(player, null);
        }
    }

    public void startGame(String teamName) throws TeamNotFoundException, TeamCurrentlyPlayingException, SomePlayersAreOfflineException, EmptyTeamException {
        // Check if team even exists
        Optional<DLDSTeam> targetTeam = getTeam(teamName);
        if(targetTeam.isEmpty()) {
            throw new TeamNotFoundException(teamName);
        }

        // Check if the team is empty
        if(targetTeam.get().getPlayers().isEmpty()) {
            throw new EmptyTeamException(targetTeam.get());
        }

        // Change world settings if this is the first team to start playing
        if(!isGameRunning()) {
            World overworld = plugin.getServer().getWorlds().getFirst();
            int worldborderSize = plugin.getConfig().getInt("worldborder");

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
        }

        // Start game for team
        targetTeam.get().startGame();
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

            for(DLDSTeam team : getPlayingTeams()) {
                for(PlayerData playerData : team.getPlayers()) {
                    Player player = plugin.getServer().getPlayer(playerData.getUuid());
                    if(player != null && player.isOnline()) {

                        if(playerData.getRemainingTime() == 0L && plugin.getConfig().getBoolean("timeout_kick")) {
                            plugin.getComponentLogger().info("{} has no time left and is kicked", player.getName());

                            int currentPoints = team.getCurrentPoints();
                            int maxPoints = team.getAchievablePoints();

                            int currentAdvancements = team.getCurrentAdvancementAmount();
                            int maxAdvancements = team.getAchievableAdvancementAmount();

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

    public void stopGame(String teamName) throws TeamNotFoundException, TeamNotPlayingException {
        // Check if team even exists
        Optional<DLDSTeam> targetTeam = getTeam(teamName);
        if(targetTeam.isEmpty()) {
            throw new TeamNotFoundException(teamName);
        }

        targetTeam.get().stopGame();

        // Reset world if this was the last team playing
        if(!isGameRunning()) {
            World overworld = plugin.getServer().getWorlds().getFirst();
            overworld.getWorldBorder().reset();
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {

        // Only consider events in which a player is damaged
        if(event.getEntity() instanceof Player player) {

            // Prevent players from taking damage while their countdown is running
            Optional<DLDSTeam> team = getTeam(player);
            if(team.isPresent() && team.get().isCountdownRunning()) {
                event.setCancelled(true);
            }

            // Prevent "bystanders" from taking damage by the world border
            if(team.isEmpty() && event.getCause().equals(EntityDamageEvent.DamageCause.WORLD_BORDER)) {
                player.sendActionBar(text().content("You are outside the world border!").color(DLDSColor.RED));
                event.setCancelled(true);
            }

        }
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {

        // Prevent players from jumping while their countdown is running
        Optional<DLDSTeam> team = getTeam(event.getPlayer());
        if(team.isPresent() && team.get().isCountdownRunning()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        // Prevent players from breaking blocks while their countdown is running
        Optional<DLDSTeam> team = getTeam(event.getPlayer());
        if(team.isPresent() && team.get().isCountdownRunning()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Optional<DLDSTeam> teamOpt = getTeam(event.getPlayer());
        final Component teamPrefix = teamOpt.map(dldsTeam -> empty()
                .append(text(dldsTeam.getName(), DLDSColor.LIGHT_BLUE))
                .append(text(" | ", DARK_GREY))
        ).orElseGet(Component::empty);

        event.renderer(((source, sourceDisplayName, message, viewer) ->
                Component.empty()
                        .append(teamPrefix)
                                .append(sourceDisplayName.style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                                .append(text(" > ", DARK_GREY))
                                .append(message.color(LIGHT_GREY))
        ));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.joinMessage(DLDSComponents.playerJoinMessage(player, getTeam(player).orElse(null)));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Optional<DLDSTeam> team = getTeam(player);

        // If the player is in a team that is currently playing and the player is kicked, we might want to change the quit message
        if(team.isPresent() && team.get().isPlaying() && event.getReason().equals(PlayerQuitEvent.QuitReason.KICKED)) {

            // Get playerData associated with the player
            // Cannot be empty since we are operating on the current team of the player
            Optional<PlayerData> playerData = team.get().getPlayerData(player);
            if(playerData.isPresent()) {

                // If the player just ran out of time, send a custom quit message
                if(plugin.getConfig().getBoolean("timeout_kick")
                        && (playerData.get().getRemainingTime() == 0L || playerData.get().getRemainingTime() == -1L)) {
                    event.quitMessage(DLDSComponents.playerTimeoutQuitMessage(player));
                    return;
                }

                // If the player is dead, do not send a kick message at all (normal death message displayed instead)
                if(plugin.getConfig().getBoolean("permadeath")
                        && playerData.get().isDead()) {
                    event.quitMessage(null);
                    return;
                }
            }
        }

        // Normal quit message
        event.quitMessage(DLDSComponents.playerQuitMessage(player, team.orElse(null)));
    }

    @EventHandler
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        handleAdvancement(event.getPlayer(), event.getAdvancement());
    }

    public void handleAdvancement(Player player, Advancement advancement) {
        NamespacedKey advancementKey = advancement.getKey();

        // Ignore recipes
        if(isRecipe(advancement)) {
            return;
        }

        // Check if the player is in a team and if the team is currently playing
        Optional<DLDSTeam> teamOpt = getTeam(player);
        if(teamOpt.isEmpty() || !teamOpt.get().isPlaying()) {
            return;
        }
        DLDSTeam team = teamOpt.get();

        // Get player data
        Optional<PlayerData> playerDataOpt = team.getPlayerData(player);
        if(playerDataOpt.isEmpty()) {
            return;
        }
        PlayerData playerData = playerDataOpt.get();

        // Check if anyone else in the players' team has this advancement
        boolean isFirst = !team.hasAdvancement(advancementKey);

        // Read rewards from configuration file
        ConfigurationSection rewardSection = getRewardsSection(advancement);
        if(rewardSection == null) {
            plugin.getComponentLogger().info("Player {} just received the advancement {} ({}), but there exists no reward section for it in the rewards.yml file!",
                    player.displayName(), advancement.getDisplay().title(), advancement.getKey().asString());
            return;
        }

        // Add advancement to players' list
        playerData.getEarnedAdvancements().add(advancementKey);

        // Give rewards if first
        if(isFirst) {
            List<ItemStack> rewards = getAdvancementRewards(rewardSection);
            int experience = getAdvancementExperience(rewardSection);
            awardItems(player, rewards);
            awardExperience(player, experience);

            team.broadcastMessage(DLDSComponents.newAdvancementMessage(player));
        }

        player.playNote(player.getLocation(), Instrument.PIANO, Note.natural(1, Note.Tone.A));

        // Send point notification to all players in the team
        int points = getAdvancementPoints(rewardSection);
        team.broadcastActionBar(Component.text("+" + points + " ").color(DLDSColor.LIGHT_GREEN)
                .append(Component.text(points == 1 ? "Point" : "Points").color(DLDSColor.LIGHT_GREY)));

        // Update Scoreboards
        plugin.getScoreboardManager().updateBoards();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        // Ignore if entity is not Ender Dragon
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
        UUID uuid = event.getPlayerProfile().getId();

        // Ignore OP player
        if(Bukkit.getServer().getOperators().stream().map(OfflinePlayer::getUniqueId).toList().contains(event.getPlayerProfile().getId())){
            return;
        }

        // Ignore if player is not in a team or the team is not playing
        Optional<DLDSTeam> teamOpt = getTeam(uuid);
        if(teamOpt.isEmpty() || !teamOpt.get().isPlaying()) {
            return;
        }
        DLDSTeam team = teamOpt.get();

        // Get player data
        Optional<PlayerData> playerDataOpt = team.getPlayerData(uuid);
        if(playerDataOpt.isEmpty()) {
            return;
        }
        PlayerData playerData = playerDataOpt.get();

        int currentPoints = team.getCurrentPoints();
        int maxPoints = team.getAchievablePoints();

        int currentAdvancements = team.getCurrentAdvancementAmount();
        int maxAdvancements = team.getAchievableAdvancementAmount();

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
        Optional<DLDSTeam> teamOpt = getTeam(player);

        event.deathMessage(DLDSComponents.playerDeathMessage(player, teamOpt.orElse(null)));

        // Play thunder sound for all other players of the team
        if(teamOpt.isPresent() && teamOpt.get().isPlaying()) {
            for(Player otherplayer : teamOpt.get().getOnlinePlayers()) {
                if(!otherplayer.getUniqueId().equals(player.getUniqueId())) {
                    otherplayer.playSound(otherplayer.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
                }
            }
        }

        // Ignore if permadeath is off
        if(!plugin.getConfig().getBoolean("permadeath")) {
            return;
        }

        // Ignore if player is in a team or the team is not playing
        if(teamOpt.isEmpty() || !teamOpt.get().isPlaying()) {
            return;
        }
        DLDSTeam team = teamOpt.get();

        int currentPoints = team.getCurrentPoints();
        int maxPoints = team.getAchievablePoints();

        int currentAdvancements = team.getCurrentAdvancementAmount();
        int maxAdvancements = team.getAchievableAdvancementAmount();

        Optional<PlayerData> playerDataOpt = team.getPlayerData(player);
        playerDataOpt.ifPresent(playerData -> playerData.setDead(true));

        player.kick(
                DLDSComponents.getPlayerDeathKickMessage(currentPoints, maxPoints, currentAdvancements, maxAdvancements)
        );


    }

    public void teleportPlayersDistributed(Location location, Collection<Player> players) {
        World overworld = plugin.getServer().getWorlds().getFirst();

        for(Player player : players) {
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
                player.teleport(location.clone().add(0.5, 0, 0.5));
                player.setRespawnLocation(location, true);
                continue;
            }

            // Teleport player to random offset location
            Location randomOffsetLoc = highestBlock.getLocation().add(0.5, 1, 0.5);
            player.teleport(randomOffsetLoc);
            player.setRespawnLocation(randomOffsetLoc, true);
        }
    }

    public Location getRandomSpawnLocation(double size) {
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

    public void setTeams(Set<DLDSTeam> teams) {
        this.teams = teams;
    }

    public Optional<DLDSTeam> getTeam(Player player) {
        return getTeam(player.getUniqueId());
    }

    public Optional<DLDSTeam> getTeam(UUID uuid) {
        return teams.stream().filter(team -> team.containsPlayer(uuid)).findFirst();
    }

    public Optional<DLDSTeam> getTeam(String teamName) {
        return teams.stream().filter(team -> team.getName().equalsIgnoreCase(teamName)).findFirst();
    }

    public List<DLDSTeam> getPlayingTeams() {
        return teams.stream().filter(DLDSTeam::isPlaying).toList();
    }

    private int computeTotalAdvancementCount() {
        List<String> configKeys = plugin.getRewardConfig().getKeys(true).stream().filter(s -> s.endsWith(".points")).toList();
        return configKeys.size();
    }

    /**
     * @return true if at least one team is currently playing, otherwise false
     */
    public boolean isGameRunning() {
        return teams.stream().anyMatch(DLDSTeam::isPlaying);
    }

    public DLDSPlugin getPlugin() {
        return plugin;
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

    public Optional<PlayerData> getPlayerData(Player player) {
        Optional<DLDSTeam> team = getTeam(player);
        if(team.isEmpty()) {
            return Optional.empty();
        }
        return team.get().getPlayerData(player);
    }

    public void setTimeForPlayer(Player player, int hours, int minutes, int seconds) {
        Optional<PlayerData> playerData = getPlayerData(player);

        if(playerData.isEmpty()) {
            // Player not in any team
            throw new PlayerNotInTeamException(player, null);
        }

        // Set remaining time
        long totalSeconds = (hours * 3600L) + (minutes * 60L) + seconds;
        playerData.get().setRemainingTime(totalSeconds);
    }
}