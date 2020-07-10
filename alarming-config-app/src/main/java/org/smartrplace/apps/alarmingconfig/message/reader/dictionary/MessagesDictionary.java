/**
 * ﻿Copyright 2014-2018 Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.apps.alarmingconfig.message.reader.dictionary;

import de.iwes.widgets.api.widgets.localisation.LocaleDictionary;

public abstract class MessagesDictionary implements LocaleDictionary {
	
	public String getTitle() {
		return "OGEMA Messages"; // default value
	}
	
	public String getColTitleAbstract() {
		return "Message";
	}
	
	public String getColTitleFull() {
		return "Full text";
	}
	
	public String getColTitlePrio() {
		return "Priority";
	}
	
	public String getColTitleApp() {
		return "Sending app";
	}
	
	public String getColTitleStatuts() {
		return "Status";
	}
	
	public abstract String getColTitleTime();

}
