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
