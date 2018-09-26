package no.nav.melosys.domain.bestemmelse;

public enum LovvalgBestemmelse_883_2004 implements LovvalgBestemmelse {

    ART11_1("ART11_1", "Kun omfattet i en medlemsstat (art 11.1)"),
    ART11_2("ART11_2", "Omfattet i medlemsstaten som utbetaler kontantytelsen (art 11.2)"),
    ART11_3A("ART11_3A", "Omfattet i arbeidsland (art 11.3a)"),
    ART11_3B("ART11_3B", "Omfattet av lovgivningen i det landet der man er vurdert som tjenestemann (art 11.3.b)"),
    ART11_3C("ART11_3C", "Omfattet i bostedsstaten dersom de utbetaler dagpenger etter art. 65 (art 11.3.c)"),
    ART11_3D("ART11_3D", "Omfattet i medlemsstaten der man er innkalt til militær-/siviltjenste (art 11.3.d)"),
    ART11_3E("ART11_3E", "Ikke-yrkesaktive er omfattet i bostedsstaten (art 11.3.e)"),
    ART11_4_2("ART11_4_2", "Arbeid på skip, lønnet av arbeidsgiveer i bostedsstaten (art 11.4.2)"),
    ART12_1("ART12_1", "Utsendt arbeidstaker (art 12.1)"),
    ART12_2("ART12_2", "Utsendt selvstendig næringsrivende (art 12.2)"),
    ART13_1A("ART13_1A", "Arbeidstaker i to/flere land - omfattet i bostedsland (art. 13.1.a)"),
    ART13_1B1("ART13_1B1", "Arbeidstaker i to/flere land - omfattet der arbeidsgiver har forretningssted (art 13.1.b.i)"),
    ART13_1B2("ART13_1B2", "Arbeidstaker i to/flere land (art 13.1.b.ii)"),
    ART13_1B3("ART13_1B3", "Arbeidstaker i to/flere land (art 13.1.b.iii)"),
    ART13_1B4("ART13_1B4", "Arbeidstaker i to/flere land (art 13.1.b.iv)"),
    ART13_2A("ART13_2A", "Selvstendig virksomhet i to/flere land - omfattet i bostedsland (art 13.2.a)"),
    ART13_2B("ART13_2B", "Selvstendig virksomhet i to/flere land - omfattet der virksomheten har hovedtyngden av virksomheten (art 13.2.b)"),
    ART13_3("ART13_3", "Arbeidstaker og selvstendig virksomhet i to/flere land (art 13.3)"),
    ART13_4("ART13_4", "Tjenestemann og arbeidstaker/ selvstendig virksomhet i to/flere land (art 13.4)"),
    ART16_1("ART16_1", "Avtale om unntak fra artikkel 11 til 15 (art 16.1)"),
    ART16_2("ART16_2", "Anmodning om unntak for pensjonist (art 16.2)");

    private String kode;
    private String beskrivelse;

    LovvalgBestemmelse_883_2004(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }
}