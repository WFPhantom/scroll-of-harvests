package se.mickelus.harvests.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import se.mickelus.harvests.Config;
import se.mickelus.harvests.HarvestsMod;
import se.mickelus.harvests.api.TierFilter;
import se.mickelus.harvests.filter.TierFilterStore;
import se.mickelus.mutil.gui.*;
import se.mickelus.mutil.gui.impl.GuiColors;
import se.mickelus.mutil.gui.impl.GuiHorizontalLayoutGroup;
import se.mickelus.mutil.gui.impl.GuiHorizontalScrollable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ScrollScreen extends Screen {
    private static final WidgetSprites SCROLL_BUTTON_SPRITE = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath(HarvestsMod.modId, "scroll_button"),
            ResourceLocation.fromNamespaceAndPath(HarvestsMod.modId, "scroll_button_active")
    );
    private static final ResourceLocation scrollTexture = ResourceLocation.fromNamespaceAndPath(HarvestsMod.modId, "textures/gui/scroll.png");
    private static TierFilter filter;
    private static double scrollOffset;
    private GuiElement defaultGui;
    private GuiElement filterTabs;
    private GuiHorizontalLayoutGroup tierGroup;
    private GuiHorizontalScrollable scrollArea;

    protected ScrollScreen() {
        super(Component.literal("harvests:gui_title"));

        defaultGui = new GuiElement(0, 0, 420, 240);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen screen) {
            Component tooltip = Component.translatable("harvests.scroll_button.tooltip");
            ImageButton button = new ImageButton(
                    screen.getGuiLeft() + Config.BUTTON_X.get(),
                    screen.getGuiTop() + Config.BUTTON_Y.get(),
                    18, 18,
                    SCROLL_BUTTON_SPRITE,
                    _button -> Minecraft.getInstance().setScreen(new ScrollScreen())
            );
            button.setTooltip(Tooltip.create(tooltip));
            event.addListener(button);
        }
    }

    @Override
    protected void init() {
        super.init();

        defaultGui.clearChildren();

        filterTabs = new GuiHorizontalLayoutGroup(15, 32, 13, 0);
        defaultGui.addChild(filterTabs);

        TierFilter[] filters = TierFilterStore.instance.getFilters();
        if (filters.length > 1) {
            for (final TierFilter tierFilter : filters) {
                filterTabs.addChild(new GuiRect(0, 2, 1, 2, GuiColors.normal).setOpacity(0.3f).setAttachment(GuiAttachment.middleLeft));
                filterTabs.addChild(new FilterTabGui(0, 0, tierFilter, this::selectFilter));
            }
            filterTabs.addChild(new GuiRect(0, 2, 1, 2, GuiColors.normal).setOpacity(0.3f).setAttachment(GuiAttachment.middleLeft));
        }

        if ((filter == null || !Arrays.asList(filters).contains(filter))) {
            filter = filters[0];
            scrollOffset = 0;
        }

        defaultGui.addChild(new GuiTexture(0, 46, 46, 154, 0, 0, scrollTexture));
        defaultGui.addChild(new GuiTexture(46, 46, 164, 154, 46, 0, scrollTexture));
        defaultGui.addChild(new GuiTexture(210, 46, 164, 154, 46, 0, scrollTexture));
        defaultGui.addChild(new GuiTexture(374, 46, 46, 154, 210, 0, scrollTexture));

        ClipRectGui clipRect = new ClipRectGui(17, 52, 394, 142);
        defaultGui.addChild(clipRect);

        scrollArea = new GuiHorizontalScrollable(0, 0, 394, 142);
        clipRect.addChild(scrollArea);

        tierGroup = new GuiHorizontalLayoutGroup(0, 0, 142, 2);
        scrollArea.addChild(tierGroup);

        defaultGui.addChild(new ScrollBarGui(0, -25, 180, 3, scrollArea, true).setAttachment(GuiAttachment.bottomCenter));

        defaultGui.addChild(new GuiTexture(11, 52, 5, 49, 128, 160, scrollTexture));
        defaultGui.addChild(new GuiTexture(11, 102, 5, 93, 137, 160, scrollTexture));
        defaultGui.addChild(new GuiTexture(-3, 52, 5, 49, 132, 160, scrollTexture).setAttachment(GuiAttachment.topRight));
        defaultGui.addChild(new GuiTexture(-3, 102, 5, 93, 141, 160, scrollTexture).setAttachment(GuiAttachment.topRight));

        defaultGui.addChild(new RowLabelGui(1, 52, 20, "tier"));
        defaultGui.addChild(new RowLabelGui(1, 52 + 21, 27, "material"));
        defaultGui.addChild(new RowLabelGui(1, 52 + 49, 43, "tools"));
        defaultGui.addChild(new RowLabelGui(1, 52 + 98, 44, "blocks"));

        defaultGui.addChild(new GuiTexture(-2, 48, 19, 12, 235, 154, scrollTexture).setAttachment(GuiAttachment.topRight));
        defaultGui.addChild(new GuiTexture(-2, 188, 19, 10, 235, 166, scrollTexture).setAttachment(GuiAttachment.topRight));

        updateFilter(filter);
        tierGroup.forceLayout();
        scrollArea.forceRefreshBounds();
        scrollArea.setOffset(scrollOffset);
    }

    private void setupTiers() {
        List<TieredItem> tieredItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof TieredItem)
                .map(item -> (TieredItem) item)
                .collect(Collectors.toList());

        tierGroup.clearChildren();
        List<Tier> sortedTiers = tieredItems.stream()
                .map(TieredItem::getTier)
                .distinct()
                .sorted(Comparator.comparingInt((Tier t) -> (int) StreamSupport.stream(
                                BuiltInRegistries.BLOCK.getTagOrEmpty(t.getIncorrectBlocksForDrops()).spliterator(), false)
                        .count()).reversed())
                .filter(tier -> filter.predicate.test(tier))
                .collect(Collectors.toList());

        for (int i = 0; i < sortedTiers.size(); i++) {
            Tier tier = sortedTiers.get(i);
            tierGroup.addChild(new TierGui(0, 0, tier, i, tieredItems, sortedTiers));
        }
        scrollArea.markDirty();
    }

    private void updateFilter(TierFilter filter) {
        filterTabs.getChildren(FilterTabGui.class).forEach(tab -> tab.updateSelectedFilter(filter));
        ScrollScreen.filter = filter;
        setupTiers();
    }

    private void selectFilter(TierFilter filter) {
        updateFilter(filter);
        scrollOffset = 0;
        scrollArea.setOffset(0);
    }

    @Override
    public void onClose() {
        scrollOffset = scrollArea.getOffset();
        super.onClose();
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        defaultGui.updateFocusState((width - defaultGui.getWidth()) / 2, (height - defaultGui.getHeight()) / 2, mouseX, mouseY);
        defaultGui.draw(graphics, (width - defaultGui.getWidth()) / 2, (height - defaultGui.getHeight()) / 2,
                width, height, mouseX, mouseY, 1);

        renderHoveredToolTip(graphics, mouseX, mouseY);
    }

    protected void renderHoveredToolTip(final GuiGraphics graphics, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0D, 0.0D, 200.0D);
        List<Component> tooltipLines = defaultGui.getTooltipLines();
        if (tooltipLines != null) {
            graphics.renderTooltip(getMinecraft().font, tooltipLines, Optional.empty(), mouseX, mouseY);
        }
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (defaultGui.onMouseClick((int) x, (int) y, button)) {
            return true;
        }

        return super.mouseClicked(x, y, button);
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (defaultGui.onMouseScroll(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (defaultGui.onKeyPress(keyCode, scanCode, modifiers)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (defaultGui.onKeyRelease(keyCode, scanCode, modifiers)) {
            return true;
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char typedChar, int keyCode) {
        return defaultGui.onCharType(typedChar, keyCode);
    }
}
