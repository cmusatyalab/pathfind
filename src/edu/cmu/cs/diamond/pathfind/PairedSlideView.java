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

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.cmu.cs.wholeslide.gui.WholeslideView;

public class PairedSlideView extends JPanel {

    private final JToggleButton linkButton;

    private final WholeslideView slides[] = new WholeslideView[2];

    private final JPanel slideViews;

    public PairedSlideView() {
        setLayout(new BorderLayout());

        // main view in center
        slideViews = new JPanel(new GridLayout(1, 2));
        add(slideViews);

        // link button at bottom
        linkButton = new JToggleButton("Link");
        add(linkButton, BorderLayout.SOUTH);
        linkButton.setVisible(false);
        linkButton.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (linkButton.isSelected()) {
                    slides[0].linkWithOther(slides[1]);
                } else {
                    slides[0].unlinkOther();
                }
            }
        });
    }

    public WholeslideView getLeftSlide() {
        return slides[0];
    }

    public WholeslideView getRightSlide() {
        return slides[1];
    }

    public void setLeftSlide(WholeslideView wv) {
        WholeslideView oldSlide = slides[0];
        if (oldSlide != null) {
            oldSlide.unlinkOther();
        }

        slides[0] = wv;

        slideViews.add(wv, 0);
    }

    public void setRightSlide(WholeslideView wv) {
        linkButton.setSelected(false);
        if (slides[1] != null) {
            slideViews.remove(slides[1]);
            slides[1].unlinkOther();
        }

        if (wv == null) {
            linkButton.setVisible(false);
        } else {
            slides[1] = wv;
            slideViews.add(wv, 1);
            linkButton.setVisible(true);
        }

        slideViews.revalidate();
    }
}
