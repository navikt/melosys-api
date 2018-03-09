package no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.kodeverk;

import no.nav.melosys.domain.dokument.KodeverkEnum;

/**
 * Denne enumen er en hardkoding av kodeverket OppgaveT:
 * https://kodeverkviewer.adeo.no/kodeverk/xml/oppgaveT.xml
 */
public enum OppgaveType implements KodeverkEnum<OppgaveType> {
    BEH_SAK_MK_MED("Behandle sak (Manuell)"),
    BEH_SED_MED("Behandle SED"),
    FDR_MED("Fordeling"),
    FLY_MED("Flyttesak"),
    GEN_MED("Generell"),
    GI_VEI_INFO_MED("Gi veiledning og informasjon"),
    INNH_DOK_MED("Innhent dokumentasjon"),
    JFR_MED("Journalføring"),
    KONT_BRUK_MED("Kontakt bruker"),
    KON_UTG_SCA_DOK_MED("Kontroller utgående skannet dokument"),
    MOTK_MED("Krav mottatt"),
    ORI_VED_BER_MED("Orienter om vedtak og beregning"),
    VURD_HENV_MED("Vurder henvendelse"),
    VUR_MED("Vurder dokument"),
    BEH_SAK_MED("Behandle sak"),
    SVAR_IK_MOT_MED("Svar ikke mottatt"),
    VUR_SVAR_MED("Vurder svar"),
    KONT_BRUK_UFM("Kontakt bruker"),
    BEH_SAK_MK_UFM("Behandle sak (Manuell)"),
    BEH_SED_UFM("Behandle SED"),
    FDR_UFM("Fordeling"),
    INNH_DOK_UFM("Innhent dokumentasjon"),
    JFR_UFM("Journalføring"),
    KON_UTG_SCA_DOK_UFM("Kontroller utgående skannet dokument"),
    VURD_HENV_UFM("Vurder henvendelse"),
    VUR_UFM("Vurder dokument"),
    VUR_SVAR_UFM("Vurder svar"),
    SVAR_IK_MOT_UFM("Svar ikke mottatt");

    private String navn;

    OppgaveType(String navn) {
        this.navn = navn;
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
