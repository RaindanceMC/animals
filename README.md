# RainsAnimals

Horses and animal armour for a Paper 26.x server, as a Rain's Flexible Modules module (`animals-module`) and a
standalone plugin (`animals-standalone`, built against RainsCore).

- **Roads:** a ridden horse runs at `animals:road-percent` of its own speed on road blocks (dirt path, gravel),
  `animals:off-road-percent` elsewhere.
- **Taming:** a tamed horse has `animals:tamed-health-percent` of its wild health (as a wolf becoming a dog).
- **Breeding:** foals land between their parents, nearer the better one; sometimes a stat beats both.
- **Whistle:** three iron nuggets over a string; calls the last of your own horses you rode.
- **Stats:** speed, jump and health above the hotbar while riding or looking at a horse; `/horses stats` switches it.
- **Horse and dog armour trims:** every trim at the smithing table, drawn from the game's own player trims
  (`art/build-animal-trims.py`).

Build: `mvn install`. CI publishes new versions to packages.tstieh.de on push to master.
Credits for the borrowed whistle texture and sounds: `animals-module/src/main/resources/de/raindancer/modules/animals/pack/CREDITS.txt`.
