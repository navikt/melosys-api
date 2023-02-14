package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.brev.Mottaker;

public class DokumentbestillingMetadata {

    // DokumenttypeID identifiserer hvilket dokument som skal produseres.
    public String dokumenttypeID;

    // Fødselsnummer/tss id/ org.nr. til personen/organisasjonen som er sakspart.
    public String brukerID;

    // Brukernavn må settes hvis dokprod ikke utleder registerInfo
    public String brukerNavn;

    public Mottaker mottaker;

    // Fødselsnummer/tss id/ org.nr. til personen/ organisasjonen som er mottaker av dokumentet.
    public String mottakerID;

    // SakID som dokument skal journalføres mot  (forskjellig fra fagsaksnummer)
    public String journalsakID;

    // Fagområdet som dokumentet tilhører.
    public String fagområde;

    public String saksbehandler;

    public UtenlandskMyndighet utenlandskMyndighet;

    public StrukturertAdresse postadresse;

    // Avgjør om postadresse og mottakers navn utledes av tjenesten
    public boolean berik;
}
