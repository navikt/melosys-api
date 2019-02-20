package no.nav.melosys.integrasjon.eessi.dto;

import java.util.List;


public class SedDataDto {
    //Søknaddok.
    private List<Ident> utenlandskIdent;

    //Persondok.
    private List<FamilieMedlem> familieMedlem;
    private boolean egenAnsatt;
    private Bruker bruker;

    //Andre medlemsvariabler
    private Adresse bostedsadresse;
    private List<Virksomhet> arbeidsgivendeVirksomheter;
    private List<Virksomhet> selvstendigeVirksomheter;
    private List<Arbeidssted> arbeidssteder;
    private List<Virksomhet> utenlandskeVirksomheter;

    private Long gsakSaksnummer;

    //Lovvalg
    private List<Lovvalgsperiode> lovvalgsperioder;

    public List<Ident> getUtenlandskIdent() {
        return utenlandskIdent;
    }

    public void setUtenlandskIdent(List<Ident> utenlandskIdent) {
        this.utenlandskIdent = utenlandskIdent;
    }

    public List<FamilieMedlem> getFamilieMedlem() {
        return familieMedlem;
    }

    public void setFamilieMedlem(List<FamilieMedlem> familieMedlem) {
        this.familieMedlem = familieMedlem;
    }

    public boolean isEgenAnsatt() {
        return egenAnsatt;
    }

    public void setEgenAnsatt(boolean egenAnsatt) {
        this.egenAnsatt = egenAnsatt;
    }

    public Bruker getBruker() {
        return bruker;
    }

    public void setBruker(Bruker bruker) {
        this.bruker = bruker;
    }

    public Adresse getBostedsadresse() {
        return bostedsadresse;
    }

    public void setBostedsadresse(Adresse bostedsadresse) {
        this.bostedsadresse = bostedsadresse;
    }

    public List<Virksomhet> getArbeidsgivendeVirksomheter() {
        return arbeidsgivendeVirksomheter;
    }

    public void setArbeidsgivendeVirksomheter(List<Virksomhet> arbeidsgivendeVirksomheter) {
        this.arbeidsgivendeVirksomheter = arbeidsgivendeVirksomheter;
    }

    public List<Virksomhet> getSelvstendigeVirksomheter() {
        return selvstendigeVirksomheter;
    }

    public void setSelvstendigeVirksomheter(List<Virksomhet> selvstendigeVirksomheter) {
        this.selvstendigeVirksomheter = selvstendigeVirksomheter;
    }

    public List<Arbeidssted> getArbeidssteder() {
        return arbeidssteder;
    }

    public void setArbeidssteder(List<Arbeidssted> arbeidssteder) {
        this.arbeidssteder = arbeidssteder;
    }

    public List<Virksomhet> getUtenlandskeVirksomheter() {
        return utenlandskeVirksomheter;
    }

    public void setUtenlandskeVirksomheter(List<Virksomhet> utenlandskeVirksomheter) {
        this.utenlandskeVirksomheter = utenlandskeVirksomheter;
    }

    public List<Lovvalgsperiode> getLovvalgsperioder() {
        return lovvalgsperioder;
    }

    public void setLovvalgsperioder(List<Lovvalgsperiode> lovvalgsperioder) {
        this.lovvalgsperioder = lovvalgsperioder;
    }

    public Long getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public void setGsakSaksnummer(Long gsakSaksnummer) {
        this.gsakSaksnummer = gsakSaksnummer;
    }
}
