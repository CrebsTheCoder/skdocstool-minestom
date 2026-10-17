package org.skdocs.skdocstool;

import org.bukkit.plugin.java.JavaPlugin;
import org.skdocs.skdocstool.command.DocsPreviewCommand;
import org.skdocs.skdocstool.docs.GenerateDocs;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import com.github.hapily04.skriptminestom.luckperms.LuckPermsLookup;

public final class Skdocstool extends JavaPlugin {
    private Command generate, docs;

    @Override public void onEnable() {
        generate = new Command("gendocs");
        generate.setCondition((sender, input) -> LuckPermsLookup.hasPermission(sender, "skdocstool.gendocs"));
        generate.setDefaultExecutor((sender, context) -> GenerateDocs.generate(sender, this));
        docs = new Command("docs");
        docs.setCondition((sender, input) -> LuckPermsLookup.hasPermission(sender, "skdocstool.docspreview"));
        docs.setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /docs preview <addon>"));
        var preview = new DocsPreviewCommand(this);
        var previewCommand = new Command("preview");
        previewCommand.setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /docs preview <addon>"));
        var addon = new ArgumentString("addon");
        addon.setSuggestionCallback((sender, context, suggestion) ->
            preview.getAvailableAddons("").forEach(name -> suggestion.addEntry(new SuggestionEntry(name))));
        previewCommand.addSyntax((sender, context) -> preview.generatePreview(sender, context.get(addon)), addon);
        docs.addSubcommand(previewCommand);
        MinecraftServer.getCommandManager().register(generate);
        MinecraftServer.getCommandManager().register(docs);
    }
    @Override public void onDisable() {
        if (generate != null) MinecraftServer.getCommandManager().unregister(generate);
        if (docs != null) MinecraftServer.getCommandManager().unregister(docs);
    }
}
