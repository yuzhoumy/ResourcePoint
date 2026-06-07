package quest.yuzhou.resourcepoint.commands.subcommands;

import org.bukkit.command.CommandSender;
import quest.yuzhou.resourcepoint.ResourcePoint;
import quest.yuzhou.resourcepoint.commands.SubCommand;
import quest.yuzhou.resourcepoint.types.Region;

public class List implements SubCommand {

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "顯示所有區域的詳細資料";
    }

    @Override
    public String getSyntax() {
        return "/rp list";
    }

    @Override
    public void perform(CommandSender sender, String[] args, ResourcePoint plugin) {
        sender.sendMessage("--- 區域資料 ---");
        for (Region region : plugin.getRegionList()) {
            sender.sendMessage("代號：" + region.getName());
            sender.sendMessage("名字：" + region.getDisplayName());
            sender.sendMessage("大小（x, z）：" + region.getxWidth() + " , " + region.getzWidth());
            sender.sendMessage("生成率百分比：" + region.getPercentage());
        }
    }
}
