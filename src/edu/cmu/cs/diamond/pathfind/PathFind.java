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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.cmu.cs.diamond.opendiamond.CookieMap;
import edu.cmu.cs.diamond.opendiamond.Filter;
import edu.cmu.cs.diamond.opendiamond.FilterCode;
import edu.cmu.cs.diamond.opendiamond.SearchFactory;
import edu.cmu.cs.openslide.OpenSlide;
import edu.cmu.cs.openslide.gui.OpenSlideView;

public class PathFind extends JFrame {

    private class CreateMacroAction extends AbstractAction {

        public CreateMacroAction() {
            super("New Macro...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String newName = JOptionPane.showInputDialog(PathFind.this,
                    "Enter the name of the new macro:");
            if (newName == null) {
                return;
            }

            qp.populateMacroListModel();
        }
    }

    private static class ExitAction extends AbstractAction {
        public ExitAction() {
            super("Exit");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }

    private final SearchPanel searchPanel;

    private final JPanel selectionPanel;

    private final JList savedSelections;

    private DefaultListModel ssModel;

    private final QueryPanel qp;

    private final PairedSlideView psv = new PairedSlideView();

    public PathFind(String ijDir, String extraPluginsDir, String jreDir,
            File slide) throws FileNotFoundException {
        super("PathFind");
        setSize(1000, 750);
        setMinimumSize(new Dimension(1000, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JFileChooser jfc = new JFileChooser();
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.setFileFilter(OpenSlide.getFileFilter());

        if (slide == null) {
            int returnVal = jfc.showDialog(this, "Open");
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                slide = jfc.getSelectedFile();
            } else {
                System.exit(0);
            }
        }

        OpenSlide os = new OpenSlide(slide);

        // slides in middle
        add(psv);

        // query bar at bottom
        qp = new QueryPanel(this, ijDir, extraPluginsDir, jreDir);
        add(qp, BorderLayout.SOUTH);

        // menubar
        setJMenuBar(createMenuBar());

        // search results at top
        searchPanel = new SearchPanel(this);
        searchPanel.setVisible(false);
        add(searchPanel, BorderLayout.NORTH);

        // save selections at left
        selectionPanel = new JPanel(new BorderLayout());
        savedSelections = new JList();
        savedSelections.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        savedSelections.setLayoutOrientation(JList.VERTICAL);
        selectionPanel.add(new JScrollPane(savedSelections,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        selectionPanel.setBorder(BorderFactory
                .createTitledBorder("Saved Selections"));
        selectionPanel.setPreferredSize(new Dimension(280, 100));
        savedSelections.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                Shape selection = (Shape) savedSelections.getSelectedValue();
                psv.getSlide().setSelection(selection);
                psv.getSlide().centerOnSelection();
            }
        });
        add(selectionPanel, BorderLayout.WEST);

        setSlide(os, slide.getName());
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu m = new JMenu("Macros");
        mb.add(m);

        m.add(new CreateMacroAction());

        m.add(new JSeparator());
        m.add(new ExitAction());

        return mb;
    }

    public void startSearch(double threshold, byte[] macroBlob, String macroName)
            throws IOException, InterruptedException {
        System.out.println("start search");

        SearchFactory factory = createFactory(threshold, macroBlob, macroName);

        searchPanel.beginSearch(factory.createSearch(null));
    }

    public void stopSearch() throws InterruptedException {
        searchPanel.endSearch();
    }

    private SearchFactory createFactory(double threshold, byte[] macroBlob,
            String macroName) throws IOException {
        List<Filter> filters = new ArrayList<Filter>();
        String macroName2 = macroName.replace(' ', '_');

        FilterCode c = new FilterCode(new FileInputStream(
                "/opt/snapfind/lib/fil_imagej_exec.so"));
        List<String> dependencies = Collections.emptyList();
        List<String> arguments = Arrays.asList(new String[] { macroName2 });
        Filter imagej = new Filter("imagej", c, "f_eval_imagej_exec",
                "f_init_imagej_exec", "f_fini_imagej_exec",
                (int) (threshold * 10000), dependencies, arguments, macroBlob);
        System.out.println(imagej);

        filters.add(imagej);

        // make a new factory
        SearchFactory factory = new SearchFactory(filters, Arrays
                .asList(new String[] { "imagej" }), CookieMap
                .createDefaultCookieMap());
        return factory;
    }

    void setSlide(OpenSlide openslide, String title) {
        final OpenSlideView wv = createNewView(openslide, title, true);

        psv.setSlide(wv);
        wv.getInputMap()
                .put(KeyStroke.getKeyStroke("INSERT"), "save selection");
        wv.getActionMap().put("save selection", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                saveSelection(wv);
            }
        });
        ssModel = new SavedSelectionModel(wv);
        savedSelections.setModel(ssModel);
        savedSelections.setCellRenderer(new SavedSelectionCellRenderer(wv));
    }

    void setResult(Icon result, String title) {
        psv.setResult(result);
    }

    protected void saveSelection(OpenSlideView wv) {
        Shape s = wv.getSelection();
        if (s != null) {
            ssModel.addElement(s);
        }
    }

    private OpenSlideView createNewView(OpenSlide openslide, String title,
            boolean zoomToFit) {
        OpenSlideView wv = new OpenSlideView(openslide, zoomToFit);
        wv.setBorder(BorderFactory.createTitledBorder(title));
        return wv;
    }

    public static void main(String[] args) {
        if (args.length != 3 && args.length != 4) {
            System.out.println("usage: " + PathFind.class.getName()
                    + " ij_dir extra_plugins_dir jre_dir");
            return;
        }

        final String ijDir = args[0];
        final String extraPluginsDir = args[1];
        final String jreDir = args[2];

        final File slide;
        if (args.length == 4) {
            slide = new File(args[3]);
        } else {
            slide = null;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                PathFind pf;
                try {
                    pf = new PathFind(ijDir, extraPluginsDir, jreDir, slide);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }
                pf.setVisible(true);
            }
        });
    }

    public BufferedImage getSelectionAsImage() {
        Shape s = psv.getSlide().getSelection();
        if (s == null) {
            return null;
        }

        Rectangle2D bb = s.getBounds2D();
        if (bb.getWidth() * bb.getHeight() > 6000 * 6000) {
            throw new SelectionTooBigException();
        }

        // move selection
        AffineTransform at = AffineTransform.getTranslateInstance(-bb.getX(),
                -bb.getY());
        s = at.createTransformedShape(s);

        BufferedImage img = new BufferedImage((int) bb.getWidth(), (int) bb
                .getHeight(), BufferedImage.TYPE_INT_RGB);

        Graphics2D g = img.createGraphics();
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, img.getWidth(), img.getHeight());
        g.clip(s);
        psv.getSlide().getOpenSlide().paintRegion(g, 0, 0, (int) bb.getX(),
                (int) bb.getY(), img.getWidth(), img.getHeight(), 1.0);
        g.dispose();

        return img;
    }

    public OpenSlideView getSlide() {
        return psv.getSlide();
    }
}
