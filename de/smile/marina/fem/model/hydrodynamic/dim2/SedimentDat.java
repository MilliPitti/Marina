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
package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.model.hydrodynamic.dim2.SedimentProperties.CriticalShieldsFunction;
import de.smile.marina.io.BoundaryConditionsReader;
import de.smile.marina.io.SmileIO;
import de.smile.marina.io.SmileIO.MeshFileType;
import de.smile.xml.marina.Marina;
import de.smile.xml.marina.TBedLoadFormula;
import de.smile.xml.marina.TCriticalShieldsFormula;
import de.smile.xml.marina.TMeshFileType;
import de.smile.xml.marina.TSSSFileType;
import de.smile.xml.marina.TSuspendedLoadFormula;

/**
 * Data for control of the depth integrated sediment model
 *
 * @version 4.1.0
 * @author Peter Milbradt
 */
public class SedimentDat {

    public String referenceDate = "1970-01-01 00:00:00 UTC+1"; // Reference date [yyyy-MM-dd HH:mm:ss z]
    public int epsgCode = -1;

    public String base_dir;
    
    public String singleSSPFileName = null;
    public TSSSFileType singleSSPFileType = TSSSFileType.CSV;
    
    public String d50FileName = null;
    public MeshFileType d50FileType = SmileIO.MeshFileType.SystemDat;
    public double standardD50 = 0.42E-3;            // Standardwert fuer Korndurchmesser d50
    
    public String dminFileName;
    public MeshFileType dminFileType;
    public double upperGrainSize = Double.NaN; // standardD50 * 10.;            // Obere Grenze der verfuegbaren Sedimentparameter (d95*2)
    
    public String dmaxFileName;
    public MeshFileType dmaxFileType;
    public double lowerGrainSize = Double.NaN; // standardD50 / 10.;            // Untere Grenze der verfuegbaren Sedimentparameter (d05/2)
    
    public String initalSortingFileName;
    public MeshFileType initalSortingFileType;
    public double initalSorting = Double.NaN; // 1.;    // StandartValue Anstieg fuer die Aenderung der Korndurchmesser
    
    public CriticalShieldsFunction csf = SedimentProperties.CriticalShieldsFunction.JulienKnoroz;

    /**
     * Konstruktor der die xml parsed
     *
     * @param sedimentTransportModel2DConfig
     */
    public SedimentDat(Marina.Configuration.SedimentTransportModel2D sedimentTransportModel2DConfig) {
        
        if(sedimentTransportModel2DConfig.getBedLoadFormula()!=null){
            TBedLoadFormula bedLoadFormulation = sedimentTransportModel2DConfig.getBedLoadFormula();
            if(bedLoadFormulation.value().equals("vanRijn84"))          this.bl =  new BedLoad2DvanRijn84();
            if(bedLoadFormulation.value().equals("vanRijn89"))          this.bl =  new BedLoad2DvanRijn89();
            if(bedLoadFormulation.value().equals("vanRijn2007"))        this.bl =  new BedLoad2DvanRijn2007();
            if(bedLoadFormulation.value().equals("MPM48"))              this.bl =  new BedLoad2DMPM48();
            if(bedLoadFormulation.value().equals("FernandezLuque_vanBeck_1976"))  this.bl =  new BedLoad2DFernandezLuque_vanBeck_1976();
            if(bedLoadFormulation.value().equals("Wilson_1966"))        this.bl =  new BedLoad2DWilson_1966();
            if(bedLoadFormulation.value().equals("Wiberg_Smith_1989"))  this.bl =  new BedLoad2DWiberg_Smith_1989();
            if(bedLoadFormulation.value().equals("EngelundHansen"))   this.bl =  new BedLoad2DEngelundHansen();
            if(bedLoadFormulation.value().equals("EngelundHansen67"))   this.bl =  new BedLoad2DEngelundHansen67();
            if(bedLoadFormulation.value().equals("EngelundHansen72"))   this.bl =  new BedLoad2DEngelundHansen72();
            if(bedLoadFormulation.value().equals("CamenenLarson2005"))  this.bl =  new BedLoad2DCL2005();
            if(bedLoadFormulation.value().equals("Zanke"))              this.bl =  new BedLoad2DZanke();
            if(bedLoadFormulation.value().equals("EHWS"))               this.bl =  new BedLoad2DEHWS();
            if(bedLoadFormulation.value().equals("Yang_1973"))          this.bl =  new BedLoadYang_1973();
            if(bedLoadFormulation.value().equals("KomarovaAndHulscher_2000"))   this.bl =  new BedLoad2DKomarovaAndHulscher_2000();
            if(bedLoadFormulation.value().equals("Einstein-Brown_1950"))   this.bl =  new BedLoadEinsteinBrown1950();
        }
        System.out.println("\tBedLoadFormula:       "+this.bl);

        if(sedimentTransportModel2DConfig.getSuspendedLoadFormula()!=null){
            TSuspendedLoadFormula suspendedLoadFormula = sedimentTransportModel2DConfig.getSuspendedLoadFormula();
            if(suspendedLoadFormula.value().equals("RossinskyDebolsky")) this.sl = new SuspendedLoad2DRD();
            if(suspendedLoadFormula.value().equals("Bagnold1966")) this.sl = new SuspendedLoad2DBagnold1966();
            if(suspendedLoadFormula.value().equals("vanRijn84")) this.sl = new SuspendedLoad2DvanRijn1984();
            if(suspendedLoadFormula.value().equals("Yang_1973")) this.sl = new SuspendedLoad2DYang_1973();
        }
        System.out.println("\tSuspendedLoadFormula: "+this.sl);
        
        if(sedimentTransportModel2DConfig.getCriticalShieldsFormula()!=null){
            TCriticalShieldsFormula suspendedLoadFormula = sedimentTransportModel2DConfig.getCriticalShieldsFormula();
            if(suspendedLoadFormula.value().equals("Classic")) this.csf = CriticalShieldsFunction.Shields;
            if(suspendedLoadFormula.value().equals("Sisyphe")) this.csf = CriticalShieldsFunction.Sisyphe;
            if(suspendedLoadFormula.value().equals("SoulsbyKnoroz")) this.csf = CriticalShieldsFunction.SoulsbyKnoroz;
            if(suspendedLoadFormula.value().equals("Soulsby")) this.csf = CriticalShieldsFunction.Soulsby;
            if(suspendedLoadFormula.value().equals("JulienKnoroz")) this.csf = CriticalShieldsFunction.JulienKnoroz;
            if(suspendedLoadFormula.value().equals("Julien")) this.csf = CriticalShieldsFunction.Julien;
            if(suspendedLoadFormula.value().equals("VanRijn")) this.csf = CriticalShieldsFunction.VanRijn;
            if(suspendedLoadFormula.value().equals("Knoroz")) this.csf = CriticalShieldsFunction.Knoroz;
        } else {
            this.csf = CriticalShieldsFunction.JulienKnoroz;
        }
        System.out.println("\tCriticalShieldsFunction: "+this.csf);

        try{
            this.morphFactor=sedimentTransportModel2DConfig.getMorphologicalFactor();
        }catch(Exception ex){this.morphFactor=1;}
        System.out.println("\tmorphological Factor = "+this.morphFactor);
    }

    /**
     * parsed die neuen xml-Elemente
     *
     * @param sedimentTransportModel2DConfig
     */
    public void parseXML(Marina.Configuration.SedimentTransportModel2D sedimentTransportModel2DConfig) {
        if (sedimentTransportModel2DConfig == null) {
            return;
        }
        if (sedimentTransportModel2DConfig.getGrainSize() != null) {
            if (sedimentTransportModel2DConfig.getGrainSize().getSingleSSSFile() != null) {
                if (sedimentTransportModel2DConfig.getGrainSize().getSingleSSSFile().getFileType() == TSSSFileType.CSV) {
                    singleSSPFileType = TSSSFileType.CSV;
                }
                singleSSPFileName = base_dir + sedimentTransportModel2DConfig.getGrainSize().getSingleSSSFile().getFileName();
            } else if (sedimentTransportModel2DConfig.getGrainSize() != null) {
                // Ist ein Modell fuer d50 gegeben ?
                if (sedimentTransportModel2DConfig.getGrainSize().getD50() != null) {
                    if (sedimentTransportModel2DConfig.getGrainSize().getD50().getTriangleMesh() != null) {
                        if (sedimentTransportModel2DConfig.getGrainSize().getD50().getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                            d50FileType = SmileIO.MeshFileType.SystemDat;
                        }
                        if (sedimentTransportModel2DConfig.getGrainSize().getD50().getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                            d50FileType = SmileIO.MeshFileType.JanetBin;
                        }
                        d50FileName = base_dir + sedimentTransportModel2DConfig.getGrainSize().getD50().getTriangleMesh().getFileName();

                    } // Ist eine konstante Korngroessenverteilung gegeben ?
                    else if (sedimentTransportModel2DConfig.getGrainSize().getD50().getConstant() != null) {
                        standardD50 = sedimentTransportModel2DConfig.getGrainSize().getD50().getConstant() / 1000.;
//                        System.out.println("\tconstant median GrainSize = " + standardD50 * 1000. + " [mm]");
                    }
                }

                // Ist ein Modell fuer dmin gegeben ?
                if (sedimentTransportModel2DConfig.getGrainSize().getDmin() != null) {
                    if (sedimentTransportModel2DConfig.getGrainSize().getDmin().getTriangleMesh() != null) {
                        if (sedimentTransportModel2DConfig.getGrainSize().getDmin().getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                            dminFileType = SmileIO.MeshFileType.SystemDat;
                        }
                        if (sedimentTransportModel2DConfig.getGrainSize().getDmin().getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                            dminFileType = SmileIO.MeshFileType.JanetBin;
                        }
                        dminFileName = base_dir + sedimentTransportModel2DConfig.getGrainSize().getDmin().getTriangleMesh().getFileName();

                    } // Ist eine konstante Korngroessenverteilung gegeben ?
                    else if (sedimentTransportModel2DConfig.getGrainSize().getDmin().getConstant() != null) {
                        lowerGrainSize = sedimentTransportModel2DConfig.getGrainSize().getDmin().getConstant() / 1000.;
//                        System.out.println("\tconstant lower GrainSize = " + lowerGrainSize * 1000. + " [mm]");
                    }
                }

                // Ist ein Modell fuer dmax gegeben ?
                if (sedimentTransportModel2DConfig.getGrainSize().getDmax() != null) {
                    if (sedimentTransportModel2DConfig.getGrainSize().getDmax().getTriangleMesh() != null) {
                        if (sedimentTransportModel2DConfig.getGrainSize().getDmax().getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                            dmaxFileType = SmileIO.MeshFileType.SystemDat;
                        }
                        if (sedimentTransportModel2DConfig.getGrainSize().getDmax().getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                            dmaxFileType = SmileIO.MeshFileType.JanetBin;
                        }
                        dmaxFileName = base_dir + sedimentTransportModel2DConfig.getGrainSize().getDmax().getTriangleMesh().getFileName();

                    } // Ist eine konstante Korngroessenverteilung gegeben ?
                    else if (sedimentTransportModel2DConfig.getGrainSize().getDmax().getConstant() != null) {
                        upperGrainSize = sedimentTransportModel2DConfig.getGrainSize().getDmax().getConstant() / 1000.;
//                        System.out.println("\tconstant upper GrainSize = " + upperGrainSize * 1000. + " [mm]");
                    }
                }

                // Ist ein Modell fuer initalSorting gegeben ?
                if (sedimentTransportModel2DConfig.getGrainSize().getSorting() != null) {
                    if (sedimentTransportModel2DConfig.getGrainSize().getSorting().getTriangleMesh() != null) {
//                            System.out.println("Lese Korngroessenmodell!");
                        if (sedimentTransportModel2DConfig.getGrainSize().getSorting().getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                            initalSortingFileType = SmileIO.MeshFileType.SystemDat;
                        }
                        if (sedimentTransportModel2DConfig.getGrainSize().getSorting().getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                            initalSortingFileType = SmileIO.MeshFileType.JanetBin;
                        }
                        initalSortingFileName = base_dir + sedimentTransportModel2DConfig.getGrainSize().getSorting().getTriangleMesh().getFileName();

                    } // Ist eine konstante Korngroessenverteilung gegeben ?
                    else if (sedimentTransportModel2DConfig.getGrainSize().getSorting().getConstant() != null) {
                        initalSorting = sedimentTransportModel2DConfig.getGrainSize().getSorting().getConstant();
//                        System.out.println("\tconstant inital Sorting = " + initalSorting + " []");
                    }
                }
            }
        } else {
        }
    }

    public BoundaryConditionsReader rndwerteReader = null;

    public BedLoad2DFormulation bl = new BedLoad2DFormulation() {
        @Override
        public String toString() {
            return "NULL";
        }

        @Override
        public double[] getLoadVector(DOF dof) {
            return new double[]{0., 0.};
        }
    };

    public SuspendedLoad2DFormulation sl = new SuspendedLoad2DFormulation() {
        @Override
        public double getLoad(DOF dof) {
            return 0.;
        }

        @Override
        public double getConcentration(DOF dof) {
            return 0.;
        }

        @Override
        public String toString() {
            return "NULL";
        }
    };

    public String concentration_name = null; // file-name with initial concentration
    public SmileIO.MeshFileType concentrationFileType = SmileIO.MeshFileType.SystemDat;
    public String rndwerte_name = null;
    public String xferg_name = "sedimenterg.bin";

    public SmileIO.MeshFileType maxerosionFileType = SmileIO.MeshFileType.SystemDat;
    public String maxErosionFileName = null;
    public double MaxErosionsTiefe = 100.;// maximale Erosionstiefe

    public SmileIO.MeshFileType noErodibleHorizonFileType = SmileIO.MeshFileType.SystemDat;
    public String noErodibleHorizonFileName = null;
    public double noErodibleHorizon = Double.NaN;// Tiefe des nicht erodierbaren Horizontes

    public MeshFileType maintainedDepthFileType = SmileIO.MeshFileType.SystemDat; // Peter 10.09.2012
    public String maintainedDepthFileName = null;
    public double maintainedDepth = -7000.;

    public double morphFactor = 1.;
    public int NumberOfThreads = 1;

    public boolean withSoilModel3DData = false;
    public int modulo = 100; // jeder wievielte Zeitschritt soll der Bodenaufbau raus geschrieben werden
    public boolean SoilModel3DResultCoarsen = false;
    public String initialSoilModel3DDataFile = null;
    public String initialSoilModel3DDataFileType = null; // z.B. csv
}
