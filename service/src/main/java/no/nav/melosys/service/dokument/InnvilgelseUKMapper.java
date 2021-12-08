package no.nav.melosys.service.dokument;

import javax.transaction.Transactional;

import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.data.IdentType;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia.*;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

@Component
public class InnvilgelseUKMapper {

    private final AvklarteMedfolgendeFamilieService avklarteMedfølgendeFamilieService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public InnvilgelseUKMapper(AvklarteMedfolgendeFamilieService avklarteMedfølgendeFamilieService,
                               LovvalgsperiodeService lovvalgsperiodeService) {
        this.avklarteMedfølgendeFamilieService = avklarteMedfølgendeFamilieService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    @Transactional
    public InnvilgelseUK map(InnvilgelseBrevbestilling brevbestilling) {
        var behandling = brevbestilling.getBehandling();
        var behandlingsgrunnlag = behandling.getBehandlingsgrunnlag();
        var lovvalgsperiode = lovvalgsperiodeService.hentValidertLovvalgsperiode(behandling.getId());

        return new InnvilgelseUK.Builder(brevbestilling)
            .artikkel((Lovvalgbestemmelser_trygdeavtale_uk) lovvalgsperiode.getBestemmelse())
            .soknad(lagSøknad(behandlingsgrunnlag))
            .familie(lagFamile(behandling.getId()))
            .virksomhetArbeidsgiverSkalHaKopi(false)
            .build();
    }

    private Familie lagFamile(long behandlingID) {
        return new Familie.Builder()
            .barn(finnBarn(behandlingID))
            .ektefelle(finnEktefelle(behandlingID))
            .build();
    }

    private Ektefelle finnEktefelle(long behandlingID) {
        var avklartMedfølgendeEktefelle = avklarteMedfølgendeFamilieService.hentAvklartMedfølgendeEktefelle(behandlingID);
        var medfolgendeEktefelleMap = avklarteMedfølgendeFamilieService.hentMedfølgendEktefelle(behandlingID);
        var ektefelleOmfattetAvNorskTrygd = avklartMedfølgendeEktefelle.getFamilieOmfattetAvNorskTrygd();
        if (!ektefelleOmfattetAvNorskTrygd.isEmpty()) {
            var ektefelleOmfattet = ektefelleOmfattetAvNorskTrygd.iterator().next();
            return tilEktefelle(medfolgendeEktefelleMap, ektefelleOmfattet.getUuid(), null);
        }
        var ektefelleIkkeOmfattetAvNorskTrygd = avklartMedfølgendeEktefelle.getFamilieIkkeOmfattetAvNorskTrygd();
        if (ektefelleIkkeOmfattetAvNorskTrygd.isEmpty()) {
            return null;
        }

        IkkeOmfattetFamilie ikkeOmfattetEktefelle = ektefelleIkkeOmfattetAvNorskTrygd.iterator().next();
        return tilEktefelle(medfolgendeEktefelleMap, ikkeOmfattetEktefelle.getUuid(), ikkeOmfattetEktefelle.getBegrunnelse());
    }

    private Ektefelle tilEktefelle(Map<String, MedfolgendeFamilie> medfølgendeFamilieMap, String uuid, String begrunnelse) {
        var medfølgendeFamilie = Optional.of(medfølgendeFamilieMap.get(uuid))
            .orElseThrow(() -> new FunksjonellException("Avklart medfølgende familie " + uuid + " finnes ikke i behandlingsgrunnlaget"));

        IdentType identType = medfølgendeFamilie.utledIdentType();
        return new Ektefelle.Builder()
            .fnr(identType == IdentType.FNR ? medfølgendeFamilie.getFnr() : null)
            .dnr(identType == IdentType.DNR ? medfølgendeFamilie.getFnr() : null)
            .navn(medfølgendeFamilie.getNavn())
            .begrunnelse(begrunnelse)
            .fødselsdato(medfølgendeFamilie.datoFraFnr())
            .build();
    }

    private List<Barn> finnBarn(long behandlingID) {
        var avklarteMedfølgendeBarn = avklarteMedfølgendeFamilieService.hentAvklarteMedfølgendeBarn(behandlingID);
        var barnOmfattetAvNorskTrygd = avklarteMedfølgendeBarn.getFamilieOmfattetAvNorskTrygd();
        var barnIkkeOmfattetAvNorskTrygd = avklarteMedfølgendeBarn.getFamilieIkkeOmfattetAvNorskTrygd();

        var medfølgendeBarn = avklarteMedfølgendeFamilieService.hentMedfølgendeBarn(behandlingID);

        return Stream.concat(barnOmfattetAvNorskTrygd.stream()
                .map(omfattetFamilie -> tilBarn(medfølgendeBarn, omfattetFamilie.getUuid(), null)),
            barnIkkeOmfattetAvNorskTrygd.stream()
                .map(ikkeOmfattetBarn -> tilBarn(medfølgendeBarn, ikkeOmfattetBarn.getUuid(), ikkeOmfattetBarn.getBegrunnelse()))
        ).toList();
    }

    private Barn tilBarn(Map<String, MedfolgendeFamilie> medfølgendeBarnMap, String uuid, String begrunnelse) {
        var medfølgendeBarn = Optional.of(medfølgendeBarnMap.get(uuid))
            .orElseThrow(() -> new FunksjonellException("Avklart medfølgende familie " + uuid +
                " finnes ikke i behandlingsgrunnlaget"));
        var identType = medfølgendeBarn.utledIdentType();
        return new Barn.Builder()
            .navn(medfølgendeBarn.getNavn())
            .fnr(identType == IdentType.FNR ? medfølgendeBarn.getFnr() : null)
            .dnr(identType == IdentType.DNR ? medfølgendeBarn.getFnr() : null)
            .foedselsdato(medfølgendeBarn.datoFraFnr())
            .begrunnelse(begrunnelse)
            .build();
    }

    private Soknad lagSøknad(Behandlingsgrunnlag behandlingsgrunnlag) {
        var foretakUtland = behandlingsgrunnlag.getBehandlingsgrunnlagdata().foretakUtland.stream()
            .filter(foretak -> foretak.adresse.getLandkode().equals(Landkoder.GB.getKode()))
            .findFirst().orElseThrow(() -> new FunksjonellException("Ingen utenlandske virksomheter funnet"));

        var periode = behandlingsgrunnlag.getBehandlingsgrunnlagdata().periode;
        return new Soknad(
            behandlingsgrunnlag.getMottaksdato(),
            periode.getFom(),
            periode.getTom(),
            foretakUtland.navn
        );
    }
}
