package svenhjol.charm.feature.atlases;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import svenhjol.charm.Charm;
import svenhjol.charm.mixin.atlases.CartographyTableScreenMixin;
import svenhjol.charmony.base.Mods;
import svenhjol.charmony.client.ClientFeature;
import svenhjol.charmony.common.CommonFeature;
import svenhjol.charmony_api.event.HeldItemRenderEvent;
import svenhjol.charmony_api.event.KeyPressEvent;

import java.util.function.Supplier;

public class AtlasesClient extends ClientFeature {
    static final RenderType ATLAS_BACKGROUND = RenderType.text(new ResourceLocation(Charm.ID, "textures/map/atlas.png"));
    static final RenderType MAP_BACKGROUND = RenderType.text(new ResourceLocation("textures/map/map_background.png"));
    static final RenderType MAP_DECORATIONS = RenderType.text(new ResourceLocation("textures/map/map_icons.png"));
    static final ResourceLocation CONTAINER_BACKGROUND = new ResourceLocation(Charm.ID, "textures/gui/atlas.png");
    static final WidgetSprites UP_BUTTON = makeButton("up");
    static final WidgetSprites DOWN_BUTTON = makeButton("down");
    static final WidgetSprites LEFT_BUTTON = makeButton("left");
    static final WidgetSprites RIGHT_BUTTON = makeButton("right");
    static final WidgetSprites BACK_BUTTON = makeButton("back");
    static final WidgetSprites ZOOM_IN_BUTTON = makeButton("zoom_in");
    static final WidgetSprites ZOOM_OUT_BUTTON = makeButton("zoom_out");
    private AtlasRenderer renderer;
    private static int swappedSlot = -1;
    public static Supplier<String> OPEN_ATLAS_KEY;

    @Override
    public Class<? extends CommonFeature> commonFeature() {
        return Atlases.class;
    }

    @Override
    public void register() {
        var registry = mod().registry();
        registry.menuScreen(Atlases.MENU_TYPE, () -> AtlasScreen::new);

        OPEN_ATLAS_KEY = registry.key("open_atlas",
            () -> new KeyMapping("key.charm.open_atlas", GLFW.GLFW_KEY_R, "key.categories.inventory"));

        if (isEnabled()) {
            registry.itemTab(
                Atlases.ITEM,
                CreativeModeTabs.TOOLS_AND_UTILITIES,
                Items.MAP
            );
        }
    }

    @Override
    public void runWhenEnabled() {
        KeyPressEvent.INSTANCE.handle(this::handleKeyPress);
        HeldItemRenderEvent.INSTANCE.handle(this::handleRenderHeldItem);
    }

    private void handleKeyPress(String id) {
        if (Minecraft.getInstance().level != null && id.equals(OPEN_ATLAS_KEY.get())) {
            AtlasesNetwork.SwapAtlasSlot.send(swappedSlot);
        }
    }

    private InteractionResult handleRenderHeldItem(float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack itemStack, float equipProgress, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {
        if (itemStack.getItem() == Atlases.ITEM.get()) {
            if (renderer == null) {
                renderer = new AtlasRenderer();
            }
            renderer.renderAtlas(poseStack, multiBufferSource, light, hand, equipProgress, swingProgress, itemStack);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public static void handleUpdateInventory(AtlasesNetwork.UpdateInventory request, Player player) {
        var slot = request.getSlot();
        ItemStack atlas = player.getInventory().getItem(slot);
        AtlasInventory.get(player.level(), atlas).reload(atlas);
    }

    /**
     * Callback from {@link CartographyTableScreenMixin} to check
     * if the cartography table contains a map or an atlas.
     * @param screen The cartography table screen.
     * @return True if the cartography table contains a map or an atlas.
     */
    public static boolean shouldDrawAtlasCopy(CartographyTableScreen screen) {
        return screen.getMenu().getSlot(0).getItem().getItem() == Atlases.ITEM.get()
            && screen.getMenu().getSlot(1).getItem().getItem() == Items.MAP;
    }

    @SuppressWarnings("unused")
    public static void handleSwappedSlot(AtlasesNetwork.SwappedAtlasSlot packet, Player player) {
        swappedSlot = packet.getSlot();
    }

    private static WidgetSprites makeButton(String name) {
        var instance = Mods.client(Charm.ID);

        return new WidgetSprites(
            instance.id("widget/atlases/" + name + "_button"),
            instance.id("widget/atlases/" + name + "_button_disabled"),
            instance.id("widget/atlases/" + name + "_button_highlighted"));
    }
}
