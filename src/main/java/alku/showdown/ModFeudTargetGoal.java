package alku.showdown;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.Collections;
import java.util.Set;

public class ModFeudTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    private final Set<EntityType<?>> hostileTypes;

    public ModFeudTargetGoal(Mob mob) {
        super(mob, LivingEntity.class, 5, false, false,
            living -> living != null && ModFeudManager.isActive());
        Set<EntityType<?>> types = ModFeudManager.getHostileEntities(mob.getType());
        this.hostileTypes = types.isEmpty() ? Collections.emptySet() : types;
    }

    @Override
    protected double getFollowDistance() {
        return 128.0;
    }

    @Override
    protected boolean canAttack(LivingEntity target, TargetingConditions conditions) {
        // Check hostile type first - O(1) HashSet lookup, cheaper than distance checks
        return hostileTypes.contains(target.getType()) && super.canAttack(target, conditions);
    }

    @Override
    public boolean canUse() {
        return !hostileTypes.isEmpty() && ModFeudManager.isActive() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return ModFeudManager.isActive() && super.canContinueToUse();
    }
}