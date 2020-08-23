package net.lldv.llamavouchers;

import cn.nukkit.plugin.PluginBase;
import lombok.Getter;
import net.lldv.llamavouchers.commands.RedeemCommand;
import net.lldv.llamavouchers.commands.VoucherCommand;
import net.lldv.llamavouchers.components.api.LlamaVoucherAPI;
import net.lldv.llamavouchers.components.forms.FormListener;
import net.lldv.llamavouchers.components.language.Language;
import net.lldv.llamavouchers.components.provider.MongodbProvider;
import net.lldv.llamavouchers.components.provider.MySqlProvider;
import net.lldv.llamavouchers.components.provider.Provider;
import net.lldv.llamavouchers.components.provider.YamlProvider;

import java.util.HashMap;
import java.util.Map;

public class LlamaVouchers extends PluginBase {

    private final Map<String, Provider> providers = new HashMap<>();
    public Provider provider;

    @Getter
    private static LlamaVouchers instance;

    @Override
    public void onEnable() {
        try {
            instance = this;
            saveDefaultConfig();
            this.providers.put("MongoDB", new MongodbProvider());
            this.providers.put("MySql", new MySqlProvider());
            this.providers.put("Yaml", new YamlProvider());
            if (!this.providers.containsKey(this.getConfig().getString("Provider"))) {
                this.getLogger().error("§4Please specify a valid provider: Yaml, MySql, MongoDB");
                return;
            }
            this.provider = this.providers.get(getConfig().getString("Provider"));
            this.provider.connect(this);
            this.getLogger().info("§aSuccessfully loaded " + provider.getProvider() + " provider.");
            LlamaVoucherAPI.setProvider(provider);
            Language.init();
            this.loadPlugin();
            this.getLogger().info("§aPlugin successfully started.");
        } catch (Exception e) {
            e.printStackTrace();
            this.getLogger().error("§4Failed to load LlamaVouchers.");
        }
    }

    private void loadPlugin() {
        this.getServer().getCommandMap().register("llamavouchers", new VoucherCommand(this));
        this.getServer().getCommandMap().register("llamavouchers", new RedeemCommand(this));

        this.getServer().getPluginManager().registerEvents(new FormListener(), this);
    }

    @Override
    public void onDisable() {
        this.provider.disconnect(this);
    }
}
