package io.github.boosterproject.booster.content.fluids.tank;

import com.simibubi.create.content.fluids.tank.BoilerData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class BoilerLevelTest {
    private static final List<String> FAILURES = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) {
        NetheriteBoilerData boiler = new NetheriteBoilerData();
        boiler.activeHeat = 100;
        boiler.waterSupply = 1200;
        expect("empty size provides no capacity", 0, boiler.getMaxHeatLevelForBoilerSize(0));
        expect("44 blocks retain the size limit", 11, boiler.getMaxHeatLevelForBoilerSize(44));
        expect("45 blocks unlock size level 100", 100, boiler.getMaxHeatLevelForBoilerSize(45));
        expect("larger boilers need no further size expansion", 100, boiler.getMaxHeatLevelForBoilerSize(108));
        boiler.activeHeat = 200;
        expect("unlocked size follows the lower water level", 100, boiler.getMaxHeatLevelForBoilerSize(45));
        boiler.waterSupply = 240;
        expect("size follows a falling water level", 20, boiler.getMaxHeatLevelForBoilerSize(45));
        boiler.activeHeat = 10;
        expect("unlocked size retains the full vanilla size rating", 18, boiler.getMaxHeatLevelForBoilerSize(45));
        boiler.waterSupply = 1200;
        boiler.activeHeat = 50;
        expect("unlocked size follows the lower heat level", 50, boiler.getMaxHeatLevelForBoilerSize(45));

        boiler.waterSupply = 216;
        expect("216 mB/t provides water level 18", 18, boiler.getMaxHeatLevelForWaterSupply());
        boiler.waterSupply = 228;
        expect("228 mB/t exceeds water level 18", 19, boiler.getMaxHeatLevelForWaterSupply());
        boiler.waterSupply = 1200;
        expect("faster water input continues increasing water level", 100, boiler.getMaxHeatLevelForWaterSupply());

        boiler.activeHeat = 18;
        boiler.waterSupply = 216;
        boiler.calcMinMaxForSize(72);
        checks++;
        if (!(boiler.getHeatLevelTextComponent().getContents() instanceof TranslatableContents contents)
            || !contents.getKey().equals("create.boiler.lvl")
            || contents.getArgs().length != 1
            || !contents.getArgs()[0].equals("18")) {
            FAILURES.add("level 18 must display its numeric level instead of a maximum-level label");
        }

        boiler.activeHeat = 20;
        boiler.attachedEngines = 20;
        boiler.waterSupply = 240;
        expect("45 blocks fully power 20 engines at level 20", 1, boiler.getEngineEfficiency(45));
        expect("44 blocks still limit 20 engines", 11 / 20F, boiler.getEngineEfficiency(44));
        expect("small size still limits output", 0.5F, boiler.getEngineEfficiency(40));
        boiler.waterSupply = 120;
        expect("water supply still limits output", 0.5F, boiler.getEngineEfficiency(45));
        boiler.waterSupply = 240;
        boiler.activeHeat = 10;
        expect("heat still limits output", 0.5F, boiler.getEngineEfficiency(45));
        boiler.activeHeat = 100;
        boiler.waterSupply = 1200;
        boiler.attachedEngines = 100;
        expect("45 blocks fully power 100 engines at level 100", 1, boiler.getEngineEfficiency(45));
        boiler.activeHeat = 40;
        expect("heat decrease immediately reduces output", 0.4F, boiler.getEngineEfficiency(45));
        boiler.activeHeat = 100;
        boiler.waterSupply = 360;
        expect("water decrease immediately reduces output", 0.3F, boiler.getEngineEfficiency(45));
        boiler.waterSupply = 0;
        expect("no water produces no power", 0, boiler.getEngineEfficiency(45));
        expect("no water retains a positive size rating for the water-input hint", 18,
            boiler.getMaxHeatLevelForBoilerSize(45));
        boiler.waterSupply = 1200;
        boiler.activeHeat = 0;
        expect("no heat produces no power", 0, boiler.getEngineEfficiency(45));

        boiler.passiveHeat = true;
        boiler.activeHeat = 0;
        boiler.attachedEngines = 1;
        boiler.waterSupply = 12;
        expect("passive heat retains its original efficiency", 0.125F, boiler.getEngineEfficiency(4));
        expect("unlocked size retains passive heating", 0.125F, boiler.getEngineEfficiency(45));
        boiler.waterSupply = 0;
        expect("passive heating still requires water", 0, boiler.getEngineEfficiency(45));

        BoilerData vanilla = new BoilerData();
        vanilla.waterSupply = 1200;
        expect("vanilla size level remains capped", 18, vanilla.getMaxHeatLevelForBoilerSize(108));
        expect("vanilla water level remains capped", 18, vanilla.getMaxHeatLevelForWaterSupply());
        vanilla.activeHeat = 20;
        vanilla.attachedEngines = 20;
        expect("vanilla 45-block boiler still has its original size limit", 11 / 20F,
            vanilla.getEngineEfficiency(45));

        if (!FAILURES.isEmpty()) {
            throw new AssertionError(String.join(System.lineSeparator(), FAILURES));
        }
        System.out.println("Boiler level regression checks passed (" + checks + " cases).");
    }

    private static void expect(String description, float expected, float actual) {
        checks++;
        if (Float.compare(expected, actual) != 0) {
            FAILURES.add(description + ": expected " + expected + ", got " + actual);
        }
    }
}
