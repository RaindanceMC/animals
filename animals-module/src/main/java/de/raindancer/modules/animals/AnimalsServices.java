package de.raindancer.modules.animals;

import de.raindancer.core.ui.messages.Messages;

import java.util.function.Supplier;

/** What the commands need from the running module. */
public record AnimalsServices(Messages messages, Supplier<AnimalsSettings> settings) {
}
