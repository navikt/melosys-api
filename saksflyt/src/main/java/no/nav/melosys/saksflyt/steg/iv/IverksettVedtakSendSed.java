package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.steg.AbstraktSendSed;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import static no.nav.melosys.saksflyt.steg.iv.validering.SendBrevValidator.validerLovvalgsperiode;
import static no.nav.melosys.saksflyt.steg.iv.validering.SendSedValidator.sedSkalSendes;


@Component
public class IverksettVedtakSendSed extends AbstraktSendSed {

    private static final Logger log = LoggerFactory.getLogger(IverksettVedtakSendSed.class);

    @Autowired
    public IverksettVedtakSendSed(BehandlingRepository behandlingRepository, EessiService eessiService, BehandlingsresultatService behandlingsresultatService) {
        super(behandlingRepository, eessiService, behandlingsresultatService);
        log.info("IverksettVedtakSendSed initialisert");
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.IV_SEND_SED;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException {
        try {
            super.utfør(prosessinstans);
            prosessinstans.setSteg(ProsessSteg.IV_AVSLUTT_BEHANDLING);
        } catch (Exception ex) {
            log.error("Kan ikke opprette og sende sed for behandling {}", prosessinstans.getBehandling().getId(), ex);
            prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
        }
    }

    @Override
    protected boolean skalSendeSed(Behandlingsresultat behandlingsresultat) {
        return sedSkalSendes(behandlingsresultat.getType(), validerLovvalgsperiode(behandlingsresultat.getLovvalgsperioder()));
    }
}
