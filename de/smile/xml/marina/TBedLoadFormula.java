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
//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 generiert 
// Siehe <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2025.12.28 um 03:43:54 PM CET 
//


package de.smile.xml.marina;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für TBedLoadFormula.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="TBedLoadFormula"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="MPM48"/&gt;
 *     &lt;enumeration value="FernandezLuque_vanBeck_1976"/&gt;
 *     &lt;enumeration value="Wilson_1966"/&gt;
 *     &lt;enumeration value="Wiberg_Smith_1989"/&gt;
 *     &lt;enumeration value="EngelundHansen"/&gt;
 *     &lt;enumeration value="EngelundHansen_1967"/&gt;
 *     &lt;enumeration value="EngelundHansen67"/&gt;
 *     &lt;enumeration value="EngelundHansen72"/&gt;
 *     &lt;enumeration value="Hunziker95"/&gt;
 *     &lt;enumeration value="CamenenLarson2005"/&gt;
 *     &lt;enumeration value="Zanke"/&gt;
 *     &lt;enumeration value="EHWS"/&gt;
 *     &lt;enumeration value="Yang_1973"/&gt;
 *     &lt;enumeration value="KomarovaAndHulscher_2000"/&gt;
 *     &lt;enumeration value="vanRijn84"/&gt;
 *     &lt;enumeration value="vanRijn89"/&gt;
 *     &lt;enumeration value="vanRijn2007"/&gt;
 *     &lt;enumeration value="Einstein-Brown_1950"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "TBedLoadFormula")
@XmlEnum
public enum TBedLoadFormula {

    @XmlEnumValue("MPM48")
    MPM_48("MPM48"),
    @XmlEnumValue("FernandezLuque_vanBeck_1976")
    FERNANDEZ_LUQUE_VAN_BECK_1976("FernandezLuque_vanBeck_1976"),
    @XmlEnumValue("Wilson_1966")
    WILSON_1966("Wilson_1966"),
    @XmlEnumValue("Wiberg_Smith_1989")
    WIBERG_SMITH_1989("Wiberg_Smith_1989"),
    @XmlEnumValue("EngelundHansen")
    ENGELUND_HANSEN("EngelundHansen"),
    @XmlEnumValue("EngelundHansen_1967")
    ENGELUND_HANSEN_1967("EngelundHansen_1967"),
    @XmlEnumValue("EngelundHansen67")
    ENGELUND_HANSEN_67("EngelundHansen67"),
    @XmlEnumValue("EngelundHansen72")
    ENGELUND_HANSEN_72("EngelundHansen72"),
    @XmlEnumValue("Hunziker95")
    HUNZIKER_95("Hunziker95"),
    @XmlEnumValue("CamenenLarson2005")
    CAMENEN_LARSON_2005("CamenenLarson2005"),
    @XmlEnumValue("Zanke")
    ZANKE("Zanke"),
    EHWS("EHWS"),
    @XmlEnumValue("Yang_1973")
    YANG_1973("Yang_1973"),
    @XmlEnumValue("KomarovaAndHulscher_2000")
    KOMAROVA_AND_HULSCHER_2000("KomarovaAndHulscher_2000"),
    @XmlEnumValue("vanRijn84")
    VAN_RIJN_84("vanRijn84"),
    @XmlEnumValue("vanRijn89")
    VAN_RIJN_89("vanRijn89"),
    @XmlEnumValue("vanRijn2007")
    VAN_RIJN_2007("vanRijn2007"),
    @XmlEnumValue("Einstein-Brown_1950")
    EINSTEIN_BROWN_1950("Einstein-Brown_1950");
    private final String value;

    TBedLoadFormula(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TBedLoadFormula fromValue(String v) {
        for (TBedLoadFormula c: TBedLoadFormula.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
