package com.github.deltawhy.invitationonly;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class InvitationOnly extends JavaPlugin {
	ConfigAccessor userConfig;
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		Player player = ((sender instanceof Player) ? (Player)sender : null);
		
		if (command.getName().equalsIgnoreCase("invite")) {
			if (args.length != 1) return false;
			String username = args[0];
			String senderName = (player == null ? "$CONSOLE$" : player.getName());
			if (player != null && !player.hasPermission("invitationonly.invite.unlimited")) {
				int playerQuota = userConfig.getConfig().getInt("members."+username.toLowerCase()+".invites-left", 0);
				if (playerQuota == 0) {
					player.sendMessage(ChatColor.RED + "You don't have any invites left!");
					return true;
				} else if (playerQuota > 0) {
					playerQuota--;
					userConfig.getConfig().set("members."+username.toLowerCase()+".invites-left", playerQuota);
				}
			}
			invite(username, senderName);
			return true;
		} else if (command.getName().equalsIgnoreCase("uninvite")) {
			if (args.length != 1) return false;
			String username = args[0];
			String senderName = (player == null ? "$CONSOLE$" : player.getName());
			if (!senderName.equalsIgnoreCase(whoInvited(username))) {
				sender.sendMessage(ChatColor.RED + "You didn't invite " + username + "!");
				return true;
			}
			if (player != null && !player.hasPermission("invitationonly.invite.unlimited")) {
				int playerQuota = userConfig.getConfig().getInt("members."+username.toLowerCase()+".invites-left", -1);
				if (playerQuota >= 0) {
					playerQuota++;
					userConfig.getConfig().set("members."+username.toLowerCase()+".invites-left", playerQuota);
				}
			}
			uninvite(username);
			return true;
		} else if (command.getName().equalsIgnoreCase("invitequota")) {
			if (player == null && args.length == 0) return false;
			String username = (args.length > 0) ? args[0] : player.getName();
			if (args.length < 2) {
				//TODO: show quota for user
			} else if (args.length == 2) {
				try {
					int quota = Integer.parseInt(args[1]);
					//TODO: update quota for user
				} catch (NumberFormatException e) {
					return false;
				}
			}
			return true;
		} else if (command.getName().equalsIgnoreCase("approveinvite")) {
			if (args.length != 1) return false;
			String username = args[0];
			promoteToMember(username);
			return true;
		} else if (command.getName().equalsIgnoreCase("unapprove")) {
			if (args.length != 1) return false;
			String username = args[0];
			removeFromMembers(username);
			return true;
		} else if (command.getName().equalsIgnoreCase("voteapprove")) {
			if (player == null) {
				sender.sendMessage("This command can not be used from the console.");
				return true;
			}
			if (args.length != 1) return false;
			String username = args[0];
			voteApprove(username, player.getName());
			return true;
		} else if (command.getName().equalsIgnoreCase("voteban")) {
			if (player == null) {
				sender.sendMessage("This command can not be used from the console.");
				return true;
			}
			if (args.length != 1) return false;
			String username = args[0];
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
	}
	
	public boolean isMember(String username) {
		//TODO: check if player is op
		return userConfig.getConfig().contains("members."+username.toLowerCase());
	}
	
	public boolean isInvited(String username) {
		return userConfig.getConfig().contains("invited."+username.toLowerCase());
	}
	
	public String whoInvited(String username) {
		return userConfig.getConfig().getString("invited."+username.toLowerCase()+".invited-by", "");
	}
	
	public boolean isMemberOnline() {
		//TODO
		return false;
	}
	
	public boolean isOpOnline() {
		//TODO
		return false;
	}
	
	//Won't check quotas here!
	public void invite(String username, String whoInvited) {
		userConfig.getConfig().createSection("invited."+username.toLowerCase()).set("invited-by", whoInvited.toLowerCase());
		userConfig.saveConfig();
	}
	
	//Won't check who invited!
	public void uninvite(String username) {
		userConfig.getConfig().set("invited."+username.toLowerCase(), null);
		userConfig.saveConfig();
	}
	
	public void promoteToMember(String username) {
		userConfig.getConfig().set("invited."+username.toLowerCase(), null);
		userConfig.getConfig().set("members."+username.toLowerCase()+".invites-left", getConfig().getInt("invite-quota", 0));
		userConfig.saveConfig();
	}
	
	public void removeFromMembers(String username) {
		userConfig.getConfig().set("invited."+username.toLowerCase(), null);
		userConfig.getConfig().set("members."+username.toLowerCase(), null);
		userConfig.saveConfig();
	}
	
	private void voteApprove(String username, String voterName) {
		List<String> approveVotes = userConfig.getConfig().getStringList("invited."+username.toLowerCase()+".approve-votes");
		if (!approveVotes.contains(voterName.toLowerCase())) approveVotes.add(voterName.toLowerCase());
		if (approveVotes.size() >= getConfig().getInt("approve-votes-needed", 0)) {
			promoteToMember(username);
		} else {
			userConfig.getConfig().set("invited."+username.toLowerCase()+".approve-votes", approveVotes);
			userConfig.saveConfig();
		}
	}
	
	private void voteBan(String username, String voterName) {
		List<String> banVotes = userConfig.getConfig().getStringList("invited."+username.toLowerCase()+".ban-votes");
		if (!banVotes.contains(voterName)) banVotes.add(voterName);
		if (banVotes.size() >= getConfig().getInt("ban-votes-needed", 0)) {
			getServer().getOfflinePlayer(username).setBanned(true);
		} else {
			userConfig.getConfig().set("invited."+username.toLowerCase()+".ban-votes", banVotes);
			userConfig.saveConfig();
		}
	}
}
