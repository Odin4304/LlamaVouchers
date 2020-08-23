package net.lldv.llamavouchers.components.provider;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.utils.Config;
import net.lldv.llamavouchers.LlamaVouchers;
import net.lldv.llamavouchers.components.data.Voucher;
import net.lldv.llamavouchers.components.language.Language;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class YamlProvider extends Provider {

    private Config vouchers;

    @Override
    public void connect(LlamaVouchers instance) {
        instance.saveResource("/data/vouchers.yml");
        this.vouchers = new Config(instance.getDataFolder() + "/data/vouchers.yml", Config.YAML);
        instance.getLogger().info("[Configuration] Ready.");
    }

    @Override
    public void createVoucher(Player player, int uses, Duration duration, List<String> rewards) {
        List<String> list = new ArrayList<>();
        String id = this.getCode() + "-" + this.getCode() + "-" + this.getCode();
        this.vouchers.set("vouchers." + id + ".uses", uses);
        this.vouchers.set("vouchers." + id + ".players", list);
        this.vouchers.set("vouchers." + id + ".time", System.currentTimeMillis() + duration.toMillis());
        this.vouchers.set("vouchers." + id + ".rewards", rewards);
        this.vouchers.save();
        this.vouchers.reload();
        player.sendMessage(Language.get("voucher-created", id));
    }

    @Override
    public void createVoucher(Player player, String target, Duration duration, List<String> rewards) {
        String id = this.getCode() + "-" + this.getCode() + "-" + this.getCode();
        this.vouchers.set("vouchers." + id + ".player", target);
        this.vouchers.set("vouchers." + id + ".time", System.currentTimeMillis() + duration.toMillis());
        this.vouchers.set("vouchers." + id + ".rewards", rewards);
        this.vouchers.save();
        this.vouchers.reload();
        player.sendMessage(Language.get("voucher-created", id));
    }

    @Override
    public void redeemCode(Player player, String code) {
        Voucher voucher = this.getVoucher(code);
        if (voucher == null) {
            player.sendMessage(Language.get("voucher-not-found", code));
            return;
        }
        if (voucher.getTime() < System.currentTimeMillis()) {
            player.sendMessage(Language.get("voucher-not-found", code));
            this.deleteVoucher(voucher.getId());
            return;
        }
        if (!voucher.getPlayer().equals("") && !voucher.getPlayer().equals(player.getName())) {
            player.sendMessage(Language.get("voucher-not-found", code));
            return;
        }
        if (voucher.getPlayers().size() >= voucher.getUses() && voucher.getPlayer().equals("")) {
            player.sendMessage(Language.get("voucher-not-found", code));
            this.deleteVoucher(voucher.getId());
            return;
        }
        if (voucher.getPlayers().contains(player.getName())) {
            player.sendMessage(Language.get("voucher-already-redeemed", code));
            return;
        }
        voucher.getRewards().forEach(reward -> Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), reward.replace("%p", player.getName())));
        player.sendMessage(Language.get("voucher-redeemed", code));
        if (voucher.getPlayer().equals("")) {
            this.addPlayer(player.getName(), voucher);
            if (voucher.getPlayers().size() + 1 >= voucher.getUses()) this.deleteVoucher(voucher.getId());
        } else this.deleteVoucher(voucher.getId());
    }

    @Override
    public void addPlayer(String player, Voucher voucher) {
        List<String> list = voucher.getPlayers();
        list.add(player);
        this.vouchers.set("vouchers." + voucher.getId() + ".players", list);
        this.vouchers.save();
        this.vouchers.reload();
    }

    @Override
    public void deleteVoucher(String id) {
        Map<String, Object> map = this.vouchers.getSection("vouchers").getAllMap();
        map.remove(id);
        this.vouchers.set("vouchers", map);
        this.vouchers.save();
        this.vouchers.reload();
    }

    @Override
    public Voucher getVoucher(String id) {
        if (this.vouchers.exists("vouchers." + id)) {
            String player = this.vouchers.getString("vouchers." + id + ".player");
            System.out.println(player);
            int uses = this.vouchers.getInt("vouchers." + id + ".uses");
            List<String> players = this.vouchers.getStringList("vouchers." + id + ".players");
            long time = this.vouchers.getLong("vouchers." + id + ".time");
            List<String> rewards = this.vouchers.getStringList("vouchers." + id + ".rewards");
            return new Voucher(id, player, uses, players, time, rewards);
        }
        return null;
    }

    @Override
    public String getProvider() {
        return "Yaml";
    }

    private String getCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();
        while (stringBuilder.length() < 3) {
            int index = (int) (random.nextFloat() * chars.length());
            stringBuilder.append(chars.charAt(index));
        }
        return stringBuilder.toString();
    }
}
