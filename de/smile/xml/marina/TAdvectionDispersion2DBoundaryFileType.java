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
 * <p>Java-Klasse für TAdvectionDispersion2DBoundaryFileType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="TAdvectionDispersion2DBoundaryFileType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="RndwerteBAW"/&gt;
 *     &lt;enumeration value="AdvectionDispersion2DXML"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "TAdvectionDispersion2DBoundaryFileType")
@XmlEnum
public enum TAdvectionDispersion2DBoundaryFileType {

    @XmlEnumValue("RndwerteBAW")
    RNDWERTE_BAW("RndwerteBAW"),
    @XmlEnumValue("AdvectionDispersion2DXML")
    ADVECTION_DISPERSION_2_DXML("AdvectionDispersion2DXML");
    private final String value;

    TAdvectionDispersion2DBoundaryFileType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TAdvectionDispersion2DBoundaryFileType fromValue(String v) {
        for (TAdvectionDispersion2DBoundaryFileType c: TAdvectionDispersion2DBoundaryFileType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
