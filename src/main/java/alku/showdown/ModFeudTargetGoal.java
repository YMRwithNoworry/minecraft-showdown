package alku.showdown;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public class ModFeudTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    public ModFeudTargetGoal(Mob mob) {
        super(mob, LivingEntity.class, 5, false, false,
            living -> living != null && ModFeudManager.areHostile(mob.getType(), living.getType()));
    }

    @Override
    protected double getFollowDistance() {
        return 128.0;
    }

    @Override
    protected boolean canAttack(LivingEntity target, TargetingConditions conditions) {
        // The selector passed to the superclass is used when picking the nearest target;
        // keep the same check here so an already-selected target cannot continue after teams change.
        return target != null && ModFeudManager.areHostile(mob.getType(), target.getType()) && super.canAttack(target, conditions);
    }

    @Override
    public boolean canUse() {
        return ModFeudManager.hasHostileEntities(mob.getType()) && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        return target != null && ModFeudManager.areHostile(mob.getType(), target.getType()) && super.canContinueToUse();
    }
}
