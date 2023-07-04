package io.github.linoxgh.moretools;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bstats.bukkit.Metrics;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.linoxgh.moretools.items.CrescentHammer;
import io.github.linoxgh.moretools.listeners.PlayerListener;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;

import net.guizhanss.guizhanlibplugin.updater.GuizhanUpdater;

public class MoreTools extends JavaPlugin implements SlimefunAddon {

    private static MoreTools instance;
    
    private final File configFile = new File(getDataFolder().getAbsolutePath() + File.separator + "config.yml");
    
    private ItemGroup moreToolsItemGroup;
    private boolean debug = true;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        debug = cfg.getBoolean("options.debugging", true);

        if (!getServer().getPluginManager().isPluginEnabled("GuizhanLibPlugin")) {
            getLogger().log(Level.SEVERE, "本插件需要 鬼斩前置库插件(GuizhanLibPlugin) 才能运行!");
            getLogger().log(Level.SEVERE, "从此处下载: https://50l.cc/gzlib");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        String version = getDescription().getVersion();
        if (version.startsWith("Build")) {
            String cfgVersion = cfg.getString("version");
            
            Configuration defaultCfg = getConfig().getDefaults();
            if (cfgVersion == null || !cfgVersion.equals(version)) {
            
                getLogger().log(Level.WARNING, "配置文件 config.yml 已经过期，正在更新...");
                
                for (String key : defaultCfg.getKeys(true)) {
                    if (!cfg.contains(key, true)) {
                        if (debug) getLogger().log(Level.INFO, "设置 \"" + key + "\" 为 \"" + defaultCfg.get(key) + "\"。");
                        cfg.set(key, defaultCfg.get(key));
                    }
                }
                cfg.set("version", version);
                
                getLogger().log(Level.INFO, "已完成 config.yml 的更新。正在保存文件...");
                
                try {
                    cfg.save(configFile);
                    getLogger().log(Level.INFO, "已保存 config.yml 文件。");
                } catch (IOException e) {
                    getLogger().log(Level.SEVERE, "保存 config.yml 文件失败。", e);
                    getServer().getPluginManager().disablePlugin(this);
                    return;
                }
            }
            
            if (cfg.getBoolean("options.auto-update")) {
                GuizhanUpdater.start(this, getFile(), "SlimefunGuguProject", "MoreTools", "main");
            }
        }
      
        if (debug) getLogger().log(Level.INFO, "正在设置 metrics...");
        Metrics metrics = new Metrics(this, 8780);
        
        if (debug) getLogger().log(Level.INFO, "正在注册监听器...");
        new PlayerListener(this);
        
        setupCategories();
        setupItems();
        setupResearches();
    }

    @Override
    public void onDisable() {
        instance = null;
    }
    
    private void setupCategories() {
        if (debug) getLogger().log(Level.INFO, "正在初始化分类...");
        
        moreToolsItemGroup = new ItemGroup(new NamespacedKey(this, "more_tools_category"),
            new CustomItemStack(Items.CRESCENT_HAMMER, "&3更多工具"), 4);
    }
    
    private void setupItems() {
        if (debug) getLogger().log(Level.INFO, "正在初始化物品...");
        
        new CrescentHammer(moreToolsItemGroup, Items.CRESCENT_HAMMER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
            SlimefunItems.TIN_INGOT, null, SlimefunItems.TIN_INGOT,
            null, SlimefunItems.COPPER_INGOT, null,
            null, SlimefunItems.TIN_INGOT, null
        }).register(this);
        
    }
    
    private void setupResearches() {
        if (debug) getLogger().log(Level.INFO, "正在初始化研究...");
        
        registerResearch("crescent_hammer", 7501, "是个锤子", 15, Items.CRESCENT_HAMMER);
    }
    
    private void registerResearch(String key, int id, String name, int defaultCost, ItemStack... items) {
        Research research = new Research(new NamespacedKey(this, key), id, name, defaultCost);
        
        for (ItemStack item : items) {
            SlimefunItem sfItem = SlimefunItem.getByItem(item);
            if (sfItem != null) {
                research.addItems(sfItem);
            }
        }
        research.register();
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/SlimefunGuguProject/MoreTools/issues";
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }
    
    public static MoreTools getInstance() {
        return instance;
    }
    
    public boolean debug() {
        return debug;
    }
    
}
