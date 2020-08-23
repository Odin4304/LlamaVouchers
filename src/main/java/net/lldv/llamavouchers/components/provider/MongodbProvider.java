package net.lldv.llamavouchers.components.provider;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.utils.Config;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.lldv.llamavouchers.LlamaVouchers;
import net.lldv.llamavouchers.components.data.Voucher;
import net.lldv.llamavouchers.components.language.Language;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MongodbProvider extends Provider {

    private final Config config = LlamaVouchers.getInstance().getConfig();

    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private MongoCollection<Document> voucherCollection;

    @Override
    public void connect(LlamaVouchers instance) {
        CompletableFuture.runAsync(() -> {
            MongoClientURI uri = new MongoClientURI(this.config.getString("MongoDB.Uri"));
            this.mongoClient = new MongoClient(uri);
            this.mongoDatabase = this.mongoClient.getDatabase(this.config.getString("MongoDB.Database"));
            this.voucherCollection = this.mongoDatabase.getCollection("vouchers");
            Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
            mongoLogger.setLevel(Level.OFF);
            instance.getLogger().info("[MongoClient] Connection opened.");
        });
    }

    @Override
    public void disconnect(LlamaVouchers instance) {
        this.mongoClient.close();
        instance.getLogger().info("[MongoClient] Connection closed.");
    }

    @Override
    public void createVoucher(Player player, String target, Duration duration, List<String> rewards) {
        CompletableFuture.runAsync(() -> {
            String id = this.getCode() + "-" + this.getCode() + "-" + this.getCode();
            Document document = new Document("id", id)
                    .append("player", target)
                    .append("time", System.currentTimeMillis() + duration.toMillis())
                    .append("rewards", rewards);
            this.voucherCollection.insertOne(document);
            player.sendMessage(Language.get("voucher-created", id));
        });
    }

    @Override
    public void createVoucher(Player player, int uses, Duration duration, List<String> rewards) {
        CompletableFuture.runAsync(() -> {
            List<String> list = new ArrayList<>();
            String id = this.getCode() + "-" + this.getCode() + "-" + this.getCode();
            Document document = new Document("id", id)
                    .append("uses", uses)
                    .append("players", list)
                    .append("time", System.currentTimeMillis() + duration.toMillis())
                    .append("rewards", rewards);
            this.voucherCollection.insertOne(document);
            player.sendMessage(Language.get("voucher-created", id));
        });
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
        if (voucher.getPlayers().size() >= voucher.getUses()) {
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
        List<String> players = voucher.getPlayers();
        players.add(player);
        Bson playersSet = new Document("$set", new Document("players", players));
        this.voucherCollection.updateOne(new Document("id", voucher.getId()), playersSet);
    }

    @Override
    public void deleteVoucher(String id) {
        this.voucherCollection.findOneAndDelete(new Document("id", id));
    }

    @Override
    public Voucher getVoucher(String id) {
        Document document = this.voucherCollection.find(new Document("id", id)).first();
        assert document != null;
        String player = document.getString("player");
        int uses = document.getInteger("ueses");
        List<String> players = document.getList("players", String.class);
        long time = document.getLong("time");
        List<String> rewards = document.getList("rewards", String.class);
        return new Voucher(id, player, uses, players, time, rewards);
    }

    @Override
    public String getProvider() {
        return "MongoDB";
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
