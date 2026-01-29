package ru.truhot.rdang.shulker;

import lombok.AllArgsConstructor;
import org.bukkit.Location;
import ru.truhot.rdang.сore.MainCore;

@AllArgsConstructor
public class BDangShulker implements ShulkerActions {

    private final MainCore bDang;

    @Override
    public void addShulker(Location location) {
        System.out.println("Adding shulker at " + location);
        bDang.addShulker(location);
    }
}
