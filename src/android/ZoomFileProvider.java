package cordova.plugin.zoomvideo;

import androidx.core.content.FileProvider;

/**
 * This is a custom FileProvider to prevent conflicts with other plugins.
 * The class is intentionally empty because it inherits all the functionality
 * from Android's FileProvider. Its only purpose is to provide a unique name
 * for the provider in the AndroidManifest.xml.
 */
public class ZoomFileProvider extends FileProvider {
    // This class remains empty.
}
    