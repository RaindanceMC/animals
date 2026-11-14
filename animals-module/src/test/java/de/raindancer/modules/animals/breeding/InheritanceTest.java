package de.raindancer.modules.animals.breeding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class InheritanceTest {

    private static final Inheritance.Range SPEED = Inheritance.SPEED;

    @Test
    @DisplayName("without a mutation a foal lands between its parents, nearer the better one")
    void leansToTheBetterParent() {
        Inheritance moderate = new Inheritance(0, 5);
        Random random = new Random(1);
        double sum = 0;
        int n = 2000;
        for (int i = 0; i < n; i++) {
            double foal = moderate.child(0.15, 0.30, SPEED, random);
            assertThat(foal).isBetween(SPEED.min(), SPEED.max());
            sum += foal;
        }
        double mean = sum / n;
        assertThat(mean).as("60 % of the way to the better parent, on average").isCloseTo(0.15 + 0.6 * 0.15, within(0.003));
        assertThat(mean).isGreaterThan((0.15 + 0.30) / 2);
    }

    @Test
    @DisplayName("10 percent of foals beat both parents, by at most 5 percent, never past the game's maximum")
    void sometimesBetter() {
        Inheritance moderate = new Inheritance(10, 5);
        Random random = new Random(7);
        int better = 0;
        int n = 10000;
        for (int i = 0; i < n; i++) {
            double foal = moderate.child(0.20, 0.30, SPEED, random);
            if (foal > 0.30) {
                better++;
                assertThat(foal).isLessThanOrEqualTo(0.30 * 1.05 + 1e-12);
            }
        }
        assertThat(better / (double) n).isCloseTo(0.10, within(0.015));
        for (int i = 0; i < 200; i++) {
            assertThat(new Inheritance(100, 5).child(SPEED.max(), SPEED.max(), SPEED, random)).isLessThanOrEqualTo(SPEED.max());
        }
    }

    @Test
    @DisplayName("the game's ranges: speed, jump strength and health")
    void ranges() {
        assertThat(Inheritance.SPEED.min()).isEqualTo(0.1125);
        assertThat(Inheritance.SPEED.max()).isEqualTo(0.3375);
        assertThat(Inheritance.JUMP.min()).isEqualTo(0.4);
        assertThat(Inheritance.JUMP.max()).isEqualTo(1.0);
        assertThat(Inheritance.HEALTH.min()).isEqualTo(15);
        assertThat(Inheritance.HEALTH.max()).isEqualTo(30);
    }
}
