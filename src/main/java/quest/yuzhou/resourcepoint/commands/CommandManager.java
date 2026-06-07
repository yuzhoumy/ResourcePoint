package quest.yuzhou.resourcepoint.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import quest.yuzhou.resourcepoint.ResourcePoint;
import quest.yuzhou.resourcepoint.commands.subcommands.*;

import java.util.ArrayList;
import java.util.List;

public class CommandManager implements CommandExecutor {

    private final ResourcePoint plugin;
    private final List<SubCommand> subCommands = new ArrayList<>();

    public CommandManager(ResourcePoint plugin) {
        this.plugin = plugin;
        this.subCommands.add(new Clean());
        this.subCommands.add(new Display());
        this.subCommands.add(new Refresh());
        this.subCommands.add(new Reload());
        this.subCommands.add(new quest.yuzhou.resourcepoint.commands.subcommands.List());
        this.subCommands.add(new CleanAndRefresh());
        this.subCommands.add(new ResourceStorm());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (!command.getName().equalsIgnoreCase("rp")) return true;

        if (args.length > 0) {
            for (SubCommand subCommand : subCommands) {
                if (subCommand.getName().equalsIgnoreCase(args[0])) {
                    subCommand.perform(sender, args, plugin);
                    break;
                }
            }
        } else {
            sender.sendMessage(ChatColor.GREEN + "~ RealmCraft: ResourcePoint ~");
            for (SubCommand subCommand : subCommands) {
                sender.sendMessage(ChatColor.YELLOW + subCommand.getSyntax() + " " + ChatColor.AQUA + subCommand.getDescription());
            }
        }
        return true;

    }
}
