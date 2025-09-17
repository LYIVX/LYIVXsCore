package net.lyivx.ls_core.docs.model;

import net.lyivx.ls_core.docs.DocIconType;

import java.util.List;

/**
 * Root document metadata loaded from data/<namespace>/ls_core/doc.json
 */
public class DocJson {
    public String title;
    public String description;
    public DocIconType iconType = DocIconType.ITEM;
    public String icon; // item id, block id, or texture path
    public String tooltip;
    public boolean searchbar = true;
    public boolean useI18n = false;

    // Optional explicit ordering for categories by id; if empty categories are sorted by title
    public List<String> categoryOrder;

    // Convenience runtime field, set by loader
    public transient String sourceNamespace;
}


