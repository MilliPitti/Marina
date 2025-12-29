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
// Generiert: 2025.12.28 um 03:42:03 PM CET 
//


package de.smile.xml.weirs;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für TWeirType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="TWeirType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice&gt;
 *         &lt;element name="BroadCrestedWeir"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="CrestLevel"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;choice&gt;
 *                             &lt;element name="Constant"&gt;
 *                               &lt;simpleType&gt;
 *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
 *                                 &lt;/restriction&gt;
 *                               &lt;/simpleType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
 *                             &lt;element name="WaterLevelControlled"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="InitalCrestLevel"&gt;
 *                                         &lt;simpleType&gt;
 *                                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
 *                                           &lt;/restriction&gt;
 *                                         &lt;/simpleType&gt;
 *                                       &lt;/element&gt;
 *                                       &lt;element name="ListofReferenceNodes" type="{http://www.example.org/Weirs}TListOfNodes"/&gt;
 *                                       &lt;element name="WaterLevel"&gt;
 *                                         &lt;complexType&gt;
 *                                           &lt;complexContent&gt;
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                               &lt;choice&gt;
 *                                                 &lt;element name="Constant"&gt;
 *                                                   &lt;simpleType&gt;
 *                                                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
 *                                                     &lt;/restriction&gt;
 *                                                   &lt;/simpleType&gt;
 *                                                 &lt;/element&gt;
 *                                                 &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
 *                                               &lt;/choice&gt;
 *                                             &lt;/restriction&gt;
 *                                           &lt;/complexContent&gt;
 *                                         &lt;/complexType&gt;
 *                                       &lt;/element&gt;
 *                                       &lt;element name="PControllerFactor" minOccurs="0"&gt;
 *                                         &lt;simpleType&gt;
 *                                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
 *                                           &lt;/restriction&gt;
 *                                         &lt;/simpleType&gt;
 *                                       &lt;/element&gt;
 *                                       &lt;element name="DControllerFactor" minOccurs="0"&gt;
 *                                         &lt;simpleType&gt;
 *                                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
 *                                           &lt;/restriction&gt;
 *                                         &lt;/simpleType&gt;
 *                                       &lt;/element&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                           &lt;/choice&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="BroadCrestedTopoWeir"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="CrestLevel"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;choice&gt;
 *                             &lt;element name="Constant"&gt;
 *                               &lt;simpleType&gt;
 *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
 *                                 &lt;/restriction&gt;
 *                               &lt;/simpleType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
 *                           &lt;/choice&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="UnderFlowWeir"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="SluiceLevel"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;choice&gt;
 *                             &lt;element name="Constant"&gt;
 *                               &lt;simpleType&gt;
 *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
 *                                 &lt;/restriction&gt;
 *                               &lt;/simpleType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
 *                           &lt;/choice&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="UnderFlowTopoWeir"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="SluiceLevel"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;choice&gt;
 *                             &lt;element name="Constant"&gt;
 *                               &lt;simpleType&gt;
 *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
 *                                 &lt;/restriction&gt;
 *                               &lt;/simpleType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
 *                           &lt;/choice&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="NeedleWeir"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="Opening"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;choice&gt;
 *                             &lt;element name="Constant"&gt;
 *                               &lt;simpleType&gt;
 *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
 *                                 &lt;/restriction&gt;
 *                               &lt;/simpleType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="TimeSeriesOfOpening" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
 *                             &lt;element name="WaterLevelControlled"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="InitalOpening"&gt;
 *                                         &lt;simpleType&gt;
 *                                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
 *                                           &lt;/restriction&gt;
 *                                         &lt;/simpleType&gt;
 *                                       &lt;/element&gt;
 *                                       &lt;element name="ListofReferenceNodes" type="{http://www.example.org/Weirs}TListOfNodes"/&gt;
 *                                       &lt;element name="WaterLevel"&gt;
 *                                         &lt;complexType&gt;
 *                                           &lt;complexContent&gt;
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                               &lt;choice&gt;
 *                                                 &lt;element name="Constant"&gt;
 *                                                   &lt;simpleType&gt;
 *                                                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
 *                                                     &lt;/restriction&gt;
 *                                                   &lt;/simpleType&gt;
 *                                                 &lt;/element&gt;
 *                                                 &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
 *                                               &lt;/choice&gt;
 *                                             &lt;/restriction&gt;
 *                                           &lt;/complexContent&gt;
 *                                         &lt;/complexType&gt;
 *                                       &lt;/element&gt;
 *                                       &lt;element name="PControllerFactor" minOccurs="0"&gt;
 *                                         &lt;simpleType&gt;
 *                                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
 *                                           &lt;/restriction&gt;
 *                                         &lt;/simpleType&gt;
 *                                       &lt;/element&gt;
 *                                       &lt;element name="DControllerFactor" minOccurs="0"&gt;
 *                                         &lt;simpleType&gt;
 *                                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
 *                                           &lt;/restriction&gt;
 *                                         &lt;/simpleType&gt;
 *                                       &lt;/element&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                           &lt;/choice&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="RadialGate"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="OpeningHight" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/choice&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TWeirType", propOrder = {
    "broadCrestedWeir",
    "broadCrestedTopoWeir",
    "underFlowWeir",
    "underFlowTopoWeir",
    "needleWeir",
    "radialGate"
})
public class TWeirType {

    @XmlElement(name = "BroadCrestedWeir")
    protected TWeirType.BroadCrestedWeir broadCrestedWeir;
    @XmlElement(name = "BroadCrestedTopoWeir")
    protected TWeirType.BroadCrestedTopoWeir broadCrestedTopoWeir;
    @XmlElement(name = "UnderFlowWeir")
    protected TWeirType.UnderFlowWeir underFlowWeir;
    @XmlElement(name = "UnderFlowTopoWeir")
    protected TWeirType.UnderFlowTopoWeir underFlowTopoWeir;
    @XmlElement(name = "NeedleWeir")
    protected TWeirType.NeedleWeir needleWeir;
    @XmlElement(name = "RadialGate")
    protected TWeirType.RadialGate radialGate;

    /**
     * Ruft den Wert der broadCrestedWeir-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link TWeirType.BroadCrestedWeir }
     *     
     */
    public TWeirType.BroadCrestedWeir getBroadCrestedWeir() {
        return broadCrestedWeir;
    }

    /**
     * Legt den Wert der broadCrestedWeir-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link TWeirType.BroadCrestedWeir }
     *     
     */
    public void setBroadCrestedWeir(TWeirType.BroadCrestedWeir value) {
        this.broadCrestedWeir = value;
    }

    /**
     * Ruft den Wert der broadCrestedTopoWeir-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link TWeirType.BroadCrestedTopoWeir }
     *     
     */
    public TWeirType.BroadCrestedTopoWeir getBroadCrestedTopoWeir() {
        return broadCrestedTopoWeir;
    }

    /**
     * Legt den Wert der broadCrestedTopoWeir-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link TWeirType.BroadCrestedTopoWeir }
     *     
     */
    public void setBroadCrestedTopoWeir(TWeirType.BroadCrestedTopoWeir value) {
        this.broadCrestedTopoWeir = value;
    }

    /**
     * Ruft den Wert der underFlowWeir-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link TWeirType.UnderFlowWeir }
     *     
     */
    public TWeirType.UnderFlowWeir getUnderFlowWeir() {
        return underFlowWeir;
    }

    /**
     * Legt den Wert der underFlowWeir-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link TWeirType.UnderFlowWeir }
     *     
     */
    public void setUnderFlowWeir(TWeirType.UnderFlowWeir value) {
        this.underFlowWeir = value;
    }

    /**
     * Ruft den Wert der underFlowTopoWeir-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link TWeirType.UnderFlowTopoWeir }
     *     
     */
    public TWeirType.UnderFlowTopoWeir getUnderFlowTopoWeir() {
        return underFlowTopoWeir;
    }

    /**
     * Legt den Wert der underFlowTopoWeir-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link TWeirType.UnderFlowTopoWeir }
     *     
     */
    public void setUnderFlowTopoWeir(TWeirType.UnderFlowTopoWeir value) {
        this.underFlowTopoWeir = value;
    }

    /**
     * Ruft den Wert der needleWeir-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link TWeirType.NeedleWeir }
     *     
     */
    public TWeirType.NeedleWeir getNeedleWeir() {
        return needleWeir;
    }

    /**
     * Legt den Wert der needleWeir-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link TWeirType.NeedleWeir }
     *     
     */
    public void setNeedleWeir(TWeirType.NeedleWeir value) {
        this.needleWeir = value;
    }

    /**
     * Ruft den Wert der radialGate-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link TWeirType.RadialGate }
     *     
     */
    public TWeirType.RadialGate getRadialGate() {
        return radialGate;
    }

    /**
     * Legt den Wert der radialGate-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link TWeirType.RadialGate }
     *     
     */
    public void setRadialGate(TWeirType.RadialGate value) {
        this.radialGate = value;
    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="CrestLevel"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;choice&gt;
     *                   &lt;element name="Constant"&gt;
     *                     &lt;simpleType&gt;
     *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
     *                       &lt;/restriction&gt;
     *                     &lt;/simpleType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
     *                 &lt;/choice&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "name",
        "crestLevel"
    })
    public static class BroadCrestedTopoWeir {

        @XmlElement(name = "Name")
        protected String name;
        @XmlElement(name = "CrestLevel", required = true)
        protected TWeirType.BroadCrestedTopoWeir.CrestLevel crestLevel;

        /**
         * Ruft den Wert der name-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getName() {
            return name;
        }

        /**
         * Legt den Wert der name-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setName(String value) {
            this.name = value;
        }

        /**
         * Ruft den Wert der crestLevel-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link TWeirType.BroadCrestedTopoWeir.CrestLevel }
         *     
         */
        public TWeirType.BroadCrestedTopoWeir.CrestLevel getCrestLevel() {
            return crestLevel;
        }

        /**
         * Legt den Wert der crestLevel-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link TWeirType.BroadCrestedTopoWeir.CrestLevel }
         *     
         */
        public void setCrestLevel(TWeirType.BroadCrestedTopoWeir.CrestLevel value) {
            this.crestLevel = value;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;complexContent&gt;
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *       &lt;choice&gt;
         *         &lt;element name="Constant"&gt;
         *           &lt;simpleType&gt;
         *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
         *             &lt;/restriction&gt;
         *           &lt;/simpleType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
         *       &lt;/choice&gt;
         *     &lt;/restriction&gt;
         *   &lt;/complexContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "constant",
            "timeSeries"
        })
        public static class CrestLevel {

            @XmlElement(name = "Constant")
            protected Double constant;
            @XmlElement(name = "TimeSeries")
            protected TTimeSeries timeSeries;

            /**
             * Ruft den Wert der constant-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Double }
             *     
             */
            public Double getConstant() {
                return constant;
            }

            /**
             * Legt den Wert der constant-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Double }
             *     
             */
            public void setConstant(Double value) {
                this.constant = value;
            }

            /**
             * Ruft den Wert der timeSeries-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link TTimeSeries }
             *     
             */
            public TTimeSeries getTimeSeries() {
                return timeSeries;
            }

            /**
             * Legt den Wert der timeSeries-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link TTimeSeries }
             *     
             */
            public void setTimeSeries(TTimeSeries value) {
                this.timeSeries = value;
            }

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="CrestLevel"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;choice&gt;
     *                   &lt;element name="Constant"&gt;
     *                     &lt;simpleType&gt;
     *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
     *                       &lt;/restriction&gt;
     *                     &lt;/simpleType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
     *                   &lt;element name="WaterLevelControlled"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="InitalCrestLevel"&gt;
     *                               &lt;simpleType&gt;
     *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
     *                                 &lt;/restriction&gt;
     *                               &lt;/simpleType&gt;
     *                             &lt;/element&gt;
     *                             &lt;element name="ListofReferenceNodes" type="{http://www.example.org/Weirs}TListOfNodes"/&gt;
     *                             &lt;element name="WaterLevel"&gt;
     *                               &lt;complexType&gt;
     *                                 &lt;complexContent&gt;
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                                     &lt;choice&gt;
     *                                       &lt;element name="Constant"&gt;
     *                                         &lt;simpleType&gt;
     *                                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
     *                                           &lt;/restriction&gt;
     *                                         &lt;/simpleType&gt;
     *                                       &lt;/element&gt;
     *                                       &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
     *                                     &lt;/choice&gt;
     *                                   &lt;/restriction&gt;
     *                                 &lt;/complexContent&gt;
     *                               &lt;/complexType&gt;
     *                             &lt;/element&gt;
     *                             &lt;element name="PControllerFactor" minOccurs="0"&gt;
     *                               &lt;simpleType&gt;
     *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
     *                                 &lt;/restriction&gt;
     *                               &lt;/simpleType&gt;
     *                             &lt;/element&gt;
     *                             &lt;element name="DControllerFactor" minOccurs="0"&gt;
     *                               &lt;simpleType&gt;
     *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
     *                                 &lt;/restriction&gt;
     *                               &lt;/simpleType&gt;
     *                             &lt;/element&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                 &lt;/choice&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "name",
        "crestLevel"
    })
    public static class BroadCrestedWeir {

        @XmlElement(name = "Name")
        protected String name;
        @XmlElement(name = "CrestLevel", required = true)
        protected TWeirType.BroadCrestedWeir.CrestLevel crestLevel;

        /**
         * Ruft den Wert der name-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getName() {
            return name;
        }

        /**
         * Legt den Wert der name-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setName(String value) {
            this.name = value;
        }

        /**
         * Ruft den Wert der crestLevel-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link TWeirType.BroadCrestedWeir.CrestLevel }
         *     
         */
        public TWeirType.BroadCrestedWeir.CrestLevel getCrestLevel() {
            return crestLevel;
        }

        /**
         * Legt den Wert der crestLevel-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link TWeirType.BroadCrestedWeir.CrestLevel }
         *     
         */
        public void setCrestLevel(TWeirType.BroadCrestedWeir.CrestLevel value) {
            this.crestLevel = value;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;complexContent&gt;
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *       &lt;choice&gt;
         *         &lt;element name="Constant"&gt;
         *           &lt;simpleType&gt;
         *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
         *             &lt;/restriction&gt;
         *           &lt;/simpleType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
         *         &lt;element name="WaterLevelControlled"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="InitalCrestLevel"&gt;
         *                     &lt;simpleType&gt;
         *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
         *                       &lt;/restriction&gt;
         *                     &lt;/simpleType&gt;
         *                   &lt;/element&gt;
         *                   &lt;element name="ListofReferenceNodes" type="{http://www.example.org/Weirs}TListOfNodes"/&gt;
         *                   &lt;element name="WaterLevel"&gt;
         *                     &lt;complexType&gt;
         *                       &lt;complexContent&gt;
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                           &lt;choice&gt;
         *                             &lt;element name="Constant"&gt;
         *                               &lt;simpleType&gt;
         *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
         *                                 &lt;/restriction&gt;
         *                               &lt;/simpleType&gt;
         *                             &lt;/element&gt;
         *                             &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
         *                           &lt;/choice&gt;
         *                         &lt;/restriction&gt;
         *                       &lt;/complexContent&gt;
         *                     &lt;/complexType&gt;
         *                   &lt;/element&gt;
         *                   &lt;element name="PControllerFactor" minOccurs="0"&gt;
         *                     &lt;simpleType&gt;
         *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
         *                       &lt;/restriction&gt;
         *                     &lt;/simpleType&gt;
         *                   &lt;/element&gt;
         *                   &lt;element name="DControllerFactor" minOccurs="0"&gt;
         *                     &lt;simpleType&gt;
         *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
         *                       &lt;/restriction&gt;
         *                     &lt;/simpleType&gt;
         *                   &lt;/element&gt;
         *                 &lt;/sequence&gt;
         *               &lt;/restriction&gt;
         *             &lt;/complexContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *       &lt;/choice&gt;
         *     &lt;/restriction&gt;
         *   &lt;/complexContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "constant",
            "timeSeries",
            "waterLevelControlled"
        })
        public static class CrestLevel {

            @XmlElement(name = "Constant")
            protected Double constant;
            @XmlElement(name = "TimeSeries")
            protected TTimeSeries timeSeries;
            @XmlElement(name = "WaterLevelControlled")
            protected TWeirType.BroadCrestedWeir.CrestLevel.WaterLevelControlled waterLevelControlled;

            /**
             * Ruft den Wert der constant-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Double }
             *     
             */
            public Double getConstant() {
                return constant;
            }

            /**
             * Legt den Wert der constant-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Double }
             *     
             */
            public void setConstant(Double value) {
                this.constant = value;
            }

            /**
             * Ruft den Wert der timeSeries-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link TTimeSeries }
             *     
             */
            public TTimeSeries getTimeSeries() {
                return timeSeries;
            }

            /**
             * Legt den Wert der timeSeries-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link TTimeSeries }
             *     
             */
            public void setTimeSeries(TTimeSeries value) {
                this.timeSeries = value;
            }

            /**
             * Ruft den Wert der waterLevelControlled-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link TWeirType.BroadCrestedWeir.CrestLevel.WaterLevelControlled }
             *     
             */
            public TWeirType.BroadCrestedWeir.CrestLevel.WaterLevelControlled getWaterLevelControlled() {
                return waterLevelControlled;
            }

            /**
             * Legt den Wert der waterLevelControlled-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link TWeirType.BroadCrestedWeir.CrestLevel.WaterLevelControlled }
             *     
             */
            public void setWaterLevelControlled(TWeirType.BroadCrestedWeir.CrestLevel.WaterLevelControlled value) {
                this.waterLevelControlled = value;
            }


            /**
             * <p>Java-Klasse für anonymous complex type.
             * 
             * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
             * 
             * <pre>
             * &lt;complexType&gt;
             *   &lt;complexContent&gt;
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *       &lt;sequence&gt;
             *         &lt;element name="InitalCrestLevel"&gt;
             *           &lt;simpleType&gt;
             *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
             *             &lt;/restriction&gt;
             *           &lt;/simpleType&gt;
             *         &lt;/element&gt;
             *         &lt;element name="ListofReferenceNodes" type="{http://www.example.org/Weirs}TListOfNodes"/&gt;
             *         &lt;element name="WaterLevel"&gt;
             *           &lt;complexType&gt;
             *             &lt;complexContent&gt;
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *                 &lt;choice&gt;
             *                   &lt;element name="Constant"&gt;
             *                     &lt;simpleType&gt;
             *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
             *                       &lt;/restriction&gt;
             *                     &lt;/simpleType&gt;
             *                   &lt;/element&gt;
             *                   &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
             *                 &lt;/choice&gt;
             *               &lt;/restriction&gt;
             *             &lt;/complexContent&gt;
             *           &lt;/complexType&gt;
             *         &lt;/element&gt;
             *         &lt;element name="PControllerFactor" minOccurs="0"&gt;
             *           &lt;simpleType&gt;
             *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
             *             &lt;/restriction&gt;
             *           &lt;/simpleType&gt;
             *         &lt;/element&gt;
             *         &lt;element name="DControllerFactor" minOccurs="0"&gt;
             *           &lt;simpleType&gt;
             *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
             *             &lt;/restriction&gt;
             *           &lt;/simpleType&gt;
             *         &lt;/element&gt;
             *       &lt;/sequence&gt;
             *     &lt;/restriction&gt;
             *   &lt;/complexContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "initalCrestLevel",
                "listofReferenceNodes",
                "waterLevel",
                "pControllerFactor",
                "dControllerFactor"
            })
            public static class WaterLevelControlled {

                @XmlElement(name = "InitalCrestLevel")
                protected double initalCrestLevel;
                @XmlElement(name = "ListofReferenceNodes", required = true)
                protected TListOfNodes listofReferenceNodes;
                @XmlElement(name = "WaterLevel", required = true)
                protected TWeirType.BroadCrestedWeir.CrestLevel.WaterLevelControlled.WaterLevel waterLevel;
                @XmlElement(name = "PControllerFactor")
                protected Double pControllerFactor;
                @XmlElement(name = "DControllerFactor")
                protected Double dControllerFactor;

                /**
                 * Ruft den Wert der initalCrestLevel-Eigenschaft ab.
                 * 
                 */
                public double getInitalCrestLevel() {
                    return initalCrestLevel;
                }

                /**
                 * Legt den Wert der initalCrestLevel-Eigenschaft fest.
                 * 
                 */
                public void setInitalCrestLevel(double value) {
                    this.initalCrestLevel = value;
                }

                /**
                 * Ruft den Wert der listofReferenceNodes-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link TListOfNodes }
                 *     
                 */
                public TListOfNodes getListofReferenceNodes() {
                    return listofReferenceNodes;
                }

                /**
                 * Legt den Wert der listofReferenceNodes-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link TListOfNodes }
                 *     
                 */
                public void setListofReferenceNodes(TListOfNodes value) {
                    this.listofReferenceNodes = value;
                }

                /**
                 * Ruft den Wert der waterLevel-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link TWeirType.BroadCrestedWeir.CrestLevel.WaterLevelControlled.WaterLevel }
                 *     
                 */
                public TWeirType.BroadCrestedWeir.CrestLevel.WaterLevelControlled.WaterLevel getWaterLevel() {
                    return waterLevel;
                }

                /**
                 * Legt den Wert der waterLevel-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link TWeirType.BroadCrestedWeir.CrestLevel.WaterLevelControlled.WaterLevel }
                 *     
                 */
                public void setWaterLevel(TWeirType.BroadCrestedWeir.CrestLevel.WaterLevelControlled.WaterLevel value) {
                    this.waterLevel = value;
                }

                /**
                 * Ruft den Wert der pControllerFactor-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link Double }
                 *     
                 */
                public Double getPControllerFactor() {
                    return pControllerFactor;
                }

                /**
                 * Legt den Wert der pControllerFactor-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link Double }
                 *     
                 */
                public void setPControllerFactor(Double value) {
                    this.pControllerFactor = value;
                }

                /**
                 * Ruft den Wert der dControllerFactor-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link Double }
                 *     
                 */
                public Double getDControllerFactor() {
                    return dControllerFactor;
                }

                /**
                 * Legt den Wert der dControllerFactor-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link Double }
                 *     
                 */
                public void setDControllerFactor(Double value) {
                    this.dControllerFactor = value;
                }


                /**
                 * <p>Java-Klasse für anonymous complex type.
                 * 
                 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
                 * 
                 * <pre>
                 * &lt;complexType&gt;
                 *   &lt;complexContent&gt;
                 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
                 *       &lt;choice&gt;
                 *         &lt;element name="Constant"&gt;
                 *           &lt;simpleType&gt;
                 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
                 *             &lt;/restriction&gt;
                 *           &lt;/simpleType&gt;
                 *         &lt;/element&gt;
                 *         &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
                 *       &lt;/choice&gt;
                 *     &lt;/restriction&gt;
                 *   &lt;/complexContent&gt;
                 * &lt;/complexType&gt;
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "constant",
                    "timeSeries"
                })
                public static class WaterLevel {

                    @XmlElement(name = "Constant")
                    protected Double constant;
                    @XmlElement(name = "TimeSeries")
                    protected TTimeSeries timeSeries;

                    /**
                     * Ruft den Wert der constant-Eigenschaft ab.
                     * 
                     * @return
                     *     possible object is
                     *     {@link Double }
                     *     
                     */
                    public Double getConstant() {
                        return constant;
                    }

                    /**
                     * Legt den Wert der constant-Eigenschaft fest.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link Double }
                     *     
                     */
                    public void setConstant(Double value) {
                        this.constant = value;
                    }

                    /**
                     * Ruft den Wert der timeSeries-Eigenschaft ab.
                     * 
                     * @return
                     *     possible object is
                     *     {@link TTimeSeries }
                     *     
                     */
                    public TTimeSeries getTimeSeries() {
                        return timeSeries;
                    }

                    /**
                     * Legt den Wert der timeSeries-Eigenschaft fest.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link TTimeSeries }
                     *     
                     */
                    public void setTimeSeries(TTimeSeries value) {
                        this.timeSeries = value;
                    }

                }

            }

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="Opening"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;choice&gt;
     *                   &lt;element name="Constant"&gt;
     *                     &lt;simpleType&gt;
     *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
     *                       &lt;/restriction&gt;
     *                     &lt;/simpleType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="TimeSeriesOfOpening" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
     *                   &lt;element name="WaterLevelControlled"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="InitalOpening"&gt;
     *                               &lt;simpleType&gt;
     *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
     *                                 &lt;/restriction&gt;
     *                               &lt;/simpleType&gt;
     *                             &lt;/element&gt;
     *                             &lt;element name="ListofReferenceNodes" type="{http://www.example.org/Weirs}TListOfNodes"/&gt;
     *                             &lt;element name="WaterLevel"&gt;
     *                               &lt;complexType&gt;
     *                                 &lt;complexContent&gt;
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                                     &lt;choice&gt;
     *                                       &lt;element name="Constant"&gt;
     *                                         &lt;simpleType&gt;
     *                                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
     *                                           &lt;/restriction&gt;
     *                                         &lt;/simpleType&gt;
     *                                       &lt;/element&gt;
     *                                       &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
     *                                     &lt;/choice&gt;
     *                                   &lt;/restriction&gt;
     *                                 &lt;/complexContent&gt;
     *                               &lt;/complexType&gt;
     *                             &lt;/element&gt;
     *                             &lt;element name="PControllerFactor" minOccurs="0"&gt;
     *                               &lt;simpleType&gt;
     *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
     *                                 &lt;/restriction&gt;
     *                               &lt;/simpleType&gt;
     *                             &lt;/element&gt;
     *                             &lt;element name="DControllerFactor" minOccurs="0"&gt;
     *                               &lt;simpleType&gt;
     *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
     *                                 &lt;/restriction&gt;
     *                               &lt;/simpleType&gt;
     *                             &lt;/element&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                 &lt;/choice&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "name",
        "opening"
    })
    public static class NeedleWeir {

        @XmlElement(name = "Name")
        protected String name;
        @XmlElement(name = "Opening", required = true)
        protected TWeirType.NeedleWeir.Opening opening;

        /**
         * Ruft den Wert der name-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getName() {
            return name;
        }

        /**
         * Legt den Wert der name-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setName(String value) {
            this.name = value;
        }

        /**
         * Ruft den Wert der opening-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link TWeirType.NeedleWeir.Opening }
         *     
         */
        public TWeirType.NeedleWeir.Opening getOpening() {
            return opening;
        }

        /**
         * Legt den Wert der opening-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link TWeirType.NeedleWeir.Opening }
         *     
         */
        public void setOpening(TWeirType.NeedleWeir.Opening value) {
            this.opening = value;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;complexContent&gt;
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *       &lt;choice&gt;
         *         &lt;element name="Constant"&gt;
         *           &lt;simpleType&gt;
         *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
         *             &lt;/restriction&gt;
         *           &lt;/simpleType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="TimeSeriesOfOpening" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
         *         &lt;element name="WaterLevelControlled"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="InitalOpening"&gt;
         *                     &lt;simpleType&gt;
         *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
         *                       &lt;/restriction&gt;
         *                     &lt;/simpleType&gt;
         *                   &lt;/element&gt;
         *                   &lt;element name="ListofReferenceNodes" type="{http://www.example.org/Weirs}TListOfNodes"/&gt;
         *                   &lt;element name="WaterLevel"&gt;
         *                     &lt;complexType&gt;
         *                       &lt;complexContent&gt;
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                           &lt;choice&gt;
         *                             &lt;element name="Constant"&gt;
         *                               &lt;simpleType&gt;
         *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
         *                                 &lt;/restriction&gt;
         *                               &lt;/simpleType&gt;
         *                             &lt;/element&gt;
         *                             &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
         *                           &lt;/choice&gt;
         *                         &lt;/restriction&gt;
         *                       &lt;/complexContent&gt;
         *                     &lt;/complexType&gt;
         *                   &lt;/element&gt;
         *                   &lt;element name="PControllerFactor" minOccurs="0"&gt;
         *                     &lt;simpleType&gt;
         *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
         *                       &lt;/restriction&gt;
         *                     &lt;/simpleType&gt;
         *                   &lt;/element&gt;
         *                   &lt;element name="DControllerFactor" minOccurs="0"&gt;
         *                     &lt;simpleType&gt;
         *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
         *                       &lt;/restriction&gt;
         *                     &lt;/simpleType&gt;
         *                   &lt;/element&gt;
         *                 &lt;/sequence&gt;
         *               &lt;/restriction&gt;
         *             &lt;/complexContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *       &lt;/choice&gt;
         *     &lt;/restriction&gt;
         *   &lt;/complexContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "constant",
            "timeSeriesOfOpening",
            "waterLevelControlled"
        })
        public static class Opening {

            @XmlElement(name = "Constant")
            protected Double constant;
            @XmlElement(name = "TimeSeriesOfOpening")
            protected TTimeSeries timeSeriesOfOpening;
            @XmlElement(name = "WaterLevelControlled")
            protected TWeirType.NeedleWeir.Opening.WaterLevelControlled waterLevelControlled;

            /**
             * Ruft den Wert der constant-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Double }
             *     
             */
            public Double getConstant() {
                return constant;
            }

            /**
             * Legt den Wert der constant-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Double }
             *     
             */
            public void setConstant(Double value) {
                this.constant = value;
            }

            /**
             * Ruft den Wert der timeSeriesOfOpening-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link TTimeSeries }
             *     
             */
            public TTimeSeries getTimeSeriesOfOpening() {
                return timeSeriesOfOpening;
            }

            /**
             * Legt den Wert der timeSeriesOfOpening-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link TTimeSeries }
             *     
             */
            public void setTimeSeriesOfOpening(TTimeSeries value) {
                this.timeSeriesOfOpening = value;
            }

            /**
             * Ruft den Wert der waterLevelControlled-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link TWeirType.NeedleWeir.Opening.WaterLevelControlled }
             *     
             */
            public TWeirType.NeedleWeir.Opening.WaterLevelControlled getWaterLevelControlled() {
                return waterLevelControlled;
            }

            /**
             * Legt den Wert der waterLevelControlled-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link TWeirType.NeedleWeir.Opening.WaterLevelControlled }
             *     
             */
            public void setWaterLevelControlled(TWeirType.NeedleWeir.Opening.WaterLevelControlled value) {
                this.waterLevelControlled = value;
            }


            /**
             * <p>Java-Klasse für anonymous complex type.
             * 
             * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
             * 
             * <pre>
             * &lt;complexType&gt;
             *   &lt;complexContent&gt;
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *       &lt;sequence&gt;
             *         &lt;element name="InitalOpening"&gt;
             *           &lt;simpleType&gt;
             *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
             *             &lt;/restriction&gt;
             *           &lt;/simpleType&gt;
             *         &lt;/element&gt;
             *         &lt;element name="ListofReferenceNodes" type="{http://www.example.org/Weirs}TListOfNodes"/&gt;
             *         &lt;element name="WaterLevel"&gt;
             *           &lt;complexType&gt;
             *             &lt;complexContent&gt;
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *                 &lt;choice&gt;
             *                   &lt;element name="Constant"&gt;
             *                     &lt;simpleType&gt;
             *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
             *                       &lt;/restriction&gt;
             *                     &lt;/simpleType&gt;
             *                   &lt;/element&gt;
             *                   &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
             *                 &lt;/choice&gt;
             *               &lt;/restriction&gt;
             *             &lt;/complexContent&gt;
             *           &lt;/complexType&gt;
             *         &lt;/element&gt;
             *         &lt;element name="PControllerFactor" minOccurs="0"&gt;
             *           &lt;simpleType&gt;
             *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
             *             &lt;/restriction&gt;
             *           &lt;/simpleType&gt;
             *         &lt;/element&gt;
             *         &lt;element name="DControllerFactor" minOccurs="0"&gt;
             *           &lt;simpleType&gt;
             *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
             *             &lt;/restriction&gt;
             *           &lt;/simpleType&gt;
             *         &lt;/element&gt;
             *       &lt;/sequence&gt;
             *     &lt;/restriction&gt;
             *   &lt;/complexContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "initalOpening",
                "listofReferenceNodes",
                "waterLevel",
                "pControllerFactor",
                "dControllerFactor"
            })
            public static class WaterLevelControlled {

                @XmlElement(name = "InitalOpening")
                protected double initalOpening;
                @XmlElement(name = "ListofReferenceNodes", required = true)
                protected TListOfNodes listofReferenceNodes;
                @XmlElement(name = "WaterLevel", required = true)
                protected TWeirType.NeedleWeir.Opening.WaterLevelControlled.WaterLevel waterLevel;
                @XmlElement(name = "PControllerFactor")
                protected Double pControllerFactor;
                @XmlElement(name = "DControllerFactor")
                protected Double dControllerFactor;

                /**
                 * Ruft den Wert der initalOpening-Eigenschaft ab.
                 * 
                 */
                public double getInitalOpening() {
                    return initalOpening;
                }

                /**
                 * Legt den Wert der initalOpening-Eigenschaft fest.
                 * 
                 */
                public void setInitalOpening(double value) {
                    this.initalOpening = value;
                }

                /**
                 * Ruft den Wert der listofReferenceNodes-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link TListOfNodes }
                 *     
                 */
                public TListOfNodes getListofReferenceNodes() {
                    return listofReferenceNodes;
                }

                /**
                 * Legt den Wert der listofReferenceNodes-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link TListOfNodes }
                 *     
                 */
                public void setListofReferenceNodes(TListOfNodes value) {
                    this.listofReferenceNodes = value;
                }

                /**
                 * Ruft den Wert der waterLevel-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link TWeirType.NeedleWeir.Opening.WaterLevelControlled.WaterLevel }
                 *     
                 */
                public TWeirType.NeedleWeir.Opening.WaterLevelControlled.WaterLevel getWaterLevel() {
                    return waterLevel;
                }

                /**
                 * Legt den Wert der waterLevel-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link TWeirType.NeedleWeir.Opening.WaterLevelControlled.WaterLevel }
                 *     
                 */
                public void setWaterLevel(TWeirType.NeedleWeir.Opening.WaterLevelControlled.WaterLevel value) {
                    this.waterLevel = value;
                }

                /**
                 * Ruft den Wert der pControllerFactor-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link Double }
                 *     
                 */
                public Double getPControllerFactor() {
                    return pControllerFactor;
                }

                /**
                 * Legt den Wert der pControllerFactor-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link Double }
                 *     
                 */
                public void setPControllerFactor(Double value) {
                    this.pControllerFactor = value;
                }

                /**
                 * Ruft den Wert der dControllerFactor-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link Double }
                 *     
                 */
                public Double getDControllerFactor() {
                    return dControllerFactor;
                }

                /**
                 * Legt den Wert der dControllerFactor-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link Double }
                 *     
                 */
                public void setDControllerFactor(Double value) {
                    this.dControllerFactor = value;
                }


                /**
                 * <p>Java-Klasse für anonymous complex type.
                 * 
                 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
                 * 
                 * <pre>
                 * &lt;complexType&gt;
                 *   &lt;complexContent&gt;
                 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
                 *       &lt;choice&gt;
                 *         &lt;element name="Constant"&gt;
                 *           &lt;simpleType&gt;
                 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
                 *             &lt;/restriction&gt;
                 *           &lt;/simpleType&gt;
                 *         &lt;/element&gt;
                 *         &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
                 *       &lt;/choice&gt;
                 *     &lt;/restriction&gt;
                 *   &lt;/complexContent&gt;
                 * &lt;/complexType&gt;
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "constant",
                    "timeSeries"
                })
                public static class WaterLevel {

                    @XmlElement(name = "Constant")
                    protected Double constant;
                    @XmlElement(name = "TimeSeries")
                    protected TTimeSeries timeSeries;

                    /**
                     * Ruft den Wert der constant-Eigenschaft ab.
                     * 
                     * @return
                     *     possible object is
                     *     {@link Double }
                     *     
                     */
                    public Double getConstant() {
                        return constant;
                    }

                    /**
                     * Legt den Wert der constant-Eigenschaft fest.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link Double }
                     *     
                     */
                    public void setConstant(Double value) {
                        this.constant = value;
                    }

                    /**
                     * Ruft den Wert der timeSeries-Eigenschaft ab.
                     * 
                     * @return
                     *     possible object is
                     *     {@link TTimeSeries }
                     *     
                     */
                    public TTimeSeries getTimeSeries() {
                        return timeSeries;
                    }

                    /**
                     * Legt den Wert der timeSeries-Eigenschaft fest.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link TTimeSeries }
                     *     
                     */
                    public void setTimeSeries(TTimeSeries value) {
                        this.timeSeries = value;
                    }

                }

            }

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="OpeningHight" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "name",
        "openingHight"
    })
    public static class RadialGate {

        @XmlElement(name = "Name")
        protected String name;
        @XmlElement(name = "OpeningHight")
        protected double openingHight;

        /**
         * Ruft den Wert der name-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getName() {
            return name;
        }

        /**
         * Legt den Wert der name-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setName(String value) {
            this.name = value;
        }

        /**
         * Ruft den Wert der openingHight-Eigenschaft ab.
         * 
         */
        public double getOpeningHight() {
            return openingHight;
        }

        /**
         * Legt den Wert der openingHight-Eigenschaft fest.
         * 
         */
        public void setOpeningHight(double value) {
            this.openingHight = value;
        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="SluiceLevel"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;choice&gt;
     *                   &lt;element name="Constant"&gt;
     *                     &lt;simpleType&gt;
     *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
     *                       &lt;/restriction&gt;
     *                     &lt;/simpleType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
     *                 &lt;/choice&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "name",
        "sluiceLevel"
    })
    public static class UnderFlowTopoWeir {

        @XmlElement(name = "Name")
        protected String name;
        @XmlElement(name = "SluiceLevel", required = true)
        protected TWeirType.UnderFlowTopoWeir.SluiceLevel sluiceLevel;

        /**
         * Ruft den Wert der name-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getName() {
            return name;
        }

        /**
         * Legt den Wert der name-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setName(String value) {
            this.name = value;
        }

        /**
         * Ruft den Wert der sluiceLevel-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link TWeirType.UnderFlowTopoWeir.SluiceLevel }
         *     
         */
        public TWeirType.UnderFlowTopoWeir.SluiceLevel getSluiceLevel() {
            return sluiceLevel;
        }

        /**
         * Legt den Wert der sluiceLevel-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link TWeirType.UnderFlowTopoWeir.SluiceLevel }
         *     
         */
        public void setSluiceLevel(TWeirType.UnderFlowTopoWeir.SluiceLevel value) {
            this.sluiceLevel = value;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;complexContent&gt;
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *       &lt;choice&gt;
         *         &lt;element name="Constant"&gt;
         *           &lt;simpleType&gt;
         *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
         *             &lt;/restriction&gt;
         *           &lt;/simpleType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
         *       &lt;/choice&gt;
         *     &lt;/restriction&gt;
         *   &lt;/complexContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "constant",
            "timeSeries"
        })
        public static class SluiceLevel {

            @XmlElement(name = "Constant")
            protected Double constant;
            @XmlElement(name = "TimeSeries")
            protected TTimeSeries timeSeries;

            /**
             * Ruft den Wert der constant-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Double }
             *     
             */
            public Double getConstant() {
                return constant;
            }

            /**
             * Legt den Wert der constant-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Double }
             *     
             */
            public void setConstant(Double value) {
                this.constant = value;
            }

            /**
             * Ruft den Wert der timeSeries-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link TTimeSeries }
             *     
             */
            public TTimeSeries getTimeSeries() {
                return timeSeries;
            }

            /**
             * Legt den Wert der timeSeries-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link TTimeSeries }
             *     
             */
            public void setTimeSeries(TTimeSeries value) {
                this.timeSeries = value;
            }

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="SluiceLevel"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;choice&gt;
     *                   &lt;element name="Constant"&gt;
     *                     &lt;simpleType&gt;
     *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
     *                       &lt;/restriction&gt;
     *                     &lt;/simpleType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
     *                 &lt;/choice&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "name",
        "sluiceLevel"
    })
    public static class UnderFlowWeir {

        @XmlElement(name = "Name")
        protected String name;
        @XmlElement(name = "SluiceLevel", required = true)
        protected TWeirType.UnderFlowWeir.SluiceLevel sluiceLevel;

        /**
         * Ruft den Wert der name-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getName() {
            return name;
        }

        /**
         * Legt den Wert der name-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setName(String value) {
            this.name = value;
        }

        /**
         * Ruft den Wert der sluiceLevel-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link TWeirType.UnderFlowWeir.SluiceLevel }
         *     
         */
        public TWeirType.UnderFlowWeir.SluiceLevel getSluiceLevel() {
            return sluiceLevel;
        }

        /**
         * Legt den Wert der sluiceLevel-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link TWeirType.UnderFlowWeir.SluiceLevel }
         *     
         */
        public void setSluiceLevel(TWeirType.UnderFlowWeir.SluiceLevel value) {
            this.sluiceLevel = value;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;complexContent&gt;
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *       &lt;choice&gt;
         *         &lt;element name="Constant"&gt;
         *           &lt;simpleType&gt;
         *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
         *             &lt;/restriction&gt;
         *           &lt;/simpleType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="TimeSeries" type="{http://www.example.org/Weirs}TTimeSeries"/&gt;
         *       &lt;/choice&gt;
         *     &lt;/restriction&gt;
         *   &lt;/complexContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "constant",
            "timeSeries"
        })
        public static class SluiceLevel {

            @XmlElement(name = "Constant")
            protected Double constant;
            @XmlElement(name = "TimeSeries")
            protected TTimeSeries timeSeries;

            /**
             * Ruft den Wert der constant-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Double }
             *     
             */
            public Double getConstant() {
                return constant;
            }

            /**
             * Legt den Wert der constant-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Double }
             *     
             */
            public void setConstant(Double value) {
                this.constant = value;
            }

            /**
             * Ruft den Wert der timeSeries-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link TTimeSeries }
             *     
             */
            public TTimeSeries getTimeSeries() {
                return timeSeries;
            }

            /**
             * Legt den Wert der timeSeries-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link TTimeSeries }
             *     
             */
            public void setTimeSeries(TTimeSeries value) {
                this.timeSeries = value;
            }

        }

    }

}
