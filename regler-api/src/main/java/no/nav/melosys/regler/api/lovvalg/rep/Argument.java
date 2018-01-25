package no.nav.melosys.regler.api.lovvalg.rep;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Argument {
    
    // KRITERIER FOR SØKNAD OM A1
    SØKNADEN_KVALIFISERER_FOR_EF_883_2004("Søknaden kvalifiserer for forordning (EF) 883/2004"),
    
    // Kriterier som velger artikkel 
    BRUKER_ER_ARBEIDSTAKER("Bruker er arbeidstaker"),
    BRUKER_ER_NÆRINGSDRIVENDE("Bruker er næringsdrivende"),
    // BRUKER_ER_TJENESTEMANN("Bruker er tjenestemann"),
    // BRUKER_ARBEIDER_I_FLY("Bruker arbeider i fly"),
    // BRUKER_ARBEIDER_PÅ_SKIP("Bruker arbeider på skip"),
    ANTALL_UTLAND_BRUKER_ARBEIDER_I("Antall land bruker arbeider i"),
    BRUKER_ARBEIDER_I_NORGE("Bruker arbeider i Norge"),
    ARBEIDSGIVER_HAR_VESENTLIG_VIRKSOMHET_I_NORGE("arbeidsgiver har vesentlig virksomhet i Norge"),
    
    SKAL_VURDERE_ART_11_2("Skal vurdere artikkel 11.2"),
    SKAL_VURDERE_ART_11_3A("Skal vurdere artikkel 11.3a"),
    SKAL_VURDERE_ART_11_3B("Skal vurdere artikkel 11.3b"),
    SKAL_VURDERE_ART_11_3C("Skal vurdere artikkel 11.3c"),
    SKAL_VURDERE_ART_11_3D("Skal vurdere artikkel 11.3d"),
    SKAL_VURDERE_ART_11_3E("Skal vurdere artikkel 11.3e"),
    SKAL_VURDERE_ART_11_4_1("Skal vurdere artikkel 11.4 første punktum"),
    SKAL_VURDERE_ART_11_4_2("Skal vurdere artikkel 11.4 annet punktum"),
    SKAL_VURDERE_ART_11_5("Skal vurdere artikkel 11.5"),
    SKAL_VURDERE_ART_12_1("Skal vurdere artikkel 12.1"),
    SKAL_VURDERE_ART_12_2("Skal vurdere artikkel 12.2"),
    SKAL_VURDERE_ART_13_1A("Skal vurdere artikkel 13.1a"),
    SKAL_VURDERE_ART_13_1B1("Skal vurdere artikkel 13.1b1"),
    SKAL_VURDERE_ART_13_1B2("Skal vurdere artikkel 13.1b2"),
    SKAL_VURDERE_ART_13_1B3("Skal vurdere artikkel 13.1b3"),
    SKAL_VURDERE_ART_13_1B4("Skal vurdere artikkel 13.1b4"),
    SKAL_VURDERE_ART_13_2A("Skal vurdere artikkel 13.2a"),
    SKAL_VURDERE_ART_13_2B("Skal vurdere artikkel 13.2b"),
    SKAL_VURDERE_ART_13_3("Skal vurdere artikkel 13.3"),
    SKAL_VURDERE_ART_13_4("Skal vurdere artikkel 13.4"),
    SKAL_VURDERE_ART_16_M("Skal vurdere artikkel 16 medlem"),
    SKAL_VURDERE_ART_16_U("Skal vurdere artikkel 16 unntak"),

    // Overgangsregler
    // FORORDNING_1408_71_SKAL_ANVENDES("forordning 1408/71 skal anvendes"),

    // Kriterier for de forskjellige artiklene
    ARBEIDSPLASSEN_I_UTLANDET_DEKKES_AV_EF_883_2004("Arbeidsplassen i utlandet er på et sted som dekkes av EF 883/2004"), // 12.1
    BRUKER_HAR_NORSK_ARBEIDSGIVER("Bruker har norsk arbeidsgiver"), // 12.1
    ANTALL_ARBEIDSGIVERE_I_SØKNADSPERIODEN("Antall arbeidsgivere i søknadsperioden"), // 12.1
    HOVEDARBEIDSFORHOLDET_VARER_I_HELE_SØKNADSPERIODEN("Hovedarbeidsforholdet varer i hele søknadsperioden"), // 12.1
    LENGDE_MND_UTENLANDSOPPHOLD("Antall måneder utenlandsoppholdet varer"), // 12.1
    BRUKEREN_SKAL_ERSTATTE_EN_ANNEN_ARBEIDSTAKER("Brukeren skal ikke erstatte en annen utsendt arbeidstaker"), // 12.1 // FIXME: Må settes
    BRUKER_ER_MEDLEM_AV_FTRL_MÅNEDEN_FØR_PERIODESTART("Bruker er medlem av ftrl måneden før periodestart"); // 12.1
    
    // FIXME BRUKER_ER_ANSATT_HOS_UTSENDENDE_ORGANISASJON_I_HELE_SØKNADSPERIODE("Bruker er ansatt hos utsendende organisasjon i hele perioden"),

    @JsonValue
    public final String beskrivelse;
    
    private Argument(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

}
