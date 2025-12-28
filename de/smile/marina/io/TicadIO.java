/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2024

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

import bijava.marina.geom3d.*;
import de.smile.marina.MarinaXML;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEApproximation;
import de.smile.marina.fem.FTriangle;
import de.smile.marina.fem.FTriangleMesh;
import de.smile.marina.fem.TicadModel;
import de.smile.marina.fem.TimeDependentFEApproximation;
import de.smile.math.Function;
import java.io.*;
import java.util.*;
import javax.vecmath.*;

/**
 * @author Peter Milbradt
 * @version 4.1.0
 */
public class TicadIO {
    
    private TicadIO() {
    }

    // Bitmasken fuer Ergebnisdatei
    public final static int HRES_NONE = (1 << 0);
    public final static int HRES_POS = (1 << 1);       // 2 float
    public final static int HRES_Z = (1 << 2);         // 1 float
    public final static int HRES_V = (1 << 3);         // 2 float
    public final static int HRES_Q = (1 << 4);         // 2 float
    public final static int HRES_H = (1 << 5);         // 1 float
    public final static int HRES_SALT = (1 << 6);      // 1 float
    public final static int HRES_EDDY = (1 << 7);      // 1 float
    public final static int HRES_SHEAR = (1 << 8);     // 2 float
    public final static int HRES_V_SCAL = (1 << 9);    // 1 float
    public final static int HRES_Q_SCAL = (1 << 10);   // 1 float
    public final static int HRES_AH = (1 << 11);       // 1 float

    // Kennzahlen fuer Randbedingungsidentifikation
    public final static byte U_GESETZT = 12;
    public final static byte V_GESETZT = 13;
    public final static byte H_GESETZT = 3;
    public final static byte qx_GESETZT = 1;
    public final static byte qy_GESETZT = 2;
    public final static byte Qx_GESETZT = 14;
    public final static byte Qy_GESETZT = 15;
    public final static byte SALT_GESETZT = 4;
    public final static byte TEMPERATURE_GESETZT = 6;
    public final static byte SEDIMENTKONZENTRATION_GESETZT = 8;

    /*----------------------------------------------------------------------*/
    /*      Elementkennung (Bits von Element[].kennung)                     */
    /*----------------------------------------------------------------------*/
    public static final int bit_nr_kante_ij = 4;  /* Bit Nr. 2 steht fuer i-j           */
    public static final int bit_nr_kante_jk = 1;  /* Bit Nr. 0 steht fuer j-k           */
    public static final int bit_nr_kante_ki = 2;  /* Bit Nr. 1 steht fuer k-i           */
    public static final int bit_kante_ij = 4;     /* (2^2)  Kante i-j geschlossen       */
    public static final int bit_kante_ji = 4;     /* (2^2)                              */
    public static final int bit_kante_jk = 1;     /* (2^0)  Kante j-k geschlossen       */
    public static final int bit_kante_kj = 1;     /* (2^0)                              */
    public static final int bit_kante_ki = 2;     /* (2^1)  Kante k-i geschlossen       */
    public static final int bit_kante_ik = 2;     /* (2^1)                              */
    public static final int bit_kante_ijk = 5;    /* (2^0 + 2^2)  Kante ij und jk       */
    public static final int bit_kante_jki = 3;    /* (2^0 + 2^1)  Kante jk und ki       */
    public static final int bit_kante_kij = 6;    /* (2^1 + 2^2)  Kante ki und ij       */
    public static final int bit_kante_ijki = 7;   /* (2^0 + 2^1 + 2^2)  alle Kanten     */


    public static int NextInt(StreamTokenizer st) {
        int wert = 0;
        int nextToken;
        try {
            do {
                nextToken = st.nextToken();
                if (nextToken == StreamTokenizer.TT_EOF) {
                    System.out.println("End of File");
                    System.exit(0);
                }
            } while (nextToken != StreamTokenizer.TT_NUMBER);
            wert = (int) st.nval;
        } catch (IOException e) {
        }
        return wert;
    }

    public static long NextLong(StreamTokenizer st) {
        long wert = 0;
        int nextToken;
        try {
            do {
                nextToken = st.nextToken();
                if (nextToken == StreamTokenizer.TT_EOF) {
                    System.out.println("End of File");
                    System.exit(0);
                }
            } while (nextToken != StreamTokenizer.TT_NUMBER);
            wert = (long) st.nval;
        } catch (IOException e) {
        }
        return wert;
    }

    public static double NextDouble(StreamTokenizer st) {
        double wert = 0.0;
        int nextToken;
        try {
            do {
                nextToken = st.nextToken();
                if (nextToken == StreamTokenizer.TT_EOF) {
                    System.out.println("End of File");
                    System.exit(0);
                }
            } while (nextToken != StreamTokenizer.TT_NUMBER);

            wert = st.nval;

        } catch (IOException e) {
        }
        return wert;
    }


    public static FTriangleMesh readFESysDat(String filename) {

        int rand_knoten;
        int gebiets_knoten;
        int knoten_nr;
        int point_nr0, point_nr1, point_nr2, status, e_nr;

        double x, y, z;
        //boolean hasNaNValues= false;
        String line;

        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');

            System.out.println("Read FEDecomposition from " + filename);

            do {
                line = systemfile.freadLine();
            } while (line.startsWith("C"));
            java.util.StringTokenizer strto = new StringTokenizer(line, " \t\n\r\f,");
            rand_knoten = Integer.parseInt(strto.nextToken());

            do {
                line = systemfile.freadLine();
            } while (line.startsWith("C"));


            strto = new StringTokenizer(line, " \t\n\r\f,");
            gebiets_knoten = Integer.parseInt(strto.nextToken());

            if (rand_knoten < 0 || rand_knoten > 10000000 || gebiets_knoten < 0 || gebiets_knoten > 10000000) {
                throw new Exception("Fehler");
            }

            // Knoten einlesen
            DOF[] dof = new DOF[rand_knoten + gebiets_knoten];
            int p_count = 0;
            while (p_count < (rand_knoten + gebiets_knoten)) {
                line = systemfile.freadLine();
                strto = new StringTokenizer(line, " \t\n\r\f,");

                //System.out.println(""+line+"\n");
                if (!line.startsWith("C")) {
                    knoten_nr = Integer.parseInt(strto.nextToken());
                    x = Double.parseDouble(strto.nextToken());
                    y = Double.parseDouble(strto.nextToken());
                    try {
                        z = Double.parseDouble(strto.nextToken());
                    } catch (NumberFormatException ex) {
                        z = Double.NaN;
                    }

                    //Sicherheitsabfrage auf z-Werte NaN fehlt!!!!
                    if (Double.isNaN(z)) {
                        //hasNaNValues=true;
                        System.out.println("");
                        System.out.println("********************************        ERROR         ***********************************");
                        System.out.println("Invalid z-value (z=NaN) in FEDecomposition-File: <" + filename + "> node number <" + p_count + ">");
                        System.out.println("To correct this problem ensure that node nr <" + p_count + "> has a correct floating point ");
                        System.out.println("depth/heigth value");
                        System.out.println("*****************************************************************************************");
                        System.out.println("");
                        System.exit(0);
                    }

                    dof[knoten_nr] = new DOF(knoten_nr, x, y, z);

                    //if(p_count%1000==0) System.out.println(p_count);
                    p_count++;
                }

            }

            // Anzahl der Elemente
            int anz_elemente;
            do {
                line = systemfile.freadLine();
            } while (line.startsWith("C"));
            strto = new StringTokenizer(line, " \t\n\r\f,");
            anz_elemente = Integer.parseInt(strto.nextToken());

            FTriangle[] elemente = new FTriangle[anz_elemente];

            int e_count = 0;
            while (e_count < anz_elemente) {
                line = systemfile.freadLine();
                strto = new StringTokenizer(line, " \t\n\r\f,");

                if (!line.startsWith("C")) {
                    point_nr0 = Integer.parseInt(strto.nextToken());
                    point_nr1 = Integer.parseInt(strto.nextToken());
                    point_nr2 = Integer.parseInt(strto.nextToken());

                    status = Integer.parseInt(strto.nextToken());
                    e_nr = Integer.parseInt(strto.nextToken());


                    FTriangle fetri = new FTriangle(dof[point_nr0], dof[point_nr1], dof[point_nr2]);
                    fetri.setKennung(status);  // Elementkennung
                    elemente[e_count] = fetri;
                    fetri.number = e_nr;  // Elementnumber
                    // Elemente den Knoten bekannt machen
                    for (int k = 0; k < fetri.getNumberofDOFs(); k++) {
                        DOF dofk = fetri.getDOF(k);
                        dofk.addFElement(fetri);
                    }

                    e_count++;
                }

            }

            systemfile.fclose(FileIO.input);
            FTriangleMesh fed = new FTriangleMesh(elemente, dof);
            fed.anzr = rand_knoten;

            return fed;

        } catch (Exception ex) {
            //System.out.println("Fehler beim Import");
            ex.printStackTrace();
        }
        return null;


    }

    public static DomainDecomposition readSysDat(String nam) {
        DomainDecomposition fed = new DomainDecomposition();

        try {
            InputStream is = new FileInputStream(nam);

            BufferedReader r = new BufferedReader(new InputStreamReader(is));

            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('C');

            // read Point3ds
            final int anzr = NextInt(st);
            final int anzk = anzr + NextInt(st);
            Point3d[] points = new Point3d[anzk];
            System.out.print("read " + anzk);

            for (int j = 0; j < anzk; j++) {
                int pos = NextInt(st);
                points[pos] = new Point3d(NextDouble(st), NextDouble(st), NextDouble(st));
            }
            System.out.println(" nodes.");

            // read Elements
            final int anze = NextInt(st);
            System.out.println(anze);
            for (int j = 0; j < anze; j++) {
                fed.addElement(new Triangle(points[NextInt(st)], points[NextInt(st)], points[NextInt(st)]));
                NextInt(st);  // Elementkennung
                NextInt(st);  // Elementnumber
            }
            System.out.println("elemente gelesen");
        } catch (FileNotFoundException e) {
        }
        return fed;
    }

    //wertet die Ergebnismaske aus
    public static int ergMaskeAuswerten(int ergMaske) {
        int anzWerte = 0;
        if ((ergMaske & TicadIO.HRES_NONE) == TicadIO.HRES_NONE) {
            anzWerte += 1;
        }
        if ((ergMaske & TicadIO.HRES_POS) == TicadIO.HRES_POS) {
            anzWerte += 1;
        }
        if ((ergMaske & TicadIO.HRES_Z) == TicadIO.HRES_Z) {
            anzWerte += 1;
        }
        if ((ergMaske & TicadIO.HRES_V) == TicadIO.HRES_V) {
            anzWerte += 2;
        }
        if ((ergMaske & TicadIO.HRES_Q) == TicadIO.HRES_Q) {
            anzWerte += 2;
        }
        if ((ergMaske & TicadIO.HRES_H) == TicadIO.HRES_H) {
            anzWerte += 1;
        }
        if ((ergMaske & TicadIO.HRES_SALT) == TicadIO.HRES_SALT) {
            anzWerte += 1;
        }
        if ((ergMaske & TicadIO.HRES_EDDY) == TicadIO.HRES_EDDY) {
            anzWerte += 1;
        }
        if ((ergMaske & TicadIO.HRES_SHEAR) == TicadIO.HRES_SHEAR) {
            anzWerte += 2;
        }
        if ((ergMaske & TicadIO.HRES_V_SCAL) == TicadIO.HRES_V_SCAL) {
            anzWerte += 1;
        }
        if ((ergMaske & TicadIO.HRES_Q_SCAL) == TicadIO.HRES_Q_SCAL) {
            anzWerte += 1;
        }
        if ((ergMaske & TicadIO.HRES_AH) == TicadIO.HRES_AH) {
            anzWerte += 1;
        }
        return anzWerte;
    }

    public static void write_xf(DataOutputStream os, FEApproximation model) {

        FTriangleMesh net = (FTriangleMesh) model.fenet;
        int ergmask = ((TicadModel) model).getTicadErgMask();

        try {
            os.writeBytes("Datei vom Typ syserg.bin, erzeugt von Marina " + MarinaXML.majorversion + "." + MarinaXML.minorversion + "." + MarinaXML.update + "\n");
            os.writeBytes("ModelComponent " + model.getClass().getName() + "\n");

            Date date = new Date();
            os.writeBytes("Datum: " + date.toString() + "\n");

            double minX = net.getDOF(0).x;
            double minY = net.getDOF(0).y;
            for (int i = 1; i < net.anzk; i++) {
                minX = Function.min(minX, net.getDOF(i).x);
                minY = Function.min(minY, net.getDOF(i).y);
            }

            final int offSetX = ((int) (minX / 100000.)) * 100000;
            final int offSetY = ((int) (minY / 100000.)) * 100000;

            os.writeBytes("OffSetX " + offSetX + "\n");
            os.writeBytes("OffSetY " + offSetY + "\n");
            if(model instanceof TimeDependentFEApproximation timeDependentFEApproximation)
                os.writeBytes("ReferenceDate " + timeDependentFEApproximation.referenceDate + "\n");
            if(model instanceof FEApproximation)
                os.writeBytes("EPSG "+ model.epsgCode + "\n");
            
            int c = 7;  // Kennzeichner fuer das Ende der Kommentarzeilen
            os.writeByte(c);

            int anzr = net.anzr;
            int anzk = net.anzk;
            int anze = net.anze;

            //      Dateikopf schreiben
            //-------------------------------------------
            os.writeInt(anzk);
            os.writeInt(anzr);
            os.writeInt(anze);
            os.writeInt(0);
            os.writeInt(0); // hier Anzahl Ergebniszustaende
            os.writeFloat((float) 0.0);
            os.writeFloat((float) 0.0);
            os.writeFloat((float) 0.0);
            os.writeFloat((float) 0.0);
            os.writeFloat((float) 0.0);
            os.writeFloat((float) 0.0);
            os.writeInt(3);
            os.writeInt(ergmask);
            os.writeInt(0);


            //      Elementverzeichnis
            //-----------------------------------------------
            for (int i = 0; i < net.getNumberofFElements(); i++) {
                FTriangle triangle = (FTriangle) net.getFElement(i);
                for (int j = 0; j < 3; j++) {
                    os.writeInt(triangle.getDOF(j).number);
                }
                os.writeInt(triangle.getKennung());
            }

            //      Randkurve
            for (int i = 0; i < anzr; i++) {
                os.writeInt(i);
            }

            //      Knotenverzeichnis
            for (int i = 0; i < anzk; i++) {
                os.writeFloat((float) (net.getDOF(i).x - offSetX));
                os.writeFloat((float) (net.getDOF(i).y - offSetY));
                os.writeFloat((float) net.getDOF(i).z);
            }
            os.flush();
        } catch (IOException e) {
        }
    }

    public static void write_xf(DataOutputStream os, FTriangleMesh net, int ergmask) {
        try {
            os.writeBytes("Datei vom Typ syserg.bin, erzeugt von Marina \n");
            os.writeBytes("Datum:-----------------------------------------\n");

            int c = 7;  // Kennzeichner fuer das Ende der Kommentarzeilen
            os.writeByte(c);

            int anzr = net.anzr;
            int anzk = net.anzk;
            int anze = net.anze;

            //      Dateikopf schreiben
            //-------------------------------------------
            os.writeInt(anzk);
            os.writeInt(anzr);
            os.writeInt(anze);
            os.writeInt(0);
            os.writeInt(0); // hier Anzahl Ergebniszustaende
            os.writeFloat((float) 0.0);
            os.writeFloat((float) 0.0);
            os.writeFloat((float) 0.0);
            os.writeFloat((float) 0.0);
            os.writeFloat((float) 0.0);
            os.writeFloat((float) 0.0);
            os.writeInt(3);
            os.writeInt(ergmask);
            os.writeInt(0);


            //      Elementverzeichnis
            //-----------------------------------------------
            for (int i = 0; i < net.getNumberofFElements(); i++) {
                FTriangle triangle = (FTriangle) net.getFElement(i);
                for (int j = 0; j < 3; j++) {
                    os.writeInt(triangle.getDOF(j).number);
                }
                os.writeInt(triangle.getKennung());
            }

            //      Randkurve
            for (int i = 0; i < anzr; i++) {
                os.writeInt(i);
            }

            //      Knotenverzeichnis
            for (int i = 0; i < anzk; i++) {
                os.writeFloat((float) net.getDOF(i).x);
                os.writeFloat((float) net.getDOF(i).y);
                os.writeFloat((float) net.getDOF(i).z);
            }
            os.flush();
        } catch (IOException e) {
        }
    }
    
    public static String getStoredReferenceDate(String text) {
        try {
            String key = "ReferenceDate";
            int offset_index = text.indexOf(key);
            if (offset_index == -1) {
                return "";
            }
            int length = key.length();
            String value_string = "";
            int i = offset_index + length + 1;
            int numberWhite = 0;
            while (i < text.length() && (!Character.isWhitespace(text.charAt(i)) || numberWhite == 0)) {
                if (Character.isWhitespace(text.charAt(i))) {
                    numberWhite++;
                }
                value_string += text.charAt(i);
                i++;
            }
            return value_string;
        } catch (Exception ex) {}
        return "";
    }

    public static int getStoredIntegerValue(String text, String key) {
        try {
            int offset_int = 0;
            int offset_index = text.indexOf(key);
            if (offset_index == -1) {
                return offset_int;
            }
            int length = key.length();
            String value_string = "";
            int i = offset_index + length + 1;
            while (i < text.length() && (Character.isDigit(text.charAt(i)) || text.charAt(i) == '-')) {
                value_string += text.charAt(i);
                i++;
            }
            offset_int = Integer.parseInt(value_string);
            return offset_int;
        } catch (NumberFormatException ex) {
        }
        return 0;
    }

    public static String getStoredModelComponent(String text) {
        try {
            String key = "ModelComponent";
            int offset_index = text.indexOf(key);
            if (offset_index == -1) {
                return "";
            }
            int length = key.length();
            String value_string = "";
            int i = offset_index + length + 1;
            while (i < text.length() && !Character.isWhitespace(text.charAt(i))) {
                value_string += text.charAt(i);
                i++;
            }
            return value_string;
        } catch (Exception ex) {
        }
        return "";
    }
}
