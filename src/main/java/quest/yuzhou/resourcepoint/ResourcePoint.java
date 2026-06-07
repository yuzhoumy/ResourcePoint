package quest.yuzhou.resourcepoint;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import quest.yuzhou.rcchest.RCChest;
import quest.yuzhou.resourcepoint.commands.CommandManager;
import quest.yuzhou.resourcepoint.types.Region;
import quest.yuzhou.resourcepoint.utilities.PasteUtilities;

import java.util.ArrayList;
import java.util.List;

public final class ResourcePoint extends JavaPlugin {

    private PasteUtilities pasteUtilities;
    private final List<Region> regionList = new ArrayList<>();
    public static final String schematicsFolder = "/schematics/";
    private RCChest rcChest;

    @Override
    public void onEnable() {
        // Plugin startup logic
        pasteUtilities = new PasteUtilities(this);
        saveDefaultConfig();
        loadRegions();
        getCommand("rp").setExecutor(new CommandManager(this));
        rcChest = (RCChest) Bukkit.getPluginManager().getPlugin("RCChest");
        if (rcChest == null) {
            getLogger().severe("Can't hook to RCChest plugin!");
            onDisable();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Bye!");
    }

    public PasteUtilities getPasteUtilities() {
        return pasteUtilities;
    }

    public List<Region> getRegionList() {
        return regionList;
    }

    public Region getRegionByName(String name) {
        for (Region region : regionList) {
            if (region.getName().equalsIgnoreCase(name)) {
                return region;
            }
        }
        return null;
    }

    public void loadRegions() {
        regionList.clear();
        ConfigurationSection pasteSection = getConfig().getConfigurationSection("paste");
        for (String key : pasteSection.getKeys(false)) {
            getLogger().info("Registering region: " + key);
            regionList.add(new Region(key, this));
        }
    }

    public RCChest getRCChest() {
        return rcChest;
    }
}
