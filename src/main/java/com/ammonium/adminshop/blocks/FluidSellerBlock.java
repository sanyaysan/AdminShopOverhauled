package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.blocks.entity.FluidSellerBE;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.screen.FluidSellerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FluidSellerBlock extends BaseEntityBlock {
    public FluidSellerBlock() {
        super(Properties.of(ModBlocks.machineBlock)
                .sound(SoundType.METAL)
                .strength(1.0f)
                .lightLevel(state -> 0)
                .dynamicShape()
                .noOcclusion()
        );
    }

//    private static final VoxelShape RENDER_SHAPE = Shapes.block();
    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof FluidSellerBE fSellerEntity) {
                fSellerEntity.setRemoved();
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos,
                                 Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            if(pLevel.getBlockEntity(pPos) instanceof FluidSellerBE fSellerEntity) {

                if (Objects.equals(fSellerEntity.getOwnerUUID(), pPlayer.getStringUUID())) {
                    // Open menu
                    NetworkHooks.openScreen((ServerPlayer) pPlayer, fSellerEntity, pPos);
                } else {
                    // Wrong user
                    pPlayer.sendSystemMessage(Component.literal("You are not this machine's owner!"));
                }

            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

//    @Override
//    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
//        return RENDER_SHAPE;
//    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
        return new SimpleMenuProvider((id, playerInventory, player) -> new FluidSellerMenu(id, playerInventory, pLevel, pPos), Component.translatable("screen.adminshop.seller"));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new FluidSellerBE(pPos, pState);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        if (!pLevel.isClientSide) {
            // Server side code
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            // Set initial values
            if (pPlacer instanceof ServerPlayer serverPlayer && blockEntity instanceof FluidSellerBE fSellerEntity) {
                MoneyManager moneyManager = MoneyManager.get(pLevel);
                Pair<String, Integer> defaultAccount = moneyManager.getDefaultAccount(serverPlayer.getStringUUID());
                fSellerEntity.setOwnerUUID(serverPlayer.getStringUUID());
                fSellerEntity.setAccount(defaultAccount);
                fSellerEntity.setChanged();
                fSellerEntity.sendUpdates();
            }
        }
    }

//    @Override
//    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
//        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
//    }
//
//    @Override
//    public BlockState rotate(BlockState pState, Rotation pRotation) {
//        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
//    }
//
//    @Override
//    public BlockState mirror(BlockState pState, Mirror pMirror) {
//        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
//    }
//
//    @Override
//    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
//        builder.add(FACING);
//    }


    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ? null : checkType(pBlockEntityType, ModBlockEntities.FLUID_SELLER.get(),
                (level, pos, state, blockEntity) -> FluidSellerBE.tick(level, pos, state, (FluidSellerBE) blockEntity));
    }

    private static <T extends BlockEntity> BlockEntityTicker<T> checkType(BlockEntityType<T> blockEntityType, BlockEntityType<?> expectedType, BlockEntityTicker<? super T> ticker) {
        return blockEntityType == expectedType ? (BlockEntityTicker<T>) ticker : null;
    }
}
