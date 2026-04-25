package com.github.brainage04.fortnite_in_minecraft.effect;

import eu.pb4.polymer.core.api.other.PolymerStatusEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class BoogieEffect extends StatusEffect implements PolymerStatusEffect {
    protected BoogieEffect() {
        super(StatusEffectCategory.HARMFUL, 0xa0ffa0);
    }
}
