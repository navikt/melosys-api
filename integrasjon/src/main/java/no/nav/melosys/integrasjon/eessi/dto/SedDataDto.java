package no.nav.melosys.integrasjon.eessi.dto;

import java.util.List;


public class SedDataDto {
    //Søknaddok.
    private List<Ident> utenlandskIdent;

    //Persondok.
    private List<FamilieMedlem> familieMedlem;
    private Bruker bruker;

    //Andre medlemsvariabler
    private Adresse bostedsadresse;
    private List<Virksomhet> arbeidsgivendeVirksomheter;
    private List<Virksomhet> selvstendigeVirksomheter;
    private List<Arbeidssted> arbeidssteder;
    private List<Virksomhet> utenlandskeVirksomheter;

    //Videresending av søknad
    private String avklartBostedsland;

    private Long gsakSaksnummer;

    //Lovvalg
    private List<Lovvalgsperiode> lovvalgsperioder;
    private List<Lovvalgsperiode> tidligereLovvalgsperioder;

    private String mottakerLand;
    private String mottakerId;

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

    public String getAvklartBostedsland() {
        return avklartBostedsland;
    }

    public void setAvklartBostedsland(String avklartBostedsland) {
        this.avklartBostedsland = avklartBostedsland;
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

    public List<Lovvalgsperiode> getTidligereLovvalgsperioder() {
        return tidligereLovvalgsperioder;
    }

    public void setTidligereLovvalgsperioder(List<Lovvalgsperiode> tidligereLovvalgsperioder) {
        this.tidligereLovvalgsperioder = tidligereLovvalgsperioder;
    }

    public String getMottakerLand() {
        return mottakerLand;
    }

    public void setMottakerLand(String mottakerLand) {
        this.mottakerLand = mottakerLand;
    }

    public String getMottakerId() {
        return mottakerId;
    }

    public void setMottakerId(String mottakerId) {
        this.mottakerId = mottakerId;
    }
}
