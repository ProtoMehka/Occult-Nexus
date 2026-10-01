# Occult Nexus

A dedicated hub mod for Minecraft Forge 1.20.1, linking three mods that otherwise have no knowledge of each other: **[Ars Nouveau](https://www.curseforge.com/minecraft/mc-mods/ars-nouveau)**, **[Occultism](https://www.curseforge.com/minecraft/mc-mods/occultism)**, and **[Goety](https://www.curseforge.com/minecraft/mc-mods/goety)**. Started as a backport of **[Ars Ocultas](https://github.com/mystchonky/Ars-Ocultas)** (mystchonky) to Forge 1.20.1 after its author moved on to NeoForge 1.21+, then grown into its own thing with two original end-game summon creatures tying all three mods together.

## What it does

### Backported from Ars Ocultas

- **Sacrificial Altar** — a block consuming Ars Nouveau Source and an Occultism Mob Jar, craftable via a cross-mod recipe through Ars Nouveau's Enchanting Apparatus.
- **Familiar system** (`FamiliarDragon`) and the **Spirit Jar** automation behaviour, both ported as-is from the original LGPL-licensed source.

### Two signature summon creatures (end-game, GeckoLib-animated)

Both require real progress across all three mods — a tamed/bound familiar or Servant, and a dedicated high-cost Ars Nouveau Source catalyst — summoned through a real in-game ritual, not a creative-only spawn egg.

**Thrallflesh** — Occultism → Goety, melee tank. A spirit forced into a physical vessel of flesh and cursed metal. Summoned by sacrificing an already-tamed Occultism familiar as part of a binding ritual. Hits hard in melee and backs it up with a heavy fang-based attack.

**Reclaimed Wraith** — Goety → Occultism, ranged caster. A Servant's soul redirected away from its normal Goety resurrection and bound into Occultism's service instead — spectral, legless, genuinely flying (not just floating). Summoned with a filled Goety Soul Jar as the key ritual reagent. Fights at range by casting Goety's own `LeechingSpell` directly (real spell, real particles/sound, not an imitation), and blinks in for a quick melee strike — teleporting adjacent to its target, hitting, then teleporting back to where it started — when its target gets too close for comfort.

Both creatures regenerate slowly on their own once they've gone a while without taking a hit, rather than staying wherever a fight left them.

## Dependencies (hard)

- [Ars Nouveau](https://www.curseforge.com/minecraft/mc-mods/ars-nouveau) 4.12.7+
- [Occultism](https://www.curseforge.com/minecraft/mc-mods/occultism) 1.158.0+
- [Goety](https://www.curseforge.com/minecraft/mc-mods/goety) 2.5.55.4+
- Minecraft Forge 1.20.1-47.4.10+

No soft-dependency/degraded mode without any of the three — this is a deliberate choice for a stronger, more integrated mod identity over broader compatibility.

## Building

```
./gradlew build
```

The output jar is in `build/libs/`.

## License

**AGPLv3.** This is a constraint inherited from Ars Ocultas' own LGPL-3.0-only license (which permits reconversion to GPLv3/AGPLv3), not a free choice — unlike this workspace's other mods, redistribution and forking of Occult Nexus must stay open. Source code is available as the AGPL requires. Original attribution to mystchonky (Ars Ocultas) is preserved in [CREDITS.txt](CREDITS.txt), along with a texture credit to Occultism (MIT License). See [LICENSE.txt](LICENSE.txt) for the full text.
