package no.nav.melosys.metrics;

public final class MetrikkerNavn {
    private MetrikkerNavn() {
        throw new IllegalStateException("Utility");
    }

    private static final String METRIKKER_NAMESPACE = "melosys.";

    public static final String SAKER_OPPRETTET = METRIKKER_NAMESPACE + "saker.opprettet";
    public static final String BEHANDLINGSTEMAER_OPPRETTET = METRIKKER_NAMESPACE + "behandlingstemaer.opprettet";
    public static final String BEHANDLINGSTYPER_OPPRETTET = METRIKKER_NAMESPACE + "behandlingstyper.opprettet";
    public static final String BEHANDLINGER_AVSLUTTET = METRIKKER_NAMESPACE + "behandlinger.avsluttet";
    public static final String PROSESSINSTANSER_OPPRETTET = METRIKKER_NAMESPACE + "prosessinstanser.opprettet";
    public static final String PROSESSINSTANSER = METRIKKER_NAMESPACE + "prosessinstanser.";
    public static final String PROSESSINSTANSER_STEG = METRIKKER_NAMESPACE + "prosessinstanser.steg.";
    public static final String UNNTAKSPERIODE_KONTROLL_TREFF = METRIKKER_NAMESPACE + "unntakperiode.treffbegrunnelse";
    public static final String SVAR_AOU = METRIKKER_NAMESPACE + "svar.aou";
    public static final String EVENTS_FEILET = METRIKKER_NAMESPACE + "events.feilet";

    public static final String TAG_TEMA = "tema";
    public static final String TAG_TYPE = "type";
    public static final String TAG_BEGRUNNELSE = "begrunnelse";
    public static final String TAG_RESULTAT = "resultat";
}
