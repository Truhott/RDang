package ru.truhot.rdang.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import java.lang.reflect.Field;
import java.util.UUID;

public class HeadUtil {

    public static ItemStack createSkullFromBase64(String base64, String sectionName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (base64 == null || base64.isEmpty()) {
            System.out.println("[Rdang] отсутствует текстура головы в секции: " + sectionName);
            return head;
        }
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        setSkullTexture(meta, base64, sectionName);
        head.setItemMeta(meta);
        return head;
    }

    public static ItemStack createSkullFromPrefixedString(String input, String sectionName) {
        if (input == null || input.isEmpty()) {
            System.out.println("[Rdang] значение материала пусто в секции: " + sectionName);
            return new ItemStack(Material.PLAYER_HEAD);
        }

        String base64 = isBase64Head(input) ? input.substring(9) : input;
        return createSkullFromBase64(base64, sectionName);
    }

    public static void setSkullTexture(SkullMeta meta, String texture, String sectionName) {
        if (meta == null || texture == null || texture.isEmpty()) return;
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", texture));
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Exception ignored) {}
    }

    public static String getSkullTexture(ItemStack head) {
        if (head == null || head.getType() != Material.PLAYER_HEAD) return null;
        ItemMeta meta = head.getItemMeta();
        if (!(meta instanceof SkullMeta)) return null;

        try {
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            GameProfile profile = (GameProfile) profileField.get(meta);
            if (profile != null && profile.getProperties().containsKey("textures")) {
                return profile.getProperties().get("textures").iterator().next().getValue();
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static boolean isBase64Head(String name) {
        return name != null && name.length() > 9 && name.toLowerCase().startsWith("basehead-");
    }

    public static String extractBase64(String input) {
        if (input == null) return null;
        return isBase64Head(input) ? input.substring(9) : input;
    }
}