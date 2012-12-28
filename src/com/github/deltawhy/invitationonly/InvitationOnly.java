package com.github.deltawhy.invitationonly;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class InvitationOnly extends JavaPlugin {
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		Player player = ((sender instanceof Player) ? (Player)sender : null);
		
		if (command.getName().equalsIgnoreCase("invite")) {
			if (args.length != 1) return false;
			String username = args[0];
			String senderName = (player == null ? "$CONSOLE$" : player.getName());
			//TODO: update quota
			invite(username, senderName);
			return true;
		} else if (command.getName().equalsIgnoreCase("uninvite")) {
			if (args.length != 1) return false;
			String username = args[0];
			String senderName = (player == null ? "$CONSOLE$" : player.getName());
			//TODO: check if sender was the inviter, update quota
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
	public void onDisable() {

	}

	@Override
	public void onEnable() {
		
	}
	
	public boolean isMember(String username) {
		//TODO
		return false;
	}
	
	public boolean isInvited(String username) {
		//TODO
		return false;
	}
	
	public String whoInvited(String username) {
		//TODO
		return "";
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
		//TODO
	}
	
	//Won't check who invited!
	public void uninvite(String username) {
		
	}
	
	public void promoteToMember(String username) {
		
	}
	
	private void voteApprove(String username, String voterName) {
		
	}
	
	private void voteBan(String username, String voterName) {
		
	}
}
