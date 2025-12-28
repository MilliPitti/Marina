package bijava.graphics;

import bijava.math.ifunction.ScalarFunction1d;
import de.smile.math.Function;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.dm.SGTMetaData;
import gov.noaa.pmel.sgt.dm.SimpleLine;
import gov.noaa.pmel.sgt.swing.JPlotLayout;

import java.awt.Color;
import javax.swing.JFrame;

/**
 * FunctionPlotter.java stellt Eigenschaften und Methoden zum Plotten
 * von eindimensionalen skalaren Funktionen mit SGT zur Verfuegung.
 * @author Peter Milbradt
 */
public class FunctionPlotter {
    private final JPlotLayout layout; // Diagramm
    private final SGTMetaData metaX, metaY; // Achsenbeschriftung
    
    /**
     * Erzeugt ein Objekt zum Plotten von eindimensionalen skalaren
     * Funktionen mit SGT.
     * @param xchar Kuerzel der x-Achsenbeschriftung.
     * @param x Bezeichnung der x-Achsenbeschriftung.
     * @param ychar Kuerzel der fx-Achsenbeschriftung.
     * @param y Bezeichnung der fx-Achsenbeschriftung.
     */
    public FunctionPlotter(String xchar, String x, String ychar, String y) {
        // Diagramm erzeugen und Titel setzen
        layout = new JPlotLayout(false, false, false, null, null, false);
        layout.setTitles("Diagramm", "", "");
        // Achsenbeschriftungen setzen
        metaX = new SGTMetaData(xchar, x, false, false);
        metaY = new SGTMetaData(ychar, y, false, false);
    }
     public void setTitles(String titel){
         layout.setTitles(titel, "", "");
     }
    
    /**
     * Fuegt eine eindimensionale skalare Funktion hinzu.
     * @param xloc x-Koordinaten der Funktion.
     * @param yloc fx-Koordinaten der Funktion.
     * @param name Bezeichnung der Funktion.
     * @param line gibt an ob sowohl Linie und Marke (1) oder nur Marke (0) gezeichnet werden soll
     * @param mark Index der Marke vergleiche gov.noaa.pmel.sgt.PlotMark
     * @param c Farbe in der die Funktionkurve ausgegeben wird
     */
    public void addFunction(double[] xloc, double[] yloc, String name, int line, int mark, Color c) {
        // Kurve erzeugen
        LineAttribute la;
        if(line == 1)
            la = new LineAttribute(LineAttribute.MARK_LINE);
        else
            la = new LineAttribute(LineAttribute.MARK);
        la.setMark(mark);
        la.setColor(c);

        SimpleLine data = new SimpleLine(xloc, yloc, name);
        data.setXMetaData(metaX);
        data.setYMetaData(metaY);
        // Kurve zum Diagramm hinzufuegen
        
        layout.addData(data,la,data.getTitle());

    }



        public void addFunction(double[] xloc, double[] yloc, String name) {
        // Kurve erzeugen

        SimpleLine data = new SimpleLine(xloc, yloc, name);
        data.setXMetaData(metaX);
        data.setYMetaData(metaY);
        // Kurve zum Diagramm hinzufuegen

        layout.addData(data,data.getTitle());


    }
    
    /**
     * Fuegt eine eindimensionale skalare Funktion hinzu.
     * @param fctn eindimensionale skalare Funktion.
     * @param xa x-Koordinate der ersten Stuetzstelle.
     * @param dx Schrittweite in x-Richtung.
     * @param n Anzahl Stuetzstellen.
     * @param name Bezeichnung der Funktion.
     */
    public void addFunction(ScalarFunction1d fctn, double xa, double dx, int n, String name) {
        // Felder fuer Funktionswerte erzeugen
        double[] xloc = new double[n];
        double[] yloc = new double[n];
        for (int i = 0; i < n; i++) {
            xloc[i] = xa + i * dx;
            yloc[i] = fctn.getValue(xloc[i]);
        }
        // Kurve erzeugen
        this.addFunction(xloc, yloc, name);

    }
    
    /**
     * Erzeugt ein Fenster, in welchem das vorhandene Diagramm
     * dargestellt wird.
     * @param title Fenstertitel.
     * @param width Fensterbreite.
     * @param height Fensterhoehe.
     */
    public void plot(String title, int width, int height) {
        // Fenster erzeugen und Abmessungen setzen
        JFrame fr = new JFrame(title);
        fr.setSize(width, height);
        // Diagramm hinzufuegen
        fr.getContentPane().add(layout);
        // Fenster auf dem Bildschirm sichbar machen
        fr.setVisible(true);
        // Fensterkreuz zum Schliessen des Fensters und Beenden des Programms benutzen
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    public static void main(String... args) {
        
        double dx = 0.01E-3;
        int n = 2000000;
        
        double[] x = new double[n];
        double[] fx = new double[n];
        for (int i = 1; i < n; i++) {
            x[i] = dx + i * dx;
            fx[i] = Function.coth(x[i]) - 1.0 / x[i];
        }

        FunctionPlotter fp = new FunctionPlotter("x", "", "f(x) = coth(x) - 1.0 / x", "");
        fp.addFunction(x, fx, "f(x) = coth(x) - 1.0 / x");
        fp.plot("f(x) = coth(x) - 1.0 / x", 640, 480);
        
    }
}