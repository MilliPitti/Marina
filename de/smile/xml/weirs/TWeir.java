//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 generiert 
// Siehe <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2025.12.28 um 03:42:03 PM CET 
//


package de.smile.xml.weirs;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für TWeir complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="TWeir"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ListofNodes" type="{http://www.example.org/Weirs}TListOfNodes"/&gt;
 *         &lt;element name="WeirType" type="{http://www.example.org/Weirs}TWeirType"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TWeir", propOrder = {
    "listofNodes",
    "weirType"
})
public class TWeir {

    @XmlElement(name = "ListofNodes", required = true)
    protected TListOfNodes listofNodes;
    @XmlElement(name = "WeirType", required = true)
    protected TWeirType weirType;

    /**
     * Ruft den Wert der listofNodes-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link TListOfNodes }
     *     
     */
    public TListOfNodes getListofNodes() {
        return listofNodes;
    }

    /**
     * Legt den Wert der listofNodes-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link TListOfNodes }
     *     
     */
    public void setListofNodes(TListOfNodes value) {
        this.listofNodes = value;
    }

    /**
     * Ruft den Wert der weirType-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link TWeirType }
     *     
     */
    public TWeirType getWeirType() {
        return weirType;
    }

    /**
     * Legt den Wert der weirType-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link TWeirType }
     *     
     */
    public void setWeirType(TWeirType value) {
        this.weirType = value;
    }

}
