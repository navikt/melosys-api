package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.Kodeverk;

/**
 * https://confluence.adeo.no/display/APPKAT/Applikasjons-ID
 */
public enum Fagsystem implements Kodeverk {
    // GSAK brukes som referanse til sakstilhørende fagsystem, men av historiske årsaker har det feil kode i Joark.
    GSAK_I_JOARK("FS22", "GSAK i Joark"), // Bruker Gosys appID
    INTET("", ""),
    MELOSYS("FS38", "Melosys");

    private String kode;
    private String beskrivelse;

    Fagsystem(String kode, String beskrivelse) {
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
