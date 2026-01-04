package rip.diamond.practice.match.listener.kit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rip.diamond.practice.Eden;
import rip.diamond.practice.match.Match;
import rip.diamond.practice.match.MatchState;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.PlayerState;

/**
 * Handles TNT Sumo specific match logic
 * Registered by MatchListener to keep gamemode-specific code separate
 * 
 * ALL events in this listener only fire for players in active TNT Sumo matches
 */
public class TNTSumoMatchListener implements Listener {

    private final Eden plugin;
    
    // Track blocks we manually returned items for to prevent duplicate drops
    // Key: Block location, Value: Info about expected drops and when they happened
    private final java.util.Map<String, BlockDropInfo> expectedDrops = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Helper class to store drop info
    private static class BlockDropInfo {
        final java.util.Set<org.bukkit.Material> expectedMaterials;
        final long timestamp;
        
        BlockDropInfo(java.util.Collection<org.bukkit.inventory.ItemStack> items) {
            this.expectedMaterials = items.stream()
                    .map(org.bukkit.inventory.ItemStack::getType)
                    .collect(java.util.stream.Collectors.toSet());
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 200; // 200ms = ~4 ticks window
        }
    }
    
    public TNTSumoMatchListener(Eden plugin) {
        this.plugin = plugin;
    }

    /**
     * Validates that the player is in an active TNT Sumo match
     * Use this at the start of EVERY event handler
     * 
     * @param player The player to validate
     * @return The Match object if valid, null otherwise
     */
    private Match validateTNTSumoMatch(Player player) {
        if (player == null) {
            return null;
        }
        
        PlayerProfile profile = PlayerProfile.get(player);
        if (profile == null) {
            return null;
        }
        
        // Must be in a match
        if (profile.getPlayerState() != PlayerState.IN_MATCH) {
            return null;
        }
        
        Match match = profile.getMatch();
        if (match == null) {
            return null;
        }
        
        // Match must be actively fighting (not starting/ending)
        if (match.getState() != MatchState.FIGHTING) {
            return null;
        }
        
        // Kit must have TNT Sumo enabled
        if (!match.getKit().getGameRules().isTntsumo()) {
            return null;
        }
        
        return match;
    }

    // ========================================
    // TNT SUMO EVENT HANDLERS
    // ========================================
    
    /**
     * Give +1 Insta Boom TNT when damaging an opponent
     * Only if current TNT count is below max limit
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageOpponent(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        // Only handle player vs player damage
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        // Validate damager is in TNT Sumo match
        Match match = validateTNTSumoMatch(damager);
        if (match == null) {
            return;
        }
        
        // Ensure victim is also in the same match (opponent check)
        PlayerProfile victimProfile = PlayerProfile.get(victim);
        if (victimProfile == null || victimProfile.getMatch() != match) {
            return;
        }
        
        // Don't reward for hitting teammates
        if (match.getTeam(damager) == match.getTeam(victim)) {
            return;
        }
        
        // Get max TNT limit from game rules
        int maxTnt = match.getKit().getGameRules().getTntsumoMaxInstaBoomTnt();
        
        // Count current Insta Boom TNT in damager's inventory
        int currentTntCount = countInstaBoomTNT(damager);
        
        // Check if at or exceeding max limit
        if (currentTntCount >= maxTnt) {
            return; // Already at max, don't give more
        }
        
        // Give +1 Insta Boom TNT
        giveInstaBoomTNT(damager, 1);
        
    }
    
    /**
     * Return blocks to placer when destroyed by Insta Boom TNT (if clear block enabled)
     * This bypasses the timer-based clear block mechanic
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onInstaBoomExplodeBlocks(org.bukkit.event.entity.EntityExplodeEvent event) {
        // Only handle TNT explosions
        if (event.getEntityType() != org.bukkit.entity.EntityType.PRIMED_TNT) {
            return;
        }
        
        // Check if it's Insta Boom TNT
        if (!event.getEntity().hasMetadata("INSTA_BOOM")) {
            return;
        }
        
        // Find the match this explosion is in
        org.bukkit.Location location = event.getLocation();
        Match match = rip.diamond.practice.match.Match.getMatches().values().stream()
                .filter(m -> m.getArenaDetail().getCuboid().contains(location))
                .findFirst()
                .orElse(null);
        
        if (match == null) {
            return;
        }
        
        // Only proceed if this is a TNT Sumo match
        if (!match.getKit().getGameRules().isTntsumo()) {
            return;
        }
        
        // Only proceed if clear block is enabled
        if (match.getKit().getGameRules().isClearBlock()) {
            // Process each destroyed block for returning items
            for (org.bukkit.block.Block block : event.blockList()) {
                org.bukkit.Location blockLoc = block.getLocation();
                
                // Check if this block was player-placed
                if (!match.getPlacedBlocks().contains(blockLoc)) {
                    continue; // Not player-placed, skip
                }
                
                // Find the MatchClearBlockTask for this block to get the placer
                match.getTasks().stream()
                        .filter(task -> task instanceof rip.diamond.practice.match.task.MatchClearBlockTask)
                        .map(task -> (rip.diamond.practice.match.task.MatchClearBlockTask) task)
                        .filter(task -> task.getLocation().equals(blockLoc))
                        .findFirst()
                        .ifPresent(task -> {
                            rip.diamond.practice.match.team.TeamPlayer placer = task.getBlockPlacer();
                            Player placerPlayer = placer.getPlayer();
                            
                            // Only give back if placer is online, alive, and not respawning
                            if (placerPlayer != null && placerPlayer.isOnline()
                                    && placer.isAlive() && !placer.isRespawning()) {
                                
                                // Get block drops
                                java.util.Collection<org.bukkit.inventory.ItemStack> drops = block.getDrops();
                                
                                // Give drops to placer
                                drops.forEach(item -> placerPlayer.getInventory().addItem(item));
                                
                                // Track this location to prevent natural item spawning
                                String locKey = locationKey(blockLoc);
                                expectedDrops.put(locKey, new BlockDropInfo(drops));
                            }
                            
                            // Cancel the clear block task for THIS SPECIFIC block only
                            task.setActivateCallback(false);
                            task.cancel();
                        });
                
                // Remove from placed blocks list (block no longer exists)
                match.getPlacedBlocks().remove(blockLoc);
            }
        }
        
        // === MANUAL PLAYER DAMAGE/KNOCKBACK ===
        // Minecraft's explosion system has a hardcoded range (~yield*2), so we manually
        // find and damage players within our custom radius to support larger ranges
        org.bukkit.entity.TNTPrimed tnt = (org.bukkit.entity.TNTPrimed) event.getEntity();
        double customRadius = rip.diamond.practice.config.Config.MATCH_INSTA_TNT_RADIUS.toDouble();
        org.bukkit.Location tntLoc = event.getLocation();
        
        // Find all entities within our custom radius
        for (org.bukkit.entity.Entity entity : tntLoc.getWorld().getNearbyEntities(
                tntLoc, customRadius, customRadius, customRadius)) {
            
            if (!(entity instanceof Player)) continue;
            Player player = (Player) entity;
            
            // Exclude spectators and respawning players
            if (rip.diamond.practice.match.util.InstaBoomKnockback.shouldExclude(player, match)) {
                continue;
            }
            
            // Check if within exact radius (sphere check)
            if (!rip.diamond.practice.match.util.InstaBoomKnockback.isWithinRadius(player, tntLoc)) {
                continue;
            }
            
            // Pre-calculate obstruction BEFORE applying damage
            String key = "obstruction_" + player.getUniqueId().toString();
            int obstructionBlocks = 0;
            if (!tnt.hasMetadata(key)) {
                obstructionBlocks = rip.diamond.practice.match.util.ExplosionDamageUtil.countBlocksInPath(
                    tntLoc, player.getLocation());
                tnt.setMetadata(key, new org.bukkit.metadata.FixedMetadataValue(Eden.INSTANCE, obstructionBlocks));
            } else {
                obstructionBlocks = tnt.getMetadata(key).get(0).asInt();
            }
            
            // Calculate knockback
            rip.diamond.practice.match.util.InstaBoomKnockback.KnockbackResult result = 
                    rip.diamond.practice.match.util.InstaBoomKnockback.calculate(player, tnt);
            
            // Apply damage (if allowed)
            if (result.shouldDamage) {
                double maxDamage = rip.diamond.practice.match.util.ExplosionDamageUtil.getMaxDamage(
                        player,
                        tnt.getSource() instanceof org.bukkit.entity.LivingEntity ? 
                            (org.bukkit.entity.LivingEntity) tnt.getSource() : null,
                        rip.diamond.practice.config.Config.MATCH_INSTA_TNT_MAX_DAMAGE_SELF.toDouble(),
                        rip.diamond.practice.config.Config.MATCH_INSTA_TNT_MAX_DAMAGE_OTHERS.toDouble());
                
                // Calculate damage - only players within 'radius' are affected
                // - yield: controls block breaking power
                // - customRadius: controls damage falloff distance (closer = more damage)
                double scaledDamage = rip.diamond.practice.match.util.ExplosionDamageUtil.calculateDamage(
                        player.getLocation(),
                        tntLoc,
                        rip.diamond.practice.config.Config.MATCH_INSTA_TNT_YIELD.toDouble(),  // For compatibility (unused in damage calc)
                        customRadius,  // Damage scales from max at center to 0 at radius distance
                        maxDamage,
                        obstructionBlocks);
                
                player.damage(scaledDamage);
            }
            
            // Apply knockback (if enabled)
            if (rip.diamond.practice.config.Config.MATCH_INSTA_TNT_KNOCKBACK_ENABLED.toBoolean()) {
                rip.diamond.practice.match.util.InstaBoomKnockback.applyKnockback(player, result.velocity);
            }
        }
        
        // Mark this TNT as "manually processed" to prevent EntityDamageByEntityEvent from double-processing
        tnt.setMetadata("MANUALLY_PROCESSED", new org.bukkit.metadata.FixedMetadataValue(Eden.INSTANCE, true));
    }

    
    /**
     * Prevent natural item drops from blocks we already manually returned
     * Uses proximity matching to handle item merging/grouping
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onItemSpawn(org.bukkit.event.entity.ItemSpawnEvent event) {
        org.bukkit.entity.Item item = event.getEntity();
        org.bukkit.Location spawnLoc = item.getLocation();
        org.bukkit.Material itemType = item.getItemStack().getType();
        
        // Check all tracked locations to see if any are nearby
        for (java.util.Map.Entry<String, BlockDropInfo> entry : expectedDrops.entrySet()) {
            BlockDropInfo dropInfo = entry.getValue();
            
            // Skip if expired
            if (dropInfo.isExpired()) {
                expectedDrops.remove(entry.getKey());
                continue;
            }
            
            // Check if item type matches this tracked block's expected drops
            if (!dropInfo.expectedMaterials.contains(itemType)) {
                continue;
            }
            
            // Parse the location from the key
            org.bukkit.Location trackedLoc = locationFromKey(entry.getKey());
            if (trackedLoc == null || !trackedLoc.getWorld().equals(spawnLoc.getWorld())) {
                continue;
            }
            
            // Check if spawn location is within 2 blocks of tracked location
            // (handles item merging and spawn randomness)
            if (trackedLoc.distance(spawnLoc) <= 2.0) {
                // This is a duplicate drop from a block we already handled
                event.setCancelled(true);
                
                // Remove this material from expected set
                dropInfo.expectedMaterials.remove(itemType);
                
                // If no more materials expected, clean up the entry
                if (dropInfo.expectedMaterials.isEmpty()) {
                    expectedDrops.remove(entry.getKey());
                }
                
                return; // Found and handled, stop checking
            }
        }
    }
    
    // ========================================
    // HELPER METHODS
    // ========================================
    
    /**
     * Create a string key from a location (block coordinates only)
     */
    private String locationKey(org.bukkit.Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }
    
    /**
     * Parse a location from a string key
     */
    private org.bukkit.Location locationFromKey(String key) {
        try {
            String[] parts = key.split(":");
            if (parts.length != 4) return null;
            
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            
            return new org.bukkit.Location(world, x, y, z);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Count how many Insta Boom TNT items the player has in their inventory
     */
    private int countInstaBoomTNT(Player player) {
        org.bukkit.inventory.ItemStack instaBoomTNT = plugin.getCustomItemManager().getItem("INSTA_BOOM_TNT");
        if (instaBoomTNT == null) {
            return 0;
        }
        
        int count = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(instaBoomTNT)) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    /**
     * Give Insta Boom TNT to the player
     */
    private void giveInstaBoomTNT(Player player, int amount) {
        org.bukkit.inventory.ItemStack instaBoomTNT = plugin.getCustomItemManager().getItem("INSTA_BOOM_TNT");
        if (instaBoomTNT == null) {
            return;
        }
        
        org.bukkit.inventory.ItemStack tntToGive = instaBoomTNT.clone();
        tntToGive.setAmount(amount);
        
        // Add to inventory (will merge with existing stacks if possible)
        player.getInventory().addItem(tntToGive);
    }

}
