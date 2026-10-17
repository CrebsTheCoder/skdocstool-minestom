package org.skdocs.skdocstool.docs.element;

import com.google.gson.JsonArray;
import org.skdocs.skdocstool.docs.addon.AddonContext;
import org.skdocs.skdocstool.docs.addon.SyntaxOwnership;

public interface DocumentationElement {
    String key();
    JsonArray collect(AddonContext addon, SyntaxOwnership ownership);
}
