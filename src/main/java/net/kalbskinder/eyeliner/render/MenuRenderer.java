package net.kalbskinder.eyeliner.render;

import net.kalbskinder.eyeliner.element.ButtonElement;
import net.kalbskinder.eyeliner.element.Element;
import net.kalbskinder.eyeliner.element.ScreenElement;
import net.kalbskinder.eyeliner.element.SpriteElement;
import net.kalbskinder.eyeliner.element.TextElement;
import net.kalbskinder.eyeliner.layout.FontMetrics;
import net.kalbskinder.eyeliner.layout.LayoutNode;
import net.kalbskinder.eyeliner.style.Colors;
import net.kalbskinder.eyeliner.style.Insets;
import net.kalbskinder.eyeliner.style.Style;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.MetadataHolder;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import net.minestom.server.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Converts a {@link LayoutNode} tree into packet entities for one player:
 * <ul>
 *     <li>backgrounds/borders: text displays containing a single space, whose background is scaled into a rectangle</li>
 *     <li>text: text displays</li>
 *     <li>sprites: item displays in GUI context</li>
 *     <li>buttons: additionally invisible interaction entities, so right clicks are sent to the server
 *     and the player cannot break blocks behind the button</li>
 * </ul>
 * Nested elements are pushed slightly towards the viewer to avoid z-fighting.
 * Text displays are only visible from the front, so the screen is rendered twice: once per side, back to back.
 */
public final class MenuRenderer {

    /**
     * Distance between nesting levels, in blocks.
     */
    public static final double Z_STEP = 0.01;

    /**
     * A text display containing " " has a background of 0.125 x 0.25 blocks at scale 1, spanning
     * x in [-0.05, 0.075] and y in [0, 0.25] relative to the entity.
     */
    private static final double SPACE_WIDTH = 0.125;
    private static final double SPACE_HEIGHT = 0.25;
    private static final double SPACE_LEFT = 0.05;

    /**
     * A single rendered button: its hit box, the background pieces recolored on hover and its interaction entities.
     */
    public record RenderedButton(LayoutNode node, ButtonElement element,
                                 List<PacketEntity<TextDisplayMeta>> fills, int bg, int hoverBg,
                                 List<Integer> interactionIds) {
    }

    /**
     * Everything that was rendered for one menu.
     */
    public record Result(List<PacketEntity<?>> entities, List<RenderedButton> buttons,
                         Map<String, List<PacketEntity<TextDisplayMeta>>> textsById) {
    }

    private final Player viewer;
    private final ScreenTransform front;
    private final ScreenElement screen;
    private ScreenTransform transform; // side currently being rendered
    private int backButtonIndex;
    private final float cullSize;

    private final List<PacketEntity<?>> entities = new ArrayList<>();
    private final List<RenderedButton> buttons = new ArrayList<>();
    private final List<List<PacketEntity<TextDisplayMeta>>> buttonFills = new ArrayList<>();
    private final Map<String, List<PacketEntity<TextDisplayMeta>>> textsById = new HashMap<>();

    public MenuRenderer(Player viewer, ScreenTransform transform, ScreenElement screen) {
        this.viewer = viewer;
        this.front = transform;
        this.screen = screen;
        this.cullSize = (float) (2 * Math.max(transform.width(), transform.height()));
    }

    public Result render(LayoutNode root) {
        transform = front;
        renderNode(root, true);
        transform = front.back();
        backButtonIndex = 0;
        renderNode(root, false);

        final List<RenderedButton> result = new ArrayList<>(buttons.size());
        for (int i = 0; i < buttons.size(); i++) {
            final RenderedButton b = buttons.get(i);
            result.add(new RenderedButton(b.node(), b.element(), List.copyOf(buttonFills.get(i)), b.bg(), b.hoverBg(), b.interactionIds()));
        }
        final Map<String, List<PacketEntity<TextDisplayMeta>>> texts = new HashMap<>();
        textsById.forEach((id, list) -> texts.put(id, List.copyOf(list)));
        return new Result(List.copyOf(entities), List.copyOf(result), Map.copyOf(texts));
    }

    /**
     * @param frontSide true for the front pass, which also creates the buttons and their interaction entities
     */
    private void renderNode(LayoutNode node, boolean frontSide) {
        final Element element = node.element();
        final Style style = element.style();
        final double z = node.depth() * Z_STEP;
        final boolean button = element instanceof ButtonElement;

        // Background and border
        final int bg = style.bg();
        final int hoverBg = button ? style.hoverBg() : bg;
        final boolean drawFill = Colors.alpha(bg) > 0 || Colors.alpha(hoverBg) > 0;
        final List<PacketEntity<TextDisplayMeta>> fills = new ArrayList<>();
        if (drawFill || style.border() > 0) {
            final double radius = style.radius().resolve(Math.min(node.width(), node.height()) / 2, 0);
            final List<ShapeBuilder.Piece> pieces = ShapeBuilder.build(
                    transform.localX(node.x()), transform.localY(node.y() + node.height()),
                    node.width(), node.height(), radius, style.border());
            for (ShapeBuilder.Piece piece : pieces) {
                if (piece.fill() && !drawFill) continue;
                final PacketEntity<TextDisplayMeta> rect = rectangle(piece, z, piece.fill() ? bg : style.borderColor());
                if (piece.fill()) fills.add(rect);
            }
        }

        switch (element) {
            case TextElement text -> renderText(node, text, z + Z_STEP / 2);
            case SpriteElement sprite -> renderSprite(node, sprite, z + Z_STEP / 2);
            default -> {
            }
        }

        if (button && frontSide) {
            buttons.add(new RenderedButton(node, (ButtonElement) element, List.of(), bg, hoverBg, interactions(node)));
            buttonFills.add(fills);
        } else if (button) {
            buttonFills.get(backButtonIndex++).addAll(fills);
        }

        for (LayoutNode child : node.children()) renderNode(child, frontSide);
    }

    private PacketEntity<TextDisplayMeta> rectangle(ShapeBuilder.Piece piece, double z, int color) {
        final PacketEntity<TextDisplayMeta> entity = display(EntityType.TEXT_DISPLAY, TextDisplayMeta::new);
        final TextDisplayMeta meta = entity.meta();
        meta.setText(Component.text(" "));
        meta.setBackgroundColor(color);
        meta.setScale(new Vec(piece.width() / SPACE_WIDTH, piece.height() / SPACE_HEIGHT, 1));
        meta.setTranslation(transform.offset(piece.left() + SPACE_LEFT * piece.width() / SPACE_WIDTH, piece.bottom(), z));
        return entity;
    }

    private void renderText(LayoutNode node, TextElement element, double z) {
        final LayoutNode.TextLayout text = node.text();
        if (text == null) return;
        final Style style = element.style();
        final PacketEntity<TextDisplayMeta> entity = display(EntityType.TEXT_DISPLAY, TextDisplayMeta::new);
        final TextDisplayMeta meta = entity.meta();
        meta.setText(text.component());
        meta.setLineWidth(text.lineWidth());
        meta.setBackgroundColor(0);
        meta.setShadow(style.shadow());
        meta.setAlignment(switch (style.textAlign()) {
            case LEFT -> TextDisplayMeta.Alignment.LEFT;
            case CENTER -> TextDisplayMeta.Alignment.CENTER;
            case RIGHT -> TextDisplayMeta.Alignment.RIGHT;
        });
        final int alpha = Colors.alpha(style.color());
        if (alpha < 255) meta.setTextOpacity((byte) alpha);
        meta.setScale(new Vec(text.scale(), text.scale(), text.scale()));
        // Glyphs are centered half a font pixel right of the entity, compensate for it.
        final double centerX = transform.localX(text.blockX() + text.blockWidth() / 2) - 0.5 * FontMetrics.PIXEL * text.scale();
        final double bottom = transform.localY(text.blockY() + text.blockHeight());
        meta.setTranslation(transform.offset(centerX, bottom, z));
        if (element.id() != null) textsById.computeIfAbsent(element.id(), _ -> new ArrayList<>()).add(entity);
    }

    private void renderSprite(LayoutNode node, SpriteElement sprite, double z) {
        final PacketEntity<ItemDisplayMeta> entity = display(EntityType.ITEM_DISPLAY, ItemDisplayMeta::new);
        final ItemDisplayMeta meta = entity.meta();
        ItemStack stack = ItemStack.of(sprite.material());
        if (sprite.glint()) stack = stack.with(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        meta.setItemStack(stack);
        meta.setDisplayContext(ItemDisplayMeta.DisplayContext.GUI);
        final double size = sprite.size();
        meta.setScale(new Vec(size, size, size));
        // Item models have depth (block items are drawn as small cubes), push them forward so they stay in front.
        final double depth = sprite.material().block() != null ? size * 0.35 : size * 0.05;
        final Insets padding = sprite.style().padding();
        final double centerX = transform.localX(node.x() + padding.left() + (node.width() - padding.horizontal()) / 2);
        final double centerY = transform.localY(node.y() + padding.top() + (node.height() - padding.vertical()) / 2);
        meta.setTranslation(transform.offset(centerX, centerY, z + depth));
    }

    /**
     * Interaction hit boxes are axis aligned with a square footprint, so wide buttons get several narrow tiles
     * to keep the boxes close to the screen plane regardless of its rotation.
     */
    private List<Integer> interactions(LayoutNode node) {
        final double width = node.width(), height = node.height();
        if (width <= 0 || height <= 0) return List.of();
        final int tiles = (int) Math.ceil(width / Math.min(width, height) - 1e-6);
        final double tileWidth = width / tiles;
        final double bottom = transform.localY(node.y() + height);
        final List<Integer> ids = new ArrayList<>(tiles);
        for (int i = 0; i < tiles; i++) {
            final double centerX = transform.localX(node.x() + (i + 0.5) * tileWidth);
            final Pos position = transform.toWorld(centerX, bottom, Z_STEP).asPos();
            final PacketEntity<InteractionMeta> entity = new PacketEntity<>(EntityType.INTERACTION, position, viewer, InteractionMeta::new);
            entity.meta().setWidth((float) tileWidth);
            entity.meta().setHeight((float) height);
            entity.meta().setResponse(true);
            entities.add(entity);
            ids.add(entity.id());
        }
        return ids;
    }

    private <M extends AbstractDisplayMeta> PacketEntity<M> display(EntityType type, BiFunction<Entity, MetadataHolder, M> factory) {
        final PacketEntity<M> entity = new PacketEntity<>(type, transform.origin(), viewer, factory);
        final M meta = entity.meta();
        meta.setLeftRotation(transform.rotation());
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.FIXED);
        if (screen.brightness() >= 0) meta.setBrightness(screen.brightness(), screen.brightness());
        // Displays are culled using this box around their origin; the screen center is the origin of everything.
        meta.setWidth(cullSize);
        meta.setHeight(cullSize);
        entities.add(entity);
        return entity;
    }
}
