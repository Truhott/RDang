package ru.truhot.rdang.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import java.lang.reflect.Field;
import java.util.UUID;
public class HeadUtil {

    public static ItemStack createSkullFromBase64(String base64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (base64 == null || base64.isEmpty()) {
            return head;
        }
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;
        setSkullTexture(meta, base64);
        head.setItemMeta(meta);
        return head;
    }

    public static ItemStack createSkullFromPrefixedString(String base64WithPrefix) {
        if (base64WithPrefix == null || base64WithPrefix.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }
        if (!base64WithPrefix.toLowerCase().startsWith("basehead-")) {
            return createSkullFromBase64(base64WithPrefix);
        }
        String base64 = base64WithPrefix.substring(9);
        return createSkullFromBase64(base64);
    }


    public static void setSkullTexture(SkullMeta meta, String texture) {
        if (meta == null || texture == null || texture.isEmpty()) {
            return;
        }
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", texture));
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[HeadUtil] Ошибка при установке текстуры головы: " + e.getMessage());
        }
    }

    public static String getSkullTexture(ItemStack head) {
        if (head == null || head.getType() != Material.PLAYER_HEAD) {
            return null;
        }
        ItemMeta meta = head.getItemMeta();
        if (!(meta instanceof SkullMeta)) {
            return null;
        }
        SkullMeta skullMeta = (SkullMeta) meta;
        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            GameProfile profile = (GameProfile) profileField.get(skullMeta);
            if (profile != null && profile.getProperties().containsKey("textures")) {
                for (Property property : profile.getProperties().get("textures")) {
                    if ("textures".equals(property.getName())) {
                        return property.getValue();
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static boolean isBase64Head(String materialName) {
        if (materialName == null) return false;
        return materialName.toLowerCase().startsWith("basehead-");
    }

    public static String extractBase64(String input) {
        if (input == null) return null;
        if (input.toLowerCase().startsWith("basehead-")) {
            return input.substring(9);
        }
        return input;
    }
}
