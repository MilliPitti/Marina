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
package de.smile.marina;

import de.smile.marina.fem.*;
import de.smile.marina.fem.model.ecological.*;
import de.smile.marina.fem.model.ground.SysErgInterpolatedBathymetricModel2D;
import de.smile.marina.fem.model.hydrodynamic.dim2.*;
import de.smile.marina.fem.model.hydrodynamic.dim3.*;
import de.smile.marina.fem.model.meteorology.*;
import de.smile.marina.io.*;
import de.smile.math.ode.ivp.*;
import de.smile.xml.marina.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.*;

/**
 * Main-Class for Marina
 *
 * @author Peter Milbradt
 */
public class MarinaXML {

    public final static int majorversion = 4;
    public final static int minorversion = 9;
    public final static String update = "3.1";

    public final static boolean release = true;

    public static String referenceDate = "1970-01-01 00:00:00 UTC+1";
    public static int epsgCode = -1;

    /**
     *
     * @param args
     * @throws java.net.UnknownHostException
     */
    public static void main(String[] args) throws UnknownHostException {

        String version = "" + majorversion + "." + minorversion + "." + update;
        String author = "smile consult GmbH";
        String lizenz = "developer's license";
        if (release)
            lizenz = "full version";

        // if( args.length < 1) args = new String[]{"-gui"}; // Peter 21.05.2024

        if (args.length < 1) {
            System.out.println("usage java MarinaXML <MarinaControl.xml>");
            // System.out.println("usage java MarinaXML -gui");
            System.exit(0);
        } else {
            String xml_file = args[0];
            // if (args[0].equals("-gui")) { // der Aufruf des JFileChoosers blockiert das
            // setzen der NumerOfThreads // Peter 21.05.2024
            // javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
            // chooser.resetChoosableFileFilters();
            // chooser.removeChoosableFileFilter(chooser.getAcceptAllFileFilter());
            // chooser.addChoosableFileFilter(new de.smile.marina.io.XMLFileFilter());
            //
            // int arg = chooser.showOpenDialog(new javax.swing.JFrame());
            //
            // if(arg == javax.swing.JFileChooser.CANCEL_OPTION)
            // System.exit(0);
            //
            // if (chooser.getSelectedFile()==null) {
            // System.out.println("No file selected");
            // return;
            // }
            //
            // xml_file=chooser.getSelectedFile().getAbsolutePath();
            // }
            System.out.println("");
            System.out.println("  =======  Marina  =========");
            System.out.println("   version " + version);
            System.out.println("   " + author);
            System.out.println("   " + lizenz);
            System.out.println("  ==========================");
            System.out.println("  start on host " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("");
            // Lesen
            try {
                JAXBContext jc = JAXBContext.newInstance(de.smile.xml.marina.ObjectFactory.class);
                Unmarshaller u = jc.createUnmarshaller();
                de.smile.xml.marina.Marina mcp = (de.smile.xml.marina.Marina) u
                        .unmarshal(new FileInputStream(xml_file));

                // ControlParameter
                FEDecomposition feapp = null;

                de.smile.xml.marina.Marina.ControlParameter controlParameter = mcp.getControlParameter();

                int majornr = 0;
                int minornr = 0;
                if (mcp.getVersion() != null) {
                    majornr = mcp.getVersion().getMajor();
                    minornr = mcp.getVersion().getMinor();
                }

                System.out.println("XML-control file version: " + majornr + "." + minornr);

                if ((majornr * 10 + minornr) < 30) {
                    System.out.println(
                            "Achtung: Die in der XML-Steuerdatei spezifizierte Versionsnummer ist kleiner als Version 3.0!");
                    System.out.println(
                            "Bitte Ueberpruefen Sie, ob die XML-Steuerdatei dem aktuellen XML-Schema der Version 3.0 entspricht!");
                    System.exit(0);
                }

                // Basisverzeichnis
                String base_dir = controlParameter.getBaseDir();
                if (base_dir.startsWith(".")) {
                    // ... dann Basisverzeichnis auf Verzeichnis der XML-Datei beziehen
                    try {
                        base_dir = (new java.io.File(xml_file)).getParent() + java.io.File.separator;
                        if ((new java.io.File(xml_file)).getParent() == null)
                            base_dir = "." + java.io.File.separator;
                    } catch (Exception ex) {
                    }
                }

                if (controlParameter.getSystem().getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                    feapp = TicadIO
                            .readFESysDat(base_dir + controlParameter.getSystem().getTriangleMesh().getFileName());
                    feapp.initialMeanEdgeLength();
                } else if (controlParameter.getSystem().getTriangleMesh()
                        .getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                    feapp = SmileIO.readFEDfromJanetBin(
                            base_dir + controlParameter.getSystem().getTriangleMesh().getFileName());
                    feapp.initialMeanEdgeLength();
                }
                try {
                    epsgCode = controlParameter.getSystem().getEPSGCode();
                } catch (Exception ex) {
                }

                try {
                    referenceDate = controlParameter.getSimulationTime().getReferenceDate();
                } catch (Exception ex) {
                }
                double startTime = controlParameter.getSimulationTime().getStartTime();

                int numberOfThreads;
                try {
                    numberOfThreads = Math.min(controlParameter.getNumberOfThreads(),
                            Runtime.getRuntime().availableProcessors());
                } catch (Exception ex) {
                    numberOfThreads = Runtime.getRuntime().availableProcessors();
                }
                System.out.println("Number of threads:     " + numberOfThreads);
                System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
                        Integer.toString(Math.max(2, numberOfThreads)));

                SimpleTStep methode = new EulerTStep();
                ((EulerTStep) methode).setNumberOfThreads(numberOfThreads);

                // Configuration
                de.smile.xml.marina.Marina.Configuration configuration = mcp.getConfiguration();

                // generate modellvector
                ArrayList<TimeDependentFEModel> TimeDependentFEModels = new ArrayList<>();

                // Meteorology
                // --------------
                if (configuration.getMeteorologyCondition() != null
                        && configuration.getMeteorologicalModel2D() == null) {
                    OKWindModel meteorologyModel = new OKWindModel(feapp,
                            base_dir + configuration.getMeteorologyCondition().getFileName());
                    double[] meteorologyerg = meteorologyModel.initialSolution(startTime);
                    TimeDependentFEModels.add(new TimeDependentFEModel(meteorologyModel, meteorologyerg));
                }

                if (configuration.getMeteorologicalModel2D() != null) {
                    System.out.println("** MeteorologicalModel2D **");

                    MeteorologicalDat meteorologydat = new MeteorologicalDat();
                    if (configuration.getMeteorologicalModel2D().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        meteorologydat.xferg_name = base_dir
                                + configuration.getMeteorologicalModel2D().getResultFile().getFileName();
                    }
                    if (configuration.getMeteorologicalModel2D().getDataBased() != null) {
                        MeteorologicalModel2D metModel = null;
                        if (configuration.getMeteorologicalModel2D().getDataBased().getWindTimeSeries() != null) {
                            metModel = new MeteorologicalModel2D(feapp, base_dir + configuration
                                    .getMeteorologicalModel2D().getDataBased().getWindTimeSeries().getFileName(),
                                    meteorologydat);
                        } else if (configuration.getMeteorologicalModel2D().getDataBased().getResultFile() != null) {
                            metModel = new SysErgInterpolatedMeteorologicalModel2D(feapp, base_dir + configuration
                                    .getMeteorologicalModel2D().getDataBased().getResultFile().getFileName(),
                                    meteorologydat);
                        }
                        if (metModel != null) {
                            metModel.setStartTime(startTime);
                            metModel.initialSolution(startTime);
                            TimeDependentFEModels.add(new TimeDependentFEModel(metModel, null));
                        }
                    }
                }

                // BathymetricModel2D
                // --------------
                if (configuration.getBathymetricModel2D() != null) {
                    System.out.println("** BathymetricModel2D **");

                    if (configuration.getBathymetricModel2D().getDataBased() != null) {
                        SysErgInterpolatedBathymetricModel2D bathymetricModel = null;
                        if (configuration.getBathymetricModel2D().getDataBased().getResultFile() != null) {
                            bathymetricModel = new SysErgInterpolatedBathymetricModel2D(feapp, base_dir + configuration
                                    .getBathymetricModel2D().getDataBased().getResultFile().getFileName());
                        }
                        if (bathymetricModel != null) {
                            bathymetricModel.setStartTime(startTime);
                            TimeDependentFEModels.add(new TimeDependentFEModel(bathymetricModel, null));
                        }
                    }
                }

                // Current2D
                // --------------
                CurrentModel2D currentmodel2D = null;
                if (configuration.getCurrentModel2D() != null) {
                    System.out.println("** CurrentModel2D **");

                    CurrentDat currentdat = new CurrentDat();
                    currentdat.referenceDate = referenceDate;
                    currentdat.epsgCode = epsgCode;

                    if (configuration.getCurrentModel2D().getBoundaryCondition() != null) { // Peter 08.01.2021
                        if (configuration.getCurrentModel2D().getBoundaryCondition()
                                .getFileType() == TCurrent2DBoundaryFileType.RNDWERTE_BAW) {
                            currentdat.rndwerte_name = base_dir
                                    + configuration.getCurrentModel2D().getBoundaryCondition().getFileName();
                            currentdat.rndwerteReader = new RndwerteBAWReader(currentdat.rndwerte_name, feapp);

                        }
                    }
                    // WehrDateiName
                    if (configuration.getCurrentModel2D().getWeirFile() != null) {
                        if (configuration.getCurrentModel2D().getWeirFile()
                                .getFileType() == TWeirFileType.WEIRS_2_DXML) {
                            currentdat.weirsFileType = CurrentDat.WeirFileType.weirXML;
                            currentdat.weirsFileName = base_dir
                                    + configuration.getCurrentModel2D().getWeirFile().getFileName();
                        }
                    }

                    if (configuration.getCurrentModel2D().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        currentdat.xferg_name = base_dir
                                + configuration.getCurrentModel2D().getResultFile().getFileName();
                    }

                    // Sind Rauheiten definiert ?
                    if (configuration.getCurrentModel2D().getBottomFriction() != null) {
                        if (configuration.getCurrentModel2D().getBottomFriction().getManningStrickler() != null) {
                            currentdat.bottomFriction = CurrentDat.BottomFriction.ManningStrickler;
                            if (configuration.getCurrentModel2D().getBottomFriction().getManningStrickler()
                                    .getTriangleMesh() != null) {
                                if (configuration.getCurrentModel2D().getBottomFriction().getManningStrickler()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    currentdat.stricklerFileType = SmileIO.MeshFileType.SystemDat;
                                }
                                if (configuration.getCurrentModel2D().getBottomFriction().getManningStrickler()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    currentdat.stricklerFileType = SmileIO.MeshFileType.JanetBin;
                                }
                                currentdat.strickler_name = base_dir + configuration.getCurrentModel2D()
                                        .getBottomFriction().getManningStrickler().getTriangleMesh().getFileName();
                            } else {
                                try {
                                    currentdat.constantStrickler = configuration.getCurrentModel2D().getBottomFriction()
                                            .getManningStrickler().getConstant();
                                    currentdat.constantNikuradse = CurrentModel2DData
                                            .Strickler2Nikuradse(currentdat.constantStrickler);
                                    System.out.println(
                                            "\tconstant roughness based on Manning-Strickler in the modeldomain: kst= "
                                                    + currentdat.constantStrickler);
                                } catch (Exception e) {
                                    System.out.println(
                                            "\tconstant roughness based on Manning-Strickler in the modeldomain: kst= "
                                                    + currentdat.constantStrickler);
                                }
                            }
                        } else if (configuration.getCurrentModel2D().getBottomFriction().getNikuradse() != null) {
                            currentdat.bottomFriction = CurrentDat.BottomFriction.Nikuradse;
                            if (configuration.getCurrentModel2D().getBottomFriction().getNikuradse()
                                    .getTriangleMesh() != null) {
                                if (configuration.getCurrentModel2D().getBottomFriction().getNikuradse()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    currentdat.nikuradseFileType = SmileIO.MeshFileType.SystemDat;
                                }
                                if (configuration.getCurrentModel2D().getBottomFriction().getNikuradse()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    currentdat.nikuradseFileType = SmileIO.MeshFileType.JanetBin;
                                }
                                currentdat.nikuradse_name = base_dir + configuration.getCurrentModel2D()
                                        .getBottomFriction().getNikuradse().getTriangleMesh().getFileName();
                            } else {
                                try {
                                    currentdat.constantNikuradse = configuration.getCurrentModel2D().getBottomFriction()
                                            .getNikuradse().getConstant();
                                    currentdat.constantStrickler = CurrentModel2DData
                                            .Nikuradse2Strickler(currentdat.constantNikuradse);
                                    System.out
                                            .println("\tconstant roughness based on Nikuradse in the modeldomain: ks= "
                                                    + currentdat.constantNikuradse);
                                } catch (Exception e) {
                                    System.out
                                            .println("\tconstant roughness based on Nikuradse in the modeldomain: ks= "
                                                    + currentdat.constantNikuradse);
                                }
                            }
                        } else
                        // @deprecated
                        // ManningStrickler Rauheit
                        if (configuration.getCurrentModel2D().getBottomFriction().getTriangleMesh() != null) {
                            System.out.println(
                                    "Veraenderung in der xml-Datei - in Zukunft das zusaetzliche Schluesselwort ManningStrickler verwenden");
                            if (configuration.getCurrentModel2D().getBottomFriction().getTriangleMesh()
                                    .getFileType() == TMeshFileType.SYS_DAT_FILE)
                                currentdat.stricklerFileType = SmileIO.MeshFileType.SystemDat;
                            if (configuration.getCurrentModel2D().getBottomFriction().getTriangleMesh()
                                    .getFileType() == TMeshFileType.JANET_BINARY_FILE)
                                currentdat.stricklerFileType = SmileIO.MeshFileType.JanetBin;
                            currentdat.strickler_name = base_dir + configuration.getCurrentModel2D().getBottomFriction()
                                    .getTriangleMesh().getFileName();
                        }
                        // Ist eine konstante Rauheit gegeben ?
                        else
                            try {
                                currentdat.constantStrickler = configuration.getCurrentModel2D().getBottomFriction()
                                        .getConstant();
                                System.out.println(
                                        "Veraenderung in der xml-Datei - in Zukunft das zusaetzliche Schluesselwort ManningStrickler verwenden");
                                System.out
                                        .println("konstante Rauhigkeitsbeiwerte nach Manning-Strickler im Gebiet: kst= "
                                                + currentdat.constantStrickler);
                            } catch (Exception e) {
                                System.out
                                        .println("konstante Rauhigkeitsbeiwerte nach Manning-Strickler im Gebiet: kst= "
                                                + currentdat.constantStrickler);
                            }
                        // @deprecated
                    } else {
                        System.out.println("\tnot specified roughness - constant Manning-Strickler value: "
                                + currentdat.constantStrickler + " used!");
                    }

                    try {
                        currentdat.watt = configuration.getCurrentModel2D().getDryFallBound();
                    } catch (Exception ex) {
                    }

                    try { // undocumented
                        currentdat.infiltrationRate = configuration.getCurrentModel2D().getInfiltrationRate();
                    } catch (Exception ex) {
                        /* System.out.println("no InfiltrationRate"); */}

                    try {
                        double latitude = configuration.getCurrentModel2D().getLatitude();
                        CurrentModel2D.setLatitude(latitude);
                        System.out.println("Latitude: lat=" + latitude + ", Coriolis Parameter: f="
                                + CurrentModel2D.getCoriolisParameter());
                    } catch (Exception ex) {
                    }

                    currentdat.NumberOfThreads = numberOfThreads;

                    currentmodel2D = new CurrentModel2D(feapp, currentdat);
                    currentmodel2D.setStartTime(startTime);

                    double[] currerg = null;

                    // Startwerte initialisiert
                    if (configuration.getCurrentModel2D().getInitialCondition() != null) {
                        // Ist eine Startdatei vorhanden ?
                        if (configuration.getCurrentModel2D().getInitialCondition().getStartFromResult() != null) {
                            // Die Unterscheidung in Dateiformate fehlt noch ...
                            currentdat.startWerteDatei = base_dir + configuration.getCurrentModel2D()
                                    .getInitialCondition().getStartFromResult().getFileName();
                            currentdat.startSatz = configuration.getCurrentModel2D().getInitialCondition()
                                    .getStartFromResult().getTimeStepCounter();
                            boolean useSysDatZ = (configuration.getSedimentTransportModel2D() == null);
                            try {
                                currerg = currentmodel2D.initialSolutionFromTicadErgFile(currentdat.startWerteDatei,
                                        currentdat.startSatz, useSysDatZ);
                            } catch (Exception e) {
                                System.out.println("\t !! Current StartFile:" + currentdat.startWerteDatei
                                        + " can not be opened !!");
                                System.out.println(e);
                                System.exit(0);
                            }

                        } else
                        // Ist ein Initialwasserstandsmodell vorhanden ?
                        if (configuration.getCurrentModel2D().getInitialCondition().getWaterLevel() != null) {
                            if (configuration.getCurrentModel2D().getInitialCondition().getWaterLevel()
                                    .getTriangleMesh() != null) {
                                if (configuration.getCurrentModel2D().getInitialCondition().getWaterLevel()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    currentdat.waterLevelFileType = SmileIO.MeshFileType.SystemDat;
                                    currentdat.waterlevel_name = base_dir + configuration.getCurrentModel2D()
                                            .getInitialCondition().getWaterLevel().getTriangleMesh().getFileName();
                                    currerg = currentmodel2D.initialHfromSysDat(currentdat.waterlevel_name, startTime);
                                }
                                if (configuration.getCurrentModel2D().getInitialCondition().getWaterLevel()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    currentdat.waterLevelFileType = SmileIO.MeshFileType.JanetBin;
                                    currentdat.waterlevel_name = base_dir + configuration.getCurrentModel2D()
                                            .getInitialCondition().getWaterLevel().getTriangleMesh().getFileName();
                                    currerg = currentmodel2D.initialHfromJanetBin(currentdat.waterlevel_name,
                                            startTime);
                                }
                            }

                            // Ist ein konstanter Initialwasserstand vorhanden ?
                            else if (configuration.getCurrentModel2D().getInitialCondition().getWaterLevel()
                                    .getConstant() != null) {
                                double Cwaterlevel = configuration.getCurrentModel2D().getInitialCondition()
                                        .getWaterLevel().getConstant();
                                currerg = currentmodel2D.constantInitialWaterLevel(Cwaterlevel);
                            }
                        }

                    } else {
                        // Interpolation aus Randwerten
                        currerg = currentmodel2D.initialSolution(startTime);
                    }

                    currentmodel2D.setBoundaryConditions();

                    currentmodel2D.setMaxTimeStep(
                            Math.min(Math.abs(controlParameter.getSimulationTime().getResultTimeStep()) / 100., 0.1));

                    TimeDependentFEModels.add(new TimeDependentFEModel(currentmodel2D, currerg));
                }

                // CurrentModel3D
                // --------------
                CurrentModel3D currentmodel3D = null;
                if (configuration.getCurrentModel3D() != null) {
                    System.out.println("** CurrentModel3D **");
                    CurrentDat currentdat = new CurrentDat();
                    currentdat.referenceDate = referenceDate;

                    if (configuration.getCurrentModel3D().getBoundaryCondition()
                            .getFileType() == TCurrent2DBoundaryFileType.RNDWERTE_BAW) {
                        currentdat.rndwerte_name = base_dir
                                + configuration.getCurrentModel3D().getBoundaryCondition().getFileName();
                        currentdat.rndwerteReader = new RndwerteBAWReader(currentdat.rndwerte_name, feapp);
                    }

                    if (configuration.getCurrentModel3D().getResultFile2D() != null)
                        if (configuration.getCurrentModel3D().getResultFile2D()
                                .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                            currentdat.xferg_name = base_dir
                                    + configuration.getCurrentModel3D().getResultFile2D().getFileName();
                        }
                    if (configuration.getCurrentModel3D().getResultFile() != null)
                        if (configuration.getCurrentModel3D().getResultFile()
                                .getFileType() == TResultFileType.CURRENT_3_D_ERG) {
                            currentdat.current3d_erg_name = base_dir
                                    + configuration.getCurrentModel3D().getResultFile().getFileName();
                        }
                    // Sind Rauheiten definiert ?
                    if (configuration.getCurrentModel3D().getBottomFriction() != null) {
                        if (configuration.getCurrentModel3D().getBottomFriction().getManningStrickler() != null) {
                            currentdat.bottomFriction = CurrentDat.BottomFriction.ManningStrickler;
                            if (configuration.getCurrentModel3D().getBottomFriction().getManningStrickler()
                                    .getTriangleMesh() != null) {
                                if (configuration.getCurrentModel3D().getBottomFriction().getManningStrickler()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    currentdat.stricklerFileType = SmileIO.MeshFileType.SystemDat;
                                }
                                if (configuration.getCurrentModel3D().getBottomFriction().getManningStrickler()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    currentdat.stricklerFileType = SmileIO.MeshFileType.JanetBin;
                                }
                                currentdat.strickler_name = base_dir + configuration.getCurrentModel3D()
                                        .getBottomFriction().getManningStrickler().getTriangleMesh().getFileName();
                            } else {
                                try {
                                    currentdat.constantStrickler = configuration.getCurrentModel3D().getBottomFriction()
                                            .getManningStrickler().getConstant();
                                    System.out.println(
                                            "konstante Rauhigkeitsbeiwerte nach Manning-Strickler im Gebiet: kst= "
                                                    + currentdat.constantStrickler);
                                } catch (Exception e) {
                                    System.out.println(
                                            "konstante Rauhigkeitsbeiwerte nach Manning-Strickler im Gebiet: kst= "
                                                    + currentdat.constantStrickler);
                                }
                            }
                        }
                        if (configuration.getCurrentModel3D().getBottomFriction().getNikuradse() != null) {
                            currentdat.bottomFriction = CurrentDat.BottomFriction.Nikuradse;
                            if (configuration.getCurrentModel3D().getBottomFriction().getNikuradse()
                                    .getTriangleMesh() != null) {
                                if (configuration.getCurrentModel3D().getBottomFriction().getNikuradse()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    currentdat.nikuradseFileType = SmileIO.MeshFileType.SystemDat;
                                }
                                if (configuration.getCurrentModel3D().getBottomFriction().getNikuradse()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    currentdat.nikuradseFileType = SmileIO.MeshFileType.JanetBin;
                                }
                                currentdat.nikuradse_name = base_dir + configuration.getCurrentModel3D()
                                        .getBottomFriction().getNikuradse().getTriangleMesh().getFileName();
                            } else {
                                try {
                                    currentdat.constantNikuradse = configuration.getCurrentModel3D().getBottomFriction()
                                            .getNikuradse().getConstant();
                                    System.out.println("konstante Rauhigkeitsbeiwerte nach Nikuradse im Gebiet: ks= "
                                            + currentdat.constantNikuradse);
                                } catch (Exception e) {
                                    System.out.println("konstante Rauhigkeitsbeiwerte nach Nikuradse im Gebiet: ks= "
                                            + currentdat.constantNikuradse);
                                }
                            }
                        }
                    } else {
                        System.out.println("Keine Rauheit gegeben !");
                    }

                    try {
                        currentdat.watt = configuration.getCurrentModel3D().getDryFallBound();
                    } catch (Exception ex) {
                    }
                    currentdat.NumberOfThreads = numberOfThreads;

                    List<Double> tiefenLayer = configuration.getCurrentModel3D().getDepthLayer().getDepth();
                    java.util.Collections.sort(tiefenLayer);
                    int anz = tiefenLayer.size();
                    double[] tiefen = new double[anz];
                    for (int i = 0; i < anz; i++)
                        tiefen[i] = tiefenLayer.get(i);

                    currentmodel3D = new CurrentModel3D(feapp, currentdat, tiefen);
                    currentmodel3D.setStartTime(startTime);
                    currentmodel3D.setMaxTimeStep(0.1);

                    double[] currerg;

                    // Sollen Startwerte initialisiert werden ?
                    double Cwaterlevel = 0.0;
                    boolean Cwaterlevelset = false;
                    if (configuration.getCurrentModel3D().getInitialCondition() != null) {
                        // Ist eine Startdatei vorhanden ?
                        if (configuration.getCurrentModel3D().getInitialCondition().getStartFromResult() != null) {
                            if (configuration.getCurrentModel3D().getInitialCondition().getStartFromResult()
                                    .getFileType() == TResultFileType.CURRENT_3_D_ERG) {
                                currentdat.startWerte3DDatei = base_dir + configuration.getCurrentModel3D()
                                        .getInitialCondition().getStartFromResult().getFileName();
                                currentdat.start3DSatz = configuration.getCurrentModel3D().getInitialCondition()
                                        .getStartFromResult().getTimeStepCounter();
                            } else if (configuration.getCurrentModel3D().getInitialCondition().getStartFromResult()
                                    .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                                currentdat.startWerteDatei = base_dir + configuration.getCurrentModel3D()
                                        .getInitialCondition().getStartFromResult().getFileName();
                                currentdat.startSatz = configuration.getCurrentModel3D().getInitialCondition()
                                        .getStartFromResult().getTimeStepCounter();
                            }
                        } else
                        // Ist ein Initialwasserstandsmodell vorhanden ?
                        if (configuration.getCurrentModel3D().getInitialCondition().getWaterLevel() != null) {
                            if (configuration.getCurrentModel3D().getInitialCondition().getWaterLevel()
                                    .getTriangleMesh() != null) {
                                if (configuration.getCurrentModel3D().getInitialCondition().getWaterLevel()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    currentdat.waterLevelFileType = SmileIO.MeshFileType.SystemDat;
                                    currentdat.waterlevel_name = base_dir + configuration.getCurrentModel3D()
                                            .getInitialCondition().getWaterLevel().getTriangleMesh().getFileName();
                                    currerg = currentmodel3D.initialHfromSysDat(currentdat.waterlevel_name, startTime);
                                }
                                if (configuration.getCurrentModel3D().getInitialCondition().getWaterLevel()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    currentdat.waterLevelFileType = SmileIO.MeshFileType.JanetBin;
                                    currentdat.waterlevel_name = base_dir + configuration.getCurrentModel3D()
                                            .getInitialCondition().getWaterLevel().getTriangleMesh().getFileName();
                                    currerg = currentmodel3D.initialHfromJanetBin(currentdat.waterlevel_name,
                                            startTime);
                                }
                            }

                            // Ist ein konstanter Initialwasserstand vorhanden ?
                            else if (configuration.getCurrentModel3D().getInitialCondition().getWaterLevel()
                                    .getConstant() != null) {
                                currerg = currentmodel3D.constantInitialWaterLevel(configuration.getCurrentModel3D()
                                        .getInitialCondition().getWaterLevel().getConstant());
                            }
                        }
                    } else {
                        System.out.println("Keine Startwerte gegeben !");
                    }
                    boolean useSysDatZ = (configuration.getSedimentTransportModel2D() == null); // Peter 20.11.2015 Wenn
                                                                                                // ein Sedimentmodell
                    if (currentdat.startWerte3DDatei != null) {
                        try {
                            currerg = currentmodel3D.initialSolutionfromErgFile(currentdat.startWerte3DDatei,
                                    currentdat.start3DSatz, useSysDatZ);
                        } catch (Exception e) {
                            System.out.println("Current3DErg-StartDatei laesst sich nicht einlesen!");
                            currerg = currentmodel3D.initialSolution(startTime);
                        }
                    } else {
                        if (currentdat.startWerteDatei != null)
                            try {
                                currerg = currentmodel3D.initialSolutionFromTicadErgFile(currentdat.startWerteDatei,
                                        currentdat.startSatz, useSysDatZ);
                            } catch (Exception e) {
                                System.out.println("TicadSysErg-StartDatei: " + currentdat.startWerteDatei
                                        + " laesst sich nicht einlesen!");
                                currerg = currentmodel3D.initialSolution(startTime);
                            }
                        else if (Cwaterlevelset)
                            currerg = currentmodel3D.constantInitialWaterLevel(Cwaterlevel);
                        else
                            currerg = currentmodel3D.initialSolution(startTime);
                    }

                    TimeDependentFEModels.add(new TimeDependentFEModel(currentmodel3D, currerg));

                }

                // Sediment2D
                // --------------
                if (configuration.getSedimentTransportModel2D() != null) {
                    System.out.println("** SedimentModel **");

                    SedimentDat sedimentdat = new SedimentDat(configuration.getSedimentTransportModel2D());
                    sedimentdat.NumberOfThreads = numberOfThreads;
                    sedimentdat.base_dir = base_dir;
                    sedimentdat.referenceDate = referenceDate;
                    sedimentdat.epsgCode = epsgCode;

                    // Sind erodierbare Tiefen definiert ?
                    if (configuration.getSedimentTransportModel2D().getMaxErosion() != null) {
                        boolean correct = false;
                        // Ist ein Modell fuer erodierbare Tiefen gegeben ?
                        if (configuration.getSedimentTransportModel2D().getMaxErosion().getTriangleMesh() != null) {
                            if (configuration.getSedimentTransportModel2D().getMaxErosion().getTriangleMesh()
                                    .getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                sedimentdat.maxerosionFileType = SmileIO.MeshFileType.SystemDat;
                            }
                            if (configuration.getSedimentTransportModel2D().getMaxErosion().getTriangleMesh()
                                    .getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                sedimentdat.maxerosionFileType = SmileIO.MeshFileType.JanetBin;
                            }
                            sedimentdat.maxErosionFileName = base_dir + configuration.getSedimentTransportModel2D()
                                    .getMaxErosion().getTriangleMesh().getFileName();
                            correct = true;
                        } // Ist eine konstante maximale Erosion gegeben ?
                        else if (configuration.getSedimentTransportModel2D().getMaxErosion().getConstant() != null) {
                            sedimentdat.MaxErosionsTiefe = configuration.getSedimentTransportModel2D().getMaxErosion()
                                    .getConstant();
                            correct = true;
                        }
                        if (!correct) {
                            System.exit(0);
                        }
                    }
                    if (configuration.getSedimentTransportModel2D().getNoErodibleHorizon() != null) {
                        boolean correct = false;
                        // Ist ein Modell fuer erodierbare Tiefen gegeben ?
                        if (configuration.getSedimentTransportModel2D().getNoErodibleHorizon()
                                .getTriangleMesh() != null) {
                            if (configuration.getSedimentTransportModel2D().getNoErodibleHorizon().getTriangleMesh()
                                    .getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                sedimentdat.noErodibleHorizonFileType = SmileIO.MeshFileType.SystemDat;
                            }
                            if (configuration.getSedimentTransportModel2D().getNoErodibleHorizon().getTriangleMesh()
                                    .getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                sedimentdat.noErodibleHorizonFileType = SmileIO.MeshFileType.JanetBin;
                            }
                            sedimentdat.noErodibleHorizonFileName = base_dir
                                    + configuration.getSedimentTransportModel2D().getNoErodibleHorizon()
                                            .getTriangleMesh().getFileName();
                            correct = true;
                        } // Ist ein konstanter nicht erodiernbarer Horizont gegeben ?
                        else if (configuration.getSedimentTransportModel2D().getNoErodibleHorizon()
                                .getConstant() != null) {
                            sedimentdat.noErodibleHorizon = configuration.getSedimentTransportModel2D()
                                    .getNoErodibleHorizon().getConstant();
                            correct = true;
                        }
                        if (!correct) {
                            System.exit(0);
                        }
                    }

                    // Sind Baggertiefen definiert ?
                    if (configuration.getSedimentTransportModel2D().getMaintainedDepth() != null) {
                        boolean correct = false;
                        // Ist ein Modell fuer Baggertiefen gegeben ?
                        if (configuration.getSedimentTransportModel2D().getMaintainedDepth()
                                .getTriangleMesh() != null) {
                            if (configuration.getSedimentTransportModel2D().getMaintainedDepth().getTriangleMesh()
                                    .getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                sedimentdat.maintainedDepthFileType = SmileIO.MeshFileType.SystemDat;
                            }
                            if (configuration.getSedimentTransportModel2D().getMaintainedDepth().getTriangleMesh()
                                    .getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                sedimentdat.maintainedDepthFileType = SmileIO.MeshFileType.JanetBin;
                            }
                            sedimentdat.maintainedDepthFileName = base_dir + configuration.getSedimentTransportModel2D()
                                    .getMaintainedDepth().getTriangleMesh().getFileName();
                            correct = true;
                        } // Ist eine konstante Baggertiefen gegeben ?
                        else if (configuration.getSedimentTransportModel2D().getMaintainedDepth()
                                .getConstant() != null) {
                            sedimentdat.maintainedDepth = configuration.getSedimentTransportModel2D()
                                    .getMaintainedDepth().getConstant();
                            correct = true;
                        }
                        if (!correct) {
                            System.exit(0);
                        }
                    }

                    // Sind Korngroessenangaben definiert ?
                    if (configuration.getSedimentTransportModel2D().getGrainSize() != null) {
                        sedimentdat.parseXML(configuration.getSedimentTransportModel2D());
                    } else {
                        System.out.println("Keine Werte fuer Korndurchmesser d50 gegeben (default value = 0.42mm)!");
                    }

                    if (configuration.getSedimentTransportModel2D().getBoundaryCondition() != null)
                        if (configuration.getSedimentTransportModel2D().getBoundaryCondition()
                                .getFileType() == TSediment2DBoundaryFileType.RNDWERTE_BAW)
                            sedimentdat.rndwerte_name = base_dir
                                    + configuration.getSedimentTransportModel2D().getBoundaryCondition().getFileName();
                    sedimentdat.rndwerteReader = new RndwerteBAWReader(sedimentdat.rndwerte_name, feapp);

                    if (configuration.getSedimentTransportModel2D().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        sedimentdat.xferg_name = base_dir
                                + configuration.getSedimentTransportModel2D().getResultFile().getFileName();
                    }

                    // *** SoilModel ***
                    if (configuration.getSedimentTransportModel2D().getSoilModel() != null) {
                        sedimentdat.withSoilModel3DData = true;
                        if (configuration.getSedimentTransportModel2D().getSoilModel().getWriteModulo() != null)
                            sedimentdat.modulo = configuration.getSedimentTransportModel2D().getSoilModel()
                                    .getWriteModulo().intValue();
                        sedimentdat.SoilModel3DResultCoarsen = false;
                        try {
                            sedimentdat.SoilModel3DResultCoarsen = configuration.getSedimentTransportModel2D()
                                    .getSoilModel().getInitialSoilModel().isCoarsen();
                        } catch (Exception e) {
                        }
                        if (configuration.getSedimentTransportModel2D().getSoilModel().getInitialSoilModel() != null) {
                            sedimentdat.initialSoilModel3DDataFile = configuration.getSedimentTransportModel2D()
                                    .getSoilModel().getInitialSoilModel().getFileName();
                            sedimentdat.initialSoilModel3DDataFileType = configuration.getSedimentTransportModel2D()
                                    .getSoilModel().getInitialSoilModel().getFileType().value();
                        }
                    }

                    SedimentModel2D sedimentmodel = null;
                    if (currentmodel2D != null)
                        sedimentmodel = new SedimentModel2D(currentmodel2D, sedimentdat);
                    if (currentmodel3D != null)
                        sedimentmodel = new SedimentModel2D(currentmodel3D, sedimentdat);

                    if (sedimentmodel != null) {
                        sedimentmodel.setStartTime(startTime);
                        sedimentmodel.setMaxTimeStep(0.1);

                        double[] sedimenterg = null;

                        // Startwerte fuer Konzentrationen initialisiert
                        if (configuration.getSedimentTransportModel2D().getInitialCondition() != null) {
                            // Ist eine Startdatei vorhanden ?
                            if (configuration.getSedimentTransportModel2D().getInitialCondition()
                                    .getStartFromResult() != null) {
                                // Die Unterscheidung in Dateiformate fehlt noch ...
                                try {
                                    sedimenterg = sedimentmodel.initialSolutionFromTicadErgFile(
                                            base_dir + configuration.getSedimentTransportModel2D().getInitialCondition()
                                                    .getStartFromResult().getFileName(),
                                            configuration.getSedimentTransportModel2D().getInitialCondition()
                                                    .getStartFromResult().getTimeStepCounter());
                                } catch (Exception e) {
                                    System.out.println("\t !! StartDatei laesst sich nicht einlesen !!");
                                    System.exit(0);
                                }

                            } else // Ist ein Initialkonzentrationsmodell vorhanden ?
                            if (configuration.getSedimentTransportModel2D().getInitialCondition()
                                    .getConcentration() != null) {
                                if (configuration.getSedimentTransportModel2D().getInitialCondition().getConcentration()
                                        .getTriangleMesh() != null) {
                                    if (configuration.getSedimentTransportModel2D().getInitialCondition()
                                            .getConcentration().getTriangleMesh()
                                            .getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                        sedimentdat.concentrationFileType = SmileIO.MeshFileType.SystemDat;
                                        sedimentdat.concentration_name = base_dir
                                                + configuration.getSedimentTransportModel2D().getInitialCondition()
                                                        .getConcentration().getTriangleMesh().getFileName();
                                        sedimenterg = sedimentmodel.initialConcentrationFromSysDat(
                                                sedimentdat.concentration_name, startTime);
                                    }
                                    if (configuration.getSedimentTransportModel2D().getInitialCondition()
                                            .getConcentration().getTriangleMesh()
                                            .getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                        sedimentdat.concentrationFileType = SmileIO.MeshFileType.JanetBin;
                                        sedimentdat.concentration_name = base_dir
                                                + configuration.getSedimentTransportModel2D().getInitialCondition()
                                                        .getConcentration().getTriangleMesh().getFileName();
                                        sedimenterg = sedimentmodel.initialConcentrationFromJanetBin(
                                                sedimentdat.concentration_name, startTime);
                                    }
                                } // Ist eine konstante Konzentration vorgegeben ?
                                else if (configuration.getSedimentTransportModel2D().getInitialCondition()
                                        .getConcentration().getConstant() != null) {
                                    double CConc = configuration.getSedimentTransportModel2D().getInitialCondition()
                                            .getConcentration().getConstant();
                                    sedimenterg = sedimentmodel.constantInitialSolution(CConc);
                                }
                            }
                        } else {
                            // Interpolation aus Randwerten
                            sedimenterg = sedimentmodel.initialSolution(startTime);
                        }

                        // Sind Duenenparameter definiert ?
                        if (configuration.getSedimentTransportModel2D().getDuneParameter() != null) {
                            if (configuration.getSedimentTransportModel2D().getDuneParameter()
                                    .getReadFromResult() != null) {
                                try {
                                    sedimentmodel.readDuneParameterFromTicadErgFile(
                                            base_dir + configuration.getSedimentTransportModel2D().getDuneParameter()
                                                    .getReadFromResult().getFileName(),
                                            configuration.getSedimentTransportModel2D().getDuneParameter()
                                                    .getReadFromResult().getTimeStepCounter());
                                } catch (Exception e) {
                                    System.out
                                            .println("\t !! Duenenparameter laesst sich nicht aus "
                                                    + (base_dir + configuration.getSedimentTransportModel2D()
                                                            .getDuneParameter().getReadFromResult().getFileName())
                                                    + " einlesen !!");
                                    System.exit(0);
                                }

                            } else if (configuration.getSedimentTransportModel2D().getDuneParameter().getDuneHight()
                                    .getTriangleMesh() != null) {
                                if (configuration.getSedimentTransportModel2D().getDuneParameter().getDuneHight()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    sedimentmodel.readDuneHightFromSysDat(
                                            base_dir + configuration.getSedimentTransportModel2D().getDuneParameter()
                                                    .getDuneHight().getTriangleMesh().getFileName());
                                }
                                if (configuration.getSedimentTransportModel2D().getDuneParameter().getDuneHight()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    sedimentmodel.readDuneHightFromJanetBin(
                                            base_dir + configuration.getSedimentTransportModel2D().getDuneParameter()
                                                    .getDuneHight().getTriangleMesh().getFileName());
                                }
                            } else if (configuration.getSedimentTransportModel2D().getDuneParameter().getDuneHight()
                                    .getConstant() != null) {
                                System.out.println("Set Duenhight as constant not implemented yet"); // ToDo
                            }
                        }
                        // *** SoilModel ***
                        if (sedimentdat.withSoilModel3DData) {
                            if (configuration.getSedimentTransportModel2D().getSoilModel()
                                    .getInitialSoilModel() != null) {
                                sedimentdat.initialSoilModel3DDataFile = base_dir
                                        + configuration.getSedimentTransportModel2D().getSoilModel()
                                                .getInitialSoilModel().getFileName();
                                boolean coarsen = false;
                                try {
                                    coarsen = configuration.getSedimentTransportModel2D().getSoilModel()
                                            .getInitialSoilModel().isCoarsen();
                                } catch (Exception e) {
                                }
                                if (configuration.getSedimentTransportModel2D().getSoilModel().getInitialSoilModel()
                                        .getFileType() == TSoilModelFileType.CSV) {
                                    sedimentmodel.readInitialSoilModel3DDataFromCVS(
                                            sedimentdat.initialSoilModel3DDataFile, coarsen);
                                }
                            }
                        }
                        TimeDependentFEModels.add(new TimeDependentFEModel(sedimentmodel, sedimenterg));
                    }
                }

                // WaveHypModel
                // --------------
                if (configuration.getWaveHypModel() != null) {
                    System.out.println("** hyperbolic WaveModel **");

                    WaveHYPDat wavehypdat = new WaveHYPDat();
                    wavehypdat.referenceDate = referenceDate;

                    if (configuration.getWaveHypModel().getBoundaryCondition()
                            .getFileType() == TWaveBoundaryFileType.WAVE_RANDN_DAT) {
                        wavehypdat.randn_name = base_dir
                                + configuration.getWaveHypModel().getBoundaryCondition().getFileName();
                    }

                    if (configuration.getWaveHypModel().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        wavehypdat.xferg_name = base_dir
                                + configuration.getWaveHypModel().getResultFile().getFileName();
                    }

                    if (configuration.getWaveHypModel().getInitialCondition() != null) {
                        // Ist eine Startdatei vorhanden ?
                        if (configuration.getWaveHypModel().getInitialCondition().getStartFromResult() != null) {
                            // Die Unterscheidung in Dateiformate fehlt noch ...
                            wavehypdat.startWerteDatei = base_dir + configuration.getWaveHypModel()
                                    .getInitialCondition().getStartFromResult().getFileName();
                            wavehypdat.startSatz = configuration.getWaveHypModel().getInitialCondition()
                                    .getStartFromResult().getTimeStepCounter();
                        }
                    }

                    try {
                        wavehypdat.waterLevelOffSet = configuration.getWaveHypModel().getWaterLevelOffSet();
                    } catch (Exception ex) {
                        wavehypdat.waterLevelOffSet = 0.;
                    }

                    wavehypdat.NumberOfThreads = numberOfThreads;

                    WaveHYPModel2D wavehypmodel = new WaveHYPModel2D(feapp, wavehypdat);
                    wavehypmodel.setStartTime(startTime);
                    wavehypmodel.setMaxTimeStep(0.1);

                    double[] wavehyperg = null;

                    if (wavehypdat.startWerteDatei != null)
                        try {
                            wavehyperg = wavehypmodel.initialSolutionFromTicadErgFile(wavehypdat.startWerteDatei,
                                    wavehypdat.startSatz);
                        } catch (Exception e) {
                            System.out.println("\t !! StartDatei laesst sich nicht einlesen !!");
                            System.exit(0);
                        }
                    else
                        wavehyperg = wavehypmodel.initialSolution(startTime);

                    TimeDependentFEModels.add(new TimeDependentFEModel(wavehypmodel, wavehyperg));

                }

                // SaltTransport 2D
                // --------------
                if (configuration.getSaltTransportModel2D() != null) {
                    System.out.println("** SaltTransportModel2D **");
                    SaltDat saltdat = new SaltDat();

                    if (configuration.getSaltTransportModel2D().getBoundaryCondition()
                            .getFileType() == TSalt2DBoundaryFileType.RNDWERTE_BAW) {
                        saltdat.saltrndwerte_name = base_dir
                                + configuration.getSaltTransportModel2D().getBoundaryCondition().getFileName();
                        saltdat.rndwerteReader = new RndwerteBAWReader(saltdat.saltrndwerte_name, feapp);
                    }

                    if (configuration.getSaltTransportModel2D().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        saltdat.xferg_name = base_dir
                                + configuration.getSaltTransportModel2D().getResultFile().getFileName();
                    }

                    saltdat.NumberOfThreads = numberOfThreads;
                    SaltModel2D saltmodel = null;
                    if (currentmodel2D != null)
                        saltmodel = new SaltModel2D(currentmodel2D, saltdat);
                    else {
                        System.out.println("no CurrentModel2D defined!");
                        System.exit(0);
                    }
                    saltmodel.setStartTime(startTime);
                    saltmodel.setMaxTimeStep(0.1);

                    double[] salterg = null;

                    // Startwerte fuer Konzentrationen initialisiert
                    if (configuration.getSaltTransportModel2D().getInitialCondition() != null) {
                        // Ist eine Startdatei vorhanden ?
                        if (configuration.getSaltTransportModel2D().getInitialCondition()
                                .getStartFromResult() != null) {
                            // Die Unterscheidung in Dateiformate fehlt noch ...
                            try {
                                salterg = saltmodel.initialSolutionFromTicadErgFile(
                                        base_dir + configuration.getSaltTransportModel2D().getInitialCondition()
                                                .getStartFromResult().getFileName(),
                                        configuration.getSaltTransportModel2D().getInitialCondition()
                                                .getStartFromResult().getTimeStepCounter());
                            } catch (Exception e) {
                                System.out.println("\t !! StartDatei laesst sich nicht einlesen !!");
                                System.exit(0);
                            }
                        } else
                        // Ist ein Initialkonzentrationsmodell vorhanden ?
                        if (configuration.getSaltTransportModel2D().getInitialCondition().getConcentration() != null) {
                            if (configuration.getSaltTransportModel2D().getInitialCondition().getConcentration()
                                    .getTriangleMesh() != null) {
                                if (configuration.getSaltTransportModel2D().getInitialCondition().getConcentration()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    saltdat.concentrationFileType = SmileIO.MeshFileType.SystemDat;
                                    saltdat.concentration_name = base_dir + configuration.getSaltTransportModel2D()
                                            .getInitialCondition().getConcentration().getTriangleMesh().getFileName();
                                    salterg = saltmodel.initialSaltConcentrationFromSysDat(saltdat.concentration_name,
                                            startTime);
                                }
                                if (configuration.getSaltTransportModel2D().getInitialCondition().getConcentration()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    saltdat.concentrationFileType = SmileIO.MeshFileType.JanetBin;
                                    saltdat.concentration_name = base_dir + configuration.getSaltTransportModel2D()
                                            .getInitialCondition().getConcentration().getTriangleMesh().getFileName();
                                    salterg = saltmodel.initialSaltConcentrationFromJanetBin(saltdat.concentration_name,
                                            startTime);
                                }
                            }

                            // Ist eine konstante Konzentration vorgegeben ?
                            else if (configuration.getSaltTransportModel2D().getInitialCondition().getConcentration()
                                    .getConstant() != null) {
                                double CConc = configuration.getSaltTransportModel2D().getInitialCondition()
                                        .getConcentration().getConstant();
                                salterg = saltmodel.constantInitialSolution(CConc);
                            }
                        }
                    } else {
                        // Interpolation aus Randwerten
                        salterg = saltmodel.initialSolution(startTime);
                    }

                    TimeDependentFEModels.add(new TimeDependentFEModel(saltmodel, salterg));

                }

                // Fluid Mud Flow 2D
                // --------------
                if (configuration.getFluidMudFlowModel2D() != null) {
                    FluidMudFlowDat fluidMudDat = new FluidMudFlowDat();

                    if (configuration.getFluidMudFlowModel2D().getBoundaryCondition()
                            .getFileType() == TFluidMudFlow2DBoundaryFileType.RNDWERTE_BAW) {
                        fluidMudDat.rndwerte_name = base_dir
                                + configuration.getFluidMudFlowModel2D().getBoundaryCondition().getFileName();
                        fluidMudDat.rndwerteReader = new RndwerteBAWReader(fluidMudDat.rndwerte_name, feapp);

                    }

                    if (configuration.getFluidMudFlowModel2D().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        fluidMudDat.xferg_name = base_dir
                                + configuration.getFluidMudFlowModel2D().getResultFile().getFileName();
                    }

                    fluidMudDat.NumberOfThreads = numberOfThreads;

                    FluidMudFlowModel2D fluidMudModel2D = new FluidMudFlowModel2D(feapp, fluidMudDat);
                    fluidMudModel2D.setStartTime(startTime);

                    double[] fluidMudErg = null;

                    // Startwerte initialisiert
                    if (configuration.getFluidMudFlowModel2D().getInitialCondition() != null) {
                        // Ist eine Startdatei vorhanden ?
                        if (configuration.getFluidMudFlowModel2D().getInitialCondition().getStartFromResult() != null) {
                            // Die Unterscheidung in Dateiformate fehlt noch ...
                            fluidMudDat.startWerteDatei = base_dir + configuration.getFluidMudFlowModel2D()
                                    .getInitialCondition().getStartFromResult().getFileName();
                            fluidMudDat.startSatz = configuration.getFluidMudFlowModel2D().getInitialCondition()
                                    .getStartFromResult().getTimeStepCounter();
                            try {
                                fluidMudErg = fluidMudModel2D.initialSolutionFromTicadErgFile(
                                        fluidMudDat.startWerteDatei, fluidMudDat.startSatz);
                            } catch (Exception e) {
                                System.out.println("\t !! Fluid Mud Flow StartFile:" + fluidMudDat.startWerteDatei
                                        + " can not be opened !!");
                                System.out.println(e);
                                System.exit(0);
                            }

                        } else
                        // Ist ein InitialFluidMudLevelsmodell vorhanden ?
                        if (configuration.getFluidMudFlowModel2D().getInitialCondition().getFluidMudLevel() != null) {
                            System.out.println("Lese InitialFluidMudLevelsModell!");
                            if (configuration.getFluidMudFlowModel2D().getInitialCondition().getFluidMudLevel()
                                    .getTriangleMesh() != null) {
                                if (configuration.getFluidMudFlowModel2D().getInitialCondition().getFluidMudLevel()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    fluidMudDat.fluidmudLevelFileType = SmileIO.MeshFileType.SystemDat;
                                    fluidMudDat.fluidmudlevel_name = base_dir + configuration.getFluidMudFlowModel2D()
                                            .getInitialCondition().getFluidMudLevel().getTriangleMesh().getFileName();
                                    fluidMudErg = fluidMudModel2D.initialHfromSysDat(fluidMudDat.fluidmudlevel_name,
                                            startTime);
                                }
                                if (configuration.getFluidMudFlowModel2D().getInitialCondition().getFluidMudLevel()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    fluidMudDat.fluidmudLevelFileType = SmileIO.MeshFileType.JanetBin;
                                    fluidMudDat.fluidmudlevel_name = base_dir + configuration.getFluidMudFlowModel2D()
                                            .getInitialCondition().getFluidMudLevel().getTriangleMesh().getFileName();
                                    fluidMudErg = fluidMudModel2D.initialHfromJanetBin(fluidMudDat.fluidmudlevel_name,
                                            startTime);
                                }
                            }
                            // Ist ein konstanter InitialFluidMudLevel vorhanden ?
                            else if (configuration.getFluidMudFlowModel2D().getInitialCondition().getFluidMudLevel()
                                    .getConstant() != null) {
                                System.out.println("Lese konstanten InitialFluidMudLevel!");
                                double Cwaterlevel = configuration.getFluidMudFlowModel2D().getInitialCondition()
                                        .getFluidMudLevel().getConstant();
                                fluidMudErg = fluidMudModel2D.constantInitialSolution(Cwaterlevel);
                            }
                        }

                        // Ist ein InitialDensityLevelModell vorhanden ?
                        if (configuration.getFluidMudFlowModel2D().getDensity() != null) {
                            System.out.println("Lese InitialDensityLevelModell!");
                            if (configuration.getFluidMudFlowModel2D().getDensity().getTriangleMesh() != null) {
                                if (configuration.getFluidMudFlowModel2D().getDensity().getTriangleMesh()
                                        .getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    fluidMudDat.densityFileType = SmileIO.MeshFileType.SystemDat;
                                    fluidMudDat.density_name = base_dir + configuration.getFluidMudFlowModel2D()
                                            .getDensity().getTriangleMesh().getFileName();
                                    // ToDo fluidMudErg =
                                    // fluidMudModel2D.initialHfromSysDat(fluidMudDat.fluidmudlevel_name,startTime);
                                }
                                if (configuration.getFluidMudFlowModel2D().getDensity().getTriangleMesh()
                                        .getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    fluidMudDat.densityFileType = SmileIO.MeshFileType.JanetBin;
                                    fluidMudDat.density_name = base_dir + configuration.getFluidMudFlowModel2D()
                                            .getDensity().getTriangleMesh().getFileName();
                                    // ToDo fluidMudErg =
                                    // fluidMudModel2D.initialHfromJanetBin(fluidMudDat.fluidmudlevel_name,startTime);
                                }
                            }
                            // Ist ein konstanter Density vorhanden ?
                            else if (configuration.getFluidMudFlowModel2D().getDensity().getConstant() != null) {
                                System.out.println("Lese konstanten Density!");
                                fluidMudDat.constantDensity = configuration.getFluidMudFlowModel2D().getDensity()
                                        .getConstant();
                            }
                        }

                        // Ist ein ViscosityLevelModell vorhanden ?
                        if (configuration.getFluidMudFlowModel2D().getViscosity() != null) {
                            System.out.println("Lese ViscosityLevelModell!");
                            if (configuration.getFluidMudFlowModel2D().getViscosity().getTriangleMesh() != null) {
                                if (configuration.getFluidMudFlowModel2D().getViscosity().getTriangleMesh()
                                        .getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    fluidMudDat.fluidmudLevelFileType = SmileIO.MeshFileType.SystemDat;
                                    fluidMudDat.fluidmudlevel_name = base_dir + configuration.getFluidMudFlowModel2D()
                                            .getViscosity().getTriangleMesh().getFileName();
                                    // ToDo fluidMudErg =
                                    // fluidMudModel2D.initialHfromSysDat(fluidMudDat.fluidmudlevel_name,startTime);
                                }
                                if (configuration.getFluidMudFlowModel2D().getViscosity().getTriangleMesh()
                                        .getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    fluidMudDat.fluidmudLevelFileType = SmileIO.MeshFileType.JanetBin;
                                    fluidMudDat.fluidmudlevel_name = base_dir + configuration.getFluidMudFlowModel2D()
                                            .getViscosity().getTriangleMesh().getFileName();
                                    // ToDo fluidMudErg =
                                    // fluidMudModel2D.initialHfromJanetBin(fluidMudDat.fluidmudlevel_name,startTime);
                                }
                            }
                            // Ist ein konstanter Viscosity vorhanden ?
                            else if (configuration.getFluidMudFlowModel2D().getViscosity().getConstant() != null) {
                                System.out.println("Lese konstanten Viscosity!");
                                fluidMudDat.constantViscosity = configuration.getFluidMudFlowModel2D().getViscosity()
                                        .getConstant();
                            }
                        }

                    } else {
                        // Interpolation aus Randwerten
                        fluidMudErg = fluidMudModel2D.initialSolution(startTime);
                    }

                    fluidMudModel2D.setBoundaryConditions();

                    fluidMudModel2D.setMaxTimeStep(
                            Math.min(Math.abs(controlParameter.getSimulationTime().getResultTimeStep()) / 100., 0.1));

                    TimeDependentFEModels.add(new TimeDependentFEModel(fluidMudModel2D, fluidMudErg));
                }

                // AdvectionDispersionModel2D
                // --------------
                if (configuration.getAdvectionDispersionModel2D() != null) {
                    System.out.println("*** AdvectionDispersionModel2D ***");
                    AdvectionDispersionDat admdat = new AdvectionDispersionDat();

                    if (configuration.getAdvectionDispersionModel2D().getBoundaryCondition()
                            .getFileType() == TAdvectionDispersion2DBoundaryFileType.RNDWERTE_BAW) {
                        admdat.adrndwerte_name = base_dir
                                + configuration.getAdvectionDispersionModel2D().getBoundaryCondition().getFileName();
                        admdat.rndwerteReader = new RndwerteBAWReader(admdat.adrndwerte_name, feapp);
                    }

                    if (configuration.getAdvectionDispersionModel2D().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        admdat.xferg_name = base_dir
                                + configuration.getAdvectionDispersionModel2D().getResultFile().getFileName();
                    }

                    admdat.NumberOfThreads = numberOfThreads;

                    try {
                        admdat.dispersionCoefficient = configuration.getAdvectionDispersionModel2D()
                                .getDispersionCoefficient();
                    } catch (Exception ex) {
                        System.out.println("no extra DispersionCoefficient");
                    }

                    try {
                        admdat.degradationRate = configuration.getAdvectionDispersionModel2D().getDegradationRate();
                    } catch (Exception ex) {
                        System.out.println("no DegradationRate");
                    }

                    AdvectionDispersionModel2D admodel = new AdvectionDispersionModel2D(feapp, admdat);
                    admodel.setStartTime(startTime);
                    admodel.setMaxTimeStep(0.1);

                    double[] aderg = null;

                    // Startwerte fuer Konzentrationen initialisiert
                    if (configuration.getAdvectionDispersionModel2D().getInitialCondition() != null) {
                        // Ist eine Startdatei vorhanden ?
                        if (configuration.getAdvectionDispersionModel2D().getInitialCondition()
                                .getStartFromResult() != null) {
                            // Die Unterscheidung in Dateiformate fehlt noch ...
                            try {
                                aderg = admodel.initialSolutionFromTicadErgFile(
                                        base_dir + configuration.getAdvectionDispersionModel2D().getInitialCondition()
                                                .getStartFromResult().getFileName(),
                                        configuration.getAdvectionDispersionModel2D().getInitialCondition()
                                                .getStartFromResult().getTimeStepCounter());
                            } catch (Exception e) {
                                System.out.println("\t !! StartDatei laesst sich nicht einlesen !!");
                                e.printStackTrace();
                                System.exit(0);
                            }
                        } else
                        // Ist ein Initialkonzentrationsmodell vorhanden ?
                        if (configuration.getAdvectionDispersionModel2D().getInitialCondition()
                                .getConcentration() != null) {
                            if (configuration.getAdvectionDispersionModel2D().getInitialCondition().getConcentration()
                                    .getTriangleMesh() != null) {
                                if (configuration.getAdvectionDispersionModel2D().getInitialCondition()
                                        .getConcentration().getTriangleMesh()
                                        .getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    admdat.concentrationFileType = SmileIO.MeshFileType.SystemDat;
                                    admdat.concentration_name = base_dir + configuration.getAdvectionDispersionModel2D()
                                            .getInitialCondition().getConcentration().getTriangleMesh().getFileName();
                                    aderg = admodel.initialConcentrationFromSysDat(admdat.concentration_name,
                                            startTime);
                                }
                                if (configuration.getAdvectionDispersionModel2D().getInitialCondition()
                                        .getConcentration().getTriangleMesh()
                                        .getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    admdat.concentrationFileType = SmileIO.MeshFileType.JanetBin;
                                    admdat.concentration_name = base_dir + configuration.getAdvectionDispersionModel2D()
                                            .getInitialCondition().getConcentration().getTriangleMesh().getFileName();
                                    aderg = admodel.initialConcentrationFromJanetBin(admdat.concentration_name,
                                            startTime);
                                }
                            }
                            // Ist eine konstante Konzentration vorgegeben ?
                            else if (configuration.getAdvectionDispersionModel2D().getInitialCondition()
                                    .getConcentration().getConstant() != null) {
                                double CConc = configuration.getAdvectionDispersionModel2D().getInitialCondition()
                                        .getConcentration().getConstant();
                                aderg = admodel.constantInitialSolution(CConc);
                            }
                        }
                    } else {
                        // Interpolation aus Randwerten
                        aderg = admodel.initialSolution(startTime);
                    }

                    TimeDependentFEModels.add(new TimeDependentFEModel(admodel, aderg));
                }

                // HeatTransport 2D
                // --------------
                if (configuration.getHeatTransportModel2D() != null) {
                    System.out.println("*** HeatTransportModel2D ***");
                    HeatTransportDat heatTdat = new HeatTransportDat();

                    try {
                        heatTdat.airTemperature = configuration.getHeatTransportModel2D().getAirTemperature();
                    } catch (Exception ex) {
                    }

                    if (configuration.getHeatTransportModel2D().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        heatTdat.xferg_name = base_dir
                                + configuration.getHeatTransportModel2D().getResultFile().getFileName();
                    }

                    if (configuration.getHeatTransportModel2D().getBoundaryCondition()
                            .getFileType() == THeat2DBoundaryFileType.RNDWERTE_BAW) {
                        heatTdat.temperaturerndwerte_name = base_dir
                                + configuration.getHeatTransportModel2D().getBoundaryCondition().getFileName();
                        heatTdat.rndwerteReader = new RndwerteBAWReader(heatTdat.temperaturerndwerte_name, feapp);
                    }

                    heatTdat.NumberOfThreads = numberOfThreads;

                    HeatTransportModel2D heatTransportModel2D = new HeatTransportModel2D(feapp, heatTdat);
                    heatTransportModel2D.setStartTime(startTime);
                    heatTransportModel2D.setMaxTimeStep(0.1);

                    double[] heatTransportErg = null;

                    // Startwerte fuer Temperaturen initialisiert
                    if (configuration.getHeatTransportModel2D().getInitialCondition() != null) {
                        // Ist eine Startdatei vorhanden ?
                        if (configuration.getHeatTransportModel2D().getInitialCondition()
                                .getStartFromResult() != null) {
                            // Die Unterscheidung in Dateiformate fehlt noch ...
                            try {
                                heatTransportErg = heatTransportModel2D.initialSolutionFromTicadErgFile(
                                        base_dir + configuration.getHeatTransportModel2D().getInitialCondition()
                                                .getStartFromResult().getFileName(),
                                        configuration.getHeatTransportModel2D().getInitialCondition()
                                                .getStartFromResult().getTimeStepCounter());
                            } catch (Exception e) {
                                System.out.println("\t !! StartDatei laesst sich nicht einlesen !!");
                                System.exit(0);
                            }

                        } else
                        // Ist ein Initialtemperaturmodell vorhanden ?
                        if (configuration.getHeatTransportModel2D().getInitialCondition().getTemperature() != null) {
                            if (configuration.getHeatTransportModel2D().getInitialCondition().getTemperature()
                                    .getTriangleMesh() != null) {
                                if (configuration.getHeatTransportModel2D().getInitialCondition().getTemperature()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    heatTdat.temperatureFileType = SmileIO.MeshFileType.SystemDat;
                                    heatTdat.temperature_name = base_dir + configuration.getHeatTransportModel2D()
                                            .getInitialCondition().getTemperature().getTriangleMesh().getFileName();
                                    heatTransportErg = heatTransportModel2D
                                            .initialTemperatureFromSysDat(heatTdat.temperature_name, startTime);
                                }
                                if (configuration.getHeatTransportModel2D().getInitialCondition().getTemperature()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    heatTdat.temperatureFileType = SmileIO.MeshFileType.JanetBin;
                                    heatTdat.temperature_name = base_dir + configuration.getHeatTransportModel2D()
                                            .getInitialCondition().getTemperature().getTriangleMesh().getFileName();

                                    heatTransportErg = heatTransportModel2D
                                            .initialTemperatureFromJanetBin(heatTdat.temperature_name, startTime);
                                }
                            }
                            // Ist eine konstante Konzentration vorgegeben ?
                            else if (configuration.getHeatTransportModel2D().getInitialCondition().getTemperature()
                                    .getConstant() != null) {
                                double CTemp = configuration.getHeatTransportModel2D().getInitialCondition()
                                        .getTemperature().getConstant();
                                heatTransportErg = heatTransportModel2D.constantInitialSolution(CTemp);
                            }
                        }
                    } else {
                        // Interpolation aus Randwerten
                        heatTransportErg = heatTransportModel2D.initialSolution(startTime);
                    }

                    TimeDependentFEModels.add(new TimeDependentFEModel(heatTransportModel2D, heatTransportErg));
                }

                // GroundWaterModel2D
                // --------------
                if (configuration.getGroundWaterModel2D() != null) {
                    System.out.println("*** GroundWaterModel2D ***");
                    GroundWaterDat groundwaterdat = new GroundWaterDat();

                    if (configuration.getGroundWaterModel2D().getBoundaryCondition()
                            .getFileType() == TGroundWater2DBoundaryFileType.RNDWERTE_BAW) {
                        groundwaterdat.rndwerte_name = base_dir
                                + configuration.getGroundWaterModel2D().getBoundaryCondition().getFileName();
                        groundwaterdat.rndwerteReader = new RndwerteBAWReader(groundwaterdat.rndwerte_name, feapp);
                    }

                    if (configuration.getGroundWaterModel2D().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        groundwaterdat.xferg_name = base_dir
                                + configuration.getGroundWaterModel2D().getResultFile().getFileName();
                    }

                    // Sind Permeabilitaeten definiert ?
                    if (configuration.getGroundWaterModel2D().getPermeability() != null) {
                        // Ist ein Modell fuer Permeabilitaeten gegeben ?
                        if (configuration.getGroundWaterModel2D().getPermeability().getTriangleMesh() != null) {
                            if (configuration.getGroundWaterModel2D().getPermeability().getTriangleMesh()
                                    .getFileType() == TMeshFileType.SYS_DAT_FILE)
                                groundwaterdat.permeability_FileType = SmileIO.MeshFileType.SystemDat;
                            if (configuration.getGroundWaterModel2D().getPermeability().getTriangleMesh()
                                    .getFileType() == TMeshFileType.JANET_BINARY_FILE)
                                groundwaterdat.permeability_FileType = SmileIO.MeshFileType.JanetBin;
                            groundwaterdat.permeability_name = base_dir + configuration.getGroundWaterModel2D()
                                    .getPermeability().getTriangleMesh().getFileName();
                        }
                        // Ist eine konstante Permeabilitaetsverteilung gegeben ?
                        else if (configuration.getGroundWaterModel2D().getPermeability().getConstant() != null) {
                            groundwaterdat.standardPermeability = configuration.getGroundWaterModel2D()
                                    .getPermeability().getConstant();
                        }
                    } else {
                        System.out.println("Keine Werte fuer Permeabiliaet gegeben (Standardwert = "
                                + groundwaterdat.standardPermeability + ")!");
                    }

                    // Sind Impermeabilitaetstiefen definiert ?
                    if (configuration.getGroundWaterModel2D().getImpermeableLayerDepth() != null) {
                        // Ist ein Modell fuer Permeabilitaeten gegeben ?
                        if (configuration.getGroundWaterModel2D().getImpermeableLayerDepth()
                                .getTriangleMesh() != null) {
                            if (configuration.getGroundWaterModel2D().getImpermeableLayerDepth().getTriangleMesh()
                                    .getFileType() == TMeshFileType.SYS_DAT_FILE)
                                groundwaterdat.impermeability_FileType = SmileIO.MeshFileType.SystemDat;
                            if (configuration.getGroundWaterModel2D().getImpermeableLayerDepth().getTriangleMesh()
                                    .getFileType() == TMeshFileType.JANET_BINARY_FILE)
                                groundwaterdat.impermeability_FileType = SmileIO.MeshFileType.JanetBin;
                            groundwaterdat.impermeability_name = base_dir + configuration.getGroundWaterModel2D()
                                    .getImpermeableLayerDepth().getTriangleMesh().getFileName();
                        }
                        // Ist eine konstante Impermeabilitaetsverteilung gegeben ?
                        else if (configuration.getGroundWaterModel2D().getImpermeableLayerDepth()
                                .getConstant() != null) {
                            groundwaterdat.standardImpermeability = configuration.getGroundWaterModel2D()
                                    .getImpermeableLayerDepth().getConstant();
                        }
                    } else {
                        System.out.println("Keine Werte fuer Impermeabiliaetstiefen gegeben (Standardwert = 10m)!");
                    }

                    // Sind Impermeabilitaetstiefen definiert ?
                    if (configuration.getGroundWaterModel2D().getUpperImpermeableLayerThickness() != null) {
                        // Ist ein Modell fuer Permeabilitaeten gegeben ?
                        if (configuration.getGroundWaterModel2D().getUpperImpermeableLayerThickness()
                                .getTriangleMesh() != null) {
                            if (configuration.getGroundWaterModel2D().getUpperImpermeableLayerThickness()
                                    .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE)
                                groundwaterdat.upperImpermeabilityThickness_FileType = SmileIO.MeshFileType.SystemDat;
                            if (configuration.getGroundWaterModel2D().getUpperImpermeableLayerThickness()
                                    .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE)
                                groundwaterdat.upperImpermeabilityThickness_FileType = SmileIO.MeshFileType.JanetBin;
                            groundwaterdat.upperImpermeabilityThickness_name = base_dir
                                    + configuration.getGroundWaterModel2D().getUpperImpermeableLayerThickness()
                                            .getTriangleMesh().getFileName();
                        }
                        // Ist eine konstante Impermeabilitaetsverteilung gegeben ?
                        else if (configuration.getGroundWaterModel2D().getUpperImpermeableLayerThickness()
                                .getConstant() != null) {
                            groundwaterdat.standartUpperImpermeabilityThickness = configuration.getGroundWaterModel2D()
                                    .getUpperImpermeableLayerThickness().getConstant();
                        }
                    } else {
                        System.out.println("Keine Werte fuer Deckschichtdicke gegeben (Standardwert = 0m)!");
                    }

                    // ist OberflaechenWasserStand gegeben? aber nur wenn kein oberflaechenmodell
                    // aktiviert ist
                    if ((configuration.getGroundWaterModel2D().getSurfaceWater() != null)
                            && (configuration.getCurrentModel2D() == null)) {
                        if (configuration.getGroundWaterModel2D().getSurfaceWater().getTriangleMesh() != null) {
                            if (configuration.getGroundWaterModel2D().getSurfaceWater().getTriangleMesh()
                                    .getFileType() == TMeshFileType.SYS_DAT_FILE)
                                groundwaterdat.surfaceWater_FileType = SmileIO.MeshFileType.SystemDat;
                            if (configuration.getGroundWaterModel2D().getSurfaceWater().getTriangleMesh()
                                    .getFileType() == TMeshFileType.JANET_BINARY_FILE)
                                groundwaterdat.surfaceWater_FileType = SmileIO.MeshFileType.JanetBin;
                            groundwaterdat.surfaceWater_name = base_dir + configuration.getGroundWaterModel2D()
                                    .getSurfaceWater().getTriangleMesh().getFileName();
                        }
                    } else {

                    }

                    // Groundwater-Modell erzeugen
                    groundwaterdat.NumberOfThreads = numberOfThreads;

                    GroundWaterModel2D groundwatermodel = new GroundWaterModel2D(feapp, groundwaterdat);
                    groundwatermodel.setStartTime(startTime);
                    groundwatermodel.setMaxTimeStep(0.1);

                    double[] groundwatererg = null;

                    // Sollen Startwerte initialisiert werden ?
                    if (configuration.getGroundWaterModel2D().getInitialCondition() != null) {
                        // Ist eine Startdatei vorhanden ?
                        if (configuration.getGroundWaterModel2D().getInitialCondition().getStartFromResult() != null) {
                            // Die Unterscheidung in Dateiformate fehlt noch ...
                            groundwaterdat.startWerteDatei = base_dir + configuration.getGroundWaterModel2D()
                                    .getInitialCondition().getStartFromResult().getFileName();
                            groundwaterdat.startSatz = configuration.getGroundWaterModel2D().getInitialCondition()
                                    .getStartFromResult().getTimeStepCounter();
                            groundwatererg = groundwatermodel.initialSolutionFromTicadErgFile(
                                    groundwaterdat.startWerteDatei, groundwaterdat.startSatz);
                        } else
                        // Ist ein Initialwasserstandsmodell vorhanden ?
                        if (configuration.getGroundWaterModel2D().getInitialCondition().getWaterLevel() != null) {
                            if (configuration.getGroundWaterModel2D().getInitialCondition().getWaterLevel()
                                    .getTriangleMesh() != null) {
                                if (configuration.getGroundWaterModel2D().getInitialCondition().getWaterLevel()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    groundwaterdat.waterLevelFileType = SmileIO.MeshFileType.SystemDat;
                                    groundwaterdat.waterlevel_name = base_dir + configuration.getGroundWaterModel2D()
                                            .getInitialCondition().getWaterLevel().getTriangleMesh().getFileName();
                                    groundwatererg = groundwatermodel
                                            .initialHfromASCIIFile(groundwaterdat.waterlevel_name, startTime);
                                }
                                if (configuration.getGroundWaterModel2D().getInitialCondition().getWaterLevel()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    groundwaterdat.waterLevelFileType = SmileIO.MeshFileType.JanetBin;
                                    groundwaterdat.waterlevel_name = base_dir + configuration.getGroundWaterModel2D()
                                            .getInitialCondition().getWaterLevel().getTriangleMesh().getFileName();
                                    groundwatererg = groundwatermodel
                                            .initialHfromJanetBin(groundwaterdat.waterlevel_name, startTime);
                                }
                            }

                            // Ist ein konstanter Initialwasserstand vorhanden ?
                            else if (configuration.getGroundWaterModel2D().getInitialCondition().getWaterLevel()
                                    .getConstant() != null) {
                                System.out.println("Lese konstanten Wasserstand!");
                                // Code fehlt noch
                                final double Cwaterlevel = configuration.getGroundWaterModel2D().getInitialCondition()
                                        .getWaterLevel().getConstant();
                                groundwatererg = groundwatermodel.ConstantInitialSolution(Cwaterlevel);

                            }
                        } else {
                            groundwatererg = groundwatermodel
                                    .initialSolution(controlParameter.getSimulationTime().getStartTime());
                            System.out.println("Keine Startwerte gegeben !");
                        }

                    } else {
                        groundwatererg = groundwatermodel
                                .initialSolution(controlParameter.getSimulationTime().getStartTime());
                    }

                    TimeDependentFEModels.add(new TimeDependentFEModel(groundwatermodel, groundwatererg));
                }

                // SpartinaAlternifloraModel2D
                // --------------
                if (configuration.getSpartinaAlternifloraModel2D() != null) {

                    SpartinaAlternifloraModel2DDat heatTdat = new SpartinaAlternifloraModel2DDat();

                    if (configuration.getSpartinaAlternifloraModel2D().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        heatTdat.xferg_name = base_dir
                                + configuration.getSpartinaAlternifloraModel2D().getResultFile().getFileName();
                    }

                    heatTdat.numberOfThreads = numberOfThreads;

                    // Startwerte fuer Dichte initialisiert
                    if (configuration.getSpartinaAlternifloraModel2D().getInitialCondition() != null) {
                        // Ist eine Startdatei vorhanden ?
                        if (configuration.getSpartinaAlternifloraModel2D().getInitialCondition()
                                .getStartFromResult() != null) {
                            heatTdat.startWerteDatei = base_dir + configuration.getHeatTransportModel2D()
                                    .getInitialCondition().getStartFromResult().getFileName();
                            heatTdat.startSatz = configuration.getHeatTransportModel2D().getInitialCondition()
                                    .getStartFromResult().getTimeStepCounter();
                        } else
                        // Ist ein InitialDichtemodell vorhanden ?
                        if (configuration.getSpartinaAlternifloraModel2D().getInitialCondition().getDensity() != null) {
                            if (configuration.getSpartinaAlternifloraModel2D().getInitialCondition().getDensity()
                                    .getTriangleMesh() != null) {
                                if (configuration.getSpartinaAlternifloraModel2D().getInitialCondition().getDensity()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    heatTdat.densityFileType = SmileIO.MeshFileType.SystemDat;
                                    heatTdat.desity_name = base_dir + configuration.getSpartinaAlternifloraModel2D()
                                            .getInitialCondition().getDensity().getTriangleMesh().getFileName();
                                }
                                if (configuration.getSpartinaAlternifloraModel2D().getInitialCondition().getDensity()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    heatTdat.densityFileType = SmileIO.MeshFileType.JanetBin;
                                    heatTdat.desity_name = base_dir + configuration.getSpartinaAlternifloraModel2D()
                                            .getInitialCondition().getDensity().getTriangleMesh().getFileName();
                                }
                            }

                            // Ist eine konstante Dichte vorgegeben ?
                            else if (configuration.getSpartinaAlternifloraModel2D().getInitialCondition().getDensity()
                                    .getConstant() != null) {
                                heatTdat.startDensity = configuration.getSpartinaAlternifloraModel2D()
                                        .getInitialCondition().getDensity().getConstant();
                            }
                        }
                    }

                    SpartinaAlternifloraModel2D heatTransportModel2D = new SpartinaAlternifloraModel2D(feapp, heatTdat);
                    heatTransportModel2D.setStartTime(startTime);

                    TimeDependentFEModels.add(new TimeDependentFEModel(heatTransportModel2D, null));
                }

                // OxygenTransportModel2D
                // --------------
                if (configuration.getOxygenTransportModel2D() != null) {
                    System.out.println("*** OxygenTransportModel2D ***");
                    OxygenTransportDat oxygendat = new OxygenTransportDat();

                    if (configuration.getOxygenTransportModel2D().getWaterTemperature() != null)
                        oxygendat.waterTemperature = configuration.getOxygenTransportModel2D().getWaterTemperature();

                    if (configuration.getOxygenTransportModel2D().getBoundaryCondition()
                            .getFileType() == TQxygen2DBoundaryFileType.RNDWERTE_BAW) {
                        oxygendat.oxygenrndwerte_name = base_dir
                                + configuration.getOxygenTransportModel2D().getBoundaryCondition().getFileName();
                        oxygendat.rndwerteReader = new RndwerteBAWReader(oxygendat.oxygenrndwerte_name, feapp);
                    }

                    if (configuration.getOxygenTransportModel2D().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        oxygendat.xferg_name = base_dir
                                + configuration.getOxygenTransportModel2D().getResultFile().getFileName();
                    }

                    // NitratTransport-Modell erzeugen
                    oxygendat.NumberOfThreads = numberOfThreads;

                    OxygenTransportModel2D oxygenTransportModel2d = new OxygenTransportModel2D(feapp, oxygendat);
                    oxygenTransportModel2d.setStartTime(startTime);
                    oxygenTransportModel2d.setMaxTimeStep(0.1);

                    double[] oxygenerg = null;

                    // Sollen Startwerte initialisiert werden ?
                    if (configuration.getOxygenTransportModel2D().getInitialCondition() != null) {
                        // Ist eine Startdatei vorhanden ?
                        if (configuration.getOxygenTransportModel2D().getInitialCondition()
                                .getStartFromResult() != null) {
                            // Die Unterscheidung in Dateiformate fehlt noch ...
                            oxygendat.initalValuesErgFile = base_dir + configuration.getOxygenTransportModel2D()
                                    .getInitialCondition().getStartFromResult().getFileName();
                            oxygendat.startCounter = configuration.getOxygenTransportModel2D().getInitialCondition()
                                    .getStartFromResult().getTimeStepCounter();
                        } else
                        // Ist ein Initialkonzentrationsmodell vorhanden ?
                        if (configuration.getOxygenTransportModel2D().getInitialCondition()
                                .getConcentration() != null) {
                            if (configuration.getOxygenTransportModel2D().getInitialCondition().getConcentration()
                                    .getTriangleMesh() != null) {
                                if (configuration.getOxygenTransportModel2D().getInitialCondition().getConcentration()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    oxygendat.concentrationFileType = SmileIO.MeshFileType.SystemDat;
                                    oxygendat.concentration_name = base_dir + configuration.getOxygenTransportModel2D()
                                            .getInitialCondition().getConcentration().getTriangleMesh().getFileName();
                                    // nitraterg =
                                    // nitratmodel.initialConcentrationFromSysDat(nitratdat.concentration_name,startTime);
                                }
                                if (configuration.getOxygenTransportModel2D().getInitialCondition().getConcentration()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    oxygendat.concentrationFileType = SmileIO.MeshFileType.JanetBin;
                                    oxygendat.concentration_name = base_dir + configuration.getOxygenTransportModel2D()
                                            .getInitialCondition().getConcentration().getTriangleMesh().getFileName();
                                    // nitraterg =
                                    // nitratmodel.initialConcentrationFromJanetBin(nitratdat.concentration_name,startTime);
                                }
                            }

                            // Ist eine konstante Konzentration vorhanden ?
                            else if (configuration.getOxygenTransportModel2D().getInitialCondition().getConcentration()
                                    .getConstant() != null) {
                                double CConc = configuration.getOxygenTransportModel2D().getInitialCondition()
                                        .getConcentration().getConstant();

                                oxygenerg = oxygenTransportModel2d.constantInitialSolution(CConc);
                            }
                        } else {
                            // Interpolation aus den spezifizierten Randbedingungen
                            oxygenerg = oxygenTransportModel2d
                                    .initialSolution(controlParameter.getSimulationTime().getStartTime());
                            System.out.println("Keine Startwerte gegeben !");
                        }

                    } else {
                        // Interpolation aus den spezifizierten Randbedingungen
                        oxygenerg = oxygenTransportModel2d
                                .initialSolution(controlParameter.getSimulationTime().getStartTime());

                    }

                    TimeDependentFEModels.add(new TimeDependentFEModel(oxygenTransportModel2d, oxygenerg));
                }

                // NitratTransportModel2D
                // --------------
                if (configuration.getNitratTransportModel2D() != null) {
                    System.out.println("Input NitratTransportModel2D initialization");
                    NitrogenDat nitratdat = new NitrogenDat();

                    if (configuration.getNitratTransportModel2D().getBoundaryCondition()
                            .getFileType() == TEco2DBoundaryFileType.ECO_BOUNDARY_DAT) {
                        nitratdat.rndwerte_name = base_dir
                                + configuration.getNitratTransportModel2D().getBoundaryCondition().getFileName();
                    }

                    if (configuration.getNitratTransportModel2D().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        nitratdat.xferg_name = base_dir
                                + configuration.getNitratTransportModel2D().getResultFile().getFileName();
                    }

                    // NitratTransport-Modell erzeugen
                    nitratdat.NumberOfThreads = numberOfThreads;

                    NitrogenModel2D nitratmodel = new NitrogenModel2D(feapp, nitratdat);
                    nitratmodel.setStartTime(startTime);
                    nitratmodel.setMaxTimeStep(0.1);

                    double[] nitraterg = null;

                    // Sollen Startwerte initialisiert werden ?
                    if (configuration.getNitratTransportModel2D().getInitialCondition() != null) {
                        // Ist eine Startdatei vorhanden ?
                        if (configuration.getNitratTransportModel2D().getInitialCondition()
                                .getStartFromResult() != null) {
                            // Die Unterscheidung in Dateiformate fehlt noch ...
                            nitratdat.startWerteDatei = base_dir + configuration.getNitratTransportModel2D()
                                    .getInitialCondition().getStartFromResult().getFileName();
                            nitratdat.startSatz = configuration.getNitratTransportModel2D().getInitialCondition()
                                    .getStartFromResult().getTimeStepCounter();
                            // nitraterg =
                            // nitratmodel.initialSolutionFromTicadErgFile(nitratdat.startWerteDatei,
                            // nitratdat.startSatz);
                        } else
                        // Ist ein Initialkonzentrationsmodell vorhanden ?
                        if (configuration.getNitratTransportModel2D().getInitialCondition()
                                .getConcentration() != null) {
                            if (configuration.getNitratTransportModel2D().getInitialCondition().getConcentration()
                                    .getTriangleMesh() != null) {
                                if (configuration.getNitratTransportModel2D().getInitialCondition().getConcentration()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    nitratdat.concentrationFileType = SmileIO.MeshFileType.SystemDat;
                                    nitratdat.concentration_name = base_dir + configuration.getNitratTransportModel2D()
                                            .getInitialCondition().getConcentration().getTriangleMesh().getFileName();
                                    // nitraterg =
                                    // nitratmodel.initialConcentrationFromSysDat(nitratdat.concentration_name,startTime);
                                }
                                if (configuration.getNitratTransportModel2D().getInitialCondition().getConcentration()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    nitratdat.concentrationFileType = SmileIO.MeshFileType.JanetBin;
                                    nitratdat.concentration_name = base_dir + configuration.getNitratTransportModel2D()
                                            .getInitialCondition().getConcentration().getTriangleMesh().getFileName();
                                    // nitraterg =
                                    // nitratmodel.initialConcentrationFromJanetBin(nitratdat.concentration_name,startTime);
                                }
                            }
                            // Ist eine konstante Konzentration vorhanden ?
                            else if (configuration.getNitratTransportModel2D().getInitialCondition().getConcentration()
                                    .getConstant() != null) {
                                double CConc = configuration.getNitratTransportModel2D().getInitialCondition()
                                        .getConcentration().getConstant();

                                nitraterg = nitratmodel.constantInitialSolution(CConc);

                            }
                        } else {
                            // Interpolation aus den spezifizierten Randbedingungen
                            nitraterg = nitratmodel
                                    .initialSolution(controlParameter.getSimulationTime().getStartTime());
                            System.out.println("Keine Startwerte gegeben !");
                        }
                    } else {
                        // Interpolation aus den spezifizierten Randbedingungen
                        nitraterg = nitratmodel.initialSolution(controlParameter.getSimulationTime().getStartTime());

                    }

                    TimeDependentFEModels.add(new TimeDependentFEModel(nitratmodel, nitraterg));
                }

                // PhytoplanktonModel2D
                // --------------
                if (configuration.getPhytoplanktonTransportModel2D() != null) {
                    System.out.println("Input PhytoplanktonModel2D initialization");
                    PhytoplanktonDat phytodat = new PhytoplanktonDat();

                    if (configuration.getPhytoplanktonTransportModel2D().getBoundaryCondition()
                            .getFileType() == TEco2DBoundaryFileType.ECO_BOUNDARY_DAT) {
                        phytodat.phyto_rndwerteName = base_dir
                                + configuration.getPhytoplanktonTransportModel2D().getBoundaryCondition().getFileName();
                    }

                    if (configuration.getPhytoplanktonTransportModel2D().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        phytodat.xferg_name = base_dir
                                + configuration.getPhytoplanktonTransportModel2D().getResultFile().getFileName();
                    }

                    // Phytoplankton-Modell erzeugen
                    phytodat.numberOfThreads = numberOfThreads;

                    PhytoplanktonModel2D phytomodel = new PhytoplanktonModel2D(feapp, phytodat);
                    phytomodel.setStartTime(startTime);
                    phytomodel.setMaxTimeStep(0.1);

                    double[] phytoerg = null;

                    // Sollen Startwerte initialisiert werden ?
                    if (configuration.getPhytoplanktonTransportModel2D().getInitialCondition() != null) {
                        // Ist eine Startdatei vorhanden ?
                        if (configuration.getPhytoplanktonTransportModel2D().getInitialCondition()
                                .getStartFromResult() != null) {
                            System.out.println("Lese Startdatei!");
                            // Die Unterscheidung in Dateiformate fehlt noch ...
                            phytodat.startWerteDatei = base_dir + configuration.getPhytoplanktonTransportModel2D()
                                    .getInitialCondition().getStartFromResult().getFileName();
                            phytodat.startSatz = configuration.getPhytoplanktonTransportModel2D().getInitialCondition()
                                    .getStartFromResult().getTimeStepCounter();
                            // phytoerg =
                            // phytomodel.initialSolutionFromTicadErgFile(phytodat.startWerteDatei,
                            // phytodat.startSatz);
                        } else
                        // Ist ein Initialkonzentrationsmodell vorhanden ?
                        if (configuration.getPhytoplanktonTransportModel2D().getInitialCondition()
                                .getConcentration() != null) {
                            System.out.println("Lese Startkonzentrationsmodell!");
                            if (configuration.getPhytoplanktonTransportModel2D().getInitialCondition()
                                    .getConcentration().getTriangleMesh() != null) {
                                if (configuration.getPhytoplanktonTransportModel2D().getInitialCondition()
                                        .getConcentration().getTriangleMesh()
                                        .getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    phytodat.concentrationFileType = SmileIO.MeshFileType.SystemDat;
                                    phytodat.concentration_name = base_dir
                                            + configuration.getPhytoplanktonTransportModel2D().getInitialCondition()
                                                    .getConcentration().getTriangleMesh().getFileName();
                                    // phytoerg =
                                    // phytomodel.initialConcentrationFromSysDat(phytodat.concentration_name,startTime);
                                }
                                if (configuration.getPhytoplanktonTransportModel2D().getInitialCondition()
                                        .getConcentration().getTriangleMesh()
                                        .getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    phytodat.concentrationFileType = SmileIO.MeshFileType.JanetBin;
                                    phytodat.concentration_name = base_dir
                                            + configuration.getPhytoplanktonTransportModel2D().getInitialCondition()
                                                    .getConcentration().getTriangleMesh().getFileName();
                                    // phytoerg =
                                    // phytomodel.initialConcentrationFromJanetBin(phytodat.concentration_name,startTime);
                                }
                            }

                            // Ist eine konstante Konzentration vorhanden ?
                            else if (configuration.getPhytoplanktonTransportModel2D().getInitialCondition()
                                    .getConcentration().getConstant() != null) {
                                System.out.println("Lese konstante Phytoplanktonkonzentration!");
                                double CConc = configuration.getPhytoplanktonTransportModel2D().getInitialCondition()
                                        .getConcentration().getConstant();

                                phytoerg = phytomodel.constantInitialSolution(CConc);

                            }
                        } else {
                            // Interpolation aus den spezifizierten Randbedingungen
                            phytoerg = phytomodel.initialSolution(controlParameter.getSimulationTime().getStartTime());
                            System.out.println("Keine Startwerte gegeben !");
                        }
                    } else {
                        // Interpolation aus den spezifizierten Randbedingungen
                        phytoerg = phytomodel.initialSolution(controlParameter.getSimulationTime().getStartTime());

                    }

                    TimeDependentFEModels.add(new TimeDependentFEModel(phytomodel, phytoerg));
                }

                // ZooplanktonModel2D
                // --------------
                if (configuration.getZooplanktonTransportModel2D() != null) {
                    System.out.println("Input ZooplanktonModel2D initialization");
                    ZooplanktonDat zoodat = new ZooplanktonDat();

                    if (configuration.getZooplanktonTransportModel2D().getBoundaryCondition()
                            .getFileType() == TEco2DBoundaryFileType.ECO_BOUNDARY_DAT) {
                        zoodat.zoo_rndwerteName = base_dir
                                + configuration.getZooplanktonTransportModel2D().getBoundaryCondition().getFileName();
                    }

                    if (configuration.getZooplanktonTransportModel2D().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        zoodat.xferg_name = base_dir
                                + configuration.getZooplanktonTransportModel2D().getResultFile().getFileName();
                    }

                    // Zooplankton-Modell erzeugen
                    zoodat.numberOfThreads = numberOfThreads;

                    ZooplanktonModel2D zoomodel = new ZooplanktonModel2D(feapp, zoodat);
                    zoomodel.setStartTime(startTime);
                    zoomodel.setMaxTimeStep(0.1);

                    double[] zooerg = null;

                    // Sollen Startwerte initialisiert werden ?
                    if (configuration.getZooplanktonTransportModel2D().getInitialCondition() != null) {
                        // Ist eine Startdatei vorhanden ?
                        if (configuration.getZooplanktonTransportModel2D().getInitialCondition()
                                .getStartFromResult() != null) {
                            System.out.println("Lese Startdatei!");
                            // Die Unterscheidung in Dateiformate fehlt noch ...
                            zoodat.startWerteDatei = base_dir + configuration.getZooplanktonTransportModel2D()
                                    .getInitialCondition().getStartFromResult().getFileName();
                            zoodat.startSatz = configuration.getZooplanktonTransportModel2D().getInitialCondition()
                                    .getStartFromResult().getTimeStepCounter();
                            // Zooerg = Zoomodel.initialSolutionFromTicadErgFile(Zoodat.startWerteDatei,
                            // Zoodat.startSatz);
                        } else
                        // Ist ein Initialkonzentrationsmodell vorhanden ?
                        if (configuration.getZooplanktonTransportModel2D().getInitialCondition()
                                .getConcentration() != null) {
                            System.out.println("Lese Startkonzentrationsmodell!");
                            if (configuration.getZooplanktonTransportModel2D().getInitialCondition().getConcentration()
                                    .getTriangleMesh() != null) {
                                if (configuration.getZooplanktonTransportModel2D().getInitialCondition()
                                        .getConcentration().getTriangleMesh()
                                        .getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    zoodat.concentrationFileType = SmileIO.MeshFileType.SystemDat;
                                    zoodat.concentration_name = base_dir
                                            + configuration.getZooplanktonTransportModel2D().getInitialCondition()
                                                    .getConcentration().getTriangleMesh().getFileName();
                                    // Zooerg =
                                    // Zoomodel.initialConcentrationFromSysDat(Zoodat.concentration_name,startTime);
                                }
                                if (configuration.getZooplanktonTransportModel2D().getInitialCondition()
                                        .getConcentration().getTriangleMesh()
                                        .getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    zoodat.concentrationFileType = SmileIO.MeshFileType.JanetBin;
                                    zoodat.concentration_name = base_dir
                                            + configuration.getZooplanktonTransportModel2D().getInitialCondition()
                                                    .getConcentration().getTriangleMesh().getFileName();
                                    // Zooerg =
                                    // Zoomodel.initialConcentrationFromJanetBin(Zoodat.concentration_name,startTime);
                                }
                            }

                            // Ist eine konstante Konzentration vorhanden ?
                            else if (configuration.getZooplanktonTransportModel2D().getInitialCondition()
                                    .getConcentration().getConstant() != null) {
                                System.out.println("Lese konstante Zooplanktonkonzentration!");
                                double CConc = configuration.getZooplanktonTransportModel2D().getInitialCondition()
                                        .getConcentration().getConstant();

                                zooerg = zoomodel.constantInitialSolution(CConc);

                            }
                        } else {
                            // Interpolation aus den spezifizierten Randbedingungen
                            zooerg = zoomodel.initialSolution(controlParameter.getSimulationTime().getStartTime());
                            System.out.println("Keine Startwerte gegeben !");
                        }

                    } else {
                        // Interpolation aus den spezifizierten Randbedingungen
                        zooerg = zoomodel.initialSolution(controlParameter.getSimulationTime().getStartTime());

                    }

                    TimeDependentFEModels.add(new TimeDependentFEModel(zoomodel, zooerg));
                }

                // DetritusModel2D
                // --------------
                if (configuration.getDetritusTransportModel2D() != null) {
                    System.out.println("Input DetritusModel2D initialization");
                    DetritusDat detritusdat = new DetritusDat();

                    if (configuration.getDetritusTransportModel2D().getBoundaryCondition()
                            .getFileType() == TEco2DBoundaryFileType.ECO_BOUNDARY_DAT) {
                        detritusdat.detrit_rndwerteName = base_dir
                                + configuration.getDetritusTransportModel2D().getBoundaryCondition().getFileName();
                    }

                    if (configuration.getDetritusTransportModel2D().getResultFile()
                            .getFileType() == TResultFileType.TICAD_SYS_ERG) {
                        detritusdat.xferg_name = base_dir
                                + configuration.getDetritusTransportModel2D().getResultFile().getFileName();
                    }

                    // Detritus-Modell erzeugen
                    detritusdat.numberOfThreads = numberOfThreads;

                    DetritusModel2D detritusmodel = new DetritusModel2D(feapp, detritusdat);
                    detritusmodel.setStartTime(startTime);
                    detritusmodel.setMaxTimeStep(0.1);

                    double[] detrituserg = null;

                    // Sollen Startwerte initialisiert werden ?
                    if (configuration.getDetritusTransportModel2D().getInitialCondition() != null) {
                        // Ist eine Startdatei vorhanden ?
                        if (configuration.getDetritusTransportModel2D().getInitialCondition()
                                .getStartFromResult() != null) {
                            // Die Unterscheidung in Dateiformate fehlt noch ...
                            detritusdat.startWerteDatei = base_dir + configuration.getDetritusTransportModel2D()
                                    .getInitialCondition().getStartFromResult().getFileName();
                            detritusdat.startSatz = configuration.getDetritusTransportModel2D().getInitialCondition()
                                    .getStartFromResult().getTimeStepCounter();
                            // Zooerg = Zoomodel.initialSolutionFromTicadErgFile(Zoodat.startWerteDatei,
                            // Zoodat.startSatz);
                        } else
                        // Ist ein Initialkonzentrationsmodell vorhanden ?
                        if (configuration.getDetritusTransportModel2D().getInitialCondition()
                                .getConcentration() != null) {
                            if (configuration.getDetritusTransportModel2D().getInitialCondition().getConcentration()
                                    .getTriangleMesh() != null) {
                                if (configuration.getDetritusTransportModel2D().getInitialCondition().getConcentration()
                                        .getTriangleMesh().getFileType() == TMeshFileType.SYS_DAT_FILE) {
                                    detritusdat.concentrationFileType = SmileIO.MeshFileType.SystemDat;
                                    detritusdat.concentration_name = base_dir
                                            + configuration.getDetritusTransportModel2D().getInitialCondition()
                                                    .getConcentration().getTriangleMesh().getFileName();
                                    // Zooerg =
                                    // Zoomodel.initialConcentrationFromSysDat(Zoodat.concentration_name,startTime);
                                }
                                if (configuration.getDetritusTransportModel2D().getInitialCondition().getConcentration()
                                        .getTriangleMesh().getFileType() == TMeshFileType.JANET_BINARY_FILE) {
                                    detritusdat.concentrationFileType = SmileIO.MeshFileType.JanetBin;
                                    detritusdat.concentration_name = base_dir
                                            + configuration.getDetritusTransportModel2D().getInitialCondition()
                                                    .getConcentration().getTriangleMesh().getFileName();
                                    // Zooerg =
                                    // Zoomodel.initialConcentrationFromJanetBin(Zoodat.concentration_name,startTime);
                                }
                            }

                            // Ist eine konstante Konzentration vorhanden ?
                            else if (configuration.getDetritusTransportModel2D().getInitialCondition()
                                    .getConcentration().getConstant() != null) {
                                System.out.println("Lese konstante Detrituskonzentration!");
                                double CConc = configuration.getDetritusTransportModel2D().getInitialCondition()
                                        .getConcentration().getConstant();
                                detrituserg = detritusmodel.constantInitialSolution(CConc);

                            }
                        } else {
                            // Interpolation aus den spezifizierten Randbedingungen
                            detrituserg = detritusmodel
                                    .initialSolution(controlParameter.getSimulationTime().getStartTime());
                            System.out.println("Keine Startwerte gegeben !");
                        }

                    } else {
                        // Interpolation aus den spezifizierten Randbedingungen
                        detrituserg = detritusmodel
                                .initialSolution(controlParameter.getSimulationTime().getStartTime());

                    }

                    TimeDependentFEModels.add(new TimeDependentFEModel(detritusmodel, detrituserg));
                }

                // ...Anfangswerte rauschreiben.....................................
                for (TimeDependentFEModel m : TimeDependentFEModels) {
                    if (m.model instanceof TimeDependentModel timeDependentModel) {
                        ((TimeDependentFEApproximation) m.model).setBoundaryConditions();
                        timeDependentModel.write_erg_xf();
                        if (m.model instanceof CurrentModel3D currentModel3D) {
                            currentModel3D.write_erg();
                        }
                    } else {
                        Object obj = m.getFEModel();
                        if (obj instanceof TicadModel ticadModel)
                            ticadModel.write_erg_xf(m.getResult(), startTime);
                    }
                }

                // start Simulation
                // -------------------------------------------------------------------
                // Zeitschleife
                // -------------------------------------------------------------------
                long time = System.currentTimeMillis();

                double dt = controlParameter.getSimulationTime().getResultTimeStep();
                boolean everyTimeStep = (dt <= 0.);

                System.out.println("Start Simulation");
                boolean resultIsNaN = false;

                // ... Schleife ueber die Zeit .......................................
                for (double t = startTime; t < controlParameter.getSimulationTime().getEndTime(); t += dt) {

                    double ta = t; // Anfangszeit
                    double te = t + dt; // Endzeit

                    // ...Schleife ueber einen Zeitschritt............................
                    do {
                        double ts = Double.MAX_VALUE;

                        // ...Zeitschritt auf Courant-Schitt setzen (minim. Zeitschritt)
                        for (TimeDependentFEModel m : TimeDependentFEModels) {
                            ts = Math.min(ts, m.getODESystem().getMaxTimeStep()); // Zeitschritt auf Courant-Schritt
                        }
                        if (everyTimeStep) {
                            te = ta + ts;
                            dt = ts;
                        }
                        // ...Groesse ueberpruefen....................................
                        if ((ta + ts) > te)
                            ts = te - ta;

                        // ...Berechnung durchfuehren.................................
                        for (TimeDependentFEModel m : TimeDependentFEModels) {
                            if (m.model instanceof TimeDependentModel timeDependentModel) {
                                timeDependentModel.timeStep(ts);
                            } else
                                m.setResult(methode.TimeStep(m.getODESystem(), ta, ts, m.getResult()));
                            resultIsNaN |= ((FEApproximation) m.model).resultIsNaN;
                        }
                        if (resultIsNaN)
                            for (TimeDependentFEModel m : TimeDependentFEModels) {
                                if (m.model instanceof TimeDependentModel timeDependentModel) {
                                    timeDependentModel.write_erg_xf();
                                    if (m.model instanceof CurrentModel3D currentModel3D)
                                        currentModel3D.write_erg();
                                } else {
                                    Object obj = m.getFEModel();
                                    if (obj instanceof TicadModel ticadModel) {
                                        ticadModel.write_erg_xf(m.getResult(), t + dt);
                                    }
                                }
                            }

                        ta += ts;

                    } while (ta < te);

                    System.out.println("t+dt = " + (t + dt));

                    // ...Ergebnisse rauschreiben.....................................
                    for (TimeDependentFEModel m : TimeDependentFEModels) {
                        if (m.model instanceof TimeDependentModel timeDependentModel) {
                            timeDependentModel.write_erg_xf();
                            if (m.model instanceof CurrentModel3D currentModel3D)
                                currentModel3D.write_erg();
                        } else {
                            Object obj = m.getFEModel();
                            if (obj instanceof TicadModel ticadModel)
                                ticadModel.write_erg_xf(m.getResult(), t + dt);
                        }
                    }

                    System.out.println("Runtime: " + ((System.currentTimeMillis() - time) / 1000 / 60) + " min ("
                            + ((System.currentTimeMillis() - time) / 1000) + " sec )");

                } // end for

                System.out.println("simulation end");

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // Zeitbeschraenkung
    public static boolean allowStart(java.util.Date date) {
        long currentTime = System.currentTimeMillis();
        long expire = date.getTime();
        return (currentTime < expire);
    }

    // MAC-Adressen Abfrage
    public static boolean check(String address) throws Exception {
        java.util.regex.Pattern macadresse = java.util.regex.Pattern
                .compile("[0-9a-fA-F]{2}-[0-9a-fA-F]{2}-[0-9a-fA-F]{2}-[0-9a-fA-F]{2}-[0-9a-fA-F]{2}-[0-9a-fA-F]{2}");
        java.util.regex.Matcher input = macadresse.matcher(address);
        // 1.Test: zu testende MAC-Adresse entspricht Pattern
        if (input.groupCount() == 0) {
            return false;
        }

        java.io.BufferedReader br;
        // Windows
        try {
            Process proc = new ProcessBuilder("ipconfig", "/all").start();
            br = new java.io.BufferedReader(new java.io.InputStreamReader(proc.getInputStream()));
        } catch (IOException ex) {
            ex.printStackTrace();
            br = null;
        }
        // Linux
        if (br == null) {
            try {
                Process proc = new ProcessBuilder("ifconfig").start();
                br = new java.io.BufferedReader(new java.io.InputStreamReader(proc.getInputStream()));
            } catch (IOException ex) {
                ex.printStackTrace();
                br = null;
            }
        }
        if (br != null) {
            try {
                StringBuilder buffer = new StringBuilder();
                for (;;) {
                    int c = br.read();
                    if (c == -1) {
                        break;
                    }
                    buffer.append((char) c);
                }
                String outputText = buffer.toString();
                br.close();

                // 2.Test: zu testende MAC-Adresse entspricht gefundenen MAC-Adressen
                java.util.regex.Matcher matcher = macadresse.matcher(outputText);
                int matches = matcher.groupCount();
                for (int i = 0; i < matches; i++) {
                    String match = matcher.group(i);
                    if (match.equals(address)) {
                        return true;
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

}
