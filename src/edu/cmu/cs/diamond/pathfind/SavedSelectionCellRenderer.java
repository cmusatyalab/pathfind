package edu.cmu.cs.diamond.pathfind;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.SwingConstants;

import edu.cmu.cs.diamond.wholeslide.Wholeslide;
import edu.cmu.cs.diamond.wholeslide.gui.WholeslideView;

public class SavedSelectionCellRenderer extends DefaultListCellRenderer {

    final private BufferedImage thumb;

    final private double downsample;

    final private static int THUMBNAIL_SIZE = 200;

    public SavedSelectionCellRenderer(WholeslideView wv) {
        Wholeslide ws = wv.getWholeSlide();
        thumb = ws.createThumbnailImage(THUMBNAIL_SIZE);

        Dimension d = ws.getLayer0Dimension();
        downsample = Math.max(d.height, d.width) / THUMBNAIL_SIZE;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        DefaultListCellRenderer c = (DefaultListCellRenderer) super
                .getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);

        Shape s = (Shape) value;

        c.setText(null);

        BufferedImage b = new BufferedImage(thumb.getWidth(),
                thumb.getHeight(), thumb.getType());
        Graphics2D g = b.createGraphics();
        g.drawImage(thumb, 0, 0, null);
        WholeslideView.paintSelection(g, s, 0, 0, downsample);
        g.dispose();

        c.setIcon(new ImageIcon(b));

        c.setHorizontalAlignment(SwingConstants.CENTER);
        c.setVerticalAlignment(SwingConstants.CENTER);

        c.setHorizontalTextPosition(SwingConstants.CENTER);
        c.setVerticalTextPosition(SwingConstants.BOTTOM);

        c.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        return c;
    }

}
