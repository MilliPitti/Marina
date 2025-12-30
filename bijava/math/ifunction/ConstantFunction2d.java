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
package bijava.math.ifunction;

import java.awt.geom.*;
public class ConstantFunction2d implements ScalarFunction2d {
    private double value=0;
    
    public ConstantFunction2d(double value){
    	this.value=value;
    }

    @Override
    public double getValue(Point2D.Double p) {
	return value;
    }
	
    @Override
    public double[] getDifferential(Point2D.Double p) {
    	double[] v=new double[2];
    	v[0]=0.; v[1]=0.;
	return v;
    }

    public void setPeriodic(boolean b){}
    
    public boolean isPeriodic(){
	return true;
    }
}
