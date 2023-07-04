package org.smartrplace.apps.alarmconfig.reminder;

import java.util.Arrays;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.user.NaturalPerson;
import org.smartrplace.gateway.device.GatewaySuperiorData;

/**
 * Provisional initialization for GatewaySuperiorData#responsibilityContacts
 */
public class ResponsibilityContactsInitializer {

	private static final Contact[] DEFAULT_CONTACTS = new Contact[] {
		new Contact("Supervision", null, null, "alarming@smartrplace.com" ),
		new Contact("Terminvereinbarung", null, null, "support@smartrplace.com" ),
		new Contact("On-site Operation", null, null, "onsite@smartrplace.com" ),
		new Contact(null, "David", "Nestle", "david.nestle@smartrplace.de"),
		new Contact(null, "Jan", "Lapp", "jan.lapp@smartrplace.de")
	
	};
	
	private final ApplicationManager appMan;
	
	public ResponsibilityContactsInitializer(final ApplicationManager appMan) {
		this.appMan = appMan;
	}
	
	public void run() {
		GatewaySuperiorData superiorData = appMan.getResourceAccess().getResources(GatewaySuperiorData.class).stream().findAny().orElse(null);
		final boolean existed = superiorData != null;
		if (!existed)
			superiorData = appMan.getResourceManagement().createResource("gatewaySuperiorDataRes", GatewaySuperiorData.class);
		if (superiorData.responsibilityContacts().isActive())
			return;
		final ResourceList<NaturalPerson> contacts = superiorData.responsibilityContacts().create();
		Arrays.stream(DEFAULT_CONTACTS).forEach(contact -> add(contacts, contact));
		if (!existed)
			superiorData.activate(true);
		else
			superiorData.responsibilityContacts().activate(true);
	}
	
	private static NaturalPerson add(ResourceList<NaturalPerson> contacts, Contact contact) {
		final String resourceName = contact.firstName != null && contact.lastName != null ? contact.firstName + "_" + contact.lastName : contact.role;
		final NaturalPerson person = contacts.addDecorator(resourceName.toLowerCase(), NaturalPerson.class);
		if (contact.role != null)
			person.userRole().<StringResource> create().setValue(contact.role);
		if (contact.firstName != null)
			person.firstName().<StringResource> create().setValue(contact.firstName);
		if (contact.lastName != null)
			person.lastName().<StringResource> create().setValue(contact.lastName);
		person.addDecorator("emailAddress", StringResource.class).setValue(contact.email);
		if(!person.isActive())
			person.activate(true);
		return person;
	}

	private static class Contact {
		
		public Contact(String role, String firstName, String lastName, String email) {
			this.role = role;
			this.firstName = firstName;
			this.lastName = lastName;
			this.email = email;
		}
		
		final String role;
		final String firstName;
		final String lastName;
		final String email;
		
	}
	
	
}
