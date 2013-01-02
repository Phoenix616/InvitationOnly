package com.github.deltawhy.invitationonly;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class InvitationOnly extends JavaPlugin {
	ConfigAccessor userConfig;
	PlayerListener playerListener;
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		Player player = ((sender instanceof Player) ? (Player)sender : null);
		
		if (command.getName().equalsIgnoreCase("invite")) {
			if (args.length != 1) return false;
			String username = getServer().getOfflinePlayer(args[0]).getName();
			String senderName = (player == null ? "$CONSOLE$" : player.getName());
			if (player != null && !isMember(senderName)) {
				player.sendMessage(ChatColor.RED + "You don't have permission to invite people.");
				return true;
			}
			if (isMember(username)) {
				sender.sendMessage(ChatColor.RED + username + " is already a member!");
				return true;
			}
			if (isInvited(username)) {
				sender.sendMessage(ChatColor.RED + username + " is already invited!");
				return true;
			}
			if (player != null && !player.hasPermission("invitationonly.invite.unlimited")) {
				int playerQuota = userConfig.getConfig().getInt("members."+senderName.toLowerCase()+".invites-left", 0);
				if (playerQuota == 0) {
					player.sendMessage(ChatColor.RED + "You don't have any invites left!");
					return true;
				} else if (playerQuota > 0) {
					playerQuota--;
					userConfig.getConfig().set("members."+senderName.toLowerCase()+".invites-left", playerQuota);
				}
			}
			invite(username, senderName);
			return true;
		} else if (command.getName().equalsIgnoreCase("uninvite")) {
			if (args.length != 1) return false;
			String username = getServer().getOfflinePlayer(args[0]).getName();
			String senderName = (player == null ? "$CONSOLE$" : player.getName());
			if (!senderName.equalsIgnoreCase(whoInvited(username))) {
				sender.sendMessage(ChatColor.RED + "You didn't invite " + username + "!");
				return true;
			}
			if (player != null && !player.hasPermission("invitationonly.invite.unlimited")) {
				int playerQuota = userConfig.getConfig().getInt("members."+senderName.toLowerCase()+".invites-left", -1);
				if (playerQuota >= 0) {
					playerQuota++;
					userConfig.getConfig().set("members."+senderName.toLowerCase()+".invites-left", playerQuota);
				}
			}
			uninvite(username);
			return true;
		} else if (command.getName().equalsIgnoreCase("invitequota")) {
			if (player == null && args.length == 0) return false;
			String username = (args.length > 0) ? getServer().getOfflinePlayer(args[0]).getName() : player.getName();
			if (!isMember(username)) {
				sender.sendMessage(ChatColor.RED + username + " is not a member!");
				return true;
			}
			if (args.length < 2) {
				int playerQuota = userConfig.getConfig().getInt("members."+username.toLowerCase()+".invites-left", 0);
				sender.sendMessage(ChatColor.GREEN + username + " has " + playerQuota + " invites left.");
			} else if (args.length == 2) {
				try {
					int quota = Integer.parseInt(args[1]);
					userConfig.getConfig().set("members."+username.toLowerCase()+".invites-left", quota);
					userConfig.saveConfig();
				} catch (NumberFormatException e) {
					return false;
				}
			}
			return true;
		} else if (command.getName().equalsIgnoreCase("approveinvite")) {
			if (args.length != 1) return false;
			String username = getServer().getOfflinePlayer(args[0]).getName();
			promoteToMember(username);
			return true;
		} else if (command.getName().equalsIgnoreCase("unapprove")) {
			if (args.length != 1) return false;
			String username = getServer().getOfflinePlayer(args[0]).getName();
			if (!isMember(username)) {
				sender.sendMessage(ChatColor.RED + username + " is not a member!");
				return true;
			}
			removeFromMembers(username);
			return true;
		} else if (command.getName().equalsIgnoreCase("voteapprove")) {
			if (player == null) {
				sender.sendMessage("This command can not be used from the console.");
				return true;
			}
			if (player != null && !isMember(player.getName())) {
				player.sendMessage(ChatColor.RED + "Only members can vote.");
				return true;
			}
			if (args.length != 1) return false;
			String username = getServer().getOfflinePlayer(args[0]).getName();
			if (isMember(username)) {
				sender.sendMessage(ChatColor.RED + username + " is already a member!");
				return true;
			}
			if (!isInvited(username)) {
				sender.sendMessage(ChatColor.RED + username + " hasn't been invited!");
				return true;
			}
			voteApprove(username, player.getName());
			return true;
		} else if (command.getName().equalsIgnoreCase("voteban")) {
			if (player == null) {
				sender.sendMessage("This command can not be used from the console.");
				return true;
			}
			if (player != null && !isMember(player.getName())) {
				player.sendMessage(ChatColor.RED + "Only members can vote.");
				return true;
			}
			if (args.length != 1) return false;
			String username = getServer().getOfflinePlayer(args[0]).getName();
			if (isMember(username)) {
				sender.sendMessage(ChatColor.RED + username + " is a member!");
				return true;
			}
			if (!isInvited(username)) {
				sender.sendMessage(ChatColor.RED + username + " hasn't been invited!");
				return true;
			}
			voteBan(username, player.getName());
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onEnable() {
		saveDefaultConfig();
		userConfig = new ConfigAccessor(this, "users.yml");
		userConfig.reloadConfig();
		playerListener = new PlayerListener(this);
		getServer().getPluginManager().registerEvents(playerListener, this);
	}
	
	public boolean isMember(String username) {
		return userConfig.getConfig().contains("members."+username.toLowerCase());
	}
	
	public boolean isInvited(String username) {
		return userConfig.getConfig().contains("invited."+username.toLowerCase());
	}
	
	public String whoInvited(String username) {
		return userConfig.getConfig().getString("invited."+username.toLowerCase()+".invited-by", "");
	}
	
	public boolean isMemberOnline() {
		for (Player p : getServer().getOnlinePlayers()) {
			if (isMember(p.getName())) return true;
		}
		return false;
	}
	
	public boolean isOpOnline() {
		for (Player p : getServer().getOnlinePlayers()) {
			if (p.isOp()) return true;
		}
		return false;
	}
	
	//Won't check quotas here!
	public void invite(String username, String whoInvited) {
		userConfig.getConfig().createSection("invited."+username.toLowerCase()).set("invited-by", whoInvited.toLowerCase());
		userConfig.saveConfig();
		if (whoInvited.equals("$CONSOLE$")) whoInvited = "An admin";
		getServer().broadcastMessage(ChatColor.YELLOW + whoInvited + " invited " + username + " to the server!");
	}
	
	//Won't check who invited!
	public void uninvite(String username) {
		userConfig.getConfig().set("invited."+username.toLowerCase(), null);
		userConfig.saveConfig();
		if (getServer().getOfflinePlayer(username).isOnline() && !isMember(username) && (!getConfig().getBoolean("open-when-op-online", false) || !isOpOnline())) {
			getServer().getPlayerExact(username).kickPlayer("You were un-invited!");
		}
		getServer().broadcastMessage(ChatColor.YELLOW + username + " was un-invited. You may re-invite them.");
	}
	
	public void promoteToMember(String username) {
		userConfig.getConfig().set("invited."+username.toLowerCase(), null);
		userConfig.getConfig().set("members."+username.toLowerCase()+".invites-left", getConfig().getInt("invite-quota", 0));
		userConfig.saveConfig();
		getServer().broadcastMessage(ChatColor.YELLOW + username + " is now a member! Congratulations!");
	}
	
	public void removeFromMembers(String username) {
		userConfig.getConfig().set("invited."+username.toLowerCase(), null);
		userConfig.getConfig().set("members."+username.toLowerCase(), null);
		userConfig.saveConfig();
		if (getServer().getOfflinePlayer(username).isOnline() && (!getConfig().getBoolean("open-when-op-online") || !isOpOnline())) {
			getServer().getPlayerExact(username).kickPlayer("You are no longer a member!");
		}
		getServer().broadcastMessage(ChatColor.YELLOW + username + " is no longer a member. You may re-invite them.");
	}
	
	private void voteApprove(String username, String voterName) {
		List<String> approveVotes = userConfig.getConfig().getStringList("invited."+username.toLowerCase()+".approve-votes");
		if (!approveVotes.contains(voterName.toLowerCase())) approveVotes.add(voterName.toLowerCase());
		int votesReceived = approveVotes.size();
		int votesNeeded = getConfig().getInt("approve-votes-needed", 0);
		if (votesReceived >= votesNeeded) {
			getServer().broadcastMessage(ChatColor.YELLOW + "The tribe has spoken!");
			promoteToMember(username);
		} else {
			getServer().broadcastMessage(ChatColor.YELLOW + voterName + " voted to make " + username
					+ " a member. " + (votesNeeded - votesReceived) + " more votes needed.");
		}
		userConfig.getConfig().set("invited."+username.toLowerCase()+".approve-votes", approveVotes);
		userConfig.saveConfig();
	}
	
	private void voteBan(String username, String voterName) {
		List<String> banVotes = userConfig.getConfig().getStringList("invited."+username.toLowerCase()+".ban-votes");
		if (!banVotes.contains(voterName)) banVotes.add(voterName);
		int votesReceived = banVotes.size();
		int votesNeeded = getConfig().getInt("ban-votes-needed", 0);
		if (votesReceived >= votesNeeded) {
			OfflinePlayer player = getServer().getOfflinePlayer(username);
			player.setBanned(true);
			if (player.isOnline()) getServer().getPlayerExact(username).kickPlayer("You have been banned!");
			getServer().broadcastMessage(ChatColor.YELLOW + "The tribe has spoken!");
			getServer().broadcastMessage(ChatColor.YELLOW + username + " has been banned and cannot be re-invited.");;
		} else {
			getServer().broadcastMessage(ChatColor.YELLOW + voterName + " voted to ban " + username
					+ ". " + (votesNeeded - votesReceived) + " more votes needed.");
		}
		userConfig.getConfig().set("invited."+username.toLowerCase()+".ban-votes", banVotes);
		userConfig.saveConfig();
	}
}
