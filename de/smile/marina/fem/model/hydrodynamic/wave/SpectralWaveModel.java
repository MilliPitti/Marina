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
package de.smile.marina.fem.model.hydrodynamic.wave;

import de.smile.marina.fem.model.meteorology.WindData;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FEModel;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.FTriangle;
import de.smile.marina.fem.ModelData;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2DData;
import de.smile.marina.fem.model.hydrodynamic.dim2.SedimentModel2DData;
import de.smile.marina.io.TicadIO;
import java.io.*;
import java.util.*;
import javax.vecmath.*;
import java.awt.geom.*;

import bijava.math.ifunction.*;
import bijava.marina.ticad.*;
import bijava.marina.spectra.*;
import bijava.math.ifunction.ShepardVectorFunction2D;
import bijava.math.ifunction.TimeVectorFunktion2D;
import de.smile.marina.fem.TimeDependentFEApproximation;
import de.smile.math.ode.ivp.ODESystem;


/** this ODE describe a spectral wave Model based on action density
 * @version 1.2
 * @author Peter Milbradt
 */
public class  SpectralWaveModel extends TimeDependentFEApproximation implements ODESystem,FEModel {
    
    private DataOutputStream xf_os = null;
    private FileOutputStream xf_fs = null;
    private DataOutputStream sp_os = null;
    private FileOutputStream sp_fs = null;
    
    
    private Vector<WBoundaryCondition> bcs = new Vector<>(); // temp boundary conditions
    
    private int numberofNodes;
    private double MaxTimeStep;
    int n;
    double[] result;
    SpectralWaveDat wavedat;
    WindData winddata;
    
    double WATT   = 0.1; // normal
    static final double MICHEKOEFF  = 0.78;
    static final double BREAKING  = 0.5;
    static final double G  = 9.81;
    static final double RhoWater=998.2;          // Water density
    static final double RhoAir=1.2;              // Air density
    static final double C_D=0.0012;              // Dragkoeffizient
    
    public SpectralWaveModel(FEDecomposition fe, WindData winddata, SpectralWaveDat _wavedat){
        
        fenet = fe;
        femodel=this;
        wavedat=_wavedat;
        this.winddata = winddata;
        
        setNumberOfThreads(wavedat.NumberOfThreads);
        
        WATT=wavedat.watt;
        
        if ( wavedat.bcname!=null ) readBoundCond(wavedat.bcname);
        
        // Randwerte Erzeugen
        double[] time =  {0.0, 44700.0 };
        
        Spectrum2D[] zerosp = {new ZeroDirectional(), new ZeroDirectional()};
        TimeSpectrum2D timezerosp =  new TimeSpectrum2D(time,zerosp);
        
        BretschneiderMitsuyasu bm = new BretschneiderMitsuyasu(1., 4.);
        Spectrum2D[] sp = { new BMDirectional(new BretschneiderMitsuyasu(1., 4.), -45., 75.), new BMDirectional(bm, -45., 75.) };
        TimeSpectrum2D timesp =  new TimeSpectrum2D(time,sp);
        
        // TEST Ruegen
        //for (int i=0; i<=72; i++) bcs.addElement(new WBoundaryCondition(i, timesp));
        int wbn[][] = readWaveBoundaryNodes(wavedat.boundNodes);
        for (int i = 0;i<wbn[0].length;i++) {
            if (wbn[1][i] == 0) {
                bcs.addElement(new WBoundaryCondition(wbn[0][i], timezerosp));
            }
            if (wbn[1][i] == 1) {
                bcs.addElement(new WBoundaryCondition(wbn[0][i], timesp));
            }
        }
        //for (int i=0; i<=12; i++) bcs.addElement(new WBoundaryCondition(i, timesp));
        //for (int i=39; i<=62; i++) bcs.addElement(new WBoundaryCondition(i, timesp));
        
        // DOFs initialisieren
        initialDOFs();
        
        n =  wavedat.frequencylength * wavedat.directionlength * fenet.getNumberofDOFs();
        result = new double[n];
        
        try {
            sp_fs = new FileOutputStream(wavedat.specoutname);
            sp_os = new DataOutputStream(sp_fs);
            write_specs_head();
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ wavedat.specoutname + " cannot be opened");
            System.out.println(e.getMessage());
        }
        
        
        try {
            xf_os = new DataOutputStream(new FileOutputStream(wavedat.xferg_name));
            TicadIO.write_xf(xf_os, this);
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ wavedat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
        }
        
    }
    
     
     public int getTicadErgMask(){
        // Setzen der Ergebnismaske Konzentration
        return TicadIO.HRES_V | TicadIO.HRES_H | TicadIO.HRES_EDDY | TicadIO.HRES_SHEAR;
    }
    
    public double[] initialSolution(double time){
        double x[] = new double[n];
        Shepard2D interpol = new Shepard2D(wavedat);
		for (int j=0; j<fenet.getNumberofDOFs(); j++){
			DOF dof = (DOF) fenet.getDOF(j);
            SpectralWaveModelData spectralwavemodeldata = SpectralWaveModelData.extract(dof);
            if(spectralwavemodeldata.bc != null) {
                Point2D.Double p = new Point2D.Double(dof.x, dof.y);
                
                interpol.addPoint(p,new DiscreteSpectrum2D(
                            wavedat.frequencylength,wavedat.directionlength,
                            wavedat.frequenzminimum,wavedat.frequenzmaximum,
                            wavedat.directionminimum,wavedat.directionmaximum,
                            new Spectrum2DatTime(spectralwavemodeldata.bc,time)
                                                          )
                                  );
                
            }
        }
		for (int j=0; j<fenet.getNumberofDOFs(); j++){
			DOF dof = (DOF) fenet.getDOF(j);
            int i = dof.number;
            double depth = Math.max(WATT,dof.z);
            SpectralWaveModelData spectralwavemodeldata = SpectralWaveModelData.extract(dof);
            DiscreteSpectrum2D s = spectralwavemodeldata.n;
            
            Point2D.Double p = new Point2D.Double(dof.x, dof.y);
            Spectrum2D start = interpol.getInterpolationAt(p);
            
            for (int ai=0; ai<s.getDirectionLength(); ai++){
                for (int fi=0; fi<s.getFrequencyLength(); fi++){
                    double[] w = s.getValueAt(fi,ai);
                    double sigma = 2.*Math.PI*w[0];
                    if(spectralwavemodeldata.bc == null){
                        x[GlobalIndex( fi, ai, i)] = start.getValue(w[0],w[1])/sigma;
                    }else{
                        // Einarbeiten der Randbedingungen
                        x[GlobalIndex( fi, ai, i)] = spectralwavemodeldata.bc.getValue(w[0],w[1],time)/sigma;
                    }
                    // bad wave breaking implementation
                    if(Math.sqrt(2.* x[GlobalIndex( fi, ai, i)]) > depth)
                        x[GlobalIndex( fi, ai, i)]= 0.5*Math.pow(depth,2.);
                    
                    // set boundary in frequence space
                    if((fi==0)||(fi==s.getFrequencyLength()-1))
                        x[GlobalIndex( fi, ai, i)]=0.;
                    
                    // set boundary in angle space
                    if(!s.fullRange() && ((ai==0)|(ai==s.getDirectionLength()-1)))
                        x[GlobalIndex( fi, ai, i)]=0.;
                    
                    spectralwavemodeldata.n.setValueAt(fi,ai,x[GlobalIndex( fi, ai, i)]);
                }
            }
        }
        write_erg_xf(x, time);
        return x;
    }
    
    /**
     */
    @Override
    public double[] getRateofChange(double time, double x[]){
        
        double windx=0., windy=0.;
        
        // Werte Aktualisieren
		for (int j=0; j<fenet.getNumberofDOFs(); j++){
			DOF dof = (DOF) fenet.getDOF(j);
            int i = dof.number;
            
            SpectralWaveModelData spectralwavemodeldata = SpectralWaveModelData.extract(dof);
            
            CurrentModel2DData currentdata = CurrentModel2DData.extract(dof);
            
            double z = dof.z;
            SedimentModel2DData sedimentdata = SedimentModel2DData.extract(dof);
            if (sedimentdata != null)
                z  = sedimentdata.z;

            double depth;
            if(currentdata!=null) depth = Math.max(WATT,z+currentdata.eta);
            else depth = Math.max(WATT,z);
            
            if (winddata!=null) {
                double[] wind=winddata.getValue(dof, time);
                windx=wind[0];
                windy=wind[1];
            }
            
            DiscreteSpectrum2D s=spectralwavemodeldata.n;
            DiscreteSpectrum2D sdt=spectralwavemodeldata.dndt;
            
            // set mean RadiationStress to zero
            spectralwavemodeldata.sxx=0.;
            spectralwavemodeldata.sxy=0.;
            spectralwavemodeldata.syy=0.;
            
            // update of DOF-values
            for (int ai=0; ai<s.getDirectionLength(); ai++)
                for (int fi=0; fi<s.getFrequencyLength(); fi++)	{
                    s.setValueAt(fi,ai,x[GlobalIndex( fi, ai, i)]);
                    sdt.setValueAt(fi,ai, result[GlobalIndex( fi, ai, i)]);
                }
            
            // Einarbeiten der Randbedingungen
            setBoundaryCondition(dof, time);
            
            for (int ai=0; ai<s.getDirectionLength(); ai++){
                for (int fi=0; fi<s.getFrequencyLength(); fi++){
                    
                    double[] werte = s.getValueAt(fi,ai);
                    double sigma=2.*Math.PI * werte[0];
                    
                    x[GlobalIndex( fi, ai, i)]=werte[2];
                    // update other values
                    
                    double wavenumber=WaveFunction.WaveNumber(depth,sigma);
                    double c = sigma/wavenumber;
                    double cg = 0.5 * (1. + 2. * wavenumber * depth / sinh(2. * wavenumber * depth)) * c;
                    spectralwavemodeldata.wavenumber.setValueAt(fi,ai,wavenumber);
                    spectralwavemodeldata.Cg.setValueAt(fi,ai,cg);
                    
                    // Battes/Janssen Wave breaking
                    double wavebr=Math.PI/(7.*wavenumber)*tanh(7.*MICHEKOEFF/Math.PI*wavenumber*depth);
                    wavebr=Math.min(wavebr,Math.pow(MICHEKOEFF*depth,2.));
                    spectralwavemodeldata.wavebreaking.setValueAt(fi,ai,wavebr);
                    
                    // Windinput
                    double uwind = Math.max(0., Math.cos(Math.PI * werte[1] / 180.)*windx+Math.sin(Math.PI * werte[1] / 180.)*windy);
                    double alpha=0., beta=0.;
                    if ((wavebr-werte[2])<=0.){
                        // Hsiao und Shemdin
                        beta = 0.12*RhoAir/RhoWater*sigma*Math.pow(Math.max(0.,8./3./Math.PI*uwind/c-1.),2);
                        // Cavalieri und Rizzoli [1981]
                        alpha = 80.*RhoAir*RhoAir*sigma/(RhoWater*RhoWater*G*G*wavenumber*wavenumber)*C_D*C_D*Math.pow(uwind,4.);
                    }
                    spectralwavemodeldata.windinput.setValueAt(fi,ai,alpha+beta*werte[2]);
                    
                    //  RadiationStress
                    double n=cg/c;
                    spectralwavemodeldata.sxx += werte[2] * (n*Math.pow(Math.cos(Math.PI * werte[1] / 180.),2)+n-0.5);
                    spectralwavemodeldata.sxy += werte[2] *  n*Math.cos(Math.PI * werte[1] / 180.)*Math.sin(Math.PI * werte[1] / 180.);
                    spectralwavemodeldata.syy += werte[2] * (n*Math.pow(Math.sin(Math.PI * werte[1] / 180.),2)+n-0.5);
                }
            }
            
            // set Results to zero
            spectralwavemodeldata.re.setZero();
        }
        
        MaxTimeStep = 10000.;
        
        // Elementloop
        performElementLoop();
        
        for (int j=0; j<fenet.getNumberofDOFs(); j++){
            DOF dof = (DOF) fenet.getDOF(j);
            int i = dof.number;
            int gamma = dof.getNumberofFElements();
            
            SpectralWaveModelData spectralwavemodeldata = SpectralWaveModelData.extract(dof);
            
            for (int ai=0; ai<spectralwavemodeldata.re.getDirectionLength(); ai++){
                for (int fi=0; fi<spectralwavemodeldata.re.getFrequencyLength(); fi++){
                    
                    result[GlobalIndex( fi, ai, i)] = (3. / gamma * spectralwavemodeldata.re.getValueAt(fi,ai)[2]);
                    if ((x[GlobalIndex( fi, ai, i)]<=0.) && (result[GlobalIndex( fi, ai, i)]<=0.))
                        result[GlobalIndex( fi, ai, i)] = 0.;
                }
            }
        }
        return result;
    }
    
    // ElementApproximation
    //======================
    @Override
    public double ElementApproximation(FElement element){
        
        double timeStep=Double.POSITIVE_INFINITY;
        
        FTriangle ele = (FTriangle) element;
        final double[][] koeffmat = ele.getkoeffmat();
        
        final double[] depth = new double[3];				// depth
        DiscreteSpectrum2D[] N = new DiscreteSpectrum2D[3]; 	// WaveAction
        DiscreteSpectrum2D[] Cg = new DiscreteSpectrum2D[3]; 	// Groupvelocity
        DiscreteSpectrum2D[] Ctheta = new DiscreteSpectrum2D[3]; 	// Anglevelocity
        DiscreteSpectrum2D[] k = new DiscreteSpectrum2D[3]; 	// Wavenumber
        
        double dudx = 0.;
        double dudy = 0.;
        double dvdx = 0.;
        double dvdy = 0.;
        double[] u = new double[3];
        double[] v = new double[3];
        
        int iwatt=0;  // Markieren ob ein Knoten Trocken ist
        
        
        DiscreteSpectrum2D CgxNdx = new DiscreteSpectrum2D(wavedat.frequencylength,wavedat.directionlength,
        wavedat.frequenzminimum,wavedat.frequenzmaximum,
        wavedat.directionminimum,wavedat.directionmaximum);
        DiscreteSpectrum2D CgyNdy = new DiscreteSpectrum2D(wavedat.frequencylength,wavedat.directionlength,
        wavedat.frequenzminimum,wavedat.frequenzmaximum,
        wavedat.directionminimum,wavedat.directionmaximum);
        
        DiscreteSpectrum2D dNdx = new DiscreteSpectrum2D(wavedat.frequencylength,wavedat.directionlength,
        wavedat.frequenzminimum,wavedat.frequenzmaximum,
        wavedat.directionminimum,wavedat.directionmaximum);
        DiscreteSpectrum2D dNdy = new DiscreteSpectrum2D(wavedat.frequencylength,wavedat.directionlength,
        wavedat.frequenzminimum,wavedat.frequenzmaximum,
        wavedat.directionminimum,wavedat.directionmaximum);
        
        DiscreteSpectrum2D dkxdy = new DiscreteSpectrum2D(wavedat.frequencylength,wavedat.directionlength,
        wavedat.frequenzminimum,wavedat.frequenzmaximum,
        wavedat.directionminimum,wavedat.directionmaximum);
        DiscreteSpectrum2D dkydx = new DiscreteSpectrum2D(wavedat.frequencylength,wavedat.directionlength,
        wavedat.frequenzminimum,wavedat.frequenzmaximum,
        wavedat.directionminimum,wavedat.directionmaximum);
        
        
        // compute element derivations
        for ( int j = 0; j < 3; j++) {
            DOF dof = ele.getDOF(j);
            
            SpectralWaveModelData spectralwavemodeldata = SpectralWaveModelData.extract(dof);
            
            CurrentModel2DData currentdata = CurrentModel2DData.extract(dof);
            
            double z = dof.z;
            SedimentModel2DData sedimentdata = SedimentModel2DData.extract(dof);
            if (sedimentdata != null)
                z  = sedimentdata.z;

            if(currentdata!=null) {
                depth[j] = Math.max(WATT,z+currentdata.eta);
                u[j]=currentdata.u;
                v[j]=currentdata.v;
                dudx += currentdata.u * koeffmat[j][1];
                dudy += currentdata.u * koeffmat[j][2];
                dvdx += currentdata.v * koeffmat[j][1];
                dvdy += currentdata.v * koeffmat[j][2];
            } else {
                depth[j] = Math.max(WATT,z);
                u[j]=0.;
                v[j]=0.;
            }
            
            if(depth[j]<=WATT) iwatt++;
            
            N[j]	= spectralwavemodeldata.n;
            Cg[j] 	= spectralwavemodeldata.Cg;
            k[j] 	= spectralwavemodeldata.wavenumber;
            
            // for all Spektralkomponents
            for (int fi=0; fi<N[j].getFrequencyLength(); fi++){
                // werte die fuer alle Richtungen Gleich sind
                double[] werte = N[j].getValueAt(fi,0);
                double sigma=2.*Math.PI * werte[0];
                double wavenumber=k[j].getValueAt(fi,0)[2];
                double c = sigma/wavenumber;
                double cg = Cg[j].getValueAt(fi,0)[2];
                
                for (int ai=0; ai<N[j].getDirectionLength(); ai++){
                    werte = N[j].getValueAt(fi,ai);
                    double theta = werte[1];
                    double cgx = cg * Math.cos(Math.PI * theta / 180.);
                    double cgy = cg * Math.sin(Math.PI * theta / 180.);
                    double kx = wavenumber * Math.cos(Math.PI * theta / 180.);
                    double ky = wavenumber * Math.sin(Math.PI * theta / 180.);
                    
                    double waction = werte[2];
                    
                    CgxNdx.setValueAt(fi, ai, CgxNdx.getValueAt(fi,ai)[2] + cgx * waction *  koeffmat[j][1]);
                    CgyNdy.setValueAt(fi, ai, CgyNdy.getValueAt(fi,ai)[2] + cgy * waction *  koeffmat[j][2]);
                    
                    dNdx.setValueAt(fi, ai, dNdx.getValueAt(fi,ai)[2] + waction *  koeffmat[j][1]);
                    dNdy.setValueAt(fi, ai, dNdy.getValueAt(fi,ai)[2] + waction *  koeffmat[j][2]);
                    
                    dkydx.setValueAt(fi, ai, dkydx.getValueAt(fi,ai)[2] + ky *  koeffmat[j][1]);
                    dkxdy.setValueAt(fi, ai, dkxdy.getValueAt(fi,ai)[2] + kx *  koeffmat[j][2]);
                }
            }
        }
        
        // teilweise trockenes Element
        if(iwatt!=0){
            for (int fi=0; fi<N[0].getFrequencyLength(); fi++)
                for (int ai=0; ai<N[0].getDirectionLength(); ai++){
                    dkydx.setValueAt(fi, ai, 0.);
                    dkxdy.setValueAt(fi, ai, 0.);
                }
            if(iwatt==1){
                // mal ueberlegen
            }
        }
        
        // Refraction Velocity
        for ( int j = 0; j < 3; j++) {
            Ctheta[j] = new DiscreteSpectrum2D(wavedat.frequencylength,wavedat.directionlength,
            wavedat.frequenzminimum,wavedat.frequenzmaximum,
            wavedat.directionminimum,wavedat.directionmaximum);
            // for all Spektralkomponents
            for (int ai=0; ai<Ctheta[j].getDirectionLength(); ai++){
                for (int fi=0; fi<Ctheta[j].getFrequencyLength(); fi++){
                    double rtheta = Math.PI * Ctheta[j].getValueAt(fi, ai)[1] / 180.; // theta in radian
                    Ctheta[j].setValueAt(fi, ai,
                    // depth refraction
                    Cg[j].getValueAt(fi, ai)[2]/k[j].getValueAt(fi, ai)[2] * (dkxdy.getValueAt(fi, ai)[2]-dkydx.getValueAt(fi, ai)[2])
                    // current refraction
                    + (dudx - dudy)*Math.sin(rtheta)*Math.cos(rtheta) + dvdx*Math.pow(Math.sin(rtheta),2) - dudy*Math.pow(Math.cos(rtheta),2)
                    // Diffraction !!
                    // + 0.015 * Math.sqrt( dEdx.getValueAt(fi,ai)[2]*dEdx.getValueAt(fi,ai)[2] + dEdy.getValueAt(fi,ai)[2]*dEdy.getValueAt(fi,ai)[2]) *
                    //		N[j].secondPartialDerivationDirection(fi,ai)
                    );
                }
            }
        }
        
        
        // Start -- compute the Element-Error for the Upwinding Step
        DiscreteSpectrum2D elementerror = new DiscreteSpectrum2D(wavedat.frequencylength,wavedat.directionlength,
        wavedat.frequenzminimum,wavedat.frequenzmaximum,
        wavedat.directionminimum,wavedat.directionmaximum);
        DiscreteSpectrum2D tau = new DiscreteSpectrum2D(wavedat.frequencylength,wavedat.directionlength,
        wavedat.frequenzminimum,wavedat.frequenzmaximum,
        wavedat.directionminimum,wavedat.directionmaximum);
        
        for ( int j = 0; j < 3; j++) {
            DOF dof = ele.getDOF(j);
            SpectralWaveModelData spectralwavemodeldata = SpectralWaveModelData.extract(dof);
            DiscreteSpectrum2D dndt=spectralwavemodeldata.dndt;
            // for all Spektralkomponents
            for (int ai=0; ai<N[j].getDirectionLength(); ai++){
                for (int fi=0; fi<N[j].getFrequencyLength(); fi++){
                    double[] werte = N[j].getValueAt(fi,ai);
                    double sigma = werte[0]*2.*Math.PI;
                    double we  = werte[2]*sigma;
                    
                    double cg  = Cg[j].getValueAt(fi, ai)[2];
                    double theta = werte[1];
                    double cgx = cg * Math.cos(Math.PI * theta / 180.);
                    double cgy = cg * Math.sin(Math.PI * theta / 180.);
                    
                    // upwinding Parameter
                    //tau.setValueAt(fi, ai, tau.getValueAt(fi, ai)[2] + 1./3. * 0.5 * ele.getVectorSize(cgx,cgy)/cg );
                    tau.setValueAt(fi, ai, tau.getValueAt(fi, ai)[2] + 1./3. * 0.5 * ele.getVectorSize(cgx+u[j],cgy+v[j])/Math.sqrt(Math.pow(cgx+u[j],2.)+Math.pow(cgy+v[j],2.)) );
                    //tau.setValueAt(fi, ai, tau.getValueAt(fi, ai)[2] + 1./3. * 0.5 * ele.getVectorSize(cgx,cgy)/Math.sqrt((Math.pow(cgx+u[j],2.)+Math.pow(cgy+v[j],2.)+1)) );
                    
                    if(we > 2.*WATT*WATT) {
                        timeStep = Math.min(timeStep, tau.getValueAt(fi, ai)[2]);
                        if (Ctheta[j].getValueAt(fi,ai)[2] > 0.01) {
                            timeStep = Math.min(timeStep, Math.PI/180.*Ctheta[j].getDirectionIncrement()/Ctheta[j].getValueAt(fi,ai)[2]);
                            //System.out.println(Ctheta[j].getValueAt(fi,ai)[2]);
                        }
                    }
                    
                    double eleerror = elementerror.getValueAt(fi, ai)[2] + 1./3. * (dndt.getValueAt(fi, ai)[2]
                                        // Propagation
                                            + CgxNdx.getValueAt(fi,ai)[2] + CgyNdy.getValueAt(fi,ai)[2]
                                        // Refraction
                                            + Ctheta[j].getValueAt(fi,ai)[2]*N[j].partialDerivationDirection(fi,ai)
                                            + N[j].getValueAt(fi,ai)[2]*Ctheta[j].partialDerivationDirection(fi,ai)
                                        // Energydissipation by wavebreaking
                                            + BREAKING/depth[j]*cg*Math.max(0.,we-spectralwavemodeldata.wavebreaking.getValueAt(fi,ai)[2])
                                        // EnergyInput by wind
                                            - spectralwavemodeldata.windinput.getValueAt(fi,ai)[2]
                                                                                    );
                    elementerror.setValueAt(fi, ai, eleerror);
                }
            }
        }
        // End -- compute the Element-Error for the Upwinding Step
        
        // Galerkin Step
        double vorfak;
        for (int j = 0; j < 3; j++) {
            DOF dof = ele.getDOF(j);
            int i = dof.number;
            SpectralWaveModelData spectralwavemodeldata = SpectralWaveModelData.extract(dof);
            DiscreteSpectrum2D re=spectralwavemodeldata.re;
            
            // for all Spektralkomponents
            for (int ai=0; ai<N[j].getDirectionLength(); ai++){
                for (int fi=0; fi<N[j].getFrequencyLength(); fi++){
                    double cg = Cg[j].getValueAt(fi, ai)[2];
                    double theta = N[j].getValueAt(fi,ai)[1];
                    double cgx = cg * Math.cos(Math.PI * theta / 180.);
                    double cgy = cg * Math.sin(Math.PI * theta / 180.);
                    
                    double res = re.getValueAt(fi,ai)[2]
                    // Upwinding
                    - tau.getValueAt(fi,ai)[2] * ( koeffmat[j][1]*cgx*elementerror.getValueAt(fi,ai)[2]
                                                 + koeffmat[j][2]*cgy*elementerror.getValueAt(fi,ai)[2]
                    )
                    // Propagation
                    - 1./3. * (CgxNdx.getValueAt(fi,ai)[2] + CgyNdy.getValueAt(fi,ai)[2])
                    ;
                    re.setValueAt( fi, ai, res );
                }
            }
            
            for (int l = 0; l < 3; l++){
                DOF dofl = ele.getDOF(l);
                int lg = dofl.number;
                SpectralWaveModelData spectralwavemodeldatal = SpectralWaveModelData.extract(dofl);
                if (l == j)
                    vorfak = 1. / 6.;
                else
                    vorfak = 1. / 12.;
                
                // for all Spektralkomponents
                for (int ai=0; ai<N[l].getDirectionLength(); ai++){
                    for (int fi=0; fi<N[l].getFrequencyLength(); fi++){
                        
                        double cg = Cg[l].getValueAt(fi, ai)[2];
                        
                        double res = re.getValueAt(fi,ai)[2] - vorfak * (
                        
                        // Refraction
                        + Ctheta[l].getValueAt(fi,ai)[2]*N[l].partialDerivationDirection(fi,ai)
                        + N[l].getValueAt(fi,ai)[2]*Ctheta[l].partialDerivationDirection(fi,ai)
                        
                        
                        // Energydissipation by wavebreaking
                        + BREAKING/depth[l]*cg*Math.max(0.,(N[l].getValueAt(fi,ai)[2]*(2.*Math.PI*N[l].getValueAt(fi,ai)[0]))
                        -spectralwavemodeldatal.wavebreaking.getValueAt(fi,ai)[2])
                        
                        
                        // EnergyInput by wind
                        - spectralwavemodeldatal.windinput.getValueAt(fi,ai)[2]
                        );
                        re.setValueAt( fi, ai, res );
                    }
                }
            }
        }
        return timeStep;
    }
    
    int GlobalIndex(int fi, int ai, int i){
        return (ai*wavedat.frequencylength + fi)*fenet.getNumberofDOFs()+i;
    }
    
    public final void readBoundCond(String name){
        InputStream is = null;
        try {
            is = new FileInputStream(name);        
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        
        StreamTokenizer st = new StreamTokenizer(r);
        st.eolIsSignificant(true);
        st.commentChar('C');
        } catch (Exception e) {}
    }
    
    double sinh(double x){
        return (Math.exp(x)-Math.exp(-x))/2.;
    }
    
    double sech(double x){
        return 2./(Math.exp(x)+Math.exp(-x));
    }
    
    double tanh(double x){
        return (Math.exp(x)-Math.exp(-x))/(Math.exp(x)+Math.exp(-x));
    }
    
    public Object getData(Point3d point){
        return new Object();
    }
    
    @Override
    public void setBoundaryCondition(DOF dof, double t){
        
        SpectralWaveModelData spectralwavemodeldata = SpectralWaveModelData.extract(dof);
            
        CurrentModel2DData currentdata = CurrentModel2DData.extract(dof);
            
        double z = dof.z;
            SedimentModel2DData sedimentdata = SedimentModel2DData.extract(dof);
        if (sedimentdata != null)
                z  = sedimentdata.z;
        
        double depth;
        if(currentdata!=null) 
            depth = Math.max(WATT,z+currentdata.eta);
        else 
            depth = Math.max(WATT,z);
        
        DiscreteSpectrum2D n=spectralwavemodeldata.n;
        DiscreteSpectrum2D dndt=spectralwavemodeldata.dndt;
        
        for (int ai=0; ai<n.getDirectionLength(); ai++){
            for (int fi=0; fi<n.getFrequencyLength(); fi++){
                
                double sigma = 2.*n.getValueAt(fi,ai)[0]*Math.PI;
                
                // set boundary in (x,y)-space
                if(spectralwavemodeldata.bc != null){
                    n.setValueAt( fi, ai, (spectralwavemodeldata.bc.getValue(n.getValueAt(fi,ai)[0],n.getValueAt(fi,ai)[1],t)
                    /sigma));
                    dndt.setValueAt( fi, ai, (spectralwavemodeldata.bc.getDifferential(n.getValueAt(fi,ai)[0],n.getValueAt(fi,ai)[1],t)
                    /sigma));
                }
                
                if(n.getValueAt(fi, ai)[2]<0.){
                    n.setValueAt( fi, ai, 0.);
                    dndt.setValueAt( fi, ai, 0.);
                } else
                    n.setValueAt( fi, ai, Math.min( n.getValueAt(fi, ai)[2], Math.pow(depth,2.)/sigma));
                
                // set boundary in frequence space
                if((fi==0)||(fi==n.getFrequencyLength()-1)){
                    n.setValueAt( fi, ai, 0.);
                    dndt.setValueAt( fi, ai, 0.);
                }
                
                // set boundary in angle space
                if(!n.fullRange() && ((ai==0)||(ai==n.getDirectionLength()-1))){
                    n.setValueAt( fi, ai, 0.);
                    dndt.setValueAt( fi, ai, 0.);
                }
            }
        }
    }
 
    // ----------------------------------------------------------------------
    // ToDO
    // ----------------------------------------------------------------------
    @Override
	public ModelData genData(FElement felement)
	{return null;}	

    /** genData generate the nessecery modeldatas for a DOF
     * @param dof
     * @return  */    
    @Override
    public ModelData genData(DOF dof){
        SpectralWaveModelData data = new SpectralWaveModelData(this);
        int dofnumber = (int) dof.number;
        
        Enumeration b = bcs.elements();
        while (b.hasMoreElements()) {
            WBoundaryCondition bcond = (WBoundaryCondition) b.nextElement();
            if ( dofnumber == bcond.pointnumber ){
                data.bc = bcond.function;
                bcs.removeElement(bcond);
            }
        }
        return data;
    }
    
    void generateClosedBoundCond(){
        double[] time =  {0., 1800., 3600.};
        Spectrum2D[] zerosp = {new ZeroDirectional(), new ZeroDirectional(), new ZeroDirectional()};
        TimeSpectrum2D timezerosp =  new TimeSpectrum2D(time,zerosp);
        
        SpectralWaveModelData spectralwavedata;
        
		for (int j=0; j<fenet.getNumberofFElements(); j++)
		{
            FTriangle tele = (FTriangle) fenet.getFElement(j);
            
            if (tele.getKennung() != 0){
                
                if (tele.getKennung() == FTriangle.bit_kante_jk) {
                    //	           			System.out.println("  "+bit_nr_kante_jk+" bit_kante_jk");
                    spectralwavedata = SpectralWaveModelData.extract(tele.getDOF(1));
                    spectralwavedata.bc = timezerosp;
                    spectralwavedata = SpectralWaveModelData.extract(tele.getDOF(2));
                    spectralwavedata.bc = timezerosp;
                }
                else if(tele.getKennung() == FTriangle.bit_kante_ki) {
                    //					System.out.println("  "+bit_nr_kante_ki+" bit_kante_ki");
                    spectralwavedata = SpectralWaveModelData.extract(tele.getDOF(0));
                    spectralwavedata.bc = timezerosp;
                    spectralwavedata = SpectralWaveModelData.extract(tele.getDOF(2));
                    spectralwavedata.bc = timezerosp;
                }
                else if(tele.getKennung() == FTriangle.bit_kante_ij) {
                    //		   			System.out.println("  "+bit_nr_kante_ij+" bit_kante_ij");
                    spectralwavedata = SpectralWaveModelData.extract(tele.getDOF(0));
                    spectralwavedata.bc = timezerosp;
                    spectralwavedata = SpectralWaveModelData.extract(tele.getDOF(1));
                    spectralwavedata.bc = timezerosp;
                }
                else if((tele.getKennung() == FTriangle.bit_kante_ijk) ||
                (tele.getKennung() == FTriangle.bit_kante_jki) ||
                (tele.getKennung() == FTriangle.bit_kante_kij) ||
                (tele.getKennung() == FTriangle.bit_kante_ijki) ) {
                    //					System.out.println("alle");
                    spectralwavedata = SpectralWaveModelData.extract(tele.getDOF(0));
                    spectralwavedata.bc = timezerosp;
                    spectralwavedata = SpectralWaveModelData.extract(tele.getDOF(1));
                    spectralwavedata.bc = timezerosp;
                    spectralwavedata = SpectralWaveModelData.extract(tele.getDOF(2));
                    spectralwavedata.bc = timezerosp;
                }
            }
        }
    }
    
    public void write_erg_xf( double[] erg, double t) {
        try {
            xf_os.writeFloat((float) t);
            for (DOF dof : fenet.getDOFs()) {
                int i = dof.number;
                SpectralWaveModelData spectralwavedata = SpectralWaveModelData.extract(dof);
                DiscreteSpectrum2D s=spectralwavedata.n;
                
                // update of DOF-values
                for (int ai=0; ai<s.getDirectionLength(); ai++)
                    for (int fi=0; fi<s.getFrequencyLength(); fi++)
                        s.setValueAt(fi,ai,erg[GlobalIndex( fi, ai, i)]);
                
                double[] werte = s.getExpectedDirection();
                if (s.getDiscreteMax()[2]>WATT){
                    xf_os.writeFloat((float)werte[0]);	// v1x
                    xf_os.writeFloat((float)werte[1]);	// v1y
                    xf_os.writeFloat((float)s.getEtotMean());	// skalar1
                } else {
                    xf_os.writeFloat(0.f);	// v1x
                    xf_os.writeFloat(0.f);	// v1y
                    xf_os.writeFloat(0.f);	// skalar1
                }
                
                double[] w = s.getDiscreteMaxVector();
                if (w[2]>WATT){
                    xf_os.writeFloat((float)w[2]);	// skalar2
                    xf_os.writeFloat((float)w[0]);	// v2x
                    xf_os.writeFloat((float)w[1]);	// v2y
                } else {
                    xf_os.writeFloat(0.f);	// v1x
                    xf_os.writeFloat(0.f);	// v1y
                    xf_os.writeFloat(0.f);	// skalar1
                }
            }
        } catch (Exception e) {}
    }
    
    public final void write_specs_head() {
        try {
            sp_os.writeInt(fenet.getNumberofDOFs());
			DOF[] dofs = fenet.getDOFs();
			for (int j=0; j<dofs.length;j++)
				write_DOF(dofs[j], sp_os);
            sp_os.writeInt(fenet.getNumberofFElements());
			FElement[] felements = fenet.getFElements();
			for (int j=0; j<felements.length;j++)
			{
                FTriangle tr = (FTriangle)felements[j];
                //TicadIO.write_Element(, sp_os);
                for(int i=0; i<3; i++)
//                    sp_os.writeInt(fenet.getIndexOfPoint3d(tr.getNode(i)));
                    sp_os.writeInt(tr.getDOF(i).number);
            }
        } catch (Exception i) {}
    }
    
    public void write_specs(double t) {
        try {
            sp_os.writeFloat((float) t);
			DOF[] dofs = fenet.getDOFs();
			for (int j=0; j<dofs.length;j++){
				DOF dof=dofs[j];
                SpectralWaveModelData spectralwavedata = SpectralWaveModelData.extract(dof);
                write_DisSpec(spectralwavedata.n, sp_os);
            }
        } catch (Exception e) {}
    }
    
    public void setWatt(double w) {
        WATT = w;
    }
    
    public final int[][] readWaveBoundaryNodes(String filename) {
        int [][] back = null;
        Vector v;
        
        try {
        
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
        
        StreamTokenizer st = new StreamTokenizer(r);
        st.eolIsSignificant(false);
        st.commentChar('C');
        
        // Read Node information
        
            v = new Vector();
            
            while( (st.nextToken() != StreamTokenizer.TT_EOF)){
                st.pushBack();
                int node[] = new int [2];
                node[0] = TicadIO.NextInt(st);
                node[1] = TicadIO.NextInt(st);
                v.addElement(node);
            }
            back = new int[2][v.size()];
            int z = 0;
            for (Enumeration en=v.elements();en.hasMoreElements();) {
                int[] f = (int[]) en.nextElement();
                back[0][z]=f[0];
                back[1][z++]=f[1];
            }
            System.out.println("Wave Boundary Nodes!");
        } catch (Exception e){}
        return back;
    }
    
    
    
    public TimeVectorFunktion2D readBSH(String filename) {
        
        Vector<BSHDat> bshFct = new Vector<>();
        
        Vector<DOF> DOFs = new Vector<>();
        FileInputStream is = null;
        int number;
        double x, y, z;
        boolean readnodes = true;
        Date firstDate = null;
        long firstTime = 0;
        double time;
        long startTime = 0;
        
        int date, tmpdate=-100;
        
        try {
            is = new FileInputStream(filename);
        } catch (Exception e) {}
        
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        
        StreamTokenizer st = new StreamTokenizer(r);
        st.eolIsSignificant(false);
        st.commentChar('C');
        
        // Read Node information
        try {
            startTime = TicadIO.NextLong(st);
            while( (st.nextToken() != StreamTokenizer.TT_EOF) && (readnodes==true)){
                st.pushBack();
                number = TicadIO.NextInt(st);
                
                if (number != -1){
                    x = TicadIO.NextDouble(st);
                    y = TicadIO.NextDouble(st);
                    z = TicadIO.NextDouble(st);
                    DOFs.addElement(new DOF(number,x,y,z));
                    //			System.out.println(number+"  "+x+"  "+y+"  "+z);
                } else
                    readnodes=false;
            }
            st.pushBack();
            
            BSHDat bshDat=null;
            double[] values;
            
            try {
                while( st.nextToken() != StreamTokenizer.TT_EOF ){
                    st.pushBack();
                    
                    number = TicadIO.NextInt(st);
                    date =  TicadIO.NextInt(st);
                    if (tmpdate != date){
                        if (bshDat!=null) bshFct.add(bshDat);
                        tmpdate = date;
                        bshDat = new BSHDat(date);
                    }
                    
                    values = new double[8];
                    // wind
                    values[0]  = TicadIO.NextDouble(st);
                    values[1]  = TicadIO.NextDouble(st);
                    // windwaves
                    values[2]  = TicadIO.NextDouble(st);
                    values[3]  = TicadIO.NextDouble(st);
                    values[4]  = TicadIO.NextDouble(st);
                    // swell
                    values[5]  = TicadIO.NextDouble(st);
                    values[6]  = TicadIO.NextDouble(st);
                    values[7]  = TicadIO.NextDouble(st);
                    // convert to MathDirection
                    values[1]  = Utils.met2math(values[1]);
                    values[4]  = Utils.met2math(values[4]);
                    values[7]  = Utils.met2math(values[7]);
                    
                    //			System.out.println(date+"\t"+values[1]+"\t"+values[7]+"\t");
                    
                    DOF dof;
                    Point2D.Double point2d=null;
                    Enumeration _dofs = DOFs.elements();
                    while (_dofs.hasMoreElements()) {
                        dof = (DOF) _dofs.nextElement();
                        if (number == dof.number)
                            point2d= new Point2D.Double(dof.x,dof.y);
                    }
                    
                    bshDat.values.add(new BSHPointValue(point2d,values));
                    
/*			// wind
                        double windspeed = TicadIO.NextDouble(st);
                        double winddir = TicadIO.NextDouble(st);
                        // windwaves
                        double wavehs = TicadIO.NextDouble(st);
                        double wavetp = TicadIO.NextDouble(st);
                        double wavedir = TicadIO.NextDouble(st);
                        // swell
                        double swellhs = TicadIO.NextDouble(st);
                        double swelltp = TicadIO.NextDouble(st);
                        double swelldir = TicadIO.NextDouble(st);
 
 
//			System.out.println(date+"\t"+windspeed+"\t"+swelldir+"\t");
 
                        // convert to MathDirection
                        winddir = convMathDirection(winddir);
                        wavedir = convMathDirection(wavedir);
                        swelldir = convMathDirection(swelldir);
 
//			System.out.println(date+"\t"+windspeed+"\t"+swelldir+"\t");
 
                        // date converting
                        int tmpdate = date;
                        int second = 0;
                        int minute = 0;
 
                        int hour = tmpdate%100;
                        tmpdate /= 100;
 
                        int day = tmpdate%100;
                        tmpdate /= 100;
 
                        int month = tmpdate%100;
                        tmpdate /= 100;
 
                        int year = tmpdate;
 
//			System.out.println(year+"\t"+month+"\t"+day+"\t"+hour);
 
                        Calendar cal = Calendar.getInstance();
                        cal.clear();
                        cal.set(year, month-1, day, hour, minute, second);
                        Date jdate = cal.getTime();
 
                        if(firstDate == null){
                                firstDate = cal.getTime();
                                firstTime = firstDate.getTime()/1000;
                        }
 
                        time = (double) jdate.getTime()/1000 - firstTime + startTime;
 
//			System.out.println(time);
 */	    	}
                if (bshDat!=null) bshFct.add(bshDat);
            } catch (Exception e){}
            
            
            
        } catch (Exception e){}
        
        
        int count = bshFct.size();
        //	System.out.println("bshFct.size()="+bshFct.size());
        int vcount = 8;// ((BSHPointValue)bshd.values.elementAt(0)).value.length;
        
        VectorFunction2d[] vf2d = new VectorFunction2d[count];
        double[] zeiten = new double[count];
        
        
        int t=0;
        for (Enumeration e = bshFct.elements();e.hasMoreElements();) {
            BSHDat bshDat = (BSHDat) e.nextElement();
            //		System.out.println("bshDat="+bshDat);
            //
            //	zeiten[t] = getTime(bshDat.date, firstDate, startTime );
            date=bshDat.date;
            // date converting
            tmpdate = date;
            int second = 0;
            int minute = 0;
            
            int hour = tmpdate%100;
            tmpdate /= 100;
            
            int day = tmpdate%100;
            tmpdate /= 100;
            
            int month = tmpdate%100;
            tmpdate /= 100;
            
            int year = tmpdate;
            
            //			System.out.println(year+"\t"+month+"\t"+day+"\t"+hour);
            
            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(year, month-1, day, hour, minute, second);
            Date jdate = cal.getTime();
            
            if(firstDate == null){
                firstDate = cal.getTime();
                firstTime = firstDate.getTime()/1000;
            }
            
            zeiten[t]= (double) jdate.getTime()/1000 - firstTime + startTime;
            //
            int numberofpoints = bshDat.values.size();
            double[][] feld = new double[numberofpoints][];
            Point2D.Double[] punkte = new Point2D.Double[numberofpoints];
            
            int it=0;
            for (Enumeration ee = bshDat.values.elements();ee.hasMoreElements();) {
                BSHPointValue bshpv = (BSHPointValue) ee.nextElement();
                punkte[it]=bshpv.p;
                feld[it]=bshpv.value;
                it++;
            }
            
            vf2d[t] = new ShepardVectorFunction2D(punkte,feld);
            t++;
        }
        
        System.out.println("BSHDaten Gelesen");
        return new TimeVectorFunktion2D( zeiten , vf2d );
    }
    
    public void write_DisSpec(DiscreteSpectrum2D n, DataOutputStream dos) {
        try {
            dos.writeInt(n.getFrequencyLength());
            dos.writeInt(n.getDirectionLength());
            dos.writeDouble(n.getFrequencyMin());
            dos.writeDouble(n.getFrequencyMax());
            dos.writeDouble(n.getDirectionMin());
            dos.writeDouble(n.getDirectionMax());
            for (int i=0; i<n.getFrequencyLength(); i++)
                for (int j=0; j<n.getDirectionLength(); j++)
                    dos.writeFloat((float) n.getValueAt(i, j)[2]);
        } catch (Exception e) {}
    }
    
    public DiscreteSpectrum2D read_DisSpec(DataInputStream dis) {
        DiscreteSpectrum2D D;
        try {
            int fl = dis.readInt();
            int dl = dis.readInt();
            System.out.println("fl: "+fl+"   dl: "+dl);
            D = new DiscreteSpectrum2D(fl , dl, dis.readDouble(),
            dis.readDouble(), dis.readDouble(), dis.readDouble());
            for (int i=0; i < fl; i++) {
                double f = 0.;
                for (int j=0; j < dl; j++) {
                    f = (double)dis.readFloat();
                    D.setValueAt(i,j, f);
                    System.out.println(j);
                }
                System.out.println(i);
            }
            return D;
        } catch (Exception e) {}
        return null;
    }
    
    public static void write_DOF(DOF dof, DataOutputStream dos) {
        try {
            dos.writeInt(dof.number);
            dos.writeDouble(dof.x);
            dos.writeDouble(dof.y);
            dos.writeDouble(dof.z);
        } catch (Exception e) {}
    }  
}

class WBoundaryCondition{
    int pointnumber;
    TimeSpectrum2D function;
    
    WBoundaryCondition( int nr, TimeSpectrum2D fkt ){
        pointnumber = nr;
        function = fkt;
    }
}

class BSHPointValue{
    Point2D.Double p;
    double[] value;
    BSHPointValue(Point2D.Double point,double[] v){
        p = point;
        value = v;
    }
}

class BSHDat{
    int date;
    Vector<BSHPointValue> values = new Vector<>();
    BSHDat(int date){
        this.date=date;
    }
}


