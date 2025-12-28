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

import java.io.*;
import java.net.URI;
import java.net.URL;

/**
 *
 * @author Peter
 */
public class FileIO {

    public static final int input = 1;
    public static final int output = 2;
    public static final int noCommentChar = -1;

    protected PrintStream out = System.out;

    protected PrintWriter fout = null;
    protected BufferedReader r = null;
    protected StreamTokenizer fin = null;
    protected DataInputStream din = null;
    protected DataOutputStream dout = null;

    protected BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    byte[] w = new byte[8];

    /**
     * Oeffnen einer Datei zum Lesen oder Schreiben (Binary).
     */
    public void fopenbinary(String file, int mode) {
        if (mode == output) {
            try {
                // dosFile=file.replace('/','\\');
                dout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            } catch (IOException e) {
                System.out.println("Fehler beim Oeffnen von " + file);
                // e.printStackTrace();
            }
        }

        if (mode == input) {
            try {
                if (file.startsWith("file:") || file.startsWith("http:")) {
                    URL url = URI.create(file).toURL();
                    InputStream is = url.openStream();
                    din = new DataInputStream(is);
                } else
                    din = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            } catch (Exception e) {
                System.out.println("Fehler beim Oeffnen von " + file);
                System.out.println("Die Datei ist nicht vorhanden");
                // e.printStackTrace();
            }
        }
    }

    /**
     * Schliessen einer geoeffneten Datei.
     */
    public void fclosebinary(int mode) {
        try {
            if (mode == output && dout != null) {
                dout.flush();
                dout.close();
                dout = null;
            } else if (mode == input && din != null) {
                din = null;
            }
        } catch (IOException e) {
            System.out.println("Fehler beim Schliessen der Eingabedatei!");
            // e.printStackTrace();
        }
    }

    /**
     * Lesen einer Ganzzahl von Datei (Binary).
     */
    public final int fbinreadint() throws java.io.IOException {
        return din.readInt();
    }

    /**
     * Lesen einer Ganzzahl von Datei (Binary) im Little-Endian-Mode.
     */
    public final int fbinreadintLittleEndian() throws java.io.IOException {
        din.readFully(w, 0, 4);
        return (w[3]) << 24 |
                (w[2] & 0xff) << 16 |
                (w[1] & 0xff) << 8 |
                (w[0] & 0xff);
    }

    /**
     * Lesen einer Ganzzahl von Datei (Binary) im Little-Endian-Mode.
     */
    public final long fbinreadlongLittleEndian() throws java.io.IOException {
        din.readFully(w, 0, 8);
        return (long) (w[7]) << 56 | /* long cast needed or shift done modulo 32 */
                (long) (w[6] & 0xff) << 48 |
                (long) (w[5] & 0xff) << 40 |
                (long) (w[4] & 0xff) << 32 |
                (long) (w[3] & 0xff) << 24 |
                (long) (w[2] & 0xff) << 16 |
                (long) (w[1] & 0xff) << 8 |
                (long) (w[0] & 0xff);
    }

    /**
     * Lesen einer Ganzzahl (short) von Datei (Binary).
     */
    public final short fbinreadshort() throws java.io.IOException {
        return din.readShort();
    }

    /**
     * Lesen einer Ganzzahl (short) von Datei (Binary).
     */
    public final byte fbinreadbyte() throws java.io.IOException {
        return din.readByte();
    }

    /**
     * Lesen einer Fliesskommazahl (double) von Datei (Binary).
     */
    public final double fbinreaddouble() throws java.io.IOException {
        return din.readDouble();
    }

    /**
     * Lesen einer Fliesskommazahl (double) im Little-Endian-Mode von Datei
     * (Binary).
     */
    public final double fbinreaddoubleLittleEndian() throws java.io.IOException {
        return Double.longBitsToDouble(fbinreadlongLittleEndian());
    }

    /**
     * Lesen einer Fliesskommazahl (float) von Datei (Binary).
     */
    public final float fbinreadfloat() throws java.io.IOException {
        return din.readFloat();
    }

    /**
     * Lesen einer Fliesskommazahl (float) von Datei (Binary).
     */
    public final float fbinreadfloatLittleEndian() throws java.io.IOException {
        return Float.intBitsToFloat(fbinreadintLittleEndian());
    }

    /**
     * Lesen eines booleschen Wertes von Datei (Binary).
     */
    public final boolean fbinreadboolean() throws java.io.IOException {
        return din.readBoolean();
    }

    /**
     * Lesen eines Strings von Datei (Binary).
     */
    public final String fbinreadString() throws java.io.IOException {
        String s = "";

        int anzBytes = din.readInt();
        // System.out.println("anzBytes: "+anzBytes);

        if (anzBytes > 20000)
            throw new IOException("i/o error");

        if (anzBytes > 0) {
            byte[] bytes = new byte[anzBytes];
            din.read(bytes);
            s = new String(bytes);
        }

        // System.out.println("gelesen: "+s);

        return s;
    }

    // NEU WIEBKE 23.02.2007 (kopiert aus de.smile.io.FileIO.java)
    /**
     * Lesen einer Zeile als Zeichenkette von Datei (ASCII).
     */
    public final String freadLine() {

        String s = "";

        if (fin == null) {
            System.out.println("Fehler beim Lesen von Datei");
            System.out.println("Es ist keine Datei geoeffnet");
        } else {

            try {
                s = r.readLine();
            } catch (IOException e) {
                System.out.println("Fehler beim Lesen einer Zeile als Zeichenkette von Datei");
                e.printStackTrace();
                return s;
            }
        }

        return s;
    }

    /**
     * Oeffnen einer Datei zum Lesen oder Schreiben. Zusaetzlich wird ein
     * Kommentarzeichen angegeben.
     */
    public void fopen(String file, int mode, int comment) throws Exception {
        // protected long file_length;
        if (mode == output) {

            fout = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        }

        if (mode == input) {
            // File f=new File(file);
            // file_length=f.length();

            r = new BufferedReader(new FileReader(file));
            fin = new StreamTokenizer(r);
            fin.eolIsSignificant(false);
            if (comment != noCommentChar)
                fin.commentChar(comment);
        }
    }

    /**
     * Schliessen einer geoeffneten Datei.
     */
    public void fclose(int mode) {
        if (mode == output && fout != null) {
            fout.flush();
            fout.close();
            fout = null;
        } else if (mode == input && fin != null) {
            try {
                r.close();
                fin = null;
            } catch (IOException e) {
                /*
                 * if (PRINT_MESSAGES)
                 * System.out.println("Fehler beim Schliessen der Eingabedatei!");
                 */
                // e.printStackTrace();
            }
        }
    }
}
