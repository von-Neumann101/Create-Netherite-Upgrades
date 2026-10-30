package io.github.boosterproject.booster.content.kinetics.mixer;

import java.util.function.BooleanSupplier;

public final class NetheriteMixerBatching {
    private NetheriteMixerBatching() {}

    public static int process(BooleanSupplier matches, BooleanSupplier apply) {
        int processed = 0;
        while (matches.getAsBoolean() && apply.getAsBoolean()) {
            processed++;
        }
        return processed;
    }
}
