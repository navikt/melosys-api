package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.steg.AbstraktSendSed;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class SendVedtakUtland extends AbstraktSendSed {
    private static final Logger log = LoggerFactory.getLogger(SendVedtakUtland.class);

    @Autowired
    public SendVedtakUtland(BehandlingRepository behandlingRepository, EessiService eessiService, BehandlingsresultatService behandlingsresultatService) {
        super(behandlingRepository, eessiService, behandlingsresultatService);
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.IV_SEND_SED;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException {
        try {
            super.utfør(prosessinstans);
            
            if (erArtikkel11(prosessinstans)) {
                prosessinstans.setSteg(ProsessSteg.IV_AVSLUTT_BEHANDLING);
            } else {
                prosessinstans.setSteg(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE);
            }
        } catch (Exception ex) {
            log.error("Kan ikke opprette og sende sed for behandling {}", prosessinstans.getBehandling().getId(), ex);
            prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
        }
    }

    @Override
    protected boolean skalSendeSed(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.erInnvilgelse() &&
            behandlingsresultat.hentValidertLovvalgsperiode().getBestemmelse() != Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1;
    }
    
    private boolean erArtikkel11(Prosessinstans prosessinstans) throws IkkeFunnetException {
        return behandlingsresultatService.hentBehandlingsresultat(prosessinstans.getBehandling().getId())
                .hentValidertLovvalgsperiode()
                .erArtikkel11();
    }
}
