package dev.gabvoid.voideddimension.client.screen;

import dev.gabvoid.voideddimension.items.ModItems;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
public class ArcaneAdvanceScreen extends Screen {
    private static final Identifier BACKGROUND = Identifier.of("minecraft", "textures/gui/advancements/backgrounds/adventure.png");
    private static final int TILE_SIZE = 16;
    private static final int PADDING = 20;
    private static final double SCREEN_SCALE = 0.21;

    private final List<ArcaneNode> nodes = new ArrayList<>();
    private double scrollX;
    private double scrollY;
    private double targetScrollX;
    private double targetScrollY;
    private boolean dragging;
    private double lastMouseX;
    private double lastMouseY;

    private final int mapWidth = 160;
    private final int mapHeight = 160;

    public ArcaneAdvanceScreen() {
        super(Text.literal("Arcane Atlas"));
        seedNodes();
    }

    @Override
    public void tick() {
        scrollX = MathHelper.lerp(0.25, scrollX, targetScrollX);
        scrollY = MathHelper.lerp(0.25, scrollY, targetScrollY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x00000000);

        int viewWidth = Math.max(120, (int) ((width - PADDING * 2) * SCREEN_SCALE));
        int viewHeight = Math.max(90, (int) ((height - PADDING * 2) * SCREEN_SCALE));
        int viewLeft = (width - viewWidth) / 2;
        int viewTop = (height - viewHeight) / 2;

        clampTargetScroll(viewWidth, viewHeight);

        context.enableScissor(viewLeft, viewTop, viewLeft + viewWidth, viewTop + viewHeight);
        drawTiledBackground(context, viewLeft, viewTop, viewWidth, viewHeight);
        drawNodes(context, viewLeft, viewTop, mouseX, mouseY);
        context.disableScissor();

        drawFrame(context, viewLeft, viewTop, viewWidth, viewHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = isInsideViewport(mouseX, mouseY);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            targetScrollX += (mouseX - lastMouseX);
            targetScrollY += (mouseY - lastMouseY);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isInsideViewport(mouseX, mouseY)) {
            targetScrollY += verticalAmount * 18.0;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void drawTiledBackground(DrawContext context, int viewLeft, int viewTop, int viewWidth, int viewHeight) {
        int startX = MathHelper.floorMod((int) scrollX, TILE_SIZE) - TILE_SIZE;
        int startY = MathHelper.floorMod((int) scrollY, TILE_SIZE) - TILE_SIZE;

        for (int x = startX; x < viewWidth; x += TILE_SIZE) {
            for (int y = startY; y < viewHeight; y += TILE_SIZE) {
                context.drawTexture(
                    BACKGROUND,
                    viewLeft + x,
                    viewTop + y,
                    0,
                    0,
                    TILE_SIZE,
                    TILE_SIZE,
                    256,
                    256
                );
            }
        }
    }

    private void drawNodes(DrawContext context, int viewLeft, int viewTop, int mouseX, int mouseY) {
        for (ArcaneNode node : nodes) {
            int nodeX = (int) (viewLeft + node.x + scrollX);
            int nodeY = (int) (viewTop + node.y + scrollY);

            context.drawItem(node.stack, nodeX, nodeY);

            if (mouseX >= nodeX && mouseX <= nodeX + 16 && mouseY >= nodeY && mouseY <= nodeY + 16) {
                context.drawTooltip(textRenderer, node.label, mouseX, mouseY);
            }
        }
    }

    private void drawFrame(DrawContext context, int viewLeft, int viewTop, int viewWidth, int viewHeight) {
        int frameColor = 0xAA101010;
        context.fill(viewLeft - 2, viewTop - 2, viewLeft + viewWidth + 2, viewTop, frameColor);
        context.fill(viewLeft - 2, viewTop + viewHeight, viewLeft + viewWidth + 2, viewTop + viewHeight + 2, frameColor);
        context.fill(viewLeft - 2, viewTop, viewLeft, viewTop + viewHeight, frameColor);
        context.fill(viewLeft + viewWidth, viewTop, viewLeft + viewWidth + 2, viewTop + viewHeight, frameColor);
    }

    private void clampTargetScroll(int viewWidth, int viewHeight) {
        int minScrollX = Math.min(0, viewWidth - mapWidth);
        int minScrollY = Math.min(0, viewHeight - mapHeight);
        targetScrollX = MathHelper.clamp(targetScrollX, minScrollX, 0);
        targetScrollY = MathHelper.clamp(targetScrollY, minScrollY, 0);
    }

    private boolean isInsideViewport(double mouseX, double mouseY) {
        return mouseX >= PADDING && mouseX <= width - PADDING && mouseY >= PADDING && mouseY <= height - PADDING;
    }

    private void seedNodes() {
        nodes.add(new ArcaneNode(10, 10, new ItemStack(Items.ENDER_EYE), Text.literal("Vigil")));
        nodes.add(new ArcaneNode(40, 12, new ItemStack(Items.AMETHYST_SHARD), Text.literal("Resonance")));
        nodes.add(new ArcaneNode(70, 10, new ItemStack(Items.BOOK), Text.literal("Whisper")));
        nodes.add(new ArcaneNode(10, 40, new ItemStack(Items.BLAZE_POWDER), Text.literal("Fervor")));
        nodes.add(new ArcaneNode(40, 42, new ItemStack(Items.ENDER_PEARL), Text.literal("Veil")));
        nodes.add(new ArcaneNode(70, 40, new ItemStack(Items.CLOCK), Text.literal("Pulse")));
        nodes.add(new ArcaneNode(100, 16, new ItemStack(Items.DIAMOND), Text.literal("Core")));
        nodes.add(new ArcaneNode(100, 46, new ItemStack(Items.EMERALD), Text.literal("Pledge")));
        nodes.add(new ArcaneNode(10, 70, new ItemStack(ModItems.AGONIZING_GLOW), Text.literal("Agonizing Glow")));
        nodes.add(new ArcaneNode(40, 72, new ItemStack(Items.QUARTZ), Text.literal("Dust")));
        nodes.add(new ArcaneNode(70, 72, new ItemStack(Items.LAPIS_LAZULI), Text.literal("Lumen")));
    }

    private static final class ArcaneNode {
        private final int x;
        private final int y;
        private final ItemStack stack;
        private final Text label;

        private ArcaneNode(int x, int y, ItemStack stack, Text label) {
            this.x = x;
            this.y = y;
            this.stack = stack;
            this.label = label;
        }
    }
}
