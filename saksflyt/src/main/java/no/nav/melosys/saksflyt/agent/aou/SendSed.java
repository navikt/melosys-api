package no.nav.melosys.saksflyt.agent.aou;

import java.time.LocalDateTime;
import java.time.ZoneId;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.AbstraktSendSed;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Sender elektronisk sed til mottakerinstitusjon
 * <p>
 * Transisjoner:
 * ProsessType.ANMODNING_OM_UNNTAK
 *  AOU_SEND_SED -> FERDIG eller FEILET_MASKINELT hvis feil
 */
@Component
public class SendSed extends AbstraktSendSed {

    private static final Logger log = LoggerFactory.getLogger(SendSed.class);

    private static final ZoneId TIME_ZONE_ID = ZoneId.systemDefault();
    private static final int SVARFRIST_MÅNEDER = 2;

    @Autowired
    public SendSed(BehandlingRepository behandlingRepository, EessiService eessiService, BehandlingsresultatService behandlingsresultatService) {
        super(behandlingRepository, eessiService, behandlingsresultatService);
        log.info("IverksettVedtakSendSed initialisert");
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_SEND_SED;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        try {
            super.utfør(prosessinstans);
            prosessinstans.setSteg(ProsessSteg.FERDIG);

            Behandling behandling = prosessinstans.getBehandling();
            LocalDateTime svarFristDato = LocalDateTime.now().plusMonths(SVARFRIST_MÅNEDER);
            behandling.setDokumentasjonSvarfristDato(svarFristDato.atZone(TIME_ZONE_ID).toInstant());
            behandlingRepository.save(behandling);
        } catch (Exception ex) {
            prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
            log.error("Kan ikke opprette og sende sed for behandling {}", prosessinstans.getBehandling().getId(), ex);
        }
    }

    @Override
    protected boolean skalSendeSed(Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat.getLovvalgsperioder().size() > 1) {
            throw new UnsupportedOperationException("Flere enn en"
                + " lovvalgsperiode er ikke støttet i første leveranse");
        }
        Lovvalgsperiode lovvalgsperiode = behandlingsresultat.getLovvalgsperioder().iterator().next();
        return behandlingsresultat.getType() == Behandlingsresultattyper.ANMODNING_OM_UNNTAK
            && (lovvalgsperiode.getBestemmelse() == LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1
            || lovvalgsperiode.getBestemmelse() == LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_2);
    }
}
