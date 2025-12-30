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
package de.smile.marina.fem.model.meteorology;
import java.io.* ;
import javax.vecmath.*;
import bijava.math.ifunction.*;

public class OKWind implements WindData {
    Point2d location=null;
    ScalarFunction1d windx = null;
    ScalarFunction1d windy = null;
    
    double wertewx[][];
    double wertewy[][];
    
    double actualtime=Double.NaN;
    double result[] = {0.,0.};
    
    InputStream is = null;
    StreamTokenizer st = null;
    Reader r = null;
    
    public static final int T=0;
    public static final int Wx=1;
    public static final int Wy=2;
    
    public OKWind(){
        windx=new ZeroFunction1d();
        windy=new ZeroFunction1d();
    }
    
    public OKWind(String name){
        try {
            is = new FileInputStream(name);
        r = new BufferedReader(new InputStreamReader(is));
        st = new StreamTokenizer(r);
        st.eolIsSignificant(true);
        st.commentChar('C');
        
        int anz=(int) Next();
        wertewx = new double [2][anz];
        wertewy = new double [2][anz];
        
        // einlesen
        for ( int K = 0; K < anz; K++ ) {
            wertewx[0][K]=Next();
            wertewy[0][K]=wertewx[0][K];
            wertewx[1][K]=Next();
            wertewy[1][K]=Next();
            
        }
        windx=new DiscretScalarFunction1d(wertewx);
        windy=new DiscretScalarFunction1d(wertewy);
        windx.setPeriodic(true);
        windy.setPeriodic(true);
        
        System.out.println("\treading wind time series: "+name);
        } catch (FileNotFoundException e) {
            System.out.println("\tcan't open "+ name + " for wind time series");
            System.exit(-1);
        }
        
    }
    
    public double[] getValue(double t) {
        if (t != actualtime) {
            result[0] = windx.getValue(t);
            result[1] = windy.getValue(t);
            actualtime=t;
        }
        return result;
    }
    
    public double[] getValue(double x, double y, double t) {
        if (t != actualtime) {
            result[0] = windx.getValue(t);
            result[1] = windy.getValue(t);
            actualtime=t;
        }
        return result;
    }
    
    @Override
    public double[] getValue(Point3d p, double t) {
        if (t != actualtime) {
            result[0] = windx.getValue(t);
            result[1] = windy.getValue(t);
            actualtime=t;
        }
        return result;
    }
    
    
//    public  String toString(){
//        String s="";
//        
//        for (int i=0;i<anz;i++) {
//            s+=" "+ werte[T][i] + " "+ werte[Wx][i] + " "+ werte[Wy][i];
//        }
//        return s;
//    }
    
    
    private double Next() {
        double wert = 0.0;
        try {
            while (st.nextToken() != StreamTokenizer.TT_NUMBER);
            wert = (double) st.nval;
        } catch (Exception e) {}
        return wert;
    }

    public Point2d getLocation(){
        return location;
    }

    public void setLocation(double x, double y){
        location=new Point2d(x,y);
    }
}
