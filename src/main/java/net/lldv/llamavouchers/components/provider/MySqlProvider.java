package net.lldv.llamavouchers.components.provider;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.utils.Config;
import net.lldv.llamavouchers.LlamaVouchers;
import net.lldv.llamavouchers.components.data.Voucher;
import net.lldv.llamavouchers.components.language.Language;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class MySqlProvider extends Provider {

    private Connection connection;

    @Override
    public void connect(LlamaVouchers instance) {
        CompletableFuture.runAsync(() -> {
            try {
                Config config = instance.getConfig();
                Class.forName("com.mysql.jdbc.Driver");
                this.connection = DriverManager.getConnection("jdbc:mysql://" + config.getString("MySql.Host") + ":" + config.getString("MySql.Port") + "/" + config.getString("MySql.Database") + "?autoReconnect=true&useGmtMillisForDatetimes=true&serverTimezone=GMT", config.getString("MySql.User"), config.getString("MySql.Password"));
                this.update("CREATE TABLE IF NOT EXISTS vouchers(id VARCHAR(30), player VARCHAR(30), uses INTEGER(30), players VARCHAR(255), rewards VARCHAR(255), time BIGINT(50), PRIMARY KEY (id));");
                instance.getLogger().info("[MySqlClient] Connection opened.");
            } catch (Exception e) {
                e.printStackTrace();
                instance.getLogger().info("[MySqlClient] Failed to connect to database.");
            }
        });
    }

    public void update(String query) {
        CompletableFuture.runAsync(() -> {
            if (this.connection != null) {
                try {
                    PreparedStatement preparedStatement = this.connection.prepareStatement(query);
                    preparedStatement.executeUpdate();
                    preparedStatement.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void createVoucher(Player player, int uses, Duration duration, List<String> rewards) {
        StringBuilder s = new StringBuilder();
        for (String e : rewards) s.append(e).append(":");
        String id = this.getCode() + "-" + this.getCode() + "-" + this.getCode();
        long time = System.currentTimeMillis() + duration.toMillis();
        this.update("INSERT INTO vouchers (ID, PLAYER, USES, PLAYERS, REWARDS, TIME) VALUES ('" + id + "', '-', '" + uses + "', '-', '" + s + "', '" + time + "');");
        player.sendMessage(Language.get("voucher-created", id));
    }

    @Override
    public void createVoucher(Player player, String target, Duration duration, List<String> rewards) {
        StringBuilder s = new StringBuilder();
        for (String e : rewards) s.append(e).append(":");
        String id = this.getCode() + "-" + this.getCode() + "-" + this.getCode();
        long time = System.currentTimeMillis() + duration.toMillis();
        this.update("INSERT INTO vouchers (ID, PLAYER, USES, PLAYERS, REWARDS, TIME) VALUES ('" + id + "', '" + target + "', '-1', '-', '" + s + "', '" + time + "');");
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
        if (!voucher.getPlayer().equals("-") && !voucher.getPlayer().equals(player.getName())) {
            player.sendMessage(Language.get("voucher-not-found", code));
            return;
        }
        if (voucher.getPlayers().size() >= voucher.getUses() & voucher.getPlayer().equals("-")) {
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
        if (voucher.getPlayer().equals("-")) {
            this.addPlayer(player.getName(), voucher);
            if (voucher.getPlayers().size() + 1 >= voucher.getUses()) this.deleteVoucher(voucher.getId());
        } else this.deleteVoucher(voucher.getId());
    }

    @Override
    public void addPlayer(String player, Voucher voucher) {
        String players = "";
        if (voucher.getPlayers().contains("-")) {
            players = player + ":";
            this.update("UPDATE vouchers SET PLAYERS= '" + players + "' WHERE ID= '" + voucher.getId() + "';");
        } else {
            for (String e : voucher.getPlayers()) {
                players = players + e + ":";
            }
            players = players + player + ":";
            this.update("UPDATE vouchers SET PLAYERS= '" + players + "' WHERE ID= '" + voucher.getId() + "';");
        }
    }

    @Override
    public void deleteVoucher(String id) {
        CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement preparedStatement = this.connection.prepareStatement("DELETE FROM vouchers WHERE ID = ?");
                preparedStatement.setString(1, id);
                preparedStatement.executeUpdate();
                preparedStatement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Voucher getVoucher(String id) {
        Voucher voucher = null;
        try {
            PreparedStatement preparedStatement = this.connection.prepareStatement("SELECT * FROM vouchers WHERE ID = ?");
            preparedStatement.setString(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                List<String> players = Arrays.asList(resultSet.getString("PLAYERS").split(":"));
                List<String> rewards = Arrays.asList(resultSet.getString("REWARDS").split(":"));
                voucher = new Voucher(resultSet.getString("ID"), resultSet.getString("PLAYER"), resultSet.getInt("USES"), players, resultSet.getLong("TIME"), rewards);
            }
            resultSet.close();
            preparedStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return voucher;
    }

    @Override
    public String getProvider() {
        return "MySql";
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
