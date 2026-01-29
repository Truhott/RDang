package ru.truhot.rdang.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class MenuHolder implements InventoryHolder {

    private final String menuId;
    private final int page;
    private Inventory inventory;

    public MenuHolder(String menuId, int page) {
        this.menuId = menuId;
        this.page = page;
    }

    public String getMenuId() {
        return menuId;
    }

    public int getPage() {
        return page;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
