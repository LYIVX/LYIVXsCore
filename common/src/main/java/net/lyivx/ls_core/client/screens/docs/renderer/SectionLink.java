package net.lyivx.ls_core.client.screens.docs.renderer;

public class SectionLink {
    public final String linkType;
    public final String linkTarget;
    public final String label;
    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public SectionLink(String linkType, String linkTarget, String label, int x, int y, int width, int height) {
        this.linkType = linkType;
        this.linkTarget = linkTarget;
        this.label = label;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}


