package svenhjol.charm.feature.bat_buckets;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import svenhjol.charm.Charm;
import svenhjol.charmony.common.CommonFeature;
import svenhjol.charmony.feature.advancements.Advancements;
import svenhjol.charmony.helper.ItemNbtHelper;
import svenhjol.charmony_api.CharmonyApi;
import svenhjol.charmony_api.event.EntityUseEvent;
import svenhjol.charmony_api.iface.IWandererTrade;
import svenhjol.charmony_api.iface.IWandererTradeProvider;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class BatBuckets extends CommonFeature implements IWandererTradeProvider {
    static Supplier<BatBucketItem> bucketItem;
    private static Supplier<SoundEvent> grabSound;
    private static Supplier<SoundEvent> releaseSound;
    static final int GLOW_TIME = 10; // In seconds.

    @Override
    public String description() {
        return """
            Right-click a bat with a bucket to capture it.
            Right-click again to release it and locate entities around you.""";
    }

    @Override
    public void register() {
        var registry = mod().registry();

        bucketItem = registry.item("bat_bucket", () -> new BatBucketItem(this));
        grabSound = registry.soundEvent("bat_bucket_grab");
        releaseSound = registry.soundEvent("bat_bucket_release");

        CharmonyApi.registerProvider(this);
    }

    @Override
    public void runWhenEnabled() {
        EntityUseEvent.INSTANCE.handle(this::handleEntityUse);
    }

    static void playGrabSound(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, grabSound.get(), SoundSource.PLAYERS, 0.6F, 0.95F + level.getRandom().nextFloat() * 0.2F);
    }

    static void playReleaseSound(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, releaseSound.get(), SoundSource.PLAYERS, 0.6F, 0.95F + level.getRandom().nextFloat() * 0.2F);
    }

    static void playLaunchSound(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, 0.25F, 1F);
    }

    private InteractionResult handleEntityUse(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
        if (!entity.level().isClientSide()
            && entity instanceof Bat bat
            && bat.getHealth() > 0
        ) {
            var held = player.getItemInHand(hand);

            if (held.isEmpty() || held.getItem() != Items.BUCKET) {
                return InteractionResult.PASS;
            }

            var batBucket = new ItemStack(bucketItem.get());
            var tag = new CompoundTag();
            ItemNbtHelper.setCompound(batBucket, BatBucketItem.STORED_BAT_TAG, bat.saveWithoutId(tag));

            if (held.getCount() == 1) {
                player.setItemInHand(hand, batBucket);
            } else {
                held.shrink(1);
                player.getInventory().placeItemBackInInventory(batBucket);
            }

            playGrabSound((ServerLevel)bat.level(), bat.blockPosition());
            player.getCooldowns().addCooldown(bucketItem.get(), 30);
            player.swing(hand);
            entity.discard();

            triggerCapturedBat(player);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public List<IWandererTrade> getWandererTrades() {
        return List.of(new IWandererTrade() {
            @Override
            public ItemLike getItem() {
                return bucketItem.get();
            }

            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public int getCost() {
                return 8;
            }
        });
    }

    public static void triggerCapturedBat(Player player) {
        Advancements.trigger(new ResourceLocation(Charm.ID, "captured_bat"), player);
    }

    public static void triggerUsedBatBucket(Player player) {
        Advancements.trigger(new ResourceLocation(Charm.ID, "used_bat_bucket"), player);
    }
}
