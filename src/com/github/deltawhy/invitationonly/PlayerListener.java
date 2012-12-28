package com.github.deltawhy.invitationonly;

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
		if (player.isOp()) return;
		if (plugin.isMember(player.getName())) return;
		if (plugin.isInvited(player.getName())) return;
		
		//Not a member, not invited...
		e.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
		e.setKickMessage("You must be invited to join this server!");
	}
}
