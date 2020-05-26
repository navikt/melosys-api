package no.nav.melosys.service.eessi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.dokument.soeknad.*;
import no.nav.melosys.domain.eessi.sed.*;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class SedGrunnlagMapper {

    private final EregFasade eregFasade;

    @Autowired
    public SedGrunnlagMapper(EregFasade eregFasade) {
        this.eregFasade = eregFasade;
    }

    public SedGrunnlag mapSedGrunnlag(SedGrunnlagDto sedGrunnlagDto) throws IntegrasjonException, FunksjonellException {
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

            List<Virksomhet> gyldigeVirksomheter = hentGyldigeVirksomheter(sedGrunnlagA003Dto.getNorskeArbeidsgivendeVirksomheter());
            List<ForetakUtland> foretakUtland = sedGrunnlagA003Dto.getNorskeArbeidsgivendeVirksomheter().stream()
                .filter(virksomhet -> !gyldigeVirksomheter.contains(virksomhet))
                .map(Virksomhet::tilForetakUtland).collect(Collectors.toList());

            sedGrunnlag.juridiskArbeidsgiverNorge = tilJuridiskArbeidsgiver(gyldigeVirksomheter);
            sedGrunnlag.foretakUtland.addAll(foretakUtland);
        }

        return sedGrunnlag;
    }

    private JuridiskArbeidsgiverNorge tilJuridiskArbeidsgiver(List<Virksomhet> norskeArbeidsgivendeVirksomheter) {
        JuridiskArbeidsgiverNorge juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();
        juridiskArbeidsgiverNorge.ekstraArbeidsgivere = norskeArbeidsgivendeVirksomheter.stream()
            .map(Virksomhet::getOrgnr).collect(Collectors.toList());

        return juridiskArbeidsgiverNorge;
    }

    private List<Virksomhet> hentGyldigeVirksomheter(List<Virksomhet> norskeArbeidsgivendeVirksomheter) throws IntegrasjonException {
        List<Virksomhet> virksomheterMedGyldigeOrgnummer = new ArrayList<>();
        for (var virksomhet : norskeArbeidsgivendeVirksomheter) {
            if (virksomhet.getOrgnr() != null && eregFasade.organisasjonFinnes(virksomhet.getOrgnr())) {
                virksomheterMedGyldigeOrgnummer.add(virksomhet);
            }
        }

        return virksomheterMedGyldigeOrgnummer;
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
