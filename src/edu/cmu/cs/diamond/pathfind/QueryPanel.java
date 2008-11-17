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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.*;

import edu.cmu.cs.diamond.opendiamond.Util;

public final class QueryPanel extends JPanel {
    public final String[] ijCmd;

    public final String ijDir;

    public class Macro {
        private final String name;

        private final String macroName;

        public Macro(String name, String macroName) {
            this.name = name;
            this.macroName = macroName;
        }

        @Override
        public String toString() {
            return name;
        }

        public double runMacro() {
            // make hourglass
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            File imgFile = null;
            double result = Double.NaN;

            try {
                // grab image
                BufferedImage img = pf.getSelectionAsImage();
                // JFrame jf = new JFrame();
                // jf.add(new JLabel(new ImageIcon(img)));
                // jf.setVisible(true);
                // jf.pack();

                // write tmp image
                BufferedOutputStream out = null;
                try {
                    imgFile = File.createTempFile("pathfind", ".ppm");
                    imgFile.deleteOnExit();
                    out = new BufferedOutputStream(
                            new FileOutputStream(imgFile));

                    out.write("P6\n".getBytes());
                    out.write(Integer.toString(img.getWidth()).getBytes());
                    out.write('\n');
                    out.write(Integer.toString(img.getHeight()).getBytes());
                    out.write("\n255\n".getBytes());

                    for (int y = 0; y < img.getHeight(); y++) {
                        for (int x = 0; x < img.getWidth(); x++) {
                            int pixel = img.getRGB(x, y);
                            out.write((pixel >> 16) & 0xFF);
                            out.write((pixel >> 8) & 0xFF);
                            out.write(pixel & 0xFF);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // run macro
                List<String> processArgs = new ArrayList<String>();
                processArgs.addAll(Arrays.asList(ijCmd));
                processArgs.add(imgFile.getPath());
                processArgs.add("-batch");
                processArgs.add(macroName);
                ProcessBuilder pb = new ProcessBuilder(processArgs);
                pb.directory(new File(ijDir));

                try {
                    StringBuilder sb = new StringBuilder();
                    Process p = pb.start();
                    BufferedInputStream pOut = new BufferedInputStream(p
                            .getInputStream());
                    int data;
                    while ((data = pOut.read()) != -1) {
                        sb.append((char) data);
                    }

                    pOut.close();

                    String sr = sb.toString();
                    System.out.println(sr);
                    String srr[] = sr.split("\n");
                    result = Double.parseDouble(srr[2]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } finally {
                // delete temp
                if (imgFile != null) {
                    imgFile.delete();
                }

                // reset hourglass
                setCursor(null);
            }
            return result / 10000.0;
        }
    }

    private final PathFind pf;

    private final JComboBox macroComboBox;

    private final JLabel resultField;

    private double result = Double.NaN;

    private final JButton computeButton;

    private final JButton searchButton;

    private final JButton stopButton;

    private final JSpinner searchBound;

    private final Macro macroList[] = createMacroList();

    private final String extraPluginsDir;

    public QueryPanel(PathFind pathFind, String ijDir, String extraPluginsDir,
            String jreDir) {
        this.ijDir = ijDir;

        this.extraPluginsDir = extraPluginsDir;

        this.ijCmd = new String[] {
                new File(jreDir + File.separator + "bin" + File.separator
                        + "java").getAbsolutePath(), "-jar", "ij.jar" };

        setLayout(new BorderLayout());

        pf = pathFind;

        Box b = Box.createHorizontalBox();

        // add macro list
        // TODO: dynamic list
        macroComboBox = new JComboBox(macroList);
        b.add(macroComboBox);
        b.add(Box.createHorizontalStrut(10));

        // add result
        resultField = new JLabel();
        resultField.setPreferredSize(new Dimension(100, 1));
        clearResult();
        b.add(resultField);

        // add compute button
        computeButton = new JButton("Calculate");
        computeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                result = macroList[macroComboBox.getSelectedIndex()].runMacro();
                updateResultField();
            }
        });
        b.add(computeButton);
        b.add(Box.createHorizontalStrut(10));

        // add divider
        b.add(new JSeparator(SwingConstants.VERTICAL));

        // add search range
        b.add(new JLabel("Search bound: "));
        searchBound = new JSpinner(new SpinnerNumberModel(0.0, 0.0, null, 0.1));
        b.add(searchBound);
        b.add(Box.createHorizontalStrut(10));

        // add search button
        searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                runRemoteMacro(macroList[macroComboBox.getSelectedIndex()].macroName);
            }
        });
        b.add(searchButton);

        // add stop button
        stopButton = new JButton("Stop");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pf.stopSearch();
            }
        });
        b.add(stopButton);

        b.add(Box.createHorizontalGlue());
        add(b);
    }

    private Macro[] createMacroList() {
        List<Macro> r = new ArrayList<Macro>();

        BufferedReader in = new BufferedReader(new InputStreamReader(getClass()
                .getResourceAsStream("resources/macros.txt")));

        try {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                StringTokenizer t = new StringTokenizer(line, ";");
                r.add(new Macro(t.nextToken(), t.nextToken()));
            }

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return r.toArray(new Macro[0]);
    }

    private void updateResultField() {
        resultField.setText("Result: " + Double.toString(result));
    }

    public void clearResult() {
        result = Double.NaN;
        updateResultField();
    }

    private void runRemoteMacro(String macroName) {
        try {
            File mm = new File(QueryPanel.this.ijDir + "/macros", macroName
                    + ".txt");
            byte blob1[] = Util.quickTar(new File(extraPluginsDir));
            byte blob2[] = Util.quickTar(new File[] { mm });
            byte macroBlob[] = new byte[blob1.length + blob2.length];
            System.arraycopy(blob1, 0, macroBlob, 0, blob1.length);
            System.arraycopy(blob2, 0, macroBlob, blob1.length, blob2.length);
            pf.startSearch(Double.isNaN(result) ? 0.0 : result, macroBlob,
                    macroName);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
