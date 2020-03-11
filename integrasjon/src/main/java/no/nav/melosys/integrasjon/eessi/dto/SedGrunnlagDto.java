package no.nav.melosys.integrasjon.eessi.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import no.nav.melosys.domain.eessi.SedType;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "sedType")
@JsonTypeIdResolver(SedGrunnlagTypeResolver.class)
public class SedGrunnlagDto {
    private SedType sedType;
    private List<Ident> utenlandskIdent = new ArrayList<>();
    private Adresse bostedsadresse;
    private List<Virksomhet> arbeidsgivendeVirksomheter = new ArrayList<>();
    private List<Virksomhet> selvstendigeVirksomheter = new ArrayList<>();
    private List<Arbeidssted> arbeidssteder = new ArrayList<>();
    private List<Lovvalgsperiode> lovvalgsperioder = new ArrayList<>();
    private String ytterligereInformasjon;

    public List<Ident> getUtenlandskIdent() {
        return utenlandskIdent;
    }

    public void setUtenlandskIdent(List<Ident> utenlandskIdent) {
        this.utenlandskIdent = utenlandskIdent;
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

    public List<Lovvalgsperiode> getLovvalgsperioder() {
        return lovvalgsperioder;
    }

    public void setLovvalgsperioder(List<Lovvalgsperiode> lovvalgsperioder) {
        this.lovvalgsperioder = lovvalgsperioder;
    }

    public String getYtterligereInformasjon() {
        return ytterligereInformasjon;
    }

    public void setYtterligereInformasjon(String ytterligereInformasjon) {
        this.ytterligereInformasjon = ytterligereInformasjon;
    }

    public SedType getSedType() {
        return sedType;
    }
}
