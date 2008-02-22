package edu.cmu.cs.diamond.pathfind;

import java.awt.Shape;

import edu.cmu.cs.diamond.wholeslide.Wholeslide;

public class WholeslideRegionResult {
    public WholeslideRegionResult(Wholeslide ws, Shape region, double value) {
        this.ws = ws;
        this.region = region;
        this.value = value;
    }
    public final Wholeslide ws;
    public final Shape region;
    public final double value;
}
