package net.lldv.llamavouchers.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementSlider;
import net.lldv.llamavouchers.LlamaVouchers;
import net.lldv.llamavouchers.components.api.LlamaVoucherAPI;
import net.lldv.llamavouchers.components.forms.custom.CustomForm;
import net.lldv.llamavouchers.components.language.Language;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VoucherCommand extends Command {

    public VoucherCommand(LlamaVouchers instance) {
        super(instance.getConfig().getString("Commands.Voucher.Name"), instance.getConfig().getString("Commands.Voucher.Description"));
        this.setPermission("llamavouchers.command.voucher");
    }

    @Override
    public boolean execute(CommandSender sender, String s, String[] args) {
        if (sender instanceof Player) {
            if (sender.hasPermission(this.getPermission())) {
                Player player = (Player) sender;
                if (args.length == 1) {
                    if (args[0].equalsIgnoreCase("public")) {
                        CustomForm form = new CustomForm.Builder(Language.getNP("form-public-title"))
                                .addElement(new ElementSlider(Language.getNP("form-public-uses"), 1, 100, 1, 1))
                                .addElement(new ElementInput(Language.getNP("form-public-time")))
                                .addElement(new ElementInput(Language.getNP("form-public-rewards")))
                                .onSubmit(((e, r) -> {
                                    try {
                                        int uses = (int) r.getSliderResponse(0);
                                        try {
                                            Duration duration = Duration.parse("PT" + r.getInputResponse(1).toUpperCase());
                                            String rawRewards = r.getInputResponse(2);
                                            if (rawRewards.isEmpty()) {
                                                player.sendMessage(Language.get("invalid-rewards"));
                                                return;
                                            }
                                            List<String> rewards = new ArrayList<>(Arrays.asList(rawRewards.split(",")));
                                            LlamaVoucherAPI.getProvider().createVoucher(player, uses, duration, rewards);
                                        } catch (Exception exception) {
                                            player.sendMessage(Language.get("invalid-time"));
                                        }
                                    } catch (NumberFormatException exception) {
                                        player.sendMessage(Language.get("invalid-number"));
                                    }
                                }))
                                .build();
                        form.send(player);
                    } else if (args[0].equalsIgnoreCase("player")) {
                        CustomForm form = new CustomForm.Builder(Language.getNP("form-player-title"))
                                .addElement(new ElementInput(Language.getNP("form-player-player")))
                                .addElement(new ElementInput(Language.getNP("form-player-time")))
                                .addElement(new ElementInput(Language.getNP("form-player-rewards")))
                                .onSubmit(((e, r) -> {
                                    try {
                                        String target = r.getInputResponse(0);
                                        if (target.isEmpty()) {
                                            player.sendMessage(Language.get("invalid-target"));
                                            return;
                                        }
                                        Duration duration = Duration.parse("PT" + r.getInputResponse(1).toUpperCase());
                                        String rawRewards = r.getInputResponse(2);
                                        if (rawRewards.isEmpty()) {
                                            player.sendMessage(Language.get("invalid-rewards"));
                                            return;
                                        }
                                        List<String> rewards = new ArrayList<>(Arrays.asList(rawRewards.split(",")));
                                        LlamaVoucherAPI.getProvider().createVoucher(player, target, duration, rewards);
                                    } catch (Exception exception) {
                                        player.sendMessage(Language.get("invalid-time"));
                                    }
                                }))
                                .build();
                        form.send(player);
                    } else player.sendMessage(Language.get("voucher-usage", getName()));
                } else player.sendMessage(Language.get("voucher-usage", getName()));
            } else sender.sendMessage(Language.get("no-permission"));
        }
        return false;
    }
}
