package me.robifoxx.block;

import com.darkblade12.particleeffect.ParticleEffect;
import me.robifoxx.block.api.Config;
import me.robifoxx.block.api.Metrics;
import me.robifoxx.block.mysql.MySQL;
import me.robifoxx.block.mysql.SQLPlayer;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by RobiFoxx.
 * All rights reserved.
 */
public class Main extends JavaPlugin  {
    public MySQL mysql;
    public ArrayList<String> inEdit = new ArrayList<>();
    public HashMap<String, List<String>> blocksss = new HashMap<>();
    public HashMap<String, String> saved_x = new HashMap<>();
    public HashMap<String, String> saved_y = new HashMap<>();
    public HashMap<String, String> saved_z = new HashMap<>();
    public HashMap<String, String> saved_world = new HashMap<>();
    public Config data;
    public boolean useMysql = false;
    public boolean unsafeSave = true;
    public ArrayList<String> eventReturn = new ArrayList<>();
    public boolean findEffect = false;
    public boolean enabled = false;
    public String disabledMsg = "&cBlocks aren't enabled yet!";
    public int checkFullInventory = 0;
    public String fullInventoryMsg = "&c&lYour inventory is full!";
    public Material hideFoundBlocks = Material.AIR;

    public void onEnable() {
        String fileName = this.getDescription().getName();
        if(!(new File("plugins/" + fileName + "/config.yml").exists())) {
            getConfig().options().copyDefaults(true);
            saveConfig();
        }
        {
            Config c = new Config("plugins/" + fileName, "data.yml", this);
            c.create();
            c.setDefault("data.yml");
            if(!c.exists()) {
                c.getConfig().options().copyDefaults(true);
                c.saveConfig();
            }
            data = c;
        }
        if(getConfig().getString("use-mysql").equalsIgnoreCase("true")) {
            mysql = new MySQL(getConfig().getString("mysql-host"), getConfig().getString("mysql-database"), getConfig().getString("mysql-username"), getConfig().getString("mysql-password"));
            createMySQL();
            useMysql = true;
        }
        Bukkit.getPluginManager().registerEvents(new BEvent(this), this);
        if(getConfig().getString("mysql-unsafe-save") != null) {
            if(getConfig().getString("mysql-unsafe-save").equalsIgnoreCase("false")) {
                unsafeSave = false;
            }
        }
        if(getConfig().getStringList("already-found-all-blocks") == null) {
            getConfig().set("already-found-all-blocks", new ArrayList<String>().add("msg %player% You already found all blocks!"));
        }
        if(getConfig().get("enabled") != null) {
            enabled = getConfig().getBoolean("enabled");
            if(getConfig().get("disabled-msg") != null) {
                disabledMsg = getConfig().getString("disabled-msg");
            }
        }
        if(getConfig().get("use-uuid") != null) {
            Utils.useUUID = getConfig().getBoolean("use-uuid");
        }
        if(getConfig().get("check-full-inventory") != null) {
            checkFullInventory = getConfig().getInt("check-full-inventory");
            if(getConfig().get("full-inventory-msg") != null) {
                fullInventoryMsg = getConfig().getString("full-inventory-msg");
            }
        }
        if(getConfig().get("hide-found-blocks") != null) {
            String st = getConfig().getString("hide-found-blocks");
            if(st.equalsIgnoreCase("NONE")) {
                hideFoundBlocks = null;
            } else {
                hideFoundBlocks = Material.valueOf(st);
            }
        } else {
            hideFoundBlocks = null;
        }
        if(getConfig().getString("placeholderapi") != null
                && getConfig().getString("placeholderapi").equalsIgnoreCase("true")) {
            if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                new Placeholders(this).hook();
            } else {
                getLogger().warning("PlaceholderAPI not found, placeholders will not work.");
                getLogger().warning("Please install the following plugin:");
                getLogger().warning("https://www.spigotmc.org/resources/p.6245/");
            }
        }
        findEffect = getConfig().getBoolean("find-effect.enabled");
        boolean enabledParticle = getConfig().getBoolean("particles.enabled");
        if(enabledParticle) {
            int loop = getConfig().getInt("particles.loop");
            String f_type;
            float f_dx;
            float f_dy;
            float f_dz;
            float f_speed;
            int f_quan;
            String nf_type;
            float nf_dx;
            float nf_dy;
            float nf_dz;
            float nf_speed;
            int nf_quan;
            {
                String f = "found";
                f_type = getConfig().getString("particles." + f + ".type");
                f_dx = Float.valueOf(getConfig().getDouble("particles." + f + ".dx") + "");
                f_dy = Float.valueOf(getConfig().getDouble("particles." + f + ".dy") + "");
                f_dz = Float.valueOf(getConfig().getDouble("particles." + f + ".dz") + "");
                f_speed = Float.valueOf(getConfig().getDouble("particles." + f + ".speed") + "");
                f_quan = getConfig().getInt("particles." + f + ".quantity");
            }
            {
                String f = "notfound";
                nf_type = getConfig().getString("particles." + f + ".type");
                nf_dx = Float.valueOf(getConfig().getDouble("particles." + f + ".dx") + "");
                nf_dy = Float.valueOf(getConfig().getDouble("particles." + f + ".dy") + "");
                nf_dz = Float.valueOf(getConfig().getDouble("particles." + f + ".dz") + "");
                nf_speed = Float.valueOf(getConfig().getDouble("particles." + f + ".speed") + "");
                nf_quan = getConfig().getInt("particles." + f + ".quantity");
            }
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
                for(String s : getConfig().getStringList("blocks")) {
                    for(Player pl : Bukkit.getOnlinePlayers()) {
                        //If it was a reload, then dont bother to proceed
                        if(blocksss.get(pl.getName()) != null) {
                            boolean found = blocksss.get(pl.getName()).contains(s);
                            String[] splt = s.split(";");
                            //x;y;z;w
                            Location loc = new Location(Bukkit.getWorld(splt[3]), Integer.valueOf(splt[0]) + 0.5, Integer.valueOf(splt[1]) + 0.25, Integer.valueOf(splt[2]) + 0.5);
                            if(found) {
                                if(!f_type.equalsIgnoreCase("DISABLED")) {
                                    ParticleEffect.valueOf(f_type).display(
                                            f_dx,
                                            f_dy,
                                            f_dz,
                                            f_speed,
                                            f_quan,
                                            loc, pl);
                                }
                            } else {
                                if(!nf_type.equalsIgnoreCase("DISABLED")) {
                                    ParticleEffect.valueOf(nf_type).display(
                                            nf_dx,
                                            nf_dy,
                                            nf_dz,
                                            nf_speed,
                                            nf_quan,
                                            loc, pl);
                                }
                            }
                        }
                    }
                }
            }, loop, loop);
        }
        getLogger().info("Enabling Metrics (bStats).");
        Metrics m = new Metrics(this);
        m.addCustomChart(new Metrics.SingleLineChart("blocks", () -> getConfig().getStringList("blocks").size()));
        getLogger().info("Enabled Metrics.");
    }

    public void createMySQL() {
        mysql.update("CREATE TABLE IF NOT EXISTS " + this.getDescription().getName() + " (UUID varchar(128), X varchar(2048) default \"none\", Y varchar(2048) default \"none\", Z varchar(2048) default \"none\", WORLD varchar(2048) default \"none\")");
    }

    public void onDisable() {
        for(Player pl : Bukkit.getOnlinePlayers()) {
            if(saved_x.get(pl.getName()) != null) {
                if (useMysql) {
                    SQLPlayer.setString(Utils.getIdentifier(pl), "X", saved_x.get(pl.getName()));
                    SQLPlayer.setString(Utils.getIdentifier(pl), "Y", saved_y.get(pl.getName()));
                    SQLPlayer.setString(Utils.getIdentifier(pl), "Z", saved_z.get(pl.getName()));
                    SQLPlayer.setString(Utils.getIdentifier(pl), "WORLD", saved_world.get(pl.getName()));
                } else {
                    data.getConfig().set("data." + Utils.getIdentifier(pl) + ".x", saved_x.get(pl.getName()));
                    data.getConfig().set("data." + Utils.getIdentifier(pl) + ".y", saved_y.get(pl.getName()));
                    data.getConfig().set("data." + Utils.getIdentifier(pl) + ".z", saved_z.get(pl.getName()));
                    data.getConfig().set("data." + Utils.getIdentifier(pl) + ".world", saved_world.get(pl.getName()));
                    data.saveConfig();
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(cmd.getName().equalsIgnoreCase("blockquest")) {
            if(!sender.hasPermission("blockquest.command")) {
                sender.sendMessage(getConfig().getString("no-permission").replace("&", "§"));
                return true;
            }
            //plugin.getConfig().getStringList("blocks").size() - Main.blocksss.get(e.getPlayer().getName()).size()
            if(args.length < 1) {
                if (inEdit.remove(sender.getName())) {
                    sender.sendMessage("§cYou disabled edit mode.");
                } else {
                    sender.sendMessage("§7§m----------------------------------------");
                    sender.sendMessage("§aYou entered edit mode!");
                    sender.sendMessage("§aClick on blocks to add it to the config file!");
                    sender.sendMessage("§aType §6/blockquest §ato exit edit mode.");
                    sender.sendMessage("§7§m----------------------------------------");
                    sender.sendMessage("§a§lType §6§l/blockquest reload §a§lto reload the config!");
                    sender.sendMessage("§a§lType §6§l/blockquest stats [player] §a§lto check stats!");
                    sender.sendMessage("§a§lType §6§l/blockquest save §a§lto save stats!");
                    sender.sendMessage("§7§m----------------------------------------");
                    if(!enabled) {
                        sender.sendMessage("§c§lBlocks are disabled. Players cant find them until you enable it with §6§l/blockquest toggle");
                    }
                    //sender.sendMessage("§a§lType §6§l/blockquest wipedata §a§lto clear data. §c§l!WARNING! This resets EVERYONE'S data!");
                    inEdit.add(sender.getName());
                }
            } else {
                if(args[0].equalsIgnoreCase("reload")) {
                    reloadConfig();
                    sender.sendMessage("§aConfig reloaded!");
                } else if(args[0].equalsIgnoreCase("toggle")) {
                    enabled = !enabled;
                    if(enabled) {
                        sender.sendMessage("§aEnabled Blocks!");
                    } else {
                        sender.sendMessage("§cDisabled Blocks!");
                    }
                    getConfig().set("enabled", enabled);
                    saveConfig();
                } else if(args[0].equalsIgnoreCase("save")) {
                    int amount = 0;
                    for(Player pl : Bukkit.getOnlinePlayers()) {
                        if (saved_x.get(pl.getName()) != null) {
                            amount++;
                            sender.sendMessage("§a§oSaving data for " + pl.getName());
                            if (useMysql) {
                                SQLPlayer.setString(Utils.getIdentifier(pl), "X", saved_x.get(pl.getName()));
                                SQLPlayer.setString(Utils.getIdentifier(pl), "Y", saved_y.get(pl.getName()));
                                SQLPlayer.setString(Utils.getIdentifier(pl), "Z", saved_z.get(pl.getName()));
                                SQLPlayer.setString(Utils.getIdentifier(pl), "WORLD", saved_world.get(pl.getName()));
                            } else {
                                data.getConfig().set("data." + Utils.getIdentifier(pl) + ".x", saved_x.get(pl.getName()));
                                data.getConfig().set("data." + Utils.getIdentifier(pl) + ".y", saved_y.get(pl.getName()));
                                data.getConfig().set("data." + Utils.getIdentifier(pl) + ".z", saved_z.get(pl.getName()));
                                data.getConfig().set("data." + Utils.getIdentifier(pl) + ".world", saved_world.get(pl.getName()));
                                data.saveConfig();
                            }
                        }
                    }
                    sender.sendMessage("§a§lFinished saving data for §6§l" + amount +" §a§l players!");
                } else if(args[0].equalsIgnoreCase("stats")) {
                    int total = 0;
                    int currentBlocks = getConfig().getStringList("blocks").size();
                    if(args.length >= 2) {
                        int foundBlocks;
                        if(!useMysql) {
                            if(data.getConfig().getString("data." + args[1] + ".x") != null) {
                                foundBlocks = data.getConfig().getString("data." + args[1] + ".x").split(";").length - 1;
                            } else {
                                sender.sendMessage("§c§lThere's no player called " + args[1] + "!");
                                if(Utils.useUUID) {
                                    sender.sendMessage("§cIf you have Use UUID on, you should try using player UUID instead of player name");
                                }
                                return true;
                            }
                        } else {
                            if(SQLPlayer.playerExists(args[1])) {
                                foundBlocks = SQLPlayer.getString(args[1], "X").split(";").length - 1;
                            } else {
                                sender.sendMessage("§c§lThere's no player called " + args[1] + "!");
                                if(Utils.useUUID) {
                                    sender.sendMessage("§cIf you have Use UUID on, you should try using player UUID instead of player name");
                                }
                                return true;
                            }
                        }
                        double foundPercent = ((foundBlocks * 1.0) / (currentBlocks * 1.0)) * 100;
                        BigDecimal dec = new BigDecimal(foundPercent).setScale(2, BigDecimal.ROUND_HALF_EVEN);
                        sender.sendMessage("§a§lCurrent Blocks: §e" + currentBlocks);
                        sender.sendMessage("§e§l" + args[1] + "§a§lhas found §e§l" + dec + "% §a§lof all blocks. §e§l(" + foundBlocks + "/" + currentBlocks + ")");
                    } else {
                        int foundAllBlocks = 0;
                        if (!useMysql) {
                            for (String s : data.getConfig().getConfigurationSection("data").getKeys(false)) {
                                if (!s.equalsIgnoreCase("1-1-1-1-1-1")) {
                                    total++;
                                    int foundBlocks = data.getConfig().getString("data." + s + ".x").split(";").length - 1;
                                    if (foundBlocks >= currentBlocks) {
                                        foundAllBlocks++;
                                    }
                                }
                            }
                        } else {
                            for (String s : SQLPlayer.getAll()) {
                                total++;
                                int foundBlocks = SQLPlayer.getString(s, "X").split(";").length - 1;
                                if (foundBlocks >= currentBlocks) {
                                    foundAllBlocks++;
                                }
                            }
                        }
                        double foundPercent = ((foundAllBlocks * 1.0) / (total * 1.0)) * 100;
                        BigDecimal dec = new BigDecimal(foundPercent).setScale(2, BigDecimal.ROUND_HALF_EVEN);
                        sender.sendMessage("§a§lCurrent Blocks: §e" + currentBlocks);
                        sender.sendMessage("§e§l" + dec + "% §a§lhas found all blocks. §e§l(" + foundAllBlocks + "/" + total + ")");
                    }
                } /*else if(args[0].equalsIgnoreCase("wipedata")) {
                    sender.sendMessage("§aWiping data...");
                    boolean success = false;
                    if(useMysql) {
                        mysql.update("DROP TABLE BlockQuest");
                        createMySQL();
                        success = true;
                    } else {

                        Config c = new Config("plugins/BlockQuest", "data.yml");
                        c.create();
                        if(c.toFile().delete()) {
                            c.setDefault("data.yml");
                            c.getConfig().options().copyDefaults(true);
                            c.saveConfig();

                            data = c;
                            success = true;
                        }
                    }
                    if(success) {
                        sender.sendMessage("§aData Wiped successfully!");
                    } else {
                        sender.sendMessage("§cData wipe failed! :(");
                    }
                }*/
            }
        }
        return true;
    }
}
