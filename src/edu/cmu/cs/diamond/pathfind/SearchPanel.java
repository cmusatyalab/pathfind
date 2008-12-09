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
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import edu.cmu.cs.diamond.opendiamond.Search;
import edu.cmu.cs.openslide.OpenSlide;

public class SearchPanel extends JPanel {
    final protected JList list;

    protected Search theSearch;

    final private PathFind pathFind;

    final String trestleDir;

    final private String sqlHost;

    final private String sqlDB;

    final private String sqlUser;

    final private String sqlPassword;

    public SearchPanel(final PathFind pf, String trestleDir, String sqlHost,
            String sqlDB, String sqlUser, String sqlPassword) {
        pathFind = pf;
        this.trestleDir = trestleDir;
        this.sqlHost = sqlHost;
        this.sqlDB = sqlDB;
        this.sqlUser = sqlUser;
        this.sqlPassword = sqlPassword;

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
                OpenSlideRegionResult r = (OpenSlideRegionResult) list
                        .getSelectedValue();

                if (r == null) {
                    pf.setRightSlide(null, null);
                } else {
                    pf.setRightSlide(new OpenSlide(r.ws), "Search Result");
                    pf.getRightSlide().setSelection(r.region);
                    pf.getRightSlide().centerOnSelection();
                }
            }
        });

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = list.locationToIndex(e.getPoint());
                    if (index == -1) {
                        return;
                    }

                    OpenSlideRegionResult r = (OpenSlideRegionResult) list
                            .getModel().getElementAt(index);
                    popupCaseInfo(r.fullInfo);
                } else if (e.getButton() == 2) {
                    int index = list.locationToIndex(e.getPoint());
                    if (index == -1) {
                        return;
                    }

                    SearchModel model = (SearchModel) list.getModel();
                    model.removeElement(index);
                }
            }

            private void popupCaseInfo(String fullInfo) {
                JFrame j = new JFrame("Case Report");

                JEditorPane text = new JEditorPane();
                text.setEditable(false);
                text.setDocument(new HTMLDocument());
                text.setEditorKit(new HTMLEditorKit());
                text.setText("<html>" + fullInfo + "</html>");

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
    }

    @Override
    public void setVisible(boolean visible) {
        boolean oldVisible = isVisible();

        super.setVisible(visible);
        if (oldVisible != visible && !visible) {
            if (theSearch != null) {
                theSearch.stop();
            }

            // deregister listeners
            deregisterListener();
        }
    }

    private void deregisterListener() {
        ListModel oldModel = list.getModel();
        if (oldModel instanceof SearchModel) {
            SearchModel m = (SearchModel) oldModel;
            m.removeSearchListener();
        }
    }

    void beginSearch(Search s) {
        if (theSearch != null) {
            theSearch.stop();
        }

        theSearch = s;

        deregisterListener();

        list.setModel(new SearchModel(theSearch, pathFind.getLeftSlide()
                .getOpenSlide(), 50, trestleDir, sqlHost, sqlDB, sqlUser,
                sqlPassword));

        theSearch.start();

        setVisible(true);
    }

    void endSearch() {
        if (theSearch != null) {
            theSearch.stop();
        }

        deregisterListener();
    }
}
