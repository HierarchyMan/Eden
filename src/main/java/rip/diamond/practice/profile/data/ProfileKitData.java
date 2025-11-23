package rip.diamond.practice.profile.data;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagString;
import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.kits.KitLoadout;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;

import java.util.ArrayList;
import java.util.List;

public class ProfileKitData {

	@Getter
	private int elo = Config.PROFILE_DEFAULT_ELO.toInteger();
	@Getter
	@Setter
	private int peakElo = Config.PROFILE_DEFAULT_ELO.toInteger();
	@Getter
	@Setter
	private int unrankedWon = 0;
	@Getter
	@Setter
	private int unrankedLost = 0;
	@Getter
	@Setter
	private int rankedWon = 0;
	@Getter
	@Setter
	private int rankedLost = 0;
	@Getter
	@Setter
	private int bestWinstreak = 0;
	@Getter
	@Setter
	private int winstreak = 0;

	// Time-based wins tracking
	@Getter
	@Setter
	private int dailyWins = 0;
	@Getter
	@Setter
	private int weeklyWins = 0;
	@Getter
	@Setter
	private int monthlyWins = 0;

	// Time-based losses tracking
	@Getter
	@Setter
	private int dailyLosses = 0;
	@Getter
	@Setter
	private int weeklyLosses = 0;
	@Getter
	@Setter
	private int monthlyLosses = 0;

	// Time-based winstreak tracking
	@Getter
	@Setter
	private int dailyWinstreak = 0;
	@Getter
	@Setter
	private int weeklyWinstreak = 0;
	@Getter
	@Setter
	private int monthlyWinstreak = 0;

	// Reset timestamps
	@Getter
	@Setter
	private long lastDailyReset = System.currentTimeMillis();
	@Getter
	@Setter
	private long lastWeeklyReset = System.currentTimeMillis();
	@Getter
	@Setter
	private long lastMonthlyReset = System.currentTimeMillis();

	@Setter
	private KitLoadout[] loadouts = new KitLoadout[8];

	public KitLoadout[] getLoadouts() {
		if (loadouts.length < 8) {
			KitLoadout[] newLoadouts = new KitLoadout[8];
			System.arraycopy(loadouts, 0, newLoadouts, 0, loadouts.length);
			loadouts = newLoadouts;
		}
		return loadouts;
	}

	public void fromBson(Document document) {
		elo = document.getInteger("elo");
		peakElo = document.getInteger("peakElo");
		unrankedWon = document.getInteger("unrankedWon");
		unrankedLost = document.getInteger("unrankedLost");
		rankedWon = document.getInteger("rankedWon");
		rankedLost = document.getInteger("rankedLost");
		bestWinstreak = document.getInteger("bestWinstreak");
		winstreak = document.getInteger("winstreak");

		// Time-based wins
		dailyWins = document.containsKey("dailyWins") ? document.getInteger("dailyWins") : 0;
		weeklyWins = document.containsKey("weeklyWins") ? document.getInteger("weeklyWins") : 0;
		monthlyWins = document.containsKey("monthlyWins") ? document.getInteger("monthlyWins") : 0;

		// Time-based losses
		dailyLosses = document.containsKey("dailyLosses") ? document.getInteger("dailyLosses") : 0;
		weeklyLosses = document.containsKey("weeklyLosses") ? document.getInteger("weeklyLosses") : 0;
		monthlyLosses = document.containsKey("monthlyLosses") ? document.getInteger("monthlyLosses") : 0;

		// Time-based winstreaks
		dailyWinstreak = document.containsKey("dailyWinstreak") ? document.getInteger("dailyWinstreak") : 0;
		weeklyWinstreak = document.containsKey("weeklyWinstreak") ? document.getInteger("weeklyWinstreak") : 0;
		monthlyWinstreak = document.containsKey("monthlyWinstreak") ? document.getInteger("monthlyWinstreak") : 0;

		// Reset timestamps
		lastDailyReset = document.containsKey("lastDailyReset") ? document.getLong("lastDailyReset")
				: System.currentTimeMillis();
		lastWeeklyReset = document.containsKey("lastWeeklyReset") ? document.getLong("lastWeeklyReset")
				: System.currentTimeMillis();
		lastMonthlyReset = document.containsKey("lastMonthlyReset") ? document.getLong("lastMonthlyReset")
				: System.currentTimeMillis();

		KitLoadout[] loadedLoadouts = Eden.GSON.fromJson(document.getString("loadouts"), KitLoadout[].class);
		if (loadedLoadouts != null) {
			if (loadedLoadouts.length < 8) {
				System.arraycopy(loadedLoadouts, 0, loadouts, 0, loadedLoadouts.length);
			} else {
				loadouts = loadedLoadouts;
			}
		}
	}

	public Document toBson() {
		return new Document()
				.append("elo", elo)
				.append("peakElo", peakElo)
				.append("unrankedWon", unrankedWon)
				.append("unrankedLost", unrankedLost)
				.append("rankedWon", rankedWon)
				.append("rankedLost", rankedLost)
				.append("won", getWon()) // Used for leaderboard display
				.append("bestWinstreak", bestWinstreak)
				.append("winstreak", winstreak)
				// Time-based wins
				.append("dailyWins", dailyWins)
				.append("weeklyWins", weeklyWins)
				.append("monthlyWins", monthlyWins)
				// Time-based losses
				.append("dailyLosses", dailyLosses)
				.append("weeklyLosses", weeklyLosses)
				.append("monthlyLosses", monthlyLosses)
				// Time-based winstreaks
				.append("dailyWinstreak", dailyWinstreak)
				.append("weeklyWinstreak", weeklyWinstreak)
				.append("monthlyWinstreak", monthlyWinstreak)
				// Reset timestamps
				.append("lastDailyReset", lastDailyReset)
				.append("lastWeeklyReset", lastWeeklyReset)
				.append("lastMonthlyReset", lastMonthlyReset)
				.append("loadouts", Eden.GSON.toJson(loadouts));
	}

	public int getWon() {
		return unrankedWon + rankedWon;
	}

	public void incrementWon(boolean ranked) {
		if (ranked) {
			this.rankedWon++;
		} else {
			this.unrankedWon++;
		}

		// Check and reset time-based wins if needed
		checkAndResetTimePeriods();

		// Increment time-based wins
		dailyWins++;
		weeklyWins++;
		monthlyWins++;
	}

	public void incrementLost(boolean ranked) {
		if (ranked) {
			this.rankedLost++;
		} else {
			this.unrankedLost++;
		}

		// Check and reset time-based losses if needed
		checkAndResetTimePeriods();

		// Increment time-based losses
		dailyLosses++;
		weeklyLosses++;
		monthlyLosses++;
	}

	public void setElo(int elo) {
		this.elo = elo;
		if (peakElo < elo) {
			peakElo = elo;
		}
	}

	public void calculateWinstreak(boolean won) {
		if (won) {
			winstreak++;
			if (bestWinstreak < winstreak)
				bestWinstreak = winstreak;

			// Check and reset time-based winstreaks if needed
			checkAndResetTimePeriods();

			// Increment time-based winstreaks
			dailyWinstreak++;
			weeklyWinstreak++;
			monthlyWinstreak++;
		} else {
			winstreak = 0;
			dailyWinstreak = 0;
			weeklyWinstreak = 0;
			monthlyWinstreak = 0;
		}
	}

	/**
	 * Check and reset all time periods if they have expired
	 */
	private void checkAndResetTimePeriods() {
		long now = System.currentTimeMillis();

		// Check daily reset
		if (!isSameDay(lastDailyReset, now)) {
			dailyWins = 0;
			dailyLosses = 0;
			dailyWinstreak = 0;
			lastDailyReset = now;
		}

		// Check weekly reset
		if (!isSameWeek(lastWeeklyReset, now)) {
			weeklyWins = 0;
			weeklyLosses = 0;
			weeklyWinstreak = 0;
			lastWeeklyReset = now;
		}

		// Check monthly reset
		if (!isSameMonth(lastMonthlyReset, now)) {
			monthlyWins = 0;
			monthlyLosses = 0;
			monthlyWinstreak = 0;
			lastMonthlyReset = now;
		}
	}

	private boolean isSameDay(long time1, long time2) {
		java.util.Calendar cal1 = java.util.Calendar.getInstance();
		cal1.setTimeInMillis(time1);
		java.util.Calendar cal2 = java.util.Calendar.getInstance();
		cal2.setTimeInMillis(time2);
		return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
				cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
	}

	private boolean isSameWeek(long time1, long time2) {
		java.util.Calendar cal1 = java.util.Calendar.getInstance();
		cal1.setTimeInMillis(time1);
		java.util.Calendar cal2 = java.util.Calendar.getInstance();
		cal2.setTimeInMillis(time2);
		return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
				cal1.get(java.util.Calendar.WEEK_OF_YEAR) == cal2.get(java.util.Calendar.WEEK_OF_YEAR);
	}

	private boolean isSameMonth(long time1, long time2) {
		java.util.Calendar cal1 = java.util.Calendar.getInstance();
		cal1.setTimeInMillis(time1);
		java.util.Calendar cal2 = java.util.Calendar.getInstance();
		cal2.setTimeInMillis(time2);
		return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
				cal1.get(java.util.Calendar.MONTH) == cal2.get(java.util.Calendar.MONTH);
	}

	public KitLoadout getLoadout(int index) {
		return loadouts[index];
	}

	public void replaceKit(int index, KitLoadout loadout) {
		loadouts[index] = loadout;
	}

	public void deleteKit(int index) {
		loadouts[index] = null;
	}

	public List<ItemStack> getKitItems(Kit kit) {
		List<ItemStack> toReturn = new ArrayList<>();

		for (KitLoadout loadout : loadouts) {
			if (loadout != null) {
				ItemStack itemStack = new ItemBuilder(Material.ENCHANTED_BOOK)
						.name(CC.AQUA + loadout.getCustomName())
						.lore(Language.PROFILE_KIT_RIGHT_CLICK_TO_RECEIVE.toStringList())
						.build();

				net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
				NBTTagCompound compound = (nmsItem.hasTag()) ? nmsItem.getTag() : new NBTTagCompound();
				compound.set("armor", new NBTTagString(loadout.getArmorAsBase64()));
				compound.set("contents", new NBTTagString(loadout.getContentsAsBase64()));
				compound.set("name", new NBTTagString(loadout.getCustomName()));
				toReturn.add(CraftItemStack.asBukkitCopy(nmsItem));
			}
		}

		ItemStack itemStack = new ItemBuilder(Material.BOOK)
				.name(CC.AQUA + kit.getKitLoadout().getCustomName())
				.lore(Language.PROFILE_KIT_RIGHT_CLICK_TO_RECEIVE.toStringList())
				.build();
		net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
		NBTTagCompound compound = (nmsItem.hasTag()) ? nmsItem.getTag() : new NBTTagCompound();
		compound.set("armor", new NBTTagString(kit.getKitLoadout().getArmorAsBase64()));
		compound.set("contents", new NBTTagString(kit.getKitLoadout().getContentsAsBase64()));
		compound.set("name", new NBTTagString(kit.getKitLoadout().getCustomName()));

		toReturn.add(CraftItemStack.asBukkitCopy(nmsItem));

		return toReturn;
	}

}
