/*
 *  PathFind -- a Diamond system for pathology
 *
 *  Copyright (c) 2008-2009 Carnegie Mellon University
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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.*;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

import edu.cmu.cs.openslide.gui.OpenSlideView;

public class PairedSlideView extends JPanel {

    private OpenSlideView slide;
    private OpenSlideView result;

    private final JPanel slideViews;
    private final JToggleButton linker;

    public PairedSlideView() {
        setLayout(new BorderLayout());

        // main view in center
        slideViews = new JPanel(new GridLayout(1, 2));
        add(slideViews);

        linker = new JToggleButton("Link");
        linker.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                switch (e.getStateChange()) {
                case ItemEvent.SELECTED:
                    slide.linkWithOther(result);
                    break;
                case ItemEvent.DESELECTED:
                    slide.unlinkOther();
                    break;
                }
            }
        });
    }

    public OpenSlideView getSlide() {
        return slide;
    }

    public void setSlide(OpenSlideView wv) {
        if (slide != null) {
            slide.getOpenSlide().dispose();
            slideViews.remove(slide);
        }

        slide = wv;

        slideViews.add(slide, 0);
    }

    public void setResult(OpenSlideView wv) {
        if (result != null) {
            result.getOpenSlide().dispose();
            slideViews.remove(result);

            remove(linker);
            linker.setSelected(false);
        }

        result = wv;

        if (result != null) {
            slideViews.add(result, 1);
            add(linker, BorderLayout.SOUTH);
        }
    }
}
