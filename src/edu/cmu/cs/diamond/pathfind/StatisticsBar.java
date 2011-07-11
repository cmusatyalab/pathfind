/*
 *  PathFind -- a Diamond system for pathology
 *
 *  Copyright (c) 2007-2011 Carnegie Mellon University
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

import java.util.Map;

import javax.swing.JProgressBar;

import edu.cmu.cs.diamond.opendiamond.ServerStatistics;

public class StatisticsBar extends JProgressBar {
    public StatisticsBar() {
        setStringPainted(true);
        clear();
    }

    public void clear() {
        setNumbers(0, 0, 0);
    }

    private void setNumbers(int total, int searched, int dropped) {
        setIndeterminate(false);
        String str = "Total: " + total + ", Searched: " + searched
                + ", Dropped: " + dropped;
        setString(str);
        setMaximum(total);
        setValue(searched);
    }

    public void update(Map<String, ServerStatistics> serverStats) {
        int t = 0;
        int s = 0;
        int d = 0;
        for (ServerStatistics ss : serverStats.values()) {
            t += ss.getTotalObjects();
            s += ss.getProcessedObjects();
            d += ss.getDroppedObjects();
        }
        setNumbers(t, s, d);
    }

    public void setIndeterminateMessage(String message) {
        setIndeterminate(true);
        setString(message);
    }

    public void showException(Throwable e) {
        setIndeterminate(false);
        Throwable c = e.getCause();
        String msg = e.getLocalizedMessage();
        if (c != null) {
            msg += ": " + c;
        }
        setString(msg);
    }

    public void setDone() {
        if (isIndeterminate()) {
            clear();
            setIndeterminate(false);
        }
    }
}
