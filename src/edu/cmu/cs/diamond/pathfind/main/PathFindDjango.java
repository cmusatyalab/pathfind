/*
 *  PathFind -- a Diamond system for pathology
 *
 *  Copyright (c) 2008-2010 Carnegie Mellon University
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

package edu.cmu.cs.diamond.pathfind.main;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.httpclient.HttpClient;

import edu.cmu.cs.diamond.pathfind.AnnotationStore;
import edu.cmu.cs.diamond.pathfind.DjangoAnnotationStore;
import edu.cmu.cs.diamond.pathfind.PathFindFrame;

public class PathFindDjango {

    public static void main(String[] args) {
        if (args.length != 4 && args.length != 5) {
            System.out
                    .println("usage: "
                            + PathFindDjango.class.getName()
                            + " predicate_dir interface_map slide_map annotation_uri");
            return;
        }

        final String predicateDir = args[0];
        final String interfaceMap = args[1];
        final String slideMap = args[2];
        final String annotationUri = args[3];

        final File slide;
        if (args.length == 5) {
            slide = new File(args[4]);
        } else {
            slide = null;
        }

        final AnnotationStore annotationStore = new DjangoAnnotationStore(
                new HttpClient(), annotationUri);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    new PathFindFrame(predicateDir, annotationStore,
                            interfaceMap, slideMap, slide, false);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, e, "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
}
