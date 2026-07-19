package ru.truhot.rdang.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import ru.truhot.rdang.util.logger.Logger;

import java.util.UUID;

public class HeadUtil {

    public static @NotNull ItemStack createSkullBase64(String base64, String sectionName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (base64 == null || base64.isEmpty()) {
            Logger.warn("отсутствует текстура головы в секции: " + sectionName);
            return head;
        }
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        setSkullTexture(meta, base64);
        head.setItemMeta(meta);
        return head;
    }

    public static @NotNull ItemStack createSkull(String input, String sectionName) {
        if (input == null || input.isEmpty()) {
            Logger.warn("значение материала пусто в секции: " + sectionName);
            return new ItemStack(Material.PLAYER_HEAD);
        }

        String base64 = isBase64Head(input) ? input.substring(9) : input;
        return createSkullBase64(base64, sectionName);
    }

    public static void setSkullTexture(SkullMeta meta, String texture) {
        if (meta == null || texture == null || texture.isEmpty()) return;

        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.getProperties().add(new ProfileProperty("textures", texture));

        meta.setPlayerProfile(profile);
    }

    public static String getSkullTexture(ItemStack head) {
        if (head == null || head.getType() != Material.PLAYER_HEAD) return null;
        ItemMeta meta = head.getItemMeta();
        if (!(meta instanceof SkullMeta)) return null;

        SkullMeta skullMeta = (SkullMeta) meta;
        PlayerProfile profile = skullMeta.getPlayerProfile();

        if (profile != null) {
            return profile.getProperties().stream()
                    .filter(prop -> prop.getName().equals("textures"))
                    .findFirst()
                    .map(ProfileProperty::getValue)
                    .orElse(null);
        }
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