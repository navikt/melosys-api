package no.nav.melosys.domain.gsak;

/**
 * Denne enumen er en hardkoding av kodeverket Underkategori:
 * https://kodeverkviewer.adeo.no/kodeverk/xml/underkategori.xml
 */
public enum Underkategori {
    ANNET_MED("Annet"),
    ARB_AA_MED("Arbeidstakere annen avtale"),
    ARB_E101_MED("Forespørsel om trygdetid/E 104"),
    ARB_FOLK_MED("Arbeidstakere folketrygdloven"),
    ARB_MED("Arbeidstakere innen EØS"),
    AU_MED("Au-pair/praktikanter"),
    BOSTED_MED("Bosted"),
    MIDL_LOVVALG_MED("Midlertidig lovvalg"),
    MI_MED("Misjonærer"),
    PEN_MED("Pensjonister"),
    STUD_MED("Studenter"),
    UNNTAK_MED("Unntak"),
    BEL_UFM("Belgia"),
    BGR_UFM("Bulgaria"),
    DNK_UFM("Danmark"),
    CZE_UFM("Den tsjekkiske rep."),
    EST_UFM("Estland"),
    FIN_UFM("Finland"),
    FRA_UFM("Frankrike"),
    GRC_UFM("Hellas"),
    IRL_UFM("Irland"),
    ISL_UFM("Island"),
    ITA_UFM("Italia"),
    CYP_UFM("Kypros (Republikken Kypros)"),
    LVA_UFM("Latvia"),
    LIE_UFM("Liechtenstein"),
    LTU_UFM("Litauen"),
    LUX_UFM("Luxembourg"),
    MLT_UFM("Malta"),
    NLD_UFM("Nederland"),
    NOR_UFM("Norge"),
    POL_UFM("Polen"),
    PRT_UFM("Portugal"),
    ROU_UFM("Romania"),
    SVK_UFM("Slovakia"),
    SVN_UFM("Slovenia"),
    ESP_UFM("Spania"),
    GBR_NIRL_UFM("Storbritannia og Nord Irland"),
    CHE_UFM("Sveits"),
    SWE_UFM("Sverige"),
    DEU_UFM("Tyskland"),
    HUN_UFM("Ungarn"),
    AUT_UFM("Østerrike"),
    HRV_UFM("Kroatia"),
    FOLK_TRY_UFM("Folketrygdloven"),
    USA_UFM("USA"),
    CAN_UFM("Canada"),
    AUS_UFM("Australia"),
    OVR_UFM("Øvrige trygdeavtaler"),
    FAE_UFM("Færøyene"),;

    private String navn;

    Underkategori(String navn) {
        this.navn = navn;
    }
}
