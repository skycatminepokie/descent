package com.skycatdev.descent.map;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public record Tetrahedron(DungeonPiece.Opening a, DungeonPiece.Opening b, DungeonPiece.Opening c, DungeonPiece.Opening d, Sphere circumsphere) {

    public Tetrahedron(DungeonPiece.Opening a, DungeonPiece.Opening b, DungeonPiece.Opening c, DungeonPiece.Opening d) {
        this(a, b, c, d, Sphere.circumsphere(a.bounds().center(), b.bounds().center(), c.bounds().center(), d.bounds().center()));
    }

    public boolean hasOpening(DungeonPiece.Opening opening) {
        return opening.equals(a) ||
               opening.equals(b) ||
               opening.equals(c) ||
               opening.equals(d);
    }

    public boolean circumsphereContains(Vec3i vec) {
        return Vec3d.of(vec).subtract(circumsphere.center()).squaredDistanceTo(0, 0, 0) <= circumsphere.radiusSquared();
    }

}
