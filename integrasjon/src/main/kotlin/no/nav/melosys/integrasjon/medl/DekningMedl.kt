package no.nav.melosys.integrasjon.medl

enum class DekningMedl(val kode: String) {
    UNNTATT("Unntatt"),
    FULL("Full"),
    FTRL_2_7_3_LEDD_B("FTL_2-7_3_ledd_b"),
    FTRL_2_7A_2_LEDD_B("FTL_2-7a_2_ledd_b"),
    FTRL_2_9_1_LEDD_A("FTL_2-9_1_ledd_a"),
    FTRL_2_9_2_LEDD_1A("FTL_2-9_2_ld_jfr_1a"),
    FTRL_2_9_1_LEDD_B("FTL_2-9_1_ledd_b"),
    FTRL_2_9_1_LEDD_C("FTL_2-9_1_ledd_c"),
    FTRL_2_9_2_LEDD_1C("FTL_2-9_2_ld_jfr_1c"),
    FTRL_2_9_2_LEDD_3_LEDD_1C("FTL_2-9_2_ld_3_ld_jfr_1c"),
    FTRL_2_9_3_LEDD_1B("FTL_2-9_3_ld_jfr_1b"),
    FTRL_2_9_3_LEDD_1C("FTL_2-9_3_ld_jfr_1c"),
    IKKE_PENSJONSDEL("IKKEPENDEL"),
    TILLEGSAVTALE_NATO_DEKNING("Helsetjenester_sykepenger_sykdom_i_familie_svangerskap_fødsel_adopsjon")

}
