package no.nav.melosys.domain.bestemmelse;

public enum LovvalgBestemmelse_883_2004 implements LovvalgBestemmelse {

    FO_883_2004_ART11_1("Kun omfattet i en medlemsstat (art 11.1)"),
    FO_883_2004_ART11_3A("Omfattet i arbeidsland (art 11.3a)"),
    FO_883_2004_ART11_3B("Omfattet av lovgivningen i det landet der man er vurdert som tjenestemann (art 11.3.b)"),
    FO_883_2004_ART11_3C("Omfattet i bostedsstaten dersom de utbetaler dagpenger etter art. 65 (art 11.3.c)"),
    FO_883_2004_ART11_3D("Omfattet i medlemsstaten der man er innkalt til militær-/siviltjenste (art 11.3.d)"),
    FO_883_2004_ART11_3E("Ikke-yrkesaktive er omfattet i bostedsstaten (art 11.3.e)"),
    FO_883_2004_ART11_4_2("Arbeid på skip, lønnet av arbeidsgiveer i bostedsstaten (art 11.4.2)"),
    FO_883_2004_ART12_1("Utsendt arbeidstaker (art 12.1)"),
    FO_883_2004_ART12_2("Utsendt selvstendig næringsrivende (art 12.2)"),
    FO_883_2004_ART13_1A("Arbeidstaker i to/flere land - omfattet i bostedsland (art. 13.1.a)"),
    FO_883_2004_ART13_1B1("Arbeidstaker i to/flere land - omfattet der arbeidsgiver har forretningssted (art 13.1.b.i)"),
    FO_883_2004_ART13_1B2("Arbeidstaker i to/flere land (art 13.1.b.ii)"),
    FO_883_2004_ART13_1B3("Arbeidstaker i to/flere land (art 13.1.b.iii)"),
    FO_883_2004_ART13_1B4("Arbeidstaker i to/flere land (art 13.1.b.iv)"),
    FO_883_2004_ART13_2A("Selvstendig virksomhet i to/flere land - omfattet i bostedsland (art 13.2.a)"),
    FO_883_2004_ART13_2B("Selvstendig virksomhet i to/flere land - omfattet der virksomheten har hovedtyngden av virksomheten (art 13.2.b)"),
    FO_883_2004_ART13_3("Arbeidstaker og selvstendig virksomhet i to/flere land (art 13.3)"),
    FO_883_2004_ART13_4("Tjenestemann og arbeidstaker/ selvstendig virksomhet i to/flere land (art 13.4)"),
    FO_883_2004_ART16_1("Avtale om unntak fra artikkel 11 til 15 (art 16.1)"),
    FO_883_2004_ART16_2("Anmodning om unntak for pensjonist (art 16.2)"),
    FO_883_2004_ANNET("Annet");

    private String beskrivelse;

    LovvalgBestemmelse_883_2004(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() {
        return name();
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }
}