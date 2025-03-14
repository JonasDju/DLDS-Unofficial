package eu.bitflare.dlds;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.OminousBottleMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class GameManager implements Listener {

    private final DLDSPlugin plugin;
    private Map<UUID, PlayerData> players;
    private boolean isGameRunning;


    public GameManager(DLDSPlugin plugin) {
        this.plugin = plugin;
        this.players = new HashMap<>();
        this.isGameRunning = false;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void registerPlayer(Player player) {
        if(isGameRunning) {
            player.sendPlainMessage("Error: You cannot enter because DLDS is already running!");
            return;
        }

        UUID uuid = player.getUniqueId();
        if(players.containsKey(uuid)) {
            player.sendPlainMessage("Error: You have already entered the event!");
        } else {
            players.put(uuid, new PlayerData(uuid, player.getName()));
            player.sendPlainMessage("You have entered the event!");
        }
    }

    public boolean startGame() {
        if(isGameRunning) {
            return false;
        }

        Bukkit.broadcast(Component.text("DLDS will start soon!"));
        isGameRunning = true;

        // Set world border
        World overworld = plugin.getServer().getWorlds().getFirst();
        int size = plugin.getConfig().getInt("worldborder");
        if(size > 0) {
            WorldBorder border = overworld.getWorldBorder();
            border.setCenter(0, 0);
            border.setSize(size);
        }

        // Set difficulty
        Difficulty difficulty = Difficulty.valueOf(plugin.getConfig().getString("difficulty"));
        for(World world : plugin.getServer().getWorlds()) {
            world.setDifficulty(difficulty);v
        }

        // Set time
        overworld.setTime(0);

        // Start countdown and teleport players
        new BukkitRunnable() {

            private int countdown = 5;

            @Override
            public void run() {
                if(countdown > 0) {
                    for(PlayerData playerData : players.values()) {
                        Player player = plugin.getServer().getPlayer(playerData.getUuid());

                        if(player != null) {
                            Title title = Title.title(Component.text(String.valueOf(countdown)).color(DLDSColor.RED), Component.text(""));
                            player.showTitle(title);
                        }

                    }
                    countdown--;
                } else {
                    //TODO: Start player timers
                    teleportPlayersRandomly();
                    removeAllAdvancements();

                    // Create Scoreboards for all registered players, clear their inventory, set gamemode to survival, and fill hunger / HP
                    for(PlayerData playerData : players.values()) {
                        Player player = plugin.getServer().getPlayer(playerData.getUuid());
                        if(player != null) {
                            plugin.getScoreboardManager().createBoardForPlayers(player);
                            player.getInventory().clear();
                            player.updateInventory();
                            player.setGameMode(GameMode.SURVIVAL);
                            player.setHealth(20D);
                            player.setFoodLevel(20);
                        }
                    }

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    public boolean stopGame() {
        if(!isGameRunning) {
            return false;
        }
        isGameRunning = false;

        Bukkit.broadcast(Component.text("DLDS has been stopped!"));

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
            plugin.getComponentLogger().info(advancementKey.asString());
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

            Bukkit.broadcast(player.displayName().append(Component.text(" has earned an advancement and got some rewards!")));
        }


        // Send point notification to all registered players
        int points = getAdvancementPoints(rewardSection);
        broadCastActionBar(Component.text("+" + points + " ").color(DLDSColor.LIGHT_GREEN)
                .append(Component.text(points == 1 ? "Point" : "Points").color(DLDSColor.LIGHT_GREY)));

        // Update Scoreboards
        plugin.getScoreboardManager().updateBoards();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        // Ignore if entity is not EnderDragon
        if(!(entity instanceof EnderDragon)) {
            return;
        }

        Entity source = event.getDamageSource().getCausingEntity();
        ItemStack reward = new ItemStack(Material.END_CRYSTAL, 4);
        if(source instanceof Player player) {
            player.sendPlainMessage("Since Ender Drake respawn is currently not implemented, you receive 4 end cystrals to respawn it yourself.");
            awardItems(player, reward);
        } else {
            Bukkit.broadcast(Component.text("Since Ender Drake respawn is currently not implemented, you receive 4 end cystrals to respawn it yourself."));
            event.getEntity().getWorld().dropItem(event.getEntity().getLocation(), reward);
        }

    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        PlayerData playerData = players.get(event.getPlayerProfile().getId());

        // Ignore if permadeath is off
        if(!plugin.getConfig().getBoolean("permadeath")) {
            return;
        }

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

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                Component.text("Unofficial DLDS").style(Style.style(DLDSColor.DARK_GREEN, TextDecoration.BOLD))
                        .appendNewline().appendNewline().append(
                                Component.text("You ").color(DLDSColor.LIGHT_GREY).append(
                                        Component.text("died").color(DLDSColor.RED)
                                ).append(Component.text("!"))
                        ).appendNewline().appendNewline().append(
                                Component.text("Points: ").color(DLDSColor.LIGHT_GREY).append(
                                                Component.text(currentPoints, DLDSColor.YELLOW))
                                        .append(Component.text(" / ", DLDSColor.DARK_GREY))
                                        .append(Component.text(maxPoints, DLDSColor.YELLOW)
                                        )
                        ).appendNewline().append(
                                Component.text("Advancements: ").color(DLDSColor.LIGHT_GREY).append(
                                                Component.text(currentAdvancements, DLDSColor.YELLOW))
                                        .append(Component.text(" / ", DLDSColor.DARK_GREY))
                                        .append(Component.text(maxAdvancements, DLDSColor.YELLOW)
                                        )
                        )
                );

    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData playerData = players.get(player.getUniqueId());

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
                Component.text("Unofficial DLDS").style(Style.style(DLDSColor.DARK_GREEN, TextDecoration.BOLD))
                        .appendNewline().appendNewline().append(
                                Component.text("You ").color(DLDSColor.LIGHT_GREY).append(
                                        Component.text("died").color(DLDSColor.RED)
                                ).append(Component.text("!"))
                        ).appendNewline().appendNewline().append(
                                Component.text("Points: ").color(DLDSColor.LIGHT_GREY).append(
                                        Component.text(currentPoints, DLDSColor.YELLOW))
                                        .append(Component.text(" / ", DLDSColor.DARK_GREY))
                                        .append(Component.text(maxPoints, DLDSColor.YELLOW)
                                )
                        ).appendNewline().append(
                                Component.text("Advancements: ").color(DLDSColor.LIGHT_GREY).append(
                                                Component.text(currentAdvancements, DLDSColor.YELLOW))
                                        .append(Component.text(" / ", DLDSColor.DARK_GREY))
                                        .append(Component.text(maxAdvancements, DLDSColor.YELLOW)
                                        )
                        )
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

    private void removeAllAdvancements() {
        for(PlayerData playerData : players.values()) {
            Player player = plugin.getServer().getPlayer(playerData.getUuid());
            if(player != null) {

                Iterator<Advancement> it = Bukkit.getServer().advancementIterator();
                while(it.hasNext()) {
                    Advancement advancement = it.next();
                    AdvancementProgress progress = player.getAdvancementProgress(advancement);
                    for(String criteria : advancement.getCriteria()) {
                        progress.revokeCriteria(criteria);
                    }
                }
            }
        }
    }

    private void teleportPlayersRandomly() {
        World overworld = plugin.getServer().getWorlds().getFirst();

        Location randomLoc;
        do {
            randomLoc = getRandomLocation(overworld).add(0, 1, 0);
        } while (overworld.getBiome(randomLoc).getKey().asString().contains("ocean"));

        for (PlayerData playerData : players.values()) {
            Player player = plugin.getServer().getPlayer(playerData.getUuid());
            if (player != null) {
                player.teleportAsync(randomLoc);
                player.setRespawnLocation(randomLoc, true);
            }
        }
    }

    private Location getRandomLocation(World world) {
        WorldBorder border = world.getWorldBorder();
        double size = border.getSize() / 2;
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

    private ConfigurationSection getRewardsSection(NamespacedKey key) {
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
            player.sendPlainMessage("Warning: Your inventory is full! All remaining rewards have been dropped at your location.");
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

}
