package rip.diamond.practice.misc.commands;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.database.impl.FlatFileHandler;
import rip.diamond.practice.database.impl.MongoHandler;
import rip.diamond.practice.database.impl.MySqlHandler;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.Checker;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.ItemEditorUtil;
import rip.diamond.practice.util.Tasks;
import rip.diamond.practice.util.command.Command;
import rip.diamond.practice.util.command.CommandArgs;
import rip.diamond.practice.util.command.argument.CommandArguments;
import rip.diamond.practice.spigot.spigotapi.SpigotAPI;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EdenCommand extends Command {

    @CommandArgs(name = "eden", inGameOnly = false)
    public void execute(CommandArguments command) {
        CommandSender sender = command.getSender();
        String[] args = command.getArgs();

        if (args.length == 0) {
            Common.sendMessage(sender,
                    CC.CHAT_BAR,
                    CC.AQUA + plugin.getDescription().getName() + CC.GRAY + " - " + CC.DARK_AQUA + "v"
                            + plugin.getDescription().getVersion(),
                    CC.WHITE + "Author: " + CC.AQUA
                            + StringUtils.join(plugin.getDescription().getAuthors(), CC.GRAY + ", " + CC.AQUA),
                    CC.WHITE + "Description: " + CC.AQUA + plugin.getDescription().getDescription(),
                    CC.WHITE + "Website: " + CC.AQUA + CC.UNDER_LINE + plugin.getDescription().getWebsite(),
                    CC.CHAT_BAR);
            return;
        }

        if (sender instanceof Player && !sender.hasPermission("eden.command.eden")) {
            Language.NO_PERMISSION.sendMessage((Player) sender);
            return;
        }

        Action action;
        try {
            action = Action.valueOf(args[0].toUpperCase());
        } catch (Exception e) {
            Common.sendMessage(sender, CC.RED + "Invalid action! Available action: "
                    + Arrays.stream(Action.values()).map(Action::name).collect(Collectors.joining(", ")));
            return;
        }

        switch (action) {
            case RELOAD:
                plugin.reloadPlugin(sender);
                return;
            case DEBUG:
                plugin.getConfigFile().set("debug", !plugin.getConfigFile().getBoolean("debug"));
                plugin.getConfigFile().save();
                Common.sendMessage(sender,
                        CC.GREEN + "Debug is now: "
                                + (plugin.getConfigFile().getBoolean("debug") ? CC.GREEN + Language.ENABLED.toString()
                                        : CC.RED + Language.DISABLED.toString()));
                return;
            case SPIGOT:
                Common.sendMessage(sender, CC.YELLOW + "Eden is currently hooked to " + CC.AQUA
                        + SpigotAPI.INSTANCE.getSpigotType().name());
                return;
            case MIGRATE:
                handleMigration(sender, args);
                return;
            case GOLDENHEAD:
                if (!(sender instanceof Player)) {
                    Common.sendMessage(sender, CC.RED + "This command can only be executed by a player.");
                    return;
                }
                handleGoldenHead((Player) sender, args);
                return;
            case EDITITEM:
                if (!(sender instanceof Player)) {
                    Common.sendMessage(sender, CC.RED + "This command can only be executed by a player.");
                    return;
                }
                handleEditItem((Player) sender, args);
                return;
            case LOCATION:
                if (!(sender instanceof Player)) {
                    Common.sendMessage(sender, CC.RED + "This command can only be executed by a player.");
                    return;
                }
                LocationCommand.handle((Player) sender, args);
                return;
        }
    }

    private void handleGoldenHead(Player player, String[] args) {
        if (!player.hasPermission("eden.command.goldenhead")) {
            Language.NO_PERMISSION.sendMessage(player);
            return;
        }


        String[] newArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];


        new rip.diamond.practice.kits.command.GoldenHeadCommand().executeGoldenHead(player, newArgs);
    }

    private void handleEditItem(Player player, String[] args) {
        if (!player.hasPermission("eden.command.edititem")) {
            Language.NO_PERMISSION.sendMessage(player);
            return;
        }

        if (args.length < 2) {
            sendEditItemUsage(player);
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "removeattributes":
                ItemEditorUtil.removeAttributes(player);
                break;
            case "setunbreakable":
                ItemEditorUtil.setUnbreakable(player);
                break;
            case "enchant":
                if (args.length < 4) {
                    Common.sendMessage(player, CC.RED + "Usage: /eden edititem enchant <ENCHANTMENT> <LEVEL>");
                    return;
                }
                if (!Checker.isInteger(args[3])) {
                    Common.sendMessage(player, CC.RED + "Invalid enchantment level! Must be a number.");
                    return;
                }
                ItemEditorUtil.addEnchantment(player, args[2], Integer.parseInt(args[3]));
                break;
            default:
                sendEditItemUsage(player);
                break;
        }
    }

    private void sendEditItemUsage(Player player) {
        Common.sendMessage(player,
                CC.CHAT_BAR,
                CC.AQUA + "/eden edititem" + CC.GRAY + " - Item Editor Commands",
                CC.YELLOW + "/eden edititem removeattributes " + CC.GRAY + "- Remove all attributes from item",
                CC.YELLOW + "/eden edititem setunbreakable " + CC.GRAY + "- Make item unbreakable",
                CC.YELLOW + "/eden edititem enchant <type> <level> " + CC.GRAY + "- Enchant item",
                CC.CHAT_BAR);
    }

    private void handleMigration(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Common.sendMessage(sender, CC.RED + "Usage: /eden migrate <source> <destination>");
            Common.sendMessage(sender, CC.YELLOW + "Available types: FLATFILE, MONGODB, MYSQL");
            Common.sendMessage(sender, CC.YELLOW + "Example: /eden migrate MONGODB MYSQL");
            return;
        }

        String source = args[1].toUpperCase();
        String destination = args[2].toUpperCase();

        if (!isValidStorageType(source) || !isValidStorageType(destination)) {
            Common.sendMessage(sender, CC.RED + "Invalid storage type! Available types: FLATFILE, MONGODB, MYSQL");
            return;
        }

        if (source.equals(destination)) {
            Common.sendMessage(sender, CC.RED + "Source and destination cannot be the same!");
            return;
        }

        Common.sendMessage(sender, CC.YELLOW + "Starting migration from " + source + " to " + destination + "...");

        Tasks.runAsync(() -> {
            try {

                Object sourceHandler = createHandler(source);
                if (sourceHandler instanceof MongoHandler) {
                    ((MongoHandler) sourceHandler).init();
                } else if (sourceHandler instanceof MySqlHandler) {
                    ((MySqlHandler) sourceHandler).init();
                } else if (sourceHandler instanceof FlatFileHandler) {
                    ((FlatFileHandler) sourceHandler).init();
                }


                List<Document> documents;
                if (sourceHandler instanceof MongoHandler) {
                    documents = ((MongoHandler) sourceHandler).getAllProfiles();
                } else if (sourceHandler instanceof MySqlHandler) {
                    documents = ((MySqlHandler) sourceHandler).getAllProfiles();
                } else {
                    documents = ((FlatFileHandler) sourceHandler).getAllProfiles();
                }


                Object destHandler = createHandler(destination);
                if (destHandler instanceof MongoHandler) {
                    ((MongoHandler) destHandler).init();
                } else if (destHandler instanceof MySqlHandler) {
                    ((MySqlHandler) destHandler).init();
                } else if (destHandler instanceof FlatFileHandler) {
                    ((FlatFileHandler) destHandler).init();
                }


                int count = 0;
                for (Document doc : documents) {
                    if (destHandler instanceof MongoHandler) {
                        ((MongoHandler) destHandler).saveDocumentRaw(doc);
                    } else if (destHandler instanceof MySqlHandler) {
                        ((MySqlHandler) destHandler).saveDocumentRaw(doc);
                    } else {
                        ((FlatFileHandler) destHandler).saveDocumentRaw(doc);
                    }
                    count++;
                }


                if (sourceHandler instanceof MongoHandler) {
                    ((MongoHandler) sourceHandler).shutdown();
                } else if (sourceHandler instanceof MySqlHandler) {
                    ((MySqlHandler) sourceHandler).shutdown();
                }

                if (destHandler instanceof MongoHandler) {
                    ((MongoHandler) destHandler).shutdown();
                } else if (destHandler instanceof MySqlHandler) {
                    ((MySqlHandler) destHandler).shutdown();
                }

                Common.sendMessage(sender, CC.GREEN + "Successfully migrated " + count + " profiles from " + source
                        + " to " + destination + ".");

            } catch (Exception e) {
                e.printStackTrace();
                Common.sendMessage(sender, CC.RED + "An error occurred during migration. Check console for details.");
            }
        });
    }

    private boolean isValidStorageType(String type) {
        return type.equals("FLATFILE") || type.equals("MONGODB") || type.equals("MYSQL");
    }

    private Object createHandler(String type) {
        switch (type) {
            case "MONGODB":
                return new MongoHandler();
            case "MYSQL":
                return new MySqlHandler();
            case "FLATFILE":
            default:
                return new FlatFileHandler();
        }
    }

    @Override
    public List<String> getDefaultTabComplete(CommandArguments command) {
        String[] args = command.getArgs();

        if (args.length == 1) {
            List<String> suggestions = Arrays.stream(Action.values()).map(Action::name).collect(Collectors.toList());
            suggestions.add("item");  // Registered as separate subcommand
            return suggestions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            return Arrays.asList("FLATFILE", "MONGODB", "MYSQL");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("migrate")) {
            return Arrays.asList("FLATFILE", "MONGODB", "MYSQL");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("goldenhead")) {
            return Arrays.asList("1", "16", "32", "64");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("edititem")) {
            return Arrays.asList("removeattributes", "setunbreakable", "enchant");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("edititem") && args[1].equalsIgnoreCase("enchant")) {

            return Arrays.asList(
                    "PROTECTION", "FIRE_PROTECTION", "FEATHER_FALLING", "BLAST_PROTECTION",
                    "PROJECTILE_PROTECTION", "RESPIRATION", "AQUA_AFFINITY", "THORNS",
                    "DEPTH_STRIDER", "SHARPNESS", "SMITE", "BANE_OF_ARTHROPODS",
                    "KNOCKBACK", "FIRE_ASPECT", "LOOTING", "EFFICIENCY", "SILK_TOUCH",
                    "UNBREAKING", "FORTUNE", "POWER", "PUNCH", "FLAME", "INFINITY",
                    "LUCK_OF_THE_SEA", "LURE");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("edititem") && args[1].equalsIgnoreCase("enchant")) {
            return Arrays.asList("1", "2", "3", "4", "5", "10");
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("location")) {
            return LocationCommand.getTabComplete(args);
        }

        return Arrays.stream(Action.values()).map(Action::name).collect(Collectors.toList());
    }

    enum Action {
        RELOAD, DEBUG, SPIGOT, MIGRATE, GOLDENHEAD, EDITITEM, LOCATION
    }
}
