package rip.diamond.practice.kiteditor.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KitEditorSelectKitMenu extends Menu {

	@Override
	public String getTitle(Player player) {
		BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
		return CC.translate(config.getString("kit-editor-select-kit-menu.title"));
	}

	@Override
	public int getSize() {
		BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
		String sizeStr = config.getString("kit-editor-select-kit-menu.size");

		if ("dynamic".equalsIgnoreCase(sizeStr)) {
			// Calculate dynamic size based on number of editable kits
			List<Kit> editableKits = getEditableKits();
			boolean hasBorder = config.getBoolean("kit-editor-select-kit-menu.border.enabled");

			// Calculate needed rows
			int kitsCount = editableKits.size();
			int slotsPerRow = hasBorder ? 7 : 9; // Account for border
			int rowsNeeded = (int) Math.ceil((double) kitsCount / slotsPerRow);
			int totalRows = rowsNeeded + (hasBorder ? 2 : 0); // Add top and bottom border

			// Ensure minimum of 3 rows
			int maxSize = config.getInt("kit-editor-select-kit-menu.max-size");
			int calculatedSize = Math.max(27, Math.min(totalRows * 9, maxSize));

			// Round to valid inventory size
			return ((calculatedSize + 8) / 9) * 9;
		} else {
			return config.getInt("kit-editor-select-kit-menu.size");
		}
	}

	@Override
	public Map<Integer, Button> getButtons(Player player) {
		Map<Integer, Button> buttons = new HashMap<>();
		BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();

		// Filler
		if (config.getBoolean("kit-editor-select-kit-menu.filler.enabled")) {
			ItemStack filler = new ItemBuilder(
					Material.valueOf(config.getString("kit-editor-select-kit-menu.filler.material")))
					.durability(config.getInt("kit-editor-select-kit-menu.filler.data"))
					.name(" ")
					.build();
			for (int i = 0; i < getSize(); i++) {
				buttons.put(i, new Button() {
					@Override
					public ItemStack getButtonItem(Player player) {
						return filler;
					}
				});
			}
		}

		// Border
		if (config.getBoolean("kit-editor-select-kit-menu.border.enabled")) {
			ItemStack border = new ItemBuilder(
					Material.valueOf(config.getString("kit-editor-select-kit-menu.border.material")))
					.durability(config.getInt("kit-editor-select-kit-menu.border.data"))
					.name(" ")
					.build();
			int size = getSize();
			for (int i = 0; i < size; i++) {
				if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
					buttons.put(i, new Button() {
						@Override
						public ItemStack getButtonItem(Player player) {
							return border;
						}
					});
				}
			}
		}

		// Kit buttons
		List<Kit> editableKits = getEditableKits();
		int kitIndex = 0;

		for (int slot = 0; slot < getSize() && kitIndex < editableKits.size(); slot++) {
			// Skip border slots
			if (buttons.containsKey(slot) && (slot < 9 || slot >= getSize() - 9 || slot % 9 == 0 || slot % 9 == 8)) {
				continue;
			}

			Kit kit = editableKits.get(kitIndex);
			buttons.put(slot, new KitButton(kit, config));
			kitIndex++;
		}

		return buttons;
	}

	private List<Kit> getEditableKits() {
		return Kit.getKits().stream()
				.filter(Kit::isEnabled)
				.filter(kit -> kit.getGameRules().isReceiveKitLoadoutBook())
				.collect(Collectors.toList());
	}

	private static class KitButton extends Button {
		private final Kit kit;
		private final BasicConfigFile config;

		public KitButton(Kit kit, BasicConfigFile config) {
			this.kit = kit;
			this.config = config;
		}

		@Override
		public ItemStack getButtonItem(Player player) {
			String name = config.getString("kit-editor-select-kit-menu.items.kit-button.name")
					.replace("{kit-name}", kit.getDisplayName());
			List<String> lore = config.getStringList("kit-editor-select-kit-menu.items.kit-button.lore");

			return new ItemBuilder(kit.getDisplayIcon().clone())
					.name(name)
					.lore(lore)
					.build();
		}

		@Override
		public void clicked(Player player, ClickType clickType) {
			player.closeInventory();
			Eden.INSTANCE.getKitEditorManager().addKitEditor(player, kit);
		}
	}
}
