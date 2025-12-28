/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2026

 * This file is part of Marina.

 * Marina is free software: you can redistribute it and/or modify              
 * it under the terms of the GNU Affero General Public License as               
 * published by the Free Software Foundation version 3.
 * 
 * Marina is distributed in the hope that it will be useful,                  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of               
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                
 * GNU Affero General Public License for more details.                          
 *                                                                              
 * You should have received a copy of the GNU Affero General Public License     
 * along with Marina.  If not, see <http://www.gnu.org/licenses/>.             
 *                                                                               
 * contact: milbradt@smileconsult.de                                        
 * smile consult GmbH                                                           
 * Schiffgraben 11                                                                 
 * 30159 Hannover, Germany 
 * 
 */
package de.smile.marina.io;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FElement;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * @version 0.5
 * @author Julian Sievers / Peter Milbradt
 */
public class MarinaAVSExport {

    public static void main(String[] args) throws Exception {

        FileNameExtensionFilter csvFilter = new FileNameExtensionFilter("csv File", "csv");
        FileNameExtensionFilter jbfFilter = new FileNameExtensionFilter("jbf File", "jbf");

        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.resetChoosableFileFilters();
        chooser.removeChoosableFileFilter(chooser.getAcceptAllFileFilter());
        chooser.addChoosableFileFilter(jbfFilter);
        int arg = chooser.showOpenDialog(new javax.swing.JFrame());
        if (arg == javax.swing.JFileChooser.CANCEL_OPTION) {
            return;
        }
        if (chooser.getSelectedFile() == null) {
            System.out.println("No file selected");
            return;
        }
        String systemjbf = chooser.getSelectedFile().getAbsolutePath();

        chooser.resetChoosableFileFilters();
        chooser.removeChoosableFileFilter(chooser.getAcceptAllFileFilter());
        chooser.addChoosableFileFilter(csvFilter);
        arg = chooser.showOpenDialog(new javax.swing.JFrame());
        if (arg == javax.swing.JFileChooser.CANCEL_OPTION) {
            return;
        }
        if (chooser.getSelectedFile() == null) {
            System.out.println("No file selected");
            return;
        }
        String datacsv = chooser.getSelectedFile().getAbsolutePath();

        String basedir = chooser.getSelectedFile().getParent();
        String csvName = chooser.getSelectedFile().getName();
        String outputName = csvName.split("\\.")[0];

        String exportavs = basedir + "/" + outputName + "_volume.inp";
        String exportavslayer = basedir + "/" + outputName + "_layer.inp";
        String exportavscores = basedir + "/" + outputName + "_cores.inp";
        double vertExFactor = 25d;
        export_avs(systemjbf, datacsv, exportavs, exportavslayer, exportavscores, vertExFactor, false);
        System.exit(0);
    }

    public static void export_avs(String systemjbf, String datacsv, String exportavsvolume, String exportavslayer,
            String exportavscores, double vertExFactor, boolean vonOben) throws Exception {
        int richtung = vonOben ? -1 : +1;
        ArrayList<SimplePoint> simple_points = new ArrayList<>();
        String lastnr = "-1";
        double ddd = 0.002 * vertExFactor;
        try (FileReader freader = new FileReader(datacsv); BufferedReader reader = new BufferedReader(freader)) {
            reader.readLine();
            String line;
            ArrayList<double[]> temp_data_values = new ArrayList<>();
            SimplePoint simp = null;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(";");
                String thisnr = split[0];
                if (!lastnr.equals(thisnr)) {
                    if (simp != null) {
                        if (temp_data_values.size() == 1) {
                            double[] get = temp_data_values.get(0).clone();
                            get[0] -= ddd;
                            // get[7]=0;
                            temp_data_values.add(get);
                        }
                        temp_data_values.sort((double[] o1, double[] o2) -> richtung * Double.compare(o1[0], o2[0]));
                        double[][] data = new double[temp_data_values.size()][8];
                        for (int i = 0; i < temp_data_values.size(); i++) {
                            double[] temp = temp_data_values.get(i);
                            for (int j = 0; j <= 7; j++) {
                                data[i][j] = temp[j];
                            }
                        }
                        simp.data = data;
                        temp_data_values.clear();
                    }
                    simp = new SimplePoint();
                    simple_points.add(simp);

                }
                lastnr = thisnr;
                try {
                    double[] data_ = new double[8];
                    // uebernehmen
                    data_[0] = Double.parseDouble(split[3]) * -1; // zl
                    data_[1] = Double.parseDouble(split[4]); // obergrenze, dmax
                    data_[2] = Double.parseDouble(split[5]); // d50
                    data_[3] = Double.parseDouble(split[6]); // untergrenze, dmin
                    data_[5] = Double.parseDouble(split[8]); // porosity
                    // berechnen
                    final double initialSorting = Double.parseDouble(split[7]);
                    final double k = 1. / ((1. - ((data_[1] + data_[3]) / 2.) / data_[1])
                            * (1. - data_[3] / ((data_[1] + data_[3]) / 2.)));
                    data_[4] = initialSorting * (1. - data_[2] / data_[1]) * (1. - data_[3] / data_[2]) * k; // sorting
                    data_[6] = (1. - (data_[4] / (1. + data_[4]))) * 0.25 / data_[5]; // ??
                    data_[7] = Double.parseDouble(split[9]); // consolidation
                    temp_data_values.add(data_);
                } catch (Exception e) {
                }
            }
            if (!temp_data_values.isEmpty()) {
                double[][] data = new double[temp_data_values.size()][8];
                for (int i = 0; i < temp_data_values.size(); i++) {
                    double[] temp = temp_data_values.get(i);
                    for (int j = 0; j <= 7; j++) {
                        data[i][j] = temp[j];
                    }
                }
                simp.data = data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // simple_points.removeIf(p -> p.data == null||p.data.length==0);
        FEDecomposition jbf = SmileIO.readFEDfromJanetBin(systemjbf);
        for (DOF point : jbf.getDOFs()) {
            double x = point.x;
            double y = point.y;
            try {

                SimplePoint sp = simple_points.get(point.number);
                sp.x = x;
                sp.y = y;
            } catch (Exception e) {
            }
        }

        ArrayList<SimpleTriangle> tris = new ArrayList<>();
        for (FElement element : jbf.getFElements()) {
            SimpleTriangle tri = new SimpleTriangle();
            SimplePoint[] points = new SimplePoint[3];
            try {
                points[0] = simple_points.get(element.getDOF(0).number);
                points[1] = simple_points.get(element.getDOF(1).number);
                points[2] = simple_points.get(element.getDOF(2).number);
                if (points[0].data == null || points[0].data.length == 0 || points[1].data == null
                        || points[1].data.length == 0 || points[2].data == null || points[2].data.length == 0) {
                    continue;
                }
                tri.points = points;
                tris.add(tri);
            } catch (Exception e) {
            }
        }

        // ab hier
        ArrayList<String> _punkte = new ArrayList<>();
        ArrayList<String> _lines = new ArrayList<>();
        ArrayList<String> _attribute = new ArrayList<>();

        SimpleTriangle.addAVSLinesToLists(tris, _punkte, _lines, _attribute, vertExFactor);

        String head = _punkte.size() + " " + _lines.size() + " " + _punkte.size() + " 0 0";
        try (FileWriter fwriter = new FileWriter(new File(exportavscores));
                BufferedWriter writer = new BufferedWriter(fwriter)) {
            writer.append(head).append("\n");
            for (String string : _punkte) {
                writer.append(string).append("\n");
            }
            for (String string : _lines) {
                writer.append(string).append("\n");
            }
            writer.append("6 1 1 1 1 1 1\n");
            writer.append("Obergrenze, mm\n");
            writer.append("d50, mm\n");
            writer.append("Untergrenze, mm\n");
            writer.append("Sortierung, -\n");
            writer.append("Porosity, 0-1\n");
            writer.append("Konsolidierung, 0-1\n");
            for (String string : _attribute) {
                writer.append(string).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList<String> punkte = new ArrayList<>();
        ArrayList<String> prismen = new ArrayList<>();
        ArrayList<String> attribute = new ArrayList<>();

        for (SimpleTriangle tri : tris) {
            tri.addAVSVolumesToLists(punkte, prismen, attribute, vertExFactor);
        }
        int punktzahl = punkte.size();
        int prismenzahl = prismen.size();

        head = punktzahl + " " + prismenzahl + " " + punktzahl + " 0 0";
        try (FileWriter fwriter = new FileWriter(new File(exportavsvolume));
                BufferedWriter writer = new BufferedWriter(fwriter)) {
            writer.append(head).append("\n");
            for (String string : punkte) {
                writer.append(string).append("\n");
            }
            for (String string : prismen) {
                writer.append(string).append("\n");
            }
            writer.append("6 1 1 1 1 1 1\n");
            writer.append("Obergrenze, mm\n");
            writer.append("d50, mm\n");
            writer.append("Untergrenze, mm\n");
            writer.append("Sortierung, -\n");
            writer.append("Porosity, 0-1\n");
            writer.append("Konsolidierung, 0-1\n");
            for (String string : attribute) {
                writer.append(string).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ArrayList<String> punkte_ = new ArrayList<>();
        ArrayList<String> dreiecke_ = new ArrayList<>();
        ArrayList<String> attribute_ = new ArrayList<>();

        for (SimpleTriangle tri : tris) {
            tri.addAVSLayersToLists(punkte_, dreiecke_, attribute_, vertExFactor);
        }
        head = punkte_.size() + " " + dreiecke_.size() + " " + punkte_.size() + " 0 0";

        try (FileWriter fwriter = new FileWriter(new File(exportavslayer));
                BufferedWriter writer = new BufferedWriter(fwriter)) {
            writer.append(head).append("\n");
            for (String string : punkte_) {
                writer.append(string).append("\n");
            }
            for (String string : dreiecke_) {
                writer.append(string).append("\n");
            }
            writer.append("6 1 1 1 1 1 1\n");
            writer.append("Obergrenze, mm\n");
            writer.append("d50, mm\n");
            writer.append("Untergrenze, mm\n");
            writer.append("Sortierung, -\n");
            writer.append("Porosity, 0-1\n");
            writer.append("Konsolidierung, 0-1\n");
            for (String string : attribute_) {
                writer.append(string).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class SimplePoint {

        double x, y;
        double[][] data;

        public double[] getDataAt(int i) {
            return data[Math.min(data.length - 1, i)];
        }

        @Override
        public String toString() {
            String res = "x: " + x + "\ny: " + y + "\n";
            res += "z, upper, d50, lower, sort, poros, consol\n";
            for (double[] ds : data) {
                res += Arrays.toString(ds);
                res += "\n";
            }
            return res;
        }

        public boolean fastEqualsEps(SimplePoint q) {
            return Math.abs(this.x - q.x) < 1e-3 || Math.abs(this.y - q.y) < 1e-3;
        }
    }

    private static class SimpleTriangle {

        SimplePoint[] points;

        private int getMaxDataLength() {
            return Math.max(Math.max(points[0].data.length, points[1].data.length), points[2].data.length);
        }

        public static void addAVSLinesToLists(ArrayList<SimpleTriangle> tris, ArrayList<String> punkte,
                ArrayList<String> lines, ArrayList<String> attribute, double vertExFactor) {
            HashSet<SimplePoint> uniques = new HashSet<>();
            for (SimpleTriangle tri : tris) {
                uniques.addAll(Arrays.asList(tri.points));
            }

            int pointnr = punkte.size();
            int linenr = lines.size();
            for (SimplePoint point : uniques) {
                for (int i = 0; i < point.data.length - 1; i++) {
                    String line = linenr + " 1 line";
                    String punkt = pointnr + " " + point.x + " " + point.y + " "
                            + (point.getDataAt(i)[0] * vertExFactor);
                    punkte.add(punkt);
                    String attribut = pointnr + " " + point.getDataAt(i)[1] + " " + point.getDataAt(i)[2] + " "
                            + point.getDataAt(i)[3] + " " + point.getDataAt(i)[4] + " " + point.getDataAt(i)[5] + " "
                            + point.getDataAt(i)[6];
                    attribute.add(attribut);
                    line += " " + pointnr;
                    pointnr++;
                    punkt = pointnr + " " + point.x + " " + point.y + " " + (point.getDataAt(i + 1)[0] * vertExFactor);
                    punkte.add(punkt);
                    attribut = pointnr + " " + point.getDataAt(i + 1)[1] + " " + point.getDataAt(i + 1)[2] + " "
                            + point.getDataAt(i + 1)[3] + " " + point.getDataAt(i + 1)[4] + " "
                            + point.getDataAt(i + 1)[5] + " " + point.getDataAt(i + 1)[6];
                    attribute.add(attribut);
                    line += " " + pointnr;
                    pointnr++;
                    lines.add(line);
                    linenr++;
                }
            }
        }

        public void addAVSLayersToLists(ArrayList<String> punkte, ArrayList<String> dreiecke,
                ArrayList<String> attribute, double vertExFactor) {
            int maxlength = this.getMaxDataLength();
            int pointnr = punkte.size();
            int trinr = dreiecke.size();
            for (int i = 0; i < maxlength; i++) {
                if (points[0].getDataAt(i)[7] == 0 || points[1].getDataAt(i)[7] == 0
                        || points[2].getDataAt(i)[7] == 0) {
                    continue;
                }
                String tri = trinr + " 1 tri";
                for (int j = 0; j <= 2; j++) {
                    String punkt = pointnr + " " + points[j].x + " " + points[j].y + " "
                            + (points[j].getDataAt(i)[0] * vertExFactor);
                    punkte.add(punkt);
                    String attribut = pointnr + " " + points[j].getDataAt(i)[1] + " " + points[j].getDataAt(i)[2] + " "
                            + points[j].getDataAt(i)[3] + " " + points[j].getDataAt(i)[4] + " "
                            + points[j].getDataAt(i)[5] + " " + points[j].getDataAt(i)[6];
                    attribute.add(attribut);
                    tri += " " + pointnr;
                    pointnr++;
                }
                dreiecke.add(tri);
                trinr++;
            }
        }

        public void addAVSVolumesToLists(ArrayList<String> punkte, ArrayList<String> prismen,
                ArrayList<String> attribute, double vertExFactor) {
            int maxlength = this.getMaxDataLength();
            int pointnr = punkte.size();
            int prismnr = prismen.size();
            for (int i = 0; i < maxlength - 1; i++) {
                String prism = prismnr + " 1 prism";
                for (int j = 0; j <= 2; j++) {
                    String punkt = pointnr + " " + points[j].x + " " + points[j].y + " "
                            + (points[j].getDataAt(i)[0] * vertExFactor);
                    punkte.add(punkt);
                    String attribut = pointnr + " " + points[j].getDataAt(i)[1] + " " + points[j].getDataAt(i)[2] + " "
                            + points[j].getDataAt(i)[3] + " " + points[j].getDataAt(i)[4] + " "
                            + points[j].getDataAt(i)[5] + " " + points[j].getDataAt(i)[6];
                    attribute.add(attribut);
                    prism += " " + pointnr;
                    pointnr++;
                }
                for (int j = 0; j <= 2; j++) {
                    String punkt = pointnr + " " + points[j].x + " " + points[j].y + " "
                            + (points[j].getDataAt(i + 1)[0] * vertExFactor);
                    punkte.add(punkt);
                    String attribut = pointnr + " " + points[j].getDataAt(i + 1)[1] + " "
                            + points[j].getDataAt(i + 1)[2] + " " + points[j].getDataAt(i + 1)[3] + " "
                            + points[j].getDataAt(i + 1)[4] + " " + points[j].getDataAt(i + 1)[5] + " "
                            + points[j].getDataAt(i + 1)[6];
                    attribute.add(attribut);
                    prism += " " + pointnr;
                    pointnr++;
                }
                prismen.add(prism);
                prismnr++;
            }
        }

    }

}
