package me.ninepin.worldboss.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundHelper {

    private SoundHelper() {}

    public static void playSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }

    public static void playError(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.6f);
    }

    public static void playCancel(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
}
