/*
 *  PathFind -- a Diamond system for pathology
 *
 *  Copyright (c) 2008-2011 Carnegie Mellon University
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
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.antlr.stringtemplate.StringTemplate;

import edu.cmu.cs.diamond.opendiamond.*;
import edu.cmu.cs.openslide.OpenSlide;
import edu.cmu.cs.openslide.gui.Annotation;
import edu.cmu.cs.openslide.gui.OpenSlideView;
import edu.cmu.cs.openslide.gui.SelectionListModel;

public class PathFindFrame extends JFrame {

    private final SearchPanel searchPanel;

    private final JPanel selectionPanel;

    private final JList savedSelections;

    private SelectionListModel ssModel;

    private final QueryPanel qp;

    private final PairedSlideView psv = new PairedSlideView();

    private CookieMap cookieMap = CookieMap.emptyCookieMap();

    private final JFileChooser jfc = new JFileChooser();

    private final JButton selectionDelete;

    private final JButton selectionEditText;

    private final String bookmarkLabelTemplate;

    private final String bookmarkHoverTemplate;

    private final String regionHoverTemplate;

    private final String bookmarkDoubleClickTemplate;

    private final AnnotationStore annotationStore;

    private final SecondWindow secondWindow;

    public PathFindFrame(String searchDir, AnnotationStore annotationStore,
            String interfaceMap, String slideMap, File slide,
            boolean twoWindowMode) throws IOException {
        super("PathFind");
        setSize(1000, 750);
        setMinimumSize(new Dimension(1000, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.annotationStore = annotationStore;

        jfc.setAcceptAllFileFilterUsed(false);
        jfc.setFileFilter(OpenSlide.getFileFilter());

        try {
            cookieMap = CookieMap.createDefaultCookieMap();
        } catch (IOException e) {
            // non-fatal if the scope has not yet been generated
            e.printStackTrace();
        }

        // slides in middle
        add(psv);

        // query bar at bottom
        qp = new QueryPanel(this, new File(searchDir));
        add(qp, BorderLayout.SOUTH);

        // search results at top
        searchPanel = new SearchPanel(this, qp, slideMap);
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
        savedSelections.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = savedSelections.locationToIndex(e.getPoint());
                    if (index == -1) {
                        return;
                    }

                    SlideAnnotation ann = (SlideAnnotation) savedSelections
                            .getModel().getElementAt(index);
                    popupInfo(ann);
                }
            }

            private void popupInfo(SlideAnnotation ann) {
                JFrame j = new JFrame("Annotation Info");

                JEditorPane text = new JEditorPane();
                text.setEditable(false);
                text.setDocument(new HTMLDocument());
                text.setEditorKit(new HTMLEditorKit());
                StringTemplate info = new StringTemplate(
                        bookmarkDoubleClickTemplate);

                // TODO show all notes, not just last
                List<SlideAnnotationNote> list = ann.getNotes();
                if (!list.isEmpty()) {
                    list.get(list.size() - 1).populateStringTemplateAttributes(
                            info);
                }

                text.setText(info.toString());
                JScrollPane jsp = new JScrollPane(text);
                jsp
                        .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                jsp.setPreferredSize(new Dimension(640, 480));

                j.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                j.add(jsp);
                j.pack();
                j.setVisible(true);
            }
        });

        if (twoWindowMode) {
            // default hover won't work
            savedSelections.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    secondWindow.setHover(null);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    int index = savedSelections.locationToIndex(e.getPoint());
                    if (index == -1) {
                        secondWindow.setHover(null);
                    } else {
                        SlideAnnotation ann = (SlideAnnotation) savedSelections
                                .getModel().getElementAt(index);
                        StringTemplate hover = new StringTemplate(
                                bookmarkHoverTemplate);

                        // TODO show all notes, not just last
                        List<SlideAnnotationNote> list = ann.getNotes();
                        if (!list.isEmpty()) {
                            list.get(list.size() - 1)
                                    .populateStringTemplateAttributes(hover);
                        }

                        secondWindow.setHover(hover.toString());
                    }
                }

                // TODO update hover when list scrolls/changes without mouse
            });
        }

        // edit/delete buttons
        JPanel selectionButtons = new JPanel(new FlowLayout());

        selectionDelete = new JButton("Delete");
        selectionDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ssModel.remove(savedSelections.getSelectedIndex());
            }
        });
        selectionButtons.add(selectionDelete);

        selectionEditText = new JButton("Edit Text");
        selectionEditText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SlideAnnotation a = (SlideAnnotation) savedSelections
                        .getSelectedValue();

                // TODO show all notes, not just last
                SlideAnnotationNote oldNote = null;
                String oldText = null;
                Integer id = null;

                id = a.getId();

                List<SlideAnnotationNote> list = a.getNotes();
                if (!list.isEmpty()) {
                    oldNote = list.get(list.size() - 1);
                    oldText = oldNote.getText();
                }

                final String newText = JOptionPane.showInputDialog(
                        PathFindFrame.this, "Enter text:", oldText);

                if ((newText != null) && (!newText.equals(oldText))) {
                    int index = savedSelections.getSelectedIndex();
                    List<SlideAnnotationNote> list2 = new ArrayList<SlideAnnotationNote>();
                    list2.add(new SlideAnnotationNote(newText));
                    ssModel.replace(index, new SlideAnnotation(a.getShape(),
                            id, null, list2));
                    savedSelections.setSelectedIndex(index);
                }
            }
        });
        selectionButtons.add(selectionEditText);

        updateSelectionButtons();

        savedSelections.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateSelectionButtons();
                psv.getSlide().centerOnSelection(
                        savedSelections.getSelectedIndex());
            }
        });

        selectionPanel.add(selectionButtons, BorderLayout.SOUTH);

        add(selectionPanel, BorderLayout.WEST);

        // read interface properties
        Properties interfaceProperties = new Properties();
        InputStream in = new BufferedInputStream(new FileInputStream(
                interfaceMap));
        try {
            interfaceProperties.load(in);
        } finally {
            try {
                in.close();
            } catch (IOException ignore) {
            }
        }

        bookmarkLabelTemplate = interfaceProperties
                .getProperty("bookmark-label-template");
        bookmarkHoverTemplate = interfaceProperties
                .getProperty("bookmark-hover-template");
        bookmarkDoubleClickTemplate = interfaceProperties
                .getProperty("bookmark-doubleclick-template");
        regionHoverTemplate = interfaceProperties
                .getProperty("region-hover-template");

        // second window?
        if (twoWindowMode) {
            secondWindow = new SecondWindow();
        } else {
            secondWindow = null;
        }

        if (slide != null) {
            setSlide(slide);
        }

        if (secondWindow != null) {
            secondWindow.setMinimumSize(new Dimension(100, 500));
            secondWindow.pack();
            secondWindow.setVisible(true);
        }
        setVisible(true);
    }

    private void updateSelectionButtons() {
        boolean selected = savedSelections.getSelectedIndex() != -1;
        selectionDelete.setEnabled(selected);
        selectionEditText.setEnabled(selected);
    }

    private void setSlide(File slide) throws IOException {
        OpenSlide os = new OpenSlide(slide);
        setSlide(os, slide.getName());
    }

    public void openCase() {
        int returnVal = jfc.showDialog(PathFindFrame.this, "Open");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File slide = jfc.getSelectedFile();
            try {
                setSlide(slide);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void defineScope() {
        try {
            cookieMap = CookieMap.createDefaultCookieMap();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void startSearch(double minScore, double maxScore,
            PathFindSearch search) throws IOException, InterruptedException {
        System.out.println("start search");

        SearchFactory factory = createFactory(minScore, maxScore, search);

        searchPanel.beginSearch(factory);
    }

    public void stopSearch() throws InterruptedException {
        searchPanel.endSearch();
    }

    public double generateScore(PathFindSearch search, byte[] data)
            throws IOException, InterruptedException {
        System.out.println("generate score");

        SearchFactory factory = createFactory(Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, search);

        String attr = "_filter." + search.getFilterName() + "_score";
        Set<String> desiredAttributes = new HashSet<String>();
        desiredAttributes.add(attr);

        Result result = factory.generateResult(data, desiredAttributes);
        byte[] value = result.getValue(attr);
        if (value != null) {
            return Double.valueOf(Util.extractString(value));
        } else {
            return Double.NaN;
        }
    }

    private SearchFactory createFactory(double minScore, double maxScore,
            PathFindSearch search) throws IOException {
        List<Filter> filters = new ArrayList<Filter>();

        InputStream in = null;

        filters.addAll(search.getFilters(minScore, maxScore));

        try {
            in = new FileInputStream("/usr/share/diamond/filters/fil_rgb");
            FilterCode c = new FilterCode(in);
            List<String> dependencies = Collections.emptyList();
            List<String> arguments = Collections.emptyList();
            Filter rgb = new Filter("RGB", c, 1, dependencies, arguments);
            filters.add(rgb);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }

        try {
            in = new FileInputStream("/usr/share/diamond/filters/fil_thumb");
            FilterCode c = new FilterCode(in);
            List<String> dependencies = Arrays.asList(new String[] { "RGB" });
            List<String> arguments = Arrays
                    .asList(new String[] { "200", "150" });
            Filter thumb = new Filter("thumbnail", c, 1, dependencies,
                    arguments);
            filters.add(thumb);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }

        // make a new factory
        SearchFactory factory = new SearchFactory(filters, cookieMap);
        return factory;
    }

    void setSlide(OpenSlide openslide, String title) throws IOException {
        final OpenSlideView wv = createNewView(openslide, title, true);

        psv.setSlide(wv);
        if (secondWindow != null) {
            secondWindow.setCellRenderer(new SavedSelectionCellRenderer(
                    openslide, bookmarkLabelTemplate, bookmarkHoverTemplate,
                    ShowGraphicsOrText.SHOW_TEXT));
            savedSelections.setCellRenderer(new SavedSelectionCellRenderer(
                    openslide, bookmarkLabelTemplate, bookmarkHoverTemplate,
                    ShowGraphicsOrText.SHOW_GRAPHICS));

        } else {
            savedSelections.setCellRenderer(new SavedSelectionCellRenderer(
                    openslide, bookmarkLabelTemplate, bookmarkHoverTemplate,
                    ShowGraphicsOrText.SHOW_GRAPHICS_AND_TEXT));
        }
        final String qh1 = openslide.getProperties().get(
                OpenSlide.PROPERTY_NAME_QUICKHASH1);

        loadAnnotations(wv, qh1);

        psv.revalidate();
        psv.repaint();
    }

    private void loadAnnotations(final OpenSlideView wv, final String qh1)
            throws IOException {
        ssModel = annotationStore.getAnnotations(qh1);
        wv.setSelectionListModel(ssModel);
        savedSelections.setModel(ssModel);
        if (secondWindow != null) {
            secondWindow.setModel(ssModel);
        }
        ssModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalRemoved(ListDataEvent e) {
                saveAndLoadAnnotations();
            }

            private void saveAndLoadAnnotations() {
                try {
                    annotationStore.saveAnnotations(qh1, ssModel);
                    loadAnnotations(wv, qh1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void intervalAdded(final ListDataEvent e) {
                saveAndLoadAnnotations();

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        savedSelections.setSelectedIndex(e.getIndex1());
                    }
                });
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                saveAndLoadAnnotations();
            }
        });
    }

    void setResult(OpenSlide slide, String title, Shape shape) {
        final OpenSlideView wv = createNewView(slide, title, false);
        wv.addSelection(shape);

        psv.setResult(wv);
        psv.revalidate();
        psv.repaint();
    }

    void clearResult() {
        psv.setResult(null);
        psv.revalidate();
        psv.repaint();
    }

    private OpenSlideView createNewView(OpenSlide openslide, String title,
            boolean zoomToFit) {
        final OpenSlideView wv = new OpenSlideView(openslide, zoomToFit);
        wv.setBorder(BorderFactory.createTitledBorder(title));

        wv.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int x = (int) wv.getSlideX(e.getX());
                int y = (int) wv.getSlideY(e.getY());
                int selection = wv.getSelectionForPoint(x, y);
                if (selection != -1) {
                    StringTemplate hover = new StringTemplate(
                            regionHoverTemplate);

                    // TODO show all notes, not just last
                    Annotation ann = ssModel.get(selection);
                    if (ann instanceof SlideAnnotation) {
                        SlideAnnotation sa = (SlideAnnotation) ann;

                        List<SlideAnnotationNote> list = sa.getNotes();
                        if (!list.isEmpty()) {
                            list.get(list.size() - 1)
                                    .populateStringTemplateAttributes(hover);
                        }
                    }

                    setHover(wv, hover.toString());
                } else {
                    setHover(wv, null);
                }
            }
        });

        return wv;
    }

    private void setHover(OpenSlideView wv, String hover) {
        if (secondWindow != null) {
            secondWindow.setHover(hover);
        } else {
            wv.setToolTipText(hover);
        }
    }

    BufferedImage getSelectionAsImage() throws IOException {
        SlideAnnotation a = (SlideAnnotation) savedSelections
                .getSelectedValue();
        if (a == null) {
            return null;
        }
        Shape s = a.getShape();

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
