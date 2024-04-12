package no.nav.melosys.service.eessi;

import java.util.List;
import java.util.stream.Stream;

import io.getunleash.Unleash;
import no.nav.melosys.domain.mottatteopplysninger.SedGrunnlag;
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland;
import no.nav.melosys.domain.mottatteopplysninger.data.OpplysningerOmBrukeren;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.UtenlandskIdent;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.eessi.sed.*;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.featuretoggle.ToggleName;
import org.springframework.util.CollectionUtils;

public class SedGrunnlagMapper {

    private SedGrunnlagMapper() {
        throw new IllegalStateException("Utility");
    }

    public static SedGrunnlag tilSedGrunnlag(SedGrunnlagDto sedGrunnlagDto, Unleash unleash) {
        SedGrunnlag sedGrunnlag = new SedGrunnlag();

        sedGrunnlag.personOpplysninger = tilPersonopplysninger(sedGrunnlagDto.getUtenlandskIdent());

        if(unleash.isEnabled(ToggleName.MELOSYS_CDM_4_3)) {
            sedGrunnlag.arbeidPaaLand.setFysiskeArbeidssteder(tilFysiskeArbeidssteder4_3(sedGrunnlagDto.getArbeidsland()));
        } else {
            sedGrunnlag.arbeidPaaLand.setFysiskeArbeidssteder(tilFysiskeArbeidssteder(sedGrunnlagDto.getArbeidssteder()));
        }
        sedGrunnlag.foretakUtland = tilForetakUtland(sedGrunnlagDto.getArbeidsgivendeVirksomheter(), sedGrunnlagDto.getSelvstendigeVirksomheter());
        sedGrunnlag.periode = tilPeriode(sedGrunnlagDto.getLovvalgsperioder());
        sedGrunnlag.setYtterligereInformasjon(sedGrunnlagDto.getYtterligereInformasjon());
        if(!sedGrunnlagDto.erA001()) {
            sedGrunnlag.soeknadsland.setLandkoder(sedGrunnlagDto.getLovvalgsperioder().stream().map(Lovvalgsperiode::getLovvalgsland).toList());
        }


        if (sedGrunnlagDto.erA003()) {
            SedGrunnlagA003Dto sedGrunnlagA003Dto = (SedGrunnlagA003Dto) sedGrunnlagDto;
            sedGrunnlag.setOvergangsregelbestemmelser(mapOvergangsregelbestemmelser(sedGrunnlagA003Dto.getOvergangsregelbestemmelser()));
            sedGrunnlag.juridiskArbeidsgiverNorge.setEkstraArbeidsgivere(sedGrunnlagA003Dto
                .getNorskeArbeidsgivendeVirksomheter()
                .stream()
                .map(Virksomhet::hentOrgnrEllerNavn)
                .toList());
        }

        return sedGrunnlag;
    }

    private static List<Overgangsregelbestemmelser> mapOvergangsregelbestemmelser(List<Bestemmelse> overgangsregelbestemmelser) {
        return overgangsregelbestemmelser.stream()
            .map(Bestemmelse::tilMelosysBestemmelse)
            .map(Overgangsregelbestemmelser.class::cast)
            .toList();
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
        opplysningerOmBrukeren.setUtenlandskIdent(tilUtenlandskIdent(utenlandskIdent));
        return opplysningerOmBrukeren;
    }

    private static List<UtenlandskIdent> tilUtenlandskIdent(List<Ident> utenlandskIdent) {
        return utenlandskIdent.stream().map(Ident::tilUtenlandskIdent).toList();
    }

    private static List<FysiskArbeidssted> tilFysiskeArbeidssteder(List<no.nav.melosys.domain.eessi.sed.Arbeidssted> arbeidssteder) {
        return arbeidssteder.stream().map(no.nav.melosys.domain.eessi.sed.Arbeidssted::tilFysiskArbeidssted).toList();
    }

    private static List<FysiskArbeidssted> tilFysiskeArbeidssteder4_3(List<no.nav.melosys.domain.eessi.sed.Arbeidsland> arbeidsland) {
        return arbeidsland.stream().flatMap(arbLand -> arbLand.getArbeidssted().stream().map(arbeidssted -> {
            arbeidssted.getAdresse().setLand(arbLand.getLand());
            return arbeidssted.tilFysiskArbeidssted();
        })).toList();
    }

    private static List<ForetakUtland> tilForetakUtland(List<Virksomhet> arbeidsgivendeVirksomheter, List<Virksomhet> selvstendigeVirksomheter) {
        return Stream.concat(
            arbeidsgivendeVirksomheter.stream().map(Virksomhet::tilForetakUtland),
            selvstendigeVirksomheter.stream().map(Virksomhet::tilSelvstendigForetakUtland)
        ).toList();
    }
}
