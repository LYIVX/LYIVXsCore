package net.lyivx.ls_core.docs.model;

import net.lyivx.ls_core.docs.DocIconType;

import java.util.List;

/**
 * Entry json, stored in data/<namespace>/ls_core/entries/<id>.json
 */
public class EntryJson {
    public String id; // file name without extension, filled by loader
    public String category; // category id this entry belongs to
    public String title;
    public String tooltip; // optional
    public DocIconType iconType = DocIconType.ITEM;
    public String icon; // item id, block id, or texture path

    // Basic content blocks
    public List<Page> pages;

    public static class Page {
        // New: pages can contain up to two sections stacked vertically
        public List<Section> sections;

        // Back-compat simple page fields (will be interpreted as a single section at render time)
        public String type; // text, image, recipe, item_showcase, link
        public String text;
        public String image;
        public String recipeId;
        public String recipeType; // crafting, furnace, smithing, brewing, etc.
        public List<String> items; // for item_showcase
        public String linkType; // entry or category
        public String linkTarget; // id of entry/category
        public String linkLabel; // optional label
    }

    public static class Section {
        public String type; // text, image, recipe, showcase, link
        public String title; // optional title to display above section
        public String text; // for text
        public String image; // texture path
        public String recipeId;
        public String recipeType; // crafting, furnace, smithing, brewing
        public List<String> items; // showcase items/blocks
        public String linkType; // entry/category
        public String linkTarget; // id
        public String linkLabel; // optional label
    }
}


