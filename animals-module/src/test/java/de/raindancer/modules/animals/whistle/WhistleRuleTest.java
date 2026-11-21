package de.raindancer.modules.animals.whistle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WhistleRuleTest {

    private static final UUID ME = UUID.randomUUID();
    private static final UUID SOMEBODY = UUID.randomUUID();

    @Test
    @DisplayName("no horse ridden yet: nothing to call")
    void none() {
        assertThat(WhistleRule.decide(ME, false, null, true, false, 10, 256)).isEqualTo(WhistleRule.Outcome.NO_HORSE);
    }

    @Test
    @DisplayName("the horse comes when it is the caller's, in the same world, within reach, and nobody else rides it")
    void comes() {
        assertThat(WhistleRule.decide(ME, true, ME, true, false, 200, 256)).isEqualTo(WhistleRule.Outcome.CALL);
        assertThat(WhistleRule.decide(ME, true, ME, true, false, 0, 256)).isEqualTo(WhistleRule.Outcome.CALL);
    }

    @Test
    @DisplayName("and says why not otherwise")
    void refusals() {
        assertThat(WhistleRule.decide(ME, true, ME, false, false, 10, 256)).isEqualTo(WhistleRule.Outcome.OTHER_WORLD);
        assertThat(WhistleRule.decide(ME, true, ME, true, false, 300, 256)).isEqualTo(WhistleRule.Outcome.TOO_FAR);
        assertThat(WhistleRule.decide(ME, true, ME, true, true, 10, 256)).isEqualTo(WhistleRule.Outcome.RIDDEN);
        assertThat(WhistleRule.decide(ME, true, SOMEBODY, true, false, 10, 256)).isEqualTo(WhistleRule.Outcome.NOT_YOURS);
        assertThat(WhistleRule.decide(ME, true, null, true, false, 10, 256)).isEqualTo(WhistleRule.Outcome.NOT_YOURS);
    }

    @Test
    @DisplayName("reach 0 means wherever you are: any distance, and from another world too")
    void unlimited() {
        assertThat(WhistleRule.decide(ME, true, ME, true, false, 100_000, 0)).isEqualTo(WhistleRule.Outcome.CALL);
        assertThat(WhistleRule.decide(ME, true, ME, false, false, 0, 0)).isEqualTo(WhistleRule.Outcome.CALL);
        assertThat(WhistleRule.decide(ME, true, ME, false, true, 0, 0)).isEqualTo(WhistleRule.Outcome.RIDDEN);
    }

    @Test
    @DisplayName("the decision is about the caller: owner compared with who whistled")
    void ownerIsTheCaller() {
        assertThat(WhistleRule.owns(ME, ME)).isTrue();
        assertThat(WhistleRule.owns(ME, SOMEBODY)).isFalse();
        assertThat(WhistleRule.owns(ME, null)).isFalse();
    }
}
