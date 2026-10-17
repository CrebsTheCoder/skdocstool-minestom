package org.skdocs.skdocstool.docs.builder;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.DocumentationId;
import ch.njol.skript.lang.function.Function;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public final class DocumentationIds {
    private DocumentationIds() {}
    public static String getId(SyntaxInfo<?> info) {
        if (info instanceof BukkitSyntaxInfos.Event<?> event) {
            String explicit = event.documentationId();
            return explicit != null && !explicit.isBlank() ? explicit : event.id();
        }
        return forClass(info.type());
    }
    public static String getId(ClassInfo<?> info) { return info.getCodeName(); }
    public static String getId(Function<?> function) { return function.getName(); }
    private static String forClass(Class<?> type) {
        DocumentationId annotation = type.getAnnotation(DocumentationId.class);
        return annotation == null ? type.getSimpleName() : annotation.value();
    }
}
