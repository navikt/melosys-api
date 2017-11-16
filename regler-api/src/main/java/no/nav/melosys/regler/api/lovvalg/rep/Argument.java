package no.nav.melosys.regler.api.lovvalg.rep;

public enum Argument {
    
    // KRITERIER FOR SØKNAD OM A1
    
    // Kriterier som velger artikkel 
    BRUKER_ER_ARBEIDSTAKER("Bruker er arbeidstaker"),
    BRUKER_ER_NÆRINGSDRIVENDE("Bruker er næringsdrivende"),
    BRUKER_ER_TJENESTEMANN("Bruker er tjenestemann"),
    BRUKER_ARBEIDER_I_FLY("Bruker arbeider i fly"),
    BRUKER_ARBEIDER_PÅ_SKIP("Bruker arbeider på skip"),
    BRUKER_ARBEIDER_I_FLERE_LAND("Bruker arbeider i flere land"),
    ANDEL_PROSENT_ARB_I_BOLAND("Andel % arb/inntekt i bostedsland"),

    A1_BRUKER_ER_ANSATT_HOS_UTSENDENDE_ORGANISASJON_I_HELE_SØKNADSPERIODE("Bruker er ansatt hos utsendende organisasjon i hele perioden"),
    
    A1_SKAL_VURDERE_ART_11_1("Skal vurdere artikkel 11.1"),
    A1_SKAL_VURDERE_ART_11_2("Skal vurdere artikkel 11.2"),
    A1_SKAL_VURDERE_ART_11_3A("Skal vurdere artikkel 11.3a"),
    A1_SKAL_VURDERE_ART_11_3B("Skal vurdere artikkel 11.3b"),
    A1_SKAL_VURDERE_ART_11_3C("Skal vurdere artikkel 11.3c"),
    A1_SKAL_VURDERE_ART_11_3D("Skal vurdere artikkel 11.3d"),
    A1_SKAL_VURDERE_ART_11_3E("Skal vurdere artikkel 11.3e"),
    A1_SKAL_VURDERE_ART_12_1("Skal vurdere artikkel 12.1"),
    A1_SKAL_VURDERE_ART_12_2("Skal vurdere artikkel 12.2"),
    A1_SKAL_VURDERE_ART_13_1A("Skal vurdere artikkel 13.1a"),
    A1_SKAL_VURDERE_ART_13_1B1("Skal vurdere artikkel 13.1b1"),
    A1_SKAL_VURDERE_ART_13_1B2("Skal vurdere artikkel 13.1b2"),
    A1_SKAL_VURDERE_ART_13_1B3("Skal vurdere artikkel 13.1b3"),
    A1_SKAL_VURDERE_ART_13_1B4("Skal vurdere artikkel 13.1b4"),
    A1_SKAL_VURDERE_ART_13_2A("Skal vurdere artikkel 13.2a"),
    A1_SKAL_VURDERE_ART_13_2B("Skal vurdere artikkel 13.2b"),
    A1_SKAL_VURDERE_ART_16_1("Skal vurdere artikkel 16.1"),
    A1_SKAL_VURDERE_ART_16_2("Skal vurdere artikkel 16.2"),

    // Overgangsregler
    A1_FORORDNING_1408_71_SKAL_ANVENDES("forordning 1408/71 skal anvendes"),

    // Kriterier for 12.1
    A1_12_1_VIRKSOMHET_I_UTSENDERLAND("Arbeidsgiver har virksomhet i landet arbeidstakeren sendes fra"),
    A1_12_1_SENDES_TIL_ANNEN_MEDLEMSSTAT("Arbeidstakeren sendes til en annen medlemsstat for å utføre arbeid for arbeidsgiveren"),
    A1_12_1_LENGDE_MND_UTENLANDSOPPHOLD("Antall måneder utenlandsoppholded varer"),
    A1_12_1_SKAL_ERSTATTE_ANNEN_PERSON("Arbeidstakeren er utsendt for å erstatte en annen person"),
    
    // ARGUMENTER SOM ER INTERNE FOR REGELMOTOREN
    REGELKJØRINGEN_SKAL_AVBRYTES("Regelkjøringen skal avbrytes"),
    

    
    FIXME("FIXME"); // FIXME: Fjern
    
    public final String beskrivelse;
    
    private Argument(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

}
