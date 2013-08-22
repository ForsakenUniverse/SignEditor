package me.meiamsome.signeditor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;
import org.bukkit.plugin.java.JavaPlugin;


public class SignEditor extends JavaPlugin implements Listener {
	HashMap<String, String[]> copies = new HashMap<String, String[]>();
	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(command.getName().equals("edit")) {
			if(!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " You cannot use this from the console.");
				return true;
			}
			final Player play = (Player) sender;
			if(!play.hasPermission("se.edit")) {
				sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " You don't have the permission to do this.");
				return true;
			}
			Block b = play.getTargetBlock(null, 10);
			if(b == null) {
				sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " You are not targeting any block.");
				return true;
			}
			if(!(b.getState() instanceof Sign)) {
				sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " You must be looking at a sign.");
				return true;
			}
			if(args.length == 0) {
				sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " You must select a line number and text.");
				return true;
			}
			if(args[0].equalsIgnoreCase("copy")) {
				copies.put(play.getName(), ((Sign)b.getState()).getLines());
				sender.sendMessage(ChatColor.AQUA + "[SignEditor] Sign Copied.");
				return true;
			}
			int line = -1;
			boolean f = false, doubleForce = false, paste = false;
			String str = args[0];
			try {
				if(str.endsWith("!") && sender.hasPermission("se.force")) {
					str = str.substring(0, str.length()-1);
					f = true;
				}
				if(str.endsWith("!") && sender.hasPermission("se.force") && sender.hasPermission("se.dforce")) {
					str = str.substring(0, str.length()-1);
					doubleForce = true;
				}
				if(str.equalsIgnoreCase("paste")) {
					paste = true;
				} else line = Integer.parseInt(str);
			} catch(Exception e) {
				sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " Could not parse '"+str+"' as a number.");
				return true;
			}
			final boolean force = f;
			final Sign s = (Sign) b.getState();
			final String[] lines;
			if(paste) {
				lines = copies.get(play.getName());
				if(doubleForce) {
					for(int i = 0; i < 4; i++) s.setLine(i, lines[i]);
					return true;
				}
			} else {
				if(line < 1 || line > 4) {
					sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " Line can only be in the range 1-4.");
					return true;
				}
				line --;
				String message = "";
				for(int i = 1; i < args.length; i++) message += " "+args[i];
				//if(play.hasPermission("se.editCol")) message = colourize(message);
				message = message.trim();
				if(message.length() > 15) {
					sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " Line is too long.");
					return true;
				}
				if(doubleForce) {
					s.setLine(line, colourize(message));
					s.update();
					return true;
				}
				lines = s.getLines().clone();
				if(play.hasPermission("se.editCol")) for(int i = 0; i < 4; i++) {
					lines[i] = decolourize(lines[i]);
				}
				lines[line] = message;
			}
			String[] oldLines = lines.clone();
			BlockBreakEvent ev1 = new BlockBreakEvent(b, play) {
				@Override
				public void setCancelled(boolean cancel) {
					if(!force) {
						super.setCancelled(cancel);
						return;
					}
					if(cancel) play.sendMessage(ChatColor.AQUA + "[SignEditor] Forced Bypass of BreakEvent.");
				}
			};
			Bukkit.getPluginManager().callEvent(ev1);
			if(ev1.isCancelled()) {
				play.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " Sign break cancelled by plugin.");
			}
			s.setLine(0, "");
			s.setLine(1, "");
			s.setLine(2, "");
			s.setLine(3, "");
			int spawnRadius = Bukkit.getServer().getSpawnRadius();
			Location spawn = b.getWorld().getSpawnLocation();
			boolean canBuild = spawnRadius <= 0 || play.isOp() || Math.max(Math.abs(b.getX()-spawn.getBlockX()), Math.abs(b.getZ()-spawn.getBlockZ())) > spawnRadius;
			BlockPlaceEvent ev2 = new BlockPlaceEvent(b, b.getState(), b.getRelative(((Attachable)b.getState().getData()).getAttachedFace()), new ItemStack(Material.SIGN), play, canBuild) {
				@Override
				public void setCancelled(boolean cancel) {
					if(!force) {
						super.setCancelled(cancel);
						return;
					}
					if(cancel) play.sendMessage(ChatColor.AQUA + "[SignEditor] Forced Bypass of PlaceEvent.");
				}
			};
			Bukkit.getPluginManager().callEvent(ev2);
			if(ev2.isCancelled()) {
				play.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " Sign place cancelled by plugin.");
			}
			
			SignChangeEvent ev3 = new SignChangeEvent(b, play, lines.clone()) {
				boolean info = false;
				@Override
				public void setCancelled(boolean cancel) {
					if(!force) {
						super.setCancelled(cancel);
						return;
					}
					if(cancel) play.sendMessage(ChatColor.AQUA + "[SignEditor] Forced Bypass of SignChangeEvent.");
				}
				@Override
				public void setLine(int index, String line) throws IndexOutOfBoundsException {
					if(!force) {
						super.setLine(index, line);
						return;
					}
					if(!info) play.sendMessage(ChatColor.AQUA + "[SignEditor] Forced Bypass of SignChangeEvent modification.");
					info = true;
					play.sendMessage(ChatColor.AQUA + "[SignEditor] (Line "+index+" change to "+line+").");
				}
				
			};
			Bukkit.getPluginManager().callEvent(ev3);
			if(!ev3.isCancelled()) {
				String[] changedLines = ev3.getLines();
				boolean changed = false, startDiff = false;
				for(int i = 0; i < changedLines.length; i++) {
					s.setLine(i, changedLines[i]);
					if(!changedLines[i].equals(lines[i])) changed = true;
					if(!changedLines[i].equals(oldLines[i])) startDiff = true;
				}
				if(changed) {
					if(startDiff) {
						play.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " Sign change modified by a plugin.");
					} else {
						play.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " Sign change denied by a plugin.");
					}
				}
			} else {
				play.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " Sign change cancelled by a plugin.");
			}
			s.update();
			return true;
		}
		return false;
	}
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if(command.getName().equals("edit")) {
			if(!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " You cannot use this from the console.");
				return null;
			}
			Player play = (Player) sender;
			if(!play.hasPermission("se.edit")) {
				sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " You don't have the permission to do this.");
				return null;
			}
			Block b = play.getTargetBlock(null, 10);
			if(b == null) {
				sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " You are not targeting any block.");
				return null;
			}
			if(!(b.getState() instanceof Sign)) {
				sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " You must be looking at a sign.");
				return null;
			}
			List<String> ret = new ArrayList<String>();
			if(args.length == 0 || args[0].length() == 0) {
				ret.add("1");
				ret.add("2");
				ret.add("3");
				ret.add("4");
				ret.add("copy");
				ret.add("paste");
				return ret;
			}
			if(args.length == 2 && args[1].trim().length() != 0) {
				sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " You must only select a line number to tab complete.");
				return null;
			}
			int line;
			String str = args[0];
			try {
				if(str.endsWith("!") && sender.hasPermission("se.force")) {
					str = str.substring(0, str.length()-1);
				}
				if(str.endsWith("!") && sender.hasPermission("se.force") && sender.hasPermission("se.dforce")) {
					str = str.substring(0, str.length()-1);
				}
				if("paste".startsWith(str)) {
					ret.add("paste");
					return ret;
				} else if("copy".startsWith(str)) {
					ret.add("copy");
					return ret;
				} else line = Integer.parseInt(str);
			} catch(Exception e) {
				sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " Could not parse '"+args[0]+"' as a number.");
				return null;
			}
			if(line < 1 || line > 4) {
				sender.sendMessage(ChatColor.AQUA + "[SignEditor]" + ChatColor.RED + " Line can only be in the range 1-4.");
				return null;
			}
			line --;
			ret.add(((args.length == 1)?((line+1) + " "):"") + decolourize(((Sign)b.getState()).getLine(line)));
			return ret;
		}
		return super.onTabComplete(sender, command, alias, args);
	}
	
	@EventHandler
	public void signChange(SignChangeEvent e) {
		if(!e.getPlayer().hasPermission("se.editCol")) return;
		for(int i = 0; i < 4; i++) {
			e.setLine(i, colourize(e.getLine(i)));
		}
	}
	
	public String colourize(String in) {
		return (" "+in).replaceAll("([^\\\\](\\\\\\\\)*)&(.)", "$1§$3").replaceAll("([^\\\\](\\\\\\\\)*)&(.)", "$1§$3").replaceAll("(([^\\\\])\\\\((\\\\\\\\)*))&(.)", "$2$3&$5").replaceAll("\\\\\\\\", "\\\\").trim();
	}
	
	public String decolourize(String in) {
		return (" "+in).replaceAll("\\\\","\\\\\\\\").replaceAll("&", "\\\\&").replaceAll("§","&").trim();
	}
}
