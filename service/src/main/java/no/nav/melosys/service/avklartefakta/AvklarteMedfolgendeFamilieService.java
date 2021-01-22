package no.nav.melosys.service.avklartefakta;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_LOVVALG_BARN;
import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER;

import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.familie.OmfattetFamilie;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.behandling.BehandlingService;

@Service
public class AvklarteMedfolgendeFamilieService {

    private final BehandlingService behandlingService;

    private final AvklartefaktaService avklartefaktaService;

    @Autowired
    public AvklarteMedfolgendeFamilieService(BehandlingService behandlingService, AvklartefaktaService avklartefaktaService) {
        this.behandlingService = behandlingService;
        this.avklartefaktaService = avklartefaktaService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void lagreMedfolgendeFamilieSomAvklartefakta(long behandlingID, AvklarteMedfolgendeFamilie medfolgendeBarn, AvklarteMedfolgendeFamilie medfolgendeEktefelleSamboer) throws FunksjonellException {
        validerMedfolgendeFamilie(behandlingID, medfolgendeBarn, medfolgendeEktefelleSamboer);

        avklartefaktaService.slettAvklarteFakta(behandlingID, VURDERING_LOVVALG_BARN);
        avklartefaktaService.slettAvklarteFakta(behandlingID, VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER);

        lagreOmfattetFamilieSomAvklartfakta(behandlingID, VURDERING_LOVVALG_BARN, medfolgendeBarn.getFamilieOmfattetAvNorskTrygd());
        lagreIkkeOmfattetFamilieSomAvklartfakta(behandlingID, VURDERING_LOVVALG_BARN, medfolgendeBarn.getFamilieIkkeOmfattetAvNorskTrygd());
        lagreOmfattetFamilieSomAvklartfakta(behandlingID, VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, medfolgendeEktefelleSamboer.getFamilieOmfattetAvNorskTrygd());
        lagreIkkeOmfattetFamilieSomAvklartfakta(behandlingID, VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, medfolgendeEktefelleSamboer.getFamilieIkkeOmfattetAvNorskTrygd());
    }

    private void validerMedfolgendeFamilie(long behandlingID, AvklarteMedfolgendeFamilie medfolgendeBarn, AvklarteMedfolgendeFamilie medfolgendeEktefelleSamboer) throws FunksjonellException {
        Map<String, MedfolgendeFamilie.Relasjonsrolle> uuidOgRolleFraBehandlingsgrunnlag = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID).getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().hentUuidOgRolleMedfølgendeFamilie();
        validerOmfattetFamilie(medfolgendeBarn.getFamilieOmfattetAvNorskTrygd(), true, uuidOgRolleFraBehandlingsgrunnlag);
        validerOmfattetFamilie(medfolgendeEktefelleSamboer.getFamilieOmfattetAvNorskTrygd(), false, uuidOgRolleFraBehandlingsgrunnlag);
        validerIkkeOmfattetFamilie(medfolgendeBarn.getFamilieIkkeOmfattetAvNorskTrygd(), true, uuidOgRolleFraBehandlingsgrunnlag);
        validerIkkeOmfattetFamilie(medfolgendeEktefelleSamboer.getFamilieIkkeOmfattetAvNorskTrygd(), false, uuidOgRolleFraBehandlingsgrunnlag);
    }

    private void validerOmfattetFamilie(Set<OmfattetFamilie> omfattetFamilieSet, boolean barn, Map<String, MedfolgendeFamilie.Relasjonsrolle> uuidOgRolle) throws FunksjonellException {
        for (OmfattetFamilie omfattetFamilie : omfattetFamilieSet) {
            if (!uuidOgRolle.containsKey(omfattetFamilie.getUuid())){
                throw new FunksjonellException("Medfolgende familie som er omfattet av norsk trygd: " + omfattetFamilie.getUuid() + " er ikke lagret i behandlingsgrunnlaget.");
            }
            if (barn ? uuidOgRolle.get(omfattetFamilie.getUuid()) != MedfolgendeFamilie.Relasjonsrolle.BARN : uuidOgRolle.get(omfattetFamilie.getUuid()) != MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER) {
                throw new FunksjonellException("Medfolgende familie som er omfattet av norsk trygd: " + omfattetFamilie.getUuid() + " er lagret med feil relasjonsrolle.");
            }
        }
    }

    private void validerIkkeOmfattetFamilie(Set<IkkeOmfattetFamilie> ikkeOmfattetFamilieSet, boolean barn, Map<String, MedfolgendeFamilie.Relasjonsrolle> uuidOgRolle) throws FunksjonellException {
        for (IkkeOmfattetFamilie ikkeOmfattetFamilie : ikkeOmfattetFamilieSet) {
            try {
                Kodeverk begrunnelse = barn ? ((Medfolgende_barn_begrunnelser_ftrl) ikkeOmfattetFamilie.getBegrunnelse()) : ((Medfolgende_ektefelle_samboer_begrunnelser_ftrl) ikkeOmfattetFamilie.getBegrunnelse());
            } catch (RuntimeException e) {
                throw new FunksjonellException("Begrunnelsen til medfolgende ektefelle/samboer: " + ikkeOmfattetFamilie.getBegrunnelse() + " er ikke gyldig.");
            }
            if (barn ? uuidOgRolle.get(ikkeOmfattetFamilie.getUuid()) != MedfolgendeFamilie.Relasjonsrolle.BARN : uuidOgRolle.get(ikkeOmfattetFamilie.getUuid()) != MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER) {
                throw new FunksjonellException("Medfolgende familie som ikke er omfattet av norsk trygd: " + ikkeOmfattetFamilie.getUuid() + " er lagret med feil relasjonsrolle.");
            }
        }
    }

    public void lagreOmfattetFamilieSomAvklartfakta(long behandlingID, Avklartefaktatyper avklartefaktatype, Set<OmfattetFamilie> omfattetFamilie) throws IkkeFunnetException {
        for(OmfattetFamilie omfattet : omfattetFamilie) {
            avklartefaktaService.leggTilAvklarteFakta(
                behandlingID,
                avklartefaktatype,
                avklartefaktatype.getKode(),
                omfattet.getUuid(),
                "TRUE"
            );
        }
    }

    public void lagreIkkeOmfattetFamilieSomAvklartfakta(long behandlingID, Avklartefaktatyper avklartefaktatype, Set<IkkeOmfattetFamilie> ikkeOmfattetFamilie) throws IkkeFunnetException {
        for(IkkeOmfattetFamilie ikkeOmfattet : ikkeOmfattetFamilie) {
            avklartefaktaService.leggTilAvklarteFakta(
                behandlingID,
                avklartefaktatype,
                avklartefaktatype.getKode(),
                ikkeOmfattet.getUuid(),
                "FALSE",
                ikkeOmfattet.getBegrunnelseFritekst(),
                ikkeOmfattet.getBegrunnelse().getKode()
            );
        }
    }
}
