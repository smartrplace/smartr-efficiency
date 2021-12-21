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
package org.smartrplace.apps.alarmingconfig.gui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.smartrplace.apps.alarmingconfig.message.reader.dictionary.MessagesDictionary;

import de.iwes.widgets.api.messaging.Message;
import de.iwes.widgets.api.messaging.listener.ReceivedMessage;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.datatable.DataTable;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.WindowCloseButton;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.messaging.MessageReader;

public class PageBuilderSimple {

	private static final int MAX_MSG_LENGTH = 60;
	private static final int MAX_TITLE_LENGTH = 50;
	private final WidgetPage<MessagesDictionary> page;
	private final MessageReader mr;
	private final Header header;
	private final DataTable dataTable;
	private final MailPopup popup;
	//private final TextField hoursToShow;
	private final ApplicationManager appMan;
	//private final boolean showCloseButton;

	public PageBuilderSimple(final WidgetPage<MessagesDictionary> page, final MessageReader mr,
			ApplicationManager appMan, boolean showCloseButton) {
		this(page, mr, appMan, showCloseButton, true);
	}
	public PageBuilderSimple(final WidgetPage<MessagesDictionary> page, final MessageReader mr,
			ApplicationManager appMan, boolean showCloseButton,
			boolean showNavigation) {
		this.page = page;
		this.mr = mr;
		this.appMan = appMan;
		//this.showCloseButton = showCloseButton;
		
		if(!showNavigation) {
			page.getMenuConfiguration().setNavigationVisible(false);
			page.getMenuConfiguration().setLanguageSelectionVisible(false);
			page.getMenuConfiguration().setShowMessages(false);
			page.getMenuConfiguration().setMenuVisible(false);
			page.getMenuConfiguration().setShowLogoutBtn(false);
		}
		this.header = new Header(page, "mainPageHeader", Boolean.getBoolean("org.ogema.messaging.basic.services.config.fixconfigenglish")?
				"Alarm Messages":"Ereignisliste"); /* {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				MessagesDictionary dict = (MessagesDictionary) page.getDictionary(req); // FIXME why is cast necessary?
				setText(dict.getTitle(),req);
			}
		};*/
		header.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
		
		this.popup = new MailPopup(page, "mailPopup");
		popup.initialize();
		
		/*hoursToShow = new TextField(page, "hoursToShow", "12");
		StaticTable topTable = new StaticTable(1, 3, new int[] {2, 2, 8});
		topTable.setContent(0, 0, "Letzte Stunden anzeigen:").setContent(0, 1, hoursToShow);
		page.append(topTable);*/
		if(showCloseButton) {
			WindowCloseButton closeTabButton = new WindowCloseButton(page, "closeTabButtonBuilding",
					System.getProperty("org.ogema.app.navigation.closetabbuttontext", "Fertig"));
			closeTabButton.addDefaultStyle(ButtonData.BOOTSTRAP_RED);
			page.append(closeTabButton).linebreak();
		}
		
		this.dataTable = new MessageTable(page, "messagesTable") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				String currentId = getSelectedRow(req);
				ReceivedMessage currentMessage = null;
				if (currentId != null) {
					long id = Long.MAX_VALUE - Long.parseLong(currentId);
					currentMessage = mr.getMessage(id);
				}
				popup.setCurrentMessage(currentMessage, req);
			}
		};
		dataTable.sortDefault(0, false);
		page.append(dataTable);
		page.append(popup);
		dataTable.triggerAction(popup, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
//		dataTable.setDefaultPollingInterval(5000L); // TODO only makes sense once ordering and paging options are kept through updates
//		hoursToShow.triggerOnPOST(dataTable);
	}

	class MessageTable extends DataTable {

		private static final long serialVersionUID = 1L;
		public static final long HOUR_MILLIS = 60*60000;

		public MessageTable(WidgetPage<?> page, String id) {
			super(page, id);
		}

		@Override
		public void onGET(OgemaHttpRequest req) {
			OgemaLocale locale = req.getLocale();
			clear(req);
			//String hourS = hoursToShow.getValue(req);
			String hourS = "8760";
			long startTime = 0;
			try {
				float hours = Float.parseFloat(hourS);
				startTime = appMan.getFrameworkTime() - (long)hours*HOUR_MILLIS;
			} catch (NumberFormatException e) {
				startTime = 0;
			}
			Map<Long, ReceivedMessage> messages = mr.getMessages(startTime); // TODO add configurable start time and message status
			Map<String,Map<String,String>> rows = getMessagesMap(messages, locale); // TODO set link, sort by status, etc
			addRows(rows, req);
			MessagesDictionary dict = (MessagesDictionary) page.getDictionary(req);
			Map<String, String> columns = getColumnTitles(dict, locale);
			setColumnTitles(columns , req);
		}

	}

	private Map<String,Map<String,String>> getMessagesMap(Map<Long,ReceivedMessage> originalMessages, OgemaLocale locale) {
		Map<String,Map<String,String>> result = new LinkedHashMap<String, Map<String,String>>();
		
		/*List<Map.Entry<Long,ReceivedMessage>> entries = new ArrayList<Map.Entry<Long,ReceivedMessage>>(originalMessages.entrySet());
		entries.sort(new Comparator<Map.Entry<Long,ReceivedMessage>>() {

			@Override
			public int compare(Entry<Long, ReceivedMessage> o1, Entry<Long, ReceivedMessage> o2) {
				return Long.compare(o2.getKey(), o1.getKey()); 
			}
		});
		Iterator<Map.Entry<Long,ReceivedMessage>> it = entries.iterator();*/
		
		Iterator<Map.Entry<Long,ReceivedMessage>> it = originalMessages.entrySet().iterator();
		//long idx = 1;
		while(it.hasNext()) {
			Map.Entry<Long,ReceivedMessage> entry = it.next();
			long time = entry.getKey();
			ReceivedMessage msg = entry.getValue();
			Message omsg = msg.getOriginalMessage();
			Map<String,String> columns = new LinkedHashMap<String, String>();
			//columns.put("nr", String.valueOf(idx));
			columns.put("time", getTimeString(msg.getTimestamp()));
			columns.put("title",cutMsg(omsg.title(locale),MAX_TITLE_LENGTH));
			columns.put("msg", cutMsg(omsg.message(locale),MAX_MSG_LENGTH));
			//columns.put("prio",omsg.priority().name());
			String fullId = msg.getAppId().getIDString();
			int dotidx = fullId.lastIndexOf('.');
			int atidx = fullId.lastIndexOf('@');
			String appId = fullId.substring(dotidx+1, atidx);
			columns.put("app", appId); // TODO adapt
			//columns.put("status",msg.getStatus().name()); // TODO use dict?
			result.put(String.valueOf(Long.MAX_VALUE-time), columns);
			//result.put(String.valueOf(idx), columns);
			//idx++;
		}
		return result;
	}

	private String cutMsg(String msg, int length) {
		if (msg == null ||msg.length() < length) return msg;
		return msg.substring(0, length);
	}

	private Map<String, String> getColumnTitles(MessagesDictionary dict, OgemaLocale locale) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		//map.put("nr", "Nr");
		map.put("time", Boolean.getBoolean("org.ogema.messaging.basic.services.config.fixconfigenglish")?
				"Time Stamp ":
				"Zeitstempel");
		map.put("title", Boolean.getBoolean("org.ogema.messaging.basic.services.config.fixconfigenglish")?
				"Subject         ":
				"Betreff         ");
		map.put("msg", "Text           ");
		//map.put("prio", "Prio.");
		map.put("app", Boolean.getBoolean("org.ogema.messaging.basic.services.config.fixconfigenglish")?
				"Source":
				"Quelle");
		//map.put("status", dict.getColTitleStatuts());
		return map;
	}

	public static String getTimeString(long tm) {
		final Date date = new Date(tm);
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
	}

}
