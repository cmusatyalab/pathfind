package edu.cmu.cs.diamond.pathfind;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import edu.cmu.cs.diamond.wholeslide.Wholeslide;
import edu.cmu.cs.diamond.wholeslide.gui.WholeslideView;

public class WholeslideRegionResult {
    private BufferedImage thumb;

    public final Wholeslide ws;
    public final Shape region;
    public final double value;
    
    public final String oneLineInfo;
    public final String hoverInfo;
    public final String fullInfo;

    private final int thumbSize;

    public WholeslideRegionResult(Wholeslide ws, Shape region, double value,
            int thumbSize, String oneLineInfo, String hoverInfo, String fullInfo) {
        this.ws = ws;
        this.region = region;
        this.value = value;
        
        this.oneLineInfo = oneLineInfo;
        this.hoverInfo = hoverInfo;
        this.fullInfo = fullInfo;

        // this.thumb = drawThumbnail(thumbSize);
        this.thumbSize = thumbSize;
    }

    public BufferedImage getThumbnail() {
        if (thumb == null) {
            thumb = drawThumbnail(thumbSize);
        }
        return thumb;
    }

    private BufferedImage drawThumbnail(int maxSize) {
        Shape s = region;

        Rectangle2D bb = s.getBounds2D();

        double downsample = Math.max(bb.getWidth(), bb.getHeight()) / maxSize;

        if (downsample < 1.0) {
            downsample = 1.0;
        }

        BufferedImage thumb = ws.createThumbnailImage((int) bb.getX(), (int) bb
                .getY(), (int) bb.getWidth(), (int) bb.getHeight(), maxSize);
        Graphics2D g = thumb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        WholeslideView.paintSelection(g, s, (int) (-bb.getX() / downsample),
                (int) (-bb.getY() / downsample), downsample);
        g.dispose();

        return thumb;
    }
}
