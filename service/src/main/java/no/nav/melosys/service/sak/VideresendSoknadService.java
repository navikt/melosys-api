package no.nav.melosys.service.sak;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideresendSoknadService {

    private static final Logger log = LoggerFactory.getLogger(VideresendSoknadService.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final ProsessinstansService prosessinstansService;
    private final LandvelgerService landvelgerService;
    private final EessiService eessiService;
    private final OppgaveService oppgaveService;

    public VideresendSoknadService(FagsakService fagsakService, BehandlingService behandlingService, ProsessinstansService prosessinstansService, LandvelgerService landvelgerService, EessiService eessiService, OppgaveService oppgaveService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.prosessinstansService = prosessinstansService;
        this.landvelgerService = landvelgerService;
        this.eessiService = eessiService;
        this.oppgaveService = oppgaveService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void henleggOgVideresend(String saksnummer, String mottakerinstitusjon) throws MelosysException {
        final Fagsak fagsak = fagsakService.hentFagsak(saksnummer);

        final long behandlingId = fagsak.getAktivBehandling().getId();
        final Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingId);
        log.info("Videresender søknad for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingId);

        final Landkoder bostedsland = landvelgerService.hentBostedsland(behandling);
        validerBehandlingOgBosted(behandling, bostedsland);

        behandlingService.avsluttBehandling(behandlingId);
        fagsakService.oppdaterStatus(fagsak, Saksstatuser.VIDERESENDT);

        final Set<String> avklarteEessiMottakere = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
            mottakerinstitusjon != null ? Set.of(mottakerinstitusjon) : Collections.emptySet(),
            List.of(bostedsland),
            BucType.LA_BUC_03
        );

        prosessinstansService.opprettProsessinstansVideresendSoknad(behandling, avklarteEessiMottakere.stream().findFirst().orElse(null));
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private void validerBehandlingOgBosted(Behandling behandling, Landkoder bostedsland) throws FunksjonellException {
        if (!behandling.erBehandlingAvSøknad()) {
            throw new FunksjonellException("Behandling " + behandling.getId() + " er ikke behandling av en søknad!");
        }
        if (bostedsland == null) {
            throw new FunksjonellException("Bostedsland ikke avklart for behandling " + behandling.getId());
        }
        if (bostedsland == Landkoder.NO) {
            throw new FunksjonellException("Kan ikke videresende søknad tilknyttet behandling " + behandling.getId() + " til Norge");
        }
    }

}
