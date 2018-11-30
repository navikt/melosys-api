package no.nav.melosys.integrasjon.medl;

import no.nav.melosys.domain.Kodeverk;

/**
 * Denne enumen er en hardkoding av kodeverket PeriodetypeMedl.
 */
public enum Periodetype implements Kodeverk {
    PMMEDSKP("PMMEDSKP", "Periode med medlemskap"),
    PUMEDSKP("PUMEDSKP", "Periode uten medlemskap"),
    E500INFO("E500INFO", "Utenlandsk id");

    private String kode;
    private String beskrivelse;

    Periodetype(String kode, String beskrivelse) {
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
