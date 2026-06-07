package quest.yuzhou.resourcepoint.commands.subcommands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import quest.yuzhou.resourcepoint.ResourcePoint;
import quest.yuzhou.resourcepoint.commands.SubCommand;

public class Reload implements SubCommand {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "重新載入配置文件";
    }

    @Override
    public String getSyntax() {
        return "/rp reload";
    }

    @Override
    public void perform(CommandSender sender, String[] args, ResourcePoint plugin) {
        plugin.reloadConfig();
        plugin.loadRegions();
        sender.sendMessage("成功重新載入");
        sender.sendMessage(ChatColor.YELLOW + "宇宙 yuzhou_ 27/4/2025");
    }
}
