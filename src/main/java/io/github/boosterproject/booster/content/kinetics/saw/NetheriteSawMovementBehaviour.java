package io.github.boosterproject.booster.content.kinetics.saw;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.saw.SawMovementBehaviour;

public class NetheriteSawMovementBehaviour extends SawMovementBehaviour {
    @Override protected float getBlockBreakingSpeed(MovementContext context) { return super.getBlockBreakingSpeed(context) * 2; }
}
