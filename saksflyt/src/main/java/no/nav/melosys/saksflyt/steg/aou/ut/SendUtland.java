package no.nav.melosys.saksflyt.steg.aou.ut;

import java.time.LocalDateTime;
import java.time.ZoneId;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.AbstraktSendUtland;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.MYNDIGHET;

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
    public SendUtland(EessiService eessiService,
                      BrevBestiller brevBestiller,
                      BehandlingService behandlingService,
                      BehandlingsresultatService behandlingsresultatService,
                      LandvelgerService landvelgerService) {
        super(eessiService, brevBestiller, behandlingsresultatService, landvelgerService);
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
    protected Brevbestilling lagBrevBestilling(Prosessinstans prosessinstans) throws IkkeFunnetException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        return new Brevbestilling.Builder().medDokumentType(Produserbaredokumenter.ANMODNING_UNNTAK)
            .medAvsender(hentSaksbehandler(prosessinstans))
            .medMottakere(Mottaker.av(MYNDIGHET))
            .medBehandling(behandling).build();
    }

    @Override
    protected boolean skalSendesUtland(Behandlingsresultat behandlingsresultat) {
        Anmodningsperiode anmodningsperiode = behandlingsresultat.hentValidertAnmodningsperiode();
        return behandlingsresultat.erAnmodningOmUnntak()
            && (anmodningsperiode.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
            || anmodningsperiode.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2);
    }
}
