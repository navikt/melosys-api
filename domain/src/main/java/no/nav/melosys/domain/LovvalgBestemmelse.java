package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum LovvalgBestemmelse implements KodeverkTabell<LovvalgBestemmelse> {

    ART_11_1("ART_11_1", "Kun omfattet i en medlemsstat (art 11.1)"),
    ART_11_2("ART_11_2", "Omfattet i medlemsstaten som utbetaler kontantytelsen (art 11.2)"),
    ART_11_3A("ART_11_3A", "Omfattet i arbeidsland (art 11.3a)"),
    ART_11_3B("ART_11_3B", "Omfattet av lovgivningen i det landet der man er vurdert som tjenestemann (art 11.3.b)"),
    ART_11_3C("ART_11_3C", "Omfattet i bostedsstaten dersom de utbetaler dagpenger etter art. 65 (art 11.3.c)"),
    ART_11_3D("ART_11_3D", "Omfattet i medlemsstaten der man er innkalt til militær-/siviltjenste (art 11.3.d)"),
    ART_11_3E("ART_11_3E", "Ikke-yrkesaktive er omfattet i bostedsstaen(art 11.3.e)"),
    ART_12_1("ART_12_1", "Utsendt arbeidstaker (art 12.1)"),
    ART_12_2("ART_12_2", "Utsendt selvstendig næringsrivende (art 12.2)"),
    ART_13_1A("ART_13_1A", "Arbeidstaker i to/flere land - omfattet i bostedsland (art. 13.1.a)"),
    ART_13_1B1("ART_13_1B1", "Arbeidstaker i to/flere land - omfattet der arbeidsgiver har forretningssted (art 13.1.b.i)"),
    ART_13_1B2("ART_13_1B2", "Arbeidstaker i to/flere land (art 13.1.b.ii)"),
    ART_13_1B3("ART_13_1B3", "Arbeidstaker i to/flere land (art 13.1.b.iii)"),
    ART_13_1B4("ART_13_1B4", "Arbeidstaker i to/flere land (art 13.1.b.iv)"),
    ART_13_2A("ART_13_2A", "Selvstendig virksomhet i to/flere land - omfattet i bostedsland (art 13.2.a)"),
    ART_13_2B("ART_13_2B", "Selvstendig virksomhet i to/flere land - omfattet der virksomheten har hovedtyngden av virksomheten (art 13.2.b)"),
    ART_16_1("ART_16_1", "Avtale om unntak fra artikkel 11 til 15 (art 16.1)"),
    ART_16_2("ART_16_2", "Anmodning om unntak for pensjonist (art 16.2)");
 
    private String kode;
    private String beskrivelse;

    private LovvalgBestemmelse(String kode, String beskrivelse) {
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

    @Converter
    public static class DbKonverterer extends KodeverkTabell.DbKonverterer<LovvalgBestemmelse> {
        @Override
        protected LovvalgBestemmelse[] getLovligeVerdier() {
            return LovvalgBestemmelse.values();
        }
    }
    
}
