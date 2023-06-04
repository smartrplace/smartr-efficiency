Hinweis: Eine initiale Version wurde in [Trello](https://trello.com/b/NgnqJbhY/operation-issues) verwaltet.

Diese Seite sammelt Probleme und Incidents, die systematisch gelöst werden müssen. Eine Aufzeichnung weiterer Incidents zur Beobachtung bzw. für statistische Zwecke erfolgt in [Incidents_Operation_Log.xslx](https://smartrplace.onlyoffice.eu/Products/Files/DocEditor.aspx?fileid=sbox-75287-%7Cpublic%7COperation%7CProcess%7CIncidents_Operation_Log.xlsx).

## Table of Contents

[[_TOC_]]


## Netzwerk / Festnetz Kunde

### PN101
### VPN-Verbindung auf Laptop möglich, aber nicht vom Controller
* Am 22.11.2021 wurde im Citypoint Bochum ein Controller mit dem Gast-WLAN des Kunden verbunden. WB konnte den Laptop über das gleiche WLAN mit dem VPN verbinden (vielleicht nicht ganz verifiziert), ansonsten vermuten wir, dass der entsprechende Port (1194) im WLAN gesperrt war.
* Lösungsvorschlag: In solchen Situationen sollte ein Gateway angeschlossen werden, das eine Verbindung über einen Tunnel herstellt und eine weitere Fehlersuche ermöglicht.

### PN120
### Gateway offline (Internetverbindung über Kundenanschluss)
* Wenn VPN-Verbindung verfügbar in das Netz, prüfen, ob noch erreichbar. Heartbeat prüfen. Wenn alles offline, Kunden informieren.
* Wenn noch Kontakt z.B. über Controller besteht: Erfordert Prüfung durch JL => Task in Zoho erstellen
* Im Moment keine Erfassung der Detail-Probleme
* Probleme mit dnsmasq etc. kann ggf. auch Child bearbeiten => wenn Jan nicht verfügbar, sollte er sich das Problem anschauen.

#### PN120.1
#### Fehlkonfiguration oder Softwarefehler Smartrplace
Fehler bei Smartrplace. z.B. fehlerhafte DNS-Konfiguration

#### PN120.22
#### Anschlüsse für LAN zum Kunden-Internet und zum Smartrplace-LAN vertauscht
Dies kann z.B. bei Netzwerkarbeiten des Kunden passieren. Beim APU muss die linke Buchse, die normaler Weise mit "LAN-Kabel" markiert ist, mit dem Kunden-Internet verbunden werden, ist also eigentlich die WAN-Buchse. Die rechte Ethernet-Schnittstelle soll mit dem Smartrplace-LAN verbunden werden, dort läuft unser DHCP-Server. Es ist allerdings nicht auszuschließen, dass diese Zuordnung bei Legacy-Gateways nicht immer zutrifft. Auch bei IPUs uns APUs mit mehr als zwei Ethernet-Schnittstellen ist die Belegung ggf. anders.

#### PN120.2
#### Portsperrung o.ä. durch Kunden
Fehler bei Kundenseite

#### PN120.3
#### Kunde trennt unser System Hardware-mäßig ohne Absprache
TODO

### PN130
### Gateway offline (Internetverbindung über Smartrplace-Mobilkarte)
über SIM-Karte, Mobilverbindung

#### PN130.1
#### Fehlkonfiguration oder Softwarefehler Smartrplace
Fehler bei Smartrplace. z.B. fehlerhafte DNS-Konfiguration

#### PN130.2
#### Mobilempfang mit Telekom schlecht im Serverraum und angrenzenden Positionen, weitere Probleme Telekom
* Aufgetreten 11.11.2022: Bei ihk-pruefungszentrum konnte erfolgreich eine Karte von m2m.mobil eingesetzt werden.
* Im IEE-Neubau: Beim Roaming erlaubt Telekom offenbar nur 2G, die Fritzbox unterstützt aber nur 3G/LTE. Daher Versuch, eine Fritzbox 4020 als "Firewall" zu nutzen und die Mobilverbindung wieder über den vorher genutzten Router herzustellen. Es kann aber auch daran liegen, dass das Missbrauchsprofil aktiviert ist und die Telekom deshalb den Zugang nicht zugelassen hat. Ärgerlich, aber nicht mehr getestet, da es mit der anderen Fritzbox funktioniert hat.

#### PN130.3
#### Kunde verändert Hardware-Verkabelung
TODO

#### PN130.4
#### Störung Mobilempfang
z.B. Ausfall bei Mobilfunkprovider so weit bekannt. Bei einmaligen temporären Störungen wird im Zweifelsfall von diesem Fehler ausgegangen


### PN140
### CCU verliert Verbindung

#### PN140.1
#### CCU hängt sich Hardware-mäßig auf
wurde das beobachtet?

#### PN140.2
#### CCU wurde vom Kunden/Nutzer abgesteckt oder entfernt
Kundenfehler

#### PN140.3
#### Netzwerkverbindung wurde vom Kunden getrennt oder verändert
* Z.B. Problem, dass Switch nicht mehr aktiv ist oder CCU sich in anderem Netzwerk befindet.
* Hinweis: Die CCU zieht sich neue IP-Adresse i.d.R. nur bei Neustart, daher muss ggf. bis zum nächtlichen automatischen Neustart gewartet werden.

#### PN140.32
#### CCU erhält vom Gateway-DHCP-Server neue IP-Adresse
* Kritisch bei Konfiguration über IP-Adresse
* Kann ggf. vorkommen, wenn Netzwerkkonfiguration gestört bzw. wenn DHCP-Port temporär mit dem Kundennetz verbunden war, s.a. PN350.22.

#### PN140.33
#### Konfiguration von Smartrplace-Seite fehlerhaft
z.B. Fehler in Homematic-Konfiguration auf SmartrOS

#### PN140.4
#### CCU defekt
Muss ausgetauscht werden.

#### PN140.5
#### CCU bekommt bei nächtlichem Neustart wieder Kontakt
Nur zu wählen, wenn die CCU vorher gar nicht erreichbar war, also ein Remote-Reboot nicht möglich war und das bekannt ist.
Dieses Problem tritt u.a. auf, wenn das DHCP-Lease ausläuft, die CCU sich aber keine neue IP-Adresse zieht und die alte Adresse nicht mehr im Netzwerk akzeptiert wird.

### PN142
### CCU startet nur noch im Recovery Modus
* Bislang beobachtet: Möbelstück drückt auf Taste, diese startet dann nur noch im Recovery Modus. Möbelstück muss abgerückt werden oder Taste muss geeignet abdeckt werden.

### PN145
### Controller verliert Verbindung

#### PN145.1
#### Controller hängt sich Hardware-mäßig auf
wurde das beobachtet?

#### PN145.2
#### Controller wurde vom Kunden/Nutzer abgesteckt oder entfernt
Kundenfehler

#### PN145.3
#### Netzwerkverbindung wurde vom Kunden getrennt oder verändert
Identisch mit PN140.3

#### PN145.4
#### Controller defekt
Muss ausgetauscht werden.

#### PN145.5
#### Controller Settings waren zurückgesetzt
Controller hat damit u.a. WLAN-Settings verloren. Grund unklar, wie das passieren kann. Vielleicht, wenn jemand die Firmware zurücksetzt?

#### PN145.6
#### Nach Restart Treiber/Framework kommt Kontakt zurück
* Wird vermutlich häufig nicht erkannt, durch automatischen MDNS-Neustart täglich

### PN150
### HAP verliert Verbindung

#### PN150.1
#### Durch Firmware-Update lösbar
s. unten

#### PN150.21
#### Problem mit LAN-Verbindung kundenseitig
Kunde hat die LAN-Verbindung getrennt oder Konfiguration verändert

#### PN150.22
#### Problem mit LAN-Verbindung von Smartrplace
LAN-Kabel wurde nicht auf Switch gepatch, ggf. nicht richtig installiert

#### PN150.3
#### Keine Klärung, da HAP nicht benötigt wird
Wenn alle Thermostate auch ohne die ausgefallenen HAPs ausreichend kommunizieren, wird aus Zeit-/Resourcengründen ggf. auf eine Wiederanbindung der HAPs verzichtet. Zur Sicherheit verbleiben diese ggf. noch vor Ort, sollten dann aber später abgebaut werden, wenn sich bestätigt, dass diese nicht benötigt werden.

#### PN150.4
#### HAP über WLAN ohne stabilen Kontakt
HAP ist pingbar, hat aber keinen stabilen Kontakt zur CCU. Dort wird ggf. die IP-Adresse des HAP angezeigt aber DutyCycle/CarrierSense mit 0% und keine Verbindung im Treiber angezeigt.

* Prüfen, ob der HAP in der CCU mit IP-Adresse angezeigt wird. Sofern der HAP nicht mit IP-Adresse angezeigt wird, prüfen, ob die letzte IP-Adresse bekannt ist und versuchen, diese zu pingen.
* Sofern der HAP in der CCU als korrekt angezeigt wird, ist zu erwarten, dass DutyCycle und CarrierSense jeweils auf 0% stehen.
* Prüfen Firmware HAP: Sofern ein Firmware-Update möglich, wird dadurch häufig der Kontakt wieder aufgebaut.
* Prüfen Firmware CCU: Aktualisieren
* Dokumentation für eq-3 wie in dem Task hier: 19170 / 99976, HAP17: HmIP-HAP 0003DD89B3B2DA, s. [Zoho: HAP verliert Kontakt](https://projects.zoho.eu/portal/smartrplacedotde#taskdetail/157619000000094067/157619000000122386/157619000000481025).

### PN155
### HAP noch im Netzwerk erreichbar, aber nicht für CCU
* HAP von CCU aus pingbar, wird aber nicht von CCU

#### PN155.1
#### CCU über WLAN mit Router verbunden
* Beobachtung: Zumindest in manchen Fällen funktioniert AR nicht, wenn CCU über Deco mit Router verbunden ist. HAPs über WLAN scheint aber kein Problem zu sein.
* Generelle Empfehlung: Das Mesh/WLAN/Deco-Netz sollte den DHCP-Server auf der zweiten LAN-Schnittstelle des Gateway nutzen, nicht den Kunden-DHCP oder LTE-Router. Dann sollte auch eine Anbindung der CCU über Deco-Netz funktionieren. 

#### PN155.2
#### Anderes Problem
TODO

### PN160
### Airzone-Gerät verliert Kontakt

#### PN160.1
#### WLAN-Verbindung abgebrochen auf Grund von Abschaltung durch Kunden
TODO

#### PN160.12
#### WLAN-Verbindung abgebrochen durch Defekt/Problem Glinet-Router
* Relevant, wenn Smartrplace eigenes WLAN aufbaut und Aircons darüber angebunden sind

#### PN160.13
#### Verbindung zum Gerät wird wieder aufgebaut nach Neustart OGEMA/Treiber
* Offenbar kann es nach Verbindungsabbrüchen notwendig sein, den Treiber neu zu starten.

#### PN160.2
#### Airzone-Gerät defekt
TODO

#### PN160.3
#### Klimaanlage inkl. Airzone durch Kunden an Sicherung von Versorgung getrennt
z.B. im Winter, wenn die Klimaanlage generell nicht benutzt werden soll

### PN170
### Airzone-Gerät liefert keine plausiblen Temperatur-Messwerte, wenn ausgeschaltet
* Wenn der Switch des Airzone auf off, werden zu hohe Temperaturmesswerte geliefert
* Weitere Analyse noch offen

## Software Development und Deployment

### PN310
### Unerwartete Nebeneffekte Rollout eines Softwareupdates an viele/alle Gateways
Hinweis: Ein Fehler wie unten beschrieben ist in der Form nicht wieder aufgetreten durch eine bessere Teststrategie. Eine generelle Lösung steht noch aus, wird zunächst weiter beobachtet.
* Ein unüberlegtes Update der Fenstererkennung am 28.10.2022 führt in der folgenden Woche bei Beginn der echten Heizperiode zu mehreren Kundenbeschwerden.
* Strategie Software-Updates:
    * Neue Gateways erhalten grundsätzlich den neuesten Software-Stand. Dort wird meistens ohnehin recht intensiv getestet.
    * Bei Problemen/Bedarf erhalten auch Bestands-Gateways die neueste Software. I.d.R. wird dann dort auch getestet.
    * Während der Heizperiode sollen neue Updates möglichst nur alle zwei Monate ausgerollt werden, z.B. im Januar und März. Außerhalb der Heizperiode kann es häufiger Updates geben.
    * Problem: Für Supervision und Datenverwaltung werden ggf. häufigere Updates benötigt. Evtl. ist da aber auch eine Art Salamitaktik sinnvoll, also nach und nach zu aktualisieren. Datenhaltung kann man ggf. auch erstmal in der Gateway-Link-Page machen.

### PN320
### Problem beim Serverbetrieb

#### PN320.1
#### Festplatte voll
* Platz schaffen bzw. Volume erweitern, s. a. [Hetzner2 - Volume Increase](/Server-Administration/OGEMA-instances-on-Smartrplace-servers#hetzner2)

#### PN320.2
#### Störung beim Provider
* Check der Störungsseite des Providers

#### PN320.3
#### Prozess überlastet den Server
* Check Prozesse mit top

### PN330
### Gateway startet nicht richtig auf (SmartrOS-Instanz instabil)
Dieser Fehler trifft zu, wenn das Gateway weiterhin erreichbar ist mit stabilem Tunnel, aber SmartrOS nicht stabil läuft.

#### PN330.1
#### SmartrOS startet immer wieder neu wegen Watchdog
* Check if watchdog is triggered due to OutofMemory or other exception. [Extend watchdog](/Development/Automatical-Update-Mechanisms#disable-or-extend-watchdog)
* Check if git pull does not work, then update script retarts all the time
* Check if slotsDB is zipped, otherwise JL should activate zipping. For very large systems additional measures may be necessary.

#### PN330.2
#### Fehlkonfiguration oder Softwarefehler Smartrplace z.B. Treiber
z.B. SmartrOS startet immer wieder auf wegen fehlerhaftem Treiber

### PN410
### Fehler Booking

#### PN410.1
#### Software-Fehler
entsprechend Rüchmeldung Daniel

#### PN410.2
#### Anwenderfehler
entsprechend Rückmeldung Daniel bzw. initialem Test

#### PN410.3
#### Fehler in der Mandantenkonfiguration
Zu prüfen im CMS

#### Analyse
* Bei neuen Kunden versuchen, das Problem zu testen und ggf. darauf hinweisen, dass funktioniert. Beispiel-Analyse für [BBraun in Mattermost, 12.04.2023](https://mattermost.smartrplace.de/smartrplace/pl/nobzytcm8fdhfx79excyrtjaeh)
* An Daniel geben.

### PN420
### User-Synchronisation meldet "Not authenticated"
* Meldung kommt in User Management Expert -> CMS Connection Analyse
* Beobachtet bei Zecher, 11/2022
* Erklärung: Firewall bricht die HTTPS-Verbindung auf, dadurch sieht das Gateway nicht das Zertifikat des CMS-Server; Lösung: Jan muss das Zertifikat der Firewall herunterladen auf das Gateway, allerdings muss dieses dann regelmäßig aktualisiert werden.
* Auch beobachtet z.B. bei 19179. Hier lag das Problem wohl eher bei Credentials. Child hat das Gateway auf dem CMS neu angelegt, danach ging es.

### PN430
### Fehler Roomcontrol

#### PN430.1
#### Software-Fehler
* Fehler Development inkl. fehlerhafter automatischer Konfiguration

#### PN430.2
#### Fehlerhafte Konfiguration
* Fehlerhafte Konfiguration z.B. durch Operation

#### PN430.3
#### Anwenderfehler
* Anwender hat Bedienung nicht richtig verstanden

#### PN430.4
#### Veraltete Hardware
z.B. Homematic Classic-Thermostate im Einsatz

### PN440
### Fehler SmartrOS
Z.B. Fehler Framework, MQTT-Replicator etc.

### PN450
### Fehler Treiber/Cloud-Connection Konfiguration/Software
### für Gerätefehler/Kundenfehler s. [PN530](/Operation/Incidents_Operation#pn530)
Fehler bei Hardware-Treibern und speziellen Cloud-Quellen-Anbindungen
s.a. PN530.

#### PN450.1
#### Software-Fehler
Gelöst durch Software-Update

#### PN450.2
#### Konfigurationsfehler
z.B. Konfiguration gelöscht etc.

## Homematic Thermostate / Geräte

### PN505
### Kompakt-Thermostate
* Erste Erkenntnisse aus Brucklyn
* Nutzung solcher Thermostate nur, wenn es wirklich notwendig ist. Wenn der Kunde defekte Thermostate manuell einstellen kann, ist eine Lösung bei Problemen meist viel einfacher. Kompaktthermostate sollten mit Wandthermostaten kombiniert werden, damit der Benutzer die Temperaturen vor Ort einstellen kann.
* Prüfen Sie die Ventilstellung im Backend gründlich. Nach der Erstinstallation alle defekten Thermostate durch einen anderen Kompaktthermostaten ersetzen, wenn dies nicht hilft, durch einen herkömmlichen mechanischen Thermostaten ersetzen.

### PN506
### CCU zeigt angelernte Geräte erst nach Neustart
* [Zoho](https://projects.zoho.eu/portal/smartrplacedotde#taskdetail/157619000000098005/157619000000098021/157619000000098023)

### PN510
### Vor-Ort-Anlernen

#### PN510.1
#### CCU hat keine valide Verbindung zum Server
* In diesem Fall soll Antennensymbol soll auf dem Thermostat NICHT dauerhaft gezeigt werden, in Logs Hinweis auf Problem mit "Keyserver" (dieser wird blockiert) oder "connection timed out: secgtw.homematic.com/62.113.249.75:8443" (Veränderung der Anfrage durch den Router)
* Anlernen mit SGTIN/Key oder
* [Verbindung CCU mit VPN](https://gitlab.smartrplace.de/i1/smartrplace/smartrplace-main/-/wikis/Operation-Support/Homematic-CCU3-Configuration#openvpn-setup): Dafür muss ein openVPN-User angelegt werden auf dem [OpenVPN-Server](https://gitlab.smartrplace.de/i1/smartrplace/smartrplace-main/-/wikis/Operation-Support/OpenVPN#openvpn-server), um auf root zu kommen muss persönlicher User, z.B. wbejranonda oder dnestle, verwendet werden.<br>
Wenn die Verbindung nicht erfolgreich, Logs prüfen:
```
more /var/log/messages | grep openvpn
```

#### PN510.2
#### EMV-Störung behindert das Anlernen
* Bislang nicht wirklich nachgewiesen

#### PN510.3
#### Andere CCU lernt Thermostat an
* Entweder das Thermostat ist auf der CCU noch bekannt (dann kann es dort wieder verbunden werden, ohne dass auf der CCU dern Anlernmodus aktiviert ist) oder auf der CCU läuft (noch) der Anlernmodus. Letzteres lässt sich mit aktueller Software (Stand 11.04.23) in den CCU-Logs erkennen, sofern der Anlernmodus über das Gateway getriggert wird.

#### PN510.4
#### CCU-Firmware nicht auf ausreichendem Stand
* Für neuen Thermostat-Typ wird offenbar mindestens 3.69.x benötigt, jedenfalls reicht 3.63.x nicht aus.
* Anleitung Update über Konsole und Konfiguration Logging/Reboot-via-GUI mittels CCU-Commands: [HM-Procedures](/Operation-processes/HM_Procedures#vorbereitung-empfehlung)

#### Analyse und Bearbeitung
* Die CCU-Faults-Page einrichten und testen, s. [Installation Special](https://gitlab.com/smartrplace/ogema-productive/-/blob/master/templates-installation-special/README.md)
* Sollte generell vor einem Vor-Ort-Anlernen eingerichtet und Seite getestet werden. Teilweise muss man erstmal eine ```findlostdevices``` auf der OGEMA-Konsole ausführen, bevor der Zugriff der Seite auf den Service funktioniert (ggf. auch Time-out/Caching-Thema).
* Erste Prüfung: Zeigt die CCU die aktuelle Zeit? Wenn nicht, sind vermutlich diverse Ports blockiert.
* Prüfen, Zugriff auf Homematic-Server möglich, Eingabe:
```
curl secgtw.homematic.com:8443
```
Erwartetes Ergebnis:
```
curl: (52) Empty reply from server
```
Wenn keine Verbindung erfolgt, dann muss die CCU ins VPN eingebunden werden.
Test VPN
```
curl 85.209.50.11:1194
```
* Ansonsten CCU über mobiles Internet oder an anderer Stelle anbinden (direkt am Gateway), um nötigen Internetzugriff zu ermöglichen. Bei einzelnen Geräten ggf. per SGTIN und KEY, sofern verfügbar.
* Für Development: Weitere Debugging-Hinweise unter [HomematicIP Einlernen](/Hardware/Homematic-Debugging-Protocols#homematicip-einlernen)

## Weitere Geräte

### PN530
### Gerät hat auf Netzwerkebene Kontakt, aber nicht in OGEMA/Treiber
* z.B. WIZ-Lampe, Airzone

#### PN530.1
#### Treiberneustart oder Framework-Neustart hilft
* Sollte bei solchen Problemen immer von Operation gemacht werden

#### PN530.2
#### Treiber-Konfigurationsfehler
TODO

#### PN530.3
#### Treiber-Softwarefehler
TODO

#### PN530.4
#### Gerät falsch konfiguriert von Seiten Operation
TODO

#### PN530.5
#### Gerät falsch konfiguriert von Seiten Kunde
TODO

#### PN530.6
#### Unerwartetes Geräte-Verhalten erfordert Workaround
Kann/muss von SmartrOS implementiert werden, aber eigentlich Fehler auf Seiten Gerät/Datenquelle.

### PN605
### Thermostate senden regelmäßig unerwartete Setpoints, die nicht manuell gesetzt wurden / übernehmen Setpoints nicht

#### PN605.1
#### Plus-/Minus-Taste
* Fehler 1: ein Gegenstand drückt regelmäßig auf die "-" oder "+"-Taste (bei eTRV-B)

#### PN605.2
#### Auto-Setpoint-Kurve wurde nicht korrekt an Thermostat übertragen
* Bislang nur manuelle Überwachung
* Auf der OGEMA-Konsole kann man alle Thermostate überprüfen und bei Abweichungen automatisch neu übertragen lassen mit
```
hmhl:checkprograms
```

* In der Room Control Status-App kann man auf der Raumseite auch die Neu-Übertragung für einzelne Räume bzw auf [Thermostat Auto-Mode Management](https://xxx.smartrplace.de/org/smartrplace/hardwareinstall/superadmin/thermostatAuto.hmtl.html) für einzelne Thermostate triggern.

#### PN605.22
#### Auto-Mode nicht erfolgreich deaktiviert, obwohl bestätigt
* Es kann offenbar vorkommen, dass das Thermostat die Einstellung nicht übernommen hat, obwohl von der CCU der Manu-Mode bestätigt wurde.
* In diesem Fall für das Thermostat manuelle Setpoints temporär deaktivieren mit Property *org.smartrplace.apps.heatcontrol.logic.faultymanualdevice.list*. Außerdem ein Resend durchführen und einen Tag später noch mal prüfen.

#### PN605.3
#### Temperatursturzerkennung
* Fehler 3: Wird ausgelöst durch Temperatursturzerkennung (sollte dann aber ignoriert werden von Roomcontrol)

#### PN605.4
#### Defekt Thermostat / Firmware
* Fehler 4: Offenbar senden manche Thermostate gelegentlich falsche Werte => muss mit eq-3 abgeklärt werden

#### PN605.5
#### Verbleibende Direktkopplung mit Wandthermostat bei Dual-Heating/Cooling-Räumen
* In diesem Fall übernimmt das Wandthermostat im Kühlfall den Setpoint zum Kühlen bzw. vom Airconditioner. Sofern noch eine Direktkopplung an ein Thermostat besteht, springt dieses ggf. ständig zwischen einem sehr hohen und einem sehr niedrigen Wert.
* Eventuell handelt es sich hier um ein Problem der Kommunikation zwischen Gateway und CCU, s. Task [Direktkopplung](https://projects.zoho.eu/portal/smartrplacedotde#taskdetail/157619000000094067/157619000000122386/157619000000371047).

#### PN605.6
#### Übertragung Manu/Auto-Mode und Setpoints gestört durch EMV-Problem
* Überprüfung des CarrierSensLevel der CCU bzw. des HAP, der für die Kommunikation relevant ist.
* Versetzen des HAP/der CCU. Möglichkeit Umstellung auf Fußbodenheizung durch Hinführung Kabel in den kritischen Bereich?

#### PN605.7
#### Übertragung Setpoints nicht erfolgreich durch AdvancedRouting-Problem
* Die Frames der Thermostate werden wohl prinzipiell sowohl von den HAPs mit Weiterleitung als auch von der CCU aufgenommen. Informationen von der CCU zu den Thermostaten gehen nur entweder über HAP oder CCU. Wenn da ein Weg gewählt wird, der faktisch nicht funktioniert, kommen die Setpoints nicht an. Im Extremfall ist umlernen auf eigene CCU erforderlich oder eine Änderung der Netzwerkkonfiguration. 

#### Analyse 1
Folgendes ist zu prüfen:
* Auf der Seite [Thermostat Auto-Mode Management](https://xxx.smartrplace.de/org/smartrplace/hardwareinstall/superadmin/thermostatAuto.hmtl.html):
    * Prüfen, ob Automode aktiv. Sofern eigentlich nicht aktiv sein soll, erneut senden
    * Sofern nicht aktiv, die Auto-Kurve erneut senden. Man könnte auch einfach beides erneut senden, wenn man das automatisiert.
* Nach 2-3 Tagen erneut prüfen, sofern nicht ohnehin wieder Alarm kommt.
* Release KnownIssue mit 605.2 / 605.22 wenn erfolgreich
* Wenn das Problem wiederholt auftritt, weiter nach Analyse 2

#### Analyse 2
* Ist der Auto-Mode deaktiviert, wenn eigentlich Eco Mode oder Summer Mode, Booking etc.?
* Sofern der Auto-Mode korrekt gesetzt ist, die Auto-Kurve erneut senden, s. PN605.2. Das ist insbesondere relevant, wenn die falschen Setpoints immer zur gleichen Uhrzeit kommen. Bei sehr willkürlichen Setpoints ist dies eher unwahrscheinlich.
* In diesem Fall die Thermostate, die im Verdacht stehen, den Fehler in dem jeweiligen Raum zu verursachen, aus dem manuellen Setzen herausnehmen mit der Property *org.smartrplace.apps.heatcontrol.logic.faultymanualdevice.list*. Dann kann man erkennen, welche Thermostate wann falsche Setpoints senden, ohne dass die übrigen Thermostate direkt folgen.
* TODO: Check T48,T52, 19135, am 5.4.23
* s.a [Thermostat springt von alleine ständig auf 4.5/5.0 oder 30.0/30.5](Operation/Alarms-for-Supervision#ac901)

### PN607
### Parameter werden nicht korrekt an Gerät übertragen
z.B. EmptyPos

#### PN607.1
#### Übertragungsprobleme zwischen CCU und Gerät
Vermutung, dass hier Problem liegt. Ganz detaillierte Analyse aber noch offen

### PN610
### Automatischer Modus wird erst noch einigen Stunden auf Thermostaten aktiviert
* Beobachtung Immovito, 08.11.2022, abends: Freischaltung manueller Modus auf Roomcontrol-Backup-Page. Eco Modus ein->aus führt zu kurzzeitiger Aktivierung Auto-Modus auf Thermostaten, wird aber schnell zurückgesetzt. Am nächsten Morgen ist Auto-Modus aber aktiviert. 
* Mögliche Erklärung: Auto-Modus ist kritisch. Erfolgreiche Übertragugn der Setpoint-Kurve muss erst geprüft werden.

### PN620
### Thermostat vergessen zu installieren
* Gelegentlich werden Heizkörper bei der Installation der Thermostate übersehen und müssen bei einem späteren Wartungstermin noch nachinstalliert werden. I.d.R. erfordert dies ein Anlernen vor Ort.

## Kritische Probleme Thermostate
## Heizkörper nicht kalt / nicht warm / F1/F2/F3-Fehler / Batterielaufzeit / Kontaktverlust

### PN810
### Ventil öffnet nicht, auch wenn dies auf Grund von Setpoint und Measurement notwendig wäre
* Abgrenzung zu [Setpoint nicht übernommen](/Operation-processes/HM_Procedures#analyse-setpoint-nicht-%C3%BCbernommen): Setpoint wird vom Thermostat bestätigt.
* Könnte mit November-Hardware-Problem zusammenhängen, erscheint allerdings eher unwahrscheinlich
* s. [Erste Bearbeitung bei Prius ab 11/2022](/Hardware/Homematic-Debugging-Protocols#thermostate-melden-keine-ventil%C3%B6ffnung-mehr-bei-prius)
* Hilft evtl. auch Entkalkungsfahrt?
* These: Erfordert Zurücksetzen in Werkzustand und neu anlernen

#### Empfohlene Prozedur Rücksetzen in den Werkzustand Remote
* Hinweis: Sofern der Fehler später an eq-3 gemeldet werden soll, muss zunächst das Thermostat auf "protokolliert" setzen und nach der Prozedur das Systemprotokoll-CSV-File zusammen mit einem Backup der CCU an den eq3-Pro-Support pro-support@eq-3.de schicken. Vor der Prozedur möglichst mindestens einige Stunden loggen (mindestens 3 Stunden, möglichst 6 Stunden).
* Öffnen der [Auto-Curve-Manamgent-Page](https://xxx.smartrplace.de/org/smartrplace/hardwareinstall/superadmin/thermostatAuto.hmtl.html) des Gateways. Prüfen auf welcher CCU das Thermostat eingebunden ist.
* Die vierstellige ID des Thermostats notieren.
* Öffnen der [CCU-Page](https://xxx.smartrplace.de/org/smartrplace/hardwareinstall/superadmin/ccutDetails.hmtl.html)
* Starten Anlern-Modus für die entsprechende CCU
* Empfehlung: Battery Change Mode aktivieren auf [Room Status Control](https://xxx.smartrplace.de/org/smartrplace/apps/smartrplaceheatcontrolv2/index.html). Ansonsten sollte am Ende der Setpoint des Thermostats auf der [Setpoint Page](https://xxx.smartrplace.de/org/smartrplace/hardwareinstall/superadmin/thermostatDetails.hmtl.html) manuell korrigiert werden.
* Öffnen der Web-Interface der entsprechenden CCU. Starten Anlern-Modus HM-IP => Prüfen, ob nach einer Minute automatisch wieder neu gestartet wird
* Öffnen Web-Interface der CCU in weiterem Tab. Auf Einstellungen->Geräte. Mit Hilfe der vierstelligen ID das Thermstat suchen. Auf Löschen => Rücksetzen in den Werkzustand
* Prüfen, dass Posteingang im anderen CCU-Tab auf 1 springt. In den Posteingang wechseln und dort mit "Fertig" bestätigen
* In der Auto-Mode-Page prüfen, ob Kontakt zu Thermostat wieder besteht. Wenn VErr den Status 2 erreicht hat, dann mit "Stat ADA" Adaptierfahrt starten.
* Wenn Adaptierfahrt fertig, noch auf "Curv Resnd" für das Thermostat klicken, auch auf "Resend".

ggf. entspricht dies PN640:
#### PN640
#### Thermostate öffnen oder schließen nicht mehr richtig
Hinweis: Der Fehler ist sehr allgemein... TODO
* Noch unklar, ob ein Reset in Werkzustand das Problem behebt
* Bei Prius: T114: Hat sich nicht bewegt (vermutlich dauerhaft offen)
* Bei Prius: T308: Unklar, ggf. gar nicht im Einsatz
* Bei Prius: T053, T057: Haben nicht mehr richtig geschlossen, nach Tausch durch Ersatzthermostate ging es problemlos; am 01.02.23 in Werkzustand zurückversetzt und bei CCU-003, Fidt8 angelernt. Batterien von T53 mussten getauscht werden
* Bei BZ-Kassel2: T559, T562 (eines nicht geöffnet, das andere nicht geschlossen; Ersatzthermostate funktionieren direkt)


### PN820
### (zu besetzen)


### PN830
### Temperatur relativ hoch, auch wenn Ventil geschlossen ist

#### PN830.1
#### Weitere Wärmequelle im Raum
z.B. Heizlüfter, Maschine; offene Heizungsrohre im Raum, auch Sonneneinstrahlung denkbar, wenn nur temporär: Sofern es ein Wandthermostat gibt, sollte die Direktkopplung aufgehoben werden und geprüft werden, ob Heizkörper oder Raum wärmer ist; wenn Heizkörper immer wärmer als Raum, dann schließt vermutlich der Heizkörper nicht richtig. Ansonsten kommt die Wärme vermutlich von benachbarten Räumen oder von Wärmequelle im Raum. Ansonsten den Kunden bitten, wenn das Ventil des Heizkörpers schon seit einigen Stunden geschlossen war, zu prüfen, ob der Heizkörper trotzdem warm ist (wärmer als der Raum).<br>
Offene Heizungsrohe, die nahe am Thermostat verlaufen, können ggf. Remote gar nicht von warmem Heizkörper unterschieden werden. Darauf sollte generell bei der Installation geachtet werden.

#### PN830.2
#### Heizkörper schließt generell nicht richtig
Problem Kundenseite

#### PN830.3
#### Ventil fährt nicht richtig zu (Problem Adaption)
Dies kann sich in einem F2-Fehler zeigen, es kann aber auch sein, dass das Thermostat nicht korrekt erkennt, dass es nicht richtig schließt und z.B. eine Münze benötigt. Dies sollte sich durch eine neue Adaptierfahrt lösen lassen.

#### PN830.31
#### Gelöst durch Einlegen einer Scheibe/Münze
Standardlösung für den Fall PN830.3

#### PN830.32
#### Thermostat defekt durch Fremdeinwirkung
Durch mechanische Einwirkung kann das Thermostat defekt sein und nicht mehr richtig schließen. Dies kann sich auch in einem F2-Fehler äußern.<br>
Dies handelt sich um einen Kundenfehler, der aber i.d.R. von Smartrplace behoben werden muss.

#### PN830.33
#### Thermostat gelockert durch Fremdeinwirkung
Es ist möglich, dass ein Thermostat durch Fremdeinwirkung zwar nicht defekt ist, aber sich gelockert hat. In diesem Fall muss dieses neu festgeschraubt und adapiert werden. Es ist im Nachheinein häufig nicht eindeutig zu klären, ob sich das Thermostat quasi "von selbst" gelockert hat, was eigentlich ein Fehler von Homematic bzw. der Montage wäre, oder ob es übermäßige Fremdeinwirkung gab. Auf Grund der Erfahrungen mit beschädigten Thermostaten bzw. der Fremdeinwirkung im Jugendzentrum im Verhältnis zu den Fehlern von gelockerten Thermostaten, ist im Zweifelsfall von einer Fremdeinwirkung auszugehen, wenn nicht ein Problem mit einem (Kunststoff-)Adapter zu vermuten ist.

#### PN830.34
#### Thermostat gelockert da Kunststoffadapter
Bei einigen Heizkörpern halten die Kunststoffadapter nicht dauerhaft. In diesem Fall sollten Metalladapter eingebaut werden.

#### PN830.35
#### Thermostat-Tausch löst Problem, kein weiterer Grund erkennbar
* Thermostat nicht locker oder erkennbar defekt, Test an anderem Heizkörper zeigt kein Problem.

#### PN830.36
#### Rücksetzen in den Werkzustand löst das Problem, kein weiterer Grund erkennbar
* [Prozedur Rücksetzen in den Werkzustand](/Operation/Incidents_Operation#empfohlene-prozedur-r%C3%BCcksetzen-in-den-werkzustand-remote)
* TODO: Noch nicht richtig erprobt.

#### PN830.4
#### Hardware-Fehler des Thermostats
Stand 10.04.2023 ist noch nicht bestätigt, dass das Problem mit der Bauteiltoleranz bei einer Lichtschranke im Thermostat dieses Problem verursachen kann, dies ist aber zumindest plausibel.<br>
Aus diesem Grund sollten Thermostate, die potenziell aus der betroffenen Charge stammen, bei denen ein F2-Fehler auftritt, ausgetauscht werden.

#### PN830.5
#### Problem mit der Firmware
Ein solches Problem trat ggf. bei Prius auf. Allerdings wurde dies bislang noch nicht systematisch nachgewiesen. Wenn eine neue Adaptierfahrt nicht hilft, aber ein Rücksetzen des Thermostats in den Werkzustand das Problem löst, dann dürfte es sich um diesen Fall handeln. 

#### PN830.6
#### Temperaturmessung defekt
unwahrscheinlich, wenn Temperaturkurve ansonsten plausibel, bislang noch nicht nachgewiesen.

#### PN830.7
#### Thermostat abgebaut vom Kunden
F2-Fehler können auch auftreten, wenn das Thermostat vom Kunden abgebaut wurde, z.B. weil dieses nicht richtig öffnet oder sonst nicht richtig funktioniert. Man kann auch nicht generell davon ausgehen, dass der Kunde uns informiert.

##### Analyse
* Prüfung der betroffenen Räume in den Roomcontrol-Charts. Auch Prüfung der Ventilstellungen.
* Die Fälle (2), (3), (4) und (5), ggf. auch (1) sind schwer zu unterscheiden. Hier muss der eTRV abgebaut und mit dem nicht-elektronischen Thermostat getestet werden, ob der Heizkörper kalt wird. Ggf auch Nutzer befragen, ob der Heizkörper früher schon nicht richtig geschlossen hat. Die Analyse und Bearbeitung der beiden Fälle erfolgt entsprechend [PN832](/Operation/Incidents_Operation#pn832).<br>
TODO: Unterscheidung (1) bis (5) fehlt noch. Fall (6) kann zunächst ohne besondere Beachtung bleiben, falls es nicht direkt Hinweise im Chart gibt, die auf eine defekte Messung hindeuten.
* Ggf. kann auch das [Rücksetzen in den Werkzustand](/Operation/Incidents_Operation#empfohlene-prozedur-r%C3%BCcksetzen-in-den-werkzustand-remote) helfen.

### PN831
### F2-Fehler (ValveErrorState=6)

#### PN831.1
#### Arbeit am Ventil erforderlich
* Dieser Punkt erfordert weitere Analyse nach PN832.
* I.d.R. muss eine Münze eingelegt werden, der Fehler kann aber auch an einem Defekt des Thermostats durch mechanische Einwirkung oder durch den bekannten Hardware-Fehler ausgelöst werden.
* Prüfen, ob der Heizkörper warm wird oder ob dieser dauerhaft zu warm ist. Prüfung muss in jedem Fall entsprechend [PN832](/Operation/Incidents_Operation#pn832) erfolgen. Sofern der Heizkörper noch warm und auch wieder kalt wird und auch die Batterie nicht schnell leer geht, kann dies bei der nächsten Wartung erfolgen, ansonsten muss eine schnelle Klärung herbeigeführt werden. Für diese Prüfung muss ggf. die Direktkopplung zum Thermostat aufgehoben werden. Aktuell ist das nur für alle Thermostate gleichzeitig im Raum möglich über [Room Status Control](https://xxx.smartrplace.de/org/smartrplace/apps/smartrplaceheatcontrolv2/index.html), das sollte in Zukunft modifiziert werden.
* Sofern starker Verdacht auf einen "spontanen F2-Fehler" durch eine wöchentliche Entkalkungsfahrt besteht, soll die automatische Verschiebung (Abschaltung der Entkalkungsfahrt) aktiviert werden auf [Room Status Control Main Page](https://xxx.smartrplace.de/org/smartrplace/apps/smartrplaceheatcontrolv2/index.html), "PostponeMode" (farbiger Button).

#### PN831.2
#### Lösung durch Entkalkungsfahrt
In seltenen Fällen kann das Problem so gelöst werden.

#### PN832
#### Bearbeitung: Thermostat schließt nicht richtig, weil Ventil nicht ausreichend zufährt
#### Bearbeitung: Thermostat mit F2-Fehler
Die folgende Prozedur soll angewendet werden, wenn auf Grund der unter Analyse genannten Kriterien ein Problem mit dem eTRV (als von Seiten Homematic/Smartrplace) nicht ausgeschlossen werden kann. Sofern wahrscheinlich oder klar ist, dass das Problem auf Seiten des Kunden liegt, sollte dies kommuniziert und geklärt werden, wie dieser das Problem abstellen kann bzw. dokumentiert werden, dass es sich um kein Problem handelt (Anpassung der Alarming-Grenzen und Dokumentation, TODO: Spezielle Form des Special Settings-Incident).<br>
Hinweis 1: Sofern das Thermostat mit Diebstahlschutz installiert wurde, ist dieser Vorgang nur mit Spezialwerkzeug oder Aufbrechen des Diebstahlschutzes möglich. In diesem Fall kann der Kunde i.d.R. den Vorgang nicht selbst durchführen oder muss mit dem Werkzeug ausgestattet sein.<br>
Hinweis 2: In einigen Fällen führt in F2-Fehler schon nach wenigen Tagen zu einer starken Absenkung der Batteriespannung. In diesem Fall sollte dann auch gleich die Batterie des Thermostats ausgetauscht werden, auch wenn diese noch nicht ganz leer ist.<br>
Das Thermostat muss neu adaptieren, erfordert i.d.R. eine Münze. Die folgende Anleitung bezieht sich auf Standard-Thermostate (eTRV-2):
* Schritt 1: Prüfen, ob das Thermostat fest am Heizkörper sitzt. Sofern die Verschraubung durch einen Nutzer gelockert hat oder sich z.B. bei Einsatz eines Adapters gelockert hat, muss das Thermostat wieder fest aufgeschraubt und adapiert werden entsprechend der Anleitung unten. Allerdings wird dann keine Münze/Metallscheibe eingelegt. Sofern ein Kunststoffadapter zum Einsatz kommt, sollte dieser baldmöglichst gegen einen Metalladapter getauscht werden.
* Schritt 2: Thermostat auffahren: Rad für ca. 1 Sekunden drücken, dann sollte Boost-Zähler beginnend bei 300 herunterzählen (Hinweis: Das Rad lässt sich nicht nur drehen, sondern auch an der runden Oberfläche als Taster drücken)
* Schritt 3: Thermostat abschrauben: Nach ca. 10 bis 20 Sekunden ist das Thermostat so weit geöffnet, dass die Verschraubung an der Verbindung zum Heizkörper mit der Hand gelöst werden kann (in Ausnahmefällen Wasserrohrzange erforderlich)
* Schritt 4: im Thermostat eine Münze einlegen. Optimal geeignet sind 5ct-Münzen, auch 1ct/2ct-Münzen sind möglich oder entsprechende Metallscheiben, sofern verfügbar. Die Münze wird am Thermostatkopf in den Ring eingelegt, der sich in der Verschraubung befindet. Anschließend wird das Thermostat einschließlich der Münze wieder auf den Heizkörper aufgesetzt und festgeschraubt.
* Schritt 5: Adaptierfahrt starten: Nach dem Anschrauben eine Batterie für ca. 20 Sekunden entnehmen. nach dem Einlegen warten, bis auf dem Display oben "Valve adapt" erscheint. Anschließend durch Drücken des Rads die Adaptierfahrt starten. Nach dem Abschluss der Adaptierfahrt sollte die Sollwerttemperatur im Display angezeigt werden.

#### Weitere Optionen
* Wenn die vorherigen Optionen nicht geholfen haben, klassisches analoges Thermostat wieder aufsetzen und damit testen, ob damit der Heizkörper richtig schließt. Das abgebaute Thermostat möglichst nah an der bisherigen Position (auf jeden Fall nach am Heizkörper, ggf. darauf, wenn nicht zu heiß) ablegen, um weiterhin die Temperatur zu überwachen. Den Schritt im Incident dokumentieren und sicherstellen, dass nach der Analyse weitere Aktion erfolgt (wobei der Verbleib des analogen Thermostatkopfs mit dem Kunden vereinbart werden kann, wenn eTRV dort nicht funktioniert). Wenn der Heizkörper weiter heiß bleibt, muss der Kunde diesen reparieren, ggf. auch ganz abklemmen.

### PN840
### Heizkörper wird nicht warm, auch wenn Ventil geöffnet ist
Analyse und Möglichkeiten

#### PN840.1
#### Fenster im Raum geöffnet
Wenn der Heizkörper zudem eher schwach versorgt ist, kann es sein, dass dann kaum eine Reaktion bei Öffnen des Ventils zu erkennen ist. I.d.R. sollte aber eine Abhängigkeit von der Außentemperatur erkennbar sein.

#### PN840.2
#### Heizkörper nicht versorgt
Heizkörperstrang ist abgeklemmt oder wird aus anderem Grund gar nicht warm/durchströmt. Das Verhalten tritt auch auf, wenn dieser so viel Luft hat, dass gar kein Wasser durchkommt. (Problem Kundenseite).

#### PN840.22
#### Heizkörper muss entlüftet werden
Heizkörper muss entlüftet werden (Problem Kundenseite).

#### PN840.23
#### Heizkörper ist verkalkt
Heizkörper muss entkalkt werden (Problem Kundenseite).

#### PN840.3
#### Ventil fährt nicht richtig auf
Entweder Fehler mit der Adaption oder mit der Firmware (TODO, s. PN830).

#### PN840.4
#### Hardware-Fehler des Thermostats
Stand 10.04.2023 ist noch nicht bestätigt, dass das Problem mit der Bauteiltoleranz bei einer Lichtschranke im Thermostat dieses Problem verursachen kann, dies ist aber zumindest plausibel.

#### PN840.5
#### Temperaturmessung defekt
(unwahrscheinlich, wenn Temperaturkurve ansonsten plausibel, bislang noch nicht nachgewiesen)

#### PN840.6 
#### Nachtabsenkung
Es kann sein, dass zum Zeitpunkt der Messung die zentrale Heizungsanlage im Nacht-/Wochendabsenkungsprogramm war. Dies lässt sich durch einen Test zur normalen Nutzungszeit ausschließen, wenn auch andere Heizkörper warm werden.

#### Analyse Supervision
Der Fall PN840.6 sollte zumindest als klarer Verdacht erkennbar sein. Fall PN840.1 tritt i.d.R. nur bei speziellen Immobilien auf mit Bauaktivitäten bzw. Lowcost-Kurzzeitvermietung. Auch der Fall PN840.5 muss aus außer bei besonderen Auffälligkeiten nicht berücksichtigt werden. Die Fälle (2) und (3) sind schwer zu unterscheiden. Hierfür muss die Prozedzur [Heizkörper wird nicht warm](/Operation/Incidents_Operation#pn842) entweder durch den Kunden oder durch Smartrplace durchgeführt werden.<br>
Außerhalb der Heizperiode lässt sich nicht prüfen, ob der Heizkörper überhaupt warm wird. Sofern hier Unklarheit besteht, den Wartungstermin auf den Beginn der neuen Heizperiode verschieben.

### PN841
### F1-Fehler (ValveErrorState=5)
* Entkalkungsfahrt durchführen (Stand 10.04.23 noch nicht automatisiert) auf [Thermostat Valve Management](https://xxx.smartrplace.de/org/smartrplace/hardwareinstall/superadmin/thermostatValve.hmtl.html), "Decalc Now" beim entsprechenden Thermostat. Es kann sein, dass man dazu das automatische Shifting abschalten muss unter [Room Status Control Main Page](https://xxx.smartrplace.de/org/smartrplace/apps/smartrplaceheatcontrolv2/index.html), "PostponeMode" (farbiger Button).

#### PN841.1
#### Entkalkungsfahrt löst Problem
* Nach der Entkalkungsfahrt wird wieder ValveErrorState=4 erreicht.

#### PN841.2
#### Weitere Analyse notwendig
* Sofern dies keinen Erfolg hat, prüfen, ob der Heizkörper warm wird oder ob dieser dauerhaft zu warm ist. Prüfung muss in jedem Fall entsprechend [PN842](/Operation/Incidents_Operation#pn842) erfolgen. Sofern der Heizkörper noch warm und auch wieder kalt wird, kann dies bei der nächsten Wartung erfolgen, ansonsten muss eine schnelle Klärung herbeigeführt werden. 

#### PN842
#### Bearbeitung: Heizkörper wird nicht warm
#### Bearbeitung: Thermostat mit F1- oder F3-Fehler
Hinweis: Sofern das Thermostat mit Diebstahlschutz installiert wurde, ist dieser Vorgang nur mit Spezialwerkzeug oder Aufbrechen des Diebstahlschutzes möglich. In diesem Fall kann der Kunde i.d.R. den Vorgang nicht selbst durchführen oder muss mit dem Werkzeug ausgestattet sein.<br>
Das Thermostat muss neu adaptieren, sofern eine Münze eingelegt ist, sollte diese entfernt werden. Die folgende Anleitung bezieht sich auf Standard-Thermostate (eTRV-2):
* Schritt 1: Thermostat auffahren: Rad für ca. 1 Sekunden drücken, dann sollte Boost-Zähler beginnend bei 300 herunterzählen (Hinweis: Das Rad lässt sich nicht nur drehen, sondern auch an der runden Oberfläche als Taster drücken)
* Schritt 2: Thermostat abschrauben: Nach ca. 10 bis 20 Sekunden ist das Thermostat so weit geöffnet, dass die Verschraubung an der Verbindung zum Heizkörper mit der Hand gelöst werden kann (in Ausnahmefällen Wasserrohrzange erforderlich)
* Schritt 3: Prüfen, ob der Heizkörper warm wird: Dieser Schritt ist nur möglich, sofern die zentrale Heizungsanlage läuft. Außerhalb der Heizperiode muss dieser Schritt ggf. übersprungen werden. Zunächst sollte dies direkt am Anschluss des Thermostats spürbar sein. Sofern der Heizkörper auch nach 10 Minuten gar nicht spürbar warm wird und keine Nacht- oder Wochenendabsenkung etc. aktiv ist, wird der Heizkörper nicht versorgt. In diesem Fall muss der Heizkörper entlüftet werden oder die Warmwasserversorgung muss durch einen Heizungsbauer aktiviert werden. In diesem Fall muss das Thermostat nur wieder aufgesetzt werden ab Schritt 8.
* Schritt 4: Sofern WD40-Spray verfügbar ist: Das Ventil am Heizkörper mit WD40-Spray behandeln, um den Stift leichtgänger zu machen.
* Schritt 5: Prüfen, ob der Stift sich leicht mit einer Zange oder anderem harten Gegenstand einfahren lässt gegen den Federdruck.
* Schritt 6: Sofern sich eine Metallscheibe/Münze zwischen Thermostat und Heizkörper befindet, diese entfernen. Die Münze befindet sich (sofern vorhanden) am Thermostatkopf in den Ring eingelegt, der sich in der Verschraubung befindet. Sollten mehrere Scheiben eingelegt sein, nur eine Scheibe entfernen. 
* Schritt 7: Anschließend wird das Thermostat wieder auf den Heizkörper aufgesetzt und festgeschraubt.
* Schritt 8: Adaptierfahrt starten: Nach dem Anschrauben eine Batterie für ca. 20 Sekunden entnehmen. nach dem Einlegen warten, bis auf dem Display oben "Valve adapt" erscheint. Anschließend durch Drücken des Rads die Adaptierfahrt starten. Nach dem Abschluss der Adaptierfahrt sollte die Sollwerttemperatur im Display angezeigt werden.

### PN843
### Err-Fehler (ValveErrorState=0)
* I.d.R. erforderlich: [Prozedur Rücksetzen in den Werkzustand](/Operation/Incidents_Operation#empfohlene-prozedur-r%C3%BCcksetzen-in-den-werkzustand-remote)

#### PN843.1
#### Durch Rücksetzen in den Werkzustand gelöst
OK

#### PN843.2
#### Nicht durch Rücksetzen in den Werkzustand gelöst
TODO

### PN850
### Thermostat oder anderes Homematic-Gerät verliert Funkkontakt

#### PN850.1
#### Batterie leer
Sollte in diesem Fall immer geprüft werden.

#### PN850.2
#### Funkreichweite nicht ausreichend
Die Funkstärke ist zu schwach. Wenn dies nur ein einzelnes Thermostat betrifft und andere Geräte in der Nähe ausreichend Kontakt haben, dann sollte das Gerät getauscht werden. Offenbar gibt es einige gewisse Streuung bei der Antennenstärke etc. Wenn hier ein Gerät mit eher schwacher Charakteristik an einem ungünstigen Ort sitzt, dann kann es zu Problemen kommen. Ggf. kann das Gerät an anderer Stelle noch verbaut werden oder sollte nach Möglichkeit beim Hersteller umgetauscht werden.<br>
Evtl. kann dieses Problem auch auftreten, wenn die Batteriespannung niedriger wird, also nicht von Anfang an. Sofern die Batterie besonders schnell leergeht, ist von einem solchen Problem auszugehen. In diesem Fall das Gerät tauschen.

#### PN850.22
#### Funkreichweite nicht ausreichend mit Vermutung bekannter Hardware-Fehler
Möglicher Weise kann dieses Problem auch mit dem bekannten Hardware-Fehler zusammenhängen, in diesem Fall sollte der Tausch basierend auf dem bekannten Problem erfolgen.

#### PN850.23
#### Gerät am AR-Limit
* Gerät müsste eigentlich Advanced Routing machen, stellt den Kontakt aber trotzdem nur zur CCU her. Der Fehler soll gewählt werden, wenn dies so massiv auftritt, dass die Nutzbarkeit des Geräts deutlich eingeschränkt ist.

#### PN850.3
#### Batterien und/oder das Thermostat könnte vom Nutzer entfernt worden sein
Batterien und/oder das Thermostat könnte vom Nutzer entfernt worden sein.

#### PN850.4
#### Absturz Firmware
* Die Firmware könnte abgestürzt sein

#### PN850.5
#### Thermostat defekt
* Auch defekter Stellantrieb FAL/FALMOT.

### PN860
### Thermostat hat immer wieder Lücken über viele Stunden oder sogar Tage

#### PN860.1
#### Funkreichweite nicht ausreichend
Wahrscheinlichstes Problem

#### PN860.2
#### HAP wird z.B. über Zeitschaltuhr oder manuell aus- und angeschaltet
* Relevant bei Advanced Routing
* Sollte am HAP erkennbar sein

#### PN860.3
#### Funkstörer oder stark dämpfende Tür etc. ändern gelegentlich ihren Status
* Strecke müsste dafür vor Ort nachverfolgt werden

#### PN860.4
#### Bei Versorgung über Battery Pack wird Stromversorgung gelegentlich ausgeschaltet
* Müsste an ValveErrorState=2 bzw. folgende Adaptierfahrt beim Einschalten erkennbar sein.

### PN870
### Thermostat hat zu kurze Batterielaufzeit

#### PN870.1
#### Batterielaufzeit unerklärlich kurz
* Stand 10.04.2023: Bearbeitung in [Kurze Batterielaufzeiten vermeiden](https://projects.zoho.eu/portal/smartrplacedotde#taskdetail/157619000000094067/157619000000122386/157619000000481003).
* Wichtig: Wechsel durch Kunden bzw. Wartungstermin veranlassen

#### PN870.2
#### Batterielaufzeit kurz, aber erklärlich
* Thermostat schwergängig, hat schwachen Funkkontakt, viele Bewegungen Fenstersensor etc. und liegt bei mindestens 8 Monaten. In diesem Fall sollte ein Battery-Pack installiert werden, Wechsel auf CL erfolgen, ggf. auch Lösung mit Hochleistungsbatterie. Ggf. Organisation mit Akku direkt beim Kunden.

#### PN870.3
#### Batterielaufzeit kurz nach Installationsphase
Insbesondere wenn die Installationsphase länger läuft, kann die Belastung für einzelne Batterien durch besondere Umstände, die ggf. später nicht komplett nachvollziehbar sind, stark belastet werden. In diesem Fall darauf achten, dass nach Abschluss der Installation alle Batteriestände auf einem der Jahreszeit entsprechenden Niveau sind. Ggf. noch mal Komplettwechsel vor Beginn der neuen Heizsaison.

#### PN880
#### Ungültige Werte von Gerät geloggt

#### PN880.1
#### Gerät hat internen Fehler und sendet z.B. Fehlercode oder Sensor defekt
* Z.B. Temperaturmesswert -3276.8 bei Thermostat

#### PN880.2
#### Treiber defekt


### PN885
### Einige Datenreihen von einem Gerät brechen plötzlich ab

#### PN885.1
#### Problem mit Gerät (Kundenkonfiguration)

#### PN885.2
#### Problem mit Gerät (Hardware-Defekt)

#### PN885.3
#### Problem mit Gerät (Konfiguration Smartrplace)

#### PN885.4
#### Problem mit Treiber/Software

## Special Heating Issues

### PR101
### Nightly derating ends too late
* At Jakob&Sozien on 01.12.2021 nightly derating was changed by Heizungsbau Sadtkowski. We need to check what exactly was changed.<br>
Result analysis 02.12.2021: Nightly derating from 22 to 6 still available, but much more power during night, only slight derating. Should be possible to adapt.
* Jakob&Sozien has a Viessmann Vitogas 100 with a Vitocontrol 150
* Gas meter also in basement, no electricity socket nearby. Forgot to check for other electricity socket.
* Fotos s. [Mattermost](https://mattermost.smartrplace.de/smartrplace/pl/ksdxjf3y47f1fdi5i1fztn4pkw)


## Web Site Operation

### PW1
### Wordpress-Seite von Hackern missbraucht
* Notizen des Vorfalls ab dem 12.11.21: [Notes for Debugging Wordpress](Server-Administration/Server-and-Backup-overview#notes-for-debugging-and-management-of-wordpress)
* Angebote Wordpress-Hoster: [Raidboxes](https://raidboxes.io/tarife/): Insgesamt sehr [positive Bewertung](https://kopfundstift.de/raidboxes-erfahrungen/)
* Nach eher schlechten Erfahrungen mit dem 1&1-Support und etwas Recherche schlage ich vor, dass wir unsere Seite künftig bei RaidBoxes statt bei 1&1 hosten: https://raidboxes.io/ . Bewertungen im Netz sind sehr gut, die machen echt viele Wordpress-Seiten und deren Website sieht für mich gut aus, sitzen in Deutschland, explizit DSGVO-konform.

### PW2
### Website blockiert von Google, Firefox and Enterprise Firewalls nach Missbrauch der Website
* Big issue
* Erste Lösung: Auf smartrplace.de wechseln als Hauptdomain, keine Weiterleitung zu smartrplace.com mehr, bis sich das Thema wieder gelegt hat.
* Ca. 20 Stunden nach Lösung war die Blockade bei Firefox und Sophos/Fraunhofer wieder entfernt.


## Gelöst und zu testen

### PN210
### Keine Räume etc. angezeigt nach Einrichtung des Gateway
* [Zoho](https://projects.zoho.eu/portal/smartrplacedotde#taskdetail/157619000000098005/157619000000098021/157619000000095015)
* These Beobachtung: Das Problem tritt auf, wenn die Räume und ggf. User ganz frisch eingerichtet sind. Nach ca. 15 bis 30 Minuten verschwindet das Problem von alleine, ggf. auch schneller. Dadurch ist es schwer zu reproduzieren.
* Frage: Sieht das auch so aus, wenn noch keine Räume eingerichtet sind?

## Alt / Deprecated

### PN120
### Internetverbindung über Kundenanschluss verloren


## Links
* [Alarms for Supervision](/Operation/Alarms-for-Supervision)
* [This page: Incident Operation: Generelle Incidents, teilweise noch Entwicklung erforderlich](https://gitlab.smartrplace.de/i1/smartrplace/smartrplace-main/-/wikis/Operation/Incidents_Operation)
* [Feature Planning, Configuration, Maintenance](/Operation/Feature-Links)
* [Alarming and Analysis Codes](/Development/Alarming-and-Analysis-Codes)
* [Alarms for Spaceplus](/Operation/SpacePlus-Störmeldung)