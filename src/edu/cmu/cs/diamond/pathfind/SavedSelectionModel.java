package edu.cmu.cs.diamond.pathfind;

import javax.swing.DefaultListModel;

import edu.cmu.cs.diamond.wholeslide.gui.WholeslideView;

public class SavedSelectionModel extends DefaultListModel {
    final private WholeslideView w;

    public SavedSelectionModel(WholeslideView w) {
        this.w = w;
    }
}
