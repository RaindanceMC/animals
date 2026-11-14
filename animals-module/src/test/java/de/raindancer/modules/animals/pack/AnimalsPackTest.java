package de.raindancer.modules.animals.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** What the client is sent: every file listed, every trim pattern drawn on horse armour, the whistle whole. */
class AnimalsPackTest {

    private static final Path ROOT = Path.of("src/main/resources" + AnimalsPack.ROOT);
    private static final List<String> PATTERNS = List.of("bolt", "coast", "dune", "eye", "flow", "host", "raiser",
            "rib", "sentry", "shaper", "silence", "snout", "spire", "tide", "vex", "ward", "wayfinder", "wild");

    private static JsonObject json(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject();
    }

    @Test
    @DisplayName("the file list is exactly what is in the folder")
    void filesListed() throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(ROOT)) {
            walk.filter(Files::isRegularFile).forEach(p -> found.add(ROOT.relativize(p).toString().replace('\\', '/')));
        }
        assertThat(new TreeSet<>(AnimalsPack.FILES)).isEqualTo(new TreeSet<>(found));
    }

    @Test
    @DisplayName("every trim pattern has a horse armour texture: tinted through the palette, shaded, in the trim greys only")
    void horseTrims() throws IOException {
        Set<Integer> greys = Set.of(0xE0E0E0, 0xC0C0C0, 0xA0A0A0, 0x808080, 0x606060, 0x404040, 0x202020, 0x000000);
        for (String pattern : PATTERNS) {
            String texture = "assets/minecraft/textures/trims/entity/horse_body/" + pattern + ".png";
            assertThat(json(texture + ".mcmeta").getAsJsonObject("palette").get("base_palette").getAsString())
                    .as(pattern).isEqualTo("trim_base");
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(ROOT.resolve(texture).toFile());
            assertThat(image.getWidth()).isEqualTo(64);
            assertThat(image.getHeight()).isEqualTo(64);
            Set<Integer> shades = new TreeSet<>();
            int drawn = 0;
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    int argb = image.getRGB(x, y);
                    if ((argb >>> 24) != 0) {
                        shades.add(argb & 0xFFFFFF);
                        drawn++;
                    }
                }
            }
            assertThat(shades).as(pattern + "'s greys").isSubsetOf(greys);
            assertThat(shades.size()).as(pattern + " has depth").isGreaterThanOrEqualTo(3);
            assertThat(drawn).as(pattern + " shows").isGreaterThan(60);
        }
    }

    @Test
    @DisplayName("the whistle: item definition, model and texture all there, and four sounds under one event")
    void whistle() throws IOException {
        assertThat(json("assets/rainsanimals/items/whistle.json").getAsJsonObject("model").get("model").getAsString())
                .isEqualTo("rainsanimals:item/whistle");
        assertThat(json("assets/rainsanimals/models/item/whistle.json").getAsJsonObject("textures").get("layer0")
                .getAsString()).isEqualTo("rainsanimals:item/whistle");
        assertThat(ROOT.resolve("assets/rainsanimals/textures/item/whistle.png")).isRegularFile();
        var sounds = json("assets/rainsanimals/sounds.json").getAsJsonObject("whistle").getAsJsonArray("sounds");
        assertThat(sounds).hasSize(4);
        for (var sound : sounds) {
            String name = sound.getAsString().replace("rainsanimals:", "");
            assertThat(ROOT.resolve("assets/rainsanimals/sounds/" + name + ".ogg")).isRegularFile();
        }
    }

    @Test
    @DisplayName("the borrowed art travels with its licences and credits")
    void licences() {
        assertThat(ROOT.resolve("licenses/horse-whistle-Apache-2.0.txt")).content().contains("Apache License");
        assertThat(ROOT.resolve("licenses/whistlebound-horse-MIT.txt")).content().contains("MIT License");
        assertThat(ROOT.resolve("CREDITS.txt")).content().contains("jbossjaslow").contains("ThBTT");
    }
}
