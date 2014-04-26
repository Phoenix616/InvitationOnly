package com.github.deltawhy.invitationonly;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {
	InvitationOnly plugin;
	
	public PlayerListener(InvitationOnly plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent e) {
		if (e.getResult() != PlayerLoginEvent.Result.ALLOWED) return;
		Player player = e.getPlayer();
		UUID userid = player.getUniqueId();
		plugin.updateConfigName(userid, player.getName());
		if (player.isOp() || plugin.isMember(userid)) return;
		if (plugin.isInvited(userid)) {
			if (plugin.getConfig().getBoolean("require-op-online", false) && !plugin.isOpOnline()) {
				e.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
				e.setKickMessage(ChatColor.YELLOW + "You can't join this server unless an op is online!");
				if (plugin.getConfig().getBoolean("broadcast-failed-logins", true)) plugin.getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + " tried to join but there is no OP online.");
			} else if (plugin.getConfig().getBoolean("require-member-online", false) && !plugin.isMemberOnline()) {
				e.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
				e.setKickMessage(ChatColor.YELLOW + "You can't join this server unless a member is online!");
			}
		} else if (plugin.getConfig().getBoolean("open-when-op-online", false) && plugin.isOpOnline()) {
			//allow access!
		} else {
			//Not a member, not invited...
			e.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
			e.setKickMessage(ChatColor.RED + "You must be invited to join this server!");
			if (plugin.getConfig().getBoolean("broadcast-failed-logins", true)) plugin.getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + " tried to join but hasn't been invited.");
		}
	}
	
	//Event for kicking players if there are no OPs/members online anymore!
	@EventHandler
	public void onPlayerDisconnect(PlayerQuitEvent e){
		//1 Tick wait because the PlayerQuitEvent gets invoked before the player actually has left the server!
		Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
			@Override
			public void run() {
				plugin.checkPlayers();
			}
		}, 1L);	
	}
}
