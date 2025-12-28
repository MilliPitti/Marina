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
 * <p>Java-Klasse für TWaveBoundaryFileType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="TWaveBoundaryFileType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="WaveRandnDat"/&gt;
 *     &lt;enumeration value="RndwerteBAW"/&gt;
 *     &lt;enumeration value="WaveXML"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "TWaveBoundaryFileType")
@XmlEnum
public enum TWaveBoundaryFileType {

    @XmlEnumValue("WaveRandnDat")
    WAVE_RANDN_DAT("WaveRandnDat"),
    @XmlEnumValue("RndwerteBAW")
    RNDWERTE_BAW("RndwerteBAW"),
    @XmlEnumValue("WaveXML")
    WAVE_XML("WaveXML");
    private final String value;

    TWaveBoundaryFileType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TWaveBoundaryFileType fromValue(String v) {
        for (TWaveBoundaryFileType c: TWaveBoundaryFileType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
