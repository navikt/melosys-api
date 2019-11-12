package no.nav.melosys.service.unntak;

import java.util.Optional;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AnmodningUnntakService {
    private static final Logger log = LoggerFactory.getLogger(AnmodningUnntakService.class);

    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final LandvelgerService landvelgerService;
    private final EessiService eessiService;

    public AnmodningUnntakService(BehandlingService behandlingService,
                                  OppgaveService oppgaveService,
                                  ProsessinstansService prosessinstansService,
                                  AnmodningsperiodeService anmodningsperiodeService,
                                  LovvalgsperiodeService lovvalgsperiodeService, LandvelgerService landvelgerService, EessiService eessiService) {
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.landvelgerService = landvelgerService;
        this.eessiService = eessiService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void anmodningOmUnntak(long behandlingID, String mottakerInstitusjon) throws MelosysException {
        validerMottakerInstitusjon(behandlingID, mottakerInstitusjon);

        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        log.info("Anmodning om unntak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        prosessinstansService.opprettProsessinstansAnmodningOmUnntak(behandling, mottakerInstitusjon);
        oppgaveService.leggTilbakeOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private void validerMottakerInstitusjon(long behandlingID, String mottakerInstitusjon) throws MelosysException {
        String landkode = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID).stream().findFirst()
            .orElseThrow(() -> new FunksjonellException("Finner ikke utenlandsk myndighet for behandling " + behandlingID)).getKode();
        String bucType = BucType.LA_BUC_01.name();
        if (eessiService.landErEessiReady(bucType, landkode)) {
            if (StringUtils.isEmpty(mottakerInstitusjon)) {
                throw new FunksjonellException(String.format("%s er EESSI-ready, men mottakerinstitusjon er ikke definert", landkode));
            } else if (!eessiService.erGyldigInstitusjonForLand(bucType, landkode, mottakerInstitusjon)) {
                throw new FunksjonellException(String.format("MottakerID %s er ugyldig for land %s", mottakerInstitusjon, landkode));
            }
        }
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void anmodningOmUnntakSvar(long behandlingID) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        validerBehandlingstypeUnntak(behandling);
        validerSvar(behandling);
        prosessinstansService.opprettProsessinstansAnmodningOmUnntakMottakSvar(behandling);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private static void validerBehandlingstypeUnntak(Behandling behandling) throws FunksjonellException {
        if (behandling.getType() != Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL) {
            throw new FunksjonellException("Behandling er ikke av type ANMODNING_OM_UNNTAK_HOVEDREGEL");
        } else if (behandling.getStatus() == Behandlingsstatus.AVSLUTTET) {
            throw new FunksjonellException("Behandlingen er avsluttet");
        }
    }

    private void validerSvar(Behandling behandling) throws FunksjonellException {
        Optional<AnmodningsperiodeSvar> anmodningsperiodeSvar = anmodningsperiodeService
            .hentAnmodningsperiodeSvarForBehandling(behandling.getId()).stream().findFirst();
        if (!anmodningsperiodeSvar.isPresent()) {
            throw new FunksjonellException("Finner ingen AnmodningsperiodeSvar for behandling " + behandling.getId());
        }

        if (lovvalgsperiodeService.hentLovvalgsperioder(behandling.getId()).isEmpty()) {
            throw new FunksjonellException("Finner ingen Lovvalgsperioder for behandling " + behandling.getId());
        }

        if (anmodningsperiodeSvar.get().erAvslag()) {
            validerFritekstLengde(anmodningsperiodeSvar.get());
        }
    }

    private void validerFritekstLengde(AnmodningsperiodeSvar anmodningsperiodeSvar) throws FunksjonellException {
        if (anmodningsperiodeSvar.getBegrunnelseFritekst() != null && anmodningsperiodeSvar.getBegrunnelseFritekst().length() > 255) {
            throw new FunksjonellException("Kan ikke ha fritekst lengre enn 255 for avslag på anmodning om unntak");
        }
    }
}
