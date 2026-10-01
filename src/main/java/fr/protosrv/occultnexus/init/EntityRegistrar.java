package fr.protosrv.occultnexus.init;

import fr.protosrv.occultnexus.OccultNexus;
import fr.protosrv.occultnexus.content.thrallflesh.ThrallfleshEntity;
import fr.protosrv.occultnexus.content.wraith.WraithEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityRegistrar {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, OccultNexus.MODID);

    public static final RegistryObject<EntityType<ThrallfleshEntity>> THRALLFLESH = ENTITY_TYPES.register("thrallflesh",
            // CREATURE, not MONSTER: it's a bound familiar, not a roaming hostile — MONSTER would
            // despawn it on Peaceful difficulty and count it against the hostile mob spawn cap.
            () -> EntityType.Builder.of(ThrallfleshEntity::new, MobCategory.CREATURE)
                    .sized(2.0f, 3.8f)
                    .clientTrackingRange(16)
                    .build("thrallflesh"));

    // Hitbox estimated from the .bbmodel rest pose (rotated cube corners, model space / 16): body
    // torso alone is ~1 block wide, but the caster's forward-reaching arm pose spans a raw ~2
    // blocks tip-to-tip and the head top sits at Y=50.5 (~3.16 blocks above ground) — matches the
    // "one of the pack's largest entities" framing in docs/occult_nexus.md. Width is kept tighter
    // than the full arm span (same convention as vanilla Enderman not hit-boxing its long arms).
    // Unverified guess, flagged for in-game check.
    public static final RegistryObject<EntityType<WraithEntity>> RECLAIMED_WRAITH = ENTITY_TYPES.register("reclaimed_wraith",
            () -> EntityType.Builder.of(WraithEntity::new, MobCategory.CREATURE)
                    .sized(1.4f, 3.3f)
                    .clientTrackingRange(16)
                    .build("reclaimed_wraith"));

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    @Mod.EventBusSubscriber(modid = OccultNexus.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Attributes {
        @SubscribeEvent
        public static void onAttributeCreate(EntityAttributeCreationEvent event) {
            event.put(THRALLFLESH.get(), ThrallfleshEntity.createAttributes().build());
            event.put(RECLAIMED_WRAITH.get(), WraithEntity.createAttributes().build());
        }
    }
}
