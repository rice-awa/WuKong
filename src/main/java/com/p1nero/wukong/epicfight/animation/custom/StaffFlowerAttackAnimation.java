package com.p1nero.wukong.epicfight.animation.custom;

import com.p1nero.wukong.Config;
import com.p1nero.wukong.epicfight.skill.custom.StaffFlower;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * 尝试修改动画播放的move lock
 * 后面直接监听输入事件取消input了。。
 */
public class StaffFlowerAttackAnimation extends BasicMultipleAttackAnimation {

    public StaffFlowerAttackAnimation(float end, HumanoidArmature biped, String path, float damageMultiplier){
        super(0, path, biped,
                        new AttackAnimation.Phase(0.0F, 0.00F, 0.25F, 1.0F, 0.26F , biped.toolR, null)
                                .addProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(damageMultiplier)),
                        new AttackAnimation.Phase(0.24F, 0.25F, 0.50F, 1.0F, 0.51F , biped.toolR, null)
                                .addProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(damageMultiplier)),
                        new AttackAnimation.Phase(0.49F, 0.50F, 0.75F, 1.0F, 0.76F , biped.toolR, null)
                                .addProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(damageMultiplier)),
                        new AttackAnimation.Phase(0.74F, 0.74F, 1.0F, 1.0F, 1.2F , biped.toolR, null)
                                .addProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(damageMultiplier)));
        this.addProperty(AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER, ((dynamicAnimation, livingEntityPatch, v, v1) -> 1.5F))
                .addStateRemoveOld(EntityState.CAN_BASIC_ATTACK, false)
                .addStateRemoveOld(EntityState.CAN_SKILL_EXECUTION, false)
                .addEvents(
                        AnimationEvent.TimeStampedEvent.create(end, ((livingEntityPatch, staticAnimation, objects) -> {
                            if(livingEntityPatch instanceof ServerPlayerPatch serverPlayerPatch){
                                SkillContainer passiveContainer = serverPlayerPatch.getSkill(SkillSlots.WEAPON_PASSIVE);
                                passiveContainer.getDataManager().setDataSync(StaffFlower.PLAYING_STAFF_FLOWER, false,serverPlayerPatch.getOriginal());
                                if(passiveContainer.getDataManager().getDataValue(StaffFlower.KEY_PRESSING)){
                                    if(serverPlayerPatch.hasStamina(Config.STAFF_FLOWER_STAMINA_CONSUME.get().floatValue())){
                                        serverPlayerPatch.consumeStamina(serverPlayerPatch.getOriginal().isCreative() ? 0 : Config.STAFF_FLOWER_STAMINA_CONSUME.get().floatValue());
                                        serverPlayerPatch.reserveAnimation(staticAnimation);
                                    }
                                }
                            }
                        }), AnimationEvent.Side.SERVER),
                        AnimationEvent.TimeStampedEvent.create(0.01F, ((livingEntityPatch, staticAnimation, objects) -> {
                            if(livingEntityPatch instanceof ServerPlayerPatch serverPlayerPatch){
                                SkillContainer passiveContainer = serverPlayerPatch.getSkill(SkillSlots.WEAPON_PASSIVE);
                                passiveContainer.getDataManager().setDataSync(StaffFlower.PLAYING_STAFF_FLOWER, true, serverPlayerPatch.getOriginal());
                            }
                        }), AnimationEvent.Side.SERVER));

    }

    public StaffFlowerAttackAnimation(float convertTime, String path, Armature armature, Phase... phases) {
        super(convertTime, path, armature, phases);
    }

    protected void bindPhaseState(Phase phase) {
        float preDelay = phase.preDelay;

        if (preDelay == 0.0F) {
            preDelay += 0.01F;
        }

        this.stateSpectrumBlueprint
                .newTimePair(phase.start, preDelay)
                .addState(EntityState.PHASE_LEVEL, 1)
                .newTimePair(phase.start, phase.contact + 0.01F)
                .addState(EntityState.CAN_SKILL_EXECUTION, false)
                .newTimePair(phase.start , phase.recovery)
                .addState(EntityState.UPDATE_LIVING_MOTION, false)
                .addState(EntityState.CAN_BASIC_ATTACK, false);
        if(phase.equals(phases[phases.length-1])){
            this.stateSpectrumBlueprint.newTimePair(0, phase.end)
                    .addState(EntityState.MOVEMENT_LOCKED, true);
        }
        this.stateSpectrumBlueprint.newTimePair(phase.start, phase.end)
                .addState(EntityState.INACTION, true)
                .newTimePair(phase.antic, phase.end)
                .addState(EntityState.TURNING_LOCKED, true)
                .newTimePair(preDelay, phase.contact + 0.01F)
                .addState(EntityState.ATTACKING, true)
                .addState(EntityState.PHASE_LEVEL, 2)
                .newTimePair(phase.contact + 0.01F, phase.end)
                .addState(EntityState.PHASE_LEVEL, 3);

    }

}
