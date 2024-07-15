package com.p1nero.wukong.epicfight.skill;

import com.p1nero.wukong.Config;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.client.keymapping.WukongKeyMappings;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import com.p1nero.wukong.network.PacketHandler;
import com.p1nero.wukong.network.PacketRelay;
import com.p1nero.wukong.network.packet.server.PlayStaffFlowerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.SourceTags;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.UUID;

/**
 * 防守技能棍花
 */
public class StaffFlower extends Skill {

    public static final SkillDataManager.SkillDataKey<Boolean> PLAYING_STAFF_FLOWER = SkillDataManager.SkillDataKey.createDataKey(SkillDataManager.ValueType.BOOLEAN);
    private static final SkillDataManager.SkillDataKey<Boolean> MOVING = SkillDataManager.SkillDataKey.createDataKey(SkillDataManager.ValueType.BOOLEAN);
    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a05b-0242ac191981");

    public StaffFlower(Builder<? extends Skill> builder) {
        super(builder);
    }

    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
        container.getDataManager().registerData(PLAYING_STAFF_FLOWER);
        container.getDataManager().registerData(MOVING);
        container.getExecuter().getEventListener().addEventListener(PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT, EVENT_UUID, (event -> {
                container.getDataManager().setData(MOVING, true);
            if (container.getDataManager().getDataValue(PLAYING_STAFF_FLOWER)) {
                LocalPlayer clientPlayer = event.getPlayerPatch().getOriginal();
                clientPlayer.setSprinting(false);
                clientPlayer.sprintTime = -1;
                Minecraft mc = Minecraft.getInstance();
                ClientEngine.getInstance().controllEngine.setKeyBind(mc.options.keySprint, false);
            }
        }));
        container.getExecuter().getEventListener().addEventListener(PlayerEventListener.EventType.HURT_EVENT_PRE, EVENT_UUID, (event -> {
            if(container.getDataManager().getDataValue(PLAYING_STAFF_FLOWER) && (WukongMoveset.canBeBlocked(event.getDamageSource().getDirectEntity()) || event.getDamageSource().isProjectile())){
                if(!isBlocked(event.getDamageSource(), event.getPlayerPatch().getOriginal())){
                    return;
                }
                event.setCanceled(true);
                event.setResult(AttackResult.ResultType.BLOCKED);
                LivingEntityPatch<?> attackerPatch = (LivingEntityPatch<?>)EpicFightCapabilities.getEntityPatch(event.getDamageSource().getEntity(), LivingEntityPatch.class);
                if (attackerPatch != null) {
                    attackerPatch.setLastAttackEntity(event.getPlayerPatch().getOriginal());
                }
                Entity directEntity = event.getDamageSource().getDirectEntity();
                LivingEntityPatch<?> entityPatch = (LivingEntityPatch<?>)EpicFightCapabilities.getEntityPatch(directEntity, LivingEntityPatch.class);
                if (entityPatch != null) {
                    entityPatch.onAttackBlocked(event.getDamageSource(), event.getPlayerPatch());
                }
                showBlockedEffect(event.getPlayerPatch(), event.getDamageSource().getDirectEntity());
                SkillContainer skillContainer = event.getPlayerPatch().getSkill(SkillSlots.WEAPON_INNATE);
                skillContainer.getSkill().setConsumptionSynchronize(event.getPlayerPatch(), skillContainer.getResource(1.0F) + skillContainer.getMaxResource() / 5);
            }
        }));
    }

    /**
     * 判断是否是正面且可被格挡
     */
    private boolean isBlocked(DamageSource damageSource, ServerPlayer player){
        Vec3 sourceLocation = damageSource.getSourcePosition();
        if (sourceLocation != null) {
            Vec3 viewVector = player.getViewVector(1.0F);
            Vec3 toSourceLocation = sourceLocation.subtract((player).position()).normalize();
            if (toSourceLocation.dot(viewVector) > 0.0) {
                if (damageSource instanceof EpicFightDamageSource epicFightDamageSource) {
                    return !epicFightDamageSource.hasTag(SourceTags.GUARD_PUNCTURE);
                }
            }
        }
        return false;
    }

    public static void showBlockedEffect(ServerPlayerPatch playerPatch, Entity directEntity){
        playerPatch.playSound(EpicFightSounds.CLASH, -0.05F, 0.1F);
        ServerPlayer serverPlayer = playerPatch.getOriginal();
        EpicFightParticles.HIT_BLUNT.get().spawnParticleWithArgument(serverPlayer.getLevel(), HitParticleType.FRONT_OF_EYES, HitParticleType.ZERO, serverPlayer, directEntity);
    }

    /**
     * 判断武器是否是悟空棍子类型
     */
    public static boolean isWeaponValid(PlayerPatch<?> playerPatch){
        return playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory().equals(WukongWeaponCategories.WK_STAFF);
    }

    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        container.getExecuter().getEventListener().removeListener(PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT, EVENT_UUID);
        container.getExecuter().getEventListener().removeListener(PlayerEventListener.EventType.HURT_EVENT_PRE, EVENT_UUID);
    }

    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        if(!container.getExecuter().isLogicalClient() || !isWeaponValid(container.getExecuter())){
            return;
        }

        if(container.getExecuter().isBattleMode() && WukongKeyMappings.STAFF_FLOWER.isDown() && container.getExecuter().hasStamina(Config.STAFF_FLOWER_STAMINA_CONSUME.get().floatValue())){
            if(!container.getDataManager().getDataValue(PLAYING_STAFF_FLOWER)){
                PacketRelay.sendToServer(PacketHandler.INSTANCE, new PlayStaffFlowerPacket());
                container.getDataManager().setDataSync(PLAYING_STAFF_FLOWER, true, ((LocalPlayer) container.getExecuter().getOriginal()));
            } else {
                container.getExecuter().getOriginal().setDeltaMovement(0,0,0);//EntityState没用
            }
        } else {
            container.getDataManager().setDataSync(PLAYING_STAFF_FLOWER, false, ((LocalPlayer) container.getExecuter().getOriginal()));
        }
    }
}
