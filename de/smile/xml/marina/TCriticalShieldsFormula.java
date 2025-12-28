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
 * <p>Java-Klasse für TCriticalShieldsFormula.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="TCriticalShieldsFormula"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Classic"/&gt;
 *     &lt;enumeration value="Sisyphe"/&gt;
 *     &lt;enumeration value="Soulsby"/&gt;
 *     &lt;enumeration value="SoulsbyKnoroz"/&gt;
 *     &lt;enumeration value="VanRijn"/&gt;
 *     &lt;enumeration value="Knoroz"/&gt;
 *     &lt;enumeration value="Julien"/&gt;
 *     &lt;enumeration value="JulienKnoroz"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "TCriticalShieldsFormula")
@XmlEnum
public enum TCriticalShieldsFormula {

    @XmlEnumValue("Classic")
    CLASSIC("Classic"),
    @XmlEnumValue("Sisyphe")
    SISYPHE("Sisyphe"),
    @XmlEnumValue("Soulsby")
    SOULSBY("Soulsby"),
    @XmlEnumValue("SoulsbyKnoroz")
    SOULSBY_KNOROZ("SoulsbyKnoroz"),
    @XmlEnumValue("VanRijn")
    VAN_RIJN("VanRijn"),
    @XmlEnumValue("Knoroz")
    KNOROZ("Knoroz"),
    @XmlEnumValue("Julien")
    JULIEN("Julien"),
    @XmlEnumValue("JulienKnoroz")
    JULIEN_KNOROZ("JulienKnoroz");
    private final String value;

    TCriticalShieldsFormula(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TCriticalShieldsFormula fromValue(String v) {
        for (TCriticalShieldsFormula c: TCriticalShieldsFormula.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
