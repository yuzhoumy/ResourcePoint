package quest.yuzhou.resourcepoint.commands.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import quest.yuzhou.resourcepoint.ResourcePoint;
import quest.yuzhou.resourcepoint.commands.SubCommand;
import quest.yuzhou.resourcepoint.types.Region;
import quest.yuzhou.resourcepoint.utilities.StaticUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CleanAndRefresh implements SubCommand {
    // Static flag to prevent multiple "all" operations at once
    private static final AtomicBoolean processingAll = new AtomicBoolean(false);

    @Override
    public String getName() {
        return "clean-and-refresh";
    }

    @Override
    public String getDescription() {
        return "清理並刷新資源點。第三個參數是當刷新結束的時候執行指令，這個功能是用來自動重啓的。(只適用於刷新所有資源點)";
    }

    @Override
    public String getSyntax() {
        return "/rp clean-and-refresh <region> <runCommandWhenDone>";
    }

    @Override
    public void perform(CommandSender sender, String[] args, ResourcePoint plugin) {
        if (args.length != 3) {
            sender.sendMessage("正確格式：" + getSyntax() + "。");
            sender.sendMessage("輸入區域，如果要清理並刷新全部請輸入/rp clean-and-refresh all false");
            plugin.getRegionList().forEach(region -> sender.sendMessage(region.getName() + " -> " + region.getDisplayName()));
            return;
        }

        // Process single region
        if (!args[1].equalsIgnoreCase("all")) {
            Region region = plugin.getRegionByName(args[1]);
            if (region != null) {
                region.cleanAndRefresh();
                sender.sendMessage("已開始清理並刷新區域: " + region.getDisplayName());
            } else {
                sender.sendMessage("找不到區域。");
            }
            return;
        }

        // Process all regions sequentially
        if (!processingAll.compareAndSet(false, true)) {
            sender.sendMessage("清理並刷新全部區域的操作正在進行中，請稍後再試。");
            return;
        }


        if (args[1].equalsIgnoreCase("all")) {
            List<Region> regions = new ArrayList<>(plugin.getRegionList());
            StaticUtilities.processRegionsSequentially(
                    plugin,
                    sender,
                    regions,
                    Region::cleanAndRefresh, // 每個區域執行 clean and refresh
                    () -> {
                        sender.sendMessage("所有區域清理完成！");
                        boolean restartServer = Boolean.parseBoolean(args[2]);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (restartServer) {
                                    String command = plugin.getConfig().getString("execute-when-done-refresh");
                                    if (command == null) {
                                        plugin.getLogger().severe("the command to be executed when refresh done in config is null.");
                                        return;
                                    }
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                                }
                                processingAll.set(false);
                            }
                        }.runTaskLater(plugin, 400L);
                    },
                    100L // 5秒延遲
            );
            return;
        }

        sender.sendMessage("找不到區域。");
    }
}