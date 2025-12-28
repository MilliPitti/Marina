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
 * <p>Java-Klasse für TSoilModelFileType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="TSoilModelFileType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="CSV"/&gt;
 *     &lt;enumeration value="AVSUCD"/&gt;
 *     &lt;enumeration value="SediMorph"/&gt;
 *     &lt;enumeration value="Delft3D.FM"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "TSoilModelFileType")
@XmlEnum
public enum TSoilModelFileType {

    CSV("CSV"),
    AVSUCD("AVSUCD"),
    @XmlEnumValue("SediMorph")
    SEDI_MORPH("SediMorph"),
    @XmlEnumValue("Delft3D.FM")
    DELFT_3_D_FM("Delft3D.FM");
    private final String value;

    TSoilModelFileType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TSoilModelFileType fromValue(String v) {
        for (TSoilModelFileType c: TSoilModelFileType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
