/*
 *  PathFind -- a Diamond system for pathology
 *
 *  Copyright (c) 2008 Carnegie Mellon University
 *  All rights reserved.
 *
 *  PathFind is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2.
 *
 *  PathFind is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PathFind. If not, see <http://www.gnu.org/licenses/>.
 *
 *  Linking PathFind statically or dynamically with other modules is
 *  making a combined work based on PathFind. Thus, the terms and
 *  conditions of the GNU General Public License cover the whole
 *  combination.
 *
 *  In addition, as a special exception, the copyright holders of
 *  PathFind give you permission to combine PathFind with free software
 *  programs or libraries that are released under the GNU LGPL or the
 *  Eclipse Public License 1.0. You may copy and distribute such a system
 *  following the terms of the GNU GPL for PathFind and the licenses of
 *  the other code concerned, provided that you include the source code of
 *  that other code when and as the GNU GPL requires distribution of source
 *  code.
 *
 *  Note that people who make modified versions of PathFind are not
 *  obligated to grant this special exception for their modified versions;
 *  it is their choice whether to do so. The GNU General Public License
 *  gives permission to release a modified version without this exception;
 *  this exception also makes it possible to release a modified version
 *  which carries forward this exception.
 */

package edu.cmu.cs.diamond.pathfind;

import java.awt.Component;
import java.awt.image.BufferedImage;

import javax.swing.*;

public class SearchPanelCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        WholeslideRegionResult r = (WholeslideRegionResult) value;

        setHorizontalAlignment(SwingConstants.CENTER);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setToolTipText("<html>" + r.hoverInfo + "</html>");

        if (r.hidden) {
            setVerticalAlignment(SwingConstants.TOP);
            BufferedImage blank = new BufferedImage(2, r.thumbSize,
                    BufferedImage.TYPE_INT_RGB);

            setIcon(new ImageIcon(blank));
            setText("x");
        } else {
            setVerticalAlignment(SwingConstants.CENTER);
            BufferedImage thumb = r.getThumbnail();

            setIcon(new ImageIcon(thumb));
            setText("<html>" + r.oneLineInfo + "<br>"
                    + Double.toString(r.value) + "</html>");
        }

        return this;
    }
}
