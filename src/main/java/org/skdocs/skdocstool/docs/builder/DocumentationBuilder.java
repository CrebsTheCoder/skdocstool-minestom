package org.skdocs.skdocstool.docs.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.skdocs.skdocstool.docs.addon.AddonContext;
import org.skdocs.skdocstool.docs.addon.SyntaxOwnership;
import org.skdocs.skdocstool.docs.element.DocumentationElement;
import org.skdocs.skdocstool.docs.element.EventDocumentation;
import org.skdocs.skdocstool.docs.element.FunctionDocumentation;
import org.skdocs.skdocstool.docs.element.SyntaxDocumentation;
import org.skdocs.skdocstool.docs.element.TypeDocumentation;
import org.skriptlang.skript.registration.SyntaxRegistry;

public final class DocumentationBuilder {
    private final SyntaxOwnership ownership;
    private final List<DocumentationElement> elements = List.of(
        new SyntaxDocumentation<>("conditions", SyntaxRegistry.CONDITION),
        new SyntaxDocumentation<>("effects", SyntaxRegistry.EFFECT),
        new SyntaxDocumentation<>("expressions", SyntaxRegistry.EXPRESSION),
        new EventDocumentation(),
        new SyntaxDocumentation<>("sections", SyntaxRegistry.SECTION),
        new SyntaxDocumentation<>("structures", SyntaxRegistry.STRUCTURE),
        new TypeDocumentation(),
        new FunctionDocumentation()
    );

    public DocumentationBuilder(SyntaxOwnership ownership) {
        this.ownership = ownership;
    }

    public JsonObject build(AddonContext addon) {
        JsonObject root = new JsonObject();
        root.addProperty("addon", addon.addon().name());
        List<JsonArray> arrays = new ArrayList<>();
        for (DocumentationElement element : elements) {
            JsonArray entries = DocumentationJson.mergeEntries(element.collect(addon, ownership));
            arrays.add(entries);
            root.add(element.key(), entries);
        }
        DocumentationJson.assignDocIds(addon.addon().name(), arrays.toArray(JsonArray[]::new));
        return root;
    }
}
