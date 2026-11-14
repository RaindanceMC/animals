package de.raindancer.modules.animals.taming;

import de.raindancer.modules.animals.AnimalsSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TamedHealthTest {

    @Test
    @DisplayName("shipped: a tamed horse has twice the health it had wild (a wolf gets five times)")
    void shipped() {
        assertThat(AnimalsSettings.DEFAULTS.tamedHealthPercent()).isEqualTo(200);
        assertThat(TamedHealth.amount(200)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    @DisplayName("100 is vanilla: no modifier at all")
    void vanilla() {
        assertThat(TamedHealth.amount(100)).isZero();
        assertThat(TamedHealth.amount(500)).isCloseTo(4.0, within(1e-9));
    }
}
