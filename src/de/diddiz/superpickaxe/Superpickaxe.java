package de.diddiz.superpickaxe;

import static org.bukkit.Bukkit.getPluginCommand;
import static org.bukkit.Bukkit.getPluginManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import de.diddiz.LogBlock.Consumer;
import de.diddiz.LogBlock.LogBlock;
import com.sk89q.wepif.PermissionsResolverManager; 

// Dropped "Permissions" support, now imports "WEPIF" to support
// many major permissions plugins and even BukkitPermissions

public class Superpickaxe extends JavaPlugin implements Listener
{
	private final Set<String> playerswithsp = new HashSet<String>();
	private Set<Integer> tools, dontBreak;
	private boolean disableDrops, overrideWorldEditCommands;
	private Consumer consumer;

	@Override
	public void onEnable() {
		final PluginManager pm = getPluginManager();
		PermissionsResolverManager.initialize(this);
		final Map<String, Object> def = new HashMap<String, Object>();
		def.put("overrideWorldEditCommands", true);
		def.put("tools", Arrays.asList(257, 270, 274, 278, 285)); // Iron, Wood, Stone, Gold & Diamond Picks
		def.put("dontBreak", Arrays.asList(7));
		def.put("disableDrops", false);
		final FileConfiguration config = getConfig();
		for (final Entry<String, Object> e : def.entrySet())
			if (!config.contains(e.getKey()))
				config.set(e.getKey(), e.getValue());
		saveConfig();
		final ConfigurationSection cfg = getConfig();
		tools = new HashSet<Integer>(cfg.getIntegerList("tools"));
		dontBreak = new HashSet<Integer>(cfg.getIntegerList("dontBreak"));
		disableDrops = cfg.getBoolean("disableDrops", false);
		overrideWorldEditCommands = cfg.getBoolean("overrideWorldEditCommands");
		if (disableDrops) {
			if (getPluginManager().isPluginEnabled("LogBlock"))
				consumer = ((LogBlock)getPluginManager().getPlugin("LogBlock")).getConsumer();
			else {
				getLogger().severe(" LogBlock not found! (REQUIRED)");
				consumer = null;
			}
		} else
			consumer = null;
		pm.registerEvents(this, this);
		if (overrideWorldEditCommands) {
			getLogger().info("SPA Now Overriding WorldEdit commands");
			for (final String cmd : new String[]{"/", "superpickaxe"})
				if (getPluginCommand(cmd) != null)
					getPluginCommand(cmd).setExecutor(this);
		}
		getLogger().info("SuperPickaxe (Version: " + getDescription().getVersion() + ") enabled!");
	}

	@Override
	public void onDisable() {
		getLogger().info("SuperPickaxe disabled");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (sender instanceof Player) {
			final Player player = (Player)sender;
			if (PermissionsResolverManager.getInstance().hasPermission(player, "superpickaxe.use")) 
			{
				if (hasEnabled(player)) {
					removePlayer(player);
				} else {
					addPlayer(player);
				}
			} else
				player.sendMessage(ChatColor.DARK_RED + "You do not have permission to use that!");
		} else
			sender.sendMessage(ChatColor.DARK_RED + "This command cannot be used unless you are a player!");
		return true;
	}

	@EventHandler
	public void onBlockDamage(BlockDamageEvent event) {
		final Player player = event.getPlayer();
		final ItemStack tool = event.getItemInHand();
		if (!event.isCancelled() && hasEnabled(player) && tool != null && tools.contains(tool.getTypeId()) && !(dontBreak.contains(event.getBlock().getTypeId()) && !PermissionsResolverManager.getInstance().hasPermission(player, "superpickaxe.breakAll")))
		{
			if (disableDrops && consumer != null) 
			{
				consumer.queueBlockBreak(player.getName(), event.getBlock().getState());
				event.getBlock().setTypeId(0);
				event.setCancelled(true);
			} 
			else 
			{
				event.setInstaBreak(true);
				if (tool.getEnchantments().isEmpty() && PermissionsResolverManager.getInstance().hasPermission(player, "superpickaxe.notooldamage")) 
				{
					tool.setDurability((short)(tool.getDurability() - 1));
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		/*final Player player = event.getPlayer();*/
		if (!event.isCancelled() && overrideWorldEditCommands) {
			final String msg = event.getMessage().toLowerCase();
			if (msg.equals("/") || msg.equals("//") || msg.equals("/,") || msg.equals("/sp")) {
				event.setMessage("dummy");
				event.setCancelled(true);
				getServer().dispatchCommand(event.getPlayer(), "spa");
			}
		}
		/* if (PermissionsResolverManager.getInstance().hasPermission(player, "superpickaxe.reload")) {
			final String msg = event.getMessage().toLowerCase();
			if (msg.equals("/spareload")) {
				this.reloadConfig();
				player.sendMessage(ChatColor.DARK_GREEN + "SPA Configuration Reloaded!");
			}				
		} else
			player.sendMessage(ChatColor.DARK_GREEN + "You do not have permission to use that!");*/
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		final Player player = event.getPlayer();
		if (hasEnabled(player) && !PermissionsResolverManager.getInstance().hasPermission(player, "superpickaxe.use"))
		{
			removePlayer(player);
		}
	}

	void addPlayer(Player player) {
		playerswithsp.add(player.getName());
		player.sendMessage(ChatColor.AQUA + "SuperPickaxe now enabled!");
	}

	void removePlayer(Player player) {
		playerswithsp.remove(player.getName());
		player.sendMessage(ChatColor.AQUA + "SuperPickaxe now disabled!");
	}

	boolean hasEnabled(Player player) {
		return playerswithsp.contains(player.getName());
	}

}