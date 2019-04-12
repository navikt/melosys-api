package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.IV_AVSLUTT_BEHANDLING;
import static no.nav.melosys.domain.ProsessSteg.IV_STATUS_BEH_AVSL;

/**
 * Avslutter en fagsak og Behandling i Melosys.
 *
 * Transisjoner:
 * ProsessType.IVERKSETT_VEDTAK
 *  IV_AVSLUTT_BEHANDLING -> IV_STATUS_BEH_AVSL eller FEILET_MASKINELT hvis feil
 */
@Component
public class AvsluttFagsakOgBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttFagsakOgBehandling.class);

    private final BehandlingRepository behandlingRepository;
    private final FagsakRepository fagsakRepository;

    @Autowired
    public AvsluttFagsakOgBehandling(BehandlingRepository behandlingRepository,
                                     FagsakRepository fagsakRepository) {
        log.info("IverksetteVedtakAvsluttBehandling initialisert");
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_AVSLUTT_BEHANDLING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();

        Fagsak fagsak = behandling.getFagsak();
        fagsak.setStatus(Saksstatuser.LOVVALG_AVKLART);
        fagsakRepository.save(fagsak);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandlingRepository.save(behandling);

        prosessinstans.setSteg(IV_STATUS_BEH_AVSL);
    }
}
