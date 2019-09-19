package no.nav.melosys.metrics;

public final class MetrikkerNavn {
    private MetrikkerNavn() {
        throw new IllegalStateException("Utility");
    }

    private static final String METRIKKER_NAMESPACE = "melosys.";

    public static final String SAKER_OPPRETTET = METRIKKER_NAMESPACE + "saker.opprettet";
    public static final String BEHANDLINGER_OPPRETTET = METRIKKER_NAMESPACE + "behandlinger.opprettet";
    public static final String BEHANDLINGER_AVSLUTTET = METRIKKER_NAMESPACE + "behandlinger.avsluttet";
    public static final String PROSESSINSTANSER_OPPRETTET = METRIKKER_NAMESPACE + "prosessinstanser.opprettet";
    public static final String PROSESSINSTANSER = METRIKKER_NAMESPACE + "prosessinstanser.";
}
