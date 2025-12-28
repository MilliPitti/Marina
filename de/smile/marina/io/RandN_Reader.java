package de.smile.marina.io;
import bijava.math.ifunction.*;
import java.io.*;
import java.util.*;

/**Diese Klasse stellt Methoden zum Einlesen einer <code>randn.dat</code> zur Verfuegung.
 * Diese Methoden sind leider aufgrund des bloeden Aufbaus einer solchen randn.dat
 * grottenlangsam. Naja, kann man wohl nix machen.				
 * @version 2.7.11
 */

public class RandN_Reader
{
    
    private int anz_werte;
    private long pos;
    private long anfang_datei;
    private RandomAccessFile lnr;
    private String line;
    private StringTokenizer st;
    
    public RandN_Reader(String filename)
    {
        anz_werte=0;
        line="";
        System.out.println("\tReading BoundaryCondition-File : " + filename);
        try{
            lnr = new RandomAccessFile(filename, "r");
            anfang_datei = lnr.getFilePointer();
            
        }
        catch(Exception e){e.printStackTrace(); System.exit(0);}
    }
    
    public DiscretScalarFunction1d[] readnextTimefunction(){
        DiscretScalarFunction1d[] fkt = null;
        try{
            anz_werte =  jumpToWavetimeseries();
            //System.out.println(anz_werte);
            if(anz_werte==0) return fkt;
            double[][] w1 = new double[2][anz_werte];
            double[][] w2 = new double[2][anz_werte]; // theta
            double[][] w3 = new double[2][anz_werte];
            
            double[][] w4 = new double[2][anz_werte]; //sintheta
            double[][] w5 = new double[2][anz_werte]; //costheta
            
            for(int i=0; i<anz_werte; i++)
            {
                line = lnr.readLine();
                st = new StringTokenizer(line);
                //System.out.println(st);
                w1[0][i] = Double.parseDouble(st.nextToken());
                w2[0][i] = w1[0][i];
                w3[0][i] = w1[0][i];
                w1[1][i] = 0.5 * Double.parseDouble(st.nextToken());
                w2[1][i] = Double.parseDouble(st.nextToken());
                w3[1][i] = 2.* Math.PI / Double.parseDouble(st.nextToken());
                //System.out.println(" "+w1[0][i]+" "+w1[1][i]+" "+w2[1][i]+" "+w3[1][i]);
                
                w4[0][i] = w1[0][i];
                w4[1][i] = Math.sin(Math.PI/180. * w2[1][i]);
                w5[0][i] = w1[0][i];
                w5[1][i] = Math.cos(Math.PI/180. * w2[1][i]);
            }
            fkt = new DiscretScalarFunction1d[5];
            
            fkt[0] = new DiscretScalarFunction1d(w1);
            
            fkt[1] = new DiscretScalarFunction1d(w2);
            
            fkt[2] = new DiscretScalarFunction1d(w3);
            
            fkt[3] = new DiscretScalarFunction1d(w4);
            fkt[4] = new DiscretScalarFunction1d(w5);
            
        }catch(IOException | NumberFormatException e){e.printStackTrace();}
        return fkt;
        
    }
    
    public int[] readNodes(){
        
        ArrayList<Integer> tmp = new ArrayList<>();
        try{
            boolean noKlammer = true;
            int nr;
            
            //lookForKeyword("knotennumbers{");
            boolean weiter = true;
            while(weiter){
                line = lnr.readLine();
                st = new StringTokenizer(line);
                int at = st.countTokens();
                for(int i =0; i< at ; i++)
                    if(st.nextToken().equals("knotennumbers{")){
                        //System.out.println("knotennumbers{");
                        weiter=false;
                        i = at;
                    }
            }

            int at = st.countTokens();
            for(int i =0; i< at ; i++){
                String token = st.nextToken();
                if(token.equals("}")){noKlammer=false;} else {
                    nr = Integer.parseInt(token);
                    tmp.add(nr);
                }
            }
            
            while(noKlammer)
            {   
                line = lnr.readLine();
                st = new StringTokenizer(line);
                at = st.countTokens();
            for(int i =0; i< at ; i++){
                   String token = st.nextToken();
                    if(token.equals("}")){noKlammer=false;} else {
                        nr = Integer.parseInt(token);
                        tmp.add(nr);
                    }
                }
            }
        }catch(IOException | NumberFormatException e){e.printStackTrace();}
        int anzahl =tmp.size();
        int [] nummern = new int[anzahl];
        for (int i=0;i<anzahl;i++){
            nummern[i]=tmp.get(i);
        }
        return nummern;
    }
    
    public DiscretScalarFunction1d[] leseZeitreihen(int knotennr)
    {
        DiscretScalarFunction1d[] fkt = new DiscretScalarFunction1d[3];
        
        try{
            
            sucheAnfangKnotenZeitreihe(knotennr);
            line = lnr.readLine();
            System.out.println(line);
            st = new StringTokenizer(line);
            st.nextToken(); 
            anz_werte = Integer.parseInt(st.nextToken());
            System.out.println(anz_werte);
            double[][] werte = new double[2][anz_werte];
            
            for(int i=0; i< anz_werte; i++)
            {
                line = lnr.readLine();
                st = new StringTokenizer(line);
                werte[0][i] = Double.parseDouble(st.nextToken());
                werte[1][i] = Double.parseDouble(st.nextToken());
            }
            fkt[0] = new DiscretScalarFunction1d(werte);
            
            sucheAnfangKnotenZeitreihe(knotennr);
            line=lnr.readLine();
            st = new StringTokenizer(line);
            st.nextToken(); 
            anz_werte = Integer.parseInt(st.nextToken());
            werte = new double[2][anz_werte];
            for(int i=0; i< anz_werte; i++)
            {
                line = lnr.readLine();
                st = new StringTokenizer(line);
                werte[0][i] = Double.parseDouble(st.nextToken());
                st.nextToken();
                werte[1][i] = Double.parseDouble(st.nextToken());
            }
            fkt[1] = new DiscretScalarFunction1d(werte);
            
            sucheAnfangKnotenZeitreihe(knotennr);
            line=lnr.readLine();
            st = new StringTokenizer(line);
            st.nextToken(); 
            anz_werte = Integer.parseInt(st.nextToken());
            werte = new double[2][anz_werte];
            for(int i=0; i< anz_werte; i++)
            {
                line = lnr.readLine();
                st = new StringTokenizer(line);
                werte[0][i] = Double.parseDouble(st.nextToken());
                st.nextToken();
                st.nextToken();
                werte[1][i] = Double.parseDouble(st.nextToken());
            }
            fkt[2] = new DiscretScalarFunction1d(werte);
            
            
        }catch(IOException | NumberFormatException e){e.printStackTrace();}
        return fkt;
    }
    
    public DiscretScalarFunction1d leseZeitreiheHs(int knotennr)
    {
        DiscretScalarFunction1d fkt=null;
        
        try{
            
            
            sucheAnfangKnotenZeitreihe(knotennr);
            line = lnr.readLine();
            System.out.println(line);
            st = new StringTokenizer(line);
            st.nextToken(); 
            anz_werte = Integer.parseInt(st.nextToken());
            System.out.println(anz_werte);
            double[][] werte = new double[2][anz_werte];
            
            for(int i=0; i< anz_werte; i++)
            {
                line = lnr.readLine();
                st = new StringTokenizer(line);
                werte[0][i] = Double.parseDouble(st.nextToken());
                werte[1][i] = Double.parseDouble(st.nextToken());
            }
            fkt = new DiscretScalarFunction1d(werte);
            
        }catch(IOException | NumberFormatException e){e.printStackTrace();}
        
        return fkt;
    }
    
    
    private int jumpToWavetimeseries() {
        int anz = 0;
        try {
            boolean weiter = true;
            while (weiter) {
                pos = lnr.getFilePointer();
                line = lnr.readLine();
                if (line == null || line.contains("Ende")) {
                    return anz;
                }
                //System.out.println(line);
                st = new StringTokenizer(line);
                for (int i = 0; i < st.countTokens(); i++) {
                    if (st.nextToken().equals("wavetimeseries{")) {	//i=st.countTokens();
                        weiter = false;
                        anz = Integer.parseInt(st.nextToken());

                    }
                }
            }
        } catch (IOException | NumberFormatException e) {/*e.printStackTrace();*/
        }
        return anz;
    }
    
    void lookForKeyword(String word) {
        try {
            boolean weiter = true;
            while (weiter) {
                line = lnr.readLine();
                st = new StringTokenizer(line);
                for (int i = 0; i < st.countTokens(); i++) {
                    if (st.nextToken().equals(word)) {
                        i = st.countTokens();
                        weiter = false;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    //.Sucht den Anfang der zu einem Knoten gehoerenden Zeitreihe und gibt diesen als
    //.int (also Zeilennummer) zurueck...
    
    private void sucheAnfangKnotenZeitreihe(int knotennr) {
        try {
            lnr.seek(anfang_datei);
            int gelesenNr = -1;
            while (knotennr != gelesenNr) {
                jumpToWavetimeseries();
                lookForKeyword("knotennumbers{");

                st = new StringTokenizer(line);
                st.nextToken();
                gelesenNr = Integer.parseInt(st.nextToken());
                //System.out.println(gelesenNr);
            }
            lnr.seek(pos);
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }
    
}