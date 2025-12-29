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

import bijava.math.ifunction.DiscretScalarFunction1d;
import bijava.math.ifunction.ScalarFunction1d;
import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FElement;
import de.smile.math.Function;

/**
 *
 * @author milbradt
 * @version 3.15.5
 */
public class H_Q_Steuerung  extends QSteuerung {
    
    private ScalarFunction1d h_q_Relation;
    
    protected H_Q_Steuerung(){}
    
    /**
     * @param h_q_Relation
     * @param knotennummern
     * @param sysdat
     */
    public H_Q_Steuerung(DiscretScalarFunction1d h_q_Relation, int[] knotennummern, FEDecomposition sysdat) {
        maxQ = h_q_Relation.getmax()[1];
        epsilon = Math.max(1.E-10, maxQ * repsilon); // Abbruchgrenze, relatives zur Groesze der Durchfluesse

        this.sysdat = sysdat;
        this.h_q_Relation=h_q_Relation;
        this.knotennummern=knotennummern;
        double[][] profilefeld = new double[2][knotennummern.length];
        profilefeld[0][0]=s;
        profilefeld[1][0]=sysdat.getDOF(knotennummern[0]).z;

        for ( int i = 1; i < knotennummern.length; i++ ){
            s+=QSteuerung.distance2d(sysdat.getDOF(knotennummern[i]),sysdat.getDOF(knotennummern[i-1]));
            profilefeld[0][i]=s;
            profilefeld[1][i]=sysdat.getDOF(knotennummern[i]).z;
        }
        this.z=new DiscretScalarFunction1d(profilefeld);			// Tiefenpolynom
        this.d=new DiscretScalarFunction1d(profilefeld);
    }
    
    /**
     * @param x
     * @param t
     */
    @Override
    public synchronized void update(SurfaceWaterModelData[] x, double t){
        if (time != t) {
/* START: bestimmen der mittleren Wasserspiegellage ueber den Querschnitt */            
            double h_mean=0.;
            int n=0;
            double zmin = -z.getValueAt(0)[1];
            for ( int i = 0; i < knotennummern.length; i++ ){
                zmin = (zmin < -z.getValueAt(i)[1]) ? zmin : -z.getValueAt(i)[1]; // min(zmin, -z.getValueAt(i)[1]);                
                DOF dof = sysdat.getDOF(knotennummern[i]); 
                for (FElement elem : dof.getFElements()) {
                    for (int ll = 0; ll < 3; ll++) {
                        if (elem.getDOF(ll) == dof) {
                            for (int ii = 0; ii < 3; ii++) {
                                int jtmp = elem.getDOF((ll + ii) % 3).number;
                                SurfaceWaterModelData tmpcdata = SurfaceWaterModelData.extract(sysdat.getDOF(jtmp));
                                if (tmpcdata.totaldepth > WATT) {
                                    h_mean += tmpcdata.eta;
                                    n++;
                                }
                            }
                            break;
                        }
                    }
                }
            }
            if(n>0){   /** todo hier gibt es noch Optimierungspotential */
                h_mean /= n;
                h_mean = Function.max(h_mean,zmin+Function.max(WATT,Math.abs(h_q_Relation.getValue(h_mean))/(Math.sqrt(PhysicalParameters.G * 20 *WATT)*s)));
            }else{ // alle Knoten der RB haben Tiefe = 0, falls Q != 0 so muss der wasserspiegel angehoben werden!!
                h_mean = zmin+Function.max(WATT,Math.abs(h_q_Relation.getValue(h_mean))/(Math.sqrt(PhysicalParameters.G * 10 *WATT)*s));
            }
/* END: bestimmen der mittleren Wasserspiegellage ueber den Querschnitt */
            for (int i = 0; i < knotennummern.length; i++) {
                if ((z.getValueAt(i)[1] + h_mean) > 0 && x[knotennummern[i]].bh == null) {
                    x[knotennummern[i]].eta = h_mean;
                }
                d.setValueAt(i, Function.max(0., z.getValueAt(i)[1] + x[knotennummern[i]].eta));
            }
            berechneV(h_q_Relation.getValue(h_mean));		// Geschwindigkeitsverteilung iterativ berechnet
            time=t;
        }
    }

    @Override
    public double getDifferential(double x) {
        throw new UnsupportedOperationException("Not supported  for H_Q_Steuerung.");
    }

    @Override
    public void setPeriodic(boolean b) {
        throw new UnsupportedOperationException("Not supported for H_Q_Steuerung."); 
    }

    @Override
    public boolean isPeriodic() {
        throw new UnsupportedOperationException("Not supported  for H_Q_Steuerung."); 
    }
    
    
    
}
