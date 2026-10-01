package fr.protosrv.occultnexus.init;

import com.hollingsworth.arsnouveau.api.mob_jar.JarBehaviorRegistry;
import com.klikli_dev.occultism.registry.OccultismEntities;
import fr.protosrv.occultnexus.content.spiritjar.SpiritJarBehaviour;

public class IntegrationRegistrar {

    public static void postInit() {
        registerJarBehaviours();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerJarBehaviours() {
        JarBehaviorRegistry.register(OccultismEntities.FOLIOT.get(), new SpiritJarBehaviour());
        JarBehaviorRegistry.register(OccultismEntities.DJINNI.get(), new SpiritJarBehaviour());
        JarBehaviorRegistry.register(OccultismEntities.AFRIT.get(), new SpiritJarBehaviour());
        JarBehaviorRegistry.register(OccultismEntities.MARID.get(), new SpiritJarBehaviour());
    }

    private IntegrationRegistrar() {
    }
}
