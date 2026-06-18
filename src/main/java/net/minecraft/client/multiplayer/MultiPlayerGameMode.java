package net.minecraft.client.multiplayer;

import baritone.utils.accessor.IPlayerControllerMP;
import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import com.mojang.logging.LogUtils;
import com.viaversion.viafabricplus.features.interaction.r1_18_2_block_ack_emulation.ClientPlayerInteractionManager1_18_2;
import com.viaversion.viafabricplus.features.interaction.replace_block_placement_logic.ActionResultException1_12_2;
import com.viaversion.viafabricplus.injection.access.interaction.container_clicking.IAbstractContainerMenu;
import com.viaversion.viafabricplus.injection.access.interaction.r1_18_2_block_ack_emulation.IMultiPlayerGameMode;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.protocoltranslator.impl.provider.viaversion.ViaFabricPlusHandItemProvider;
import com.viaversion.viafabricplus.protocoltranslator.translator.ItemTranslator;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.Protocol1_16_4To1_17;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPackets1_21_4;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.Objects;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.player.EventAttack;
import ru.arixcompany.utils.MessageSender;


public class MultiPlayerGameMode implements IPlayerControllerMP, IMultiPlayerGameMode {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;
    private final ClientPacketListener connection;
    private BlockPos destroyBlockPos = new BlockPos(-1, -1, -1);
    private ItemStack destroyingItem = ItemStack.EMPTY;
    private float destroyProgress;
    private float destroyTicks;
    private int destroyDelay;
    private boolean isDestroying;
    private GameType localPlayerMode = GameType.DEFAULT_MODE;
    private  GameType previousLocalPlayerMode;
    private int carriedIndex;
    private ItemStack viaFabricPlus$oldCursorStack;
    private List<ItemStack> viaFabricPlus$oldItems;
    private final ClientPlayerInteractionManager1_18_2 viaFabricPlus$1_18_2InteractionManager = new ClientPlayerInteractionManager1_18_2();

    public MultiPlayerGameMode(Minecraft p_105203_, ClientPacketListener p_105204_) {
        this.minecraft = p_105203_;
        this.connection = p_105204_;
    }

    public void adjustPlayer(Player p_105222_) {
        this.localPlayerMode.updatePlayerAbilities(p_105222_.getAbilities());
    }

    public void setLocalMode(GameType p_171806_,  GameType p_171807_) {
        this.localPlayerMode = p_171806_;
        this.previousLocalPlayerMode = p_171807_;
        this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
    }

    public void setLocalMode(GameType p_105280_) {
        if (p_105280_ != this.localPlayerMode) {
            this.previousLocalPlayerMode = this.localPlayerMode;
        }

        this.localPlayerMode = p_105280_;
        this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
    }

    public boolean canHurtPlayer() {
        return this.localPlayerMode.isSurvival();
    }

    public boolean destroyBlock(BlockPos p_105268_) {
        if (this.minecraft.player.blockActionRestricted(this.minecraft.level, p_105268_, this.localPlayerMode)) {
            return false;
        }

        Level level = this.minecraft.level;
        BlockState blockstate = level.getBlockState(p_105268_);
        if (!this.minecraft.player.getMainHandItem().canDestroyBlock(blockstate, level, p_105268_, this.minecraft.player)) {
            return false;
        }

        Block block = blockstate.getBlock();
        if (block instanceof GameMasterBlock && !this.minecraft.player.canUseGameMasterBlocks()) {
            return false;
        }

        if (blockstate.isAir()) {
            return false;
        }

        block.playerWillDestroy(level, p_105268_, blockstate, this.minecraft.player);
        FluidState fluidstate = level.getFluidState(p_105268_);
        boolean flag = level.setBlock(p_105268_, fluidstate.createLegacyBlock(), 11);
        if (flag) {
            block.destroy(level, p_105268_, blockstate);
        }

        if (SharedConstants.DEBUG_BLOCK_BREAK) {
            LOGGER.error("client broke {} {} -> {}", p_105268_, blockstate, level.getBlockState(p_105268_));
        }

        // resetBlockBreaking
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_3)) {
            this.destroyBlockPos = new BlockPos(this.destroyBlockPos.getX(), -1, this.destroyBlockPos.getZ());
        }

        return flag;
    }

    public boolean startDestroyBlock(BlockPos p_105270_, Direction p_105271_) {
        if (this.minecraft.player.blockActionRestricted(this.minecraft.level, p_105270_, this.localPlayerMode)) {
            return false;
        }

        if (!this.minecraft.level.getWorldBorder().isWithinBounds(p_105270_)) {
            return false;
        }

        if (this.minecraft.player.getAbilities().instabuild) {
            BlockState blockstate = this.minecraft.level.getBlockState(p_105270_);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, p_105270_, blockstate, 1.0F);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
                LOGGER.info("Creative start {} {}", p_105270_, blockstate);
            }

            this.startPrediction(this.minecraft.level, p_233757_ -> {
                if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
                    if (!this.viaFabricPlus$extinguishFire(p_105270_, p_105271_)) {
                        this.destroyBlock(p_105270_);
                    }
                } else {
                    this.destroyBlock(p_105270_);
                }
                return this.viaFabricPlus$createPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, p_105270_, p_105271_, p_233757_);
            });
            this.destroyDelay = 5;
        } else if (!this.isDestroying || !this.sameDestroyTarget(p_105270_)) {
            if (this.isDestroying) {
                if (SharedConstants.DEBUG_BLOCK_BREAK) {
                    LOGGER.info("Abort old break {} {}", p_105270_, this.minecraft.level.getBlockState(p_105270_));
                }

                ServerboundPlayerActionPacket abortPacket = this.viaFabricPlus$createPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, p_105271_
                );
                this.connection.send(abortPacket);
            }

            BlockState blockstate1 = this.minecraft.level.getBlockState(p_105270_);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, p_105270_, blockstate1, 0.0F);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
                LOGGER.info("Start break {} {}", p_105270_, blockstate1);
            }

            this.startPrediction(this.minecraft.level, p_233728_ -> {
                boolean flag = !blockstate1.isAir();
                if (flag && this.destroyProgress == 0.0F) {
                    blockstate1.attack(this.minecraft.level, p_105270_, this.minecraft.player);
                }

                if (flag && blockstate1.getDestroyProgress(this.minecraft.player, this.minecraft.player.level(), p_105270_) >= 1.0F) {
                    // checkFireBlock
                    if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
                        if (!this.viaFabricPlus$extinguishFire(p_105270_, p_105271_)) {
                            this.destroyBlock(p_105270_);
                        }
                    } else {
                        this.destroyBlock(p_105270_);
                    }
                } else {
                    this.isDestroying = true;
                    this.destroyBlockPos = p_105270_;
                    this.destroyingItem = this.minecraft.player.getMainHandItem();
                    this.destroyProgress = 0.0F;
                    this.destroyTicks = 0.0F;
                    this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
                }

                return this.viaFabricPlus$createPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, p_105270_, p_105271_, p_233728_);
            });
        }

        return true;
    }
    private boolean viaFabricPlus$extinguishFire(BlockPos blockPos, final Direction direction) {
        blockPos = blockPos.relative(direction);
        if (this.minecraft.level.getBlockState(blockPos).getBlock() == Blocks.FIRE) {
            this.minecraft.level.levelEvent(this.minecraft.player, 1009, blockPos, 0);
            this.minecraft.level.removeBlock(blockPos, false);
            return true;
        }
        return false;
    }

    public void stopDestroyBlock() {
        // fixMiningReset1_7: для <= 1.7.6 всегда заходим в блок
        boolean shouldStop = ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_7_6) || this.isDestroying;

        if (shouldStop) {
            BlockState blockstate = this.minecraft.level.getBlockState(this.destroyBlockPos);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, this.destroyBlockPos, blockstate, -1.0F);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
                LOGGER.info("Stop dest {} {}", this.destroyBlockPos, blockstate);
            }

            // preventPacketWhenNotMining1_7
            if (ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_7_6) || this.isDestroying) {
                ServerboundPlayerActionPacket abortPacket = this.viaFabricPlus$createPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, Direction.DOWN
                );
                this.connection.send(abortPacket);
            }

            this.isDestroying = false;
            this.destroyProgress = 0.0F;
            this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, -1);

            // preventAttackResetWhenNotMining1_7
            if (ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_7_6) || this.isDestroying) {
                this.minecraft.player.resetAttackStrengthTicker();
            }
        }
    }

    // Создаёт пакет и трекает действие для <= 1.18.2
    private ServerboundPlayerActionPacket viaFabricPlus$createPlayerActionPacket(
            ServerboundPlayerActionPacket.Action action, BlockPos pos, Direction direction
    ) {
        if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_14_4, ProtocolVersion.v1_18_2)) {
            this.viaFabricPlus$1_18_2InteractionManager.trackPlayerAction(action, pos);
        }
        return new ServerboundPlayerActionPacket(action, pos, direction);
    }

    private ServerboundPlayerActionPacket viaFabricPlus$createPlayerActionPacket(
            ServerboundPlayerActionPacket.Action action, BlockPos pos, Direction direction, int sequence
    ) {
        if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_14_4, ProtocolVersion.v1_18_2)) {
            this.viaFabricPlus$1_18_2InteractionManager.trackPlayerAction(action, pos);
        }
        return new ServerboundPlayerActionPacket(action, pos, direction, sequence);
    }

    @Override
    public ClientPlayerInteractionManager1_18_2 viaFabricPlus$get1_18_2InteractionManager() {
        return this.viaFabricPlus$1_18_2InteractionManager;
    }

    public boolean continueDestroyBlock(BlockPos p_105284_, Direction p_105285_) {
        this.ensureHasSentCarriedItem();
        if (this.destroyDelay > 0) {
            this.destroyDelay--;
            return true;
        }

        if (this.minecraft.player.getAbilities().instabuild && this.minecraft.level.getWorldBorder().isWithinBounds(p_105284_)) {
            this.destroyDelay = 5;
            BlockState blockstate1 = this.minecraft.level.getBlockState(p_105284_);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, p_105284_, blockstate1, 1.0F);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
                LOGGER.info("Creative cont {} {}", p_105284_, blockstate1);
            }

            this.startPrediction(this.minecraft.level, p_233753_ -> {
                this.destroyBlock(p_105284_);
                return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, p_105284_, p_105285_, p_233753_);
            });
            return true;
        } else if (this.sameDestroyTarget(p_105284_)) {
            BlockState blockstate = this.minecraft.level.getBlockState(p_105284_);
            if (blockstate.isAir()) {
                this.isDestroying = false;
                return false;
            }

            this.destroyProgress = this.destroyProgress + blockstate.getDestroyProgress(this.minecraft.player, this.minecraft.player.level(), p_105284_);
            if (this.destroyTicks % 4.0F == 0.0F) {
                SoundType soundtype = blockstate.getSoundType();
                this.minecraft
                    .getSoundManager()
                    .play(
                        new SimpleSoundInstance(
                            soundtype.getHitSound(),
                            SoundSource.BLOCKS,
                            (soundtype.getVolume() + 1.0F) / 8.0F,
                            soundtype.getPitch() * 0.5F,
                            SoundInstance.createUnseededRandom(),
                            p_105284_
                        )
                    );
            }

            this.destroyTicks++;
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, p_105284_, blockstate, Mth.clamp(this.destroyProgress, 0.0F, 1.0F));
            if (this.destroyProgress >= 1.0F) {
                this.isDestroying = false;
                if (SharedConstants.DEBUG_BLOCK_BREAK) {
                    LOGGER.info("Finished breaking {} {}", p_105284_, blockstate);
                }

                this.startPrediction(this.minecraft.level, p_233739_ -> {
                    this.destroyBlock(p_105284_);
                    return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, p_105284_, p_105285_, p_233739_);
                });
                this.destroyProgress = 0.0F;
                this.destroyTicks = 0.0F;
                this.destroyDelay = 5;
            }

            this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
            return true;
        } else {
            return this.startDestroyBlock(p_105284_, p_105285_);
        }
    }

    private void startPrediction(ClientLevel p_233730_, PredictiveAction p_233731_) {
        try (BlockStatePredictionHandler blockstatepredictionhandler = p_233730_.getBlockStatePredictionHandler().startPredicting()) {
            int i = blockstatepredictionhandler.currentSequence();
            Packet<ServerGamePacketListener> packet = p_233731_.predict(i);

            // trackPlayerAction (Inject at HEAD of startPrediction)
            if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_14_4, ProtocolVersion.v1_18_2)
                    && packet instanceof ServerboundPlayerActionPacket playerActionPacket) {
                this.viaFabricPlus$1_18_2InteractionManager.trackPlayerAction(playerActionPacket.getAction(), playerActionPacket.getPos());
            }

            this.connection.send(packet);
        }
    }

    public void tick() {
        this.ensureHasSentCarriedItem();
        if (this.connection.getConnection().isConnected()) {
            this.connection.getConnection().tick();
        } else {
            this.connection.getConnection().handleDisconnection();
        }
    }

    private boolean sameDestroyTarget(BlockPos p_105282_) {
        ItemStack itemstack = this.minecraft.player.getMainHandItem();
        return p_105282_.equals(this.destroyBlockPos) && ItemStack.isSameItemSameComponents(itemstack, this.destroyingItem);
    }

    public void ensureHasSentCarriedItem() {
        int i = this.minecraft.player.getInventory().getSelectedSlot();
        if (i != this.carriedIndex) {
            this.carriedIndex = i;
            this.connection.send(new ServerboundSetCarriedItemPacket(this.carriedIndex));
        }
    }

    public InteractionResult useItemOn(LocalPlayer p_233733_, InteractionHand p_233734_, BlockHitResult p_233735_) {
        // cancelOffHandBlockPlace
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8) && !InteractionHand.MAIN_HAND.equals(p_233734_)) {
            return InteractionResult.PASS;
        }

        this.ensureHasSentCarriedItem();
        if (!this.minecraft.level.getWorldBorder().isWithinBounds(p_233735_.getBlockPos())) {
            return InteractionResult.FAIL;
        }

        MutableObject<InteractionResult> mutableobject = new MutableObject<>();

        // catchPacketCancelException
        try {
            this.startPrediction(this.minecraft.level, p_233745_ -> {
                // lambdauseItemOn4 / trackLastUsedItem for useItemOn
                if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
                    ViaFabricPlusHandItemProvider.lastUsedItem = p_233733_.getItemInHand(p_233734_).copy();
                }
                try {
                    mutableobject.setValue(this.performUseItemOn(p_233733_, p_233734_, p_233735_));
                    return new ServerboundUseItemOnPacket(p_233734_, p_233735_, p_233745_);
                } catch (ActionResultException1_12_2 e) {
                    mutableobject.setValue(e.getActionResult());
                    throw e;
                }
            });
        } catch (ActionResultException1_12_2 ignored) {
        }

        return mutableobject.get();
    }

    private InteractionResult performUseItemOn(LocalPlayer p_233747_, InteractionHand p_233748_, BlockHitResult p_233749_) {
        BlockPos blockpos = p_233749_.getBlockPos();
        ItemStack itemstack = p_233747_.getItemInHand(p_233748_);

        // changeSpectatorAction
        if (this.localPlayerMode == GameType.SPECTATOR) {
            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_21)) {
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.CONSUME;
            }
        }

        boolean flag = !p_233747_.getMainHandItem().isEmpty() || !p_233747_.getOffhandItem().isEmpty();
        boolean flag1 = p_233747_.isSecondaryUseActive() && flag;
        if (!flag1) {
            BlockState blockstate = this.minecraft.level.getBlockState(blockpos);
            if (!this.connection.isFeatureEnabled(blockstate.getBlock().requiredFeatures())) {
                return InteractionResult.FAIL;
            }

            InteractionResult interactionresult = blockstate.useItemOn(p_233747_.getItemInHand(p_233748_), this.minecraft.level, p_233747_, p_233748_, p_233749_);
            if (interactionresult.consumesAction()) {
                return interactionresult;
            }

            if (interactionresult instanceof InteractionResult.TryEmptyHandInteraction && p_233748_ == InteractionHand.MAIN_HAND) {
                InteractionResult interactionresult1 = blockstate.useWithoutItem(this.minecraft.level, p_233747_, p_233749_);
                if (interactionresult1.consumesAction()) {
                    return interactionresult1;
                }
            }
        }

        // interactBlock1_12_2
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            BlockHitResult checkHitResult = p_233749_;
            if (itemstack.getItem() instanceof BlockItem) {
                final BlockState clickedBlock = this.minecraft.level.getBlockState(p_233749_.getBlockPos());
                if (clickedBlock.getBlock().equals(Blocks.SNOW)) {
                    if (clickedBlock.getValue(SnowLayerBlock.LAYERS) == 1) {
                        checkHitResult = p_233749_.withDirection(Direction.UP);
                    }
                }
                final UseOnContext itemUsageContext = new UseOnContext(p_233747_, p_233748_, checkHitResult);
                final BlockPlaceContext itemPlacementContext = new BlockPlaceContext(itemUsageContext);
                if (!itemPlacementContext.canPlace() || ((BlockItem) itemPlacementContext.getItemInHand().getItem()).getPlacementState(itemPlacementContext) == null) {
                    throw new ActionResultException1_12_2(InteractionResult.PASS);
                }
            }

            this.connection.send(new ServerboundUseItemOnPacket(p_233748_, p_233749_, 0));
            if (itemstack.isEmpty()) {
                throw new ActionResultException1_12_2(InteractionResult.PASS);
            }
            final UseOnContext itemUsageContext = new UseOnContext(p_233747_, p_233748_, checkHitResult);
            InteractionResult actionResult;
            if (this.localPlayerMode.isCreative()) {
                final int count = itemstack.getCount();
                actionResult = itemstack.useOn(itemUsageContext);
                itemstack.setCount(count);
            } else {
                actionResult = itemstack.useOn(itemUsageContext);
            }
            if (!actionResult.consumesAction()) {
                actionResult = InteractionResult.PASS;
            }
            throw new ActionResultException1_12_2(actionResult);
        }

        if (!itemstack.isEmpty() && !p_233747_.getCooldowns().isOnCooldown(itemstack)) {
            UseOnContext useoncontext = new UseOnContext(p_233747_, p_233748_, p_233749_);
            InteractionResult interactionresult2;
            if (p_233747_.hasInfiniteMaterials()) {
                int i = itemstack.getCount();
                interactionresult2 = itemstack.useOn(useoncontext);
                itemstack.setCount(i);
            } else {
                interactionresult2 = itemstack.useOn(useoncontext);
            }

            return interactionresult2;
        } else {
            return InteractionResult.PASS;
        }
    }

    public InteractionResult useItem(Player p_233722_, InteractionHand p_233723_) {
        // cancelOffHandItemInteract
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8) && !InteractionHand.MAIN_HAND.equals(p_233723_)) {
            return InteractionResult.PASS;
        }

        if (this.localPlayerMode == GameType.SPECTATOR) {
            return InteractionResult.PASS;
        }

        this.ensureHasSentCarriedItem();

        // sendPlayerPosPacket
        if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_17, ProtocolVersion.v1_20_5)) {
            this.connection.send(new ServerboundMovePlayerPacket.PosRot(p_233722_.getX(), p_233722_.getY(), p_233722_.getZ(), p_233722_.getYRot(), p_233722_.getXRot(), p_233722_.onGround(), p_233722_.horizontalCollision));
        }

        MutableObject<InteractionResult> mutableobject = new MutableObject<>();

        // fixPacketOrder
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_18_2)) {
            ServerboundUseItemPacket serverbounduseitempacket = new ServerboundUseItemPacket(p_233723_, 0, p_233722_.getYRot(), p_233722_.getXRot());
            ItemStack itemstack = p_233722_.getItemInHand(p_233723_);

            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
                ViaFabricPlusHandItemProvider.lastUsedItem = itemstack.copy();
            }

            if (p_233722_.getCooldowns().isOnCooldown(itemstack)) {
                mutableobject.setValue(InteractionResult.PASS);
            } else {
                final int count = itemstack.getCount();
                InteractionResult interactionresult = itemstack.use(this.minecraft.level, p_233722_, p_233723_);

                // eitherSuccessOrPass
                if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
                    ItemStack output;
                    if (interactionresult instanceof InteractionResult.Success success) {
                        output = Objects.requireNonNullElseGet(success.heldItemTransformedTo(), () -> p_233722_.getItemInHand(p_233723_));
                    } else {
                        output = p_233722_.getItemInHand(p_233723_);
                    }
                    final boolean accepted = !output.isEmpty() && (output != itemstack || output.getCount() != count);
                    if (interactionresult.consumesAction() != accepted) {
                        interactionresult = accepted ? InteractionResult.SUCCESS.heldItemTransformedTo(output) : InteractionResult.PASS;
                    }
                }

                ItemStack itemstack1;
                if (interactionresult instanceof InteractionResult.Success interactionresult$success) {
                    itemstack1 = Objects.requireNonNullElseGet(interactionresult$success.heldItemTransformedTo(), () -> p_233722_.getItemInHand(p_233723_));
                } else {
                    itemstack1 = p_233722_.getItemInHand(p_233723_);
                }

                if (itemstack1 != itemstack) {
                    p_233722_.setItemInHand(p_233723_, itemstack1);
                }

                mutableobject.setValue(interactionresult);
            }

            this.connection.send(serverbounduseitempacket);
            return mutableobject.get();
        }

        this.startPrediction(
                this.minecraft.level,
                p_357795_ -> {
                    ServerboundUseItemPacket serverbounduseitempacket = new ServerboundUseItemPacket(
                            p_233723_, p_357795_, p_233722_.getYRot(), p_233722_.getXRot()
                    );

                    if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
                        ViaFabricPlusHandItemProvider.lastUsedItem = p_233722_.getItemInHand(p_233723_).copy();
                    }

                    ItemStack itemstack = p_233722_.getItemInHand(p_233723_);
                    if (p_233722_.getCooldowns().isOnCooldown(itemstack)) {
                        mutableobject.setValue(InteractionResult.PASS);
                        return serverbounduseitempacket;
                    }

                    final int count = itemstack.getCount();
                    InteractionResult interactionresult = itemstack.use(this.minecraft.level, p_233722_, p_233723_);

                    // eitherSuccessOrPass
                    if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
                        ItemStack output;
                        if (interactionresult instanceof InteractionResult.Success success) {
                            output = Objects.requireNonNullElseGet(success.heldItemTransformedTo(), () -> p_233722_.getItemInHand(p_233723_));
                        } else {
                            output = p_233722_.getItemInHand(p_233723_);
                        }
                        final boolean accepted = !output.isEmpty() && (output != itemstack || output.getCount() != count);
                        if (interactionresult.consumesAction() != accepted) {
                            interactionresult = accepted ? InteractionResult.SUCCESS.heldItemTransformedTo(output) : InteractionResult.PASS;
                        }
                    }

                    ItemStack itemstack1;
                    if (interactionresult instanceof InteractionResult.Success interactionresult$success) {
                        itemstack1 = Objects.requireNonNullElseGet(interactionresult$success.heldItemTransformedTo(), () -> p_233722_.getItemInHand(p_233723_));
                    } else {
                        itemstack1 = p_233722_.getItemInHand(p_233723_);
                    }

                    if (itemstack1 != itemstack) {
                        p_233722_.setItemInHand(p_233723_, itemstack1);
                    }

                    mutableobject.setValue(interactionresult);
                    return serverbounduseitempacket;
                }
        );
        return mutableobject.get();
    }

    public LocalPlayer createPlayer(ClientLevel p_105247_, StatsCounter p_105248_, ClientRecipeBook p_105249_) {
        return this.createPlayer(p_105247_, p_105248_, p_105249_, Input.EMPTY, false);
    }

    public LocalPlayer createPlayer(ClientLevel p_105251_, StatsCounter p_105252_, ClientRecipeBook p_105253_, Input p_409444_, boolean p_105254_) {
        return new LocalPlayer(this.minecraft, p_105251_, this.connection, p_105252_, p_105253_, p_409444_, p_105254_);
    }

    public void attack(Player p_105224_, Entity p_105225_) {
        EventAttack event = new EventAttack(p_105225_);
        EventRepo.call(event);

        if (event.isCancelled()) {
            return;
        }

        this.ensureHasSentCarriedItem();
        this.connection.send(ServerboundInteractPacket.createAttackPacket(p_105225_, p_105224_.isShiftKeyDown()));
        MessageSender.sendOverlayMessage(Component.literal(minecraft.player.attackStrengthTicker + "")); //debug
        if (this.localPlayerMode != GameType.SPECTATOR) {
            p_105224_.attack(p_105225_);
            p_105224_.resetAttackStrengthTicker();
        }
    }

    public InteractionResult interact(Player p_105227_, Entity p_105228_, InteractionHand p_105229_) {
        this.ensureHasSentCarriedItem();
        this.connection.send(ServerboundInteractPacket.createInteractionPacket(p_105228_, p_105227_.isShiftKeyDown(), p_105229_));
        return this.localPlayerMode == GameType.SPECTATOR ? InteractionResult.PASS : p_105227_.interactOn(p_105228_, p_105229_);
    }

    public InteractionResult interactAt(Player p_105231_, Entity p_105232_, EntityHitResult p_105233_, InteractionHand p_105234_) {
        this.ensureHasSentCarriedItem();
        Vec3 vec3 = p_105233_.getLocation().subtract(p_105232_.getX(), p_105232_.getY(), p_105232_.getZ());
        this.connection.send(ServerboundInteractPacket.createInteractionPacket(p_105232_, p_105231_.isShiftKeyDown(), p_105234_, vec3));
        return this.localPlayerMode == GameType.SPECTATOR ? InteractionResult.PASS : p_105232_.interactAt(p_105231_, vec3, p_105234_);
    }

    public void handleInventoryMouseClick(int p_171800_, int p_171801_, int p_171802_, ClickType p_171803_, Player p_171804_) {
        // removeClickActions
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_5tob1_5_2) && !p_171803_.equals(ClickType.PICKUP)) {
            return;
        } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_4_6tor1_4_7)
                && !p_171803_.equals(ClickType.PICKUP)
                && !p_171803_.equals(ClickType.QUICK_MOVE)
                && !p_171803_.equals(ClickType.SWAP)
                && !p_171803_.equals(ClickType.CLONE)) {
            return;
        }
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)
                && p_171803_ == ClickType.SWAP
                && p_171802_ == 40) {
            return;
        }

        AbstractContainerMenu abstractcontainermenu = p_171804_.containerMenu;
        if (p_171800_ != abstractcontainermenu.containerId) {
            LOGGER.warn("Ignoring click in mismatching container. Click in {}, player has {}.", p_171800_, abstractcontainermenu.containerId);
            return;
        }

        NonNullList<Slot> nonnulllist = abstractcontainermenu.slots;
        int i = nonnulllist.size();
        List<ItemStack> list = Lists.newArrayListWithCapacity(i);

        for (Slot slot : nonnulllist) {
            list.add(slot.getItem().copy());
        }

        // captureOldItems
        this.viaFabricPlus$oldCursorStack = this.minecraft.player.containerMenu.getCarried().copy();
        this.viaFabricPlus$oldItems = list;

        abstractcontainermenu.clicked(p_171801_, p_171802_, p_171803_, p_171804_);
        Int2ObjectMap<HashedStack> int2objectmap = new Int2ObjectOpenHashMap<>();

        for (int j = 0; j < i; j++) {
            ItemStack itemstack = list.get(j);
            ItemStack itemstack1 = nonnulllist.get(j).getItem();
            if (!ItemStack.matches(itemstack, itemstack1)) {
                int2objectmap.put(j, HashedStack.create(itemstack1, this.connection.decoratedHashOpsGenenerator()));
            }
        }

        HashedStack hashedstack = HashedStack.create(abstractcontainermenu.getCarried(), this.connection.decoratedHashOpsGenenerator());
        ServerboundContainerClickPacket clickSlotPacket = new ServerboundContainerClickPacket(
                p_171800_,
                abstractcontainermenu.getStateId(),
                Shorts.checkedCast(p_171801_),
                SignedBytes.checkedCast(p_171802_),
                p_171803_,
                int2objectmap,
                hashedstack
        );

        // handleWindowClick
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4)) {
            this.viaFabricPlus$clickSlot1_16_5(clickSlotPacket);
        } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_21_4)) {
            this.viaFabricPlus$clickSlot1_21_4(clickSlotPacket);
        } else {
            this.connection.send(clickSlotPacket);
        }
    }
    private void viaFabricPlus$clickSlot1_21_4(final ServerboundContainerClickPacket packet) {
        final PacketWrapper containerClick = PacketWrapper.create(ServerboundPackets1_21_4.CONTAINER_CLICK, ProtocolTranslator.getPlayNetworkUserConnection());
        containerClick.write(Types.VAR_INT, packet.containerId());
        containerClick.write(Types.VAR_INT, packet.stateId());
        containerClick.write(Types.SHORT, packet.slotNum());
        containerClick.write(Types.BYTE, packet.buttonNum());
        containerClick.write(Types.VAR_INT, packet.clickType().id());

        final Int2ObjectMap<HashedStack> modifiedStacks = packet.changedSlots();
        containerClick.write(Types.VAR_INT, modifiedStacks.size());
        for (Int2ObjectMap.Entry<HashedStack> entry : modifiedStacks.int2ObjectEntrySet()) {
            final ItemStack itemStack = minecraft.player.containerMenu.slots.get(entry.getIntKey()).getItem();
            containerClick.write(Types.SHORT, (short) entry.getIntKey());
            containerClick.write(VersionedTypes.V1_21_4.item, ItemTranslator.mcToVia(itemStack, ProtocolVersion.v1_21_4));
        }

        final ItemStack cursorStack = minecraft.player.containerMenu.getCarried();
        containerClick.write(VersionedTypes.V1_21_4.item, ItemTranslator.mcToVia(cursorStack, ProtocolVersion.v1_21_4));
        containerClick.scheduleSendToServer(Protocol1_21_4To1_21_5.class);
    }

    private void viaFabricPlus$clickSlot1_16_5(final ServerboundContainerClickPacket packet) {
        ItemStack slotItemBeforeModification;
        if (this.viaFabricPlus$shouldBeEmpty(packet.clickType(), packet.slotNum())) {
            slotItemBeforeModification = ItemStack.EMPTY;
        } else if (packet.slotNum() < 0 || packet.slotNum() >= viaFabricPlus$oldItems.size()) {
            slotItemBeforeModification = viaFabricPlus$oldCursorStack;
        } else {
            slotItemBeforeModification = viaFabricPlus$oldItems.get(packet.slotNum());
        }

        final PacketWrapper containerClick = PacketWrapper.create(ServerboundPackets1_16_2.CONTAINER_CLICK, ProtocolTranslator.getPlayNetworkUserConnection());
        containerClick.write(Types.BYTE, (byte) packet.containerId());
        containerClick.write(Types.SHORT, packet.slotNum());
        containerClick.write(Types.BYTE, packet.buttonNum());
        containerClick.write(Types.SHORT, minecraft.player.containerMenu.viaFabricPlus$incrementAndGetActionId());
        containerClick.write(Types.VAR_INT, packet.clickType().ordinal());
        containerClick.write(Types.ITEM1_13_2, ItemTranslator.mcToVia(slotItemBeforeModification, ProtocolVersion.v1_16_4));
        containerClick.scheduleSendToServer(Protocol1_16_4To1_17.class);

        viaFabricPlus$oldCursorStack = null;
        viaFabricPlus$oldItems = null;
    }

    private boolean viaFabricPlus$shouldBeEmpty(final ClickType type, final int slot) {
        // quick craft always uses empty stack for verification
        if (type == ClickType.QUICK_CRAFT) return true;

        // Special case: throw always uses empty stack for verification
        if (type == ClickType.THROW) return true;

        // quick move always uses empty stack for verification since 1.12
        if (type == ClickType.QUICK_MOVE && ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_11_1))
            return true;

        // pickup with slot -999 (outside window) to throw items always uses empty stack for verification
        return type == ClickType.PICKUP && slot == -999;
    }

    public void handlePlaceRecipe(int p_105218_, RecipeDisplayId p_365843_, boolean p_105220_) {
        this.connection.send(new ServerboundPlaceRecipePacket(p_105218_, p_365843_, p_105220_));
    }

    public void handleInventoryButtonClick(int p_105209_, int p_105210_) {
        this.connection.send(new ServerboundContainerButtonClickPacket(p_105209_, p_105210_));
    }

    public void handleCreativeModeItemAdd(ItemStack p_105242_, int p_105243_) {
        if (this.minecraft.player.hasInfiniteMaterials() && this.connection.isFeatureEnabled(p_105242_.getItem().requiredFeatures())) {
            this.connection.send(new ServerboundSetCreativeModeSlotPacket(p_105243_, p_105242_));
        }
    }

    public void handleCreativeModeItemDrop(ItemStack p_105240_) {
        boolean flag = this.minecraft.screen instanceof AbstractContainerScreen && !(this.minecraft.screen instanceof CreativeModeInventoryScreen);
        if (this.minecraft.player.hasInfiniteMaterials() && !flag && !p_105240_.isEmpty() && this.connection.isFeatureEnabled(p_105240_.getItem().requiredFeatures())) {
            this.connection.send(new ServerboundSetCreativeModeSlotPacket(-1, p_105240_));
            this.minecraft.player.getDropSpamThrottler().increment();
        }
    }

    public void releaseUsingItem(Player p_105278_) {
        this.ensureHasSentCarriedItem();
        this.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
        p_105278_.releaseUsingItem();
    }

    public void piercingAttack(PiercingWeapon p_458427_) {
        this.ensureHasSentCarriedItem();
        this.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STAB, BlockPos.ZERO, Direction.DOWN));
        this.minecraft.player.onAttack();
        this.minecraft.player.lungeForwardMaybe();
        p_458427_.makeSound(this.minecraft.player);
    }

    public boolean hasExperience() {
        return this.localPlayerMode.isSurvival();
    }

    public boolean hasMissTime() {
        return !this.localPlayerMode.isCreative();
    }

    public boolean isServerControlledInventory() {
        return this.minecraft.player.isPassenger() && this.minecraft.player.getVehicle() instanceof HasCustomInventoryScreen;
    }

    public boolean isSpectator() {
        return this.localPlayerMode == GameType.SPECTATOR;
    }

    public  GameType getPreviousPlayerMode() {
        return this.previousLocalPlayerMode;
    }

    public GameType getPlayerMode() {
        return this.localPlayerMode;
    }

    public boolean isDestroying() {
        return this.isDestroying;
    }

    public int getDestroyStage() {
        // changeCalculation
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_4)) {
            return (int)(this.destroyProgress * 10.0F) - 1;
        }
        return this.destroyProgress > 0.0F ? (int)(this.destroyProgress * 10.0F) : -1;
    }

    public void handlePickItemFromBlock(BlockPos p_376249_, boolean p_375534_) {
        this.connection.send(new ServerboundPickItemFromBlockPacket(p_376249_, p_375534_));
    }

    public void handlePickItemFromEntity(Entity p_378506_, boolean p_375481_) {
        this.connection.send(new ServerboundPickItemFromEntityPacket(p_378506_.getId(), p_375481_));
    }

    public void handleSlotStateChanged(int p_312970_, int p_309738_, boolean p_310073_) {
        this.connection.send(new ServerboundContainerSlotStateChangedPacket(p_312970_, p_309738_, p_310073_));
    }

    @Override
    public void setIsHittingBlock(boolean isHittingBlock) {
        this.isDestroying = isHittingBlock;
    }

    @Override
    public boolean isHittingBlock() {
        return this.isDestroying;
    }

    @Override
    public BlockPos getCurrentBlock() {
        return this.destroyBlockPos;
    }

    @Override
    public void callSyncCurrentPlayItem() {
        this.ensureHasSentCarriedItem();
    }

    @Override
    public void setDestroyDelay(int destroyDelay) {
        this.destroyDelay = destroyDelay;
    }
}
