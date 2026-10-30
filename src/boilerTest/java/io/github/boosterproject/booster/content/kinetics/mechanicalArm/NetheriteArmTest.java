package io.github.boosterproject.booster.content.kinetics.mechanicalArm;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

public final class NetheriteArmTest {
    private static final List<String> FAILURES = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) {
        expect("stopped arm stays stopped", 0, NetheriteArmBehavior.movementSpeed(256, 0));
        expect("64 RPM moves three times faster", 192, NetheriteArmBehavior.movementSpeed(256, 64));
        expect("reverse rotation has the same working speed", 192, NetheriteArmBehavior.movementSpeed(256, -64));
        expect("128 RPM moves three times faster", 384, NetheriteArmBehavior.movementSpeed(256, 128));
        expect("full speed reaches three times the native limit", 768, NetheriteArmBehavior.movementSpeed(256, 256));
        expect("overspeed retains the three-times limit", 768, NetheriteArmBehavior.movementSpeed(256, 2048));
        expect("the native clamp argument is respected", 192, NetheriteArmBehavior.movementSpeed(64, 256));
        expect("default stress is 3 SU per RPM", 3, NetheriteArmBehavior.stressImpact(2));
        expect("custom native stress scales dynamically", 6, NetheriteArmBehavior.stressImpact(4));
        expect("disabled native stress stays zero", 0, NetheriteArmBehavior.stressImpact(0));
        expect("default range increases from 5 to 15", 15, NetheriteArmBehavior.workRange(5));
        expect("custom range scales dynamically", 21, NetheriteArmBehavior.workRange(7));
        expect("range remains positive at the integer limit", Integer.MAX_VALUE - 1,
            NetheriteArmBehavior.workRange(Integer.MAX_VALUE));
        int range = NetheriteArmBehavior.workRange(5);
        check(NetheriteArmBehavior.isWithinRange(new BlockPos(14, 0, 0), range), "a distant target inside the new range is accepted");
        check(!NetheriteArmBehavior.isWithinRange(new BlockPos(15, 0, 0), range), "the native exclusive range boundary is preserved");
        check(NetheriteArmBehavior.isWithinRange(new BlockPos(-14, 0, 0), range), "negative offsets have the same range");
        check(NetheriteArmBehavior.isWithinRange(new BlockPos(10, 10, 0), range), "an in-range diagonal target is accepted");
        check(!NetheriteArmBehavior.isWithinRange(new BlockPos(11, 11, 0), range), "range uses spherical distance");
        check(!NetheriteArmBehavior.isWithinRange(new BlockPos(100000, 0, 0), range), "remote targets are rejected");
        check(!NetheriteArmBehavior.isWithinRange(new BlockPos(14, 0, 0), 5), "the same target stays outside vanilla range");
        CompoundTag target = new CompoundTag();
        target.put("Pos", NbtUtils.writeBlockPos(new BlockPos(14, 0, 0)));
        check(NetheriteArmBehavior.isWithinRange(target, range), "serialized native relative targets retain extended range");
        target.put("Pos", NbtUtils.writeBlockPos(new BlockPos(15, 0, 0)));
        check(!NetheriteArmBehavior.isWithinRange(target, range), "serialized boundary targets are rejected before world access");
        target.put("Pos", NbtUtils.writeBlockPos(new BlockPos(100000, 0, 0)));
        check(!NetheriteArmBehavior.isWithinRange(target, range), "serialized remote targets are rejected before world access");
        if (!FAILURES.isEmpty()) throw new AssertionError(String.join(System.lineSeparator(), FAILURES));
        System.out.println("Netherite arm regression checks passed (" + checks + " cases).");
    }

    private static void expect(String message, double expected, double actual) {
        check(Double.compare(expected, actual) == 0, message + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) FAILURES.add(message);
    }
}
