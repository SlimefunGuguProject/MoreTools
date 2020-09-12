package io.github.linoxgh.moretools;

import java.util.logging.Level;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.linoxgh.moretools.items.CrescentHammer;
import io.github.linoxgh.moretools.listeners.PlayerListener;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.core.researching.Research;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.bstats.bukkit.Metrics;
import me.mrCookieSlime.Slimefun.cscorelib2.config.Config;
import me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem;
import me.mrCookieSlime.Slimefun.cscorelib2.updater.Updater;
import me.mrCookieSlime.Slimefun.cscorelib2.updater.GitHubBuildsUpdater;

public class MoreTools extends JavaPlugin implements SlimefunAddon {

    private static MoreTools instance;
    
    private Config config;
    private Category moreToolsCat;

    @Override
    public void onEnable() {
        instance = this;
        config = new Config(this);
        String version = getDescription().getVersion();
        
        if (version.startsWith("DEV")) {
            String cfgVersion = config.getString("version");
            if (cfgVersion != null && !cfgVersion.equals(version)) {
                saveResource("config.yml", true);
                
                config = new Config(this);
                config.setValue("version", version);
                config.save();
                
                getServer().getPluginManager().disablePlugin(this);
                getLogger().log(Level.SEVERE, "Your config.yml file is outdated, resetting it and then disabling the plugin! Please restart.");
                return;
            }

            if (config.getBoolean("options.auto-update")) {
                Updater updater = new GitHubBuildsUpdater(this, this.getFile(), "LinoxGH/MoreTools/build");
                updater.start();
            }
        }

        new Metrics(this, 8780);
        
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
        moreToolsCat = new Category(new NamespacedKey(this, "more_tools_category"), new CustomItem(Items.CRESCENT_HAMMER, "&3More Tools"), 4);
    }
    
    private void setupItems() {
        new CrescentHammer(moreToolsCat, Items.CRESCENT_HAMMER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
            SlimefunItems.TIN_INGOT, null, SlimefunItems.TIN_INGOT,
            null, SlimefunItems.COPPER_INGOT, null,
            null, SlimefunItems.TIN_INGOT, null
        }).register(this);
        
    }
    
    private void setupResearches() {
        registerResearch("crescent_hammer", 7501, "Not A Hammer", 15, Items.CRESCENT_HAMMER);
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
    
    public Config getCfg() {
        return config;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/LinoxGH/MoreTools/issues";
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }
    
    public static MoreTools getInstance() {
        return instance;
    }
    
}
