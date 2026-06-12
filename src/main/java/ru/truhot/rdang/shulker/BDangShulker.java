package ru.truhot.rdang.shulker;

import lombok.AllArgsConstructor;
import org.bukkit.Location;
import ru.truhot.rdang.util.logger.Logger;
import ru.truhot.rdang.сore.MainCore;

import java.util.Collection;

@AllArgsConstructor
public class BDangShulker implements ShulkerActions {

    private final MainCore bDang;

    @Override
    public void addShulker(Location location) {
        Logger.info("Добавление шалкера в " + location);
        bDang.addShulker(location);
    }

    @Override
    public int addShulkers(Collection<Location> locations) {
        return bDang.addShulkers(locations);
    }
}
