package net.lyivx.ls_core.docs.model;

import net.lyivx.ls_core.docs.DocIconType;

/**
 * Category json, stored in data/<namespace>/ls_core/categories/<id>.json
 */
public class CategoryJson {
    public String id; // file name without extension, filled by loader
    public String title;
    public String tooltip; // optional
    public DocIconType iconType = DocIconType.ITEM;
    public String icon; // item id, block id, or texture path
    public Boolean searchbar; // optional; overrides doc-level searchbar when present
}


