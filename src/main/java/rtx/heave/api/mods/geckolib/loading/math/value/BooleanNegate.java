package rtx.heave.api.mods.geckolib.loading.math.value;

import java.util.Set;
import rtx.heave.api.mods.geckolib.animation.state.ControllerState;
import rtx.heave.api.mods.geckolib.loading.math.MathValue;

public record BooleanNegate(MathValue value) implements MathValue {
    @Override
    public double get(ControllerState state) {
  return value.get(state) == 0.0 ? 1.0 : 0.0;
    }

    @Override
    public Set<Variable> getUsedVariables() {
  return value.getUsedVariables();
    }
}