package de.smile.marina.io;

import bijava.math.ifunction.DiscretScalarFunction1d;
import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.model.hydrodynamic.BoundaryCondition;
import de.smile.marina.fem.model.hydrodynamic.dim2.H_Q_Steuerung;
import de.smile.marina.fem.model.hydrodynamic.dim2.QSteuerung;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 *
 * @author milbradt
 * @version 3.15.1
 */
public class RndwerteBAWReader implements BoundaryConditionsReader {

    // Kennzahlen fuer Randbedingungsidentifikation
    public final static byte qx_GESETZT = 1; // m^3/s/m
    public final static byte qy_GESETZT = 2; // m^3/s/m
    public final static byte H_GESETZT = 3;
    public final static byte SALT_GESETZT = 4;
    public final static byte TEMPERATURE_GESETZT = 6;
    public final static byte CONCENTRATION_GESETZT = 8; // g/l oder kg/m^3
    public final static byte BOTTOM_GESETZT = 10; // m unter NN
    public final static byte D50_GESETZT = 11; // mm
    public final static byte U_GESETZT = 12;  // depth integrated velocity in x-direction [m/s]
    public final static byte V_GESETZT = 13;  // depth integrated velocity in y-direction [m/s]
    public final static byte Qx_GESETZT = 14;
    public final static byte Qy_GESETZT = 15;
    public final static byte QxygenConcentration = 16; // mg/l
    
    public final static byte Q_Source = 20;  // m^3/s
    public final static byte H_Source = 21;  // m/m^2/s
    
    public final static byte SALT_Source = 24; // g/s
    
    public static final int wellenperiode = 30; // s
    public static final int wellenhoehe = 31;   // m
    public static final int wellenrichtung = 32;// meteorologische Richtung?
        
    private boolean qx_FLAG = false;
    private boolean qy_FLAG = false;
    private boolean H_FLAG = false;
    private boolean SALT_FLAG = false;
    private boolean TEMPERATURE_FLAG = false;
    private boolean BOTTOM_FLAG = false;
    private boolean D50_FLAG = false;
    private boolean CONCENTRATION_FLAG = false;
    private boolean U_FLAG = false;
    private boolean V_FLAG = false;
    private boolean Qx_FLAG = false;
    private boolean Qy_FLAG = false;
    private boolean Qxygen_FLAG = false;
    private boolean Point_Q_FLAG = false;
    private boolean Point_H_FLAG = false;
    private boolean Point_SALT_FLAG = false;
    private final String filename;
    private final FEDecomposition fedecomposition;
    
    // Art der Funktion der Randwertebeschreibung Art_Rndfkt
    //  0 konstant (nicht implementiert)
    //  1 Summe aus mehreren Cosinus-Funktionen  (nicht implementiert)
    //  2 stueckweise lineare
    //  3 kubische Spline-Interpolation zwischen nicht Aequidistanten Stuetzstellen (nicht implementiert)
    //  4 stueckweise lineare Interpolation bei der H-Q-gesteurten Randbedingung
    //  5 stueckweise lineare Interpolation bei der H-Qmax-gesteurten Randbedingung

    /** Creates a new instance of RndwerteBAWReader
     * @param name
     * @param fedecomp */
    public RndwerteBAWReader(String name, FEDecomposition fedecomp) {
        filename = name;
        fedecomposition = fedecomp;
    }

    @Override
    public BoundaryCondition[] readBoundaryConditions(String[] boundary_condition_key_mask) throws Exception {
        ArrayList<BoundaryCondition> bc = new ArrayList<>();

        //Flags false setzen  
        qx_FLAG = false;
        qy_FLAG = false;
        H_FLAG = false;
        SALT_FLAG = false;
        TEMPERATURE_FLAG = false;
        BOTTOM_FLAG = false;
        D50_FLAG = false;
        CONCENTRATION_FLAG = false;
        U_FLAG = false;
        V_FLAG = false;
        Qx_FLAG = false;
        Qy_FLAG = false;
        Qxygen_FLAG = false;
        Point_Q_FLAG = false;
        Point_H_FLAG = false;
        Point_SALT_FLAG = false;

        for (String boundary_condition_key : boundary_condition_key_mask) {
            //todo evtl HashMap verwenden
            if (boundary_condition_key.equals(BoundaryCondition.specific_flowrate_x)) {
                qx_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.specific_flowrate_y)) {
                qy_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.free_surface)) {
                H_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.concentration_salt)) {
                SALT_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.water_temperature)) {
                TEMPERATURE_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.bottom)) {
                BOTTOM_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.d50)) {
                D50_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.concentration_sediment)) {
                CONCENTRATION_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.velocity_u)) {
                U_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.velocity_v)) {
                V_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.absolute_flowrate_x)) {
                Qx_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.absolute_flowrate_y)) {
                Qy_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.concentration_oxygen)){
                Qxygen_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.pointbased_Q_source)) {
                Point_Q_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.pointbased_h_source)) {
                Point_H_FLAG = true;
            }
            if (boundary_condition_key.equals(BoundaryCondition.pointbased_salt_source)) {
                Point_SALT_FLAG = true;
            }
        }

        //Implementierung der Einleselogik fuer das BAW-Format

        System.out.println("\tReading BoundaryCondition-File (in BAW-Format): " + filename);
        DataLine dataLine;
        StringTokenizer stringTokenizer;
        int countToken;
        int anzrndwrt = 0;
        int lineNumber = 0;

        int pointer = 0;
        int anz_identische_Knoten = 0, i1 = 0, K;
        int Art_Rndwrt = 0, Art_Rndfkt = 2, Gesamtzahl = 0, Anz_Periode = 0;
        double Translation=0., Spreizung, Ordinaten=1.;

        double feld[][];
        int KnotenNr[];

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filename))){

            //Anzahl der Randknoten einlesen
            dataLine = readNextDataLine(bufferedReader, lineNumber);
            lineNumber = dataLine.nr;

            stringTokenizer = new StringTokenizer(dataLine.line);
            countToken = stringTokenizer.countTokens();
            if (countToken != 1) {
                System.out.println("Error in line " + lineNumber + "!");
                //System.out.println("Fehler in Zeile "+ lineNumber +"!");
                System.out.println("Wrong number of arguments in specification the number of nodes for boundary condition timeseries. ");
                //System.out.println("Falsche Anzahl Argumente bei der Angabe der Knotenanzahl fuer Randwertzeitreihen.");
                System.out.println("> " + dataLine.line);
                throw new Exception("Error in line " + lineNumber + "!\n"
                        + "Wrong number of arguments in specification the number of nodes for boundary condition timeseries.\n"
                        + "> " + dataLine.line);
            }
            anzrndwrt = 0;
            try {
                anzrndwrt = Integer.parseInt(stringTokenizer.nextToken());
            } catch (NumberFormatException e) {
                System.out.println("Error in line " + lineNumber + "!");
                //System.out.println("Fehler in Zeile "+ lineNumber +"!");
                System.out.println("Wrong format in specification the number of nodes for boundary condition timeseries (Integer format expected)  !");
                //System.out.println("Fehlerhaftes Format bei der Angabe der Knotenanzahl fuer Randwertzeitreihen (Integerformat erwartet) !");
                System.out.println("> " + dataLine.line);
                throw new Exception();
            }

            KnotenNr = new int[anzrndwrt];

            //Zeitintervall einlesen
            dataLine = readNextDataLine(bufferedReader, lineNumber);
            lineNumber = dataLine.nr;

            stringTokenizer = new StringTokenizer(dataLine.line);
            countToken = stringTokenizer.countTokens();
            if (countToken != 1) {
                System.out.println("Error in line " + lineNumber + "!");
                System.out.println("Falsche Anzahl Argumente bei der Definition des Zeitintervalls.");
                System.out.println("> " + dataLine.line);
                System.exit(0);
            }

            try {
                Double.parseDouble(stringTokenizer.nextToken());
            } catch (NumberFormatException e) {
                System.out.println("Fehler in Zeile " + lineNumber + "!");
                System.out.println("Fehlerhaftes Format bei der Angabe des Zeitintervalls fuer Randwertzeitreihen (Integer- oder Doubleformat erwartet) !");
                System.out.println("> " + dataLine.line);
                System.exit(0);
            }

            while (pointer < anzrndwrt) {
                //Anzahl der identische Knoten einlesen
                dataLine = readNextDataLine(bufferedReader, lineNumber);
                lineNumber = dataLine.nr;

                stringTokenizer = new StringTokenizer(dataLine.line);
                countToken = stringTokenizer.countTokens();
                if (countToken != 8) {
                    System.out.println("Fehler in Zeile " + lineNumber + "!");
                    System.out.println("Falsche Anzahl Argumente bei der Spezifikation der Randbedingung.");
                    System.out.println("> " + dataLine.line);
                    System.exit(0);
                }

                try {
                    anz_identische_Knoten = Integer.parseInt(stringTokenizer.nextToken());
                } catch (NumberFormatException e) {
                    System.out.println("Fehler in Zeile " + lineNumber + "!");
                    System.out.println("Fehlerhaftes Format bei der Angabe der Anzahl der Knoten mit identischen Randbedingungen (Integerformat erwartet) !");
                    System.out.println("> " + dataLine.line);
                    System.exit(0);
                }
                try {
                    Art_Rndwrt = Integer.parseInt(stringTokenizer.nextToken());
                } catch (NumberFormatException e) {
                    System.out.println("Fehler in Zeile " + lineNumber + "!");
                    System.out.println("Fehlerhaftes Format bei der Art der Randbedingung (Integerformat erwartet) !");
                    System.out.println("> " + dataLine.line);
                    System.exit(0);
                }
                try {
                    Art_Rndfkt = Integer.parseInt(stringTokenizer.nextToken());
                } catch (NumberFormatException e) {
                    System.out.println("Fehler in Zeile " + lineNumber + "!");
                    System.out.println("Fehlerhaftes Format bei der Angabe des Interpolationstyps (Integerformat erwartet) !");
                    System.out.println("> " + dataLine.line);
                    System.exit(0);
                }
                if (Art_Rndfkt != 2 && (Art_Rndwrt!=14 || Art_Rndwrt!=15 )) {
                    System.out.println("Warnung in Zeile " + lineNumber + "!");
                    System.out.println("Warnung: Falscher Interpolationstyp (lineare Interpolation (ITYP=2) erwartet) !");
                    System.out.println("> " + dataLine.line);

                }
                try {
                    Anz_Periode = Integer.parseInt(stringTokenizer.nextToken());
                } catch (NumberFormatException e) {
                    System.out.println("Fehler in Zeile " + lineNumber + "!");
                    System.out.println("Fehlerhaftes Format bei der Angabe der spezifizierten Intervalle (Integerformat erwartet) !");
                    System.out.println("> " + dataLine.line);
                    System.exit(0);
                }
                try {
                    Gesamtzahl = Integer.parseInt(stringTokenizer.nextToken());
                } catch (NumberFormatException e) {
                    System.out.println("Fehler in Zeile " + lineNumber + "!");
                    System.out.println("Fehlerhaftes Format bei der Angabe der zu erzeugenden Intervalle (Integerformat erwartet) !");
                    System.out.println("> " + dataLine.line);
                    System.exit(0);
                }
                try {
                    Translation = Double.parseDouble(stringTokenizer.nextToken());
                } catch (NumberFormatException e) {
                    System.out.println("Fehler in Zeile " + lineNumber + "!");
                    System.out.println("Fehlerhaftes Format bei der Angabe von SUMMA !");
                    System.out.println("> " + dataLine.line);
                    System.exit(0);
                }
                try {
                    Spreizung = Double.parseDouble(stringTokenizer.nextToken());
                } catch (NumberFormatException e) {
                    System.out.println("Fehler in Zeile " + lineNumber + "!");
                    System.out.println("Fehlerhaftes Format bei der Angabe von FAKTZ !");
                    System.out.println("> " + dataLine.line);
                    System.exit(0);
                }
                try {
                    Ordinaten = Double.parseDouble(stringTokenizer.nextToken());
                } catch (NumberFormatException e) {
                    System.out.println("Fehler in Zeile " + lineNumber + "!");
                    System.out.println("Fehlerhaftes Format bei der Angabe von FAKTH !");
                    System.out.println("> " + dataLine.line);
                    System.exit(0);
                }

                int ip = 0;
                do {
                    //identische Randknotennummern einlesen
                    dataLine = readNextDataLine(bufferedReader, lineNumber);
                    lineNumber = dataLine.nr;

                    stringTokenizer = new StringTokenizer(dataLine.line);
                    countToken = stringTokenizer.countTokens();
                    if (ip + countToken > anz_identische_Knoten) {
//                    if (countToken!=anz_identische_Knoten) {
                        System.out.println("Fehler in Zeile " + lineNumber + "!");
                        System.out.println("Anzahl der Knoten mit identischen Randbedingungen stimmt nicht mit Vorgabe ueberein!");
                        System.out.println("> " + dataLine.line);
                        System.exit(0);
                    }
                    //Auslesen der Knoten aus StringTokenizer
                    for (K = pointer + ip; K < pointer + ip + countToken; K++) {
                        try {
                            i1 = Integer.parseInt(stringTokenizer.nextToken());
                        } catch (NumberFormatException e) {
                            System.out.println("Fehler in Zeile " + lineNumber + "!");
                            System.out.println("Knotennummer nicht im Integerformat!");
                            System.out.println("> " + dataLine.line);
                            System.exit(0);
                        }
                        KnotenNr[K] = i1;
                    }
                    ip += countToken;
                } while (ip < anz_identische_Knoten);


                feld = new double[2][Anz_Periode + 1];

                for (K = 0; K <= Anz_Periode; K++) {

                    //hole naechste Zeitreihenzeile
                    dataLine = readNextDataLine(bufferedReader, lineNumber);
                    lineNumber = dataLine.nr;

                    stringTokenizer = new StringTokenizer(dataLine.line);
                    countToken = stringTokenizer.countTokens();
                    if (countToken != 2) {
                        System.out.println("Fehler in Zeile " + lineNumber + "!");
                        System.out.println("Falsche Zeitreiheninformation ! ");
                        System.out.println("> " + dataLine.line);
                        System.exit(0);
                    }
                    try {
                        feld[0][K] = Double.parseDouble(stringTokenizer.nextToken());
                    } catch (NumberFormatException e) {
                        System.out.println("Fehler in Zeile " + lineNumber + "!");
                        System.out.println("Fehlerhaftes Format bei der Angabe der Zeitreiheninformation !");
                        System.out.println("> " + dataLine.line);
                        System.exit(0);
                    }
                    try {
                        feld[1][K] = Double.parseDouble(stringTokenizer.nextToken());
                        if (Art_Rndwrt == D50_GESETZT) {
                            feld[1][K] /= 1000.;
                        }
                        if (Art_Rndwrt == CONCENTRATION_GESETZT) {
                            feld[1][K] /= PhysicalParameters.RHO_SEDIM;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Fehler in Zeile " + lineNumber + "!");
                        System.out.println("Fehlerhaftes Format bei der Angabe der Zeitreiheninformation !");
                        System.out.println("> " + dataLine.line);
                        System.exit(0);
                    }

                }

                for (K = 0; K <= Anz_Periode; K++) {
                    feld[1][K] *= Ordinaten;
                    feld[1][K] += Translation;
                }
                DiscretScalarFunction1d boundcond = new DiscretScalarFunction1d(feld);
                if ((Art_Rndwrt == Qx_GESETZT) || (Art_Rndwrt == Qy_GESETZT)) {
                    int[] knotennummern = new int[anz_identische_Knoten];
                    int i = 0;
                    for (K = pointer; K < pointer + anz_identische_Knoten; K++) {
                        knotennummern[i] = KnotenNr[K];
                        i++;
                    }
                    if (Art_Rndwrt == Qx_GESETZT) {
                        QSteuerung Qx = new QSteuerung(boundcond, knotennummern, fedecomposition);
                        Qx.setPeriodic(Gesamtzahl > 0);
                        if(Art_Rndfkt==4) Qx = new H_Q_Steuerung(boundcond, knotennummern, fedecomposition);
                        for (K = pointer; K < pointer + anz_identische_Knoten; K++) {
                            if (Qx_FLAG) {
                                bc.add(new BoundaryCondition(BoundaryCondition.absolute_flowrate_x, KnotenNr[K], Qx));
                            }

                        }
                        pointer += anz_identische_Knoten;
                    }
                    if (Art_Rndwrt == Qy_GESETZT) {
                        QSteuerung Qy = new QSteuerung(boundcond, knotennummern, fedecomposition);
                        Qy.setPeriodic(Gesamtzahl > 0);
                        if(Art_Rndfkt==4) Qy = new H_Q_Steuerung(boundcond, knotennummern, fedecomposition);
                        for (K = pointer; K < pointer + anz_identische_Knoten; K++) {
                            if (Qy_FLAG) {
                                bc.add(new BoundaryCondition(BoundaryCondition.absolute_flowrate_y, KnotenNr[K], Qy));
                            }

                        }
                        pointer += anz_identische_Knoten;
                    }
                } else {
                    boundcond.setPeriodic(Gesamtzahl > 0);
                    for (K = pointer; K < pointer + anz_identische_Knoten; K++) {

                        switch (Art_Rndwrt) {
                            case qx_GESETZT:
                                if (qx_FLAG) {
                                    bc.add(new BoundaryCondition(BoundaryCondition.specific_flowrate_x, KnotenNr[K], boundcond));
                                }
                                break;
                            case qy_GESETZT:
                                if (qy_FLAG) {
                                    bc.add(new BoundaryCondition(BoundaryCondition.specific_flowrate_y, KnotenNr[K], boundcond));
                                }
                                break;
                            case H_GESETZT:
                                if (H_FLAG) {
                                    bc.add(new BoundaryCondition(BoundaryCondition.free_surface, KnotenNr[K], boundcond));
                                }
                                break;
                            case U_GESETZT:
                                if (U_FLAG) {
                                    bc.add(new BoundaryCondition(BoundaryCondition.velocity_u, KnotenNr[K], boundcond));
                                }
                                break;
                            case V_GESETZT:
                                if (V_FLAG) {
                                    bc.add(new BoundaryCondition(BoundaryCondition.velocity_v, KnotenNr[K], boundcond));
                                }
                                break;
                            case Q_Source:
                                if (Point_Q_FLAG) {
                                    bc.add(new BoundaryCondition(BoundaryCondition.pointbased_Q_source, KnotenNr[K], boundcond));
                                }
                                break;
                            case H_Source:
                                if (Point_H_FLAG) {
                                    bc.add(new BoundaryCondition(BoundaryCondition.pointbased_h_source, KnotenNr[K], boundcond));
                                }
                                break;
                            case TEMPERATURE_GESETZT:
                                if (TEMPERATURE_FLAG) {
                                    bc.add(new BoundaryCondition(BoundaryCondition.water_temperature, KnotenNr[K], boundcond));
                                }
                                break;
                            case SALT_GESETZT:
                                if (SALT_FLAG) {
                                    bc.add(new BoundaryCondition(BoundaryCondition.concentration_salt, KnotenNr[K], boundcond));
                                }
                                break;
                            case SALT_Source:
                                if (Point_SALT_FLAG) {
                                    bc.add(new BoundaryCondition(BoundaryCondition.pointbased_salt_source, KnotenNr[K], boundcond));
                                }
                                break;
                            case BOTTOM_GESETZT:
                                if (BOTTOM_FLAG) {
                                    bc.add(new BoundaryCondition(BoundaryCondition.bottom, KnotenNr[K], boundcond));
                                }
                                break;
                            case CONCENTRATION_GESETZT:
                                if (CONCENTRATION_FLAG) {
                                    bc.add(new BoundaryCondition(BoundaryCondition.concentration_sediment, KnotenNr[K], boundcond));
                                }
                                break;
                            case QxygenConcentration:
                                if (Qxygen_FLAG) {
                                    bc.add(new BoundaryCondition(BoundaryCondition.concentration_oxygen, KnotenNr[K], boundcond));
                                }
                                break;
                            case D50_GESETZT:
                                if (D50_FLAG) {
                                    bc.add(new BoundaryCondition(BoundaryCondition.d50, KnotenNr[K], boundcond));
                                }
                                break;
                        }
                    }
                    pointer += anz_identische_Knoten;

                }//else


            }//while(pointer < anzrndwrt)

        } catch (java.io.FileNotFoundException e) {
            System.out.println("Randwertedatei nicht gefunden!");
            System.exit(0);
        } catch (Exception e) {
            if (pointer < anzrndwrt) {
                System.out.println("pointer="+pointer);
                System.out.println("anzrndwrt="+anzrndwrt);
                System.out.println("Fehlende Anzahl an Datensaetze!");
                System.out.println(e);
            }
            System.out.println("Fehler beim Lesen der Datei!");
            System.exit(0);
        }

//        System.out.println("\tRandwertedatei <"+filename+"> gelesen");

        return bc.toArray(new BoundaryCondition[bc.size()]);
    } // end readBoundCond

    //ueberliest alle Kommentarzeilen und gibt naechste relevante Zeile zurueck
    DataLine readNextDataLine(BufferedReader bufferedReader, int currentLineNr) throws IOException {
        String line;
        int cnt = 0;
        // ueberlesen der Kommentarzeilen
        do {
            line = bufferedReader.readLine(); // Leerzeile
            cnt++;

        } while (isCommandLine(line));

        return new DataLine(line, currentLineNr + cnt);
    }

    boolean isCommandLine(String line) {
        StringTokenizer strto = new StringTokenizer(line);
        int anz = strto.countTokens();
        if (anz <= 0) {
            return false;
        } else {
            if (strto.nextToken().compareTo("C") == 0) {
                return true;
            }
        }
        return false;

    }

    private static class DataLine {

        int nr;
        String line;

        DataLine(String l, int nr) {
            line = l;
            this.nr = nr;
        }
    }
}
