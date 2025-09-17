package net.lyivx.ls_core.docs;

/**
 * Global access to the DocsManager instance.
 */
public final class DocsService {
    private static final DocsManager MANAGER = new DocsManager();

    private DocsService() {}

    public static DocsManager get() {
        return MANAGER;
    }
}


