package net.minecraft.world.item.context;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.protocoltranslator.impl.ViaFabricPlusMappingDataLoader;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockPlaceContext extends UseOnContext {
    private final BlockPos relativePos;
    protected boolean replaceClicked = true;

    public BlockPlaceContext(Player p_43631_, InteractionHand p_43632_, ItemStack p_43633_, BlockHitResult p_43634_) {
        this(p_43631_.level(), p_43631_, p_43632_, p_43633_, p_43634_);
    }

    public BlockPlaceContext(UseOnContext p_43636_) {
        this(p_43636_.getLevel(), p_43636_.getPlayer(), p_43636_.getHand(), p_43636_.getItemInHand(), p_43636_.getHitResult());
    }

    protected BlockPlaceContext(Level p_43638_, @Nullable Player p_43639_, InteractionHand p_43640_, ItemStack p_43641_, BlockHitResult p_43642_) {
        super(p_43638_, p_43639_, p_43640_, p_43641_, p_43642_);
        this.relativePos = p_43642_.getBlockPos().relative(p_43642_.getDirection());
        this.replaceClicked = p_43638_.getBlockState(p_43642_.getBlockPos()).canBeReplaced(this);
    }

    public static BlockPlaceContext at(BlockPlaceContext p_43645_, BlockPos p_43646_, Direction p_43647_) {
        return new BlockPlaceContext(
            p_43645_.getLevel(),
            p_43645_.getPlayer(),
            p_43645_.getHand(),
            p_43645_.getItemInHand(),
            new BlockHitResult(
                new Vec3(
                    p_43646_.getX() + 0.5 + p_43647_.getStepX() * 0.5,
                    p_43646_.getY() + 0.5 + p_43647_.getStepY() * 0.5,
                    p_43646_.getZ() + 0.5 + p_43647_.getStepZ() * 0.5
                ),
                p_43647_,
                p_43646_,
                false
            )
        );
    }

    @Override
    public BlockPos getClickedPos() {
        return this.replaceClicked ? super.getClickedPos() : this.relativePos;
    }

    public boolean canPlace() {
        boolean canPlace = this.replaceClicked || this.getLevel().getBlockState(this.getClickedPos()).canBeReplaced(this);

        if (!canPlace && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            return ViaFabricPlusMappingDataLoader.getBlockMaterial(this.getLevel().getBlockState(this.getClickedPos()).getBlock()).equals("decoration")
                    && Block.byItem(this.getItemInHand().getItem()).equals(Blocks.ANVIL);
        }

        return canPlace;
    }

    public boolean replacingClickedOnBlock() {
        return this.replaceClicked;
    }

    public Direction getNearestLookingDirection() {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            final Player player = this.getPlayer();
            final BlockPos placementPos = this.getClickedPos();
            final double blockPosCenterFactor = ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_10) ? 0.5 : 0;

            if (Math.abs(player.getX() - (placementPos.getX() + blockPosCenterFactor)) < 2
                    && Math.abs(player.getZ() - (placementPos.getZ() + blockPosCenterFactor)) < 2) {
                final double eyeY = player.getY() + player.getEyeHeight(player.getPose());

                if (eyeY - placementPos.getY() > 2) {
                    return Direction.DOWN;
                }

                if (placementPos.getY() - eyeY > 0) {
                    return Direction.UP;
                }
            }

            return player.getDirection();
        }

        return Direction.orderedByNearest(this.getPlayer())[0];
    }

    public Direction getNearestLookingVerticalDirection() {
        return Direction.getFacingAxis(this.getPlayer(), Direction.Axis.Y);
    }

    public Direction[] getNearestLookingDirections() {
        Direction[] adirection = Direction.orderedByNearest(this.getPlayer());
        if (this.replaceClicked) {
            return adirection;
        }

        Direction direction = this.getClickedFace();
        int i = 0;

        while (i < adirection.length && adirection[i] != direction.getOpposite()) {
            i++;
        }

        if (i > 0) {
            System.arraycopy(adirection, 0, adirection, 1, i);
            adirection[0] = direction.getOpposite();
        }

        return adirection;
    }
}
