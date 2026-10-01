package fr.protosrv.occultnexus;

import net.minecraftforge.common.ForgeConfigSpec;

public final class OccultNexusConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue CONTAINMENT_JARS_SOUL_GEM_PICKUP;
    public static final ForgeConfigSpec.BooleanValue CONTAINMENT_JARS_SOUL_GEM_PLACE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Occultism Soul Gem <-> Ars Nouveau Mob Jar interaction (ported from Ars Ocultas)");
        CONTAINMENT_JARS_SOUL_GEM_PICKUP = builder
                .comment("Allow empty Soul Gems to be used on filled Mob Jars to pick up the contained mob")
                .define("containment_jar.soul_gem.pickup", true);
        CONTAINMENT_JARS_SOUL_GEM_PLACE = builder
                .comment("Allow filled Soul Gems to be used on empty Mob Jars to place the mob into the jar")
                .define("containment_jar.soul_gem.place", true);

        SPEC = builder.build();
    }

    private OccultNexusConfig() {
    }
}
