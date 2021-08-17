package no.nav.melosys.service.avklartefakta;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.domain.person.familie.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.apache.cxf.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_LOVVALG_BARN;
import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER;

@Service
public class AvklarteMedfolgendeFamilieService {

    private final BehandlingService behandlingService;

    private final BehandlingsresultatService behandlingsresultatService;

    private final AvklartefaktaService avklartefaktaService;

    private final BehandlingsgrunnlagService behandlingsgrunnlagService;

    @Autowired
    public AvklarteMedfolgendeFamilieService(BehandlingService behandlingService,
                                             BehandlingsresultatService behandlingsresultatService,
                                             AvklartefaktaService avklartefaktaService,
                                             BehandlingsgrunnlagService behandlingsgrunnlagService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.avklartefaktaService = avklartefaktaService;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
    }

    @Transactional
    public void lagreMedfolgendeFamilieSomAvklartefakta(long behandlingID, AvklarteMedfolgendeFamilie medfolgendeFamilie) {
        Map<String, MedfolgendeFamilie.Relasjonsrolle> uuidOgRolleFraBehandlingsgrunnlag =
            behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID).getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().hentUuidOgRolleMedfølgendeFamilie();

        validerMedfolgendeFamilie(medfolgendeFamilie, uuidOgRolleFraBehandlingsgrunnlag);

        avklartefaktaService.slettAvklarteFakta(behandlingID, VURDERING_LOVVALG_BARN);
        avklartefaktaService.slettAvklarteFakta(behandlingID, VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER);

        lagreFamilieSomAvklartfakta(behandlingID, medfolgendeFamilie.tilAvklartefakta(uuidOgRolleFraBehandlingsgrunnlag));
    }

    public AvklarteMedfolgendeBarn hentAvklarteMedfølgendeBarn(long behandlingId) {
        AvklarteMedfolgendeBarn avklarteMedfolgendeBarn = avklartefaktaService.hentAvklarteMedfølgendeBarn(behandlingId);
        Map<String, MedfolgendeFamilie> medfølgendeBarn = hentMedfølgendeBarn(behandlingId);
        avklarteMedfolgendeBarn.barnOmfattetAvNorskTrygd.stream()
            .filter(omfattetBarn -> !medfølgendeBarn.containsKey(omfattetBarn.getUuid())).forEach(omfattetBarn -> {
            throw new FunksjonellException("Avklart medfølgende barn " + omfattetBarn.getUuid() + " finnes ikke i behandlingsgrunnlaget");
        });

        avklarteMedfolgendeBarn.barnIkkeOmfattetAvNorskTrygd.stream()
            .filter(ikkeMedfølgendeBarn -> !medfølgendeBarn.containsKey(ikkeMedfølgendeBarn.uuid)).forEach(ikkeMedfølgendeBarn -> {
            throw new FunksjonellException("Avklart medfølgende barn " + ikkeMedfølgendeBarn.uuid + " finnes ikke i behandlingsgrunnlaget");
        });

        return avklarteMedfolgendeBarn;
    }

    public AvklarteMedfolgendeFamilie hentAvklartMedfølgendeEktefelle(long behandlingId) {
        AvklarteMedfolgendeFamilie avklarteMedfølgendeEktefelle = avklartefaktaService.hentAvklarteMedfølgendeEktefelle(behandlingId);
        Map<String, MedfolgendeFamilie> medfolgendeEktefelle = hentMedfølgendEktefelle(behandlingId);
        avklarteMedfølgendeEktefelle.getFamilieOmfattetAvNorskTrygd().stream()
            .filter(omfattetEkte -> !medfolgendeEktefelle.containsKey(omfattetEkte.getUuid())).forEach(omfattetEkte -> {
            throw new FunksjonellException("Avklart medfølgende ektefelle/samboer " + omfattetEkte.getUuid() + " finnes ikke i behandlingsgrunnlaget");
        });

        avklarteMedfølgendeEktefelle.getFamilieIkkeOmfattetAvNorskTrygd().stream()
            .filter(ikkeOmfattetEkte -> !medfolgendeEktefelle.containsKey(ikkeOmfattetEkte.getUuid())).forEach(ikkeOmfattetEkte -> {
            throw new FunksjonellException("Avklart medfølgende ektefelle/samboer " + ikkeOmfattetEkte.getUuid() + " finnes ikke i behandlingsgrunnlaget");
        });

        return avklarteMedfølgendeEktefelle;
    }

    public Map<String, MedfolgendeFamilie> hentMedfølgendeBarn(long behandlingID) {
        var behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID);
        return behandlingsgrunnlag == null ? Collections.emptyMap()
            : behandlingsgrunnlag.getBehandlingsgrunnlagdata().hentMedfølgendeBarn();
    }

    public Map<String, MedfolgendeFamilie> hentMedfølgendEktefelle(long behandlingID) {
        var behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID);
        return behandlingsgrunnlag == null ? Collections.emptyMap()
            : behandlingsgrunnlag.getBehandlingsgrunnlagdata().hentMedfølgendeEktefelle();
    }

    private void validerMedfolgendeFamilie(AvklarteMedfolgendeFamilie medfolgendeFamilie, Map<String, MedfolgendeFamilie.Relasjonsrolle> uuidOgRolleFraBehandlingsgrunnlag) {
        validerOmfattetFamilie(medfolgendeFamilie.getFamilieOmfattetAvNorskTrygd(), uuidOgRolleFraBehandlingsgrunnlag);
        validerIkkeOmfattetFamilie(medfolgendeFamilie.getFamilieIkkeOmfattetAvNorskTrygd(), uuidOgRolleFraBehandlingsgrunnlag);
    }

    private void validerOmfattetFamilie(Set<OmfattetFamilie> omfattetFamilieSet, Map<String, MedfolgendeFamilie.Relasjonsrolle> uuidOgRolle) {
        for (OmfattetFamilie omfattetFamilie : omfattetFamilieSet) {
            if (!uuidOgRolle.containsKey(omfattetFamilie.getUuid())) {
                throw new FunksjonellException("Medfolgende familie som er omfattet av norsk trygd: " + omfattetFamilie.getUuid() + " er ikke lagret i behandlingsgrunnlaget.");
            }
        }
    }

    private void validerIkkeOmfattetFamilie(Set<IkkeOmfattetFamilie> ikkeOmfattetFamilieSet, Map<String, MedfolgendeFamilie.Relasjonsrolle> uuidOgRolle) {
        var begrunnelserBarn = Stream.of(Medfolgende_barn_begrunnelser_ftrl.values()).map(Medfolgende_barn_begrunnelser_ftrl::getKode).collect(Collectors.toList());
        var begrunnelserEktefelleSamboer =Stream.of(Medfolgende_ektefelle_samboer_begrunnelser_ftrl.values()).map(Medfolgende_ektefelle_samboer_begrunnelser_ftrl::getKode).collect(Collectors.toList());

        for (IkkeOmfattetFamilie ikkeOmfattetFamilie : ikkeOmfattetFamilieSet) {
            if (!uuidOgRolle.containsKey(ikkeOmfattetFamilie.getUuid())) {
                throw new FunksjonellException("Medfolgende familie som ikke er omfattet av norsk trygd: " + ikkeOmfattetFamilie.getUuid() + " er ikke lagret i behandlingsgrunnlaget.");
            }
            if (StringUtils.isEmpty(ikkeOmfattetFamilie.getBegrunnelse())) {
                throw new FunksjonellException("Begrunnelsen til medfolgende familie " + ikkeOmfattetFamilie.getUuid() + ": " + ikkeOmfattetFamilie.getBegrunnelse() + " er ikke satt.");
            }
            if (MedfolgendeFamilie.Relasjonsrolle.BARN.equals(uuidOgRolle.get(ikkeOmfattetFamilie.getUuid())) && !begrunnelserBarn.contains(ikkeOmfattetFamilie.getBegrunnelse())) {
                throw new FunksjonellException("Begrunnelsen til medfolgende barn " + ikkeOmfattetFamilie.getUuid() + ": " + ikkeOmfattetFamilie.getBegrunnelse() + " er ikke gyldig.");
            }
            if (MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER.equals(uuidOgRolle.get(ikkeOmfattetFamilie.getUuid())) && !begrunnelserEktefelleSamboer.contains(ikkeOmfattetFamilie.getBegrunnelse())) {
                throw new FunksjonellException("Begrunnelsen til medfolgende ektefelle/samboer " + ikkeOmfattetFamilie.getUuid() + ": " + ikkeOmfattetFamilie.getBegrunnelse() + " er ikke gyldig.");
            }
        }
    }

    private void lagreFamilieSomAvklartfakta(long behandlingID, Collection<Avklartefakta> avklartefaktaFraMedfolgendeFamilie) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        avklartefaktaFraMedfolgendeFamilie.forEach(avklartefakta -> {
            avklartefakta.setBehandlingsresultat(behandlingsresultat);
            avklartefaktaService.lagre(avklartefakta);
        });
    }
}
