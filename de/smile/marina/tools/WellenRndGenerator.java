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
package de.smile.marina.tools;

import de.smile.math.Function;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.text.DecimalFormat;

/** Screibt aus einer Wind.dat eine datei mit wellendaten
 *
 * @author milbradt
 */
public class WellenRndGenerator {

	StreamTokenizer st;
	PrintWriter fileWriterOut;

	public WellenRndGenerator(String windDatName, String waveRndName) {
        
        try {
            // Datei oeffnen
            FileInputStream is = new FileInputStream(windDatName);
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('C');

            //Datei erzeugen
            File outFile = new File(waveRndName);
            FileWriter writer = new FileWriter(outFile);
            
            BufferedWriter bwriter = new BufferedWriter(writer);
            fileWriterOut = new PrintWriter(bwriter);
        } catch (Exception e) {
            System.exit(0);
        }
    }

	private double Next() {
		double wert = 0.0;
		try {
			while (st.nextToken() != StreamTokenizer.TT_NUMBER);
			wert = (double) st.nval;
		} catch (Exception e) {}
		return wert;
	}

	private int nextInt() {
		int wert = 0;
		try {
			while (st.nextToken() != StreamTokenizer.TT_NUMBER);
			wert = (int) st.nval;
		} catch (Exception e) {}
		return wert;
	}

	public static void main(String[] args)
	{
//		if (args.length < 2 || args[0].equals("-help")) {
//			System.out.println("liest Datei und Schreibt WellenDaten raus");
//			System.out.println("Usage:");
//			System.out.println("java WindWrt <Datei.dat> <wind.dat> ");
//		} else {
//			String iname = args[0];
//			String outname  = args[1];
//			WellenRndGenerator rconv = new WellenRndGenerator(iname,outname);
//			rconv.conv();
//		}

                String iname = "/net/smile2/space/milbradt/Projekte/AufMod/ProcessModell/Elbe/2006/Wave/wind.dat";
                String outname  = "/net/smile2/space/milbradt/Projekte/AufMod/ProcessModell/Elbe/2006/Wave/wave.dat";
                WellenRndGenerator rconv = new WellenRndGenerator(iname,outname);
			rconv.conv();
	}


	public void conv(){

             DecimalFormat df =   new DecimalFormat  ( "0000" );

		int anzwerte = (int) Next();

		fileWriterOut.println("\t"+anzwerte);

                double windx, windy, uwind;

		double wh, periode;
		double theta, zeit;

		for (int i=0;i<anzwerte;i++){
			zeit= nextInt();
                        windx = Next();
                        windy = Next();
                        uwind = Function.norm(windx, windy);

                        wh=(0.008*uwind*uwind+0.11*uwind);
			periode=1.8*Math.sqrt(uwind);

                        theta = Function.getAngle(windx, windy);
			fileWriterOut.println("\t"+df.format(zeit)+" \t"+wh+" \t"+theta+" \t"+periode);
		}
	    System.out.println("WellenDaten geschrieben");

			fileWriterOut.close();
	}
}
