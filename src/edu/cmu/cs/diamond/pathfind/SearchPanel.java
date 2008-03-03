package edu.cmu.cs.diamond.pathfind;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.cmu.cs.diamond.opendiamond.Search;

public class SearchPanel extends JPanel {
    final protected JList list;

    protected Search theSearch;

    final private PathFind pathFind;

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
                WholeslideRegionResult r = (WholeslideRegionResult) list
                        .getSelectedValue();

                if (r == null) {
                    pf.setRightSlide(null, null);
                } else {
                    pf.setRightSlide(r.ws, "Search Result");
                    pf.getRightSlide().setSelection(r.region);
                    pf.getRightSlide().centerOnSelection();
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
                .getWholeslide(), 50));

        theSearch.start();

        setVisible(true);
    }

    void endSearch() {
        if (theSearch != null)
            theSearch.stop();
    }
}
