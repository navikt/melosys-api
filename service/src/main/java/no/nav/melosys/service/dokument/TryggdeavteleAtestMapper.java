package no.nav.melosys.service.dokument;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.transaction.Transactional;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Soeknadsland;
import no.nav.melosys.domain.brev.storbritannia.AttestStorbritanniaBrevbestilling;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.TrygdeavteleAtestDto;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.FamiliemedlemInfo;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.IdentType;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.IkkeOmfattetBarn;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.IkkeOmfattetEktefelle;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import org.springframework.stereotype.Component;

@Component
public class TryggdeavteleAtestMapper {

    private final AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    private final DokgenMapperDatahenter dokgenMapperDatahenter;

    public TryggdeavteleAtestMapper(AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService,
                                    DokgenMapperDatahenter dokgenMapperDatahenter) {
        this.avklarteMedfolgendeFamilieService = avklarteMedfolgendeFamilieService;
        this.dokgenMapperDatahenter = dokgenMapperDatahenter;
    }

    @Transactional
    public TrygdeavteleAtestDto map(AttestStorbritanniaBrevbestilling brevbestilling) {
        long behandlingId = brevbestilling.getBehandlingId();
        var behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(behandlingId);
        var avklarteMedfolgendeBarn = avklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(behandlingId);
        var avklarteMedfolgendeEktefelle = avklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(behandlingId);

        BehandlingsgrunnlagData behandlingsgrunnlagdata = behandlingsresultat.getBehandling().getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();

        return new TrygdeavteleAtestDto(
            brevbestilling,
            mapOmfattetFamilie(behandlingId, avklarteMedfolgendeEktefelle.getFamilieOmfattetAvNorskTrygd(), avklarteMedfolgendeBarn.barnOmfattetAvNorskTrygd),
            mapIkkeOmfattetBarn(behandlingId, avklarteMedfolgendeBarn.barnIkkeOmfattetAvNorskTrygd),
            mapIkkeOmfattetEktefelle(behandlingId, avklarteMedfolgendeEktefelle.getFamilieIkkeOmfattetAvNorskTrygd())
        );
    }

    private List<FamiliemedlemInfo> mapOmfattetFamilie(long behandlingID, Set<OmfattetFamilie> omfattetEktefelle, Set<OmfattetFamilie> omfattetBarn) {
        Map<String, MedfolgendeFamilie> medfolgendeEktefelle = avklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(behandlingID);
        Map<String, MedfolgendeFamilie> medfolgendeBarn = avklarteMedfolgendeFamilieService.hentMedfølgendeBarn(behandlingID);

        return Stream.concat(
            omfattetEktefelle.stream()
                .map(ektefelle -> tilFamiliemedlemInfo(medfolgendeEktefelle, ektefelle.getUuid())),
            omfattetBarn.stream()
                .map(barn -> tilFamiliemedlemInfo(medfolgendeBarn, barn.getUuid()))
        ).toList();
    }

    private List<IkkeOmfattetBarn> mapIkkeOmfattetBarn(long behandlingID, Set<no.nav.melosys.domain.person.familie.IkkeOmfattetBarn> barnIkkeOmfattetAvNorskTrygd) {
        Map<String, MedfolgendeFamilie> medfoelgendeBarn = avklarteMedfolgendeFamilieService.hentMedfølgendeBarn(behandlingID);
        return barnIkkeOmfattetAvNorskTrygd.stream()
            .map(ikkeOmfattetBarn -> new IkkeOmfattetBarn(tilFamiliemedlemInfo(medfoelgendeBarn, ikkeOmfattetBarn.uuid), ikkeOmfattetBarn.begrunnelse))
            .toList();
    }

    private IkkeOmfattetEktefelle mapIkkeOmfattetEktefelle(long behandlingId, Set<no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie> ektefelleIkkeOmfattet) {
        return ektefelleIkkeOmfattet.stream()
            .findFirst()
            .map(ikkeOmfattet -> new IkkeOmfattetEktefelle(
                tilFamiliemedlemInfo(avklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(behandlingId), ikkeOmfattet.getUuid()),
                ikkeOmfattet.getBegrunnelse()))
            .orElse(null);
    }

    private FamiliemedlemInfo tilFamiliemedlemInfo(Map<String, MedfolgendeFamilie> avklartMedfolgende, String uuid) {
        MedfolgendeFamilie medfolgendeFamilie = Optional.of(avklartMedfolgende.get(uuid))
            .orElseThrow(() -> new FunksjonellException("Avklart medfølgende familie " + uuid + " finnes ikke i behandlingsgrunnlaget"));
        String sammensattNavn = medfolgendeFamilie.fnr != null ? dokgenMapperDatahenter.hentSammensattNavn(medfolgendeFamilie.fnr) : medfolgendeFamilie.navn;
        return new FamiliemedlemInfo(sammensattNavn, medfolgendeFamilie.fnr, IdentType.FNR);
    }
}
