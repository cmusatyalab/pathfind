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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import edu.cmu.cs.diamond.opendiamond.*;

public class PathFindSearch {
    private final File bundle;

    private final String displayName;

    private PathFindSearch(File bundle, Map<String, byte[]> zipMap,
            Properties p) {
        this.bundle = bundle;
        this.displayName = p.getProperty("Name");
    }

    public String getDisplayName() {
        return displayName;
    }

    // Returns the name of the filter whose score should be reported when
    // "Calculate" is pressed.
    public String getFilterName() {
        return "imagej";
    }

    public List<Filter> getFilters(double minScore, double maxScore)
            throws IOException {
        List<Filter> filters = new ArrayList<Filter>();

        // reread the bundle in case it changed since PathFind was started
        FileInputStream in = new FileInputStream(bundle);
        Map<String, byte[]> zipMap = Util.readZipFile(in);
        Properties p = Util.extractManifest(zipMap);
        String macro = p.getProperty("Macro");

        in = new FileInputStream("/usr/share/diamond/filters/fil_imagej_exec");
        try {
            FilterCode c = new FilterCode(in);
            List<String> dependencies = Collections.emptyList();
            List<String> arguments = Arrays.asList(new String[] { macro });
            Filter imagej = new Filter("imagej", c, minScore, maxScore,
                    dependencies, arguments, Util.encodeZipFile(zipMap));
            filters.add(imagej);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }

        return filters;
    }

    public static List<PathFindSearch> getSearches(File searchDir) {
        List<PathFindSearch> searches = new ArrayList<PathFindSearch>();
        for (File f : searchDir.listFiles()) {
            PathFindSearch search = fromFile(f);
            if (search != null) {
                searches.add(search);
            }
        }
        Collections.sort(searches, new Comparator<PathFindSearch>() {
            @Override
            public int compare(PathFindSearch o1, PathFindSearch o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });
        return searches;
    }

    private static PathFindSearch fromFile(File file) {
        try {
            FileInputStream in = new FileInputStream(file);
            Map<String, byte[]> zipMap = Util.readZipFile(in);
            Properties p = Util.extractManifest(zipMap);
            if ("ImageJ".equals(p.getProperty("Plugin"))) {
                return new PathFindSearch(file, zipMap, p);
            }
        } catch (IOException e) {
        }
        return null;
    }
}
