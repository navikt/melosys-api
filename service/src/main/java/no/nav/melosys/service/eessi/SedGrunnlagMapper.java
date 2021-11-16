package no.nav.melosys.service.eessi;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.data.*;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.eessi.sed.*;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser;
import no.nav.melosys.exception.FunksjonellException;
import org.springframework.util.CollectionUtils;

public class SedGrunnlagMapper {

    private SedGrunnlagMapper() {
        throw new IllegalStateException("Utility");
    }

    public static SedGrunnlag tilSedGrunnlag(SedGrunnlagDto sedGrunnlagDto) {
        SedGrunnlag sedGrunnlag = new SedGrunnlag();

        sedGrunnlag.personOpplysninger = tilPersonopplysninger(sedGrunnlagDto.getUtenlandskIdent());
        sedGrunnlag.bosted = tilBosted(sedGrunnlagDto.getBostedsadresse());
        sedGrunnlag.arbeidPaaLand.fysiskeArbeidssteder = tilFysiskeArbeidssteder(sedGrunnlagDto.getArbeidssteder());
        sedGrunnlag.foretakUtland = tilForetakUtland(sedGrunnlagDto.getArbeidsgivendeVirksomheter(), sedGrunnlagDto.getSelvstendigeVirksomheter());
        sedGrunnlag.periode = tilPeriode(sedGrunnlagDto.getLovvalgsperioder());
        sedGrunnlag.ytterligereInformasjon = sedGrunnlagDto.getYtterligereInformasjon();
        if(!sedGrunnlagDto.erA001()) {
            sedGrunnlag.soeknadsland.landkoder = sedGrunnlagDto.getLovvalgsperioder().stream().map(Lovvalgsperiode::getLovvalgsland).collect(Collectors.toList());
        }

        if (sedGrunnlagDto.erA003()) {
            SedGrunnlagA003Dto sedGrunnlagA003Dto = (SedGrunnlagA003Dto) sedGrunnlagDto;
            sedGrunnlag.overgangsregelbestemmelser = mapOvergangsregelbestemmelser(sedGrunnlagA003Dto.getOvergangsregelbestemmelser());
            sedGrunnlag.juridiskArbeidsgiverNorge.ekstraArbeidsgivere = sedGrunnlagA003Dto
                .getNorskeArbeidsgivendeVirksomheter()
                .stream()
                .map(Virksomhet::hentOrgnrEllerNavn)
                .collect(Collectors.toList());
        }

        return sedGrunnlag;
    }

    private static List<Overgangsregelbestemmelser> mapOvergangsregelbestemmelser(List<Bestemmelse> overgangsregelbestemmelser) {
        return overgangsregelbestemmelser.stream()
            .map(Bestemmelse::tilMelosysBestemmelse)
            .map(Overgangsregelbestemmelser.class::cast)
            .collect(Collectors.toList());
    }

    private static Periode tilPeriode(List<Lovvalgsperiode> lovvalgsperioder) {
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

    private static List<FysiskArbeidssted> tilFysiskeArbeidssteder(List<no.nav.melosys.domain.eessi.sed.Arbeidssted> arbeidssteder) {
        return arbeidssteder.stream().map(no.nav.melosys.domain.eessi.sed.Arbeidssted::tilFysiskArbeidssted).collect(Collectors.toList());
    }

    private static List<ForetakUtland> tilForetakUtland(List<Virksomhet> arbeidsgivendeVirksomheter, List<Virksomhet> selvstendigeVirksomheter) {
        return Stream.concat(
            arbeidsgivendeVirksomheter.stream().map(Virksomhet::tilForetakUtland),
            selvstendigeVirksomheter.stream().map(Virksomhet::tilSelvstendigForetakUtland)
        ).collect(Collectors.toList());
    }
}
