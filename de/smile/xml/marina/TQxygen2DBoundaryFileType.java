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
 * <p>Java-Klasse für TQxygen2DBoundaryFileType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="TQxygen2DBoundaryFileType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="RndwerteBAW"/&gt;
 *     &lt;enumeration value="Oxygen2DXML"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "TQxygen2DBoundaryFileType")
@XmlEnum
public enum TQxygen2DBoundaryFileType {

    @XmlEnumValue("RndwerteBAW")
    RNDWERTE_BAW("RndwerteBAW"),
    @XmlEnumValue("Oxygen2DXML")
    OXYGEN_2_DXML("Oxygen2DXML");
    private final String value;

    TQxygen2DBoundaryFileType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TQxygen2DBoundaryFileType fromValue(String v) {
        for (TQxygen2DBoundaryFileType c: TQxygen2DBoundaryFileType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
