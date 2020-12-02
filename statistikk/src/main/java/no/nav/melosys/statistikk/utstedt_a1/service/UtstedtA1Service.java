package no.nav.melosys.statistikk.utstedt_a1.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.hendelser.A1BestiltHendelse;
import no.nav.melosys.service.hendelser.FeiletHendelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.UtstedtA1Producer;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.A1TypeUtstedelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Lovvalgsbestemmelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Periode;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class UtstedtA1Service {
    private static final Logger log = LoggerFactory.getLogger(UtstedtA1Service.class);

    private final UtstedtA1Producer utstedtA1Producer;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final LandvelgerService landvelgerService;
    private final ApplicationEventMulticaster melosysHendelseMulticaster;

    @Autowired
    public UtstedtA1Service(UtstedtA1Producer utstedtA1Producer,
                            BehandlingService behandlingService,
                            BehandlingsresultatService behandlingsresultatService,
                            LandvelgerService landvelgerService,
                            ApplicationEventMulticaster melosysHendelseMulticaster) {
        this.utstedtA1Producer = utstedtA1Producer;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.landvelgerService = landvelgerService;
        this.melosysHendelseMulticaster = melosysHendelseMulticaster;
    }

    @EventListener
    @SuppressWarnings("unused")
    public void håndterA1Bestilt(A1BestiltHendelse a1BestiltHendelse) {
        try {
            log.info("Mottatt hendelse om bestilt A1");
            sendMeldingOmUtstedtA1(a1BestiltHendelse.getBehandlingID());
        } catch (TekniskException | FunksjonellException e) {
            FeiletHendelse feiletHendelse = new FeiletHendelse(this, e, a1BestiltHendelse);
            melosysHendelseMulticaster.multicastEvent(feiletHendelse);
        }
    }

    public UtstedtA1Melding sendMeldingOmUtstedtA1(Long behandlingID) throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        return sendMeldingOmUtstedtA1(behandling, behandlingsresultat);
    }

    public UtstedtA1Melding sendMeldingOmUtstedtA1(Behandling behandling, Behandlingsresultat behandlingsresultat) throws TekniskException, FunksjonellException {
        if (behandlingsresultat.erAvslag()) {
            log.info("Behandling {} er avslått. Ingen melding om utstedt A1 blir sendt", behandling.getId());
            return null;
        }

        final UtstedtA1Melding melding = lagMelding(behandling, behandlingsresultat);
        return utstedtA1Producer.produserMelding(melding);
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
