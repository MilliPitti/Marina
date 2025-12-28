package bijava.marina.spectra;

public abstract class DirectionalSpectrum implements Spectrum2D {

    Spectrum1D		    spectrum;
    DirectionalSpreadingFkt spread_fkt;
    ParameterFkt	    para_fkt;
    
    double		    theta_mean;
}