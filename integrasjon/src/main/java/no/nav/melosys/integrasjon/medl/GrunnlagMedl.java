package no.nav.melosys.integrasjon.medl;

public enum GrunnlagMedl {
    FO_11_2("FO_11_2"),
    FO_11_3_A("FO_11_3_a"),
    FO_11_3_B("FO_11_3_b"),
    FO_11_3_C("FO_11_3_c"),
    FO_11_3_D("FO_11_3_d"),
    FO_11_3_E("FO_11_3_e"),
    FO_11_4("FO_11_4"),
    FO_11_4_1("FO_11_4_1"),
    FO_11_4_2("FO_11_4_2"),
    FO_11_5("FO_11_5"),
    FO_12_1("FO_12_1"),
    FO_12_2("FO_12_2"),
    FO_13_1_A("FO_13_1_a"),
    FO_13_1_B("FO_13_1_b"),
    FO_13_B_II("FO_13_b_ii"),
    FO_13_B_III("FO_13_b_iii"),
    FO_13_B_IV("FO_13_b_iv"),
    FO_13_2_A("FO_13_2_a"),
    FO_13_2_B("FO_13_2_b"),
    FO_13_3("FO_13_3"),
    FO_13_4("FO_13_4"),
    FO_15("FO_15"),
    FO_16("FO_16"),
    FO_987_2009_14_11("FO_987_2009_14_11"),
    MEDFT("MEDFT"),
    IMEDEOS("IMEDEOS"),
    FTL_2_8("FTL_2-8"),
    Storbrit_NIrland_6_1("Storbrit_NIrland_6_1"),
    Storbrit_NIrland_6_5("Storbrit_NIrland_6_5"),
    Storbrit_NIrland_7_1("Storbrit_NIrland_7_1"),
    Storbrit_NIrland_7_3("Storbrit_NIrland_7_3"),
    Storbrit_NIrland_8_2("Storbrit_NIrland_8_2"),
    Storbrit_NIrland_9("Storbrit_NIrland_9");

    private final String kode;

    GrunnlagMedl(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
