package io.github.linoxgh.moretools.items;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import io.github.linoxgh.moretools.Messages;
import io.github.linoxgh.moretools.MoreTools;
import io.github.linoxgh.moretools.handlers.ItemInteractHandler;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.DamageableItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.ToolUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.CargoManager;
import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.ReactorAccessPort;
import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.TrashCan;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.EnergyRegulator;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.ColoredMaterial;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * A {@link CrescentHammer} is a {@link SlimefunItem} which allows you to dismantle placed machine blocks
 * with a single left click, rotate those machine blocks with a right click, and increase/decrease cargo
 * nodes' channels(frequencies) with shift left/right clicking.
 *
 * @author Linox
 *
 * @see ItemInteractHandler
 * 
 */
public class CrescentHammer extends SimpleSlimefunItem<ItemInteractHandler> implements DamageableItem {

    private final boolean isChestTerminalInstalled = false;
    
    private final boolean damageable;
    private final boolean rotationEnabled;
    private final boolean channelChangeEnabled;
    
    private final int cooldown;
    private final List<String> whitelist;
    
    private final HashMap<UUID, Long> lastUses = new HashMap<>();
    private final HashMap<String, Integer> slotCurrents = new HashMap<>();

    public CrescentHammer(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        
        FileConfiguration cfg = MoreTools.getInstance().getConfig();
        
        damageable = cfg.getBoolean("item-settings.crescent-hammer.damageable");
        rotationEnabled = cfg.getBoolean("item-settings.crescent-hammer.features.enable-rotation");
        channelChangeEnabled = cfg.getBoolean("item-settings.crescent-hammer.features.enable-channel-change");
        
        cooldown = cfg.getInt("item-settings.crescent-hammer.cooldown");
        whitelist = cfg.getStringList("item-settings.crescent-hammer.rotation-whitelist");
        
        slotCurrents.put("CARGO_NODE_INPUT", 42);
        slotCurrents.put("CARGO_NODE_OUTPUT", 13);
        slotCurrents.put("CARGO_NODE_OUTPUT_ADVANCED", 42);
    }
    
    @Override
    public ItemInteractHandler getItemHandler() {
        return (e, sfItem) -> {
            if (!sfItem.getId().equals(getId())) {
                return;
            }
            e.setCancelled(true);
            
            Block b = e.getClickedBlock();
            if (b != null) {
                Player p = e.getPlayer();
                if (Slimefun.getProtectionManager().hasPermission(p, b.getLocation(), Interaction.BREAK_BLOCK)) {
                    
                    Long lastUse = lastUses.get(p.getUniqueId()); 
                    if (lastUse != null) {
                        if ((System.currentTimeMillis() - lastUse) < cooldown) {
                            p.sendMessage(
                                Messages.CRESCENTHAMMER_COOLDOWN.getMessage().replace(
                                    "{left-cooldown}", 
                                    String.valueOf(cooldown - (System.currentTimeMillis() - lastUse)))
                                );
                            return;
                        }
                    }
                    lastUses.put(p.getUniqueId(), System.currentTimeMillis());
                    
                    switch (e.getAction()) {
                        case RIGHT_CLICK_BLOCK:
                            if (p.isSneaking()) {
                                alterChannel(b, p, -1);
                            } else {
                                rotateBlock(b, p);
                            }
                            break;
                        
                        case LEFT_CLICK_BLOCK:
                            if (p.isSneaking()) {
                                alterChannel(b, p, 1);
                            } else {
                                dismantleBlock(b, p, e.getItem());
                            }
                            break;
                        
                        default:
                            break;
                    }
                }
            }
            e.setCancelled(true);
        };
    }
    
    private void alterChannel(Block b, Player p, int change) {
    
        if (!channelChangeEnabled) {
            return;
        }
        
        SlimefunItem sfItem = BlockStorage.check(b);
        if (sfItem != null) {
            if (sfItem.getId().startsWith("CARGO_NODE_")) {
            
                String frequency = BlockStorage.getLocationInfo(b.getLocation(), "frequency");
                if (frequency != null) {
                    int current = Integer.parseInt(frequency);
                    current += change;
                    
                    if (current < 0) {
                        current = isChestTerminalInstalled ? 16 : 15;
                    } else if (isChestTerminalInstalled && current > 16) {
                        current = 0;
                    } else if (!isChestTerminalInstalled && current > 15) {
                        current = 0;
                    }
                    
                    BlockMenu menu = BlockStorage.getInventory(b);
                    int slotCurrent = slotCurrents.get(sfItem.getId());
                    
                    if (current == 16) { 
                        menu.replaceExistingItem(
                            slotCurrent, 
                            new CustomItemStack(HeadTexture.CHEST_TERMINAL.getAsItemStack(),
                                "&b信道 ID：&3" + (current + 1))
                        );
                    } else { 
                        menu.replaceExistingItem(
                            slotCurrent, 
                            new CustomItemStack(ColoredMaterial.WOOL.get(current), "&b信道 ID：&3" + (current + 1))
                        );
                    }
                    menu.addMenuClickHandler(slotCurrent, ChestMenuUtils.getEmptyClickHandler());
                    
                    BlockStorage.addBlockInfo(b.getLocation(), "frequency", Integer.toString(current));
                    p.sendMessage(Messages.CRESCENTHAMMER_CHANNELCHANGESUCCESS.getMessage().replace("{channel}", Integer.toString(current + 1)));
                    return;
                }
            }
        }
        p.sendMessage(Messages.CRESCENTHAMMER_CHANNELCHANGEFAIL.getMessage());
    }
    
    private void dismantleBlock(Block b, Player p, ItemStack item) {
        SlimefunItem sfItem = BlockStorage.check(b);
        if (sfItem != null) {
            if (sfItem instanceof EnergyNetComponent || sfItem instanceof EnergyRegulator ||
                sfItem.getId().startsWith("CARGO_NODE") || sfItem instanceof CargoManager ||
                sfItem instanceof ReactorAccessPort || sfItem instanceof TrashCan) {
            
                BlockBreakEvent event = new BlockBreakEvent(b, p);
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType());
                
                    if (isDamageable()) {
                        damageItem(p, item);
                    }
                    return;
                }
            }
        }
        p.sendMessage(Messages.CRESCENTHAMMER_DISMANTLEFAIL.getMessage());
    }
    
    private void rotateBlock(Block b, Player p) {
    
        if (!rotationEnabled) {
            return;
        }
        
        if (whitelist != null && !p.hasPermission("moretools.items.crescent-hammer.rotation-whitelist-bypass")) {
            if (!whitelist.contains(b.getType().name())) {
                p.sendMessage(Messages.CRESCENTHAMMER_ROTATEFAIL.getMessage());
                return;
            }
        }
        
        if (b.getBlockData() instanceof Directional) {
            Directional data = (Directional) b.getBlockData();
            BlockFace[] directions = data.getFaces().toArray(new BlockFace[0]);
            
            for (int i = 0; i < directions.length; i++) {
                if (data.getFacing() == directions[i]) {
                    i = (i == directions.length - 1) ? 0 : i + 1;
                    data.setFacing(directions[i]);
                    b.setBlockData(data, true);
                    
                    // Special management for cargo nodes.
                    if (b.getType() == Material.PLAYER_WALL_HEAD) {
                        SlimefunItem sfItem = BlockStorage.check(b);
                        if (sfItem != null) {
                            if (sfItem.getId().startsWith("CARGO_NODE")) {
                                Slimefun.getNetworkManager().updateAllNetworks(b.getLocation());
                            }
                        }
                    }
                    return;
                }
            }
        }
        p.sendMessage(Messages.CRESCENTHAMMER_ROTATEFAIL.getMessage());
    }
    
    private ToolUseHandler getToolUseHandler() {
        return (e, item, i, list) -> {
            if (isItem(item)) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(Messages.CRESCENTHAMMER_BLOCKBREAKING.getMessage());
            }
        };
    }
    
    @Override
    public void preRegister() {
        super.preRegister();
        addItemHandler(getToolUseHandler());
    }
    
    @Override
    public boolean isDamageable() {
        return damageable;
    }
    
}
