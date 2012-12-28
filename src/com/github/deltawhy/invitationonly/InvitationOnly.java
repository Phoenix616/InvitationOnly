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
			//TODO: add user to probation whitelist
			return true;
		} else if (command.getName().equalsIgnoreCase("uninvite")) {
			if (args.length != 1) return false;
			String username = args[0];
			String senderName = (player == null ? "$CONSOLE$" : player.getName());
			//TODO: check if sender was the inviter
			//TODO: remove user from probation whitelist
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
			//TODO: add user to member list
			return true;
		} else if (command.getName().equalsIgnoreCase("voteapprove")) {
			if (args.length != 1) return false;
			String username = args[0];
			//TODO: log votes
			//TODO: approve if threshold reached
			return true;
		} else if (command.getName().equalsIgnoreCase("voteban")) {
			if (args.length != 1) return false;
			String username = args[0];
			//TODO: log votes
			//TODO: approve if threshold reached
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onDisable() {
		// TODO Auto-generated method stub
		super.onDisable();
	}

	@Override
	public void onEnable() {
		// TODO Auto-generated method stub
		super.onEnable();
	}
}
