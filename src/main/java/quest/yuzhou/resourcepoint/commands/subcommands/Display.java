package quest.yuzhou.resourcepoint.commands.subcommands;

import org.bukkit.command.CommandSender;
import quest.yuzhou.resourcepoint.ResourcePoint;
import quest.yuzhou.resourcepoint.commands.SubCommand;
import quest.yuzhou.resourcepoint.types.Region;
import quest.yuzhou.resourcepoint.utilities.StaticUtilities;

import java.util.ArrayList;
import java.util.List;

public class Display implements SubCommand {

    @Override
    public String getName() {
        return "display";
    }

    @Override
    public String getDescription() {
        return "顯示資源點";
    }

    @Override
    public String getSyntax() {
        return "/rp display <region>";
    }

    @Override
    public void perform(CommandSender sender, String[] args, ResourcePoint plugin) {
        if (args.length != 2) {
            sender.sendMessage("請輸入要顯示的區域，如果要顯示全部請輸入/rp display all");
            plugin.getRegionList().forEach(region -> sender.sendMessage(region.getName() + " -> " + region.getDisplayName()));
            return;
        }

        Region region = plugin.getRegionByName(args[1]);
        if (region != null) {
            region.display();
            sender.sendMessage("操作完成。");
            return;
        }

        if (args[1].equalsIgnoreCase("all")) {
            List<Region> regions = new ArrayList<>(plugin.getRegionList());
            StaticUtilities.processRegionsSequentially(
                    plugin,
                    sender,
                    regions,
                    Region::display, // 每個區域執行 display
                    () -> sender.sendMessage("所有區域清理完成！"),
                    20L // 1秒延遲
            );
            return;
        }

        sender.sendMessage("找不到區域。");
    }

}
