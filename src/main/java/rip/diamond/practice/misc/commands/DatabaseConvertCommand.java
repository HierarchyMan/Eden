package rip.diamond.practice.misc.commands;

import org.bson.Document;
import org.bukkit.command.CommandSender;
import rip.diamond.practice.config.DatabaseConfig;
import rip.diamond.practice.database.impl.FlatFileHandler;
import rip.diamond.practice.database.impl.MongoHandler;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.Tasks;
import rip.diamond.practice.util.command.Command;
import rip.diamond.practice.util.command.CommandArgs;
import rip.diamond.practice.util.command.argument.CommandArguments;

import java.util.List;

public class DatabaseConvertCommand extends Command {

    @CommandArgs(name = "databaseconvert", permission = "eden.command.databaseconvert", inGameOnly = false)
    public void execute(CommandArguments command) {
        CommandSender sender = command.getSender();
        String[] args = command.getArgs();

        Common.sendMessage(sender, CC.YELLOW + "This command is deprecated!");
        Common.sendMessage(sender, CC.YELLOW + "Please use: " + CC.AQUA + "/eden migrate <source> <destination>");
        Common.sendMessage(sender, CC.YELLOW + "Example: " + CC.AQUA + "/eden migrate MONGODB MYSQL");

        if (args.length == 0) {
            Common.sendMessage(sender, CC.RED + "Usage: /databaseconvert <mongo2flatfile|flatfile2mongo>");
            return;
        }

        String type = args[0];
        if (type.equalsIgnoreCase("mongo2flatfile")) {
            Common.sendMessage(sender, CC.YELLOW + "Starting conversion from MongoDB to FlatFile...");
            Tasks.runAsync(() -> {
                try {
                    MongoHandler mongoHandler = new MongoHandler();
                    mongoHandler.init();
                    List<Document> mongoDocs = mongoHandler.getAllProfiles();

                    FlatFileHandler flatFileHandler = new FlatFileHandler();
                    flatFileHandler.init();

                    int count = 0;
                    for (Document doc : mongoDocs) {
                        flatFileHandler.saveDocumentRaw(doc);
                        count++;
                    }

                    if (!DatabaseConfig.STORAGE_TYPE.toString().equalsIgnoreCase("MONGODB")) {
                        mongoHandler.shutdown();
                    }

                    Common.sendMessage(sender,
                            CC.GREEN + "Successfully converted " + count + " profiles from MongoDB to FlatFile.");

                } catch (Exception e) {
                    e.printStackTrace();
                    Common.sendMessage(sender,
                            CC.RED + "An error occurred during conversion. Check console for details.");
                }
            });

        } else if (type.equalsIgnoreCase("flatfile2mongo")) {
            Common.sendMessage(sender, CC.YELLOW + "Starting conversion from FlatFile to MongoDB...");
            Tasks.runAsync(() -> {
                try {
                    FlatFileHandler flatFileHandler = new FlatFileHandler();
                    flatFileHandler.init();
                    List<Document> flatFileDocs = flatFileHandler.getAllProfiles();

                    MongoHandler mongoHandler = new MongoHandler();
                    if (!DatabaseConfig.STORAGE_TYPE.toString().equalsIgnoreCase("MONGODB")) {
                        mongoHandler.init();
                    } else {
                        mongoHandler.init();
                    }

                    int count = 0;
                    for (Document doc : flatFileDocs) {
                        mongoHandler.saveDocumentRaw(doc);
                        count++;
                    }

                    if (!DatabaseConfig.STORAGE_TYPE.toString().equalsIgnoreCase("MONGODB")) {
                        mongoHandler.shutdown();
                    }

                    Common.sendMessage(sender,
                            CC.GREEN + "Successfully converted " + count + " profiles from FlatFile to MongoDB.");

                } catch (Exception e) {
                    e.printStackTrace();
                    Common.sendMessage(sender,
                            CC.RED + "An error occurred during conversion. Check console for details.");
                }
            });
        } else {
            Common.sendMessage(sender, CC.RED + "Usage: /databaseconvert <mongo2flatfile|flatfile2mongo>");
        }
    }
}
