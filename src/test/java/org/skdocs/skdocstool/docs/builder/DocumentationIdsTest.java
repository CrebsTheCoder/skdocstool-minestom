package org.skdocs.skdocstool.docs.builder;

import ch.njol.skript.lang.util.SimpleEvent;
import org.junit.jupiter.api.Test;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentationIdsTest {
    @Test void sharedEventImplementationUsesRegistrationIdentity() {
        SyntaxInfo<?> join = event("player_join", null);
        SyntaxInfo<?> chat = event("player_chat", null);
        assertEquals("player_join", DocumentationIds.getId(join));
        assertEquals("player_chat", DocumentationIds.getId(chat));
        assertNotEquals(DocumentationIds.getId(join), DocumentationIds.getId(chat));
    }
    @Test void explicitEventDocumentationIdTakesPrecedence() {
        assertEquals("custom-join", DocumentationIds.getId(event("player_join", "custom-join")));
    }
    @Test void blankExplicitIdFallsBackToRegistrationId() {
        assertEquals("player_join", DocumentationIds.getId(event("player_join", " ")));
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BukkitSyntaxInfos.Event<?> event(String id, String explicit) {
        BukkitSyntaxInfos.Event event = mock(BukkitSyntaxInfos.Event.class);
        when(event.type()).thenReturn(SimpleEvent.class);
        when(event.id()).thenReturn(id);
        when(event.documentationId()).thenReturn(explicit);
        return event;
    }
}
