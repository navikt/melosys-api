package no.nav.melosys.domain.begrunnelse;

import no.nav.melosys.domain.kodeverk.Kodeverk;

public enum VesentligVirksomhet implements Kodeverk {

    MINDRE_ENN_25_PROSENT("MINDRE_ENN_25_PROSENT", "Foretaket har hatt mindre enn 25% av samlet omsetning i Norge."),
    ADMINISTRATIVT_ANDEL_OVER_50_PROSENT("ADMINISTRATIVT_ANDEL_OVER_50_PROSENT", "Andelen administrativt ansatte i Norge er mer enn 50%."),
    ANSATTE_IKKE_REKRUTTERT_I_NORGE("ANSATTE_IKKE_REKRUTTERT_I_NORGE", "Ansatte blir ikke rekruttert i Norge."),
    MER_ENN_50_PROSENT_I_NORGE("MER_ENN_50_PROSENT_I_NORGE", "Utfører mindre enn 50% av oppdrag i Norge."),
    MER_ENN_50_PROSENT_OPPDRAGSKONTRAKT("MER_ENN_50_PROSENT_OPPDRAGSKONTRAKT", "Mindre enn 50 prosent oppdragskontrakter inngått i Norge."),
    NORSK_LOVGIVNING_ER_GJELDENDE("NORSK_LOVGIVNING_ER_GJELDENDE", "Norsk lovgivning er ikke gjeldende for kontraktene.");

    private String kode;
    private String beskrivelse;

    VesentligVirksomhet(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }
}
