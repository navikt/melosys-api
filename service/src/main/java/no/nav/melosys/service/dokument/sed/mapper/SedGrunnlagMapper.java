package no.nav.melosys.service.dokument.sed.mapper;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.*;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.eessi.dto.*;
import org.apache.cxf.common.util.CollectionUtils;

public class SedGrunnlagMapper {

    private SedGrunnlagMapper() {
        throw new IllegalStateException("Utility");
    }

    public static SedGrunnlag lagSedGrunnlag(SedGrunnlagDto sedGrunnlagDto) throws FunksjonellException {
        SedGrunnlag sedGrunnlag = new SedGrunnlag();

        sedGrunnlag.personOpplysninger = tilPersonopplysninger(sedGrunnlagDto.getUtenlandskIdent());
        sedGrunnlag.bosted = tilBosted(sedGrunnlagDto.getBostedsadresse());
        sedGrunnlag.arbeidUtland = tilArbeidUtland(sedGrunnlagDto.getArbeidssteder());
        sedGrunnlag.foretakUtland = tilForetakUtland(sedGrunnlagDto.getArbeidsgivendeVirksomheter(), sedGrunnlagDto.getSelvstendigeVirksomheter());
        sedGrunnlag.periode = tilPeriode(sedGrunnlagDto.getLovvalgsperioder());
        sedGrunnlag.ytterligereInformasjon = sedGrunnlagDto.getYtterligereInformasjon();

        if (sedGrunnlagDto.erA003()) {
            SedGrunnlagA003Dto sedGrunnlagA003Dto = (SedGrunnlagA003Dto) sedGrunnlagDto;
            sedGrunnlag.overgangsregelbestemmelser = mapOvergangsregelbestemmelser(sedGrunnlagA003Dto.getOvergangsregelbestemmelser());
            sedGrunnlag.juridiskArbeidsgiverNorge = mapJuridiskArbeidsgiver(sedGrunnlagA003Dto);
            sedGrunnlag.norskeArbeidsgivere = sedGrunnlagA003Dto.getNorskeArbeidsgivendeVirksomheter().stream()
                .map(Virksomhet::tilOrganisasjon).collect(Collectors.toList());
        }

        return sedGrunnlag;
    }

    private static List<Overgangsregelbestemmelser> mapOvergangsregelbestemmelser(List<Bestemmelse> overgangsregelbestemmelser) {
        return overgangsregelbestemmelser.stream()
            .map(LovvalgTilBestemmelseDtoMapper::mapBestemmelseDtoTilMelosysLovvalgBestemmelse)
            .map(lovvalgBestemmelse -> (Overgangsregelbestemmelser) lovvalgBestemmelse)
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
