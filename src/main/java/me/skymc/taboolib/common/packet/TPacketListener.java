package me.skymc.taboolib.common.packet;

import org.bukkit.entity.Player;

/**
 * @Author 坏黑
 * @Since 2018-10-28 14:35
 */
public abstract class TPacketListener {

    public boolean onSend(Player player, Object packet) {
        return true;
    }

    public boolean onReceive(Player player, Object packet) {
        return true;
    }

}
