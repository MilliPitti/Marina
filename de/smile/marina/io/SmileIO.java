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
import de.smile.marina.fem.FTriangle;
import de.smile.marina.fem.FTriangleMesh;

/**
 *
 * @author Peter Milbradt, Christoph Lippert
 * @version 2.7.9
 */
public class SmileIO {

    public static enum MeshFileType {
        JanetBin, SystemDat
    };

    public static enum ResultFileType {
        SysErg, Current3DErg
    };

    public static enum BoundaryConditionFileType {
        BAWFormat
    };

    /** Creates a new instance of SmileIO */
    public SmileIO() {
    }

    /**
     * Lesen eines Objekts der Klasse TopoElementMesh aus einer Datei im
     * Binaerformat mit
     * expliziter uebergabe der Ladeparameter
     * 
     * @param filename
     * @return
     */
    @SuppressWarnings("unused")
    public static FEDecomposition readFEDfromJanetBin(String filename) // throws Exception
    {
        FTriangleMesh fed = null;

        double x, y, z;
        boolean hasNaNValues = false;
        int nr;
        short status, kennung;
        int anzPolys, anzPoints;
        boolean read_status_byte = false;

        FileIO bin_in = new FileIO();

        try {
            bin_in.fopenbinary(filename, FileIO.input);

            // Netz aus einer Binaerdatei lesen

            // Version auslesen
            float version = bin_in.fbinreadfloat();
            if (version < 1.5f) {
                System.out.println("Deprecated version of Janet-Binary-Format, version found: " + version
                        + ", current version: 1.8");
                return null;
                // throw new Exception("Deprecated version of Janet-Binary-Format, version
                // found: "+version+", current version: 1.8");
            }

            if (version < 1.79)
                read_status_byte = true;

            System.out.println("Read FEDecomposition from " + filename);

            // zunaechst den FileHeader lesen
            boolean writePointNumbers = bin_in.fbinreadboolean();
            boolean writePointAttributes = bin_in.fbinreadboolean();
            int anzAttributes = bin_in.fbinreadint();
            boolean writePointStatus = bin_in.fbinreadboolean();
            boolean writeConstraintPolygons = bin_in.fbinreadboolean();
            boolean writeConstraintEdges = bin_in.fbinreadboolean();
            boolean writeElements = bin_in.fbinreadboolean();
            boolean writeElementNumbers = bin_in.fbinreadboolean();
            boolean writeElementKennung = bin_in.fbinreadboolean();
            boolean hasHeights = bin_in.fbinreadboolean(); // Peter 05.04.2016 true = Hoehen, false = Tiefen

            // Layertyp ueberlesen
            int filetype = bin_in.fbinreadint();
            // liegt UnTRIM-Gitetr mit diskreten Kantentiefen vor??
            boolean is_untrim = (filetype == 2);

            // Anzahl der Punkte lesen
            int anzk = bin_in.fbinreadint();
            DOF[] dof = new DOF[anzk];

            // Punkte lesen
            for (int i = 0; i < anzk; i++) {
                // Punktnummer schreiben...
                if (writePointNumbers)
                    nr = bin_in.fbinreadint();
                else
                    nr = i;

                // x,y,z immer schreiben
                x = bin_in.fbinreaddouble();
                y = bin_in.fbinreaddouble();
                z = bin_in.fbinreaddouble();
                if (hasHeights)
                    z *= -1.; // Peter 05.04.2016 wenn Hoehen dann auf Tiefen umrechnen
                // Plausibilitaetskontrolle
                if (Double.isNaN(z))
                    hasNaNValues = true;
                dof[nr] = new DOF(nr, x, y, z);

                // Status-Flag lesen
                if (writePointStatus) {
                    int pointstatus = bin_in.fbinreadshort();
                    // System.out.println(nr+": "+pointstatus);
                }

            }

            // Abbruch, wenn Netz nicht ok!
            if (hasNaNValues) {
                System.out.println("***                     ACHTUNG                       ***");
                System.out.println("***   Das Berechnungsnetz hat Knoten mit nicht        ***");
                System.out.println("***   definierten Tiefenwerten (z= NaN)!              ***");
                System.out.println("***   Weisen Sie den Knoten Tiefen zu und starten     ***");
                System.out.println("***   Marina erneut!                                  ***");
                System.out.println("***                                                   ***");
                System.exit(0);
            }

            // ***********************************
            // Polygone ueberlesen
            // ***********************************
            // Anzahl der Polygonzuege
            anzPolys = bin_in.fbinreadint();

            if (anzPolys != 0) {
                for (int i = 0; i < anzPolys; i++) {
                    // alle ueberlesen...

                    // offen/geschlossen
                    bin_in.fbinreadboolean();
                    // Anzahl der Polygonpunkte
                    anzPoints = bin_in.fbinreadint();
                    // Nummern
                    bin_in.fbinreadint();
                    // Typ
                    bin_in.fbinreadString();
                    // Bezeichnung
                    bin_in.fbinreadString();
                    // Status lesen
                    bin_in.fbinreadbyte();
                    // Punktnummern
                    for (int a = 0; a < anzPoints; a++)
                        bin_in.fbinreadint();
                }
            }

            // Anzahl der Dreiecke lesen
            int anze = bin_in.fbinreadint();

            FTriangle[] elemente = new FTriangle[anze];

            // ***********************************
            // Dreieckselemente lesen
            // ***********************************
            int anzNodes;
            if (anze > 0) {
                for (int i = 0; i < anze; i++) {
                    // Dreiecksnummer
                    if (writeElementNumbers)
                        nr = bin_in.fbinreadint();
                    else
                        nr = i;

                    anzNodes = bin_in.fbinreadshort();

                    int[] node_numbers = new int[anzNodes];
                    for (int a = 0; a < anzNodes; a++) {
                        node_numbers[a] = bin_in.fbinreadint();
                        if (is_untrim)
                            bin_in.fbinreaddouble();
                    }
                    // Randkennung lesen??
                    kennung = 0;
                    if (writeElementKennung) {
                        if (read_status_byte) {
                            byte b = bin_in.fbinreadbyte();
                            status = 0;
                            status = (short) (status | b);
                            status = (short) (status & 255);
                        } else
                            status = bin_in.fbinreadshort();

                        // Kantenkennung ermitteln
                        if ((status & 1) == 1)
                            kennung = (short) (kennung | 1);
                        if ((status & 2) == 2)
                            kennung = (short) (kennung | 2);
                        if ((status & 4) == 4)
                            kennung = (short) (kennung | 4);
                    }
                    // Element erzeugen

                    elemente[nr] = new FTriangle(dof[node_numbers[0]], dof[node_numbers[1]], dof[node_numbers[2]]);
                    elemente[nr].setKennung(kennung); // Elementkennung
                    elemente[nr].number = nr;

                    // Elemente den Knoten bekannt machen
                    for (int k = 0; k < elemente[nr].getNumberofDOFs(); k++) {
                        DOF dofk = elemente[nr].getDOF(k);
                        dofk.addFElement(elemente[nr]);
                    }

                }
            }

            fed = new FTriangleMesh(elemente, dof);

            bin_in.fclosebinary(FileIO.input);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen");
        }
        return fed;
    }
}
