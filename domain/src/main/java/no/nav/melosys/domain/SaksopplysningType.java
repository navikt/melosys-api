package no.nav.melosys.domain;

import java.util.Set;

import no.nav.melosys.domain.kodeverk.Kodeverk;

public enum SaksopplysningType implements Kodeverk {

    ARBFORH("ARBFORH", "Arbeidsforhold"),
    INNTK("INNTK", "Inntekt"),
    MEDL("MEDL", "Medlemskap"),
    ORG("ORG", "Arbeidsgiver"),
    PERSHIST("PERSHIST", "Personhistorikk"),
    PERSOPL("PERSOPL", "Personopplysning"),
    SEDOPPL("SEDOPPL", "SED-opplysninger"),
    SOB_SAK("SOB_SAK", "Sak og behandling-sak"),
    UTBETAL("UTBETAL", "Utbetaldata");

    private String kode;
    private String beskrivelse;

    SaksopplysningType(String kode, String beskrivelse) {
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

    public static final Set<SaksopplysningType> KREVER_FNR = Set.of(
        ARBFORH, INNTK, MEDL, PERSHIST, PERSOPL, SOB_SAK, UTBETAL
    );

    public static final Set<SaksopplysningType> KREVER_PERIODE = Set.of(
        ARBFORH, INNTK, MEDL, PERSHIST, UTBETAL
    );
}

