package no.nav.melosys.domain.person;

public enum Informasjonsbehov {
    INGEN("Uten tilleggsopplysninger"),
    STANDARD("Med adresseopplysninger"),
    MED_FAMILIERELASJONER("Med adresseopplysninger og familierelasjoner");

    private String beskrivelse;

    Informasjonsbehov(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}
