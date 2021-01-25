/*
   Copyright 2017 Francis Appels - http://www.z-application.com/

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

/**
 * This class provides access to a ipda018 series build in barcode reader
 */

var exec = require('cordova/exec');

var Ipda018BCR = function () { };

/**
 * Constants for checking Ipda018 BCR states
 */

Ipda018BCR.prototype.STATE_NONE = 0;       // we're doing nothing
Ipda018BCR.prototype.STATE_READING = 1; //reading BCR reader
Ipda018BCR.prototype.STATE_READ = 2; ///read received BCR reader
Ipda018BCR.prototype.STATE_ERROR = 3; // error
Ipda018BCR.prototype.STATE_DESTROYED = 4; // BCR reader destroyed
Ipda018BCR.prototype.STATE_READY = 5;       // init ok

/**
 * init ipda018 bcr
 * 
 * @param successCallback function to be called when plugin is destroyed
 * @param errorCallback well never be called
 */
Ipda018BCR.prototype.init = function (successCallback, failureCallback) {
	cordova.exec(successCallback, failureCallback, 'Ipda018BCR', 'init', []);
};

/**
 * destroy ipda018 bcr
 * 
 * @param successCallback function to be called when plugin is destroyed
 * @param errorCallback well never be called
 */
Ipda018BCR.prototype.destroy = function (successCallback, failureCallback) {
	cordova.exec(successCallback, failureCallback, 'Ipda018BCR', 'destroy', []);
};

/**
 * Check ipda018 bcr current state
 * 
 * @param successCallback(object) returns json object containing state, property state (int)
 * @param errorCallback function to be called when problem fetching state.
 *  
 */
Ipda018BCR.prototype.getState = function (successCallback, failureCallback) {
	cordova.exec(successCallback, failureCallback, 'Ipda018BCR', 'getState', []);
};

/**
 * Read BCR
 * 
 * @param successCallback(data) asynchronous function to be called each time reading was successful.
 * 		returns ASCII string with received data 
 * @param errorCallback asynchronous function to be called when there was a problem while reading
 */
Ipda018BCR.prototype.read = function (successCallback, failureCallback) {
	cordova.exec(successCallback, failureCallback, 'Ipda018BCR', 'read', []);
};

var ipda018BCR = new Ipda018BCR();
module.exports = ipda018BCR;
