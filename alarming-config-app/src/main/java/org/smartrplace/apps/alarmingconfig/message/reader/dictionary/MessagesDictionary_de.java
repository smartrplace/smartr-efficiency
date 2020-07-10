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

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class MessagesDictionary_de extends MessagesDictionary {

	@Override
	public OgemaLocale getLocale() {
		return OgemaLocale.GERMAN;
	}
	
	@Override
	public String getTitle() {
		return "OGEMA Nachrichten";
	}
	
	@Override
	public String getColTitleAbstract() {
		return "Nachricht";
	}
	
	@Override
	public String getColTitleFull() {
		return "Text";
	}
	
	@Override
	public String getColTitlePrio() {
		return "Priorit�t";
	}
	
	@Override
	public String getColTitleApp() {
		return "Sendende App";
	}

	@Override
	public String getColTitleTime() {
		return "Zeit";
	}

}
