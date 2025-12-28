package de.smile.marina.fem;

/** Interface welches sicherstellt, dass ein Modell einer Ergebnisdatei im syserg.bin format schreiben kann
 *
 * @author milbradt
 */
public interface TicadModel {
    public void write_erg_xf( double[] erg, double t);
    public int getTicadErgMask();
}
