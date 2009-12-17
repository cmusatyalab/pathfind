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
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.cmu.cs.diamond.opendiamond.Result;
import edu.cmu.cs.diamond.opendiamond.Search;

public class SearchPanel extends JPanel {
    final protected JList list;

    protected Search theSearch;

    final private PathFind pathFind;

    private SwingWorker<Object, edu.cmu.cs.diamond.pathfind.ResultIcon> workerFuture;

    public SearchPanel(final PathFind pf) {
        pathFind = pf;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Search Results"));

        list = new JList();
        list.setCellRenderer(new SearchPanelCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(1);

        setPreferredSize(new Dimension(100, 280));

        add(new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                ResultIcon r = (ResultIcon) list.getSelectedValue();

                if (r == null) {
                    pf.setResult(null, null);
                } else {
                    pf.setResult(r.getIcon(), "Search Result");
                }
            }
        });
    }

    @Override
    public void setVisible(boolean visible) {
        boolean oldVisible = isVisible();

        super.setVisible(visible);
        if (oldVisible != visible && !visible) {
            if (theSearch != null) {
                try {
                    theSearch.close();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    void beginSearch(final Search s) throws InterruptedException {
        if (theSearch != null) {
            theSearch.close();
        }

        theSearch = s;

        final DefaultListModel model = new DefaultListModel();
        list.setModel(model);
        workerFuture = new SwingWorker<Object, ResultIcon>() {
            @Override
            protected Object doInBackground() throws InterruptedException {
                // non-AWT thread
                try {
                    try {
                        while (true) {
                            Result r = s.getNextResult();
                            if (r == null) {
                                break;
                            }
                            // System.out.println(r);

                            byte[] thumbData = r.getValue("thumbnail.jpeg");
                            BufferedImage thumb = null;
                            if (thumbData != null) {
                                ByteArrayInputStream in = new ByteArrayInputStream(
                                        thumbData);
                                try {
                                    thumb = ImageIO.read(in);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (thumb == null) {
                                // cook up blank image
                                thumb = new BufferedImage(200, 150,
                                        BufferedImage.TYPE_INT_RGB);
                            }

                            final ResultIcon resultIcon = new ResultIcon(
                                    new ImageIcon(thumb), r
                                            .getObjectIdentifier());

                            publish(resultIcon);
                        }
                    } finally {
                        System.out.println("STOP");

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                if (s != null) {
                                    try {
                                        s.close();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                        Thread.currentThread().interrupt();
                                    }
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    // TODO pop up something?
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void process(List<ResultIcon> chunks) {
                // AWT thread
                for (ResultIcon resultIcon : chunks) {
                    model.addElement(resultIcon);
                }
            }
        };
        workerFuture.execute();
        setVisible(true);
    }

    void endSearch() throws InterruptedException {
        if (theSearch != null) {
            theSearch.close();
        }
    }
}
