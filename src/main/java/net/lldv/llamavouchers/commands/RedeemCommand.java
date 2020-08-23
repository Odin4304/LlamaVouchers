package net.lldv.llamavouchers.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import net.lldv.llamavouchers.LlamaVouchers;
import net.lldv.llamavouchers.components.api.LlamaVoucherAPI;
import net.lldv.llamavouchers.components.language.Language;

public class RedeemCommand extends Command {

    public RedeemCommand(LlamaVouchers instance) {
        super(instance.getConfig().getString("Commands.Redeem.Name"), instance.getConfig().getString("Commands.Redeem.Description"));
    }

    @Override
    public boolean execute(CommandSender sender, String s, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1) {
                String id = args[0];
                LlamaVoucherAPI.getProvider().redeemCode(player, id);
            } else player.sendMessage(Language.get("redeem-usage", getName()));
        }
        return false;
    }
}
