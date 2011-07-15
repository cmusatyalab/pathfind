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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import edu.cmu.cs.diamond.opendiamond.*;

public class PathFindSearch {
    private final File macro;

    private final File extraPluginsDir;

    private PathFindSearch(File macro, File extraPluginsDir) {
        this.macro = macro;
        this.extraPluginsDir = extraPluginsDir;
    }

    public static PathFindSearch fromFile(File file, File extraPluginsDir) {
        // accept only relevant file extensions
        String lc = file.getName().toLowerCase();
        if (!lc.endsWith(".js") && !lc.endsWith(".txt")
                && !lc.endsWith(".ijm")) {
            return null;
        }

        return new PathFindSearch(file, extraPluginsDir);
    }

    public String getDisplayName() {
        return macro.getName();
    }

    public List<Filter> getFilters(double minScore, double maxScore)
            throws IOException {
        List<Filter> filters = new ArrayList<Filter>();

        FileInputStream in = new FileInputStream("/opt/snapfind/lib/fil_imagej_exec");
        try {
            FilterCode c = new FilterCode(in);
            List<String> dependencies = Collections.emptyList();
            List<String> arguments = Arrays.asList(new String[] {
                    macro.getName() });
            Filter imagej = new Filter("primary", c, minScore, maxScore,
                    dependencies, arguments, encodeMacro(extraPluginsDir));
            filters.add(imagej);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }

        return filters;
    }

    private void encodeResource(ZipOutputStream zos, File file) throws
            IOException {
        ZipEntry ze = new ZipEntry(file.getName());
        // gratuitously storing different timestamps on every run would
        // defeat server-side result caching
        ze.setTime(0);
        zos.putNextEntry(ze);
        // stream data into the archive
        FileInputStream fis = new FileInputStream(file);
        byte bb[] = new byte[4096];
        int amount;
        while ((amount = fis.read(bb)) != -1) {
            zos.write(bb, 0, amount);
        }
        fis.close();
    }

    private byte[] encodeMacro(File extraPluginsDir) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        encodeResource(zos, macro);
        for (File file : extraPluginsDir.listFiles()) {
            if (!file.getName().equals(macro.getName())) {
                encodeResource(zos, file);
            }
        }
        zos.close();
        return baos.toByteArray();
    }
}
