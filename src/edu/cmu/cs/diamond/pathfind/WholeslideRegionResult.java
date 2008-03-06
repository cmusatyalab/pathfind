package edu.cmu.cs.diamond.pathfind;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

import edu.cmu.cs.diamond.wholeslide.Wholeslide;
import edu.cmu.cs.diamond.wholeslide.gui.WholeslideView;

public class WholeslideRegionResult {
    private BufferedImage thumb;

    public final File ws;

    public final Shape region;

    public final double value;

    public final String oneLineInfo;

    public final String hoverInfo;

    public final String fullInfo;

    public final int thumbSize;

    public boolean hidden;
    
    public WholeslideRegionResult(File ws, Shape region, double value,
            int thumbSize, String oneLineInfo, String hoverInfo, String fullInfo) {
        this.ws = ws;
        this.region = region;
        this.value = value;

        this.oneLineInfo = oneLineInfo;
        this.hoverInfo = hoverInfo;
        this.fullInfo = fullInfo;

        this.thumb = drawThumbnail(thumbSize);
        this.thumbSize = thumbSize;
    }

    public BufferedImage getThumbnail() {
        if (thumb == null) {
            thumb = drawThumbnail(thumbSize);
        }
        return thumb;
    }

    private BufferedImage drawThumbnail(int maxSize) {
        Wholeslide slide = new Wholeslide(ws);

        try {
            System.out.println("drawing thumbnail from " + Thread.currentThread());
            return drawThumbnail(slide, region, maxSize);
        } finally {
            slide.dispose();
        }
    }

    static public BufferedImage drawThumbnail(Wholeslide slide,
            Shape selection, int maxSize) {
        Rectangle2D bb = selection.getBounds2D();

        double downsample = Math.max(bb.getWidth(), bb.getHeight()) / maxSize;

        if (downsample < 1.0) {
            downsample = 1.0;
        }

        BufferedImage thumb = slide.createThumbnailImage((int) bb.getX(),
                (int) bb.getY(), (int) bb.getWidth(), (int) bb.getHeight(),
                maxSize);
        Graphics2D g = thumb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        WholeslideView.paintSelection(g, selection,
                (int) (-bb.getX() / downsample),
                (int) (-bb.getY() / downsample), downsample);
        g.dispose();

        return thumb;
    }
}
