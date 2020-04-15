package no.nav.melosys.integrasjon.eessi.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.*;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser;
import no.nav.melosys.exception.FunksjonellException;
import org.springframework.util.CollectionUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "sedType", visible = true)
@JsonTypeIdResolver(SedGrunnlagTypeResolver.class)
public class SedGrunnlagDto {
    private String sedType;
    private List<Ident> utenlandskIdent = new ArrayList<>();
    private Adresse bostedsadresse;
    private List<Virksomhet> arbeidsgivendeVirksomheter = new ArrayList<>();
    private List<Virksomhet> selvstendigeVirksomheter = new ArrayList<>();
    private List<Arbeidssted> arbeidssteder = new ArrayList<>();
    private List<Lovvalgsperiode> lovvalgsperioder = new ArrayList<>();
    private String ytterligereInformasjon;

    public boolean erA003() {
        return SedType.A003.name().equalsIgnoreCase(getSedType()) && this instanceof SedGrunnlagA003Dto;
    }

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

    public String getSedType() {
        return sedType;
    }

    public SedGrunnlag tilDomene() throws FunksjonellException {
        SedGrunnlag sedGrunnlag = new SedGrunnlag();

        sedGrunnlag.personOpplysninger = tilPersonopplysninger(utenlandskIdent);
        sedGrunnlag.bosted = tilBosted(bostedsadresse);
        sedGrunnlag.arbeidUtland = tilArbeidUtland(arbeidssteder);
        sedGrunnlag.foretakUtland = tilForetakUtland(arbeidsgivendeVirksomheter, selvstendigeVirksomheter);
        sedGrunnlag.periode = tilPeriode(lovvalgsperioder);
        sedGrunnlag.ytterligereInformasjon = ytterligereInformasjon;

        if (erA003()) {
            SedGrunnlagA003Dto sedGrunnlagA003Dto = (SedGrunnlagA003Dto) this;
            sedGrunnlag.overgangsregelbestemmelser = mapOvergangsregelbestemmelser(sedGrunnlagA003Dto.getOvergangsregelbestemmelser());
            sedGrunnlag.juridiskArbeidsgiverNorge = mapJuridiskArbeidsgiver(sedGrunnlagA003Dto);
            sedGrunnlag.norskeArbeidsgivere = sedGrunnlagA003Dto.getNorskeArbeidsgivendeVirksomheter().stream()
                .map(Virksomhet::tilOrganisasjon).collect(Collectors.toList());
        }

        return sedGrunnlag;
    }

    private static List<Overgangsregelbestemmelser> mapOvergangsregelbestemmelser(List<Bestemmelse> overgangsregelbestemmelser) {
        return overgangsregelbestemmelser.stream()
            .map(Bestemmelse::tilMelosysBestemmelse)
            .map(Overgangsregelbestemmelser.class::cast)
            .collect(Collectors.toList());
    }

    private static JuridiskArbeidsgiverNorge mapJuridiskArbeidsgiver(SedGrunnlagA003Dto sedGrunnlagA003Dto) {
        JuridiskArbeidsgiverNorge juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();
        juridiskArbeidsgiverNorge.ekstraArbeidsgivere = sedGrunnlagA003Dto.getNorskeArbeidsgivendeVirksomheter().stream()
            .map(Virksomhet::getOrgnr).collect(Collectors.toList());

        return juridiskArbeidsgiverNorge;
    }

    private static Periode tilPeriode(List<Lovvalgsperiode> lovvalgsperioder) throws FunksjonellException {
        if (CollectionUtils.isEmpty(lovvalgsperioder)) {
            return new Periode();
        }

        if (lovvalgsperioder.size() > 1) {
            throw new FunksjonellException("Mottatt flere lovvalgsperioder fra SED");
        }

        return lovvalgsperioder.iterator().next().tilPeriode();
    }

    private static OpplysningerOmBrukeren tilPersonopplysninger(List<Ident> utenlandskIdent) {
        OpplysningerOmBrukeren opplysningerOmBrukeren = new OpplysningerOmBrukeren();
        opplysningerOmBrukeren.utenlandskIdent = tilUtenlandskIdent(utenlandskIdent);
        return opplysningerOmBrukeren;
    }

    private static List<UtenlandskIdent> tilUtenlandskIdent(List<Ident> utenlandskIdent) {
        return utenlandskIdent.stream().map(Ident::tilUtenlandskIdent).collect(Collectors.toList());
    }

    private static Bosted tilBosted(Adresse bostedsadresse) {
        Bosted bosted = new Bosted();
        bosted.oppgittAdresse = bostedsadresse.tilStrukturertAdresse();
        return bosted;
    }

    private static List<ArbeidUtland> tilArbeidUtland(List<Arbeidssted> arbeidssteder) {
        return arbeidssteder.stream().map(Arbeidssted::tilArbeidUtland).collect(Collectors.toList());
    }

    private static List<ForetakUtland> tilForetakUtland(List<Virksomhet> arbeidsgivendeVirksomheter, List<Virksomhet> selvstendigeVirksomheter) {
        return Stream.concat(
            arbeidsgivendeVirksomheter.stream().map(virksomhet -> virksomhet.tilForetakUtland(false)),
            selvstendigeVirksomheter.stream().map(virksomhet -> virksomhet.tilForetakUtland(true))
        ).collect(Collectors.toList());
    }
}
