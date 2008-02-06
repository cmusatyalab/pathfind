/*
 *  Wholeslide -- a library for reading virtual slides
 *
 *  Copyright (c) 2007-2008 Carnegie Mellon University
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
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.*;

import edu.cmu.cs.diamond.wholeslide.Wholeslide;
import edu.cmu.cs.diamond.wholeslide.gui.WholeslideView;

public class PathFind {

    public static void main(String[] args) {
        /* Toplevel frame and layout */

        JFrame jf = new JFrame("PathFind");
        jf.setSize(800, 600);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // jf.setExtendedState(JFrame.MAXIMIZED_BOTH);

        final JPanel j = new JPanel(new BorderLayout());

        /* Wholeslide view */

        final File wsf;

        if (args.length > 0) {
            wsf = new File(args[0]);
        } else {
            JFileChooser ch = new JFileChooser();
            ch.showOpenDialog(jf);

            wsf = ch.getSelectedFile();
        }

        WholeslideView ws = new WholeslideView(new Wholeslide(wsf));
        ws.setBackground(Color.WHITE);

        /* Menu bar and doodads */

        JMenuBar mb = new JMenuBar();

        JMenuItem z = new JMenuItem("Simulate selection search");
        /* Search pane */

        z.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final Box search = Box.createHorizontalBox();

                JButton close = new JButton("Cancel search");
                close.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        search.setVisible(false);
                    }
                });

                Box summary = Box.createVerticalBox();
                summary.setAlignmentY(Component.TOP_ALIGNMENT);
                summary.add(new JLabel("Search results"));
                summary.add(Box.createVerticalStrut(4));
                summary.add(new JSeparator());
                summary.add(Box.createVerticalStrut(4));
                summary.add(close);
                // summary.add(Box.createVerticalStrut(100));
                summary.setBorder(BorderFactory.createEmptyBorder(5, 5, 90, 5));

                Box results = Box.createHorizontalBox();
                for (int i = 0; i < 6; i++) {
                    WholeslideView r = new WholeslideView(new Wholeslide(wsf));
                    // r.setBackground(Color.WHITE);
                    r.setEnabled(false);
                    r.setAlignmentY(Component.TOP_ALIGNMENT);
                    r.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(5, 15, 15, 0),
                            BorderFactory.createLineBorder(Color.GRAY)));

                    results.add(r);
                }

                search.add(summary);
                search.add(results);

                j.add(search, BorderLayout.NORTH);
                j.revalidate();
            }
        });

        JMenu f = new JMenu("File");
        f.add(z);
        mb.add(f);
        mb.add(new JMenu("Image"));
        mb.add(new JMenu("Case"));
        mb.add(new JMenu("View"));
        mb.add(new JMenu("Window"));
        mb.add(new JMenu("Help"));

        jf.setJMenuBar(mb);

        /* Toplevel component */

        j.add(ws);

        jf.getContentPane().add(j);
        jf.setVisible(true);
    }
}
