package no.nav.melosys.domain.begrunnelse;

import no.nav.melosys.domain.Kodeverk;

public enum Artikkel12_2 implements Kodeverk {
    NORMALT_IKKE_DRIFT_NORGE("NORMALT_IKKE_DRIFT_NORGE", "Normalt ikke drift i Norge"),
    IKKE_LIGNENDE_VIRKSOMHET("IKKE_LIGNENDE_VIRKSOMHET", "Ikke lignende virksomhet"),
    UTSENDELSE_OVER_24_MN("UTSENDELSE_OVER_24_MN", "Utsendelseperioden overskrider 24 måneder");

    private String kode;
    private String beskrivelse;

    Artikkel12_2(String kode, String beskrivelse) {
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