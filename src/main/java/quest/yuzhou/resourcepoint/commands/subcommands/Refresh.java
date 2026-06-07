package quest.yuzhou.resourcepoint.commands.subcommands;

import org.bukkit.command.CommandSender;
import quest.yuzhou.resourcepoint.ResourcePoint;
import quest.yuzhou.resourcepoint.commands.SubCommand;
import quest.yuzhou.resourcepoint.types.Region;
import quest.yuzhou.resourcepoint.utilities.StaticUtilities;

import java.util.ArrayList;
import java.util.List;

public class Refresh implements SubCommand {

    @Override
    public String getName() {
        return "refresh";
    }

    @Override
    public String getDescription() {
        return "刷新資源點";
    }

    @Override
    public String getSyntax() {
        return "/rp refresh <region>";
    }

    @Override
    public void perform(CommandSender sender, String[] args, ResourcePoint plugin) {
        if (args.length != 2) {
            sender.sendMessage("請輸入要刷新的區域，如果要刷新全部請輸入/rp display all");
            plugin.getRegionList().forEach(region -> sender.sendMessage(region.getName() + " -> " + region.getDisplayName()));
            return;
        }

        Region region = plugin.getRegionByName(args[1]);
        if (region != null) {
            region.refresh();
            sender.sendMessage("操作完成。");
            return;
        }

        if (args[1].equalsIgnoreCase("all")) {
            List<Region> regions = new ArrayList<>(plugin.getRegionList());
            StaticUtilities.processRegionsSequentially(
                    plugin,
                    sender,
                    regions,
                    Region::refresh, // 每個區域執行 refresh
                    () -> sender.sendMessage("所有區域清理完成！"),
                    20L // 1秒延遲
            );
            return;
        }

        sender.sendMessage("找不到區域。");
    }

}
