package de.raindancer.modules.animals.stats;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.animals.AnimalsSettings;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("horse stats on /preferences")
class StatsPreferenceTest {

    private final Messages messages = mock(Messages.class);
    private final Player player = mock(Player.class);
    private final PersistentDataContainer data = mock(PersistentDataContainer.class);
    private final AnimalsSettings settings = mock(AnimalsSettings.class);

    StatsPreferenceTest() {
        when(player.getPersistentDataContainer()).thenReturn(data);
        when(settings.stats()).thenReturn(true);
    }

    @Test
    @DisplayName("a switch in group Horses with its own id")
    void built() {
        Preference stats = StatsPreference.build(() -> settings, messages);
        assertThat(stats.id()).isEqualTo("animals:horse-stats");
        assertThat(stats.group()).isEqualTo("Horses");
        assertThat(stats.options()).containsExactly("On", "Off");
    }

    @Test
    @DisplayName("on unless the player hid them")
    void reads() {
        Preference stats = StatsPreference.build(() -> settings, messages);
        when(data.has(any(org.bukkit.NamespacedKey.class), any(PersistentDataType.class))).thenReturn(false);
        assertThat(stats.state(player)).isEqualTo("On");
        when(data.has(any(org.bukkit.NamespacedKey.class), any(PersistentDataType.class))).thenReturn(true);
        assertThat(stats.state(player)).isEqualTo("Off");
    }

    @Test
    @DisplayName("turning off hides them and says so, like the command; turning on forgets the hiding")
    void writes() {
        Preference stats = StatsPreference.build(() -> settings, messages);
        stats.choose().accept(player, 1);
        verify(data).set(StatsDisplay.HIDDEN, PersistentDataType.BYTE, (byte) 1);
        verify(messages).send(player, "animals.stats.hidden");
        stats.choose().accept(player, 0);
        verify(data).remove(StatsDisplay.HIDDEN);
        verify(messages).send(player, "animals.stats.shown");
    }

    @Test
    @DisplayName("not offered while the server has the stats off")
    void serverOff() {
        Preference stats = StatsPreference.build(() -> settings, messages);
        assertThat(stats.shown().test(player)).isTrue();
        when(settings.stats()).thenReturn(false);
        assertThat(stats.shown().test(player)).isFalse();
    }
}
