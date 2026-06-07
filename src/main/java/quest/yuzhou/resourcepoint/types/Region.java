package quest.yuzhou.resourcepoint.types;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import quest.yuzhou.resourcepoint.ResourcePoint;
import quest.yuzhou.resourcepoint.utilities.StaticUtilities;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

import static quest.yuzhou.resourcepoint.ResourcePoint.schematicsFolder;

public final class Region {

    private final ResourcePoint plugin;
    private final String name;
    private final String displayName;
    private final List<Location> locations;
    private final int percentage;
    private final int xWidth;
    private final int zWidth;
    private final List<File> schematics;
    private final File cleanFile;
    private final File displayFile;
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    public Region(String name, ResourcePoint plugin) {
        this.plugin = plugin;
        this.name = name;

        ConfigurationSection pasteSection = plugin.getConfig().getConfigurationSection("paste");
        for (String key : pasteSection.getKeys(false)) {
            if (name.equalsIgnoreCase(key)) {
                ConfigurationSection thisSection = pasteSection.getConfigurationSection(key);
                this.displayName = thisSection.getString("name");
                World world = Bukkit.getWorld(thisSection.getString("world"));
                this.percentage = thisSection.getInt("percentage");
                this.xWidth = Integer.parseInt(thisSection.getString("size").split(",")[0]);
                this.zWidth = Integer.parseInt(thisSection.getString("size").split(",")[1]);
                this.locations = new ArrayList<>();
                for (String locationString : thisSection.getStringList("locations")) {
                    int x = Integer.parseInt(locationString.split(",")[0]);
                    int y = Integer.parseInt(locationString.split(",")[1]);
                    int z = Integer.parseInt(locationString.split(",")[2]);
                    this.locations.add(new Location(world, x, y, z));
                }

                this.schematics = thisSection
                        .getStringList("files")
                        .stream()
                        .map((fileString) -> new File(plugin.getDataFolder(), schematicsFolder + fileString + ".schem"))
                        .toList();
                this.cleanFile = new File(plugin.getDataFolder(), schematicsFolder + thisSection.getString("cleanFile") + ".schem");
                this.displayFile = new File(plugin.getDataFolder(), schematicsFolder + thisSection.getString("displayFile") + ".schem");

                if (world == null) throw new NullPointerException("World not found: " + thisSection.getString("world"));
                return;
            }
        }

        throw new NullPointerException("No such region in config: " + name);
    }

    public void clean() {
        clean(null);
    }

    public void clean(Runnable onComplete) {
        if (!isRefreshing.compareAndSet(false, true)) {
            plugin.getLogger().warning("Region " + displayName + " is already being processed. Skipping clean operation.");
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        long startTime = System.currentTimeMillis();
        plugin.getPasteUtilities().pasteMultipleWithCallback(cleanFile, locations, true, () -> {
            long endTime = System.currentTimeMillis();
            plugin.getLogger().info("已清理 " + displayName + " 共耗時 " + (endTime - startTime) + " 毫秒，約等於 " + (endTime - startTime) / 1000 + " 秒。");
            isRefreshing.set(false);

            if (onComplete != null) {
                onComplete.run();
            }
            removeBlockAboveChest();
        });
    }

    public void display() {
        display(null);
    }

    public void display(Runnable onComplete) {
        if (!isRefreshing.compareAndSet(false, true)) {
            plugin.getLogger().warning("Region " + displayName + " is already being processed. Skipping display operation.");
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        long startTime = System.currentTimeMillis();
        plugin.getPasteUtilities().pasteMultipleWithCallback(displayFile, locations, true, () -> {
            long endTime = System.currentTimeMillis();
            plugin.getLogger().info("已顯示 " + displayName + " 共耗時 " + (endTime - startTime) + " 毫秒，約等於 " + (endTime - startTime) / 1000 + " 秒。");
            isRefreshing.set(false);

            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void refresh() {
        refresh(null);
    }

    public void refresh(Runnable onComplete) {
        if (!isRefreshing.compareAndSet(false, true)) {
            plugin.getLogger().warning("Region " + displayName + " is already being processed. Skipping refresh operation.");
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 1. Filter locations based on percentage
            List<Location> filteredLocations = StaticUtilities.splitListAndRemoveElements(locations, percentage);

            // 2. Split locations evenly for schematics
            List<List<Location>> splittedLocations = StaticUtilities.splitListEvenly(filteredLocations, schematics.size());

            // 3. Calculate total operations for tracking completion
            final int totalOperations = splittedLocations.size();
            final int[] completedOperations = {0};

            // 4. Process each batch with its own schematic
            for (int i = 0; i < splittedLocations.size(); i++) {
                List<Location> locs = splittedLocations.get(i);
                if (locs.isEmpty()) {
                    synchronized (completedOperations) {
                        completedOperations[0]++;
                        checkCompletion(startTime, completedOperations[0], totalOperations, onComplete);
                    }
                    continue;
                }

                // Select schematic (with fallback)
                File schematic;
                if (i < schematics.size()) {
                    schematic = schematics.get(i);
                } else {
                    schematic = schematics.get(StaticUtilities.randomInt(0, schematics.size() - 1));
                }

                // Use callback to track completion
                Runnable callback = () -> {
                    synchronized (completedOperations) {
                        completedOperations[0]++;
                        checkCompletion(startTime, completedOperations[0], totalOperations, onComplete);
                    }
                };

                // Paste with or without rotation based on dimensions
                if (xWidth == zWidth) {
                    plugin.getPasteUtilities().pasteMultipleRandomRotationWithCallback(schematic, locs, true, xWidth, callback);
                } else {
                    plugin.getPasteUtilities().pasteMultipleWithCallback(schematic, locs, true, callback);
                }
            }

            // Handle the case of empty operation list
            if (totalOperations == 0) {
                plugin.getLogger().info("No operations to perform for " + displayName);
                isRefreshing.set(false);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error during refresh operation for " + displayName + ": " + e.getMessage());
            e.printStackTrace();
            isRefreshing.set(false);
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    // returning pasted locations, for display purpose
    public List<Location> resourceStorm(int amount, List<File> schematics) {

        List<Location> locationsToPaste = new ArrayList<>(locations);
        Collections.shuffle(locationsToPaste);
        locationsToPaste = locationsToPaste.subList(0, Math.min(amount, locationsToPaste.size()));

        plugin.getLogger().info("resourcestorm method");

        try {
            // process for each schematic

            Random random = new Random();
            for (Location location : locationsToPaste) {
                plugin.getPasteUtilities().pasteSingle(cleanFile, location, true);
                File schematic = schematics.get(random.nextInt(schematics.size()));
                plugin.getLogger().info(schematic.getName());
                // Paste with or without rotation based on dimensions
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (xWidth == zWidth) {
                        plugin.getPasteUtilities().pasteSingleRotation(schematic, location, true, xWidth);
                    } else {
                        plugin.getPasteUtilities().pasteSingle(schematic, location, true);
                    }
                }, 100);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error during refresh operation for " + displayName + ": " + e.getMessage());
            e.printStackTrace();
        }
        return locationsToPaste;
    }

    private void checkCompletion(long startTime, int completed, int total, Runnable onComplete) {
        if (completed >= total) {
            long endTime = System.currentTimeMillis();
            plugin.getLogger().info("已刷新 " + displayName + "，共耗時 " + (endTime - startTime) + " 毫秒，約等於 " + (endTime - startTime) / 1000 + " 秒。");
            isRefreshing.set(false);
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    public void cleanAndRefresh() {
        cleanAndRefresh(null);
    }

    public void cleanAndRefresh(Runnable onComplete) {
        if (!isRefreshing.compareAndSet(false, true)) {
            plugin.getLogger().warning("Region " + displayName + " is already being processed. Skipping cleanAndRefresh operation.");
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        long startTime = System.currentTimeMillis();

        // First clean all locations
        plugin.getPasteUtilities().pasteMultipleWithCallback(cleanFile, locations, true, () -> {
            // Log clean completion time
            long cleanEndTime = System.currentTimeMillis();
            plugin.getLogger().info("已清理 " + displayName + " 共耗時 " + (cleanEndTime - startTime) + " 毫秒，約等於 " + (cleanEndTime - startTime) / 1000 + " 秒。");

            // Start refresh with its own timing
            long refreshStartTime = System.currentTimeMillis();

            try {
                // 1. Filter locations
                List<Location> filteredLocations = StaticUtilities.splitListAndRemoveElements(locations, percentage);

                // 2. Split locations evenly
                List<List<Location>> splittedLocations = StaticUtilities.splitListEvenly(filteredLocations, schematics.size());

                // 3. Track completion
                final int totalOperations = splittedLocations.size();
                final int[] completedOperations = {0};

                // 4. Process each batch
                for (int i = 0; i < splittedLocations.size(); i++) {
                    List<Location> locs = splittedLocations.get(i);
                    if (locs.isEmpty()) {
                        synchronized (completedOperations) {
                            completedOperations[0]++;
                            if (completedOperations[0] >= totalOperations) {
                                finalizeRefresh(startTime, refreshStartTime, onComplete);
                            }
                        }
                        continue;
                    }

                    // Select schematic
                    File schematic;
                    if (i < schematics.size()) {
                        schematic = schematics.get(i);
                    } else {
                        schematic = schematics.get(StaticUtilities.randomInt(0, schematics.size() - 1));
                    }

                    // Callback for completion tracking
                    Runnable batchComplete = () -> {
                        synchronized (completedOperations) {
                            completedOperations[0]++;
                            if (completedOperations[0] >= totalOperations) {
                                finalizeRefresh(startTime, refreshStartTime, onComplete);
                            }
                        }
                    };

                    // Paste with appropriate method
                    if (xWidth == zWidth) {
                        plugin.getPasteUtilities().pasteMultipleRandomRotationWithCallback(schematic, locs, true, xWidth, batchComplete);
                    } else {
                        plugin.getPasteUtilities().pasteMultipleWithCallback(schematic, locs, true, batchComplete);
                    }
                }

                // Handle empty operation list
                if (totalOperations == 0) {
                    finalizeRefresh(startTime, refreshStartTime, onComplete);
                }

                removeBlockAboveChest();
            } catch (Exception e) {
                plugin.getLogger().severe("Error during refresh phase of cleanAndRefresh for " + displayName + ": " + e.getMessage());
                e.printStackTrace();
                isRefreshing.set(false);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private void finalizeRefresh(long overallStartTime, long refreshStartTime, Runnable onComplete) {
        long endTime = System.currentTimeMillis();
        long refreshMillisecond = endTime - refreshStartTime;
        plugin.getLogger().info("已刷新 " + displayName + "，共耗時 " + refreshMillisecond + " 毫秒，約等於 " + refreshMillisecond / 1000 + " 秒。");

        long totalMillisecond = endTime - overallStartTime;
        plugin.getLogger().info("清理並刷新 " + displayName + " 完成，總共耗時 " + totalMillisecond + " 毫秒，約等於 " + totalMillisecond / 1000 + " 秒。");

        isRefreshing.set(false);

        if (onComplete != null) {
            onComplete.run();
        }
    }

    // Add this method to check if a region is currently being processed
    public boolean isRefreshing() {
        return isRefreshing.get();
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPercentage() {
        return percentage;
    }

    public int getxWidth() {
        return xWidth;
    }

    public int getzWidth() {
        return zWidth;
    }

    private void removeBlockAboveChest() {

        List<Block> blockAboveList = plugin.getRCChest().getChestAndCommandManager().getBlockAboveList();
        List<Block> filteredList = new ArrayList<>();

        for (Block block : blockAboveList) {
            int x = block.getX();
            int z = block.getZ();
            for (Location location : locations) {
                int minX = location.getBlockX();
                int maxX = minX + xWidth;
                int minZ = location.getBlockZ();
                int maxZ = minZ + zWidth;

                if (x <= minX && x >= maxX && z <= minZ && z >= maxZ)
                    filteredList.add(block);
            }
        }

        plugin.getRCChest().getChestAndCommandManager().setBlockAboveList(filteredList);
        plugin.getLogger().info("已將區域 " + displayName + " 的箱子上方方塊清除");
    }
}