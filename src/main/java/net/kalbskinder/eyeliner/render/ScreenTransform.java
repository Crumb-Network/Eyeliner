package net.kalbskinder.eyeliner.render;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Nullable;

/**
 * Maps between screen coordinates and world coordinates for a vertical screen.
 * <ul>
 *     <li>UI coordinates: origin at the top-left corner, x right, y down (what the layout produces)</li>
 *     <li>local coordinates: origin at the screen center, x right, y up, z towards the viewer</li>
 * </ul>
 * A display entity with no rotation faces south (+Z) with its local x axis pointing east. The screen is
 * rotated around Y by {@code -yaw}, which makes it face the direction of {@code yaw} (Minecraft convention).
 */
public final class ScreenTransform {

    private final Pos origin;
    private final double width;
    private final double height;
    private final Vec right;
    private final Vec up = new Vec(0, 1, 0);
    private final Vec normal;
    private final float[] rotation;
    private final boolean mirrored;

    /**
     * @param origin world position of the screen center; its yaw is the direction the screen faces
     */
    public ScreenTransform(Pos origin, double width, double height) {
        this(origin, width, height, false);
    }

    private ScreenTransform(Pos origin, double width, double height, boolean mirrored) {
        this.mirrored = mirrored;
        this.origin = origin.withView(0, 0);
        this.width = width;
        this.height = height;
        final double yaw = Math.toRadians(origin.yaw());
        this.right = new Vec(Math.cos(yaw), 0, Math.sin(yaw));
        this.normal = new Vec(-Math.sin(yaw), 0, Math.cos(yaw));
        // Quaternion (x, y, z, w) for a rotation of -yaw around the Y axis
        final double half = -yaw / 2;
        this.rotation = new float[]{0, (float) Math.sin(half), 0, (float) Math.cos(half)};
    }

    /**
     * The back side of this screen: faces the opposite direction, but UI coordinates map to the same world positions
     * (the UI x axis is mirrored), so shapes rendered with it are the back faces of the front ones.
     */
    public ScreenTransform back() {
        return new ScreenTransform(origin.withView(origin.yaw() + 180, 0), width, height, !mirrored);
    }

    /**
     * Whether {@code point} lies on the side of the screen it faces.
     */
    public boolean isInFront(Point point) {
        return point.sub(origin).asVec().dot(normal) >= 0;
    }

    /**
     * Creates a screen {@code distance} blocks in front of the given eye position, facing back at it.
     */
    public static Pos inFrontOf(Pos eye, double distance) {
        final double yaw = Math.toRadians(eye.yaw());
        final Vec forward = new Vec(-Math.sin(yaw), 0, Math.cos(yaw));
        return eye.add(forward.mul(distance)).withView(eye.yaw() + 180, 0);
    }

    /**
     * Spawn position shared by all display entities of this screen (rotation is applied via metadata).
     */
    public Pos origin() {
        return origin;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    /**
     * The left rotation quaternion every display entity of this screen uses.
     */
    public float[] rotation() {
        return rotation.clone();
    }

    /**
     * Note that on a {@link #back()} transform the UI x axis points left, so this maps the left edge of a box to
     * its right edge.
     */
    public double localX(double uiX) {
        return mirrored ? width / 2 - uiX : uiX - width / 2;
    }

    public double localY(double uiY) {
        return height / 2 - uiY;
    }

    /**
     * Offset from {@link #origin()} to a point in local coordinates, in world axes.
     * Used as the translation of display entities.
     */
    public Vec offset(double localX, double localY, double localZ) {
        return right.mul(localX).add(up.mul(localY)).add(normal.mul(localZ));
    }

    public Point toWorld(double localX, double localY, double localZ) {
        return origin.add(offset(localX, localY, localZ));
    }

    /**
     * Intersects a ray with the front of the screen.
     *
     * @return the hit as (ui x, ui y, distance along the ray), or null if the ray misses the plane, hits it from
     * behind or is further away than {@code maxDistance}. The point may lie outside the screen bounds.
     */
    public double @Nullable [] raycast(Point eye, Vec direction, double maxDistance) {
        final double denominator = direction.dot(normal);
        if (denominator > -1e-6) return null; // parallel or looking at the back side
        final Vec toOrigin = origin.sub(eye).asVec();
        final double t = toOrigin.dot(normal) / denominator;
        if (t < 0 || t > maxDistance) return null;
        final Vec hit = eye.asVec().add(direction.mul(t)).sub(origin.asVec());
        final double localX = hit.dot(right);
        final double localY = hit.dot(up);
        return new double[]{mirrored ? width / 2 - localX : localX + width / 2, height / 2 - localY, t};
    }

    public static Vec direction(float yaw, float pitch) {
        final double y = Math.toRadians(yaw);
        final double p = Math.toRadians(pitch);
        return new Vec(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }
}
