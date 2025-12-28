package de.smile.marina.tools;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.FTriangle;
import de.smile.marina.fem.FTriangleMesh;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2DData;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exportiert auf der Basis einer Kilometrierung/Schnitt als insel.dat eine csv
 * mit zugehoeriger Wasserspiegellage
 *
 * @author milbradt
 */
public class WasseSpiegelLagenExport {

    public static void exportCSV(String currentergName, String inseldatName, int record) {

        try (FileInputStream stream = new FileInputStream(new File(currentergName));
                DataInputStream inStream = new DataInputStream(stream)) {
            // erstes Durchscannen
            // Kommentar lesen, bis ASCII-Zeichen 7 kommt
            StringBuilder description = new StringBuilder();
            char c;
            do {
                c = (char) inStream.readByte();
                description.append(c);
            } while (c != 7);
            // Ende Kommentar
            // sind Offset-Koordianten gespeichert
            String model_component = "";
            double offset_x = 0.0, offset_y = 0.0;
            try {
                offset_x = TicadIO.getStoredIntegerValue(description.toString(), "OffSetX");
                offset_y = TicadIO.getStoredIntegerValue(description.toString(), "OffSetY");
                model_component = TicadIO.getStoredModelComponent(description.toString());
                // System.out.println("ModelComponent " + model_component);
            } catch (Exception ex) {
            }

            // Anzahl Elemente, Knoten und Rand lesen
            int anzKnoten = inStream.readInt();
            int anzr = inStream.readInt();
            int anzElemente = inStream.readInt();
            // Ueberlesen folgender Zeilen
            inStream.skip(9 * 4);
            // Ergebnismaske lesen und auswerten
            int ergMaske = inStream.readInt();
            int anzWerte = TicadIO.ergMaskeAuswerten(ergMaske);
            // dummy
            inStream.readInt();
            // Knoten erzeugen
            DOF[] dof = new DOF[anzKnoten];
            for (int i = 0; i < anzKnoten; i++) {
                dof[i] = new DOF(i, 0., 0., 0.);
            }
            // Elementverzeichnis Lesen
            FTriangle[] elemente = new FTriangle[anzElemente];
            for (int i = 0; i < anzElemente; i++) {
                int nodeI = inStream.readInt();
                int nodeJ = inStream.readInt();
                int nodeK = inStream.readInt();
                int kennung = i;
                int id = inStream.readInt();
                FTriangle elmt = new FTriangle(dof[nodeI], dof[nodeJ], dof[nodeK]);
                elmt.setKennung(kennung);
                elemente[i] = elmt;
            }
            // Knotenkoordinaten Lesen
            for (int i = 0; i < anzKnoten; i++) {
                dof[i].x = inStream.readFloat() + offset_x;
                dof[i].y = inStream.readFloat() + offset_y;
                dof[i].z = inStream.readFloat();
                dof[i].addModelData(new CurrentModel2DData());
            }

            // FED erzeugen
            FTriangleMesh fed = new FTriangleMesh(elemente, dof);
            fed.anzr = anzr;
            fed.anzk = anzr + anzKnoten;

            // bis zum record-Satz springen
            inStream.skip((4L + anzKnoten * anzWerte * 4) * record);
            float t = inStream.readFloat();
            for (int i = 0; i < anzKnoten; i++) {
                CurrentModel2DData cmd = CurrentModel2DData.extract(dof[i]);
                if ((ergMaske & TicadIO.HRES_NONE) == TicadIO.HRES_NONE) {
                    inStream.skip(4);
                }

                if ((ergMaske & TicadIO.HRES_POS) == TicadIO.HRES_POS) {
                    inStream.skip(4);
                }

                if ((ergMaske & TicadIO.HRES_Z) == TicadIO.HRES_Z) {
                    cmd.z = inStream.readFloat();
                }

                if ((ergMaske & TicadIO.HRES_V) == TicadIO.HRES_V) {
                    cmd.u = inStream.readFloat();
                    cmd.v = inStream.readFloat();
                    cmd.cv = Function.norm(cmd.u, cmd.v);
                }

                if ((ergMaske & TicadIO.HRES_Q) == TicadIO.HRES_Q) {
                    inStream.skip(8);
                }

                if ((ergMaske & TicadIO.HRES_H) == TicadIO.HRES_H) {
                    final double h = inStream.readFloat();
                    cmd.setWaterLevel(h);
                }

                if ((ergMaske & TicadIO.HRES_SALT) == TicadIO.HRES_SALT) {
                    inStream.skip(4);
                }

                if ((ergMaske & TicadIO.HRES_EDDY) == TicadIO.HRES_EDDY) {
                    inStream.skip(4);
                }

                if ((ergMaske & TicadIO.HRES_SHEAR) == TicadIO.HRES_SHEAR) {
                    cmd.tauBx = inStream.readFloat();
                    cmd.tauBy = inStream.readFloat();
                }

                if ((ergMaske & TicadIO.HRES_V_SCAL) == TicadIO.HRES_V_SCAL) {
                    inStream.skip(4);
                }

                if ((ergMaske & TicadIO.HRES_Q_SCAL) == TicadIO.HRES_Q_SCAL) {
                    inStream.skip(4);
                }

                if ((ergMaske & TicadIO.HRES_AH) == TicadIO.HRES_AH) {
                    inStream.skip(4);
                }
            }

            // Insel.dat Lesen
            // for all DateiNamen
            Path path = FileSystems.getDefault().getPath(inseldatName);

            List<String> lines = Files.readAllLines(path, Charset.defaultCharset());

            for (String zeile : lines) {
                if (zeile.contains("DAMM")) {
                    continue;
                }
                if (zeile.contains("Kilometrierung")) {
                    continue;
                }
                if (zeile.contains("ENDE DATEI")) {
                    continue;
                }
                StringTokenizer sT = new StringTokenizer(zeile);
                double x = Double.parseDouble(sT.nextToken());
                double y = Double.parseDouble(sT.nextToken());
                double z = Double.parseDouble(sT.nextToken());
                DOF rdof = new DOF(-1, x, y, z);
                CurrentModel2DData md = new CurrentModel2DData();
                rdof.addModelData(md);
                for (FElement ftriangle : fed.getFElements()) {
                    if (ftriangle.contains(rdof)) {
                        double[] lambda = ftriangle.getNaturefromCart(rdof);
                        CurrentModel2DData md0 = CurrentModel2DData.extract(ftriangle.getDOF(0));
                        CurrentModel2DData md1 = CurrentModel2DData.extract(ftriangle.getDOF(1));
                        CurrentModel2DData md2 = CurrentModel2DData.extract(ftriangle.getDOF(2));
                        md.eta = lambda[0] * md0.eta + lambda[1] * md1.eta + lambda[2] * md2.eta;
                        System.out.println(x + "\t" + y + "\t" + z + "\t" + md.eta);
                        break;
                    }
                }
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(WasseSpiegelLagenExport.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WasseSpiegelLagenExport.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void main(String[] args) {
        // exportCSV("/home/milbradt/Desktop/nas6/NABU/SHGruetz/Arbeitsdaten/NachweisRechnungen/HydroDynamik/IST/01_NNQ/cSHGr_IST_NNQ.bin",
        // "/home/milbradt/Desktop/nas6/NABU/SHGruetz/Basisdaten/Kilometrierung_SHGruetz_Achspoly_Havel_alle100m.dat",
        // 5);
        exportCSV(args[0], args[1], Integer.parseInt(args[2]));
    }
}
