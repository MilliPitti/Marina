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
 * <p>Java-Klasse für TEco2DBoundaryFileType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="TEco2DBoundaryFileType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="EcoBoundaryDat"/&gt;
 *     &lt;enumeration value="EcoBoundary2DXML"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "TEco2DBoundaryFileType")
@XmlEnum
public enum TEco2DBoundaryFileType {

    @XmlEnumValue("EcoBoundaryDat")
    ECO_BOUNDARY_DAT("EcoBoundaryDat"),
    @XmlEnumValue("EcoBoundary2DXML")
    ECO_BOUNDARY_2_DXML("EcoBoundary2DXML");
    private final String value;

    TEco2DBoundaryFileType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TEco2DBoundaryFileType fromValue(String v) {
        for (TEco2DBoundaryFileType c: TEco2DBoundaryFileType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
