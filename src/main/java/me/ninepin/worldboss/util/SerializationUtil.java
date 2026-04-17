package me.ninepin.worldboss.util;

import org.bukkit.inventory.ItemStack;

import java.util.Base64;

public class SerializationUtil {

    public static String serializeItemStack(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        byte[] bytes = item.serializeAsBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static ItemStack deserializeItemStack(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            return null;
        }
    }
}
