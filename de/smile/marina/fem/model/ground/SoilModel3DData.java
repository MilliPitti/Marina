/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2022

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
package de.smile.marina.fem.model.ground;

import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import de.smile.marina.fem.model.hydrodynamic.dim2.SedimentModel2DData;
import de.smile.marina.fem.model.hydrodynamic.dim2.SedimentProperties;
import de.smile.math.Function;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Point2d;

/**
 *
 * @author Peter Milbradt
 * @version 3.0
 */
public class SoilModel3DData extends SedimentModel2DData {
    
    private static final long serialVersionUID = 1L;

    private static int id = NO_MODEL_DATA;

    public static boolean exportDatum = false; // schreibt in den toString() als letztes das Datum, z.B. bei der Nutzung im datenbasierten Modell: SequenceStratigraficModel
    public static boolean everyLayer = false; // nimmt jeden berechneten Layer in den Stack auf, z.B. bei der Nutzung im datenbasierten Modell: SequenceStratigraficModel

    public Stack<LayerValues> layerValues = new Stack<>();
    private boolean sedimentation = false;

    public SoilModel3DData() {
        id = SEARCH_MODEL_DATA;
    }

    public static SoilModel3DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof SoilModel3DData soilModel3DData) {
                    id = dof.getIndexOf(md);
                    return soilModel3DData;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (SoilModel3DData) dof.getModelData(id);
        }
        return null;
    }

    public void timeStep(double time, double dt) {
        update(time + dt);
        /**
         * Schichten kompremieren mit dt als Zeitschritt
         */
        if (layerValues.size() > 1) {
            double gewicht = 0.;
            double zOben = layerValues.get(layerValues.size() - 1).zl;
            for (int i = layerValues.size() - 2; i >= 0; i--) {
                LayerValues layer = layerValues.get(i);
                gewicht += (zOben - layer.zl) * (1. - layer.porosity) * PhysicalParameters.RHO_SEDIM;
                final double kappa = getPermeabilityBear1972(layer.d50, layer.porosity) * PhysicalParameters.KINVISCOSITY_WATER / PhysicalParameters.G;
                final double so = layer.initialSorting * (1. - layer.d50 / layer.dmax) * (1. - layer.dmin / layer.d50) * layer.k; // Sorting
                final double consolidatedPorosity = 0.25 / (1. + so * Math.sqrt(layer.wc));
                final double consolidation = Math.min(1., consolidatedPorosity / layer.porosity);
                layer.porosity -= dt * kappa * wc * gewicht * (1. - consolidation);
                layer.porosity = Math.max(layer.porosity, 1.01 * consolidatedPorosity);
                layer.consolidation = Math.min(1., consolidatedPorosity / layer.porosity);
                zOben = layer.zl;
            }
        }
    }
    
    private void pushLayer(LayerValues layer){
        if (layerValues.isEmpty()) {
            layerValues.push(layer);
        } else {
            LayerValues topLayer = layerValues.peek();
            if (layer.zl < topLayer.zl) {// Sedimentieren
                    layerValues.push(layer);
            } else {// Erodieren
                while (!layerValues.isEmpty()) {
                    topLayer = layerValues.peek();
                    if (topLayer.zl > layer.zl + layer.dmin / 100.) { // Schicht wird NICHT durchstoszen
                        layerValues.push(layer);
                        break;
                    } else { // Schicht wird durchstoszen 
                        layerValues.pop(); // loeschen der durchstoszenen Schichtgrenze
                    }
                }
                if (layerValues.isEmpty()) {
                    layerValues.push(layer);
                }
            }
        }
    }

    public void update(double time) {
        if (layerValues.isEmpty()) {
            layerValues.push(new LayerValues(this, time));
        } else {
            LayerValues topLayer = layerValues.peek();
            if (z <= topLayer.zl + dmin / 100.) {// Sedimentieren
                if (sedimentation && !everyLayer) {
                    topLayer.zl = z;
                    topLayer.d50 = d50;
                    topLayer.sorting = sorting;
                    topLayer.porosity = porosity;
                    topLayer.consolidation = consolidation; // 1 wenn minmaler Zwischenraum - fest!
                    topLayer.wc = wc; // [m/s] Sinkgeschwindigkeit
                    topLayer.time = time;
                } else { // neuen Layer im Stack erstellen
                    sedimentation = true;
                    layerValues.push(new LayerValues(this, time));
                }
            } else {// Erodieren
                sedimentation = false;
                boolean firstLayer = true;
                while (!layerValues.isEmpty()) {
                    topLayer = layerValues.peek();
                    if (topLayer.zl > z + dmin / 100.) { // Schicht wird NICHT durchstoszen
                        layerValues.push(new LayerValues(this, time));
                        break;
                    } else { // Schicht wird durchstoszen 
                        final double lambda = firstLayer ? 0. : 1.;
                        d50 = (1. - lambda) * d50 + lambda * topLayer.d50;
                        dmax = (1. - lambda) * dmax + lambda * topLayer.dmax;
                        dmin = (1. - lambda) * dmin + lambda * topLayer.dmin;
                        d50 = Function.max(1.1 * dmin, Function.min(0.9 * dmax, d50));

                        initialSorting = (1. - lambda) * initialSorting + lambda * topLayer.initialSorting;

                        setD50(d50);

                        consolidation = (1. - lambda) * consolidation + lambda * topLayer.consolidation;

                        layerValues.pop(); // loeschen der durchstoszenen Schichtgrenze
                    }
                    firstLayer = false;
                }
                if (layerValues.isEmpty()) {
                    layerValues.push(new LayerValues(this, time));
                }
            }
        }
    }

    @Override
    public String toString() {
        String rValue = "";
        return layerValues.stream().map((layer) -> layer.toString()).reduce(rValue, String::concat);
    }

    @Override
    public SoilModel3DData clone() throws CloneNotSupportedException {
        SoilModel3DData rvalue = (SoilModel3DData) super.clone(); //To change body of generated methods, choose Tools | Templates.

        rvalue.sedimentation = sedimentation;

        rvalue.layerValues = new Stack<>();
        for (int i = 0; i < layerValues.size(); i++) {
            rvalue.layerValues.add(i, layerValues.get(i).clone());
        }

        return rvalue;
    }

    /**
     * gibt den Layer zurueck der zu zl mit einem epsilon passt
     */
    private LayerValues get(double zl, double epsilon) {
        // Doofe Loesung
//        for (int i = Math.max(0, lastLayerIndex-1); i < layerValues.size(); i++) {
//            if (Math.abs(layerValues.get(i).zl - zl) < epsilon) {
//                lastLayerIndex = i;
//                return layerValues.get(i);
//            }
//        }
        // binaere Suche
        int insertion_point = Collections.binarySearch(layerValues, new LayerValues(zl, Double.NaN));
        if (insertion_point >= 0) {
            return layerValues.get(insertion_point);
        } else {
            int actPosition = (-1) * insertion_point - 2;
            if (Math.abs(layerValues.get(actPosition).zl - zl) < epsilon) {
                return layerValues.get(actPosition);
            }
            if (Math.abs(layerValues.get(actPosition + 1).zl - zl) < epsilon) {
                return layerValues.get(actPosition + 1);
            }
        }
        return null;
    }

    public void checkANDcorrect() {
        // Sortierung korregieren
        Collections.sort(layerValues);
        // check doppelte Schichten und korrigieren
        for (int i = 0; i < layerValues.size(); i++) {
            if (i > 0) {
                final double dzl = Math.abs(layerValues.get(i).zl - layerValues.get(i - 1).zl);
                if (dzl < Double.MIN_NORMAL * 10.) {
                    layerValues.removeElementAt(i);
                }
            }
        }
    }

    public Stack<LayerValues> getCoarsenedLayerValuesStack() {

        if (layerValues.size() < 3) {
            return layerValues;
        }

        double mindzl = Double.POSITIVE_INFINITY;
        final double EPSILON = 1.e-3;

        ArrayList<LayerValues> _values = new ArrayList<>();
        for (int i = 0; i < layerValues.size(); i++) {
            _values.add(layerValues.get(i));
            if (i > 0) {
                final double dzl = Math.abs(layerValues.get(i).zl - layerValues.get(i - 1).zl);
                if (dzl < mindzl) {
                    mindzl = dzl;
                }
            }
        }
        final double epsilon = mindzl / 4.;
        int lastsize;
        do {
            lastsize = _values.size();
            for (int i = 1; i < _values.size() - 1; i++) {
                double x1 = _values.get(i - 1).zl;
                SedimentProperties y1 = _values.get(i - 1);
                double x2 = _values.get(i + 1).zl;
                SedimentProperties y2 = _values.get(i + 1);
                double x = _values.get(i).zl;
                SedimentProperties y = _values.get(i);
                final SedimentProperties neuy = (y1.add(((y2.sub(y1)).mult(((x - x1) / (x2 - x1))))));
                double distance = Math.max(y1.distance(y2) * EPSILON, Double.MIN_NORMAL * 100.);
                SedimentProperties m1 = neuy;
                SedimentProperties m2 = y;
                SedimentProperties m3 = get(x, epsilon);
                double aehnlichkeit_neuy = m1.distance(m2);
                double aehnlichkeit_alty = m3.distance(m2);
                if (aehnlichkeit_neuy < distance && aehnlichkeit_alty < distance) {
                    _values.remove(i);
                    i--;
                }
            }
        } while (lastsize != _values.size());

        Stack<LayerValues> rValue = new Stack<>();
        rValue.addAll(_values);
        Collections.sort(rValue);

        return rValue;
    }

    /**
     *
     * @param args
     * @throws java.text.ParseException
     */
    static public void main(String[] args) throws ParseException{
        String csvBaseFileName = "/home/milbradt/Gebiete/Nordsee/marina_Bohrkerne_points_rev04u_mesh___y2012_zusammengedrueckt_01.01.2012.csv";
        String csvSimulatedFileName = "/home/milbradt/Gebiete/Nordsee/NSM_TUHH_2012_Test/78864210/soilModel3DData.csv";
        String csvResultFileName = "/home/milbradt/Gebiete/Nordsee/NSM_TUHH_2012_Test/78864210/soilModel3DDataResult.csv";
        String startDatum = "2012-01-01 00:00:00 UTC+1";
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        long timeOffSetInSeconds = parser.parse(startDatum).getTime() / 1000l;
        int morphologischerFaktor = 10;
        
        SoilModel3DData.exportDatum=true;

        HashMap<Integer, Pair<Point2d, SoilModel3DData>> Basedata = readSoilModel3DDatafromCSV(csvBaseFileName);
        HashMap<Integer, Pair<Point2d, SoilModel3DData>> Simuldata = readSoilModel3DDatafromCSV(csvSimulatedFileName);
        for (Integer key : Simuldata.keySet()) {
            Pair<Point2d, SoilModel3DData> pair = Simuldata.get(key);
            SoilModel3DData smd = pair.getSecond();

            Pair<Point2d, SoilModel3DData> bpair = Basedata.get(key);
            SoilModel3DData bsmd = bpair.getSecond();

            for (LayerValues lv : smd.layerValues) {
                lv.time *= 10;
                lv.time += timeOffSetInSeconds;
                bsmd.pushLayer(lv);
            }
//            bsmd.checkANDcorrect();
        }
        
        writeSoilModel3DData2CVS(Basedata, csvResultFileName);

    }

    public static void writeSoilModel3DData2CVS(HashMap<Integer, Pair<Point2d, SoilModel3DData>> data, String csvFileName) {

        // create file-in-subdirectory path
        Path outPath = FileSystems.getDefault().getPath(csvFileName);
        try (BufferedWriter writer = Files.newBufferedWriter(outPath, Charset.defaultCharset(), StandardOpenOption.CREATE)) {
            writer.write("nodenumber ; x ; y ; " + SoilModel3DData.LayerValues.getASCIIHeaderLine());
            for (Integer nr : data.keySet()) {
                Pair<Point2d, SoilModel3DData> bpair = data.get(nr);
                SoilModel3DData sm3d = bpair.getSecond();
                Point2d dof = bpair.getFirst();

                // ***  heilen !
                sm3d.checkANDcorrect();
                // ***  vergroebern !
                sm3d.layerValues = sm3d.getCoarsenedLayerValuesStack();

                double lastLayerZ = Double.NaN;
                boolean firstLayer = true;
                for (SoilModel3DData.LayerValues layer : sm3d.layerValues) { // Tiefste zu erst, hoechste zu letzte
                    if (firstLayer) {
                        writer.write("\n" + nr + " ; " + dof.x + " ; " + dof.y + " ; " + layer.toString());
                        firstLayer = false;
                    } else {
                        if (!(Math.abs(layer.zl - lastLayerZ) < Double.MIN_NORMAL * 128)) // wenn die Schicht nicht doppelt ist
                        {
                            writer.write("\n" + nr + " ; " + dof.x + " ; " + dof.y + " ; " + layer.toString());
                        }
                    }
                    lastLayerZ = layer.zl;
                }
            }
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(SoilModel3DData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static public HashMap<Integer, Pair<Point2d, SoilModel3DData>> readSoilModel3DDatafromCSV(String csvBaseFileName) {
        System.out.println("\tRead SoilModel3DData from " + csvBaseFileName);
        HashMap<Integer, Pair<Point2d, SoilModel3DData>> dof_data = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(FileSystems.getDefault().getPath(csvBaseFileName))) {
            reader.readLine(); // Kopfzeile lesen
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] split = line.split(";");
                    int dofNumber = Integer.parseInt(split[0].trim());
                    double x = Double.parseDouble(split[1].trim());
                    double y = Double.parseDouble(split[2].trim());
                    SoilModel3DData sm3d;
                    try{
                        sm3d=dof_data.get(dofNumber).getSecond();
                    }catch(Exception e){
                        sm3d = new SoilModel3DData();
                        dof_data.put(dofNumber, new Pair<>(new Point2d(x, y), sm3d));
                    }                    
                    SoilModel3DData.LayerValues layer = new SoilModel3DData.LayerValues(line);
                    if (Double.isNaN(layer.sorting)) {
                        layer.sorting = 1.;
                    }
                    if (!Double.isNaN(layer.zl)) {
                        sm3d.layerValues.push(layer);
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                } // wenn z.B. keine Daten an einem Knoten vorliegen
            }
        } catch (IOException ex) {
            Logger.getLogger(SoilModel3DData.class.getName()).log(Level.SEVERE, null, ex);
        }
        return dof_data;
    }

    static public class LayerValues extends SedimentProperties implements Comparable<LayerValues> {

        public double zl; // LayerDepth
        public double time; // Zeit in Sekunden an dem dieser Wert angenommen wurde

        protected LayerValues(double zl, double time) {
            super();
            this.zl = zl;
            this.time = time;
        }

        public LayerValues(double zl, double upperGrainSize, double d50, double lowerGrainSize, double meanSorting, double porosity, double consolidation) {
            super(lowerGrainSize, d50, upperGrainSize, meanSorting);
            this.zl = zl;
            this.porosity = porosity;
            this.consolidation = consolidation; // 1 wenn minmaler Zwischenraum - fest!
        }

        public LayerValues(double zl, SedimentModel2DData smd, double time) {
            this(smd, time);
            this.zl = zl;
        }

        public LayerValues(SedimentModel2DData smd, double time) {
            this.zl = smd.z;
            dmax = smd.dmax; // [m] groesztes verfuegbares Korn (entspricht in etwa d_95)
            d50 = smd.d50; // [m] mittlerer Kordurchmesser, wenn eine Verteilung da ist soll dieser Wert berechnet werden
            dmin = smd.dmin; // [m] kleinstes verfuegbares Korn (entspricht in etwa d_05)
            this.k = smd.k;
            initialSorting = smd.initialSorting;
            sorting = smd.sorting;
            porosity = smd.porosity;
            consolidation = smd.consolidation; // 1 wenn minmaler Zwischenraum - fest!
            wc = smd.wc; // [m/s] Sinkgeschwindigkeit
            this.time = time;
        }

        public LayerValues(String csvLine) throws Exception {
            String[] split = csvLine.split(";");
            this.zl = Double.parseDouble(split[3]);
            this.dmin = Double.parseDouble(split[6]) / 1000.;
            this.d50 = Double.parseDouble(split[5]) / 1000.;
            this.dmax = Double.parseDouble(split[4]) / 1000.;
            this.initialSorting = Double.parseDouble(split[7]);
            this.k = 1. / ((1. - ((dmax + dmin) / 2.) / dmax) * (1. - dmin / ((dmax + dmin) / 2.)));
            this.sorting = initialSorting * (1. - d50 / dmax) * (1. - dmin / d50) * k;
            this.porosity = Double.parseDouble(split[8]);
            this.consolidation = Double.parseDouble(split[9]);
            try {
                this.time = Double.parseDouble(split[10]); // seit 01.11.2020 wird die Zeit mit raus geschrieben und danach eventuell auch das Datum
            } catch (Exception e) {
                this.time = 0.;
            }
        }

        @Override
        public int compareTo(LayerValues lv) {
            int rvalue = 0;
            if (this.zl < lv.zl) {
                rvalue = 1;
            }
            if (this.zl > lv.zl) {
                rvalue = -1;
            }
            return rvalue;
        }

        /**
         * die Klassenmethode generiert die Kopfzeile zum ASCII-Backup
         *
         * @return Kopfzeile die den ascii-Backup beschreibt
         */
        public static String getASCIIHeaderLine() {
            if (exportDatum) {
                return "z [m] in depth" + " ; " + "dmax [mm]" + " ; " + "d50 [mm]" + " ; " + "dmin [mm]" + " ; " + "meanInitialSorting" + " ; " + "porosity" + " ; " + "consolidation" + " ; " + "time [s] since 01.01.1970 ; Datum";
            } else {
                return "z [m] in depth" + " ; " + "dmax [mm]" + " ; " + "d50 [mm]" + " ; " + "dmin [mm]" + " ; " + "meanInitialSorting" + " ; " + "porosity" + " ; " + "consolidation" + " ; " + "time [s]";
            }
        }
        protected static final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

        @Override
        public String toString() {
            DecimalFormat df = new DecimalFormat("#");
            if (exportDatum) {
                return zl + " ; " + dmax * 1000. + " ; " + d50 * 1000. + " ; " + dmin * 1000. + " ; " + initialSorting + " ; " + porosity + " ; " + consolidation + " ; " + df.format(time) + ";" + sdf.format(time * 1000l);
            } else {
                return zl + " ; " + dmax * 1000. + " ; " + d50 * 1000. + " ; " + dmin * 1000. + " ; " + initialSorting + " ; " + porosity + " ; " + consolidation + " ; " + df.format(time);
            }
        }

        @Override
        public LayerValues clone() throws CloneNotSupportedException {
            LayerValues rvalue = (LayerValues) super.clone();
            rvalue.zl = this.zl;
            return rvalue;
        }
    }
}
