package io.github.boosterproject.booster.content.fluids.tank;

import com.simibubi.create.content.fluids.tank.BoilerData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class BoilerLevelTest {
    private static final List<String> FAILURES = new ArrayList<>();

    public static void main(String[] args) {
        NetheriteBoilerData boiler = new NetheriteBoilerData();
        expect("72 blocks provide size level 18", 18, boiler.getMaxHeatLevelForBoilerSize(72));
        expect("76 blocks exceed size level 18", 19, boiler.getMaxHeatLevelForBoilerSize(76));
        expect("larger boilers continue increasing size level", 27, boiler.getMaxHeatLevelForBoilerSize(108));

        boiler.waterSupply = 216;
        expect("216 mB/t provides water level 18", 18, boiler.getMaxHeatLevelForWaterSupply());
        boiler.waterSupply = 228;
        expect("228 mB/t exceeds water level 18", 19, boiler.getMaxHeatLevelForWaterSupply());
        boiler.waterSupply = 1200;
        expect("faster water input continues increasing water level", 100, boiler.getMaxHeatLevelForWaterSupply());

        boiler.activeHeat = 18;
        boiler.waterSupply = 216;
        boiler.calcMinMaxForSize(72);
        if (!(boiler.getHeatLevelTextComponent().getContents() instanceof TranslatableContents contents)
            || !contents.getKey().equals("create.boiler.lvl")
            || contents.getArgs().length != 1
            || !contents.getArgs()[0].equals("18")) {
            FAILURES.add("level 18 must display its numeric level instead of a maximum-level label");
        }

        boiler.activeHeat = 20;
        boiler.attachedEngines = 20;
        boiler.waterSupply = 240;
        expect("level 20 fully powers 20 engines", 1, boiler.getEngineEfficiency(80));
        expect("small size still limits output", 0.5F, boiler.getEngineEfficiency(40));
        boiler.waterSupply = 120;
        expect("water supply still limits output", 0.5F, boiler.getEngineEfficiency(80));
        boiler.waterSupply = 240;
        boiler.activeHeat = 10;
        expect("heat still limits output", 0.5F, boiler.getEngineEfficiency(80));
        boiler.waterSupply = 0;
        expect("no water produces no power", 0, boiler.getEngineEfficiency(80));

        boiler.passiveHeat = true;
        boiler.activeHeat = 0;
        boiler.attachedEngines = 1;
        boiler.waterSupply = 12;
        expect("passive heat retains its original efficiency", 0.125F, boiler.getEngineEfficiency(4));

        BoilerData vanilla = new BoilerData();
        vanilla.waterSupply = 1200;
        expect("vanilla size level remains capped", 18, vanilla.getMaxHeatLevelForBoilerSize(108));
        expect("vanilla water level remains capped", 18, vanilla.getMaxHeatLevelForWaterSupply());

        if (!FAILURES.isEmpty()) {
            throw new AssertionError(String.join(System.lineSeparator(), FAILURES));
        }
        System.out.println("Boiler level regression checks passed (15 cases).");
    }

    private static void expect(String description, float expected, float actual) {
        if (Float.compare(expected, actual) != 0) {
            FAILURES.add(description + ": expected " + expected + ", got " + actual);
        }
    }
}
