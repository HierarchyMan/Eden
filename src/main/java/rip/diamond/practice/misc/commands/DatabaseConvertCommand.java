package rip.diamond.practice.misc.commands;

import org.bson.Document;
import org.bukkit.command.CommandSender;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.database.DatabaseHandler;
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

        if (args.length == 0) {
            Common.sendMessage(sender, CC.RED + "Usage: /databaseconvert <mongo2flatfile|flatfile2mongo>");
            return;
        }

        String type = args[0];
        if (type.equalsIgnoreCase("mongo2flatfile")) {
            Common.sendMessage(sender, CC.YELLOW + "Starting conversion from MongoDB to FlatFile...");
            Tasks.runAsync(() -> {
                try {
                    // Source: Mongo
                    MongoHandler mongoHandler = new MongoHandler();
                    // We don't init mongoHandler here because we assume it might be active or we
                    // just want to read
                    // But strictly speaking, if we are in FlatFile mode, Mongo might not be
                    // initialized.
                    // So we should probably init it temporarily or assume the user has configured
                    // it.
                    // For safety, let's just use the raw connection logic or instantiate it.
                    // However, MongoHandler.init() connects to the database.

                    // If we are currently in FlatFile mode, we need to connect to Mongo to read.
                    // If we are in Mongo mode, we are already connected.

                    // Let's assume we can just create a new instance and init it for the purpose of
                    // this command.
                    // But we need to be careful not to conflict with existing connections if any.
                    // Actually, the safest way is to just create a new handler, init it, read, and
                    // close it.

                    mongoHandler.init();
                    List<Document> mongoDocs = mongoHandler.getAllProfiles();

                    // Target: FlatFile
                    // If we are in Mongo mode, FlatFile might not be active.
                    FlatFileHandler flatFileHandler = new FlatFileHandler();
                    flatFileHandler.init(); // Ensures folder exists

                    int count = 0;
                    for (Document doc : mongoDocs) {
                        flatFileHandler.saveDocumentRaw(doc);
                        count++;
                    }

                    // We don't necessarily need to shutdown flatfile handler as it just writes
                    // files.
                    // But we should shutdown mongo if we opened it just for this.
                    if (!Config.STORAGE_TYPE.toString().equalsIgnoreCase("MONGODB")) {
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
                    // Source: FlatFile
                    FlatFileHandler flatFileHandler = new FlatFileHandler();
                    flatFileHandler.init();
                    List<Document> flatFileDocs = flatFileHandler.getAllProfiles();

                    // Target: Mongo
                    MongoHandler mongoHandler = new MongoHandler();
                    // Only init if not already the active handler (though init is usually
                    // idempotent-ish or we should check)
                    // If we are in FlatFile mode, we MUST init Mongo.
                    if (!Config.STORAGE_TYPE.toString().equalsIgnoreCase("MONGODB")) {
                        mongoHandler.init();
                    } else {
                        // If we are in Mongo mode, we can potentially use the existing one,
                        // but creating a new one is safer to avoid state issues,
                        // provided the driver handles connection pooling correctly (which it does).
                        // However, to be safe and simple:
                        mongoHandler.init();
                    }

                    int count = 0;
                    for (Document doc : flatFileDocs) {
                        mongoHandler.saveDocumentRaw(doc);
                        count++;
                    }

                    if (!Config.STORAGE_TYPE.toString().equalsIgnoreCase("MONGODB")) {
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
