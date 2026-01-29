package ru.truhot.rdang.сore;

import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.UndoUtil;
import ru.truhot.rdang.сore.managers.EventManager;
import ru.truhot.rdang.сore.managers.ItemChecker;
import ru.truhot.rdang.сore.managers.LootManager;
import ru.truhot.rdang.сore.managers.ShulkerManager;

public class CoreFactory {

    public static MainCore createDang(Storage items, Storage shulkers, ConfigManager configManager, UndoUtil undoUtil) {
        LootManager lootManager = new LootManager(items);
        ShulkerManager shulkerManager = new ShulkerManager(shulkers, lootManager);
        ItemChecker itemChecker = new ItemChecker(configManager);
        EventManager eventManager = new EventManager(shulkers, configManager, shulkerManager, itemChecker, undoUtil);
        return new MainCore(items, shulkers, configManager, lootManager, shulkerManager, itemChecker, eventManager);
    }
}