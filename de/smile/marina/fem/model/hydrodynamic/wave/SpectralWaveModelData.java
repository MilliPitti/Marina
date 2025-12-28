package de.smile.marina.fem.model.hydrodynamic.wave;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.*;
import bijava.marina.spectra.*;

//Parant-Class 
public class SpectralWaveModelData implements ModelData{
    
    private static int id = NO_MODEL_DATA;
    // zustandsgroessen
    DiscreteSpectrum2D n;
    DiscreteSpectrum2D dndt;
    
    DiscreteSpectrum2D wavenumber;
    DiscreteSpectrum2D Cg;
    
    DiscreteSpectrum2D wavebreaking;
    DiscreteSpectrum2D windinput;
    
    // ergebnisvector
    DiscreteSpectrum2D re;
    
    // boudary conditions
    TimeSpectrum2D bc=null;
    
    // mean Radiationstresses
    double sxx, sxy, syy;
    
    
    SpectralWaveModelData (SpectralWaveModel m){
        
        id = SEARCH_MODEL_DATA;
        
	n   = new DiscreteSpectrum2D( m.wavedat.frequencylength,m.wavedat.directionlength,
					m.wavedat.frequenzminimum,m.wavedat.frequenzmaximum,
					m.wavedat.directionminimum,m.wavedat.directionmaximum);
									
	dndt= new DiscreteSpectrum2D(m.wavedat.frequencylength,m.wavedat.directionlength,
					m.wavedat.frequenzminimum,m.wavedat.frequenzmaximum,
					m.wavedat.directionminimum,m.wavedat.directionmaximum);
					
	wavenumber= new DiscreteSpectrum2D(m.wavedat.frequencylength,m.wavedat.directionlength,
					m.wavedat.frequenzminimum,m.wavedat.frequenzmaximum,
					m.wavedat.directionminimum,m.wavedat.directionmaximum);
	Cg= new DiscreteSpectrum2D(m.wavedat.frequencylength,m.wavedat.directionlength,
					m.wavedat.frequenzminimum,m.wavedat.frequenzmaximum,
					m.wavedat.directionminimum,m.wavedat.directionmaximum);
					
	wavebreaking= new DiscreteSpectrum2D(m.wavedat.frequencylength,m.wavedat.directionlength,
					m.wavedat.frequenzminimum,m.wavedat.frequenzmaximum,
					m.wavedat.directionminimum,m.wavedat.directionmaximum);
	windinput= new DiscreteSpectrum2D(m.wavedat.frequencylength,m.wavedat.directionlength,
					m.wavedat.frequenzminimum,m.wavedat.frequenzmaximum,
					m.wavedat.directionminimum,m.wavedat.directionmaximum);
					
	re  = new DiscreteSpectrum2D(m.wavedat.frequencylength,m.wavedat.directionlength,
					m.wavedat.frequenzminimum,m.wavedat.frequenzmaximum,
					m.wavedat.directionminimum,m.wavedat.directionmaximum);
    }
    /** extrahiert die SpectralWaveModelData aus dem DOF */
    public static SpectralWaveModelData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof SpectralWaveModelData) {
                    id = dof.getIndexOf(md);
                    return (SpectralWaveModelData) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (SpectralWaveModelData) dof.getModelData(id);
        }
        return null;
    }
}
