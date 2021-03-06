package org.dhbw.mosbach.ai.tickets.ejb;

import org.dhbw.mosbach.ai.tickets.database.EntryDAO;
import org.dhbw.mosbach.ai.tickets.database.TicketDAO;
import org.dhbw.mosbach.ai.tickets.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.*;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.security.Principal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Klasse zum Anlegen von Testdaten, sofern beim Starten der Applikation noch keine vorhanden sind.
 *
 * Es werden sowohl Rollen und Benutzer, als auch Tickets mit gültigen Einträgen bei Bedarf erzeugt.
 */
@Startup
@Singleton
@RunAs(Roles.ADMIN)
public class DemoDataProvider {
	private static final Logger logger = LoggerFactory.getLogger(DemoDataProvider.class);

	@PersistenceContext
	private EntityManager em;

	@Resource
	private EJBContext context;

	@Resource
	private TimerService timerService;

	@EJB
	private UserDAOProxy userDAOProxy;

	@Inject
	private TicketDAO ticketDAO;

	@Inject
	private EntryDAO entryDAO;

	@PostConstruct
	private void init() {
		timerService.createSingleActionTimer(1, new TimerConfig("ddp", false));
	}

	// Wenn ein Timeout auftritt...
	@Timeout
	private void timer() {
		logger.info("In DemoDataProvider.init");

		final Principal callerPrincipal = context.getCallerPrincipal();
		final boolean isAdmin = context.isCallerInRole(Roles.ADMIN);
		logger.info("Principal: {}, admin: {}", callerPrincipal, isAdmin);

		// ... dann überprüfe, ob bereits valide Daten in der Datenbank vorhanden sind.
		final Long userCount = (Long) em.createQuery("SELECT COUNT(u) FROM User u").getSingleResult();
		final Long ticketCount = (Long) em.createQuery("SELECT COUNT(t) FROM Ticket t").getSingleResult();

		// Wenn keine User existieren, so werden diese für Testzwecke erzeugt.
		if (userCount == 0)
			createUsers();

		// Wenn keine Tickets existieren, so werden diese für Testzwecke erzeugt.
		if (ticketCount == 0)
			createTickets();
	}

	/**
	 * Methoe zum Erzeugen von Test-Usern.
	 *
	 * Zunächst werden die drei notwendigen Rollen erzeugt und in die Datenbank gespeichert.
	 * Anschließend werden valide Test-User erzeugt, wobei diesen die zu angelegten Rollen zugewiesen werden.
	 */
	private void createUsers() {
		final Role adminRole = new Role(Roles.ADMIN, "Administrator");
		final Role editorRole = new Role(Roles.EDITOR, "Bearbeiter");
		final Role customerRole = new Role(Roles.CUSTOMER, "Kunde");
		em.persist(adminRole);
		em.persist(editorRole);
		em.persist(customerRole);

		// Der User "root" dient zu Testzwecken und besitzt alle der möglichen Rollen!
		// Ansonsten werden von jeder Sorte einige Benutzer erstellt (1x Admin, 4x Editor, 4x Kunde).
		//
		// Mehrere Editoren erlauben es, die Funktion von Tickets Übernehmen und Zuweisen zu testen.
		// Mehrere Kunden erlauben es, Tickets der gleichen Firma von mehreren Kunden einzusehen.
		createUser("root", "Root", "Ticket Master", "root@ticket.master", "toor", adminRole, editorRole, customerRole);
		createUser("admin", "The Admin", "Ticket Master", "admin@ticket.master", "admin", adminRole);
		createUser("editor1", "Rolf Meyer", "Ticket Master", "rolf@ticket.master", "mosbach", editorRole);
		createUser("editor2", "Alex Löwen", "Ticket Master", "alex@ticket.master", "mosbach", editorRole);
		createUser("editor3", "Mike Pfeffer", "Ticket Master", "mike@ticket.master", "mosbach", editorRole);
		createUser("editor4", "Fritz Birnbaum", "Ticket Master", "fritz@ticket.master", "mosbach", editorRole);
		createUser("customer1", "Edwin Kopf", "IBM", "edwin@ibm.com", "mosbach", customerRole);
		createUser("customer2", "Jens Hadarmad", "Deutsche Bundesbank", "jens@bundesbank.de", "mosbach", customerRole);
		createUser("customer3", "Benno Gut", "Deutsche Bundesbank", "benno@bundesbank.de", "mosbach", customerRole);
		createUser("customer4", "Vanessa Richter", "IBM", "vanessa@ibm.com", "mosbach", customerRole);
	}

	/**
	 * Methoden zum Anlegen eines konkreten Test-Users
	 *
	 * Zunächst wird ein Benutzer mit seinen erforderlichen Attributen erzeugt.
	 * Danach wird das Passwort gesetzt.
	 * Abschließend wird der Benutzer in die Datenbank gespeichert, wobei auch die ID als Primärschlüssel gesetzt wird.
	 */
	private void createUser(String login, String userName, String companyName, String email, String password, Role... userRoles) {
		final User user = new User(login, userName, companyName, email);
		user.getRoles().addAll(Arrays.asList(userRoles));
		userDAOProxy.changePassword(user, password);
		userDAOProxy.persist(user);
	}

    /**
     * Methode zum Erzeugen von Test-Tickets.
     *
     * Zunächst werden alle verfügbaren Bearbeiter und Kunden erfasst.
     * Anschließend werden Tickets mit Testdaten erzeugt.
     *
     * Dabei wird zufällig festgelegt, welcher Kunde ein Ticket eingereicht hat und welcher Editor ein Ticket gerade
     * in Bearbeitung hat!
     */
	private void createTickets() {
		List<User> editors = userDAOProxy.getAll().stream().filter(user -> user.getRoles().stream().allMatch(role -> role.getName().equals("editor"))).collect(Collectors.toList());
		List<User> customers = userDAOProxy.getAll().stream().filter(user -> user.getRoles().stream().allMatch(role -> role.getName().equals("customer"))).collect(Collectors.toList());
		Random randomIndex = new Random();
		long editorId;

		// Freie Tickets
		createTicket("The Grinch hated Christmas",
				Ticket.Status.open,
				"How the Grinch Stole Christmas!",
				0,
				customers.get(randomIndex.nextInt(customers.size())).getId(),
				new Date()
		);

		createTicket("We should take Bikini Bottom and push it somewhere else",
				Ticket.Status.open,
				"SpongeBob SquarePants",
				0,
				customers.get(randomIndex.nextInt(customers.size())).getId(),
				new Date()
		);

		createTicket("Computer is broken",
				Ticket.Status.open,
				"No Content",
				0,
				customers.get(randomIndex.nextInt(customers.size())).getId(),
				new Date()
		);

		createTicket("IE11 is broken",
				Ticket.Status.open,
				"What's the difference between snowmen and snowladies? Snowballs",
				0,
				customers.get(randomIndex.nextInt(customers.size())).getId(),
				new Date()
		);

		createTicket(
				"I used to like my neighbors",
				Ticket.Status.open,
				"Until they put a password on their Wi-Fi.",
				0,
				customers.get(randomIndex.nextInt(customers.size())).getId(),
				new Date()
		);

		createTicket(
				"I once farted in an elevator",
				Ticket.Status.open,
				"I once farted in an elevator, it was wrong on so many levels.",
				0,
				customers.get(randomIndex.nextInt(customers.size())).getId(),
				new Date()
		);

		// Tickets in Bearbeitung
		editorId = editors.get(randomIndex.nextInt(editors.size())).getId();
		createTicket("Windows startet nicht!",
				Ticket.Status.inProcess,
				"Leider startet mein Windows-PC nicht mehr. Bitte helfen Sie mir!",
				editorId,
				customers.get(randomIndex.nextInt(customers.size())).getId(),
				new Date(),
				new Entry(editorId, "Haben Sie den PC schon einmal aus und wieder an geschaltet?", new Date())
		);

		editorId = editors.get(randomIndex.nextInt(editors.size())).getId();
		createTicket("Ich habe aus versehen system32 gelöscht, was kann ich da tun?",
				Ticket.Status.inProcess,
				"Bitte um schnellstmögliche Hilfe. Frage in Betreff.",
				editorId,
				customers.get(randomIndex.nextInt(customers.size())).getId(),
				new Date(),
				new Entry(editorId, "Haben Sie den PC schon einmal aus und wieder an geschaltet?", new Date())
		);

		createTicket("Windows is broken 3",
				Ticket.Status.inProcess,
				"If con is the opposite of pro, it must mean Congress is the opposite of progress?",
				editors.get(randomIndex.nextInt(editors.size())).getId(),
				customers.get(randomIndex.nextInt(customers.size())).getId(),
				new Date()
		);

		createTicket("How do you make holy water?",
				Ticket.Status.inProcess, "You boil the hell out of it.",
				editors.get(randomIndex.nextInt(editors.size())).getId(),
				customers.get(randomIndex.nextInt(customers.size())).getId(),
				new Date()
		);

		createTicket("What do you call a fat psychic?",
				Ticket.Status.inProcess, "A four chin teller.",
				editors.get(randomIndex.nextInt(editors.size())).getId(),
				customers.get(randomIndex.nextInt(customers.size())).getId(),
				new Date()
		);

		createTicket("Light travels faster than sound",
				Ticket.Status.inProcess,
				"This is why some people appear bright until they speak.",
				editors.get(randomIndex.nextInt(editors.size())).getId(),
				customers.get(randomIndex.nextInt(customers.size())).getId(),
				new Date()
		);

		// Geschlossene Tickets
		createTicket(
				"What do you call a bear with no teeth?",
				Ticket.Status.closed,
				"A gummy bear!",
				0,
				customers.get(randomIndex.nextInt(customers.size())).getId(),
				new Date()
		);

		createTicket(
				"Just deleted the internet",
				Ticket.Status.closed,
				"Please Help",
				0,
				customers.get(randomIndex.nextInt(customers.size())).getId(),
				new Date()
		);

	}

    /**
     * Methode zum Anlegen eines konkreten Tickets.
     *
     * Zunächst wird ein Ticket mit Header erstellt.
     * Anschließend wird der erste Eintrag eines Tickets angelegt und diesem hinzugefügt.
     * Zum Schluss wird das Ticket und dessen Eintrag in die entsprechenden Tabellen der Datenbank gespeichert.
     */
	private void createTicket(String subject, Ticket.Status status, String content, long editorId, long customerId, Date createDate, Entry... additionalEntry){
		final Ticket ticket = new Ticket(subject, status, editorId, customerId);
		final Entry entry = new Entry(customerId, content, createDate);

		ticket.addEntry(entry);
		if (additionalEntry.length > 0) {
			ticket.addEntry(additionalEntry[0]);
		}

		ticketDAO.persist(ticket);
		entryDAO.persist(entry);
		if (additionalEntry.length > 0) {
			entryDAO.persist(additionalEntry);
		}
	}

}