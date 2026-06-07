package quest.yuzhou.resourcepoint.utilities;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import quest.yuzhou.resourcepoint.ResourcePoint;
import quest.yuzhou.resourcepoint.types.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class StaticUtilities {

    public static List<Location> splitListAndRemoveElements(List<Location> original, int percentage) {
        List<Location> list = new ArrayList<>(original);
        int removeCount = (int) (list.size() * (1 - percentage / 100.0));

        for (int i = 0; i < removeCount; i++) {
            if (!list.isEmpty()) {
                list.remove(randomInt(0, list.size() - 1));
            }
        }
        return list;
    }

    public static List<List<Location>> splitListEvenly(List<Location> original, int groups) {
        List<List<Location>> result = new ArrayList<>();
        for (int i = 0; i < groups; i++) {
            result.add(new ArrayList<>());
        }
        for (int i = 0; i < original.size(); i++) {
            result.get(i % groups).add(original.get(i));
        }
        return result;
    }

    private static final Random random = new Random();

    public static int randomInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    public static void processRegionsSequentially(ResourcePoint plugin,
                                                  CommandSender sender,
                                                  List<Region> regions,
                                                  Consumer<Region> perRegionAction,
                                                  Runnable onComplete,
                                                  long delayTicks) {

        processNextRegion(plugin, sender, regions, perRegionAction, onComplete, delayTicks, 0);
    }

    private static void processNextRegion(ResourcePoint plugin,
                                          CommandSender sender,
                                          List<Region> regions,
                                          Consumer<Region> perRegionAction,
                                          Runnable onComplete,
                                          long delayTicks,
                                          int index) {
        if (index >= regions.size()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        Region region = regions.get(index);
        sender.sendMessage("開始處理區域 (" + (index + 1) + "/" + regions.size() + "): " + region.getDisplayName());

        // 執行每個區域的處理邏輯
        perRegionAction.accept(region);

        // 延遲後處理下一個
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            sender.sendMessage("區域 " + region.getDisplayName() + " 處理完成。");
            processNextRegion(plugin, sender, regions, perRegionAction, onComplete, delayTicks, index + 1);
        }, delayTicks);
    }

}
