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
 * <p>Java-Klasse für TSuspendedLoadFormula.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="TSuspendedLoadFormula"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="RossinskyDebolsky"/&gt;
 *     &lt;enumeration value="Bagnold1966"/&gt;
 *     &lt;enumeration value="vanRijn84"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "TSuspendedLoadFormula")
@XmlEnum
public enum TSuspendedLoadFormula {

    @XmlEnumValue("RossinskyDebolsky")
    ROSSINSKY_DEBOLSKY("RossinskyDebolsky"),
    @XmlEnumValue("Bagnold1966")
    BAGNOLD_1966("Bagnold1966"),
    @XmlEnumValue("vanRijn84")
    VAN_RIJN_84("vanRijn84");
    private final String value;

    TSuspendedLoadFormula(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TSuspendedLoadFormula fromValue(String v) {
        for (TSuspendedLoadFormula c: TSuspendedLoadFormula.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
