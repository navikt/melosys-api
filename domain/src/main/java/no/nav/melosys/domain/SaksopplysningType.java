package no.nav.melosys.domain;

import java.util.Set;

import no.nav.melosys.domain.kodeverk.Kodeverk;

public enum SaksopplysningType implements Kodeverk {

    ARBFORH("ARBFORH", "Arbeidsforhold"),
    INNTK("INNTK", "Inntekt"),
    MEDL("MEDL", "Medlemskap"),
    ORG("ORG", "Arbeidsgiver"),
    PDL_PERSOPL("PDL_PERSOPL", "Personopplysning fra PDL"),
    PDL_PERS_SAKS("PDL_PERS_SAKS", "Personopplysning fra PDL til saksbehandler"),
    PERSHIST("PERSHIST", "Personhistorikk fra TPS"),
    PERSOPL("PERSOPL", "Personopplysning fra TPS"),
    SEDOPPL("SEDOPPL", "SED-opplysninger"),
    UTBETAL("UTBETAL", "Utbetaldata");

    private final String kode;
    private final String beskrivelse;

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
        ARBFORH, INNTK, MEDL, PERSHIST, PERSOPL, UTBETAL
    );

    public static final Set<SaksopplysningType> KREVER_PERIODE = Set.of(
        ARBFORH, INNTK, MEDL, PERSHIST, UTBETAL
    );

    public static final Set<SaksopplysningType> TYPER_SOM_LAGRES_INITIELT = Set.of(ARBFORH, INNTK, MEDL, ORG, UTBETAL);
}

