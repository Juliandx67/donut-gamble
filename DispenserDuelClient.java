package dispenserduel;

import net.fabricmc.api.ClientModInitializer;

/** Nur fuer den Einzelbetrieb. In einer gemeinsamen Mod loeschst du diese Datei
 *  und rufst stattdessen DispenserDuelFeature.init() in deinem eigenen Entrypoint auf. */
public class DispenserDuelClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DispenserDuelFeature.init();
    }
}
