package no.nav.melosys.saksflyt.steg.aou.ut;

import java.time.LocalDateTime;
import java.time.ZoneId;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktSendUtland;
import no.nav.melosys.service.BehandlingService;
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
public class SendUtland extends AbstraktSendUtland {
    private static final Logger log = LoggerFactory.getLogger(SendUtland.class);

    private final BehandlingService behandlingService;

    private static final ZoneId TIME_ZONE_ID = ZoneId.systemDefault();
    private static final int SVARFRIST_MÅNEDER = 2;

    @Autowired
    public SendUtland(EessiService eessiService, BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService) {
        super(eessiService, behandlingsresultatService);
        this.behandlingService = behandlingService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_SEND_SED;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        try {
            super.utfør(prosessinstans);
            prosessinstans.setSteg(ProsessSteg.AOU_OPPDATER_OPPGAVE);

            Behandling behandling = prosessinstans.getBehandling();
            LocalDateTime svarFristDato = LocalDateTime.now().plusMonths(SVARFRIST_MÅNEDER);
            behandling.setDokumentasjonSvarfristDato(svarFristDato.atZone(TIME_ZONE_ID).toInstant());
            behandlingService.lagre(behandling);
        } catch (Exception ex) {
            prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
            log.error("Kan ikke opprette og sende sed for behandling {}", prosessinstans.getBehandling().getId(), ex);
        }
    }

    @Override
    protected boolean skalSendeSed(Behandlingsresultat behandlingsresultat) {
        Anmodningsperiode anmodningsperiode = behandlingsresultat.hentValidertAnmodningsperiode();
        return behandlingsresultat.getType() == Behandlingsresultattyper.ANMODNING_OM_UNNTAK
            && (anmodningsperiode.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
            || anmodningsperiode.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2);
    }
}
