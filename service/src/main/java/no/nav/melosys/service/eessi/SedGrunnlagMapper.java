package no.nav.melosys.service.eessi;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.dokument.soeknad.*;
import no.nav.melosys.domain.eessi.SedOrganisasjon;
import no.nav.melosys.domain.eessi.sed.*;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser;
import no.nav.melosys.exception.FunksjonellException;
import org.springframework.util.CollectionUtils;

public class SedGrunnlagMapper {

    private SedGrunnlagMapper() {
        throw new IllegalStateException("Utility");
    }

    public static SedGrunnlag tilSedGrunnlag(SedGrunnlagDto sedGrunnlagDto) throws FunksjonellException {
        SedGrunnlag sedGrunnlag = new SedGrunnlag();

        sedGrunnlag.personOpplysninger = tilPersonopplysninger(sedGrunnlagDto.getUtenlandskIdent());
        sedGrunnlag.bosted = tilBosted(sedGrunnlagDto.getBostedsadresse());
        sedGrunnlag.arbeidUtland = tilArbeidUtland(sedGrunnlagDto.getArbeidssteder());
        sedGrunnlag.foretakUtland = tilForetakUtland(sedGrunnlagDto.getArbeidsgivendeVirksomheter(), sedGrunnlagDto.getSelvstendigeVirksomheter());
        sedGrunnlag.periode = tilPeriode(sedGrunnlagDto.getLovvalgsperioder());
        sedGrunnlag.ytterligereInformasjon = sedGrunnlagDto.getYtterligereInformasjon();
        sedGrunnlag.soeknadsland.landkoder = sedGrunnlagDto.getLovvalgsperioder().stream().map(Lovvalgsperiode::getLovvalgsland).collect(Collectors.toList());

        if (sedGrunnlagDto.erA003()) {
            SedGrunnlagA003Dto sedGrunnlagA003Dto = (SedGrunnlagA003Dto) sedGrunnlagDto;
            sedGrunnlag.overgangsregelbestemmelser = mapOvergangsregelbestemmelser(sedGrunnlagA003Dto.getOvergangsregelbestemmelser());
            sedGrunnlag.norskeArbeidsgivere = tilSedOrganisasjoner(sedGrunnlagA003Dto.getNorskeArbeidsgivendeVirksomheter());
        }

        return sedGrunnlag;
    }

    private static List<SedOrganisasjon> tilSedOrganisasjoner(List<Virksomhet> norskeArbeidsgivendeVirksomheter) {
        return norskeArbeidsgivendeVirksomheter.stream().map(Virksomhet::tilOrganisasjon).collect(Collectors.toList());
    }

    private static List<Overgangsregelbestemmelser> mapOvergangsregelbestemmelser(List<Bestemmelse> overgangsregelbestemmelser) {
        return overgangsregelbestemmelser.stream()
            .map(Bestemmelse::tilMelosysBestemmelse)
            .map(Overgangsregelbestemmelser.class::cast)
            .collect(Collectors.toList());
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
            arbeidsgivendeVirksomheter.stream().map(Virksomhet::tilForetakUtland),
            selvstendigeVirksomheter.stream().map(Virksomhet::tilSelvstendigForetakUtland)
        ).collect(Collectors.toList());
    }
}
