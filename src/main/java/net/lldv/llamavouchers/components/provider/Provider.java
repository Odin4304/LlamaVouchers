package net.lldv.llamavouchers.components.provider;

import cn.nukkit.Player;
import net.lldv.llamavouchers.LlamaVouchers;
import net.lldv.llamavouchers.components.data.Voucher;

import java.time.Duration;
import java.util.List;

public class Provider {

    public void connect(LlamaVouchers instance) {

    }

    public void disconnect(LlamaVouchers instance) {

    }

    public void createVoucher(Player player, int uses, Duration duration, List<String> rewards) {

    }

    public void createVoucher(Player player, String target, Duration duration, List<String> rewards) {

    }

    public void redeemCode(Player player, String code) {

    }

    public void addPlayer(String player, Voucher voucher) {

    }

    public void deleteVoucher(String id) {

    }

    public Voucher getVoucher(String id) {
        return null;
    }

    public String getProvider() {
        return null;
    }

}
