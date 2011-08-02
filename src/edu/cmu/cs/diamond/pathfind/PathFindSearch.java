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

import java.io.File;
import java.io.IOException;
import java.util.*;

import edu.cmu.cs.diamond.opendiamond.*;

public class PathFindSearch {
    private final Bundle bundle;

    private PathFindSearch(Bundle bundle) {
        this.bundle = bundle;
    }

    public String getDisplayName() {
        return bundle.getDisplayName();
    }

    // Returns the name of the filter whose score should be reported when
    // "Calculate" is pressed.  Currently this is always the last filter
    // declared in the bundle.
    public String getFilterName() throws IOException {
        List<Filter> filters = getFilters(Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);
        return filters.get(filters.size() - 1).getName();
    }

    public List<Filter> getFilters(double minScore, double maxScore)
            throws IOException {
        // We completely ignore the options declared by the filter bundle.
        // Instead, we provide two hardcoded option values, minScore and
        // maxScore.
        Map<String, String> optionMap = new HashMap<String, String>();
        optionMap.put("minScore", Double.toString(minScore));
        optionMap.put("maxScore", Double.toString(maxScore));
        return bundle.getFilters(optionMap);
    }

    public static List<PathFindSearch> getSearches(File searchDir) {
        List<PathFindSearch> searches = new ArrayList<PathFindSearch>();
        BundleFactory factory = new BundleFactory(Arrays.asList(searchDir),
                Arrays.asList(new File("/usr/share/diamond/filters")));
        for (Bundle b : factory.getBundles()) {
            searches.add(new PathFindSearch(b));
        }
        Collections.sort(searches, new Comparator<PathFindSearch>() {
            @Override
            public int compare(PathFindSearch o1, PathFindSearch o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });
        return searches;
    }
}
