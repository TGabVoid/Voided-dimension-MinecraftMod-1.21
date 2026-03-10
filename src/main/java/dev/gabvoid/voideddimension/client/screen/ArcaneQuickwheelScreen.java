package dev.gabvoid.voideddimension.client.screen;

import dev.gabvoid.voideddimension.items.ModItems;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ArcaneQuickwheelScreen extends Screen {
    private static final double WHEEL_SCALE = 0.21;
    private static final int ICON_SIZE = 16;
    private static final int CENTER_MARKER = 2;
    private static final int PICK_RADIUS = 10;

    private final List<QuickNode> nodes = new ArrayList<>();

    public ArcaneQuickwheelScreen() {
        super(Text.literal("Arcane Wheel"));
        seedNodes();
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void removed() {
        super.removed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x00000000);

        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.max(18, (int) (Math.min(width, height) / 5.0 * WHEEL_SCALE));

        for (int index = 0; index < nodes.size(); index++) {
            QuickNode node = nodes.get(index);
            double angle = Math.toRadians(-90 + (index * 72));
            int iconX = centerX + (int) (Math.cos(angle) * radius) - ICON_SIZE / 2;
            int iconY = centerY + (int) (Math.sin(angle) * radius) - ICON_SIZE / 2;

            context.drawItem(node.stack, iconX, iconY);

            if (isHovering(mouseX, mouseY, iconX, iconY)) {
                context.drawTooltip(textRenderer, node.label, mouseX, mouseY);
            }

            node.lastX = iconX + ICON_SIZE / 2;
            node.lastY = iconY + ICON_SIZE / 2;
        }

        context.fill(centerX - CENTER_MARKER, centerY - CENTER_MARKER, centerX + CENTER_MARKER, centerY + CENTER_MARKER, 0x88FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            QuickNode picked = pickNode((int) mouseX, (int) mouseY);
            if (picked != null && client != null && client.player != null) {
                client.player.giveItemStack(picked.stack.copy());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private boolean isHovering(int mouseX, int mouseY, int iconX, int iconY) {
        return mouseX >= iconX && mouseX <= iconX + ICON_SIZE && mouseY >= iconY && mouseY <= iconY + ICON_SIZE;
    }

    private QuickNode pickNode(int mouseX, int mouseY) {
        for (QuickNode node : nodes) {
            if (node.lastX == Integer.MIN_VALUE) {
                continue;
            }
            int dx = mouseX - node.lastX;
            int dy = mouseY - node.lastY;
            if ((dx * dx + dy * dy) <= (PICK_RADIUS * PICK_RADIUS)) {
                return node;
            }
        }
        return null;
    }

    private void seedNodes() {
        nodes.add(new QuickNode(new ItemStack(Items.DIAMOND), Text.literal("Diamond")));
        nodes.add(new QuickNode(new ItemStack(Items.EMERALD), Text.literal("Emerald")));
        nodes.add(new QuickNode(new ItemStack(Items.QUARTZ), Text.literal("Quartz")));
        nodes.add(new QuickNode(new ItemStack(Items.AMETHYST_SHARD), Text.literal("Amethyst")));
        nodes.add(new QuickNode(new ItemStack(ModItems.AGONIZING_GLOW), Text.literal("Agonizing Glow")));
    }

    private static final class QuickNode {
        private final ItemStack stack;
        private final Text label;
        private int lastX = Integer.MIN_VALUE;
        private int lastY = Integer.MIN_VALUE;

        private QuickNode(ItemStack stack, Text label) {
            this.stack = stack;
            this.label = label;
        }
    }
}
