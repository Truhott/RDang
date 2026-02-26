package ru.truhot.rdang;

import org.bukkit.plugin.java.JavaPlugin;
import ru.truhot.rdang.addshulkers.AddShulkers;
import ru.truhot.rdang.comands.Command;
import ru.truhot.rdang.comands.RTabCompleter;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.dung.DungActions;
import ru.truhot.rdang.menu.MenuManager;
import ru.truhot.rdang.schem.SchemAction;
import ru.truhot.rdang.shulker.BDangShulker;
import ru.truhot.rdang.shulker.ShulkerActions;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.Metrics;
import ru.truhot.rdang.util.UndoUtil;
import ru.truhot.rdang.util.UpdateUtil;
import ru.truhot.rdang.сore.MainCore;
import ru.truhot.rdang.сore.CoreFactory;

import java.io.File;

public final class RDang extends JavaPlugin {

    @Override
    public void onEnable() {
        new File(getDataFolder(), "schem").mkdirs();
        new File(getDataFolder(), "data").mkdirs();
        new File(getDataFolder(), "backups").mkdirs();
        ConfigManager configManager = new ConfigManager(this);
        Storage shulkers = new Storage("shulkers.yml", this);
        Storage items = new Storage("items.yml", this);
        Storage blockStorage = new Storage("block.yml", this);
        UndoUtil undoUtil = new UndoUtil(configManager, shulkers, blockStorage, this);
        MainCore mainCore = CoreFactory.createDang(items, shulkers, configManager, undoUtil);
        ShulkerActions shulkerActions = new BDangShulker(mainCore);
        AddShulkers addShulkers = new AddShulkers(shulkerActions);
        SchemAction schemAction = new SchemAction(this, configManager);
        DungActions dungActions = new DungActions(schemAction, addShulkers, configManager, undoUtil);
        MenuManager menuManager = new MenuManager(configManager, shulkers, blockStorage, this);
        Command command = new Command(mainCore, dungActions, this, items, shulkers, blockStorage, configManager, menuManager, undoUtil);
        getServer().getPluginManager().registerEvents(menuManager, this);
        getCommand("rdang").setExecutor(command);
        getCommand("rdang").setTabCompleter(new RTabCompleter(this));
        getServer().getPluginManager().registerEvents(mainCore, this);
        getServer().getPluginManager().registerEvents(mainCore.getEventHandler(), this);
        System.out.println("[Rdang] успешно запущен!");
        UpdateUtil updateUtil = new UpdateUtil(this);
        if (getConfig().getBoolean("settings.update-check")) {
            updateUtil.check();
        }
        if (getConfig().getBoolean("settings.metrics")) {
            int pluginId = 28720;
            new Metrics(this, pluginId);
            System.out.println("[Rdang] bStats успешно инициализирован!");
        }
    }

    @Override
    public void onDisable() {
        System.out.println("[Rdang] отключен!");
    }
}