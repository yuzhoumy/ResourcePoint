package quest.yuzhou.resourcepoint.utilities;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import quest.yuzhou.resourcepoint.ResourcePoint;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class PasteUtilities {

    private final ResourcePoint plugin;
    private final Random random = new Random();
    private final Map<String, Clipboard> clipboardCache = new ConcurrentHashMap<>();
    private static final int BATCH_SIZE = 10; // Increased batch size
    private static final long BATCH_DELAY = 2L; // Reduced delay between batches

    public PasteUtilities(ResourcePoint plugin) {
        this.plugin = plugin;
    }

    /**
     * Paste a single schematic at a location (no rotation)
     */
    public void pasteSingle(File schematicFile, Location location, boolean withAir) {
        pasteBatch(schematicFile, List.of(location), withAir, false, 0, null);
    }

    /**
     * Paste a single schematic at a location with random rotation
     */
    public void pasteSingleRotation(File schematicFile, Location location, boolean withAir, int width) {
        pasteBatch(schematicFile, List.of(location), withAir, true, width, null);
        plugin.getLogger().info("pastesinglerotation");
    }


    /**
     * Paste multiple locations (without rotation)
     */
    public void pasteMultiple(File schematicFile, List<Location> locations, boolean withAir) {
        pasteBatch(schematicFile, locations, withAir, false, 0, null);
    }

    /**
     * Paste multiple locations (with random rotation)
     */
    public void pasteMultipleRandomRotation(File schematicFile, List<Location> locations, boolean withAir, int width) {
        pasteBatch(schematicFile, locations, withAir, true, width, null);
    }

    /**
     * Paste multiple locations with callback
     */
    public void pasteMultipleWithCallback(File schematicFile, List<Location> locations, boolean withAir, Runnable onComplete) {
        pasteBatch(schematicFile, locations, withAir, false, 0, onComplete);
    }

    /**
     * Paste multiple locations with random rotation and callback
     */
    public void pasteMultipleRandomRotationWithCallback(File schematicFile, List<Location> locations, boolean withAir, int width, Runnable onComplete) {
        pasteBatch(schematicFile, locations, withAir, true, width, onComplete);
    }

    /**
     * Core async batch paste logic with optimizations
     */
    private void pasteBatch(File schematicFile, List<Location> locations, boolean withAir, boolean randomRotation, int width, Runnable onComplete) {
        // Group locations by world to minimize EditSession creation
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Clipboard clipboard;
            try {
                // Use cached clipboard if available
                String cacheKey = schematicFile.getAbsolutePath();
                clipboard = clipboardCache.computeIfAbsent(cacheKey, key -> {
                    try {
                        return loadClipboard(schematicFile);
                    } catch (IOException e) {
                        plugin.getLogger().warning("Failed to load schematic: " + schematicFile.getName());
                        e.printStackTrace();
                        return null;
                    }
                });

                if (clipboard == null) {
                    if (onComplete != null) {
                        Bukkit.getScheduler().runTask(plugin, onComplete);
                    }
                    return;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load schematic: " + schematicFile.getName());
                e.printStackTrace();
                if (onComplete != null) {
                    Bukkit.getScheduler().runTask(plugin, onComplete);
                }
                return;
            }

            // Group locations by world
            Map<World, List<Location>> worldLocations = new HashMap<>();
            for (Location loc : locations) {
                worldLocations.computeIfAbsent(loc.getWorld(), w -> new ArrayList<>()).add(loc);
            }

            // Process each world separately
            int totalWorlds = worldLocations.size();
            int[] completedWorlds = {0};

            for (Map.Entry<World, List<Location>> entry : worldLocations.entrySet()) {
                World world = entry.getKey();
                List<Location> worldLocs = entry.getValue();

                // Don't create a new runnable for each world, process in batches
                new BukkitRunnable() {
                    private int index = 0;
                    private final Clipboard clipboardRef = clipboard;

                    @Override
                    public void run() {
                        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                                .world(BukkitAdapter.adapt(world))
                                .build()) {

                            int processed = 0;
                            while (index < worldLocs.size() && processed < BATCH_SIZE) {
                                Location location = worldLocs.get(index++);

                                try {
                                    if (randomRotation) {
                                        pasteWithRotation(editSession, location, clipboardRef, withAir, width);
                                    } else {
                                        pasteNormal(editSession, location, clipboardRef, withAir);
                                    }
                                    processed++;
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Error pasting at " + location + ": " + e.getMessage());
                                }
                            }

                            // If this world is complete
                            if (index >= worldLocs.size()) {
                                cancel();

                                // Check if all worlds are done
                                synchronized (completedWorlds) {
                                    completedWorlds[0]++;
                                    if (completedWorlds[0] >= totalWorlds && onComplete != null) {
                                        Bukkit.getScheduler().runTask(plugin, onComplete);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error in paste batch: " + e.getMessage());
                            e.printStackTrace();
                            cancel();

                            // Ensure callback is still called on error
                            synchronized (completedWorlds) {
                                completedWorlds[0]++;
                                if (completedWorlds[0] >= totalWorlds && onComplete != null) {
                                    Bukkit.getScheduler().runTask(plugin, onComplete);
                                }
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0L, BATCH_DELAY);
            }
        });
    }

    private Clipboard loadClipboard(File schematicFile) throws IOException {
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        if (format == null) {
            throw new IOException("Unknown schematic format: " + schematicFile.getName());
        }
        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            return reader.read();
        }
    }

    private void pasteNormal(EditSession editSession, Location location, Clipboard clipboard, boolean withAir) {
        try {
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            Operation operation = holder
                    .createPaste(editSession)
                    .to(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                    .ignoreAirBlocks(!withAir)
                    .build();

            Operations.complete(operation);
        } catch (WorldEditException e) {
            plugin.getLogger().warning("Error during paste operation: " + e.getMessage());
        }
    }

    private void pasteWithRotation(EditSession editSession, Location location, Clipboard clipboard, boolean withAir, int width) {
        try {
            int rotation = randomRotation();
            Location adjustedLocation = adjustLocation(location.clone(), width, rotation);

            AffineTransform transform = new AffineTransform().rotateY(rotation);
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            holder.setTransform(transform);

            Operation operation = holder
                    .createPaste(editSession)
                    .to(BlockVector3.at(
                            adjustedLocation.getBlockX(),
                            adjustedLocation.getBlockY(),
                            adjustedLocation.getBlockZ())
                    )
                    .ignoreAirBlocks(!withAir)
                    .build();

            Operations.complete(operation);
        } catch (WorldEditException e) {
            plugin.getLogger().warning("Error during paste operation with rotation: " + e.getMessage());
        }
    }

    private int randomRotation() {
        int[] angles = {0, 90, 180, 270};
        return angles[random.nextInt(angles.length)];
    }

    private Location adjustLocation(Location original, int width, int rotation) {
        Location newLoc = original.clone();
        width--;
        switch (rotation) {
            case 270 -> newLoc.add(width, 0, 0);
            case 180 -> newLoc.add(width, 0, width);
            case 90 -> newLoc.add(0, 0, width);
        }
        return newLoc;
    }
}