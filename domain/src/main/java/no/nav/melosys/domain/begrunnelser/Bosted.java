package no.nav.melosys.domain.begrunnelser;

import no.nav.melosys.domain.Kodeverk;

public enum Bosted implements Kodeverk {

    OPPHOLD_MER_ENN_12_MND("OPPHOLD_MER_ENN_12_MND", "Oppholdet er mer enn 12 måneder."),
    HAR_IKKE_FORUTGAENDE_BOSTED_I_NORGE("HAR_IKKE_FORUTGAENDE_BOSTED_I_NORGE", "Har ikke forutgående bosted i Norge."),
    IKKE_INTENSJON_OM_RETUR("IKKE_INTENSJON_OM_RETUR", "Har ikke intensjon om retur til Norge."),
    FAMILIE_BOR_IKKE_I_NORGE("FAMILIE_BOR_IKKE_I_NORGE", "Familie bor ikke i Norge."),
    STUDIER_FINANSIERES_IKKE_FRA_NORGE("STUDIER_FINANSIERES_IKKE_FRA_NORGE", "Studier finansieres ikke fra Norge."),
    HAR_IKKE_STUDIESTED_I_UTLANDET("HAR_IKKE_STUDIESTED_I_UTLANDET", "Har ikke studiested i utlandet.");

    private String kode;
    private String beskrivelse;

    Bosted(String kode, String beskrivelse) {
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
