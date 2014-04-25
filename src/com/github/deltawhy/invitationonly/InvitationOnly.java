package com.github.deltawhy.invitationonly;

import java.util.List;
import java.util.UUID;

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
			UUID userid = getOfflinePlayerUUID(args[0]);
			if (player != null && !isMember(player.getUniqueId())) {
				player.sendMessage(ChatColor.RED + "You don't have permission to invite people.");
				return true;
			}
			if (isMember(userid)) {
				sender.sendMessage(ChatColor.RED + getServer().getOfflinePlayer(userid).getName() + " is already a member!");
				return true;
			}
			if (isInvited(userid)) {
				sender.sendMessage(ChatColor.RED + getServer().getOfflinePlayer(userid).getName() + " is already invited!");
				return true;
			}
			if (player != null && !player.hasPermission("invitationonly.invite.unlimited")) {
				int playerQuota = userConfig.getConfig().getInt("members."+player.getUniqueId().toString()+".invites-left", 0);
				if (playerQuota == 0) {
					player.sendMessage(ChatColor.RED + "You don't have any invites left!");
					return true;
				} else if (playerQuota > 0) {
					playerQuota--;
					userConfig.getConfig().set("members."+player.getUniqueId().toString()+".invites-left", playerQuota);
					player.sendMessage(ChatColor.GREEN + "You have " + playerQuota + " invites left!");
				}
			}
			invite(userid, player.getUniqueId());
			return true;
		} else if (command.getName().equalsIgnoreCase("uninvite")) {
			if (args.length != 1) return false;
			UUID userid = getOfflinePlayerUUID(args[0]);
			// String senderName = (player == null ? "$CONSOLE$" : player.getName()); TODO
			if (!player.getUniqueId().equals(whoInvited(userid))) {
				sender.sendMessage(ChatColor.RED + "You didn't invite " + getServer().getOfflinePlayer(userid).getName() + "!");
				return true;
			}
			if (player != null && !player.hasPermission("invitationonly.invite.unlimited")) {
				int playerQuota = userConfig.getConfig().getInt("members."+player.getUniqueId().toString()+".invites-left", -1);
				if (playerQuota >= 0) {
					playerQuota++;
					userConfig.getConfig().set("members."+player.getUniqueId().toString()+".invites-left", playerQuota);
				}
			}
			uninvite(userid);
			return true;
		} else if (command.getName().equalsIgnoreCase("invitequota")) {
			if (player == null && args.length == 0) return false;
			UUID userid = getOfflinePlayerUUID(args[0]);
			String username = getServer().getOfflinePlayer(userid).getName();
			if (!isMember(userid)) {
				sender.sendMessage(ChatColor.RED + username + " is not a member!");
				return true;
			}
			if (args.length < 2) {
				int playerQuota = userConfig.getConfig().getInt("members."+userid.toString()+".invites-left", 0);
				sender.sendMessage(ChatColor.GREEN + username + " has " + playerQuota + " invites left.");
			} else if (args.length == 2) {
				try {
					int quota = Integer.parseInt(args[1]);
					userConfig.getConfig().set("members."+userid.toString()+".invites-left", quota);
					userConfig.saveConfig();
				} catch (NumberFormatException e) {
					return false;
				}
			}
			return true;
		} else if (command.getName().equalsIgnoreCase("approveinvite")) {
			if (args.length != 1) return false;
			UUID userid = getOfflinePlayerUUID(args[0]);
			promoteToMember(userid);
			return true;
		} else if (command.getName().equalsIgnoreCase("unapprove")) {
			if (args.length != 1) return false;
			UUID userid = getOfflinePlayerUUID(args[0]);
			if (!isMember(userid)) {
				sender.sendMessage(ChatColor.RED + getServer().getOfflinePlayer(userid).getName() + " is not a member!");
				return true;
			}
			removeFromMembers(userid);
			return true;
		} else if (command.getName().equalsIgnoreCase("voteapprove")) {
			if (player == null) {
				sender.sendMessage("This command can not be used from the console.");
				return true;
			}
			if (player != null && !isMember(player.getUniqueId())) {
				player.sendMessage(ChatColor.RED + "Only members can vote.");
				return true;
			}
			if (args.length != 1) return false;
			UUID userid = getOfflinePlayerUUID(args[0]);
			String username = getServer().getOfflinePlayer(userid).getName();
			if (isMember(userid)) {
				sender.sendMessage(ChatColor.RED + username + " is already a member!");
				return true;
			}
			if (!isInvited(userid)) {
				sender.sendMessage(ChatColor.RED + username + " hasn't been invited!");
				return true;
			}
			voteApprove(userid, player.getUniqueId());
			return true;
		} else if (command.getName().equalsIgnoreCase("voteban")) {
			if (player == null) {
				sender.sendMessage("This command can not be used from the console.");
				return true;
			}
			if (player != null && !isMember(player.getUniqueId())) {
				player.sendMessage(ChatColor.RED + "Only members can vote.");
				return true;
			}
			if (args.length != 1) return false;
			UUID userid = getOfflinePlayerUUID(args[0]);
			String username = getServer().getOfflinePlayer(userid).getName();
			if (isMember(userid)) {
				sender.sendMessage(ChatColor.RED + username + " is a member!");
				return true;
			}
			if (!isInvited(userid)) {
				sender.sendMessage(ChatColor.RED + username + " hasn't been invited!");
				return true;
			}
			voteBan(userid, player.getUniqueId());
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
	
	@SuppressWarnings("deprecation")
	public UUID getOfflinePlayerUUID(String username) {
		return getServer().getOfflinePlayer(username).getUniqueId();
	}
	
	public boolean isMember(UUID userid) {
		return userConfig.getConfig().contains("members."+userid.toString());
	}
	
	public boolean isInvited(UUID userid) {
		return userConfig.getConfig().contains("invited."+userid.toString());
	}
	
	public UUID whoInvited(UUID userid) {
		return UUID.fromString(userConfig.getConfig().getString("invited."+userid.toString()+".invited-by", ""));
	}
	
	public boolean isMemberOnline() {
		for (Player p : getServer().getOnlinePlayers()) {
			if (isMember(p.getUniqueId())) return true;
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
	public void invite(UUID userid, UUID uuid) {
		userConfig.getConfig().createSection("invited."+userid.toString()).set("invited-by", uuid.toString());
		userConfig.saveConfig();
		//if (uuid.equals("$CONSOLE$")) uuid = "An admin";
		getServer().broadcastMessage(ChatColor.YELLOW + getServer().getPlayer(uuid).getName() + " invited " + getServer().getOfflinePlayer(userid).getName() + " to the server!");
	}
	
	//Won't check who invited!
	public void uninvite(UUID userid) {
		userConfig.getConfig().set("invited."+userid.toString(), null);
		userConfig.saveConfig();
		if (getServer().getOfflinePlayer(userid).isOnline() && !isMember(userid) && (!getConfig().getBoolean("open-when-op-online", false) || !isOpOnline())) {
			getServer().getPlayer(userid).kickPlayer("You were un-invited!");
		}
		getServer().broadcastMessage(ChatColor.YELLOW + getServer().getOfflinePlayer(userid).getName() + " was un-invited. You may re-invite them.");
	}
	
	public void promoteToMember(UUID userid) {
		userConfig.getConfig().set("invited."+userid.toString(), null);
		userConfig.getConfig().set("members."+userid.toString()+".invites-left", getConfig().getInt("invite-quota", 0));
		userConfig.saveConfig();
		getServer().broadcastMessage(ChatColor.YELLOW + getServer().getOfflinePlayer(userid).getName() + " is now a member! Congratulations!");
	}
	
	public void removeFromMembers(UUID userid) {
		userConfig.getConfig().set("invited."+userid.toString(), null);
		userConfig.getConfig().set("members."+userid.toString(), null);
		userConfig.saveConfig();
		if (getServer().getOfflinePlayer(userid).isOnline() && (!getConfig().getBoolean("open-when-op-online") || !isOpOnline())) {
			getServer().getPlayer(userid).kickPlayer("You are no longer a member!");
		}
		getServer().broadcastMessage(ChatColor.YELLOW + getServer().getOfflinePlayer(userid).getName() + " is no longer a member. You may re-invite them.");
	}
	
	private void voteApprove(UUID userid, UUID voterid) {
		List<String> approveVotes = userConfig.getConfig().getStringList("invited."+userid.toString()+".approve-votes");
		if (!approveVotes.contains(voterid.toString())) approveVotes.add(voterid.toString());
		int votesReceived = approveVotes.size();
		int votesNeeded = getConfig().getInt("approve-votes-needed", 0);
		if (votesReceived >= votesNeeded) {
			getServer().broadcastMessage(ChatColor.YELLOW + "The tribe has spoken!");
			promoteToMember(userid);
		} else {
			getServer().broadcastMessage(ChatColor.YELLOW + getServer().getPlayer(voterid).getName()+ " voted to make " + getServer().getOfflinePlayer(userid).getName()
					+ " a member. " + (votesNeeded - votesReceived) + " more votes needed.");
		}
		userConfig.getConfig().set("invited."+userid.toString()+".approve-votes", approveVotes);
		userConfig.saveConfig();
	}
	
	@SuppressWarnings("deprecation")
	private void voteBan(UUID userid, UUID voterid) {
		List<String> banVotes = userConfig.getConfig().getStringList("invited."+userid.toString()+".ban-votes");
		if (!banVotes.contains(voterid.toString())) banVotes.add(voterid.toString());
		int votesReceived = banVotes.size();
		int votesNeeded = getConfig().getInt("ban-votes-needed", 0);
		OfflinePlayer player = getServer().getOfflinePlayer(userid);
		if (votesReceived >= votesNeeded) {
			player.setBanned(true); //TODO: Change to non-deprecated BanList.addBan method when it uses UUIDs instead of usernames...
			if (player.isOnline()) getServer().getPlayer(userid).kickPlayer("You have been banned!");
			getServer().broadcastMessage(ChatColor.YELLOW + "The tribe has spoken!");
			getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + " has been banned and cannot be re-invited.");;
		} else {
			getServer().broadcastMessage(ChatColor.YELLOW + getServer().getPlayer(voterid).getName() + " voted to ban " + player.getName()
					+ ". " + (votesNeeded - votesReceived) + " more votes needed.");
		}
		userConfig.getConfig().set("invited."+userid.toString()+".ban-votes", banVotes);
		userConfig.saveConfig();
	}
}
