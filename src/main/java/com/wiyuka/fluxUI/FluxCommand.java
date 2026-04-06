package com.wiyuka.fluxUI;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FluxCommand implements CommandExecutor {

    private final EffectManager manager;

    public FluxCommand(EffectManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if(!sender.isOp())  {
            sender.sendMessage(ChatColor.RED + "You're not op!");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("§e用法: /flux <特效名> [参数...]");
            return true;
        }
        String action = args[0].toLowerCase();
        if (action.equals("stop")) {
            manager.stopEffect(player);
            return true;
        }
        String[] effectArgs = new String[args.length - 1];
        System.arraycopy(args, 1, effectArgs, 0, args.length - 1);
        if (manager.playEffect(player, action, effectArgs)) {
            player.sendMessage("§b[FluxUI] 已启动特效: " + action);
        } else {
            player.sendMessage("§c找不到该特效！");
        }
        return true;
    }
}