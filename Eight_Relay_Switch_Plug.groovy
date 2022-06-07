/**
*  Tasmota Sync N Port Relay\Switch\Plug Driver with PM
*  Version: v1.2.3
*  Download: See importUrl in definition
*  Description: Hubitat Driver for Tasmota N Port Relay\Switch\Plug with Power Monitoring. Provides Realtime and native synchronization between Hubitat and Tasmota.
*  The N port version handles any number of switches from 1 to 8. The SINGLE, DUAL, TRIPLE, QUAD and EIGHT port relay/switch/plug are all simply copies of this driver with the following adjustment.
*  1) edit line @Field static final Integer switchCount = ?  to reflect correct switchcount
*  2) definition - Uncomment the appropriate definition.
*  Names used for published drivers are found in the definitions.
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
*  N PORT RELAY\SWITCH\PLUG - UNIQUE - CHANGELOG
*  Version 0.91 - Internal version
*  Version 0.92.0 - Changed versioning to comply with Semantic Versioning standards (https://semver.org/). Moved CORE changelog to beginning of CORE section. Added links 
*  Version 0.98.0 - All versions incremented and synchronised for HPM plublication
*  Version 1.0.0 - Created new version for 8 Port Relay switch\plug. Remove LastOn\LastOff per switch state variables because it was too messy in the UI.
*  Version 1.1.0 - Created new generic version for N ports\switches\plugs to reduce maintenance footprint.
*  Version 1.2.0 - Added Power Monitoring logic. Only applies if switchCount is 2 or less. 
*  Version 1.2.1 - Fixed bug for interchangeable handling of POWER\POWER1 responses.
*  Version 1.2.2 - Fixed bug for POWER values received by TasmotaSync not being recorded properly
*  Version 1.2.2 - Fixed bug for POWER values received by TasmotaSync not being recorded properly
*  Version 1.2.3 - Restored missing Toggle command option. Added option to display\track all Tasmota Energy events
*
*  Authors Notes:
*  For more information on Tasmota Sync drivers check out these resources:
*  Original posting on Hubitat Community forum.  https://community.hubitat.com/t/tasmota-sync-drivers-native-and-real-time-synchronization-between-hubitat-and-tasmota-11/93651
*  How to upgrade from Tasmota 8.X to Tasmota 11.X  https://github.com/GaryMilne/Hubitat-Tasmota/blob/main/How%20to%20Upgrade%20from%20Tasmota%20from%208.X%20to%2011.X.pdf
*  Tasmota Sync Installation and Use Guide https://github.com/GaryMilne/Hubitat-Tasmota/blob/main/Tasmota%20Sync%20Documentation.pdf
*
*  Gary Milne - June 5th, 2022
*
**/

import groovy.transform.Field
import groovy.json.JsonSlurper

//This determines the number of switches that will appear within the device. If the switchCount is greater than 2 then power monitoring option is no longer visible in settings.
@Field static final Integer switchCount = 8

metadata {
	    //definition (name: "Tasmota Sync - Single Relay/Switch/Plug with PM", namespace: "garyjmilne", author: "Gary J. Milne", importUrl: "https://raw.githubusercontent.com/GaryMilne/Hubitat-Tasmota/main/Single_Relay_Switch_Plug.groovy", singleThreaded: true )  {
        //definition (name: "Tasmota Sync - Dual Relay/Switch/Plug with PM", namespace: "garyjmilne", author: "Gary J. Milne", importUrl: "https://raw.githubusercontent.com/GaryMilne/Hubitat-Tasmota/main/Dual_Relay_Switch_Plug.groovy", singleThreaded: true )  {
        //definition (name: "Tasmota Sync - Triple Relay/Switch/Plug", namespace: "garyjmilne", author: "Gary J. Milne", importUrl: "https://raw.githubusercontent.com/GaryMilne/Hubitat-Tasmota/main/Triple_Relay_Switch_Plug.groovy", singleThreaded: true )  {
        //definition (name: "Tasmota Sync - Quad Relay/Switch/Plug", namespace: "garyjmilne", author: "Gary J. Milne", importUrl: "https://raw.githubusercontent.com/GaryMilne/Hubitat-Tasmota/main/Quad_Relay_Switch_Plug.groovy", singleThreaded: true )  {
        definition (name: "Tasmota Sync - Eight Relay/Switch/Plug", namespace: "garyjmilne", author: "Gary J. Milne", importUrl: "https://raw.githubusercontent.com/GaryMilne/Hubitat-Tasmota/main/Eight_Relay_Switch_Plug.groovy", singleThreaded: true )  {
        capability "Switch"
        capability "Refresh"
            
        //Driver specific variables where case does not matter.
        attribute "Status", "string" 
        
        //Allow Power Monitoring for Plugs with 2 or less switches
        if (switchCount <= 2) { capability "PowerMeter" ; capability "CurrentMeter" ; capability "EnergyMeter" ; capability "VoltageMeasurement"                                   
            attribute "current", "number" ; attribute "power", "number" ; attribute "voltage", "number" ; attribute "apparentPower", "number" ; attribute "energyToday", "number" ;
            attribute "energyTotal", "number" ; attribute "energyYesterday", "number" ; attribute "powerFactor", "number" ; "number" ; attribute "reactivePower", "number"
            }
            
        //log.debug ("SwitchCount is: ${switchCount}")
        if (switchCount >= 1) { attribute "switch",  "string" }
        if (switchCount >= 2) { attribute "switch2", "string" ; command "Power2On" ; command "Power2Off" }
		if (switchCount >= 3) { attribute "switch3", "string" ; command "Power3On" ; command "Power3Off" }
        if (switchCount >= 4) { attribute "switch4", "string" ; command "Power4On" ; command "Power4Off" }
		if (switchCount >= 5) { attribute "switch5", "string" ; command "Power5On" ; command "Power5Off" }
        if (switchCount >= 6) { attribute "switch6", "string" ; command "Power6On" ; command "Power6Off" }
		if (switchCount >= 7) { attribute "switch7", "string" ; command "Power7On" ; command "Power7Off" }
        if (switchCount >= 8) { attribute "switch8", "string" ; command "Power8On" ; command "Power8Off" }
            
        command "initialize"
        command "tasmotaInjectRule"
        command "tasmotaCustomCommand", [ [name:"Command*", type: "STRING", description: "A single word command to be issued such as COLOR, CT, DIMMER etc."], [name:"Parameter", type: "STRING", description: "An optional single parameter that accompanies the command such as FFFFFFFF, 350, 75 etc."] ]
        command "tasmotaTelePeriod", [ [name:"Seconds*", type: "STRING", description: "The number of seconds between Tasmota sensor updates (TelePeriod XX)."] ]
        command "toggle"
        //command "test"
	}
    
    section("Configure the Inputs"){
			input name: "destIP", type: "text", title: bold(dodgerBlue("Tasmota Device IP Address")), description: italic("The IP address of the Tasmota device."), defaultValue: "192.168.0.X", required:true, displayDuringSetup: true
            input name: "HubIP", type: "text", title: bold(dodgerBlue("Hubitat Hub IP Address")), description: italic("The Hubitat Hub Address. Used by Tasmota rules to send HTTP responses."), defaultValue: "192.168.0.X", required:true, displayDuringSetup: true
            if (switchCount <= 2) {    
                input name: "relayType", type: "enum", title: bold(dodgerBlue("Type: Simple Plug - Plug (W) - Plug (W,V,A) - Plug (All Tasmota Energy Stats)")),description: italic("A Tasmota device that reports data can generate a lot of events, especially if the Tasmota TelePeriod is a low value. This setting will reduce the amount of data being recorded by ignoring non-essential changes. ") + underline("Note: A change in plug type requires re-installation of RULE3 for proper function."),
                    options: [[0:"Simple Plug. Log ONLY switch events. (Default)"], [1:"Plug with Power Monitoring. Log ONLY Switch and Power events."],[2:"Plug with Power Monitoring. Log ONLY Switch, Power, Current and Voltage events"],[3:"Plug with Power Monitoring. Log ALL Switch and Tasmota Energy stats"]], defaultValue: 0, required:true
            }
            input name: "timeout", type: "number", title: bold("Timeout for Tasmota reponse."), description: italic("Time in ms after which a Transaction is closed by the watchdog and subsequent responses will be ignored. Default 5000ms."), defaultValue: "5000", required:true, displayDuringSetup: false
            input name: "debounce", type: "number", title: bold("Debounce Interval for Tasmota Sync."), description: italic("The period in ms from command invocation during which a Tasmota Sync request will be ignored. Default 7000ms."), defaultValue: "7000", required:true, displayDuringSetup: false
            input name: "switchBehaviour", type: "enum", title: bold("Switch Behaviour."), description: italic("Whether the primary Switch affects only Relay 1 or ALL Relays."),
                options: [ [1:"Turns ON\\OFF only Power1"],[2:"Turns ON\\OFF ALL Power Relays"] ], defaultValue: 1
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

//Function used for quickly testing out logic and cleaning up.
def test(){
    
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
	log("Initialize", "Device initialized", 0)
    //Cancel any existing scheduled tasks for this device
    unschedule("poll")
	//Make sure we are using the right address
    updateDeviceNetworkID()
    
    log("Initialize", "pollFrequency value: ${settings.pollFrequency} seconds.",0)
    
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
    
    //If the number of switches is greater than 2 then the relayType is set to Simple with no PM. I'm not aware of any PM switches with greater than 2 ports.
    if (switchCount > 2) device.updateSetting("relayType", [value:"0", type:"enum"])
    
    //Simple switch - no PM
    if (settings.relayType.toInteger() == 0 ) {
        state.relayType = "Simple switch"
        log("Initialize", "Configured as simple switch. No power monitoring information.", 0)
        device.deleteCurrentState("current")
        device.deleteCurrentState("voltage")
        device.deleteCurrentState("power")
        device.deleteCurrentState("apparentPower")
        device.deleteCurrentState("energyToday")
        device.deleteCurrentState("energyTotal")
        device.deleteCurrentState("energyYesterday")
        device.deleteCurrentState("powerFactor")
        device.deleteCurrentState("reactivePower")
    }
    
    //Plug with PM. Only log switch and power events
    if (settings.relayType.toInteger() == 1 ) {
        state.relayType = "Lite Power Monitoring (Watts only)."
        log("Initialize", "Configured as switch with lite power monitoring. ONLY logging switch and power events.", 0)
        if ( device.currentValue("power") == null ) sendEvent(name: "power", value: 0, descriptionText: "Power (watts) events have been enabled", unit: "Watts", isStateChange: true )
        device.deleteCurrentState("current")
        device.deleteCurrentState("voltage")
        device.deleteCurrentState("apparentPower")
        device.deleteCurrentState("energyToday")
        device.deleteCurrentState("energyTotal")
        device.deleteCurrentState("energyYesterday")
        device.deleteCurrentState("powerFactor")
        device.deleteCurrentState("reactivePower")
    }
    
    //Plug with PM. Log switch, current and voltage events
    if (settings.relayType.toInteger() == 2 ) {
        state.relayType = "Normal Power Monitoring (Current, Voltage and Watts)."
        log("Initialize", "Configured as switch with power monitoring. Logging ALL switch, power, voltage and amps events.", 0)
        if ( device.currentValue("current") == null ) sendEvent(name: "current", value: 0, descriptionText: "Current events have been enabled", unit: "Amps", isStateChange: true )
        if ( device.currentValue("voltage") == null ) sendEvent(name: "voltage", value: 0, descriptionText: "Voltage events have been enabled", unit: "Volts", isStateChange: true )
        if ( device.currentValue("power") == null ) sendEvent(name: "power", value: 0, descriptionText: "Power (watts) events have been enabled", unit: "Watts", isStateChange: true )
        device.deleteCurrentState("apparentPower")
        device.deleteCurrentState("energyToday")
        device.deleteCurrentState("energyTotal")
        device.deleteCurrentState("energyYesterday")
        device.deleteCurrentState("powerFactor")
        device.deleteCurrentState("reactivePower")
    }
    
    //Plug with PM. All Tasmota Energy Events
    if (settings.relayType.toInteger() == 3 ) {
        state.relayType = "Full Power Monitoring (All Tasmota Energy Events)."
        log("Initialize", "Configured as switch with power monitoring. Logging ALL switch, power, voltage and amps events.", 0)
        if ( device.currentValue("current") == null ) sendEvent(name: "current", value: 0, descriptionText: "Current events have been enabled", unit: "Amps", isStateChange: true )
        if ( device.currentValue("voltage") == null ) sendEvent(name: "voltage", value: 0, descriptionText: "Voltage events have been enabled", unit: "Volts", isStateChange: true )
        if ( device.currentValue("power") == null ) sendEvent(name: "power", value: 0, descriptionText: "Power (watts) events have been enabled", unit: "Watts", isStateChange: true )
        if ( device.currentValue("apparentPower") == null ) sendEvent(name: "apparentPower", value: 0, descriptionText: "Apparent Power events have been enabled", unit: "VA", isStateChange: true )
        if ( device.currentValue("energyToday") == null ) sendEvent(name: "energyToday", value: 0, descriptionText: "Energy Today events have been enabled", unit: "kWh", isStateChange: true )
        if ( device.currentValue("energyTotal") == null ) sendEvent(name: "energyTotal", value: 0, descriptionText: "Energy Total events have been enabled", unit: "kWh", isStateChange: true )
        if ( device.currentValue("energyYesterday") == null ) sendEvent(name: "energyYesterday", value: 0, descriptionText: "Energy Yesterday events have been enabled", unit: "kWh", isStateChange: true )
        if ( device.currentValue("powerFactor") == null ) sendEvent(name: "powerFactor", value: 0, descriptionText: "Power Factor events have been enabled", isStateChange: true )
        if ( device.currentValue("reactivePower") == null ) sendEvent(name: "reactivePower", value: 0, descriptionText: "Reactive events have been enabled", unit: "VAr", isStateChange: true )
    }
     
    //Do a refresh to sync the device driver
    refresh()
}

//Function used to remove old state variables that are no longer used
def clean(){
    state.remove("lastOff")
    state.remove("lastOn")
    state.remove("switchType")
    state.remove("plugType")
    device.deleteCurrentState("powerWithUnit")
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

//Turns the Power on to one or all switches based on settings. Note: Power and Power1 are synonymous in Tasmota
def on() {
    if (settings.switchBehaviour == "1") {
        log("Action", "Turn on switch", 0)
        callTasmota("POWER", "ON")
        }
    else 
        {
        log("Action", "Turn on all switches", 0)
        
        i = 1
        String command = "POWER ON;"
        while ( i <= switchCount ) {
            command = command + "POWER${i} ON;"
            i++
            }
        //command = "POWER ON; POWER2 ON; POWER3 ON; POWER4 ON; POWER5 ON; POWER6 ON; POWER7 ON; POWER8 ON"
        callTasmota("BACKLOG", command)
        }
    
    //Remove any left over state variables from prior versions. This will get swept into version control when added.
    clean()
    }
        
//Turns off one or all switches.
def off() {
    if (settings.switchBehaviour == "1") {
        log("Action", "Turn off switch", 0)
        callTasmota("POWER", "OFF")
        }
    else 
        {
        log("Action", "Turn off all switches", 0)
        
        i = 1
        String command = "POWER OFF;"
        while ( i <= switchCount ) {
            command = command + "POWER${i} OFF;"
            i++
            }
        callTasmota("BACKLOG", command)
        }
	}

def Power2On() {
    log("Action", "Turn on switch 2", 0)
    callTasmota("POWER2", "ON")
    }

def Power2Off() {
    log("Action", "Turn off switch 2", 0)
    callTasmota("POWER2", "OFF")
    }
	
def Power3On() {
    log("Action", "Turn on switch 3", 0)
    callTasmota("POWER3", "ON")
    }

def Power3Off() {
    log("Action", "Turn off switch 3", 0)
    callTasmota("POWER3", "OFF")
    }
	
def Power4On() {
    log("Action", "Turn on switch 4", 0)
    callTasmota("POWER4", "ON")
    }

def Power4Off() {
    log("Action", "Turn off switch 4", 0)
    callTasmota("POWER4", "OFF")
    }
def Power5On() {
    log("Action", "Turn on switch 5", 0)
    callTasmota("POWER5", "ON")
    }

def Power5Off() {
    log("Action", "Turn off switch 5", 0)
    callTasmota("POWER5", "OFF")
    }
	
def Power6On() {
    log("Action", "Turn on switch 6", 0)
    callTasmota("POWER6", "ON")
    }

def Power6Off() {
    log("Action", "Turn off switch 6", 0)
    callTasmota("POWER6", "OFF")
    }

def Power7On() {
    log("Action", "Turn on switch 7", 0)
    callTasmota("POWER7", "ON")
    }

def Power7Off() {
    log("Action", "Turn off switch 7", 0)
    callTasmota("POWER7", "OFF")
    }
	
def Power8On() {
    log("Action", "Turn on switch 8", 0)
    callTasmota("POWER8", "ON")
    }

def Power8Off() {
    log("Action", "Turn off switch 8", 0)
    callTasmota("POWER8", "OFF")
    }

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
    
     //If it is configured for PM then we will get sensor data (STATUS 8) first.
    if (settings.relayType.toInteger() > 0 ) {
        log("refresh", "Getting sensor data.", 1)
        state.lastSensorData = new Date().format('yyyy-MM-dd HH:mm:ss')
        callTasmota("STATUS", "8" )
    }
    
    //Calculate when the current operations should be finished and schedule the "STATE" command to run after them.
    def parameters = ["STATE",""]
    runInMillis(remainingTime() + 500, "callTasmota", [data:parameters])
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
            case ["POWER"]:
                //On a single relay\plug\switch the response to "POWER1 OFF" is {"POWER":"OFF"}.  On a multi-port relay the response to "POWER1 OFF" is {"POWER1":"OFF"} so we have to be ready to accept either response.
                if (body?.POWER != null) log("hubitatResponse","Command: Power ${body.POWER}", 0)
                if (body?.POWER1 != null) log("hubitatResponse","Command: Power ${body.POWER1}", 0)
                if (ActionValue.equalsIgnoreCase(body.POWER) || ActionValue.equalsIgnoreCase(body.POWER1) ){
                    log ("hubitatResponse","Power state applied successfully", 0)
                    updateStatus("Complete:Success")
                    //We got the response we were looking for so we can actually change the state of the switch in the UI.
                    //If the switch is turned off then the power statistics must be zero. However, if TSync is enabled then it will fire a Sync anyway.
                    sendEvent(name: "switch", value: ActionValue.toLowerCase(), descriptionText: "The switch has been turned ${ActionValue.toLowerCase()}", isStateChange: true )
                    } 
            else {
                log("hubitatResponse","Power state failed to apply", -1)
                updateStatus("Complete:Fail")
                }
            	break
            
            case ["POWER2"]:
        		log("hubitatResponse","Command: Power2 ${body.POWER2}", 0)
                if (ActionValue.equalsIgnoreCase(body.POWER2) ){
                    log ("hubitatResponse","Power2 state applied successfully", 0)
                    updateStatus("Complete:Success")
                    //We got the response we were looking for so we can actually change the state of the switch in the UI.
                    //If the switch is turned off then the power statistics must be zero. However, if TSync is enabled then it will fire a Sync anyway.
                    sendEvent(name: "switch2", value: ActionValue.toLowerCase(), descriptionText: "The switch2 has been turned ${ActionValue.toLowerCase()}", isStateChange: true )
                    } 
            else {
                log("hubitatResponse","Power2 state failed to apply", -1)
                updateStatus("Complete:Fail")
                }
            	break
				
			case ["POWER3"]:
        		log("hubitatResponse","Command: Power3 ${body.POWER3}", 0)
                if (ActionValue.equalsIgnoreCase(body.POWER3) ){
                    log ("hubitatResponse","Power3 state applied successfully", 0)
                    updateStatus("Complete:Success")
                    //We got the response we were looking for so we can actually change the state of the switch in the UI.
                    //If the switch is turned off then the power statistics must be zero. However, if TSync is enabled then it will fire a Sync anyway.
                    sendEvent(name: "switch3", value: ActionValue.toLowerCase(), descriptionText: "The switch3 has been turned ${ActionValue.toLowerCase()}", isStateChange: true )
                    } 
            else {
                log("hubitatResponse","Power3 state failed to apply", -1)
                updateStatus("Complete:Fail")
                }
            	break
				
			case ["POWER4"]:
        		log("hubitatResponse","Command: Power4 ${body.POWER4}", 0)
                if (ActionValue.equalsIgnoreCase(body.POWER4) ){
                    log ("hubitatResponse","Power4 state applied successfully", 0)
                    updateStatus("Complete:Success")
                    //We got the response we were looking for so we can actually change the state of the switch in the UI.
                    //If the switch is turned off then the power statistics must be zero. However, if TSync is enabled then it will fire a Sync anyway.
                    sendEvent(name: "switch4", value: ActionValue.toLowerCase(), descriptionText: "The switch4 has been turned ${ActionValue.toLowerCase()}", isStateChange: true )
                    } 
            else {
                log("hubitatResponse","Power4 state failed to apply", -1)
                updateStatus("Complete:Fail")
                }
            	break
				
			case ["POWER5"]:
        		log("hubitatResponse","Command: Power5 ${body.POWER5}", 0)
                if (ActionValue.equalsIgnoreCase(body.POWER5) ){
                    log ("hubitatResponse","Power5 state applied successfully", 0)
                    updateStatus("Complete:Success")
                    //We got the response we were looking for so we can actually change the state of the switch in the UI.
                    //If the switch is turned off then the power statistics must be zero. However, if TSync is enabled then it will fire a Sync anyway.
                    sendEvent(name: "switch5", value: ActionValue.toLowerCase(), descriptionText: "The switch5 has been turned ${ActionValue.toLowerCase()}", isStateChange: true )
                    } 
            else {
                log("hubitatResponse","Power5 state failed to apply", -1)
                updateStatus("Complete:Fail")
                }
            	break
				
			case ["POWER6"]:
        		log("hubitatResponse","Command: Power6 ${body.POWER6}", 0)
                if (ActionValue.equalsIgnoreCase(body.POWER6) ){
                    log ("hubitatResponse","Power6 state applied successfully", 0)
                    updateStatus("Complete:Success")
                    //We got the response we were looking for so we can actually change the state of the switch in the UI.
                    //If the switch is turned off then the power statistics must be zero. However, if TSync is enabled then it will fire a Sync anyway.
                    sendEvent(name: "switch6", value: ActionValue.toLowerCase(), descriptionText: "The switch6 has been turned ${ActionValue.toLowerCase()}", isStateChange: true )
                    } 
            else {
                log("hubitatResponse","Power6 state failed to apply", -1)
                updateStatus("Complete:Fail")
                }
            	break
				
			case ["POWER7"]:
        		log("hubitatResponse","Command: Power7 ${body.POWER7}", 0)
                if (ActionValue.equalsIgnoreCase(body.POWER7) ){
                    log ("hubitatResponse","Power7 state applied successfully", 0)
                    updateStatus("Complete:Success")
                    //We got the response we were looking for so we can actually change the state of the switch in the UI.
                    //If the switch is turned off then the power statistics must be zero. However, if TSync is enabled then it will fire a Sync anyway.
                    sendEvent(name: "switch7", value: ActionValue.toLowerCase(), descriptionText: "The switch7 has been turned ${ActionValue.toLowerCase()}", isStateChange: true )
                    } 
            else {
                log("hubitatResponse","Power7 state failed to apply", -1)
                updateStatus("Complete:Fail")
                }
            	break
				
			case ["POWER8"]:
        		log("hubitatResponse","Command: Power8 ${body.POWER8}", 0)
                if (ActionValue.equalsIgnoreCase(body.POWER8) ){
                    log ("hubitatResponse","Power8 state applied successfully", 0)
                    updateStatus("Complete:Success")
                    //We got the response we were looking for so we can actually change the state of the switch in the UI.
                    //If the switch is turned off then the power statistics must be zero. However, if TSync is enabled then it will fire a Sync anyway.
                    sendEvent(name: "switch8", value: ActionValue.toLowerCase(), descriptionText: "The switch8 has been turned ${ActionValue.toLowerCase()}", isStateChange: true )
                   } 
            else {
                log("hubitatResponse","Power8 state failed to apply", -1)
                updateStatus("Complete:Fail")
                }
            	break
            
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
                log ("hubitatResponse","Setting device handler values to match device.", 0)
                if (body?.POWER)  sendEvent(name: "switch",  value: body.POWER.toLowerCase(), displayed:false)
                if (body?.POWER1) sendEvent(name: "switch",  value: body.POWER1.toLowerCase(), displayed:false)
                if (body?.POWER2) sendEvent(name: "switch2", value: body.POWER2.toLowerCase(), displayed:false)
                if (body?.POWER3) sendEvent(name: "switch3", value: body.POWER3.toLowerCase(), displayed:false)
                if (body?.POWER4) sendEvent(name: "switch4", value: body.POWER4.toLowerCase(), displayed:false)
                if (body?.POWER5) sendEvent(name: "switch5", value: body.POWER5.toLowerCase(), displayed:false)
                if (body?.POWER6) sendEvent(name: "switch6", value: body.POWER6.toLowerCase(), displayed:false)
                if (body?.POWER7) sendEvent(name: "switch7", value: body.POWER7.toLowerCase(), displayed:false)
                if (body?.POWER8) sendEvent(name: "switch8", value: body.POWER8.toLowerCase(), displayed:false)
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
    log ("syncTasmota", "Data received: ${body}", 0)
    //This is a special case that only happens when the rules are being injected
    if (state.ruleInjection == true){
        log ("syncTasmota", "Rule3 special case complete.", 1)
        state.ruleInjection = false
        state.inTransaction = false
        log ("syncTasmota","Closing Transaction", 2)
        updateStatus("Complete:Success")
        return
        } 
    
    //Let's see how long it's been since the last command initiated by Hubitat.  If it is less than X seconds we will ignore this sync request as it is an "echo" of the Hubitat request. 
    elapsed = now() - state.startTime
   
    if (elapsed > settings.debounce){
        log ("syncTasmota", "Tasmota Sync request processing.", 1)
        state.Action = "Tasmota"
        state.ActionValue = "Sync"
        state.lastTasmotaSync = new Date().format('yyyy-MM-dd HH:mm:ss')
        //Now parse into JSON to extract data.
        body = parseJson(body)
        
        //Preset the values for when the %vars% are empty
        switch1 = -1 ; switch2 = -1 ; switch3 = -1 ; switch4 = -1 ; switch5 = -1 ; switch6 = -1 ; switch7 = -1 ; switch8 = -1 
        current = -1 ; voltage = -1 ; power = -1 ; apparentPower = -1 ; energyToday = -1 ; energyTotal = -1 ; energyYesterday = -1 ; powerFactor = -1 ;  reactivePower = -1
        
        //A value of '' for any of these means no update. Probably because the device has restarted and the %vars% have not repopulated. This is expected.
        if (body?.CURRENT != '') { current = body?.CURRENT ; log ("syncTasmota","Current is: ${current}", 2) }
        if (body?.VOLTAGE != '') { voltage = body?.VOLTAGE ; log ("syncTasmota","Voltage is: ${voltage}", 2) }
        if (body?.POWER != '') { power = body?.POWER ; log ("syncTasmota","Power is: ${power}", 2) }
        if (body?.APPARENTPOWER != '') { apparentPower = body?.APPARENTPOWER ; log ("syncTasmota","apparentPower is: ${apparentPower}", 2) }
        if (body?.TODAY != '') { energyToday = body?.TODAY ; log ("syncTasmota","energyToday is: ${energyToday}", 2) }
        if (body?.TOTAL != '') { energyTotal = body?.TOTAL ; log ("syncTasmota","energyTotal is: ${energyTotal}", 2) }
        if (body?.YESTERDAY != '') { energyYesterday = body?.YESTERDAY ; log ("syncTasmota","energyYesterday is: ${energyYesterday}", 2) }
        if (body?.FACTOR != '') { powerFactor = body?.FACTOR ; log ("syncTasmota","powerFactor is: ${powerFactor}", 2) }
        if (body?.REACTIVEPOWER != '') { reactivePower = body?.REACTIVEPOWER ; log ("syncTasmota","reactivePower is: ${reactivePower}", 2) }
        
        //A value of '' for any of these means no update. Probably because the device has restarted and the %vars% have not repopulated. This is expected.
        if (body?.SWITCH1 != '') { switch1 = body?.SWITCH1 ; log ("syncTasmota","Switch 1 is: ${switch1}", 2) }
        if (body?.SWITCH2 != '') { switch2 = body?.SWITCH2 ; log ("syncTasmota","Switch 2 is: ${switch2}", 2) }
		if (body?.SWITCH3 != '') { switch3 = body?.SWITCH3 ; log ("syncTasmota","Switch 3 is: ${switch3}", 2) }
		if (body?.SWITCH4 != '') { switch4 = body?.SWITCH4 ; log ("syncTasmota","Switch 4 is: ${switch4}", 2) }
		if (body?.SWITCH5 != '') { switch5 = body?.SWITCH5 ; log ("syncTasmota","Switch 5 is: ${switch5}", 2) }
		if (body?.SWITCH6 != '') { switch6 = body?.SWITCH6 ; log ("syncTasmota","Switch 6 is: ${switch6}", 2) }
		if (body?.SWITCH7 != '') { switch7 = body?.SWITCH7 ; log ("syncTasmota","Switch 7 is: ${switch7}", 2) }
		if (body?.SWITCH8 != '') { switch8 = body?.SWITCH8 ; log ("syncTasmota","Switch 8 is: ${switch8}", 2) }
        
        //Now apply any changes that have been found. In Tasmota, "power" is the switch state unless referring to sensor data.
        if ( switch1?.toInteger() == 0 ) sendEvent(name: "switch", value: "off", descriptionText: "Switch was turned off.")
        if ( switch1?.toInteger() == 1 ) sendEvent(name: "switch", value: "on", descriptionText: "Switch was turned on.")
        if ( switch2?.toInteger() == 0 ) sendEvent(name: "switch2", value: "off", descriptionText: "Switch 2 was turned off.")
        if ( switch2?.toInteger() == 1 ) sendEvent(name: "switch2", value: "on", descriptionText: "Switch 2 was turned on.")
		if ( switch3?.toInteger() == 0 ) sendEvent(name: "switch3", value: "off", descriptionText: "Switch 3 was turned off.")
        if ( switch3?.toInteger() == 1 ) sendEvent(name: "switch3", value: "on", descriptionText: "Switch 3 was turned on.")
		if ( switch4?.toInteger() == 0 ) sendEvent(name: "switch4", value: "off", descriptionText: "Switch 4 was turned off.")
        if ( switch4?.toInteger() == 1 ) sendEvent(name: "switch4", value: "on", descriptionText: "Switch 4 was turned on.")
		if ( switch5?.toInteger() == 0 ) sendEvent(name: "switch5", value: "off", descriptionText: "Switch 5 was turned off.")
        if ( switch5?.toInteger() == 1 ) sendEvent(name: "switch5", value: "on", descriptionText: "Switch 5 was turned on.")
		if ( switch6?.toInteger() == 0 ) sendEvent(name: "switch6", value: "off", descriptionText: "Switch 6 was turned off.")
        if ( switch6?.toInteger() == 1 ) sendEvent(name: "switch6", value: "on", descriptionText: "Switch 6 was turned on.")
		if ( switch7?.toInteger() == 0 ) sendEvent(name: "switch7", value: "off", descriptionText: "Switch 7 was turned off.")
        if ( switch7?.toInteger() == 1 ) sendEvent(name: "switch7", value: "on", descriptionText: "Switch 7 was turned on.")
		if ( switch8?.toInteger() == 0 ) sendEvent(name: "switch8", value: "off", descriptionText: "Switch 8 was turned off.")
        if ( switch8?.toInteger() == 1 ) sendEvent(name: "switch8", value: "on", descriptionText: "Switch 8 was turned on.")
        
        //Send Wattage information if configured. Ignore anything less than 0.
        if (settings?.relayType.toInteger() >= 1 ){ 
            if ( power?.toFloat() >= 0 ) sendEvent(name: "power", value: power, unit: "Watts") 
            }
        
        //Send current and voltage events if configured. Ignore anything less than 0.
        if (settings?.relayType.toInteger() >= 2 ){
            if ( current?.toFloat() >= 0 ) sendEvent(name: "current", value: current, unit: "Amps" )
            if ( voltage?.toFloat() >= 0 ) sendEvent(name: "voltage", value: voltage, unit: "Volts" )
            }
        
        //Send remaining Tasmota Energy events if configured. Ignore anything less than 0.
        if (settings?.relayType.toInteger() >= 3 ){
            if ( apparentPower?.toFloat() >= 0 ) sendEvent(name: "apparentPower", value: apparentPower, unit: "VA" )
            if ( energyToday?.toFloat() >= 0 ) sendEvent(name: "energyToday", value: energyToday, unit: "kWh" )
            if ( energyTotal?.toFloat() >= 0 ) sendEvent(name: "energyTotal", value: energyTotal, unit: "kWh" )
            if ( energyYesterday?.toFloat() >= 0 ) sendEvent(name: "energyYesterday", value: energyYesterday, unit: "kWh" )
            if ( powerFactor?.toFloat() >= 0 ) sendEvent(name: "powerFactor", value: powerFactor )
            if ( reactivePower?.toFloat() >= 0 ) sendEvent(name: "reactivePower", value: reactivePower, unit: "VAr" )    
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
//****** UNIQUE: The only things that gets routed here are responses to requests for Sensor updates. Not used in this particular driver.
//******
//*************************************************************************************************************************************************************************************************************

def statusResponse(body){
    log ("statusResponse", "Entering, data Received.", 1)
    log ("statusResponse", "Raw data is: ${body}.", 0)
    
    //Now parse into JSON to extract data.
    body = parseJson(body)
    
    //STATUS 1 - 12 calls return data fields about Tasmota.  STATUS 8 returns sensor data and is probably the most important to Hubitat.
    if ( (state.ActionValue == "8") && (body.STATUSSNS.ENERGY != null) )
        {
        state.lastSensorData = new Date().format('yyyy-MM-dd HH:mm:ss')
        
        //Update the Power\Watts information.
        if (settings.relayType.toInteger() >= 1){
            if (body?.STATUSSNS?.ENERGY?.POWER != null ) {
                log("updateData", "Watts is: ${body.STATUSSNS.ENERGY.POWER}" , 2)
                sendEvent(name: "power", value: body.STATUSSNS.ENERGY.POWER ) 
                }
        }
            
        //Update Current and Voltage
        if (settings.relayType.toInteger() >= 2){
            if (body?.STATUSSNS?.ENERGY?.CURRENT != null ) {  log("updateData", "Current is: ${body.STATUSSNS.ENERGY.CURRENT}" , 2)  ;  sendEvent(name: "current", value: body.STATUSSNS.ENERGY.CURRENT ) }
            if (body?.STATUSSNS?.ENERGY?.VOLTAGE != null ) {  log("updateData", "Voltage is: ${body.STATUSSNS.ENERGY.VOLTAGE}" , 2)  ;  sendEvent(name: "voltage", value: body.STATUSSNS.ENERGY.VOLTAGE ) }  
        }
            
        //Update remaining Tasmota Energy attributes
        if (settings.relayType.toInteger() >= 3){
            if (body?.STATUSSNS?.ENERGY?.APPARENTPOWER != null ) {  log("updateData", "Apparent Power is: ${body.STATUSSNS.ENERGY.APPARENTPOWER}" , 2)  ;  sendEvent(name: "apparentPower", value: body.STATUSSNS.ENERGY.APPARENTPOWER ) }
            if (body?.STATUSSNS?.ENERGY?.TODAY != null ) {  log("updateData", "Energy Today is: ${body.STATUSSNS.ENERGY.TODAY}" , 2)  ;  sendEvent(name: "energyToday", value: body.STATUSSNS.ENERGY.TODAY ) }
            if (body?.STATUSSNS?.ENERGY?.TOTAL != null ) {  log("updateData", "Energy Total is: ${body.STATUSSNS.ENERGY.TOTAL}" , 2)  ;  sendEvent(name: "energyTotal", value: body.STATUSSNS.ENERGY.TOTAL ) }
            if (body?.STATUSSNS?.ENERGY?.YESTERDAY != null ) {  log("updateData", "Energy Yesterday is: ${body.STATUSSNS.ENERGY.YESTERDAY}" , 2)  ;  sendEvent(name: "energyYesterday", value: body.STATUSSNS.ENERGY.YESTERDAY ) }
            if (body?.STATUSSNS?.ENERGY?.FACTOR != null ) {  log("updateData", "Power Factor is: ${body.STATUSSNS.ENERGY.POWERFACOR}" , 2)  ;  sendEvent(name: "powerFactor", value: body.STATUSSNS.ENERGY.FACTOR ) }
            if (body?.STATUSSNS?.ENERGY?.REACTIVEPOWER != null ) {  log("updateData", "Reactive Power is: ${body.STATUSSNS.ENERGY.REACTIVEPOWER}" , 2)  ;  sendEvent(name: "reactivePower", value: body.STATUSSNS.ENERGY.REACTIVEPOWER ) }
        }
                
        log("statusResponse","STATUS 8 - ENERGY values processed.", 0)
        updateStatus("Complete:Success")
        }
    else    
        {
        log("statusResponse","STATUS 8 - NO ENERGY data found.", 0)
        updateStatus("Complete:No Data")
        }
    log ("statusResponse","Closing Transaction", 1)
    state.inTransaction = false
   	log ("statusResponse","Exiting", 1)
   }

//******** End of statusResponse() ***************************************************************************************************************************************************************************


//*************************************************************************************************************************************************************************************************************
//******
//****** UNIQUE: Installs the rule onto the Tasmota device and enables it.
//	    	 Note that the variables are initially empty and the bulb has go through a change in Power, Color, Dimmer, CT, Fade and Speed before the values are all populated.
// 	         This function is very unique on a driver by driver basis as the triggers are all different.
//******
//*************************************************************************************************************************************************************************************************************

//This version of the function dynamically allocates variables and builds the rule based on the switchCount.
//Design to handle any number of relays from 1 - 8
//Also handles Energy monitoring triggers when specified in settings.
def tasmotaInjectRule(){
    log ("Action - tasmotaInjectRule","Injecting Rule3 into Tasmota Host. To verify go to Tasmota console and type: rule 3", 0)
    state.ruleInjection = true
    
    //Variables used in assembling final rule3 string
    varString1 = ""
    varString2 = ""
    rule3 = ""
    
    //start the use of vars at the highest possible number.  var15 and 16 are reserved for comparisons.
    nextVar = 14 - switchCount + 1
    
    //Build a list of Triggers for this configuration
    //This formatting keeps things roughly consistent with how rule3 is built in the Universal Sensor driver.
    log ("tasmotaInjectRule", "Relay Type:" + settings.relayType.toInteger(), 0)
    def triggers = "[]"
    
    if ( settings.relayType.toInteger() == 0 ) triggers = "[]"
    if ( settings.relayType.toInteger() == 1 ) triggers = "[Tele-ENERGY#POWER]"
    if ( settings.relayType.toInteger() == 2 ) triggers = "[Tele-ENERGY#POWER,Tele-ENERGY#VOLTAGE,Tele-ENERGY#CURRENT]"
    if ( settings.relayType.toInteger() == 3 ) triggers = "[Tele-ENERGY#POWER,Tele-ENERGY#VOLTAGE,Tele-ENERGY#CURRENT,Tele-ENERGY#APPARENTPOWER,Tele-ENERGY#TODAY,Tele-ENERGY#TOTAL,Tele-ENERGY#YESTERDAY,Tele-ENERGY#FACTOR,Tele-ENERGY#REACTIVEPOWER]"
        
    temp = triggers.replace("[","") 
    temp = temp.replace("]","") 
    triggers = temp.tokenize(',')
    triggerCount = triggers.size
    
    log("tasmotaInjectRule", "triggers list is: " + triggers.toString() , 1)    
    log("tasmotaInjectRule", "trigger count is: " + triggerCount , 1)    
    
    nextVar = nextVar - triggerCount 
    startVar = nextVar
    
    //Iterate through each switch and add a Power#State trigger to the rule.
    i = 1
    while ( i <= switchCount ) {
        rule3 = rule3 + "ON Power${i}#state DO backlog0 Var${nextVar} %value% ; RuleTimer1 1 ENDON "
        varString1 = varString1 + "'%var${nextVar}%',"
        varString2 = varString2 + "'SWITCH${i}':'%var${nextVar}%',"
        i++
        nextVar++
        }
    
    //Iterate through each trigger and add a Tele-Energy#Value trigger to the rule.
    triggers.each { trigger ->
	log("tasmotaInjectRule", "trigger name is: " + trigger , 1)    
    rule3 = rule3 + "ON ${trigger} DO backlog0 var${nextVar} %value% ; RuleTimer1 1 ENDON "   
	//Build strings that will be used in the final rules.
    varString1 = varString1 + "'%var${nextVar}%',"
            
    attribute = trigger.substring(trigger.lastIndexOf("#")+1)
    log("tasmotaInjectRule", "Attribute is: " + attribute , 1)    
    varString2 = varString2 + "'${attribute}':'%var${nextVar}%',"
	nextVar = nextVar + 1
    }
    
    //Clean up the varStrings to remove the trailing character
    varString1 = varString1.substring(0, varString1.length() - 1) 
    varString2 = varString2.substring(0, varString2.length() - 1) 
    log("tasmotaInjectRule", "varString1 is: " + varString1 , 2)    
    log("tasmotaInjectRule", "varString2 is: " + varString2 , 2)   
    
    //Assemble the final rule3.
    rule3 = rule3 + "ON Rules#Timer=1 DO var15 " + varString1 + " ENDON "  
    rule3 = rule3 + "ON var15#State\$!%var16% DO backlog ; var16 %var15% ; webquery http://" + settings.HubIP + ":39501/ POST {'TSync':'True'," + varString2 + "} ENDON "

    //Now install the rule onto Tasmota
    callTasmota("RULE3", rule3)
    log("tasmotaInjectRule", "rule3 is: " + rule3 , 0) 
    
    clearVars = ""
    command = ""
    
    //Create a string that we can use to clear the VARs whenever the rule changes
    i = startVar
    while ( i < 15 ) { command = command + "VAR${i}" + '""' + ";" ; i++ }

    //Now clear all the VARs that were used and make sure the rule is turned on.
    command = command + " RULE3 ON"
    def parameters = ["BACKLOG","${command}"]
    log("tasmotaInjectRule", "parameters is: " + parameters , 0) 
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
    log("Action - setColorTemp3", "Request CT: ${kelvin} ; Dimmer: ${Dimmer} ; Speed2: ${duration}", 0)
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
	log ("Action - setLevel2", "Request Dimmer: ${Dimmer}% ;  Speed2: ${duration}", 0)
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
    
    //If a dimmer level is set we will preserve it when changing the color.
    if ( device.currentValue('level') == 100 ) callTasmota("COLOR", HEX )
    else callTasmota("COLOR2", HEX )
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
    
    //If a dimmer level is set we will preserve it when changing the color.
    if ( device.currentValue('level') == 100 ) callTasmota("COLOR", HEX )
    else callTasmota("COLOR2", HEX )
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
        //If a dimmer level is set we will preserve it when changing the color.
        if ( device.currentValue('level') == 100 ) callTasmota("COLOR", desiredColor )
        else callTasmota("COLOR2", desiredColor )
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
    path = path?.replace('"',"%22") 
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
