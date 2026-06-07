package quest.yuzhou.resourcepoint.commands;

import org.bukkit.command.CommandSender;
import quest.yuzhou.resourcepoint.ResourcePoint;

public interface SubCommand {

    String getName();
    String getDescription();
    String getSyntax();
    void perform(CommandSender sender, String[] args, ResourcePoint plugin);

}
