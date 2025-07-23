package com.skycatdev.descent.map;

public record Edge(DungeonPiece.Opening u, DungeonPiece.Opening v, double length) {

    public Edge(DungeonPiece.Opening u, DungeonPiece.Opening v) {
        this(u, v, u.center().getManhattanDistance(v.center()));
    }

    public double getLength() {
        return length;
    }
}
