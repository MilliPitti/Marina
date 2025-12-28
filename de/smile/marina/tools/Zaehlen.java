package de.smile.marina.tools;

/**
 *
 * @author Peter
 */
public class Zaehlen {
    public static void main(String... args){
        for(int i=0;i<=100;i++){
            System.out.print("\t"+ i);
            if (i%10 == 0) System.out.print("\n");
        }
    } 
}
