package no.nav.melosys.statistikk.utstedt_a1.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.hendelser.FeiletHendelse;
import no.nav.melosys.service.hendelser.VedtakMetadataLagretHendelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.UtstedtA1Producer;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.A1TypeUtstedelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Lovvalgsbestemmelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Periode;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class UtstedtA1Service {
    private static final Logger log = LoggerFactory.getLogger(UtstedtA1Service.class);

    private final UtstedtA1Producer utstedtA1Producer;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final LandvelgerService landvelgerService;
    private final Unleash unleash;
    private final ApplicationEventMulticaster melosysHendelseMulticaster;

    @Autowired
    public UtstedtA1Service(UtstedtA1Producer utstedtA1Producer,
                            BehandlingService behandlingService,
                            BehandlingsresultatService behandlingsresultatService,
                            LandvelgerService landvelgerService,
                            Unleash unleash,
                            ApplicationEventMulticaster melosysHendelseMulticaster) {
        this.utstedtA1Producer = utstedtA1Producer;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.landvelgerService = landvelgerService;
        this.unleash = unleash;
        this.melosysHendelseMulticaster = melosysHendelseMulticaster;
    }

    @Async
    @TransactionalEventListener
    @SuppressWarnings("unused")
    public void handterA1Bestilt(VedtakMetadataLagretHendelse vedtakMetadataLagretHendelse) {
        try {
            log.info("Mottatt hendelse om vedtak metadata lagret");
            sendMeldingOmUtstedtA1(vedtakMetadataLagretHendelse.getBehandlingID());
        } catch (Exception e) {
            melosysHendelseMulticaster.multicastEvent(new FeiletHendelse(this, e, vedtakMetadataLagretHendelse));
        }
    }

    public void sendMeldingOmUtstedtA1(Long behandlingID) throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultatMedSaksbehandling(behandlingID);
        sendMeldingOmUtstedtA1(behandling, behandlingsresultat);
    }

    public void sendMeldingOmUtstedtA1(Behandling behandling, Behandlingsresultat behandlingsresultat) throws TekniskException, FunksjonellException {
        if (!unleash.isEnabled("melosys.statistikkA1")) {
            return;
        }

        validerBehandling(behandling, behandlingsresultat);

        final UtstedtA1Melding melding = lagMelding(behandling, behandlingsresultat);
        utstedtA1Producer.produserMelding(melding);
    }

    private void validerBehandling(Behandling behandling, Behandlingsresultat behandlingsresultat) throws FunksjonellException {
        if (behandlingsresultat.erAvslag()) {
            throw new FunksjonellException(String.format("Behandling %s er avslått. Ingen melding om utstedt A1 blir sendt", behandling.getId()));
        }

        if (behandling.erAktiv()) {
            throw new FunksjonellException(String.format("Behandling %s er aktiv. Ingen melding om utstedt A1 blir sendt", behandling.getId()));
        }
    }

    private UtstedtA1Melding lagMelding(Behandling behandling, Behandlingsresultat behandlingsresultat) throws TekniskException, FunksjonellException {
        final Fagsak fagsak = behandling.getFagsak();

        final String saksnummer = fagsak.getSaksnummer();
        final Long behandlingID = behandling.getId();
        final String aktørID = fagsak.hentBruker().getAktørId();

        final Lovvalgsperiode lovvalgsperiode = behandlingsresultat.hentValidertLovvalgsperiode();
        final Lovvalgsbestemmelse artikkel = Lovvalgsbestemmelse.av(lovvalgsperiode.getBestemmelse());
        final String utsendtTilLand = hentUtsendtTilLand(behandlingID, lovvalgsperiode.erArtikkel13());

        final VedtakMetadata vedtakMetadata = behandlingsresultat.getVedtakMetadata();
        final LocalDate vedtaksdato = LocalDate.ofInstant(vedtakMetadata.getVedtaksdato(), ZoneId.systemDefault());

        return new UtstedtA1Melding(
            saksnummer,
            behandlingID,
            aktørID,
            artikkel,
            Periode.av(lovvalgsperiode),
            utsendtTilLand,
            vedtaksdato,
            A1TypeUtstedelse.av(vedtakMetadata.getVedtakstype())
        );
    }

    private String hentUtsendtTilLand(Long behandlingID, boolean erArt13) throws FunksjonellException {
        if (erArt13) {
            return null;
        }

        Collection<Landkoder> mottakere = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        if (mottakere.size() > 1) {
            throw new FunksjonellException("Finner flere enn én mottaker av A1 for behandling " + behandlingID);
        }

        return mottakere.stream().findFirst()
            .orElseThrow(() -> new FunksjonellException("Finner ingen gyldige mottakere av A1 for behandling " + behandlingID))
            .getKode();
    }
}
