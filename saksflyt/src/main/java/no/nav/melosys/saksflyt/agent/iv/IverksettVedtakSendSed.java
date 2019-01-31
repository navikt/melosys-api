package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.dokument.sed.SedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IverksettVedtakSendSed extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(IverksettVedtakSendSed.class);

    private final BehandlingsresultatRepository behandlingsResultatRepo;
    private final BehandlingRepository behandlingRepository;
    private final SedService sedService;

    @Autowired
    public IverksettVedtakSendSed(BehandlingsresultatRepository behandlingsResultatRepo, BehandlingRepository behandlingRepository, SedService sedService) {
        this.behandlingsResultatRepo = behandlingsResultatRepo;
        this.behandlingRepository = behandlingRepository;
        this.sedService = sedService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.IV_SEND_SED;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException {

        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(prosessinstans.getBehandling().getId());
        if (behandling == null) {
            throw new TekniskException(String.format("Finner ikke behandlingen %s.", prosessinstans.getBehandling().getId()));
        }
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandling);

        try {
            if (skalSendeSed(behandlingsresultat)) {
                sedService.opprettOgSendSed(behandling, behandlingsresultat);
            }
            prosessinstans.setSteg(ProsessSteg.IV_AVSLUTT_BEHANDLING);
        } catch (MelosysException ex) {
            log.error("Kan ikke opprette og sende sed for behandling {}", behandling.getId());
            prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
        }
    }

    private boolean skalSendeSed(Behandlingsresultat behandlingsresultat) {
        //OBS: Denne sjekken gjelder kun for LA_BUC_04/A009 per nå.
        return behandlingsresultat.getLovvalgsperioder().stream().anyMatch(lovvalgsperiode ->
            lovvalgsperiode.getBestemmelse() == LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1
            || lovvalgsperiode.getBestemmelse() == LovvalgBestemmelse_883_2004.FO_883_2004_ART12_2);
    }

    private Behandlingsresultat hentBehandlingsresultat(Behandling behandling) throws TekniskException {
        Optional<Behandlingsresultat> resultat = behandlingsResultatRepo.findById(behandling.getId());
        return resultat.orElseThrow(() -> new TekniskException("Kan ikke finne behandlingsresultat for behandling: " + behandling.getId()));
    }
}
