package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class DokumentbestillingMetadata {

    // DokumenttypeID identifiserer hvilket dokument som skal produseres.
    public String dokumenttypeID;

    // Fødselsnummer/tss id/ org.nr. til personen/organisasjonen som er sakspart.
    public String bruker;

    // Fødselsnummer/tss id/ org.nr. til personen/ organisasjonen som er mottaker av dokumentet.
    public String mottakerID;

    public Aktoersroller mottakersRolle;

    // SakID som dokument skal journalføres mot  (forskjellig fra fagsaksnummer)
    public String journalsakID;

    // Fagområdet som dokumentet tilhører.
    public String fagområde;

    public String saksbehandler;

    public UtenlandskMyndighet utenlandskMyndighet;

}
