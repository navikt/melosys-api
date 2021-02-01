package no.nav.melosys.service.avklartefakta;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_LOVVALG_BARN;
import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.familie.OmfattetFamilie;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.behandling.BehandlingService;

@Service
public class AvklarteMedfolgendeFamilieService {

    private final BehandlingService behandlingService;

    private final BehandlingsresultatService behandlingsresultatService;

    private final AvklartefaktaService avklartefaktaService;

    @Autowired
    public AvklarteMedfolgendeFamilieService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService, AvklartefaktaService avklartefaktaService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.avklartefaktaService = avklartefaktaService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void lagreMedfolgendeFamilieSomAvklartefakta(long behandlingID, AvklarteMedfolgendeFamilie medfolgendeFamilie) throws FunksjonellException {
        Map<String, MedfolgendeFamilie.Relasjonsrolle> uuidOgRolleFraBehandlingsgrunnlag =
            behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID).getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().hentUuidOgRolleMedfølgendeFamilie();

        validerMedfolgendeFamilie(medfolgendeFamilie, uuidOgRolleFraBehandlingsgrunnlag);

        avklartefaktaService.slettAvklarteFakta(behandlingID, VURDERING_LOVVALG_BARN);
        avklartefaktaService.slettAvklarteFakta(behandlingID, VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER);

        lagreFamilieSomAvklartfakta(behandlingID, medfolgendeFamilie.tilAvklartefakta(uuidOgRolleFraBehandlingsgrunnlag));
    }

    private void validerMedfolgendeFamilie(AvklarteMedfolgendeFamilie medfolgendeFamilie, Map<String, MedfolgendeFamilie.Relasjonsrolle> uuidOgRolleFraBehandlingsgrunnlag) throws FunksjonellException {
        validerOmfattetFamilie(medfolgendeFamilie.getFamilieOmfattetAvNorskTrygd(), uuidOgRolleFraBehandlingsgrunnlag);
        validerIkkeOmfattetFamilie(medfolgendeFamilie.getFamilieIkkeOmfattetAvNorskTrygd(), uuidOgRolleFraBehandlingsgrunnlag);
    }

    private void validerOmfattetFamilie(Set<OmfattetFamilie> omfattetFamilieSet, Map<String, MedfolgendeFamilie.Relasjonsrolle> uuidOgRolle) throws FunksjonellException {
        for (OmfattetFamilie omfattetFamilie : omfattetFamilieSet) {
            if (!uuidOgRolle.containsKey(omfattetFamilie.getUuid())) {
                throw new FunksjonellException("Medfolgende familie som er omfattet av norsk trygd: " + omfattetFamilie.getUuid() + " er ikke lagret i behandlingsgrunnlaget.");
            }
        }
    }

    private void validerIkkeOmfattetFamilie(Set<IkkeOmfattetFamilie> ikkeOmfattetFamilieSet, Map<String, MedfolgendeFamilie.Relasjonsrolle> uuidOgRolle) throws FunksjonellException {
        var begrunnelserBarn = Stream.of(Medfolgende_barn_begrunnelser_ftrl.values()).map(Medfolgende_barn_begrunnelser_ftrl::getKode).collect(Collectors.toList());
        var begrunnelserEktefelleSamboer =Stream.of(Medfolgende_ektefelle_samboer_begrunnelser_ftrl.values()).map(Medfolgende_ektefelle_samboer_begrunnelser_ftrl::getKode).collect(Collectors.toList());

        for (IkkeOmfattetFamilie ikkeOmfattetFamilie : ikkeOmfattetFamilieSet) {
            if (!uuidOgRolle.containsKey(ikkeOmfattetFamilie.getUuid())) {
                throw new FunksjonellException("Medfolgende familie som ikke er omfattet av norsk trygd: " + ikkeOmfattetFamilie.getUuid() + " er ikke lagret i behandlingsgrunnlaget.");
            }
            if (ikkeOmfattetFamilie.getBegrunnelse() == null || ikkeOmfattetFamilie.getBegrunnelse().isEmpty()) {
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

    public void lagreFamilieSomAvklartfakta(long behandlingID, Collection<Avklartefakta> avklartefaktaFraMedfolgendeFamilie) throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        avklartefaktaFraMedfolgendeFamilie.forEach(avklartefakta -> {
            avklartefakta.setBehandlingsresultat(behandlingsresultat);
            avklartefaktaService.lagre(avklartefakta);
        });
    }
}
