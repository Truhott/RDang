package ru.truhot.rdang.util.logger.impl;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import ru.truhot.rdang.util.logger.ILogger;
import ru.truhot.rdang.util.MessageUtil;

public class LegacyLogger implements ILogger {

    @Override
    public void info(String s) {
        send(s);
    }

    @Override
    public void warn(String s) {
        send(s);
    }

    @Override
    public void error(String s) {
        send(s);
    }

    private void send(String s) {
        Component component = MessageUtil.parseText(s);
        Bukkit.getConsoleSender().sendMessage(component);
    }
}