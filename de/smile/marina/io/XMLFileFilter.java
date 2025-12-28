package de.smile.marina.io;

import java.io.*;

/**
 *
 * @author lippert
 * @version
 */
public class XMLFileFilter extends javax.swing.filechooser.FileFilter {

    public XMLFileFilter() {
        super();
    }

    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }

        return file.getName().indexOf(".xml") > 0 || file.getName().indexOf(".xml".toUpperCase()) > 0 || file.getName().indexOf(".xml".toLowerCase()) > 0;
    }

    @Override
    public String getDescription() {
        return "Marina-Steuerdatei (.xml)";
    }
}