package rip.diamond.practice.managers.chest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

@Getter
@Setter
@AllArgsConstructor
public class Chest {

    private ItemStack[] items;
    private int number;
}
