package no.nav.melosys.integrasjon.medl;

public enum DekningMedl {
    UNNTATT("Unntatt"),
    FULL("Full"),
    FTRL_2_9_1_LEDD_A("FTL_2-9_1_ledd_a"),
    FTRL_2_9_2_LEDD_1A("FTL_2-9_2_ld_jfr_1a"),
    FTRL_2_9_1_LEDD_B("FTL_2-9_1_ledd_b"),
    FTRL_2_9_1_LEDD_C("FTL_2-9_1_ledd_c"),
    FTRL_2_9_2_LEDD_1C("FTL_2-9_2_ld_jfr_1c"),
    IKKEPENDEL("Ikke pensjonsdel");


    private final String kode;

    DekningMedl(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

}
