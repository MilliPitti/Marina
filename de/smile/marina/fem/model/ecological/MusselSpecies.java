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
package de.smile.marina.fem.model.ecological;

import de.smile.geom.LinearPoint;
import de.smile.math.function.ScalarFunction1d;

/**
 *
 * @author milbradt
 */
public enum MusselSpecies  implements LinearPoint<LinearPoint<?>>{
    // Magallana (Crassostrea) gigas
    Auster(2., // growthRate
            110., // maxBiomasseProMuschel [g]
            0.45, // maxDiameter
            0.15, // mortalityRate
            1., // ausbreiteRadiusDerLarven
            5.E-3, // minDiameter
            1700, // max Anzahl Individuen
            100.E3, // max BioMasse [g/m**2]
            (double trockenFallAnteil) -> {
                if (trockenFallAnteil > 0.42 || Double.isNaN(trockenFallAnteil)) {
                    return 0.;
                } else {
                    return cos_pos(0., 0.42, trockenFallAnteil);
                }
            },
            (double salinity) -> {
                final double min = 17;
                final double max = 23;
                if (salinity < min || Double.isNaN(salinity)) {
                    return 0.;
                } else if (salinity >= min && salinity <= max) {
                    return cos_pos(min, max, salinity);
                } else if (salinity > max) {
                    return 1.;
                }
                return 0;
            },
            (double erosion) -> {
                double maxErosion = -.2;
                if (erosion >= 0.) {
                    return 1.;
                }
                if (erosion <= maxErosion) {
                    return 0.;
                }
                return cos_pos(maxErosion, 0., erosion);
            },
            (double musselShellLength) -> { // Bestimmung des LebenddNassGewicht aus der Muschelgroesze
                if (musselShellLength < 0 || Double.isNaN(musselShellLength)) {
                    return 0.;
                } else {
//                        return (0.0057  * Math.pow(musselShellLength, 2.0455)); // Senkenberg 0.0057
                    return (0.0032 * Math.pow(musselShellLength, 2.2321)); // (Nehls & Boettger 2006)
                }
            }),
    // Mytilus edulis L
    Miesmuschel(1.8,
            30.,
            .12,
            0.3,
            1.,
            5.E-3,
            6000, // max Anzahl Individuen
            25.E3, // max BioMasse [g/m**2]
            (double trockenFallAnteil) -> {
                if (trockenFallAnteil > 0.42 || Double.isNaN(trockenFallAnteil)) {
                    return 0.;
                } else {
                    return cos_pos(0., 0.42, trockenFallAnteil);
                }
            },
            (double salinity) -> {
                final double min = 10;
                final double max = 18;
                if (salinity < min || Double.isNaN(salinity)) {
                    return 0.;
                } else if (salinity >= min && salinity <= max) {
                    return cos_pos(min, max, salinity);
                } else if (salinity > max) {
                    return 1.;
                }
                return 0;
            },
            (double erosion) -> {
                double maxErosion = -.2;
                if (erosion >= 0.) {
                    return 1.;
                }
                if (erosion <= maxErosion) {
                    return 0.;
                }
                return cos_pos(maxErosion, 0., erosion);
            },
            (double musselShellLength) -> { // Bestimmung des LebenddNassGewicht aus der Muschelgroesze
                if (musselShellLength < 0 || Double.isNaN(musselShellLength)) {
                    return 0.;
                } else {
                    return (Math.max(0, 2.919 * Math.log(musselShellLength) - 8.764)); // LebenddNassGewicht LGN Miesmuschel: 2,919 * ln(Schalenl�nge in mm) -8,764	 (Nehls,G. 1995: Strategien der Ern�hrung und ihre Bedeutung f�r Energiehaushalt und Oekologie der Eiderente (Somateria mollissima (L. 1758)). Dissertation, Universitaet Kiel)
                }
            }), 
    // Cerastoderma edule
    Herzmuschel(1.5, 
            20.,
            0.05,
            0.25,
            1.,
            2.5E-3,
            3000, // max Anzahl Individuen
            10.e3, // max BioMasse [g/m**2]
            (double trockenFallAnteil) -> {
                if (trockenFallAnteil > 0.42 || Double.isNaN(trockenFallAnteil)) {
                    return 0.;
                } else {
                    return cos_pos(0., 0.42, trockenFallAnteil);
                }
            }, (double salinity) -> {
                final double min = 14;
                final double max = 22;
                if (salinity < min || Double.isNaN(salinity)) {
                    return 0.;
                } else if (salinity >= min && salinity <= max) {
                    return cos_pos(min, max, salinity);
                } else if (salinity > max) {
                    return 1.;
                }
                return 0;
            }, (double erosion) -> {
                double maxErosion = -.2;
                if (erosion >= 0.) {
                    return 1.;
                }
                if (erosion <= maxErosion) {
                    return 0.;
                }
                return cos_pos(maxErosion, 0., erosion);
            }, (double musselShellLength) -> {
                throw new UnsupportedOperationException("Not supported yet.");
            });

    public final double growthRate; // g/m**2/a Biomasse
    // groeszenWachstumsrate haengt von Durchmesser der Muschel ab, je groeszer desto kleiner die Wachstumsrate (kleine Auster 1 (Verdoppelung in einem Jahr) bis grosze mit 0.13)

    public final double maxBiomasseProMuschel; // g
    public final double maxDiameter; // [m] maximale Muschelgroesze
    public final double maxBioMassPerSqrMeter; // [g/m**2] maximale biomasse pro quadratmeter

    public final int maxIndividualPerSqrMeter;

    public final double mortalityRate; // inclusive Vogelfrass
    // abnahme der Biomasse und wechsel auf tote Muscheln

    public final double ausbreiteRadiusDerLarven;

    public final double minDiameter; // [m] minimale Ausdehnung wenn diese benthisch geworden ist und kein Larvenstadium

    public ScalarFunction1d trockenFallPotential;
    public ScalarFunction1d salzPotential;
    public ScalarFunction1d erosionPotential;

    public ScalarFunction1d lGN; //   Funktion zur Bestimmung des LebenddNassGewicht aus der Muschelgroesze

    private MusselSpecies(double wachstumsRate,
            double maxBiomasseProMuschel,
            double maxDiameter,
            double sterbeRate,
            double ausbreiteRadiusDerLarven,
            double minDiameter,
            int maxIndividualPerSqrMeter,
            double maxBioMassPerSqrMeter,
            ScalarFunction1d trockenFallPotential,
            ScalarFunction1d salzPotential,
            ScalarFunction1d erosionPotential,
            ScalarFunction1d lGN
    ) {

        this.growthRate = wachstumsRate;

        this.maxBiomasseProMuschel = maxBiomasseProMuschel;
        this.maxDiameter = maxDiameter;

        this.maxBioMassPerSqrMeter = maxBioMassPerSqrMeter;

        this.mortalityRate = sterbeRate;

        this.ausbreiteRadiusDerLarven = ausbreiteRadiusDerLarven;

        this.minDiameter = minDiameter;

        this.maxIndividualPerSqrMeter = maxIndividualPerSqrMeter;

        this.trockenFallPotential = trockenFallPotential;
        this.salzPotential = salzPotential;
        this.erosionPotential = erosionPotential;

        this.lGN = lGN;

    }

    private static double cos_pos(double minX, double maxX, double x) {
        return .5 * Math.cos((Math.PI / (maxX - minX)) * x - (maxX / (maxX - minX)) * Math.PI) + .5;
    }
    
    @Override
    public LinearPoint<?> add(LinearPoint<?> point) {
        if (!(this == point)) {
            throw new IllegalArgumentException("this.species." + this + "! point.species." + point);
        }
        return this;
    }

    @Override
    public LinearPoint<?> sub(LinearPoint<?> point) {
        if (!(this == point)) {
            throw new IllegalArgumentException("this.species." + this + "! point.species." + point);
        }
        return this;
    }

    @Override
    public LinearPoint<?> mult(double scalar) {
        return this;
    }
}
