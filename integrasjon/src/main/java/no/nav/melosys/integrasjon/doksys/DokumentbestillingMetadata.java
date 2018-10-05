package no.nav.melosys.integrasjon.doksys;

public class DokumentbestillingMetadata {

    // DokumenttypeID identifiserer hvilket dokument som skal produseres.
    public String dokumenttypeID;

    // Parameter som settes for å angi om registerInfo skal utledes for dokumentet som bestilles. Default false.
    public boolean utledRegisterInfo;

    // Fødselsnummer/tss id/ org.nr. til personen/organisasjonen som er sakspart.
    public String bruker;

    // Fødselsnummer/tss id/ org.nr. til personen/ organisasjonen som er mottaker av dokumentet.
    public String mottaker;

    // SakID som dokument skal journalføres mot  (forskjellig fra fagsaksnummer)
    public String journalsakID;

    // Fagområdet som dokumentet tilhører.
    public String fagområde;

    public String saksbehandler;

}
