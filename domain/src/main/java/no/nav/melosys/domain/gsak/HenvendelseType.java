package no.nav.melosys.domain.gsak;

/**
 * Denne enumen er en hardkoding av kodeverket HenvendelseT:
 * https://kodeverkviewer.adeo.no/kodeverk/xml/henvendelseT.xml
 */
public enum HenvendelseType {
    BER_UT_MED("Beregning og utbetaling"),
    FORESP_MOTE_MED("Forespørsel møte"),
    FOR_SAK_MED("Forespørsel saksdokumenter"),
    GEN_VEILEDNING_MED("Generell veiledning"),
    INTERN_HENV_MED("Intern henvendelse"),
    OPPFOLGING_MED("Oppfølging"),
    SP_OM_SAKSGANG_MED("Spørsmål om saksgang"),
    SP_OM_VEDTAK_MED("Spørsmål om vedtak");

    private String navn;

    HenvendelseType(String navn) {
        this.navn = navn;
    }
}
