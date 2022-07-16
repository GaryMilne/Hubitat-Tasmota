/**
*  Tasmota Sync Universal Multi Sensor Driver - Dev
*  Version: v0.99.3
*  Download: See importUrl in definition
*  Description: Hubitat Driver for Tasmota Sensors. Provides Realtime and native synchronization between Hubitat and Tasmota
*
*  Copyright 2022 Gary J. Milne
*
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation.
*
*  This driver is one of several in the Tasmota Sync series. All of these drivers are architecturally similar and much of the code is identical.
*  To simplifiy maintenance all of these drivers have two sections. Search for the phrase "END OF UNIQUE FUNCTIONS" to find the split.
*  #1 The top section contains code that is UNIQUE to a specific driver such as a bulb vs a switch vs a dimmer. Although this code is UNIQUE it is very similar between drivers.
*  #2 The bottom section is code that is IDENTICAL and shared across all drivers and is about 700 - 800 lines of code. This section of code is referred to as CORE.
*
*  UNIVERSAL SENSOR - UNIQUE - CHANGELOG
*  Version 0.91 - Internal version
*  Version 0.92 - Added fixed logic for extracting for sensor SI7102
*  Version 0.93 - Virtualized the name of the sensor and added it to settings.
*  Version 0.93B - Fixed error in RULE3 where sensorname was not virtualized to the settings.sensorName but accidentally used an embedded string.
*  Version 0.94 - Added support for a broad range of sensors and capabilities. Driver will look for any data in sensor field and populate the attribute if found.
*  Version 0.95 - Changed sensor methodology to iterate all sensor fields, pair them with Hubitat attributes and update the data accordingly. Theoretically this makes the driver capable of working with any type of sensor and any data field with only adding a few names to the sensorMap FIELDS.
*  Version 0.96.0 - Changed versioning to comply with Semantic Versioning standards (https://semver.org/). Moved CORE changelog to beginning of CORE section. Added links 
*  Version 0.98.0 - All versions incremented and synchronised for HPM plublication
*  Version 0.98.1 - Added definitions for PM2.5 sensor data. Add logic to statusResponse() to handle a STATUSSNS "switch1" field used by some sensors.
*  Version 0.98.11 - Added definitions for ANALOG sensor data. Added map for "RANGE" to "range" which is used by analog inputs.
*  Version 0.98.3 - Virtualized all sensor and trigger names
*  Version 0.98.4 - Added support for load sensors
*  Version 0.98.5 - Added capability to evaluate sensor output for testing
*  Version 0.99.1 - Added support to handle ANALOG CTENERGY JSON inputs
*  Version 0.99.2 - Added support to handle two TEMPERATURE sensors. Reporting as temperature and temperature1.
*  Version 0.99.3 - Added function adjustBody for pre-processing of ANALOG JSON inputs as well as de-duping Temperature fields.
*
* Authors Notes:
* For more information on Tasmota Sync drivers check out these resources:
* Original posting on Hubitat Community forum.  https://community.hubitat.com/t/tasmota-sync-drivers-native-and-real-time-synchronization-between-hubitat-and-tasmota-11/93651
* How to upgrade from Tasmota 8.X to Tasmota 11.X  https://github.com/GaryMilne/Hubitat-Tasmota/blob/main/How%20to%20Upgrade%20from%20Tasmota%20from%208.X%20to%2011.X.pdf
* Tasmota Sync Installation and Use Guide https://github.com/GaryMilne/Hubitat-Tasmota/blob/main/Tasmota%20Sync%20Documentation.pdf
*
*  Gary Milne - June, 2022
*
**/

import groovy.json.JsonSlurper
import groovy.transform.Field

sensorType = "All"
//sensorType = "Accelerometer"
//sensorType = "AirQuality"
//sensorType = "Analog"
//sensorType = "Chemical"
//sensorType = "Energy"
//sensorType = "Environmental"
//sensorType = "Flow"
//sensorType = "IR"
//sensorType = "IO"
//sensorType = "Light_Gesture_Sensor"
//sensorType = "Load"
//sensorType = "NFC"
//sensorType = "Rain"
//sensorType = "RF"

//First item in the pair is the name of the Tasmota sensor data type in uppercase form. The second name is the driver attribute that will contain the data. They may be identical in some case and very different in others.
@Field static final sensor2AttributeMap = ['TEMPERATURE' : 'temperature', 'TEMPERATURE1' : 'temperature1','HUMIDITY' : 'humidity', 
                                           'ACCELXAXIS' : 'accelXAxis' , 'ACCELYAXIS' : 'accelYAxis' , 'ACCELZAXIS' : 'accelZAxis' , 'GYROXAXIS' : 'gyroXAxis', 'GYROYAXIS' : 'gyroYAxis', 'GYROZAXIS' : 'gyroZAxis' , 'YAW' : 'yaw', 'PITCH' : 'pitch', 'ROLL' : 'roll',  //Accelerometer - Gyro
                                           'AIRQUALITYINDEX' : 'airQualityIndex', 'ECO2' : 'ECO2', 'ECO' : 'ECO', 'PM2.5' : 'pm25' , 'RESISTANCE' : 'resistance', 'TVOC' : 'tvoc',  //Air Quality
                                           'RANGE' : 'range', //Analog input
                                           'PH' : 'pH',         //Chemical
                                           'DISTANCE' : 'distance', //Physical
                                           'CURRENT' : 'current', 'POWER' : 'power' , 'VOLTAGE' : 'voltage', 'TOTAL' : 'total', 'YESTERDAY' : 'yesterday', 'TODAY' : 'today', 'APPARENTPOWER' : 'apparentPower', 'REACTIVEPOWER' : 'reactivePower' , 'FACTOR' : 'factor',  'FREQUENCY' : 'frequency', 'PERIOD' : 'period', 'TOTALSTARTTIME' : 'totalStartTime', //Electrical Energy
                                           'DEWPOINT' : 'dewPoint', 'ILLUMINANCE' : 'illuminance' , 'PRESSURE' : 'pressure', 'GAS' : 'gas', 'ULTRAVIOLET' : 'ultraViolet', 'SOUNDPRESSURELEVEL' : 'soundPressureLevel',    //Environmental
                                           'FLOW' : 'rate',    //Flow
                                           'D0' : 'd0', 'D1' : 'd1', 'D2' : 'd2', 'D3' : 'd3', 'D4' : 'd4', 'D5' : 'd5', 'D6' : 'd6', 'D7' : 'd7', 'MS' : 'ms',     //  I/O Expansion
                                           'PROTOCOL' : 'protocol', 'BITS' : 'bits', 'DATA' : 'data',    //Infra Red
                                           'RED' : 'red', 'BLUE' : 'blue', 'GREEN' : 'green', 'AMBIENT' : 'ambient', 'CCT' : 'cct' , 'PROXIMITY' : 'proximity',     //Some kind of gesture sensor.
                                           'OBJTMP' : 'objTmp', 'AMBTMP' : 'ambTmp',     //Infra Red Thermometer
                                           'EVENT' : 'event' , 'ENERGY' : 'energy', 'STAGE' : 'stage', //Lightning sensor. Note that distance is also part of lightning but is already defined.
                                           'WEIGHT' : 'weight' , 'WEIGHTRAW' : 'weightRaw' , 'ABSRAW' : 'absRaw', //Load Sensor
                                           'UID' : 'uid', //NFC. Note that data is also a part of NFC but has already been defined elsewhere.
                                           'ACTIVE' : 'active', 'FLOWRATE' : 'flowRate',  //Rain. Note that event and total are also part of RAIN but are defined elsewhere.
                                           'PULSE' : 'pulse',  //RF sensor. Note that data, bits and protocol are all defined elsewhere. 
                                           'TYPE' : 'type'     //RFID. Note that uid and data are defined elsewhere.
                                          ]

//If a Tasmota device has one of these fields in its StatusSNS then a Trigger will be created for that field.
@Field static final triggerEligibleList =  ['TEMPERATURE', 'TEMPERATURE1', 'HUMIDITY', 'DEWPOINT',
                                           'ACCELXAXIS', 'ACCELYAXIS', 'ACCELZAXIS', 'GYROXAXIS', 'GYROYAXIS', 'GYROZAXIS', 'YAW', 'ROLL', 'PITCH', //Accelerometer - Gyro
                                           'AIRQUALITYINDEX', 'ECO2', 'ECO', 'PM2.5', 'RESISTANCE', 'TVOC',  //Air Quality
                                           'ANALOG', 'RANGE',    //Analog inputs
                                           'PH',        //Chemical
                                           'DISTANCE', //Physical
                                           'CURRENT', 'POWER', 'VOLTAGE', //Electrical Energy
                                           'ILLUMINANCE', 'PRESSURE', 'GAS', 'ULTRAVIOLET', 'SOUNDPRESSURELEVEL',    //Environmental
                                           'FLOW',    //Flow
                                           'D0', 'D1', 'D2', 'D3', 'D4', 'D5', 'D6', 'D7',     //  I/O Expansion
                                           'DATA',    //Multiple
                                           'RED', 'BLUE', 'GREEN', 'AMBIENT', 'CCT', 'PROXIMITY',     //Some kind of gesture sensor.
                                           'OBJTMP', 'AMBTMP',     //Infra Red Thermometer
                                           'EVENT', 'STAGE', 'ENERGY', //Lightning sensor. Note that distance is also part of lightning but is already defined.
                                           'WEIGHT', 'WEIGHTRAW', 'ABSRAW', //Load Sensor
                                           //NFC. Note that data is also a part of NFC but has already been defined elsewhere.
                                           'ACTIVE', 'FLOWRATE',  //Rain. Note that event and total are also part of RAIN but are defined elsewhere.
                                           'PULSE'  //RF sensor. Note that data, bits and protocol are all defined elsewhere. 
                                           ]

//First item in the pair is the name of the Tasmota sensor data type in uppercase form. The second name is the name of the driver unit attribute for that type of data.  For example 'tempUnit' will contain either a 'C' or 'F' if temperature is a valid data field.
@Field static final sensor2UnitMap = ['TEMPERATURE' : 'tempUnit', 'PRESSURE' : 'pressureUnit', 'SPEED' : 'speedUnit']
//First item in the pair is the name of the Tasmota sensor in uppercase form. The second name is suffix commonly associated with this type of data. For examples, degrees (°) applies to both C and F. Currently these suffixes are only used for event log entries.
@Field static final sensor2UnitSuffix = ['TEMPERATURE' : 'Degrees', 'TEMPERATURE1' : 'Degrees', 'HUMIDITY' : '% RH', 'DEWPOINT' : 'Degrees', 'ILLUMINANCE' : 'lux', 'PM2.5' : 'µg/M³']

//Tasmota<<-->>Hubitat discrepancies. This driver uses the Tasmota names but will mirror those values to the expected Hubitat attributes. First value is tasmota name followed by the Hubitat attribute
@Field static final mirroredAttributes = ['POWER' : 'energy', 'CURRENT' : 'amperage', 'ECO' : 'carbonMonoxide' , 'ECO2' : 'carbonDioxide' , 'FLOW' : 'rate']

metadata {
		definition (name: "Tasmota Sync - Universal Multi Sensor", namespace: "garyjmilne", author: "Gary J. Milne", importUrl: "https://raw.githubusercontent.com/GaryMilne/Hubitat-Tasmota/main/Universal_Multi_Sensor.groovy", singleThreaded: true )  {
        //capability "LiquidFlowRate"
        //capability "PressureMeasurement"
        capability "Refresh"
        capability "Sensor"
  
        command "initialize"
        command "tasmotaInjectRule"
        command "tasmotaCustomCommand", [ [name:"Command*", type: "STRING", description: "A single word command to be issued such as COLOR, CT, DIMMER etc."], [name:"Parameter", type: "STRING", description: "A single parameter that accompanies the command such as FFFFFFFF, 350, 75 etc."] ]
        command "tasmotaTelePeriod", [ [name:"Seconds*", type: "STRING", description: "The number of seconds between Tasmota data updates (TelePeriod XX)."] ]
        command "test"
        command "evaluateSensorData", [ [name:"STATUS_8*", type: "STRING", description: "The STATUS 8 output from a Tasmota device in the form {\"STATUSSNS\":{\"Time\": \"2019-11-03T19:34:28\",\"BME280\": {\"Temperature\": 21.7,\"Humidity\": 66.6,\"Pressure\": 988.6},\"PressureUnit\": \"hPa\",\"TempUnit\": \"C\"}}}" ] ]
        command "clearAttributes", [[name:"Message: Clears all of the Attributes. Do browser refresh.", type: "BOOLEAN", description: ""]]
        
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"
        
        //Attributes do not display in the Attribute State when they are null.
        //It does not seem to matter if an attribute is declared multiple times.
        //Adding a new attribute for sensor use should also be accompanied by entries in the sensor2AttributeMap, sensor2UnitMap and sensor2UnitSuffix
        attribute "Status", "string"
            
        //Attributes. These are usually at the sensor level of the STATUSSNS message.
        attribute "humidity", "number"               //unit %rh               //Tasmota example: {"Time":"2022-05-17T03:33:05","SI7021":{"Temperature":69,"Humidity":28,"DewPoint":34},"TempUnit":"F"}
        attribute "temperature", "number"            //unit F || C            //Tasmota example: {"Time":"2022-05-17T03:33:05","SI7021":{"Temperature":69,"Humidity":28,"DewPoint":34},"TempUnit":"F"}
        attribute "temperature1", "number"            //unit F || C            //Tasmota example: {"Time":"2022-05-17T03:33:05","SI7021":{"Temperature":69,"Humidity":28,"DewPoint":34},"TempUnit":"F"}
            
        //Descriptors. These are at the top level of the STATUSSNS message.
        attribute "switch1", "string"           //Tasmota example: Some "SENSOR" devices have a switch. The RCWL-0516 STATUS 8 output is: {"StatusSNS":{"Time":"2022-05-25T10:23:58","Switch1":"ON"}}  I'm sure others use the same or similar, possibly even a switch array.
        attribute "switch2", "string"           //Tasmota example: Some "SENSOR" devices have a second switch. '{"STATUSSNS":{"Time":"2022-01-07T19:43:24","Switch1":"ON","Switch2":"ON","ENERGY":{"TotalStartTime":"2021-04-03T18:52:24","Total":[0.005,1.507],"Yesterday":[0.000,0.648],"Today":[0.000,0.197],"Period":[ 0, 0],"Power":[ 0,26],"ApparentPower":[ 0,43],"ReactivePower":[ 0,33],"Factor":[0.00,0.62],"Frequency":50.016,"Voltage":225,"Current":[0.000,0.189]},"ESP32":{"Temperature":65.6},"TempUnit":"C"}}'
        attribute "pressureUnit", "string"      //Tasmota example: {"Time": "2019-11-03T19:34:28","BME280": {"Temperature": 21.7,"Humidity": 66.6,"Pressure": 988.6},"PressureUnit": "hPa","TempUnit": "C"}  -  Change with SetOption24 
        attribute "tempUnit", "string"          //Tasmota example: {"Time":"2022-05-17T03:33:05","SI7021":{"Temperature":69,"Humidity":28,"DewPoint":34},"TempUnit":"F"}  - Change with SetOption8. off is celsius and on is fahrenheit. Must do a refresh to update Hubitat.
        attribute "speedUnit", "string"        //Tasmota example: '{"Time": "2020-03-03T00:00:00+00:00","TX23": {"Speed": {"Act": 14.8,"Avg": 8.5,"Min": 12.2,"Max": 14.8},"Dir": {"Card": "WSW","Deg": 247.5,"Avg": 266.1,"AvgCard": "W","Min": 247.5,"Max": 247.5,"Range": 0}},"SpeedUnit": "km/h"}}}'
        
        log.debug ("Type ${sensorType}")
        //Accelerometer - '{"STATUSSNS":{"Time":"2019-12-10T19:37:50","MPU6050":{"Temperature":27.7,"AccelXAxis":-7568.00,"AccelYAxis":-776.00,"AccelZAxis":12812.00,"GyroXAxis":270.00,"GyroYAxis":-741.00,"GyroZAxis":700.00},"TempUnit":"C"}}'
        if ( sensorType == "All" || sensorType == "Accelerometer") {
            attribute "accelXAxis", "string"
            attribute "accelYAxis", "string"
            attribute "accelXAxis", "string"
            attribute "gyroXAxis", "string"
            attribute "gyroYAxis", "string"
            attribute "gyroZAxis", "string" 
            attribute "yaw", "string" 
            attribute "pitch", "string"
            attribute "roll", "string" 
	    }
        
        //Air Quality - '{"STATUSSNS":{"Time":"2020-01-01T00:00:00","IAQ":{"eCO2":450,"TVOC":125,"Resistance":76827}}}'
        if ( sensorType == "All" || sensorType == "AirQuality") {
            capability "AirQuality"
            capability "CarbonDioxideMeasurement"
            attribute "airQualityIndex", "number"  //range 0 - 500
            attribute "eCO", "number"              //Carbon Monoxide
            attribute "eCO2", "number"             //Carbon Dioxide
            attribute "pm25", "number"             //unit µg/M³
            attribute "resistance", "number"        
            attribute "tvoc", "number" 
            
            //Extra attributes for compatibility with Hubitat capabilities
            attribute "carbonDioxide", "number"    //unit ppm    carbonDioxide - NUMBER, unit:ppm
            attribute "carbonMonoxide", "string"   //unit    carbonMonoxide - ENUM ["clear", "tested", "detected"]
        }
        
        //ANALOG - '{"StatusSNS":{"Time":"2022-05-26T00:34:18","ANALOG":{"Range":9}}}'
        if ( sensorType == "All" || sensorType == "ANALOG") {
            attribute "range", "number"    //unit variable as this may be an analog input representing many unique things. //Tasmota example: {"StatusSNS":{"Time":"2022-05-26T00:34:18","ANALOG":{"Range":9}}}
            //CTEnergy - {"STATUSSNS":{"TIME":"2022-06-26T22:06:56","ANALOG":{"CTENERGY":{"ENERGY":2.882,"POWER":49,"VOLTAGE":230,"CURRENT":0.215}}}}.
            attribute "energy", "number"
            attribute "power", "number"
            attribute "voltage", "number"
            attribute "current", "number"
	    }
            
        //Chemical
        if ( sensorType == "All" || sensorType == "Chemical") {
            capability "pHMeasurement"
            attribute "pH", "number"                     //unit none provided
        }
            
        //Distance
        if ( sensorType == "All" || sensorType == "Distance") {
            attribute "distance", "number"                     //unit none provided
        }
            
        //Energy
        if ( sensorType == "All" || sensorType == "Energy") {
            capability "PowerMeter"
            capability "VoltageMeasurement"
            capability "CurrentMeter"
            attribute "current", "number"          //Unit A
            attribute "power", "number"            //Unit W
            attribute "voltage", "number"          //unit V
            attribute "total", "number"            //kWh
            attribute "yesterday", "number"        //kWh
            attribute "today", "number"            //kWh
            attribute "apparentPower", "number"    
            attribute "reactivePower", "number"
            attribute "factor", "number"
            attribute "frequency", "number"
            attribute "period", "number"
            attribute "totalStartTimePeriod", "string"
            
            //Extra attributes for compatibility with Hubitat capabilities
            attribute "amperage", "number"          //Unit A
            attribute "energy", "number"            //Unit W
        }
        
        //Environmental
        if ( sensorType == "All" || sensorType == "Environmental") {
            capability "IlluminanceMeasurement"
            capability "SoundPressureLevel"
            capability "UltravioletIndex"
            attribute "dewPoint", "number"              //Tasmota example: {"Time":"2022-05-17T03:33:05","SI7021":{"Temperature":69,"Humidity":28,"DewPoint":34},"TempUnit":"F"}  - Change with SetOption8. off is celsius and on is fahrenheit. Must do a refresh to update Hubitat.
            attribute "illuminance", "number"            //unit lx                //Tasmota example: {"Time":"2019-11-03T20:45:37","BH1750":{"Illuminance":79}}  {"Time":"2019-11-03T21:04:05","TSL2561":{"Illuminance":21.180}}
            attribute "pressure", "number"               //unit hPa || mmHg        //Tasmota example: {"Time": "2019-11-03T19:34:28","BME280": {"Temperature": 21.7,"Humidity": 66.6,"Pressure": 988.6},"PressureUnit": "hPa","TempUnit": "C"}
            attribute "gas", "number"                    //Seems to be the same as tvoc.  '{"STATUSSNS":{"Time": "2021-09-22T17:00:00","BME680": {"Temperature": 24.5,"Humidity":33.0,"DewPoint": 7.1,"Pressure": 987.7,"Gas": 1086.43 },"PressureUnit": "hPa","TempUnit": "C"}}'
            attribute "ultravioletIndex", "number"       //unit none provided
            attribute "soundPressureLevel", "number"     //unit dB    
        }           
        
        //Flow
        if ( sensorType == "All" || sensorType == "Flow") {
            capability "LiquidFlowRate"
            attribute "rate", "number"
        }
        
        //IO - '{"STATUSSNS":{"Time":"2018-08-18T16:13:47","MCP230XX": "D0":0,"D1":0,"D2":1,"D3":0,"D4":0,"D5":0,"D6":0,"D7":1}}}'
        if ( sensorType == "All" || sensorType == "IO") {
            attribute "d0", "number"
            attribute "d1", "number"
            attribute "d2", "number"
            attribute "d3", "number"
            attribute "d4", "number"
            attribute "d5", "number"
            attribute "d6", "number"
            attribute "d7", "number"
            attribute "ms", "number"
	    }
            
        //IR - '{"STATUSSNS":{"Time": "2019-01-01T00:00:00","IrReceived": {"Protocol": "NEC","Bits": 32,"Data": "0x00FF00FF"}}}'
        if ( sensorType == "All" || sensorType == "IR") {
            attribute "protocol", "string"
            attribute "bits", "number"
            attribute "data", "string"
	    }
            
        //Infra-Red (Far) Thermometer -'{"STATUSSNS":{"Time":"2019-11-11T00:03:30","MLX90614":{"OBJTMP":23.8,"AMBTMP":22.7}}}'
        if ( sensorType == "All" || sensorType == "IR_Thermometer") {
            attribute "objTmp", "number"
            attribute "ambTmp", "number"
	    }
            
        //Light_Gesture_Sensor - '{"STATUSSNS":{"Time":"2019-10-31T21:48:51","APDS9960":{"Red":282,"Green":252,"Blue":196,"Ambient":169,"CCT":4217,"Proximity":9}}}'
        if ( sensorType == "All" || sensorType == "Light_Gesture_Sensor") {
            attribute "red", "number"
            attribute "green", "number"
            attribute "blue", "number"
            attribute "ambient", "number"
            attribute "cct", "number"
            attribute "proximity", "number"
	    }
              
        //Lightning Sensor - '{"STATUSSNS":{"Time":"2020-01-01T17:07:07","AS3935":{"Event":4,"Distance":12,"Energy":58622,"Stage":1}}}'
        if ( sensorType == "All" || sensorType == "Lightning") {
            attribute "event", "number"
            attribute "distance", "number"
            attribute "energy", "number"
            attribute "stage", "number"
	    }
        
        //Load Sensor - '{"StatusSNS":{"Time":"2022-06-11T10:58:18","HX711":{"Weight":0,"WeightRaw":4782,"AbsRaw":110004}}}'
        if ( sensorType == "All" || sensorType == "Load") {
            attribute "weight", "number"
            attribute "weightRaw", "number"
            attribute "absRaw", "number"
	    }
            
        //NFC - '{"STATUSSNS":{"Time":"2019-01-10T18:31:39","PN532":{"UID":"94D8FC5F", "DATA":"ILOVETASMOTA"}}}'
        if ( sensorType == "All" || sensorType == "NFC") {
            attribute "uid", "string"
            attribute "data", "string"
	    }
            
         //Rain - '{"STATUSSNS":{"Time": "2021-08-25T17:15:45","RG-15": {"Active": 0.01,"Event": 0.13,"Total": 26.80,"FlowRate": 0.32},"TempUnit": "C"}}'
        if ( sensorType == "All" || sensorType == "Rain") {
            attribute "active", "number"
            attribute "event", "number"
            attribute "total", "number"
            attribute "flowRate", "number"
	    }
            
        //RF - '{"STATUSSNS":{"Time": "2019-01-01T00:00:00","RfReceived": {"Data": "0x7028D5","Bits": 24,"Protocol": 1,"Pulse": 238}}}'
        if ( sensorType == "All" || sensorType == "RF") {
            attribute "data", "string"
            attribute "bits", "number"
            attribute "protocol", "number"
            attribute "pulse", "number"
	    }
            
        //RFID - '{"STATUSSNS":{"Time":"2021-01-23T13:10:50","RC522":{"UID":"BA839D07","Data":"","Type":"MIFARE 1KB"}}}'
        if ( sensorType == "All" || sensorType == "RFID") {
            attribute "uid", "string"
            attribute "data", "string"
            attribute "type", "string"            
	    }

        section("Configure the Inputs"){
			input name: "destIP", type: "text", title: bold(dodgerBlue("Tasmota Device IP Address")), description: italic("The IP address of the Tasmota device."), defaultValue: "192.168.0.X", required:true, displayDuringSetup: true
            input name: "HubIP", type: "text", title: bold(dodgerBlue("Hubitat Hub IP Address")), description: italic("The Hubitat Hub Address. Used by Tasmota rules to send HTTP responses."), defaultValue: "192.168.0.X", required:true, displayDuringSetup: true
            input name: "timeout", type: "number", title: bold("Timeout for Tasmota reponse."), description: italic("Time in ms after which a Transaction is closed by the watchdog and subsequent responses will be ignored. Default 5000ms."), defaultValue: "5000", required:true, displayDuringSetup: false
            input name: "debounce", type: "number", title: bold("Debounce Interval for Tasmota Sync."), description: italic("The period in ms from command invocation during which a Tasmota Sync request will be ignored. Default 7000ms."), defaultValue: "7000", required:true, displayDuringSetup: false
            input name: "logging_level", type: "number", title: bold("Level of detail displayed in log"), description: italic("Enter log level 0-3. (Default is 0.)"), defaultValue: "0", required:true, displayDuringSetup: false            
	        input name: "loggingEnhancements", type: "enum", title: bold("Logging Enhancements."), description: italic("Allows log entries for this device to be enhanced with HTML tags for increased increased readability. (Default - All enhancements.)"),
                options: [ [0:" No enhancements."],[1:" Prepend log events with device name."],[2:" Enable HTML tags on logged events for this device."],[3:" Prepend log events with device name and enable HTML tags." ] ], defaultValue: 3, required:true
    		input name: "pollFrequency", type: "enum", title: bold("Poll Frequency. Polling not required if using Tasmota Sync on Tasmota 11."), description: italic("The time between Hubitat initiated synchronisation of values with Tasmota. Tasmota is considered authoritative (Default - 0 (Never) )"),
                options: [ [0:" Never"],[60:" 1 minute"],[300:" 5 minutes"],[600:"10 minutes"],[900:"15 minutes"],[1800:"30 minutes"],[3600:" 1 hour"],[10800:" 3 hours"] ], defaultValue: 0
            input name: "destPort", type: "text", title: bold("Port"), description: italic("The Tasmota webserver port. Only required if not at the default value of 80."), defaultValue: "80", required:false, displayDuringSetup: true
            input name: "username", type: "text", title: bold("Tasmota Username"), description: italic("Tasmota username is required if configured on the Tasmota device."), required: false, displayDuringSetup: true
          	input name: "password", type: "password", title: bold("Tasmota Password"), description: italic("Tasmota password is required if configured on the Tasmota device."), required: false, displayDuringSetup: true
        }
    }
}

//Function used for quickly testing out SENSOR logic when I don't actually have the sensor.
def test(){
    
    //To test out a sensor paste the result of STATUS 8
    //Many of these are taken from https://tasmota.github.io/docs/Supported-Peripherals/  Others are from first hand experience or submitted by Hubitat users.
    //body = '{"StatusSNS":{"Time":"2022-05-25T10:26:47","VINDRIKTNING":{"PM2.5":2}}}'
    //body = '{"StatusSNS":{"Time":"2022-05-25T10:23:58","Switch1":"ON","VINDRIKTNING":{"PM2.5":17}}}'
    ///body = '{"StatusSNS":{"Time":"2022-05-26T00:34:18","ANALOG":{"Range":9}}}'
    //body = '{"StatusSNS":{"Time":"2022-05-26T17:14:10","Switch1":"ON","ANALOG":{"Range":9},"SI7021":{"Temperature":63,"Humidity":49,"DewPoint":44},"IAQ":{"eCO2":450,"TVOC":125,"Resistance":76827},"PressureUnit": "hPa","TempUnit": "C"}}'
    ///body = '{"StatusSNS":{"Time":"2022-05-27T02:18:25","SI7021":{"Temperature":67,"Humidity":39,"DewPoint":41},"TempUnit":"F"}}'
    //body = '{"StatusSNS":{"Time":"2022-05-26T17:14:10","Switch1":"ON","ANALOG":{"Range":9},"SI7021":{"Temperature":63,"Humidity":49,"DewPoint":44},"IAQ":{"eCO2":450,"TVOC":125,"Resistance":76827, "BAD":76827},"PressureUnit": "hPa","TempUnit": "C"}}'
    //body = '{"StatusSNS":{"Time":"2022-05-27T21:23:35","ENERGY":{"TotalStartTime":"2022-03-10T23:36:04","Total":4.247,"Yesterday":0.054,"Today":0.111,"Power": 7,"ApparentPower":13,"ReactivePower":11,"Factor":0.57,"Voltage":122,"Current":0.11}}}'
    ///body = '{"STATUSSNS":{"Time":"2020-01-01T00:00:00","AHT1X-0x38":{"Temperature":24.7,"Humidity":61.9,"DewPoint":16.8},"TempUnit":"C"}}'
    ///body = '{"STATUSSNS":{"Time": "2019-01-01T00:00:00","AM2301": {"Temperature": 24.6,"Humidity": 58.2},"TempUnit": "C"}}'
    ///body = '{"STATUSSNS":{"Time":"2019-10-31T21:48:51","APDS9960":{"Red":282,"Green":252,"Blue":196,"Ambient":169,"CCT":4217,"Proximity":9}}}'
    ///body = '{"STATUSSNS":{"Time":"2020-01-01T17:07:07","AS3935":{"Event":4,"Distance":12,"Energy":58622,"Stage":1}}}'
    ///body = '{"STATUSSNS":{"Time":"2019-11-03T20:45:37","BH1750":{"Illuminance":79}}}'
    //body = '{"STATUSSNS":{"Time": "2019-11-03T19:34:28","BME280": {"Temperature": 21.7,"Humidity": 66.6,"Pressure": 988.6},"PressureUnit": "hPa","TempUnit": "C"}}'
    //body = '{"STATUSSNS":{"Time": "2021-09-22T17:00:00","BME680": {"Temperature": 24.5,"Humidity":33.0,"DewPoint": 7.1,"Pressure": 987.7,"Gas": 1086.43 },"PressureUnit": "hPa","TempUnit": "C"}}'
    ///body = '{"STATUSSNS":{"Time":"2021-07-17T17:29:51","DS18B20":{"Id":"000003287EF1","Temperature":24.7},"TempUnit":"C"}}'
    ///body = '{"STATUSSNS":{"Time":"2019-01-01T22:42:35","SR04":{"Distance":16.754}}}'
    ///body = '{"STATUSSNS":{"Time": "2021-08-25T17:15:45","RG-15": {"Active": 0.01,"Event": 0.13,"Total": 26.80,"FlowRate": 0.32},"TempUnit": "C"}}'
    ///body = '{"STATUSSNS":{"Time":"2020-01-01T00:00:00","IAQ":{"eCO2":450,"TVOC":125,"Resistance":76827}}}'
    ///body = '{"STATUSSNS":{"Time": "2019-01-01T00:00:00","IrReceived": {"Protocol": "NEC","Bits": 32,"Data": "0x00FF00FF"}}}'
    ///body = '{"STATUSSNS":{"Time":"2021-01-23T13:10:50","RC522":{"UID":"BA839D07","Data":"","Type":"MIFARE 1KB"}}}'
    ///body = '{"STATUSSNS":{"Time":"2019-11-11T00:03:30","MLX90614":{"OBJTMP":23.8,"AMBTMP":22.7}}}'   - Infra Red Thermometer
    //body = '{"STATUSSNS":{"Time":"2019-12-10T19:37:50","MPU6050":{"Temperature":27.7,"AccelXAxis":-7568.00,"AccelYAxis":-776.00,"AccelZAxis":12812.00,"GyroXAxis":270.00,"GyroYAxis":-741.00,"GyroZAxis":700.00,"Yaw":0.86,"Pitch":-1.45,"Roll":-10.76},"TempUnit":"C"}}'
    ///body = '{"STATUSSNS":{"Time":"2019-01-10T18:31:39","PN532":{"UID":"94D8FC5F", "DATA":"ILOVETASMOTA"}}}'
    ///body = '{"StatusSNS":{"Time":"2022-05-25T10:23:58","Switch1":"ON"}}'
    ///body = '{"STATUSSNS":{"Time": "2019-01-01T00:00:00","RfReceived": {"Data": "0x7028D5","Bits": 24,"Protocol": 1,"Pulse": 238}}}'
    ///body = '{"STATUSSNS":{"Time":"2019-11-03T21:04:05","TSL2561":{"Illuminance":21.180}}}'
    //body = '{"STATUSSNS":{"Time":"2019-12-20T11:29:22","VL53L0X":{"Distance":263}}}'
    //body = '{"STATUSSNS":{"Time":"2022-01-07T19:43:24","Switch1":"ON","Switch2":"ON","ENERGY":{"TotalStartTime":"2021-04-03T18:52:24","Total":[0.005,1.507],"Yesterday":[0.000,0.648],"Today":[0.000,0.197],"Period":[ 0, 0],"Power":[ 0,26],"ApparentPower":[ 0,43],"ReactivePower":[ 0,33],"Factor":[0.00,0.62],"Frequency":50.016,"Voltage":225,"Current":[0.000,0.189]},"ESP32":{"Temperature":65.6},"TempUnit":"C"}}'
    //body = '{"STATUSSNS":{"Time": "2020-09-11T09:18:08","MLX90640": {"Temperature": [30.8, 28.5, 24.2, 25.7, 24.5, 24.6, 24.9]},"TempUnit": "C"}}' // - IR Thermal Sensor Array
    //body = '{"STATUSSNS":{"Time":"2018-08-18T16:13:47","MCP230XX":{"D0":0,"D1":0,"D2":1,"D3":0,"D4":0,"D5":0,"D6":0,"D7":1}}}'
    //body = '{"STATUSSNS":{"TIME":"2022-06-26T16:20:33","DS18B20-1":{"ID":"011937B1E1FD","TEMPERATURE":86.7},"DS18B20-2":{"ID":"011937D1CBA3","TEMPERATURE":-11.0},"TEMPUNIT":"F"}}'
    //body = '{"STATUSSNS":{"Time": "2021-09-22T17:00:00","VINDRIKTNING":{"PM2.5":2},"BME680": {"Temperature": 24.5,"Humidity":33.0,"DewPoint": 7.1,"Pressure": 987.7,"Gas": 1086.43 },"PressureUnit": "hPa","TempUnit": "C"}}'
    body = '{"StatusSNS":{"Time":"2022-06-11T10:58:18","HX711":{"Weight":0,"WeightRaw":4782,"AbsRaw":110004}}}'
    
    //Known non-working examples
    //body = '{"STATUSSNS":{"Time": "2020-03-03T00:00:00+00:00","TX23": {"Speed": {"Act": 14.8,"Avg": 8.5,"Min": 12.2,"Max": 14.8},"Dir": {"Card": "WSW","Deg": 247.5,"Avg": 266.1,"AvgCard": "W","Min": 247.5,"Max": 247.5,"Range": 0}},"SpeedUnit": "km/h"}}}'
    state.Action = "STATUS"
    state.ActionValue = "8"
    body = body.toUpperCase()
    statusResponse(body)
}

//Used to evaluate how well the driver will translate the Tasmota Sensor Data.
void evaluateSensorData(String status8){
    state.Action = "STATUS"
    state.ActionValue = "8"
    body = status8.toUpperCase()
    statusResponse(body)  
}

def clearAttributes(){
    log("clearAttributes", "Clearing All Attributes", 0)
    
    sensor2AttributeMap.each { 
        log("clearAttributes", "Clearing attribute: $it.value", 3)
        device.deleteCurrentState(it.value)
        }
    //These are the attributes not listed in the sensor2AttributeMap
    device.deleteCurrentState("amperage")
    device.deleteCurrentState("carbonDioxide")
    device.deleteCurrentState("carbonMonoxide")
    device.deleteCurrentState("pressureUnit")
    device.deleteCurrentState("tempUnit")
    device.deleteCurrentState("speedUnit")
    device.deleteCurrentState("switch1")
    device.deleteCurrentState("switch2")    
    
    state.sensorTriggers = []
    state.itemNames = []
    state.sensorNames = []
    state.lastMessage = ""
    state.thisMessage = ""
    
    updateStatus ("Attributes Cleared - Refresh Browser!")
}

//*********************************************************************************************************************************************
//******
//****** Start of All functions that have any uniqueness to them across all of the TSync driver base.
//****** This allows for easier updates to core functions
//******
//*******************************************************************************************************************************************

//*********************************************************************************************************************************************************************
//******
//****** Start of UNIQUE standard functions
//******
//*********************************************************************************************************************************************************************


//Updated gets run when the "Initialize" button is clicked or when the device driver is selected
def initialize(){
	log("Action", "Initialize/Update Device", 0)
    //Cancel any existing scheduled tasks for this device
    unschedule("poll")
	//Make sure we are using the right address
    updateDeviceNetworkID()
    
    log("Initialize", "pollFrequency value: ${settings.pollFrequency}",1)
    
    //Test to make sure the entered frequency is in range
    switch(settings.pollFrequency) { 
        case "0": unschedule("poll") ; break
        case "60": runEvery1Minute("poll") ; break
        case "300": runEvery5Minutes("poll") ; break
        case "600": runEvery10Minutes("poll") ; break
        case "900": runEvery15Minutes("poll") ; break
        case "1800": runEvery30Minutes("poll") ; break
        case "3600": runEvery1Hours("poll") ; break
        case "10800": runEvery3Hours("poll") ; break
    } 
   	//To be safe these are populated with initial values to prevent a null return if they are used as logic flags
    if ( state.Action == null ) state.Action = "None"
    if ( state.ActionValue == null ) state.ActionValue = "None"
    if ( device.currentValue("Status") == null ) updateStatus("Complete")    

    //Do a refresh to sync the device driver
    refresh()
}

//*********************************************************************************************************************************************************************
//******
//****** End of UNIQUE standard functions
//******
//*********************************************************************************************************************************************************************



//*********************************************************************************************************************************************************************
//******
//****** UNIQUE: Start of Power related functions. These may be UNIQUE across all Tasmota Sync drivers
//******
//*********************************************************************************************************************************************************************

//None

//*********************************************************************************************************************************************************************
//******
//****** End of Power related functions
//******
//*********************************************************************************************************************************************************************



//**************************************************************************************************************************************************************************
//******
//****** UNIQUE: Start of Background task run by Hubitat
//******
//**************************************************************************************************************************************************************************


//Sync the UI to the actual status of the device. The results come back to the parse function.
//This function is called from the button press and automatically via the polling method
//In drivers with SENSOR data this function is a little different.
def refresh(){
    log ("Action", "Refresh started....", 0)
    
    log("refresh", "Getting sensor data.", 1)
    state.lastSensorData = new Date().format('yyyy-MM-dd HH:mm:ss')
    callTasmota("STATUS", "8" )
    
    //Calculate when the current operations should be finished and schedule the "STATE" command to run after them.
    //Not required in a sensor only device.
    state.LastSync = new Date().format('yyyy-MM-dd HH:mm:ss')
    }

//*****************************************************************************************************************************************************************************************************
//******
//****** End of Background tasks
//******
//*****************************************************************************************************************************************************************************************************



//******************************************************************************************************************************************************************************************************
//******
//****** Start of main program section where most of the work gets done. There are 3 main functions, parse which receives all LAN input and directs it to either hubitatResponse or syncTasmota for processing.
//****** The functions callTasmota() and parse() are IDENTICAL in all Tasmota Sync drivers and are found toward the end of the file.
//****** The functions syncTasmota, hubitatResponse() and tasmotaInjectRule() are UNIQUE in all Tasmota Sync drivers and are located immediately below.
//******
//******************************************************************************************************************************************************************************************************


//******************************************************************************************************************************************************************************************************
//******
//****** Start of main program section where most of the work gets done. There are 3 main functions, parse which receives all LAN input and directs it to either hubitatResponse or syncTasmota for processing.
//****** The functions callTasmota() and parse() are IDENTICAL in all Tasmota Sync drivers and are found toward the end of the file.
//****** The functions syncTasmota, hubitatResponse() and tasmotaInjectRule() are UNIQUE in all Tasmota Sync drivers and are located immediately below.
//******
//******************************************************************************************************************************************************************************************************


//*************************************************************************************************************************************************************************************************************
//******
//****** UNIQUE: The only things that get routed here are expected responses to commands issued through Hubitat.
//******
//*************************************************************************************************************************************************************************************************************

def hubitatResponse(body){
    log ("hubitatResponse", "Entering, data received", 1)
    log ("hubitatResponse", "Raw data is: ${body}.", 2)
   
    //Get the command and value that was submitted to the callTasmota function
    Action = state.Action
    ActionValue = state.ActionValue
    
	log ("hubitatResponse", "Flags are Action:${state.Action}  ActionValue:${state.ActionValue}", 2)
    
    //Test to see if we got a warning from Tasmota
    tasmotaWarning = false
    if (body.contains("WARNING") == true ) {
        tasmotaWarning = true            
        log ("hubitatResponse","A warning was received from Tasmota. Review the message '${body}' and make appropriate changes.", -1)
        updateStatus("Complete:Failed")
    }
    
    //Now parse into JSON to extract data.
    body = parseJson(body)
    
    //Check to make sure we have some data to act on.
    if (body !=null){
        //If the response contains the WiFi info then we extract the RSSI value for display as a state variable.
        if (body.WIFI != null ){
            def wifi = body.WIFI
            def RSSI = wifi.RSSI
            state.RSSI = RSSI
            log ("hubitatResponse", "RSSI: ${state.RSSI}", 2)
            }
        
        switch(Action.toUpperCase()) { 
             case ["TELEPERIOD"]:
        		log("hubitatResponse","Command: TelePeriod ${body.TELEPERIOD}", 1)
                if (ActionValue.toInteger() == body.TELEPERIOD)
                    {
                    log ("hubitatResponse","TelePeriod applied successfully", 0)
                    state.TelePeriod = body.TELEPERIOD
                    updateStatus("Complete:Success")
                  }
            else {
                log("hubitatResponse","TelePeriod failed to apply", -1)
                updateStatus("Complete:Fail")
                }
            	break
                
            case ["BACKLOG"]:
                //Backlog commands do not return anything useful to indicate success or failure.  A typical response might be [WARNING:Enable weblog 2 if response expected]. But the bulb may be in weblog 4 and get a different response.
            	//If we come back to this spot we know a BACKLOG command was issued and SOMETHING came back so we know the command at least got to the device.
                log ("hubitatResponse","Backlog Command acknowledged.", 0)
                updateStatus("Complete:Backlogged")
            	break
                
            case ["STATE"]:
                //Synchronise the UI to the values we get from the device via the STATE command. Typical response looks like this
                //{"Time":"2022-04-12T06:20:36","Uptime":"0T10:05:13","UptimeSec":36313,"Heap":26,"SleepMode":"Dynamic","Sleep":50,"LoadAvg":19,"MqttCount":0,"Power":"OFF","Dimmer":68,"Color":"00000000AD",
                //"HSBColor":"248,84,0","White":68,"CT":500,"Channel":[0,0,0,0,68],"Scheme":0,"Fade":"OFF","Speed":20,"LedTable":"ON","Wifi":{"AP":1,"SSId":"5441","BSSId":"A0:04:60:95:0E:62","Channel":6,"Mode":"11n",
                //"RSSI":100,"Signal":-47,"LinkCount":1,"Downtime":"0T00:00:06"}}
                //Does nothing really in a sensor only device but retained for compatibility with all other device drivers.
            
                log ("hubitatResponse","Setting device handler values to match device.", 0)
                updateStatus("Complete:Success")
            	break

            default:
                //Response to any other undefined commands will come here.  This is most likely because of a custom command
            	//If we come back to this spot we know a command was issued and SOMETHING came back so we know the command at least got to the device.
                log ("hubitatResponse","Command acknowledged.", 0)
                updateStatus("Complete")
            	break
        	}
        }
    log ("hubitatResponse","Closing Transaction", 1)
    state.inTransaction = false
   	log ("hubitatResponse","Exiting", 1)
   }


//*****************************************************************************************************************************************************************************************************
//******
//****** End of hubitatResponse()
//******
//*****************************************************************************************************************************************************************************************************



//*************************************************************************************************************************************************************************************************************
//******
//****** UNIQUE: The only things that get routed here are expected responses to commands issued through Hubitat.
//******
//*************************************************************************************************************************************************************************************************************

def syncTasmota(body){
    log ("syncTasmota", "Data received: ${body}", 0)    // Body looks something like: {"TSYNC":"TRUE","TEMPERATURE":"67","HUMIDITY":"43","DEWPOINT":"44"}
	
    //This is a special case that only happens when the rules are being injected
    if (state.ruleInjection == true){
        log ("syncTasmota", "Rule3 special case complete.", 1)
        state.ruleInjection = false
        state.inTransaction = false
        log ("syncTasmota","Closing Transaction", 2)
        updateStatus("Complete:Success")
        return
        } 
    
    //Let's see how long it's been since the last command initiated by Hubitat.  If it is less than X seconds we will ignore this sync request as it is likely an "echo" of the Hubitat request. 
    elapsed = now() - state.startTime
   
    if (elapsed > settings.debounce){
        log ("syncTasmota", "Tasmota Sync request processing.", 1)
        state.Action = "Tasmota"
        state.ActionValue = "Sync"
        state.lastTasmotSync = new Date().format('yyyy-MM-dd HH:mm:ss')
		
        //Now parse into JSON to extract data.
        body = parseJson(body)
                
        //Iterate through the data values received. Because the SENSOR fields are populated every time RULE3 runs they should never be empty\null.
		body.each {
            try {
				//Lookup the Hubitat attribute name used for this sensor value
				hubitatAttributeName = sensor2AttributeMap[it.key]
                if (it.key.toUpperCase() != "TSYNC" ) {
				    log("syncTasmota", "${it.key} sensor data (${it.value}) mapped to Hubitat attribute: ${hubitatAttributeName}" , 1)
				
                    //Check to see if the field is blank which would inidicate no change. This should never happen but you never know.
                    if ( "${it.value}" == "" ) log("syncTasmota", "Sensor data blank (${it.value})" , 2)
                    //Hubitat will de-dupe updates that do not have changed values so we can report each time.
                    else {
                        //We have a valid attribute and data so let's get the display suffix.
                        unitSuffix = sensor2UnitSuffix[it.key]
                        log("syncTasmota", "Key: ${it.key} unitSuffix is (${unitSuffix})" , 3)
                        if ( unitSuffix == null ) sendEvent(name: hubitatAttributeName, value: it.value )
                        else sendEvent(name: hubitatAttributeName, value: it.value , unit: unitSuffix )
                        }
				    }
                }
				catch (Exception e) { log ("statusResponse", "Error: Unable to match ${it.key} with a known Hubitat attribute.", -1) } 
			}
                
        updateStatus ("Complete:Tasmota Sync")
        log ("syncTasmota", "Sync completed. Exiting", 0)
        return
        }
    else {
        log ("syncTasmota", "Tasmota Sync request debounced. Exiting.", 0)
        log ("syncTasmota", "Elapsed time of ${elapsed}ms is less than debounce limit of ${settings.debounce}. This can be adjusted in settings.", 1)
    }
}
//*****************************************************************************************************************************************************************************************************
//******
//****** End of syncTasmota()
//******
//*****************************************************************************************************************************************************************************************************


//*************************************************************************************************************************************************************************************************************
//******
//****** UNIQUE: //Here we make adjustments to the Body for one off instances. This function is only present in the Universal Sensor example.
//******
//*************************************************************************************************************************************************************************************************************

def adjustBody(String body){
  
    log("adjustBody", "Received body is: ${body}" , 3)
    
    // An ANALOG sensor might have multi-level data as in this case with CTENERGY. I'm taking the easy approach and "un-nesting" the JSON data into 2 levels.
    // Converts --> {"StatusSNS":{"Time":"2022-06-26T22:06:56","ANALOG":{"Energy":2.882,"Power":49,"Voltage":230,"Current":0.215}}}} into --> {"StatusSNS":{"Time":"2022-06-26T22:06:56","ANALOG":{"Energy":2.882,"Power":49,"Voltage":230,"Current":0.215}}}}
    // Yes there is an excess '}' on the end but the JSON handler does not seem to care. Other corrections for nested ANALOG data should be added here and work in the same way.
    if (body.contains('"ANALOG":{"CTENERGY"')==true ) {
        body = body?.replace('"ANALOG":{"CTENERGY"','"ANALOG"') 
        log("statusResponse", "Modified ANALOG Body ${body}" , 3)
    }
    
    //If a device has multiple Temperature Sensors then the "TEMPERATURE" field will occur more than once.
    //When that happens we have to rename the second Temperature field to Temperature1 so the attributes are reflected correctly.
    tempCount = body.count("TEMPERATURE")
    log("statusResponse", "tempCount is: ${tempCount}" , 3)
    if (tempCount > 1 ){
        	body = body?.replace("TEMPERATURE","TEMPERATURE1") 
            body = body?.replaceFirst("TEMPERATURE1","TEMPERATURE")             
    }
    
    log("adjustBody", "Adjusted Body ${body}" , 3)
    return(body)
}
//*****************************************************************************************************************************************************************************************************
//******
//****** End of adjustBody()
//******
//*****************************************************************************************************************************************************************************************************

//*************************************************************************************************************************************************************************************************************
//******
//****** UNIQUE: The only things that gets routed here are responses to Hubitat initiated requests for Sensor updates.
//******
//*************************************************************************************************************************************************************************************************************

def statusResponse(body){
    //If we want to test some input then add a line here like the following example
    //body = '{"STATUSSNS":{"TIME":"2022-06-26T16:20:33","DS18B20-1":{"ID":"011937B1E1FD","TEMPERATURE":86.7},"DS18B20-2":{"ID":"011937D1CBA3","TEMPERATURE":-11.0},"TEMPUNIT":"F"}}'
    
    //log ("statusResponse", "Entering, data Received.", 1)
    //log ("statusResponse", "Raw data is: ${body}.", 0)
    body = adjustBody(body)
    
    //Now parse into JSON to extract data.
    body = parseJson(body)
    
    //STATUS 1 - 12 calls return data fields about Tasmota.  STATUS 8 returns sensor data and is probably the most important to Hubitat.
    //STATUS 8 response looks something like: {"StatusSNS":{"Time":"2022-05-17T04:23:22","SI7021":{"Temperature":65,"Humidity":31,"DewPoint":33},"TempUnit":"F"}}
    if ( (state.ActionValue == "8") && (body.STATUSSNS != null) )
        {
        state.lastSensorData = new Date().format('yyyy-MM-dd HH:mm:ss')
        STATUSSNS = body?.STATUSSNS
        //STATUSSNS looks like: [TIME:2022-05-17T04:26:39, TEMPUNIT:F, SI7021:[HUMIDITY:31, TEMPERATURE:64, DEWPOINT:33]] but may look like {"StatusSNS":{"Time":"2022-05-25T10:23:58","Switch1":"ON"}}
        log("statusResponse","STATUSSNS is: " + STATUSSNS, 1)

		def sensorNames = []
		def itemNames = []
		//Go through each of the top level fields
		STATUSSNS.each{
			 //log("statusResponse", "Name: ${it.key}" , 3)
			 DATA = STATUSSNS?."$it.key"
			 String mystring = "${DATA}"
			
			 //If it is JSON format we need to iterate it for the pair data. If not in JSON we just grab the data later.
			if (mystring.contains("[") == true ) {
				sensorNames.add(it.key) 
				log("statusResponse", "Add sensor: ${it.key}" , 3)
                }
			else { 
				itemNames.add(it.key)  
				log("statusResponse", "Add item: ${it.key}" , 3)
				}
			}
		
        //Make these viewable to the user.
		state.sensorNames = sensorNames.toString()
        state.itemNames = itemNames.toString()
        state.remove("Sensors")
        
		//All good to here
		log("statusResponse", "****************** Extracted Items and Sensors ******************" , 2)
            
		//Go through the top level items and get the values
		itemNames.each {
			log("statusResponse", "Item is: ${it}" , 3)
			switch("${it}") { 
			case "SWITCH1": 
				sendEvent(name: "switch1" , value: STATUSSNS?."$it".toLowerCase())
				log("statusResponse", "STATUSSNS Switch1 is: ${STATUSSNS?."$it".toLowerCase()}" , 1)    
				break
             case "SWITCH2": 
				sendEvent(name: "switch2" , value: STATUSSNS?."$it".toLowerCase())
				log("statusResponse", "STATUSSNS Switch2 is: ${STATUSSNS?."$it".toLowerCase()}" , 1)    
				break
			case "TEMPUNIT": 
                sendEvent(name: "tempUnit" , value: STATUSSNS?."$it".toUpperCase())
				log("statusResponse", "STATUSSNS TempUnit is: ${STATUSSNS?.TEMPUNIT}" , 1) 
				break
			case "PRESSUREUNIT": 
				sendEvent(name: "pressureUnit" , value: STATUSSNS?."$it".toUpperCase())
				log("statusResponse", "STATUSSNS PressureUnit is: ${STATUSSNS?.PRESSUREUNIT}" , 1) 
				break
            case "SPEEDUNIT": 
				sendEvent(name: "speedUnit" , value: STATUSSNS?."$it".toUpperCase())
				log("statusResponse", "STATUSSNS SpeedUnit is: ${STATUSSNS?.SPEEDUNIT}" , 1) 
				break
			case "TIME": 
                log("statusResponse", "STATUSSNS Time is: ${STATUSSNS?.TIME}" , 1) 
				break
			}
		}
                
        log("statusResponse", "****************** Items Processed ******************" , 2)
        //Now we go through the Sensors and gather the underlying data
        def sensorTriggers = []
		sensorNames.any{ sensors ->
			sensor = sensors
            sensorFields = STATUSSNS?."$sensor"
            log("statusResponse", "SENSOR is: ${sensor} and Sensor fields are: ${sensorFields}" , 1)
			//Iterate through the list of data items.
			sensorFields.any { data ->
				try {   
				log("statusResponse", "Data is: ${data.key}  Value: ${data.value}" , 0)
				//Lookup the Hubitat attribute name used for this sensor value
				attribute = sensor2AttributeMap[data.key]
                    
                //If a match is found the attribute will be processed. Otherwise it will be ignored
                if ( attribute != null ){
                    sendEvent( name: attribute, value: data.value )
				    log("statusResponse", "${data.key} data (${data.value}) mapped to Hubitat attribute: ${attribute}" , 1)

                    //Test to see if attribute is a valid Trigger.
                    if ( triggerEligibleList.contains(data.key) == true ){
                        log("statusResponse", "${data.key} is trigger eligible" , 3)
                        trigger = ("Tele-${sensor}#${data.key}")    //Note that we use the data.key and not data.value.
                        sensorTriggers.add (trigger) 
                        }
                    }
                else log ("statusResponse", "Error: Unable to match sensor data field '${data.key}' with a known Hubitat attribute. Is it missing from the attributes section and\\or the sensor2AttributeMap?", -1)
				
				//Hubitat will de-dupe updates that do not have changed values so we can report each time.
				sendEvent(name: attribute, value: data.value )

				}
                catch (Exception e) { log ("statusResponse", "Error processing sensor ${data.key} with data ${data.value}", -1) } 
			}
		}  //End of sensorNames.any
        
        state.sensorTriggers = sensorTriggers.toString()
        log("statusResponse", "****************** Data Processed ******************" , 2)
        
		log("statusResponse","STATUS 8 - SENSOR values processed.", 0)
        updateStatus("Complete:Success")
    }
        
	else {  //body.STATUSSNS IS null
        log("statusResponse","STATUS 8 - No data found.", 0)
        updateStatus("Complete:No Data")
        }
    
    log ("statusResponse","Closing Transaction", 1)
    state.inTransaction = false
   	log ("statusResponse","Exiting", 0)
}

//*****************************************************************************************************************************************************************************************************
//******
//****** End of statusResponse()
//******
//*****************************************************************************************************************************************************************************************************


//*************************************************************************************************************************************************************************************************************
//******
//****** UNIQUE: Installs the rule onto the Tasmota device and enables it.
//	    	 Note that the variables are initially empty and the device has go through a change in Power or TelePeriod before the values are all populated.
// 	         This function is very unique on a driver by driver basis as the triggers are all different.
//******
//*************************************************************************************************************************************************************************************************************

def tasmotaInjectRule(){
    log ("Action - tasmotaInjectRule","Injecting Rule3 into Tasmota Host. To verify go to Tasmota console and type: rule 3", 0)
    state.ruleInjection = true
    
    //These variables are used to accumulate complex strings that are used in the rules. Var use started at 10 previously but the number of sensor data fields is unknown so I added a little room.
    //Power state will be at 6, leaving 7 - 14 (8 fields) for sensor data.  Vars 15 and 16 are used for rollup and comparison.
    
    varString1 = ""
    varString2 = ""
    rule3 = ""
    def triggers = []
        
    temp = state.sensorTriggers.replace("[","") 
    temp = temp.replace("]","") 
    triggers = temp.tokenize(',')
    triggerCount = triggers.size
    
    log("tasmotaInjectRule", "triggers list is: " + triggers.toString() , 0)    
    log("tasmotaInjectRule", "trigger count is: " + triggerCount , 0)    
    
    nextVar = 15 - triggerCount 
    
		triggers.each { trigger ->
		    log("tasmotaInjectRule", "trigger name is: " + trigger , 3)    
            rule3 = rule3 + "ON ${trigger} DO backlog0 var${nextVar} %value% ; RuleTimer1 1 ENDON "   
	        //Build strings that will be used in the final rules.
            varString1 = varString1 + "'%var${nextVar}%',"
            
            attribute = trigger.substring(trigger.lastIndexOf("#")+1)
            log("tasmotaInjectRule", "Attribute is: " + attribute , 3)    
            
            //If the VarString2 already contains a match for this attribute then we will append a 1 to the end of the attribute.
            //This should allow up to two of the same sensors on a device assuming the correct attributes have been defined.
            if (varString2.contains(attribute)) {
                log("tasmotaInjectRule", "Already contains attribute: " + attribute , 3)   
                attribute = attribute + "1"
            }
            varString2 = varString2 + "'${attribute}':'%var${nextVar}%',"
			nextVar = nextVar + 1
        }
    
    //Remove the trailing "," from the varStrings
    varString1 = varString1.substring(0, varString1.length() - 1) //varString1 should be something like this: '%var9%','%var10%','%var11%'    
    varString2 = varString2.substring(0, varString2.length() - 1) //varString2 should be something like this: 'temperature':'%var9%','humidity':'%var10%','dewPoint':'%var11%'
    
    log("tasmotaInjectRule", "varString1 is: " + varString1 , 3)    
    log("tasmotaInjectRule", "varString2 is: " + varString2 , 3)        
    
    //In the prior version this looked like: rule3 = rule3 + "ON Rules#Timer=1 DO var15 %var11%,%var12%,%var13%,%var14% ENDON "
    rule3 = rule3 + "ON Rules#Timer=1 DO var15 " + varString1 + " ENDON "  
    
    //In the prior version this looked like: rule3 = rule3 + "ON var15#State\$!%var16% DO backlog ; var16 %var15% ; webquery http://" + settings.HubIP + ":39501/ POST {'TSync':'True','Temperature':'%var12%','Humidity':'%var13%','DewPoint':'%var14%'} ENDON "
    rule3 = rule3 + "ON var15#State\$!%var16% DO backlog ; var16 %var15% ; webquery http://" + settings.HubIP + ":39501/ POST {'TSync':'True'," + varString2 + "} ENDON "
    
    log("tasmotaInjectRule", "Rule3 is: ${rule3}" , 0)        
    
    //Now install the rule onto Tasmota
    callTasmota("RULE3", rule3)
    
    //and then make sure the rule is turned on.
    command = "RULE3 ON"
    def parameters = ["BACKLOG","${command}"]
    //Runs the prepared BACKLOG command after the latest that last command could have finished.
    runInMillis(remainingTime() + 50, "callTasmota", [data:parameters])
    }
    
//*********************************************************************************************************************************************************************
//******
//****** End of main program section
//******
//*********************************************************************************************************************************************************************


//*********************************************************************************************************************************************************************
//*********************************************************************************************************************************************************************
//**********************                              *****************************************************************************************************************
//**********************  END OF UNIQUE FUNCTIONS     *****************************************************************************************************************
//**********************  EVERYTHING BELOW HERE IS    *****************************************************************************************************************
//**********************  COMMON CODE FOR ALL TSYNC   *****************************************************************************************************************
//**********************  FAMILY OF DRIVERS           *****************************************************************************************************************
//**********************                              *****************************************************************************************************************
//*********************************************************************************************************************************************************************
//*********************************************************************************************************************************************************************

/*
*  CORE - IDENTICAL - CHANGELOG
*  All changes to code in the CORE section will be commented here. Changes to the UNIQUE section that are made across all drivers will also be commented here.
*  Version 0.91 - Internal version
*  Version 0.92E - Global rename of some variables
*  Version 0.93A - Enhancement of Tasmota rules to provide more granular data and less MEM usage. Although in the unique section this change was made across all drivers.
*  Version 0.93B - Enhancement of Tasmota rules to use only a single MEM register.
*  Version 0.94C - Tasmota rules moved to all VAR use, no MEM. Driver handles non-populated TSync fields.
*  Version 0.95A - Updates to parse to handle inTransaction logic and reject lan messages after timeout window has closed.
*  Version 0.95B - Added toggle function and state variables for lastOff and lastOn
*  Version 0.96A - Tweaks to formatting of logging.
*  Version 0.96B - Added logging enhancements with HTML tags. Added blue highlight to key fields in preferences.
*  Version 0.96C - Added handling for Tasmota "WARNING" message that occurs when authentication fails and possibly other scenarios.
*  Version 0.97 - Added option in settings to disable use of HTML enhancements in logging. These do not show correctly on a secondary hub in a two+ hub environment. This option allows them to be disabled.
*  Version 0.98.0 - Changed versioning to comply with Semantic Versioning standards (https://semver.org/). Moved CORE changelog to beginning of CORE section.
*
*/

//*********************************************************************************************************************************************************************
//******
//****** STANDARD: Start of System Required Function
//******
//*********************************************************************************************************************************************************************

//Installed gets run when the device driver is selected and saved
def installed(){
	log ("Installed", "Installed with settings: ${settings}", 0)
}

//Updated gets run when the "Save Preferences" button is clicked
def updated(){
	log ("Update", "Settings: ${settings}", 0)
	initialize()
}

//Uninstalled gets run when called from a parent app???
def uninstalled() {
	log ("Uninstall", "Device uninstalled", 0)
}

//********************************************************************************************************************************************************************
//******
//****** End of System Required functions
//******
//********************************************************************************************************************************************************************


//**************************************************************************************************************************************************************************
//******
//****** STANDARD: Start of Background task run by Hubitat - Is executed by the polling function which syncs the state of the device with the UI. The device being considered authoritative.
//****** All of these functions are IDENTICAL across all Tasmota Sync drivers
//******
//**************************************************************************************************************************************************************************

//Runs on a frequency determined by the user. It will synchronize the Hubitat values to those of the actual device.
//This function is only called internally and is used to schedule future refreshes. Polling is not require with Tasmota 11 and Rule3 installed.
def poll(nextPoll){
	    log ("Poll", "Polling started.. ", 0)
        refresh()
        log ("Poll", "Polling ended. Next poll in ${settings.pollFrequency} seconds.", 0)
	}

//This function is called settings.timeout milliseconds after the the transaction started.
//If the transaction has timed then it resets out and resets any temporary values.
def watchdog(){
    if (state.inTransaction == false ) {
        log ("watchdog", "All normal. Not in a transaction.", 2)
        }
    else
        {
        log ("watchdog", "Transaction timed out. Cancelled.", 2)
        updateStatus("Complete:Timeout") 
        //If the transaction has not finished successfully then we should mark it complete now the timeout has expired.   
        state.inTransaction = false
        }
    
    state.remove("ruleInjection")
    log ("watchdog", "Finished.", 1)
    
    //If the last command was a backlog then we don't really know what happened so we should do a refresh.
    if ( state.Action == "BACKLOG" ) {
        log ("watchdog", "Last command was a BACKLOG. Initiating STATE refresh for current settings.", 0)
        //Calculate when the current operations should be finished and schedule the "STATE" command to run after them.
        def parameters = ["STATE",""]
        runInMillis(remainingTime() + 500, "callTasmota", [data:parameters])
        state.LastSync = new Date().format('yyyy-MM-dd HH:mm:ss')
        }
    }

//*****************************************************************************************************************************************************************************************************
//******
//****** End of Background tasks
//******
//*****************************************************************************************************************************************************************************************************


//******************************************************************************************************************************************************************************************************
//******
//****** Start of main program section where most of the work gets done. There are 3 main functions, parse which receives all LAN input and directs it to either hubitatResponse or syncTasmota for processing.
//****** The functions callTasmota() and parse() are IDENTICAL in all Tasmota Sync drivers and are located in this section.
//****** The functions syncTasmota, hubitatResponse() and tasmotaInjectRule() are UNIQUE and can be found near the beginning of the file.
//******
//*************************************************************************************************************************************************************************************************************

//*************************************************************************************************************************************************************************************************************
//******
//****** STANDARD: This function places a call to the Tasmota device using HTTP via a hubCommand. A successful call will result in an HTTP response to the parse() function. The HUB IP address must be configured.
//******
//*************************************************************************************************************************************************************************************************************

def callTasmota(action, receivedvalue){
	log ("callTasmota", "Sending command: ${action} ${receivedvalue}", 0)
    //Update the status to show that we are sending info to the device
    
    def actionValue = receivedvalue.toString()
    if (actionValue == "") {actionValue = "None"}
    state.Action = action
    state.ActionValue = actionValue
    
	//Capture what we are doing so we can validate whether it executed successfully or not
    //We are essentially using the Attribute "Action" as a container for global variables.
    state.startTime = now()
    log ("callTasmota","Opening Transaction", 2)
    state.inTransaction = true
    
    //Watchdog is used to ensure that the transaction state is closed after the expiration time. Subsequent data will be ignored unless it is a TSync request.
    log ("callTasmota", "Starting Watchdog", 3)
    runInMillis(settings.timeout, "watchdog")
    path = "/cm?user=${username}&password=${password}&cmnd=${action} ${actionValue}"
    
    def newPath = cleanURL(path)

    log ("callTasmota", "Path: ${newPath}", 3)
    try {
            def hubAction = new hubitat.device.HubAction(
                method: "GET",
                path: newPath,
                headers: [HOST: "${settings.destIP}:${settings.destPort}"]
                )
            log ("callTasmota", "hubaction: ${hubAction}", 3)
            sendHubCommand(hubAction)
        updateStatus("Sent:${action} ${receivedvalue}")
        }
        catch (Exception e) {
            log ("calltasmota", "Exception $e in $hubAction", -1)
        }
    //The response to this HubAction request will come back to the parse function.
    log ("callTasmota","Exiting", 1)
}

//*****************************************************************************************************************************************************************************************************
//******
//****** STANDARD: parse(). This function handles all communication from Tasmota, both the Hubitat and Tasmota initiated changes. 
//****** When these changes originate on Hubitat they will be routed to hubitatResponse.
//****** When the changes originate on Tasmota they will be routed to syncTasmota for hubitatResponse() and statusResponse() for SENSOR data if applicable
//****** Note: A Hubitat initiated change will cause RULE3 on Tasmota to fire and ALSO send a TSync request. This is expected..... 
//****** Note: .....if they are received during a transaction (inTransaction==true) then they are ignored as they are just an "echo" of the command sent from Hubitat.
//****** Note: .....These are ignored when within the debounce window.
//******
//*****************************************************************************************************************************************************************************************************

def parse(LanMessage){
    log ("parse", "Entering, data received.", 1)
    log ("parse","data is ${LanMessage}", 3)
    
    def msg = parseLanMessage(LanMessage)
    def body = msg.body
    log ("parse","body is ${body}", 2)
    state.lastMessage = state.thisMessage
    state.thisMessage = msg.body       
        
	//TSync message use single quotes and must be cleaned up to be handled as JSON later
	body = body?.replace("'","\"") 
	//Convert all the contents to upper case for consistency
	body = body?.toUpperCase()
	//Search body for the word STATUS while it is still in string form
	StatusSync = false
	if (body.contains("STATUS")==true ) StatusSync = true
	log ("parse","StatusSync is: ${StatusSync}.", 2)

	//Search body for the word TSYNC while it is still in string form
	TSync = false
	if (body.contains("TSYNC")==true ) TSync = true
	log ("parse","TSync is: ${TSync}.", 2)

	//If the TSync flag is true then this is a message generated by the Tasmota rules and we should send the response to syncTasmota function.
	if (TSync == true) {
		log ("parse","Exit to syncTasmota()", 1)
		syncTasmota(body)
		return
		}
        
    //For every other response we need to check to see if we are in a transaction or not. 
	//If inTransaction == true then we need to processs it. If inTransaction == false then the response was received after the timeout window has closed.
    //If this happens we will acknowledge it and discard the data. This does not apply to TSync requests as they can occur at any time.
    if (state.inTransaction == true ) {  
        //This is for an responses that contain the word STATUS which means they are probably responses to STATUS 1 - 12 requests.
        if (StatusSync == true){
            log ("parse","Exit to statusResponse()", 1)
            statusResponse(body) 
            return    
        }
        //If we were not routed to syncTasmota or statusResponse then everything else goes to main hubitatResponse function
        log ("parse","Exit to hubitatResponse()", 1)
        hubitatResponse(body) 
		}    
    else{
       log ("parse","Data has been received outside the timeout window and has been ignored - exiting. (Increase the timeout window if this happens frequently.)", 0)
		}
}

//*****************************************************************************************************************************************************************************************************
//****** End of parse()
//*****************************************************************************************************************************************************************************************************



//*********************************************************************************************************************************************************************
//******
//****** Start of logging related functions. These functions are IDENTICAL in all Tasmota Sync drivers
//******
//*********************************************************************************************************************************************************************

//Simple function to send event message and log them.
def updateStatus(status){
    log ("updateStatus", status, 1)
    sendEvent(name: "Status", value: status )
    }

//*****************************************************************************************************************************************************************************************************
//******
//****** STANDARD: Start of log()
//****** Function to selectively log activity based on various logging levels. Normal runtime configuration is threshold = 0
//****** Loglevels are cumulative: -1 All errors, 0 = Action and results, 1 = Entering\Exiting modules with parameters, 2 = Key variables, 3 = Extended debugging info
//******
//*****************************************************************************************************************************************************************************************************

private log(name, message, int loglevel){
	
    //This is a quick way to filter out messages based on loglevel
	int threshold = settings.logging_level
    if (loglevel > threshold) {return}
    def indent = ""
    def icon1 = ""
    def icon2 = ""
    def icon3 = ""
    
    if (loglevel == -1) {
        icon1 = "🛑 "    //This is reserved for gross errors
        indent = ""
        }
    
    if (loglevel == 0) { 
        icon1 = "0️⃣"    //Used for normal operations, on, off, Color change etc
        indent = ""
        }
    
    if (loglevel == 1) {
        icon1 = "*️⃣1️⃣"    //Adds entering\exiting functions with basic parameters
        indent = ".."
    }
    if (loglevel == 2) {
        icon1 = "*️⃣*️⃣2️⃣"    //Adds display of additional data points
        indent = "...."
    }
    
    if (loglevel == 3) { 
        icon1 = "*️⃣*️⃣*️⃣3️⃣"    //Used for diagnostic logging. Everything else that was not previously covered.
        indent = "......"
    }
     
    //These will be the default icons for the primary functions. Others that may be useful in future ☎️ 📜 👎 👍 🔂 🎬 ⚰️ 🚪 💣
    if (name.toString().toUpperCase().contains("CALLTASMOTA")==true ) icon2 = "📞 "  
    if (name.toString().toUpperCase().contains("ACTION")==true ) icon2 = "⚡ "
    if (name.toString().toUpperCase().contains("DELETE")==true ) icon2 = "🗑️ "
    if (name.toString().toUpperCase().contains("SAVE")==true ) icon2 = "💾 "
    if (name.toString().toUpperCase().contains("WATCHDOG")==true ) icon2 = "🐶 "
    
    //These will ovverride the secondary icons Keyword search and icon replacement. Obviously icon2 may get overwritten so order is important.
    if (message.toString().toUpperCase().contains("APPLIED SUCCESSFULLY")==true ) icon2 = "⭐ "
    if (message.toString().toUpperCase().contains("FAILED TO APPLY")==true ) icon2 = "💩 "
    
    if (message.toString().toUpperCase().contains("ENTER")==true ) icon2 = "🏁 "
    if (message.toString().toUpperCase().contains("FINISH")==true ) icon3 = "🛑 "
    if (name.toString().toUpperCase().contains("SYNC")==true ) icon2 = "🔄 "
    if (message.toString().toUpperCase().contains("EXIT")==true ) icon2 = "💨 "
    if (message.toString().toUpperCase().contains("<CRLF>")==true ) { message = message.replace("<CRLF>","\n🔷 ") }
    if ( (name.toString().toUpperCase().contains("ACTION")==true ) && (message.toString().toUpperCase().contains("COLOR")==true ) ) icon3 = "🎨"

    displayName = ""
    newMessage = message
    
    //log.info ("settings.loggingEnhancements: " + settings.loggingEnhancements )
    
    switch(settings.loggingEnhancements) { 
        case "0": 
             break
        case "1": 
            displayName = device.displayName + " - "
            break
        case "2": 
            break
        case "3":
            displayName = blue(device.displayName) + " - "
        }
   
    //For logging enhancements (2 & 3) then we make the newMessage formatted with HTML colors. 0 & 1 have no HTML
    if ( settings.loggingEnhancements == "2" || settings.loggingEnhancements == "3") {
        if ( loglevel <= 0 ) {
            //If the logging level is 0 then we do not need to highlight the display as much as we are not trying to make it stand out against anything.
            if ( settings.logging_level == 0 ) newMessage = name + ": " + green(message)
            else newMessage = bold(name) + ": " + green(bold(message))
        }
        if ( loglevel == 1 ) newMessage = black(name) + ": " + green(message)
        if ( loglevel == 2 ) newMessage = goldenrod(name) + ": " + goldenrod(message)
        if ( loglevel >= 3 ) newMessage = midnightBlue(name) + ": " + midnightBlue(message)
       }
    
    if ( loglevel <= 1 ) { log.info ( displayName + icon1 + icon2 + icon3 + indent + newMessage)  }
    if ( loglevel >= 2 ) { log.debug ( displayName + icon1 + icon2 + icon3 + indent + newMessage) }
}

//*********************************************************************************************************************************************************************
//****** End of log function
//*********************************************************************************************************************************************************************



//*****************************************************************************************************************************************************************************************************
//******
//****** Start of HTML enhancement functions. Primarily used for logging with a few uses in settings. Most of these are unused but easier to just keep everything.
//******
//*****************************************************************************************************************************************************************************************************

//Functions to enhance text appearance
String bold(s) { return "<b>$s</b>" }
String italic(s) { return "<i>$s</i>" }
String underline(s) { return "<u>$s</u>" }

//String tomato(s) { return '"<p style="background-color:Tomato;">' + s + '</p>' }
//String test(s) { return '<body text = "#00FFFF" bgcolor = "#808000">' + s + '</body>'}

//Reds
String indianRed(s) { return '<font color = "IndianRed">' + s + '</font>'}
String lightCoral(s) { return '<font color = "LightCoral">' + s + '</font>'}
String crimson(s) { return '<font color = "Crimson">' + s + '</font>'}
String red(s) { return '<font color = "Red">' + s + '</font>'}
String fireBrick(s) { return '<font color = "FireBrick">' + s + '</font>'}
String coral(s) { return '<font color = "Coral">' + s + '</font>'}
//Oranges
String orangeRed(s) { return '<font color = "OrangeRed">' + s + '</font>'}
String darkOrange(s) { return '<font color = "DarkOrange">' + s + '</font>'}
String orange(s) { return '<font color = "Orange">' + s + '</font>'}
//Yellows
String gold(s) { return '<font color = "Gold">' + s + '</font>'}
String yellow(s) { return '<font color = "yellow">' + s + '</font>'}
String paleGoldenRod(s) { return '<font color = "PaleGoldenRod">' + s + '</font>'}
String peachPuff(s) { return '<font color = "PeachPuff">' + s + '</font>'}
String darkKhaki(s) { return '<font color = "DarkKhaki">' + s + '</font>'}
//Purples
String magenta(s) { return '<font color = "Magenta">' + s + '</font>'}
String rebeccaPurple(s) { return '<font color = "RebeccaPurple">' + s + '</font>'}
String blueViolet(s) { return '<font color = "BlueViolet">' + s + '</font>'}
String slateBlue(s) { return '<font color = "SlateBlue">' + s + '</font>'}
String darkSlateBlue(s) { return '<font color = "DarkSlateBlue">' + s + '</font>'}
//Greens
String limeGreen(s) { return '<font color = "LimeGreen">' + s + '</font>'}
String green(s) { return '<font color = "green">' + s + '</font>'}
String darkGreen(s) { return '<font color = "DarkGreen">' + s + '</font>'}
String olive(s) { return '<font color = "Olive">' + s + '</font>'}
String darkOliveGreen(s) { return '<font color = "DarkOliveGreen">' + s + '</font>'}
String lightSeaGreen(s) { return '<font color = "LightSeaGreen">' + s + '</font>'}
String darkCyan(s) { return '<font color = "DarkCyan">' + s + '</font>'}
String teal(s) { return '<font color = "Teal">' + s + '</font>'}
//Blues
String cyan(s) { return '<font color = "Cyan">' + s + '</font>'}
String lightSteelBlue(s) { return '<font color = "LightSteelBlue">' + s + '</font>'}
String steelBlue(s) { return '<font color = "SteelBlue">' + s + '</font>'}
String lightSkyBlue(s) { return '<font color = "LightSkyBlue">' + s + '</font>'}
String deepSkyBlue(s) { return '<font color = "DeepSkyBlue">' + s + '</font>'}
String dodgerBlue(s) { return '<font color = "DodgerBlue">' + s + '</font>'}
String blue(s) { return '<font color = "blue">' + s + '</font>'}
String midnightBlue(s) { return '<font color = "midnightBlue">' + s + '</font>'}
//Browns
String burlywood(s) { return '<font color = "Burlywood">' + s + '</font>'}
String goldenrod(s) { return '<font color = "Goldenrod">' + s + '</font>'}
String darkGoldenrod(s) { return '<font color = "DarkGoldenrod">' + s + '</font>'}
String sienna(s) { return '<font color = "Sienna">' + s + '</font>'}
//Grays
String lightGray(s) { return '<font color = "LightGray">' + s + '</font>'}
String gray(s) { return '<font color = "Gray">' + s + '</font>'}
String dimGray(s) { return '<font color = "DimGray">' + s + '</font>'}
String slateGray(s) { return '<font color = "SlateGray">' + s + '</font>'}
String black(s) { return '<font color = "Black">' + s + '</font>'}

//*****************************************************************************************************************************************************************************************************
//******
//****** End of HTML enhancement functions.
//******
//*****************************************************************************************************************************************************************************************************


//*********************************************************************************************************************************************************************
//******
//****** End of logging related functions. These functions are IDENTICAL in all Tasmota Sync drivers
//******
//*********************************************************************************************************************************************************************


//*********************************************************************************************************************************************************************
//******
//******  STANDARD: Start of Color related functions - Typical Hubitat functions with adjustments for calls to Tasmota
//******
//*********************************************************************************************************************************************************************

//Note: When issuing multiple commands we use backlog.  To reduce feedback we turn off rule3 at the beginning and turn it back on again after the end.
//If only one argument provided it is CT
def setColorTemperature(kelvin){
    log("Action - setColor1", "Request CT Kelvin: ${kelvin}" , 0)
    callTasmota("CT", kelvinToMireds(kelvin) )
    }

//If only two arguments provided it is CT and Dimmer (Hubitat uses the word Dimmer but I consistently use Dimmer.
def setColorTemperature(kelvin, Dimmer){
    if (Dimmer < 0) Dimmer = 0
    if (Dimmer > 100) Dimmer = 100
    log("Action - setColor2", "Request CT: ${kelvin} ; Dimmer: ${Dimmer}" , 0)
    mireds = kelvinToMireds(kelvin)
    command = "Rule3 OFF ; CT ${mireds} ; Dimmer ${Dimmer} ; DELAY ${10} ; Rule3 ON"
    callTasmota("BACKLOG", command )
    }

//If 3 arguments are provided or only CT and duration are provided it will come here. In the latter case Dimmer will be null.
def setColorTemperature(kelvin, Dimmer, duration){
    log("Action - setColorTemp3", "Request CT: ${kelvin} ; DIMMER: ${Dimmer} ; SPEED2: ${duration}", 0)
    if (duration < 0) duration = 0
    if (duration > 40) duration = 40
    if (duration > 0 ) duration = Math.round(duration * 2)    //Tasmota uses 0.5 second increments so double it for Tasmota Speed value
    mireds = kelvinToMireds(kelvin)
    
    delay = duration * 10 + 5    //Delay is in 1/10 of a second so we make it slightly longer than the actual fade delay.
    
    if (Dimmer != null) {
        if (Dimmer < 0) Dimmer = 0
        if (Dimmer > 100) Dimmer = 100
        command = "Rule3 OFF ; CT ${mireds} ; Dimmer ${Dimmer} ; SPEED2 ${duration} ; DELAY ${delay} ; Rule3 ON"
        }
    else{
        command = "Rule3 OFF ; CT ${mireds} ; SPEED2 ${duration} ; DELAY ${delay} ; Rule3 ON"
        }
    callTasmota("BACKLOG", command )
    }

//Dimmer control for only Dimmer value.
def setLevel(Dimmer) {
	log ("Action - setLevel1", "Request Dimmer: ${Dimmer}%", 0)
	callTasmota("Dimmer", Dimmer)
	}

//Dimmer control for dimmer and fade values.
def setLevel(Dimmer, duration) {
    if (duration < 0) duration = 0
    if (duration > 40) duration = 40
    if (duration > 0 ) duration = Math.round(duration * 2)    //Tasmota uses 0.5 second increments so double it for Tasmota Speed value
    delay = duration * 10 + 5    //Delay is in 1/10 of a second so we make it slightly longer than the actual fade delay.
	log ("Action - setLevel2", "Request Dimmer: ${Dimmer}% ;  SPEED2: ${duration}", 0)
    command = "Rule3 OFF ; Dimmer ${Dimmer} ; SPEED2 ${duration} ; DELAY ${delay} ; Rule3 ON"
	callTasmota("BACKLOG", command)
	}

def setHue(float value){
    log("Action - SetHue", "Request Hue: ${value}", 0)
    def color = device.currentValue('color')
    log("SetHue", "Current Color is: ${color}", 2)
    def map = isColor(color)
    desiredColor = map.Color.substring(0, 6)
    
    log("SetHue", "Current HEX Color is: #${desiredColor}", 3)
    
    //Now convert HEX to RGB
    RGB = hubitat.helper.ColorUtils.hexToRGB("#${desiredColor}")
    HSV = hubitat.helper.ColorUtils.rgbToHSV(RGB)
    HSV[0] = value
    log("SetHue", "New HSV Color is: ${HSV}", 3)
    
    //Now convert it back into HEX
    RGB = hubitat.helper.ColorUtils.hsvToRGB(HSV)
    HEX = hubitat.helper.ColorUtils.rgbToHEX(RGB)
    log ("setHue", "New HEX Color is: ${HEX}", 1)
    
    callTasmota("COLOR", HEX )   
}

def setSaturation(float value){
    log("Action - SetSaturation", "Request Saturation: ${value}", 0)
    def color = device.currentValue('color')
    log("SetSaturation", "Current Color is: ${color}", 2)
    def map = isColor(color)
    desiredColor = map.Color.substring(0, 6)
    log("SetSaturation", "Current HEX Color is: #${desiredColor}", 3)
    
    //Now convert HEX to RGB
    RGB = hubitat.helper.ColorUtils.hexToRGB("#${desiredColor}")
    HSV = hubitat.helper.ColorUtils.rgbToHSV(RGB)
    HSV[1] = value
    log("SetSaturation", "New HSV Color is: ${HSV}", 3)
    
    //Now convert it back into HEX
    RGB = hubitat.helper.ColorUtils.hsvToRGB(HSV)
    HEX = hubitat.helper.ColorUtils.rgbToHEX(RGB)
    log ("setSaturation", "New HEX Color is: ${HEX}", 1)
    
    callTasmota("COLOR", HEX )   
}

//Extracts the corresponding HSV values for a given HEX color and populates the respective attributes
//Hubitat uses the two built in names of hue and saturation so those are updated for compatibility. Hubitat does not use a built in attribute for "value" for some unknown reason.
//Because an attribute of name "value" is likely to be confusing I have opted to use an hsv attribute and included all three HSV values into it.
def setHSVfromColor(valueHex){
    //Now convert HEX to RGB
    log("setHSVfromColor", "Color: ${valueHex}", 2)
    def map = isColor(valueHex)
    color = map.Color
    
    RGB = hubitat.helper.ColorUtils.hexToRGB("#${color}")
    HSV = hubitat.helper.ColorUtils.rgbToHSV(RGB)
    
    log("setHSVfromColor", "Hue is: ${HSV[0]}", 3)
    sendEvent(name: "hue", value: HSV[0])
    
    log("setHSVfromColor", "Saturation is: ${HSV[1]}", 3)
    sendEvent(name: "saturation", value: HSV[1])
    
    log("setHSVfromColor", "value is: ${HSV[2]}", 3)
    sendEvent(name: "value", value: "${HSV[2]}" )    
}

//This function is called directly by the Color picker which provides an HSV Color which we must convert to a HEX Color for Tasmota.
//It also supports HSV for compatibility with other platforms such as SharpTools
def setColor(value) {
	def desiredColor
	log("Action - setColor", "Request Color: ${value}", 0)
    
    def valuehex = value?.hex
    def valuehue = value?.hue
    def valuesat = value?.saturation
    def valueDimmer = value?.level
    
    //These safeguards are required as Sharptools will send only hue and saturation from their color control.
    if (valuesat == null) valuesat = 0
    if (valueDimmer == null) valueDimmer = 100
    
    if (valuehex != null){
    	//We can just treat this as a hex Color
    	log ("setColor", "Requested Hex Color: ${valuehex}", 2)
    	def map = isColor(valuehex)
    	desiredColor = map.Color
        }
    
    if ((valuehex == null) && (valuehue != null) && (valuesat != null)){
    	//It must be an HSL Color
        log ("setColor", "Requested HSL - H:${valuehue} S:${valuesat} L:${valueDimmer}", 3)
        
        RGBColor = hubitat.helper.ColorUtils.hsvToRGB([valuehue, valuesat, valueDimmer])
        log ("setColor", "RGBColor is: ${RGBColor}", 3)
        
        String HSVColor = hubitat.helper.ColorUtils.rgbToHSV(RGBColor)
        log ("setColor", "HSVColor is: ${HSVColor}", 3)
        
        String HEXColor = hubitat.helper.ColorUtils.rgbToHEX(RGBColor)
        log ("setColor", "HEXColor is: ${HEXColor}", 2)
        
        desiredColor = HEXColor
        //This is going to appear to Tasmota as a Color change and Tasmota will respond with setting the Dimmer at 100.
        //This change will be reflected automatically in the Hubitat app but may not be picked up by other integration platforms if that was the source of the Color selection.
        }
    	callTasmota("COLOR", desiredColor )   
    }

//Tests whether a given Color is RGB or W and returns true or false plus the cleaned up Color
def isColor(ColorIn){
	String Color = ColorIn.toString()
    if (Color.substring(0, 1) == "#"){
        Color = Color.substring(1)
        }
    //Add trailing 0's if needed
    if (Color.length() == 6){
    	log ("isColor", "Length: 6 - Color:${Color}", 3) 
        Color = Color + "0000"
        }
    else {
        log ("isColor", "Length: ${Color.length()} - Color:${Color}", 3)
        }
        
    if ( Color.startsWith("000000") == true ){
    	log ("isColor", "False - ${Color}", 2)
        return [isColor: false, Color: Color]
    	}	
    else {
    	log ("isColor", "True - ${Color}", 2)
    	return [isColor: true, Color: Color]
        }
}
//*********************************************************************************************************************************************************************
//******
//******  End of Color related functions
//******
//*********************************************************************************************************************************************************************



//*********************************************************************************************************************************************************************
//******
//****** STANDARD: Start of Device related functions - These functions are IDENTICAL across all Tasmota Sync drivers where present
//******
//*********************************************************************************************************************************************************************

//Allows users to enter customer Tasmota commands without having to go to the Tasmota console.
void tasmotaCustomCommand(String command, String parameter) {
    log ("Action - tasmotaCustomCommand", "Issuing custom command '${command} ${parameter}' ", 0)
    try {
        callTasmota(command, parameter )
        }
    catch (Exception e) { log ("tasmotaCustomCommand", "Error: Invalid request", -1) } 
    log ("tasmotaCustomCommand", "Exiting", 1)
}

//Set the reporting period for a Tasmota device. Typically used for sensor data.
void tasmotaTelePeriod(String seconds) {
    log ("Action", "Set Tasmota TelePeriod to ${seconds} seconds.", 0)
    callTasmota("TELEPERIOD", seconds)
}

//Toggles the device state
void toggle() {
    log("Action", "Toggle ", 0)
    if (device.currentValue("switch") == "on" ) off()
    else on()
}

//*********************************************************************************************************************************************************************
//******
//****** End of device related functions
//******
//*********************************************************************************************************************************************************************



//*********************************************************************************************************************************************************************
//******
//****** STANDARD: Start of Supporting functions
//******
//*********************************************************************************************************************************************************************

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}

//Updates the device network information - Allows user to force an update of the device network information if required.
private updateDeviceNetworkID() {
	
    try{
    	log("updateDeviceNetworkID", "Settings are:" + settings.destIP, 3)
        def hosthex = convertIPtoHex(settings.destIP)
    	def desireddni = "$hosthex"
        
        def actualdni = device.deviceNetworkId
        
        //If they don't match then we need to update the DNI
        if (desireddni !=  actualdni){
        	device.deviceNetworkId = "$hosthex" 
            log("Action", "Save updated DNI: ${"$hosthex"}", 0)
         	}
        else
        	{
            log("Action", "DNI: ${"$hosthex"} is correct. Not updated. ", 2)
            }
        }
    catch (e){
    	log("Save", "Error updating Device Network ID: ${e}", -1)
     	}
}

//Tasmota CT is defined in Mireds
def int miredsToKelvin(int mireds){
	mireds = mireds.toInteger()
    if (mireds < 153) mireds = 153
    if (mireds > 500) mireds = 500
	def float kelvinfloat = 1000000/mireds
    def int kelvin = kelvinfloat.toInteger()
    log("miredsToKelvin", "Converted ${mireds} mireds to ${kelvin} kelvin.", 3)
	return kelvin
    }

//Tasmota only recognizes Mireds in a range from 153-500. Values outside that range are ignored.
def int kelvinToMireds(kelvin){
	kelvin = kelvin.toInteger()
    def float miredsfloat = 1000000/kelvin
    def int mireds = miredsfloat.toInteger()
    if (mireds < 153) return 153
    if (mireds > 500) return 500
    log("miredsToKelvin", "Converted ${kelvin} kelvin to ${mireds} mireds.", 3)
	return mireds
    }

//Cleans up Tasmota command URL by substituting for illegal characters
//Note: There is no way to pass a double quotation mark " 
def cleanURL(path){
    log ("cleanURL", "Fixing path: ${path}", 3)
    //We obviously have to do this one first as it is the % sign. Characters with a leading \ are escaped.
    path = path?.replace("%","%25") 
    //And then we can do the rest which also use this symbol
    path = path?.replace("\\","%5C") 
    path = path?.replace(" ","%20") 
    path = path?.replace("#","%23") 
    path = path?.replace("\$","%24") 
    path = path?.replace("+","%2B") 
    path = path?.replace(":","%3A") 
    path = path?.replace(";","%3B") 
    path = path?.replace("<","%3C") 
    path = path?.replace(">","%3E") 
    path = path?.replace("{","%7B") 
    path = path?.replace("}","%7D") 
    log ("cleanURL", "Returning fixed path: ${path}", 3)
    return path
    } 

//Returns the maximum amount of time until a Transaction is guaranteed to be finished.  Used to slow sequential BACKLOG transactions.
def remainingTime(){
    if (state.inTransaction == true ) {
        start = state.startTime
        remainingTime = ( start + settings.timeout - now() )
    }
    else { remainingTime = 0 }
    //remainingTime = 3000
    log ("remainingTime", "Remaining time ${remainingTime}", 3)
    return remainingTime  
}



//*********************************************************************************************************************************************************************
//******
//****** STANDARD: End of Supporting functions
//******
//*********************************************************************************************************************************************************************

