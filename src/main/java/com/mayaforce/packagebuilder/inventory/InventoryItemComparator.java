package com.mayaforce.packagebuilder.inventory;

import java.util.Comparator;

public class InventoryItemComparator implements Comparator<InventoryItem> {

    @Override
    public int compare(InventoryItem o1, InventoryItem o2) {
        // TODO Auto-generated method stub
        return o1.getItemName().compareTo(o2.getItemName());
    }

}
