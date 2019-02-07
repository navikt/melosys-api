package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.SedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.saksflyt.agent.iv.validering.SendBrevValidator.validerLovvalgsperiode;
import static no.nav.melosys.saksflyt.agent.iv.validering.SendSedValidator.sedSkalSendes;


@Component
public class IverksettVedtakSendSed extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(IverksettVedtakSendSed.class);

    private final BehandlingRepository behandlingRepository;
    private final SedService sedService;
    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public IverksettVedtakSendSed(BehandlingRepository behandlingRepository, SedService sedService, BehandlingsresultatService behandlingsresultatService) {
        this.behandlingRepository = behandlingRepository;
        this.sedService = sedService;
        this.behandlingsresultatService = behandlingsresultatService;
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

        try {
            Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
            if (sedSkalSendes(behandlingsresultat.getType(), validerLovvalgsperiode(behandlingsresultat.getLovvalgsperioder()))) {
                sedService.opprettOgSendSed(behandling, behandlingsresultat);
            }
            prosessinstans.setSteg(ProsessSteg.IV_AVSLUTT_BEHANDLING);
        } catch (MelosysException ex) {
            log.error("Kan ikke opprette og sende sed for behandling {}", behandling.getId(), ex);
            prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
        }
    }
}
