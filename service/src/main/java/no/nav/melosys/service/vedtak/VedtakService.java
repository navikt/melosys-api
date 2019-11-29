package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.util.LovvalgBestemmelseUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;

@Service
public class VedtakService {
    private static final Logger log = LoggerFactory.getLogger(VedtakService.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;
    private final EessiService eessiService;
    private final LandvelgerService landvelgerService;

    @Autowired
    public VedtakService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService,
                         OppgaveService oppgaveService, ProsessinstansService prosessinstansService,
                         EessiService eessiService, LandvelgerService landvelgerService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.eessiService = eessiService;
        this.landvelgerService = landvelgerService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultattype) throws MelosysException {
        fattVedtak(behandlingID, behandlingsresultattype, null);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultatType, String mottakerInstitusjon) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        log.info("Fatter vedtak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        if (skalSendesSed(behandlingsresultat, behandlingsresultatType)) {
            validerMottakerInstitusjon(behandling, behandlingsresultat, mottakerInstitusjon);
        }
        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        behandlingService.lagre(behandling);
        prosessinstansService.opprettProsessinstansIverksettVedtak(behandling, behandlingsresultatType, mottakerInstitusjon);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private boolean skalSendesSed(Behandlingsresultat behandlingsresultat, Behandlingsresultattyper behandlingsresultattype) {
        if (behandlingsresultattype == Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL || behandlingsresultat.erAvslag()) {
            return false;
        }
        return !behandlingsresultat.erArt16EtterUtlandMedRegistrertSvar();
    }

    private void validerMottakerInstitusjon(Behandling behandling, Behandlingsresultat behandlingsresultat, String mottakerInstitusjon) throws MelosysException {

        Collection<Landkoder> landkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling.getId());
        String landkode = landkoder.iterator().next().getKode();
        String bucType = avklarBucType(behandlingsresultat);
        boolean landErEessiReady = eessiService.landErEessiReady(bucType, landkode);
        if (landErEessiReady) {
            if (StringUtils.isEmpty(mottakerInstitusjon)) {
                throw new FunksjonellException(String.format("Kan ikke fatte vedtak: %s er EESSI-ready, men mottaker er ikke satt", landkode));
            } else if (!eessiService.erGyldigInstitusjonForLand(bucType, landkode, mottakerInstitusjon)) {
                throw new FunksjonellException(String.format("MottakerID %s er ugyldig for land %s", mottakerInstitusjon, landkode));
            }
        }
    }

    private String avklarBucType(Behandlingsresultat behandlingsresultat) {
        return LovvalgBestemmelseUtils.hentBucTypeFraBestemmelse(
            behandlingsresultat.hentValidertMedlemskapsperiode().getBestemmelse()
        ).name();
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void endreVedtak(Long behandlingID, Endretperiode endretperiode) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        log.info("Endrer vedtak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        prosessinstansService.opprettProsessinstansForkortPeriode(behandling, endretperiode);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

}