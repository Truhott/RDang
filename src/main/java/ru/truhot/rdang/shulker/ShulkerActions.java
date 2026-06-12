package ru.truhot.rdang.shulker;

import org.bukkit.Location;

import java.util.Collection;

public interface ShulkerActions {
    void addShulker(Location location);

    int addShulkers(Collection<Location> locations);
}
