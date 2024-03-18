/**
 *  Tasmota Sync - Child Switch
 *
 *  Copyright 2023 Joel Wetzel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */


metadata {
	definition (name: "Tasmota Sync - Child Switch", namespace: "joelwetzel", author: "Joel Wetzel") {
		capability "Actuator"
		capability "Sensor"
		capability "Switch"
		
		command "markOn"
		command "markOff"
	}
	
	preferences {
		section {
		}
	}
}


def parse(String description) {
}



def on() {
	sendEvent(name: "switch", value: "on", isStateChange: true)
	
	getParent().lightOn()
}


def off() {
	sendEvent(name: "switch", value: "off", isStateChange: true)

    getParent().lightOff()
}


// Set the virtual switch on without sending state back to Tasmota. Prevents cyclical firings.
def markOn() {
	sendEvent(name: "switch", value: "on")	
}


// Set the virtual switch off without sending state back to Tasmota.  Prevents cyclical firings.
def markOff() {
	sendEvent(name: "switch", value: "off")	
}



    
