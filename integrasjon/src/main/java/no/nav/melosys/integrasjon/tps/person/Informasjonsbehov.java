package no.nav.melosys.integrasjon.tps.person;

public enum Informasjonsbehov {
    ADRESSE(no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov.ADRESSE),
    FAMILIERELASJONER(no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov.FAMILIERELASJONER);

    private final no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov kode;

    Informasjonsbehov(no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov kode) {
        this.kode = kode;
    }

    public no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov getKode() {
        return kode;
    }
}
