/*
 *  PathFind -- a Diamond system for pathology
 *
 *  Copyright (c) 2008-2009 Carnegie Mellon University
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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.cmu.cs.diamond.opendiamond.Util;

public final class QueryPanel extends JPanel {
    public final String[] ijCmd;

    public final File ijDir;

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

        public double runMacro() throws IOException {
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
                pb.directory(ijDir);
                System.out.println(processArgs);

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

                    result = 0.0;
                    for (int i = 0; i < srr.length; i++) {
                        if (srr[i].equals("RESULT")) {
                            result = Double.parseDouble(srr[i+2]);
                        }
                    }
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
            return result;
        }

        public String getMacroName() {
            return macroName;
        }
    }

    private final PathFindFrame pf;

    private final JComboBox macroComboBox;

    private final JLabel resultField;

    private double result = Double.NaN;

    private final JButton openCaseButton;

    private final JButton computeButton;

    private final JButton defineScopeButton;

    private final JButton searchButton;

    private final JButton stopButton;

    private final JSpinner minScore;

    private final JSpinner maxScore;

    private final JCheckBox minScoreEnabled;

    private final JCheckBox maxScoreEnabled;

    private final DefaultComboBoxModel macroListModel = new DefaultComboBoxModel();

    private final File extraPluginsDir;

    private final File macrosDir;

    private boolean running;

    public QueryPanel(PathFindFrame pathFind, File ijDir, File macrosDir,
            File extraPluginsDir, File jreDir) {
        this.ijDir = ijDir;
        this.macrosDir = macrosDir;
        this.extraPluginsDir = extraPluginsDir;

        macrosDir.mkdir();

        System.out.printf("macrosDir: %s\nextraPluginsDir: %s\n", macrosDir,
                extraPluginsDir);

        populateMacroListModel();

        this.ijCmd = new String[] {
                new File(jreDir + File.separator + "bin" + File.separator
                        + "java").getAbsolutePath(), "-jar", "ij.jar" };

        setLayout(new GridBagLayout());

        pf = pathFind;

        JPanel b = new JPanel(new GridBagLayout());
        GridBagConstraints c;

        // add open case button
        openCaseButton = new JButton("Open Case...");
        openCaseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pf.openCase();
            }
        });
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 10);
        b.add(openCaseButton, c);

        // add macro list
        macroComboBox = new JComboBox(macroListModel);
        macroComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                JLabel r = (JLabel) super.getListCellRendererComponent(list,
                        value, index, isSelected, cellHasFocus);

                if (value != null) {
                    r.setText(((File) value).getName());
                }

                return r;
            }
        });
        b.add(macroComboBox);

        // add space
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        b.add(new JLabel(), c);

        // add result
        resultField = new JLabel();
        clearResult();
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 10);
        b.add(resultField, c);

        // add compute button
        computeButton = new JButton("Calculate");
        computeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File f = (File) macroComboBox.getSelectedItem();
                Macro m = new Macro(f.getName(), f.getAbsolutePath());
                try {
                    result = m.runMacro();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                updateResultField();
            }
        });
        b.add(computeButton);

        // add first row
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        add(b, c);
        b = new JPanel(new GridBagLayout());

        // add min score
        JLabel l = new JLabel("Minimum score: ");
        b.add(l);
        minScoreEnabled = new JCheckBox();
        minScoreEnabled.setSelected(true);
        b.add(minScoreEnabled);
        minScore = new JSpinner(new SpinnerNumberModel(0, 0,
                Integer.MAX_VALUE, 0.1));
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(0, 0, 0, 15);
        b.add(minScore, c);
        setGuardCheckBox(minScoreEnabled, l);

        // add max score
        l = new JLabel("Maximum score: ");
        b.add(l);
        maxScoreEnabled = new JCheckBox();
        b.add(maxScoreEnabled);
        maxScore = new JSpinner(new SpinnerNumberModel(0, 0,
                Integer.MAX_VALUE, 0.1));
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.VERTICAL;
        b.add(maxScore, c);
        setGuardCheckBox(maxScoreEnabled, l);

        // add space
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        b.add(new JLabel(), c);

        // add define scope button
        defineScopeButton = new JButton("Define Scope");
        defineScopeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pf.defineScope();
            }
        });
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 2);
        b.add(defineScopeButton, c);

        // add search button
        searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    File f = (File) macroComboBox.getSelectedItem();
                    runRemoteMacro(f.getName());
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 2);
        b.add(searchButton, c);

        // add stop button
        stopButton = new JButton("Stop");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    pf.stopSearch();
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        b.add(stopButton);

        // add second row
        c = new GridBagConstraints();
        c.insets = new Insets(2, 0, 0, 0);
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        add(b, c);

        // set widget enablement
        refresh();
    }

    private static boolean checkBoxSelected(JCheckBox checkBox) {
        return checkBox.getSelectedObjects() != null;
    }

    private void setGuardCheckBox(final JCheckBox guard, final JLabel label) {
        guard.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                refresh();
            }
        });
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                guard.doClick();
            }
        });
    }

    void populateMacroListModel() {
        macroListModel.removeAllElements();

        File[] files = macrosDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lc = name.toLowerCase();
                return lc.endsWith(".js") || lc.endsWith(".txt")
                        || lc.endsWith(".ijm");
            }
        });

        Collections.sort(Arrays.asList(files));

        for (File f : files) {
            macroListModel.addElement(f);
        }
    }

    private void updateResultField() {
        if (!Double.isNaN(result)) {
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(3);
            resultField.setText("Result: " + df.format(result));
        } else {
            resultField.setText("");
        }
    }

    public void clearResult() {
        result = Double.NaN;
        updateResultField();
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

    private void runRemoteMacro(String macroName) throws InterruptedException,
            IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        File macro = new File(QueryPanel.this.ijDir + "/macros", macroName);
        encodeResource(zos, macro);
        for (File file : extraPluginsDir.listFiles()) {
            if (!file.getName().equals(macro.getName())) {
                encodeResource(zos, file);
            }
        }
        zos.close();
        double min = checkBoxSelected(minScoreEnabled) ?
                ((Number) minScore.getValue()).doubleValue() :
                Double.NEGATIVE_INFINITY;
        double max = checkBoxSelected(maxScoreEnabled) ?
                ((Number) maxScore.getValue()).doubleValue() :
                Double.POSITIVE_INFINITY;
        pf.startSearch(min, max, baos.toByteArray(), macroName);
    }

    private void refresh() {
        defineScopeButton.setEnabled(!running);
        searchButton.setEnabled(!running);
        stopButton.setEnabled(running);
        macroComboBox.setEnabled(!running);
        openCaseButton.setEnabled(!running);
        computeButton.setEnabled(!running);
        minScore.setEnabled(!running && checkBoxSelected(minScoreEnabled));
        maxScore.setEnabled(!running && checkBoxSelected(maxScoreEnabled));
        minScoreEnabled.setEnabled(!running);
        maxScoreEnabled.setEnabled(!running);
    }

    void setSearchRunning(boolean running) {
        this.running = running;
        refresh();
    }
}
