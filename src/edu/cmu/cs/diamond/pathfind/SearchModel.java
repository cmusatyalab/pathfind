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

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;

import edu.cmu.cs.diamond.opendiamond.Result;
import edu.cmu.cs.diamond.opendiamond.Search;
import edu.cmu.cs.diamond.opendiamond.SearchEvent;
import edu.cmu.cs.diamond.opendiamond.SearchEventListener;

final public class SearchModel extends AbstractListModel implements
        SearchEventListener {
    protected volatile boolean running;

    final protected Search search;

    final protected int limit;

    final protected Object lock = new Object();

    final protected List<Shape> list = new LinkedList<Shape>();

    public SearchModel(Search search, int limit) {
        this.search = search;
        this.limit = limit;

        search.addSearchEventListener(this);

        Thread t = new Thread(new Runnable() {
            public void run() {
                // wait for start
                synchronized (lock) {
                    while (!running) {
                        try {
                            System.out.println("waiting for start signal");
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    int i = 0;
                    while (running && i < SearchModel.this.limit) {
                        final Result r = SearchModel.this.search
                                .getNextResult();
                        if (r == null) {
                            break;
                        }

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                System.out.println(" *** adding " + r);

                                String name = r.getObjectName();
                                
                                list.add(new Rectangle(100, 100, 100, 100));
                                int index = list.size();
                                fireIntervalAdded(SearchModel.this, index,
                                        index);
                            }
                        });
                        i++;
                    }
                } catch (InterruptedException e) {
                } finally {
                    System.out.println("search done");
                    running = false;
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void searchStarted(SearchEvent e) {
        synchronized (lock) {
            System.out.println("sending start notify");
            running = true;
            lock.notify();
        }
    }

    public void searchStopped(SearchEvent e) {
        running = false;
    }

    public void removeSearchListener() {
        search.removeSearchEventListener(this);
    }

    public Object getElementAt(int index) {
        return list.get(index);
    }

    public int getSize() {
        return list.size();
    }
}
