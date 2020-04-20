package no.nav.melosys.integrasjonstest.felles.opplysninger;

public final class Testbehandlinger {
    private Testbehandlinger() {
        throw new IllegalStateException("Utility class")
    }

    public static final long TOM_BEHANDLING_MARITIMT_ARBEID_OG_OPPGITT_ADRESSE = 1L;
    public static final long TOM_BEHANDLING = 2L;
    public static final long UTFYLT_BEHANDLING_ART12 = 3L;
    public static final long UTFYLT_BEHANDLING_ART16_UTEN_ART12 = 4L;
    public static final long UTFYLT_ANMODNING_SVAR = 5L;
    public static final long TOM_BEHANDLING_REPRESENTANT = 6L;
}