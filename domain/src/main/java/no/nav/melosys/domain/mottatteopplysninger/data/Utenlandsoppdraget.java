package no.nav.melosys.domain.mottatteopplysninger.data;

public class Utenlandsoppdraget {
    public Periode samletUtsendingsperiode = new Periode();
    public Boolean erUtsendelseForOppdragIUtlandet;
    public Boolean erFortsattAnsattEtterOppdraget;
    public Boolean erAnsattForOppdragIUtlandet;
    public Boolean erDrattPaaEgetInitiativ;
    public Boolean erErstatningTidligereUtsendte;

    public Utenlandsoppdraget() {
    }

    public Utenlandsoppdraget(
        Periode samletUtsendingsperiode,
        Boolean erUtsendelseForOppdragIUtlandet,
        Boolean erFortsattAnsattEtterOppdraget,
        Boolean erAnsattForOppdragIUtlandet,
        Boolean erDrattPaaEgetInitiativ,
        Boolean erErstatningTidligereUtsendte
    ) {
        this.samletUtsendingsperiode = samletUtsendingsperiode;
        this.erUtsendelseForOppdragIUtlandet = erUtsendelseForOppdragIUtlandet;
        this.erFortsattAnsattEtterOppdraget = erFortsattAnsattEtterOppdraget;
        this.erAnsattForOppdragIUtlandet = erAnsattForOppdragIUtlandet;
        this.erDrattPaaEgetInitiativ = erDrattPaaEgetInitiativ;
        this.erErstatningTidligereUtsendte = erErstatningTidligereUtsendte;
    }
}
