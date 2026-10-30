package io.github.boosterproject.booster.content.kinetics.mixer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class NetheriteMixerBatchingTest {
    private static final List<String> FAILURES = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) {
        AtomicInteger attempts = new AtomicInteger();
        expect("no matching recipe performs no application", 0,
            NetheriteMixerBatching.process(() -> false, () -> { attempts.incrementAndGet(); return true; }));
        expect("an unmatched recipe does not consume anything", 0, attempts.get());

        AtomicInteger ingredients = new AtomicInteger(8);
        expect("one cycle processes every available recipe", 8,
            NetheriteMixerBatching.process(() -> ingredients.get() > 0, () -> { ingredients.decrementAndGet(); return true; }));
        expect("a full batch consumes exactly its inputs", 0, ingredients.get());

        ingredients.set(8);
        AtomicInteger outputs = new AtomicInteger();
        expect("output capacity stops a batch", 3, NetheriteMixerBatching.process(
            () -> ingredients.get() > 0 && outputs.get() < 3,
            () -> { ingredients.decrementAndGet(); outputs.incrementAndGet(); return true; }));
        expect("blocked output retains unprocessed inputs", 5, ingredients.get());

        attempts.set(0);
        expect("an application failure stops even if simulation matches", 0, NetheriteMixerBatching.process(
            () -> attempts.get() < 10, () -> { attempts.incrementAndGet(); return false; }));
        expect("a failed application is not retried in the same batch", 1, attempts.get());

        attempts.set(0);
        expect("a later failure retains the completed count", 2, NetheriteMixerBatching.process(
            () -> attempts.get() < 10, () -> attempts.incrementAndGet() <= 2));
        expect("batch stops at the first later application failure", 3, attempts.get());

        AtomicInteger heat = new AtomicInteger(1);
        expect("changed recipe requirements are checked before the next application", 1,
            NetheriteMixerBatching.process(() -> heat.get() > 0, () -> { heat.set(0); return true; }));
        if (!FAILURES.isEmpty()) throw new AssertionError(String.join(System.lineSeparator(), FAILURES));
        System.out.println("Netherite mixer batching checks passed (" + checks + " cases).");
    }

    private static void expect(String message, int expected, int actual) {
        checks++;
        if (expected != actual) FAILURES.add(message + ": expected " + expected + ", got " + actual);
    }
}
