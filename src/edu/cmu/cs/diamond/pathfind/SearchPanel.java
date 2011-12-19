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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.geom.Path2D;
import java.io.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.concurrent.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.cmu.cs.diamond.opendiamond.ObjectIdentifier;
import edu.cmu.cs.diamond.opendiamond.Result;
import edu.cmu.cs.diamond.opendiamond.Search;
import edu.cmu.cs.diamond.opendiamond.SearchFactory;
import edu.cmu.cs.diamond.opendiamond.SearchClosedException;
import edu.cmu.cs.diamond.opendiamond.ServerStatistics;

import org.openslide.OpenSlide;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class SearchPanel extends JPanel {
    final protected JList list;
    final private Map<String,String> slideHashMap;

    final private QueryPanel qp;
    final private StatisticsBar stats;
    private ScheduledExecutorService timerExecutor;
    private ScheduledFuture<?> statsTimerFuture;

    private Search theSearch;

    private SwingWorker<Object, edu.cmu.cs.diamond.pathfind.ResultIcon> workerFuture;

    public SearchPanel(final PathFindFrame pf, QueryPanel qp,
            String slideMap, final AnnotationStore annotationStore) {
        this.qp = qp;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Search Results"));

        // Load quickhash1 to slide mappings
        slideHashMap = new HashMap<String,String>();
        try {
            Scanner s = new Scanner(new FileInputStream(slideMap));
            System.out.println("Parsing " + slideMap);
            while (s.hasNext()) {
                // parse md5sum/sha1sum/openslide-quickhash1sum (sha256) output
                if (s.findInLine("(\\w{32,64}) [ *](.+)") != null) {
                    MatchResult result = s.match();
                    String hash = result.group(1);
                    String file = result.group(2);
                    slideHashMap.put(hash, file);
                }
                s.nextLine();
            }
            s.close();
        } catch (IOException ignore) {
            System.out.println("Failed to parse " + slideMap);
        }

        list = new JList();
        list.setCellRenderer(new SearchPanelCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(1);

        add(new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

        stats = new StatisticsBar();
        add(stats, BorderLayout.SOUTH);

        int height = 200 + stats.getPreferredSize().height;
        setPreferredSize(new Dimension(200, height));

        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                ResultIcon r = (ResultIcon) list.getSelectedValue();

                if (r == null) {
                    pf.clearResult();
                } else {
                    Cursor oldCursor = pf.getCursor();
                    try {
                        pf.setCursor(Cursor
                                .getPredefinedCursor(Cursor.WAIT_CURSOR));

                        String quickhash1 = r.getQuickHash1();
                        if (quickhash1 == null) {
                            JOptionPane.showMessageDialog(pf,
                                    "Result is not a whole-slide image.",
                                    "PathFind", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        String slidefile = slideHashMap.get(quickhash1);
                        if (slidefile == null) {
                            JOptionPane.showMessageDialog(pf,
                                    "Could not locate whole-slide image.",
                                    "PathFind", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        OpenSlide slide = new OpenSlide(new File(slidefile));

                        long width = slide.getLayer0Width();
                        Path2D tile = r.getTileBounds(width);

                        pf.setResult(slide, slidefile, tile);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } finally {
                        pf.setCursor(oldCursor);
                    }
                }
            }
        });

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 2) {
                    int index = list.locationToIndex(e.getPoint());
                    if (index == -1) {
                        return;
                    }

                    DefaultListModel model = (DefaultListModel) list.getModel();
                    model.removeElementAt(index);
                }
                if (e.getButton() == 3) {
                    int index = list.locationToIndex(e.getPoint());
                    if (index == -1) {
                        return;
                    }

                    DefaultListModel model = (DefaultListModel) list.getModel();
                    ResultIcon r = (ResultIcon) model.elementAt(index);

                    String qh1 = r.getQuickHash1();
                    try {
                        String descr = annotationStore.getDescription(qh1);
                        popupCaseInfo("<pre>" + descr + "</pre>");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    private void popupCaseInfo(String fullInfo) {
        JFrame j = new JFrame("Case Report");

        JEditorPane text = new JEditorPane();
        text.setEditable(false);
        text.setDocument(new HTMLDocument());
        text.setEditorKit(new HTMLEditorKit());
        text.setText("<html>" + fullInfo + "</html>");

        JScrollPane jsp = new JScrollPane(text);
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        jsp.setPreferredSize(new Dimension(640, 480));

        j.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        j.add(jsp);
        j.pack();
        j.setVisible(true);
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

    private void startStatsTimer() {
        timerExecutor = Executors.newSingleThreadScheduledExecutor();
        statsTimerFuture = timerExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    updateStats();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    void beginSearch(final SearchFactory factory)
            throws IOException, InterruptedException {

        Set<String> attributes = new HashSet<String>();
        attributes.add("thumbnail.jpeg");
        attributes.add("openslide.quickhash-1");
        attributes.add("algum.tile-bounds");

        final Search s = factory.createSearch(attributes);
        if (theSearch != null) {
            theSearch.close();
        }

        theSearch = s;

        startStatsTimer();

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

                            byte[] quickhash1 = r.getValue("openslide.quickhash-1");
                            byte[] tilebounds = r.getValue("algum.tile-bounds");

                            final ResultIcon resultIcon = new ResultIcon(
                                    new ImageIcon(thumb),
                                    r.getObjectIdentifier(),
                                    quickhash1, tilebounds);

                            publish(resultIcon);
                        }
                    } finally {
                        System.out.println("STOP");

                        updateStats();

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

                                qp.setSearchRunning(false);

                                if (timerExecutor != null) {
                                    timerExecutor.shutdownNow();
                                }

                                if (statsTimerFuture != null) {
                                    statsTimerFuture.cancel(true);
                                }
                                stats.setDone();
                            }
                        });
                    }
                } catch (final IOException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            stats.showException(e.getCause());
                        }
                    });
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
        qp.setSearchRunning(true);
        workerFuture.execute();
        setVisible(true);
    }

    void endSearch() throws InterruptedException {
        if (theSearch != null) {
            theSearch.close();
        }
    }

    private void updateStats() throws IOException, InterruptedException {
        try {
            final Map<String, ServerStatistics> serverStats =
                theSearch.getStatistics();

            boolean hasStats = false;
            for (ServerStatistics s : serverStats.values()) {
                if (s.getTotalObjects() != 0) {
                    hasStats = true;
                    break;
                }
            }
            if (hasStats) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        stats.update(serverStats);
                    }
                });
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        stats.setIndeterminateMessage("Waiting for First Results");
                    }
                });
            }
        } catch (SearchClosedException ignore) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    stats.setDone();
                }
            });
        }
    }
}
