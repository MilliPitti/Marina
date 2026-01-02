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
package de.smile.marina.fem.model.ground;

import de.smile.marina.TimeDependentModel;
import de.smile.marina.fem.*;
import de.smile.marina.io.TicadIO;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Datenbasiertes Modell fuer den veraenderlichen Gewaesserboden
 *
 * @author milbradt
 * @version 2.8.1
 */
public class SysErgInterpolatedBathymetricModel2D extends TimeDependentFEApproximation implements FEModel, TimeDependentModel {

    FEBathymetricModel2D fe0 = null;
    FEBathymetricModel2D fe1 = null;

    String fileName=null;

    /** Creates a new instance of SysErgInterpolatedMeteorologicalModel2D
     * @param fe
     * @param bathymetryErgPath
     * @throws java.io.IOException */
    public SysErgInterpolatedBathymetricModel2D(FEDecomposition fe, String bathymetryErgPath) throws IOException {
        fenet = fe;
        System.out.println("BathymetricModel2D initialization");
        femodel = this;
        this.fileName = bathymetryErgPath;
        System.out.println("\tOpen bathymetryErg file " + bathymetryErgPath);
        
        this.fe0 = new FEBathymetricModel2D(fileName, 0);

        // DOFs initialisieren
        initialDOFs();

    }

    @Override
    public ModelData genData(DOF dof) {
        
        return new FEInterpolatedBathymetryData2D(dof);
    }

    @Override
   public void timeStep(double dt) {

        this.time += dt;

//System.out.println("time = "+this.time+", time0="+this.fe0.time);
// Peter 02.11.2017
//        if(fe0==null) 
//            try {
//            fe0 = new FEBathymetricModel2D(fileName, 0);
//        } catch (IOException ex) {
//            Logger.getLogger(SysErgInterpolatedBathymetricModel2D.class.getName()).log(Level.SEVERE, null, ex);
//        }
        if(fe0.time > time)
            return;

        if(fe1==null)
            try {
                fe1 = new FEBathymetricModel2D(fileName, fe0.record + 1);
//System.out.println("updaten");
                for (DOF dof : fenet.getDOFs()) {
                    FEInterpolatedBathymetryData2D md = (FEInterpolatedBathymetryData2D) BathymetryData2D.extract(dof);
                    md.update(fe0, fe1);
                }
            } catch (IOException ex) {
                Logger.getLogger(SysErgInterpolatedBathymetricModel2D.class.getName()).log(Level.SEVERE, null, ex);
            }
        if (fe1==null) return; // ToDo was passiert wenn die Zeit spaeter als der letzte Datensatz ist?
//System.out.println("time = "+this.time+", time1="+this.fe1.time);
        while (fe1.time < time) {
            fe0 = fe1;
            try {
                fe1 = new FEBathymetricModel2D(fileName, fe0.record + 1);
//System.out.println("updaten");
                for (DOF dof : fenet.getDOFs()) {
                    FEInterpolatedBathymetryData2D md = (FEInterpolatedBathymetryData2D) BathymetryData2D.extract(dof);
                    md.update(fe0, fe1);
                }
            } catch (IOException ex) {
                Logger.getLogger(SysErgInterpolatedBathymetricModel2D.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
//System.out.println("time0="+this.fe0.time+", time = "+this.time+", time1="+this.fe1.time);

        double lambda = (this.time - fe0.time)/(fe1.time-fe0.time);

        for (DOF dof : fenet.getDOFs()) {
            FEInterpolatedBathymetryData2D md = (FEInterpolatedBathymetryData2D) BathymetryData2D.extract(dof);
            md.z = (1 - lambda) * md.md0.z + lambda * md.md1.z;
        }
    }


    public double[] initialSolution(double StartTime) {
        this.time = StartTime;

        timeStep(0.);

        return null;
    }

    @Override
    public void setBoundaryCondition(DOF dof, double t) {
        // nicht Notwendig
    }

    @Override
    @Deprecated
    public double[] getRateofChange(double time, double[] x) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double ElementApproximation(FElement ele) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write_erg_xf() {
        // Es wird kein ergebnisfile geschrieben
    }

    @Override
    public ModelData genData(FElement felement) {
        // wird nicht genutzt
        return null;
    }

    class FEInterpolatedBathymetryData2D extends BathymetryData2D {

        DOF myDof;
        double time0;
        BathymetryData2D md0;
        double time1;
        BathymetryData2D md1;

        FEInterpolatedBathymetryData2D(DOF dof) {
            super();
            this.myDof = dof;
            md0 = BathymetryData2D.extract(fe0.getValue(dof));
            time0=fe0.time;
            md1 = BathymetryData2D.extract(fe0.getValue(dof));
            time1=fe0.time;
            this.z= md0.z;
        }

        void update(FEBathymetricModel2D fe0, FEBathymetricModel2D fe1){
            md0 = BathymetryData2D.extract(fe0.getValue(myDof));
            time0=fe0.time;
            md1 = BathymetryData2D.extract(fe1.getValue(myDof));
            time1=fe1.time;
        }
    }

    class FEBathymetricModel2D extends FEInterpolation {

        double time=0;
        int record=0;

        FEBathymetricModel2D(String bathymetrieErgPath, int record) throws IOException {

            File sysergFile = new File(bathymetrieErgPath);
            try (FileInputStream stream = new FileInputStream(sysergFile);
                 DataInputStream inStream = new DataInputStream(stream)) {

                // Kommentar lesen, bis ASCII-Zeichen 7 kommt
                StringBuilder description = new StringBuilder();
                char c;
                do {
                    c = (char) inStream.readByte();
                    description.append(c);
                } while (c != 7);
                // Ende Kommentar
                // sind Offset-Koordianten gespeichert
                @SuppressWarnings("unused")
                String model_component = "";
                double offset_x = 0.0, offset_y = 0.0;
                try {
                    offset_x = TicadIO.getStoredIntegerValue(description.toString(), "OffSetX");
                    offset_y = TicadIO.getStoredIntegerValue(description.toString(), "OffSetY");
                    model_component = TicadIO.getStoredModelComponent(description.toString());
                } catch (Exception ex) {
                }

                //Anzahl Elemente, Knoten und Rand lesen
                int anzKnoten = inStream.readInt();
                int anzr = inStream.readInt();
                int anzElemente = inStream.readInt();
                //Ueberlesen folgender Zeilen
                inStream.skip(9 * 4);
                //Ergebnismaske lesen und auswerten
                int ergMaske = inStream.readInt();
                int anzWerte = TicadIO.ergMaskeAuswerten(ergMaske);

                final boolean None_gesetzt = ((ergMaske & TicadIO.HRES_NONE) == TicadIO.HRES_NONE);
                final boolean Pos_gesetzt = ((ergMaske & TicadIO.HRES_POS) == TicadIO.HRES_POS);
                final boolean Z_gesetzt = ((ergMaske & TicadIO.HRES_Z) == TicadIO.HRES_Z);
                final boolean V_gesetzt = ((ergMaske & TicadIO.HRES_V) == TicadIO.HRES_V);
                final boolean Q_gesetzt = ((ergMaske & TicadIO.HRES_Q) == TicadIO.HRES_Q);
                final boolean H_gesetzt = ((ergMaske & TicadIO.HRES_H) == TicadIO.HRES_H);
                final boolean SALT_gesetzt = ((ergMaske & TicadIO.HRES_SALT) == TicadIO.HRES_SALT);
                final boolean EDDY_gesetzt = ((ergMaske & TicadIO.HRES_EDDY) == TicadIO.HRES_EDDY);
                final boolean SHEAR_gesetzt = ((ergMaske & TicadIO.HRES_SHEAR) == TicadIO.HRES_SHEAR);
                final boolean V_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_V_SCAL) == TicadIO.HRES_V_SCAL);
                final boolean Q_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_Q_SCAL) == TicadIO.HRES_Q_SCAL);
                final boolean AH_gesetzt = ((ergMaske & TicadIO.HRES_AH) == TicadIO.HRES_AH);

                inStream.readInt();

                DOF[] dof = new DOF[anzKnoten];
                for(int i=0;i<dof.length;i++)
                    dof[i] = new DOF(i,0.,0.,0.);

                FTriangle[] elem = new FTriangle[anzElemente];

                for (int i=0; i<anzElemente; i++){

                    DOF dof0 = dof[inStream.readInt()];
                    DOF dof1 = dof[inStream.readInt()];
                    DOF dof2 = dof[inStream.readInt()];

                    elem[i] = new FTriangle(dof0, dof1, dof2);

                    inStream.readInt(); // Kennung
		}

                // Schleife Nummern der Randknoten lesen
                for (int i = 0; i < anzr; i++) {
                    inStream.readInt(); // Knotennummer ueberlesen
                }

                // Schleife Knoten lesen 3 mal float
                for (int i = 0; i < anzKnoten; i++) {
                    DOF p = dof[i];
                    p.x = inStream.readFloat() + offset_x;
                    p.y = inStream.readFloat() + offset_y;
                    p.z = inStream.readFloat();
                }

                //Elemente, Rand und Knoten Ueberlesen
//                inStream.skip((anzElemente * 4 + anzr + 3 * anzKnoten) * 4); //4 Bytes je float und int

                fenet = new FTriangleMesh(elem, dof);

                // bis zum record-Satz springen
                inStream.skip((4L + anzKnoten * anzWerte * 4) * record);
                this.record = record;
                time = inStream.readFloat();
                for (int i = 0; i < fenet.getNumberofDOFs(); i++) {

                    BathymetryData2D data = new BathymetryData2D();
                    dof[i].addModelData(data);

                    if (None_gesetzt) {
                        inStream.skip(4);
                    }
                    if (Pos_gesetzt) {
                        inStream.skip(4);
                    }
                    if (Z_gesetzt) {
                        data.z = inStream.readFloat();
                    }
                    if (V_gesetzt) {
                        inStream.skip(8);
                    }
                    if (Q_gesetzt) {
                        inStream.skip(8);
                    }
                    if (H_gesetzt) {
                        inStream.skip(4);
                    }
                    if (SALT_gesetzt) {
                        inStream.skip(4);
                    }
                    if (EDDY_gesetzt) {
                        inStream.skip(4);
                    }
                    if (SHEAR_gesetzt) {
                        inStream.skip(8);
                    }
                    if (V_SCAL_gesetzt) {
                        inStream.skip(4);
                    }
                    if (Q_SCAL_gesetzt) {
                        inStream.skip(4);
                    }
                    if (AH_gesetzt) {
                        inStream.skip(4);
                    }
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(SysErgInterpolatedBathymetricModel2D.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }

        @Override
        public ModelData genData(DOF dof) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void ElementInterpolation(FElement ele) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public DOF getValue(DOF dof) {
            DOF rdof = new DOF(-1, dof.x, dof.y, dof.z);
            BathymetryData2D md = new BathymetryData2D();
            rdof.addModelData(md);
            for (FElement elem : fenet.getFElements()) {
                if (elem.contains(rdof)) {
                    double[] lambda = elem.getNaturefromCart(rdof);
                    BathymetryData2D md0 = BathymetryData2D.extract(elem.getDOF(0));
                    BathymetryData2D md1 = BathymetryData2D.extract(elem.getDOF(1));
                    BathymetryData2D md2 = BathymetryData2D.extract(elem.getDOF(2));
                    md.z = lambda[0]*md0.z + lambda[1]*md1.z +lambda[2]*md2.z;
                    break;
                }
            }
            if(Double.isNaN(md.z)) System.out.println(dof.number+" ist NaN");
            return rdof;
        }
    }
}
