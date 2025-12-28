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
 * <p>Java-Klasse für TMeshFileType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="TMeshFileType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="JanetBinaryFile"/&gt;
 *     &lt;enumeration value="SysDatFile"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "TMeshFileType")
@XmlEnum
public enum TMeshFileType {

    @XmlEnumValue("JanetBinaryFile")
    JANET_BINARY_FILE("JanetBinaryFile"),
    @XmlEnumValue("SysDatFile")
    SYS_DAT_FILE("SysDatFile");
    private final String value;

    TMeshFileType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TMeshFileType fromValue(String v) {
        for (TMeshFileType c: TMeshFileType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
