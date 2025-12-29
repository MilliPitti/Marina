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
package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FEdge;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.FTriangle;
import de.smile.marina.fem.model.hydrodynamic.wave.WaveFunction;
import de.smile.marina.fem.model.meteorology.MeteorologyData2D;
import java.util.ArrayList;

/** Klasse zur Beschreibung geschlossener Raender im Wellenmodell
 * wenn die Wellenhoehe kleiner WATT dann 
 *          wenn Windfeld vorhanden
 *              Richtung in den Wind drehen, Periode auf Winderzeugt setzen und wa nicht veraendern
 *          sonst
 *              Richtung aus inneren Knoten extrapolieren, Periode aus inneren Knoten extrapolieren und wa nicht veraendern
 * wenn Wellenhoehe groeszer WATT
 *          wenn Wellen in Richtung geschlossenen Rand laufen
 *              nichts tun
 *          sonst
 *              wenn Windfeld vorhanden
 *                  Richtung in den Wind drehen, Periode auf Winderzeugt setzen und wa = 0
 *              sonst
 *                  Richtung aus inneren Knoten extrapolieren, Periode aus inneren Knoten extrapolieren, wa = 0
 *
 * @author milbradt
 */
public class WaveHypClosedBoundaryCondition {

    private final DOF dof;
    private final WaveHYPModel2DData wavehyp;
    private ArrayList<FEdge> closedEdges;
    
    private double totaldepth;

    public WaveHypClosedBoundaryCondition(FEDecomposition fed, DOF dof) {
        this.dof = dof;
        totaldepth = dof.z;
        for (FElement felem : fed.getDOF(dof.number).getFElements()) {
            FTriangle tele = (FTriangle) felem;
            if (tele.getKennung() != 0) {
                if (tele.getKennung() == FTriangle.bit_kante_jk) {
                    //	System.out.println("  "+FTriangle.bit_nr_kante_jk+" bit_kante_jk");
                    if (tele.getDOF(1).number == dof.number) {
                        if (closedEdges == null) closedEdges = new ArrayList<>(2); //****
                        closedEdges.add(new FEdge(dof, tele.getDOF(2)));
                    }
                    if (tele.getDOF(2).number == dof.number) {
                        if (closedEdges == null) closedEdges = new ArrayList<>(2); //****
                        closedEdges.add(new FEdge(dof, tele.getDOF(1)));
                    }
                } else if (tele.getKennung() == FTriangle.bit_kante_ki) {
                    //	System.out.println("  "+FTriangle.bit_nr_kante_ki+" bit_kante_ki");
                    if (tele.getDOF(0).number == dof.number) {
                        if (closedEdges == null) closedEdges = new ArrayList<>(2); //****
                        closedEdges.add(new FEdge(dof, tele.getDOF(2)));
                    }
                    if (tele.getDOF(2).number == dof.number) {
                        if (closedEdges == null) closedEdges = new ArrayList<>(2); //****
                        closedEdges.add(new FEdge(dof, tele.getDOF(0)));
                    }
                } else if (tele.getKennung() == FTriangle.bit_kante_ij) {
                    //	System.out.println("  "+FTriangle.bit_nr_kante_ij+" bit_kante_ij");
                    if (tele.getDOF(0).number == dof.number) {
                        if (closedEdges == null) closedEdges = new ArrayList<>(2); //****
                        closedEdges.add(new FEdge(dof, tele.getDOF(1)));
                    }
                    if (tele.getDOF(1).number == dof.number) {
                        if (closedEdges == null) closedEdges = new ArrayList<>(2); //****
                        closedEdges.add(new FEdge(dof, tele.getDOF(0)));
                    }
                } else if ((tele.getKennung() == FTriangle.bit_kante_ijk)
                        || (tele.getKennung() == FTriangle.bit_kante_jki)
                        || (tele.getKennung() == FTriangle.bit_kante_kij)
                        || (tele.getKennung() == FTriangle.bit_kante_ijki)) {
                    //	System.out.println("alle");
                    for (int ll = 0; ll < 3; ll++) {
                        if (tele.getDOF(ll) == dof) {
                            if (closedEdges == null) closedEdges = new ArrayList<>(2); //****
                            closedEdges.add(new FEdge(dof, tele.getDOF((ll + 1) % 3)));
                            closedEdges.add(new FEdge(dof, tele.getDOF((ll - 1 + 3) % 3)));
                        }
                    }
                }
            }
        }
        wavehyp = WaveHYPModel2DData.extract(dof);
    }
    public void setClosedBoundaryCondition() {
        CurrentModel2DData current = CurrentModel2DData.extract(dof);
        if (current != null) {
            totaldepth = current.totaldepth;
        }
        MeteorologyData2D meteorologyData2D = MeteorologyData2D.extract(dof);
        if (wavehyp.wa < WaveHYPModel2D.WATT) {
            if (meteorologyData2D != null) {
                if ((meteorologyData2D.windspeed > 2.) && (totaldepth > 10. * WaveHYPModel2D.WATT)) { // in den Wind ausrichten
                    wavehyp.sigma = 2. * Math.PI / (1.8 * Math.sqrt(meteorologyData2D.windspeed));
                    wavehyp.kres = WaveFunction.WaveNumber(totaldepth, wavehyp.sigma);
                    wavehyp.kx = meteorologyData2D.windx / meteorologyData2D.windspeed * wavehyp.kres;
                    wavehyp.ky = meteorologyData2D.windy / meteorologyData2D.windspeed * wavehyp.kres;
                    wavehyp.wa = 0.;
                } else { // wind zu gering - extrapolierte Wellendaten
                    WaveHYPModel2DData tmpdata;
                    for (FElement elem : dof.getFElements()) {
                        for (int ll = 0; ll < 3; ll++) {
                            if (elem.getDOF(ll) == dof) {
                                for (int ii = 1; ii < 3; ii++) {
                                    tmpdata = WaveHYPModel2DData.extract(elem.getDOF((ll + ii) % 3));
                                    double dsigma = (wavehyp.sigma - tmpdata.sigma) / 10.;
                                    double dkx = (wavehyp.kx - tmpdata.kx) / 10.;
                                    double dky = (wavehyp.ky - tmpdata.ky) / 10.;
                                    if (tmpdata.extrapolate_sigma) { // ToDo hasClousdeBoundarsCondition
                                        dsigma /= 10.;
                                        dkx /= 10.;
                                        dky /= 10.;
                                    }
                                    synchronized (wavehyp) {
                                        wavehyp.sigma -= dsigma;
                                        wavehyp.kx -= dkx;
                                        wavehyp.ky -= dky;
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            } else { // kein Windfeld
                
            }
        } else {

        }
    }

}
