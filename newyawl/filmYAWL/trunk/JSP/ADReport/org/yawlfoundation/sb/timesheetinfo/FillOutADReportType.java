//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.3-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.05.21 at 04:49:32 PM EST 
//


package org.yawlfoundation.sb.timesheetinfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Fill_Out_AD_ReportType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Fill_Out_AD_ReportType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="generalInfo" type="{http://www.yawlfoundation.org/sb/timeSheetInfo}generalInfoType"/>
 *         &lt;element name="producer" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="director" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="assistantDirector" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="timeSheetInfo" type="{http://www.yawlfoundation.org/sb/timeSheetInfo}timeSheetInfoType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Fill_Out_AD_ReportType", propOrder = {
    "generalInfo",
    "producer",
    "director",
    "assistantDirector",
    "timeSheetInfo"
})
public class FillOutADReportType {

    @XmlElement(required = true)
    protected GeneralInfoType generalInfo;
    @XmlElement(required = true)
    protected String producer;
    @XmlElement(required = true)
    protected String director;
    @XmlElement(required = true)
    protected String assistantDirector;
    @XmlElement(required = true)
    protected TimeSheetInfoType timeSheetInfo;

    /**
     * Gets the value of the generalInfo property.
     * 
     * @return
     *     possible object is
     *     {@link GeneralInfoType }
     *     
     */
    public GeneralInfoType getGeneralInfo() {
        return generalInfo;
    }

    /**
     * Sets the value of the generalInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link GeneralInfoType }
     *     
     */
    public void setGeneralInfo(GeneralInfoType value) {
        this.generalInfo = value;
    }

    /**
     * Gets the value of the producer property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProducer() {
        return producer;
    }

    /**
     * Sets the value of the producer property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProducer(String value) {
        this.producer = value;
    }

    /**
     * Gets the value of the director property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDirector() {
        return director;
    }

    /**
     * Sets the value of the director property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDirector(String value) {
        this.director = value;
    }

    /**
     * Gets the value of the assistantDirector property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAssistantDirector() {
        return assistantDirector;
    }

    /**
     * Sets the value of the assistantDirector property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAssistantDirector(String value) {
        this.assistantDirector = value;
    }

    /**
     * Gets the value of the timeSheetInfo property.
     * 
     * @return
     *     possible object is
     *     {@link TimeSheetInfoType }
     *     
     */
    public TimeSheetInfoType getTimeSheetInfo() {
        return timeSheetInfo;
    }

    /**
     * Sets the value of the timeSheetInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link TimeSheetInfoType }
     *     
     */
    public void setTimeSheetInfo(TimeSheetInfoType value) {
        this.timeSheetInfo = value;
    }

}