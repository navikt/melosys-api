package no.nav.melosys.statistikk.utstedt_a1.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.UtstedtA1Producer;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.A1TypeUtstedelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Lovvalgsbestemmelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Periode;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UtstedtA1Service {
    private static final Logger log = LoggerFactory.getLogger(UtstedtA1Service.class);

    private final UtstedtA1Producer utstedtA1Producer;
    private final BehandlingsresultatService behandlingsresultatService;
    private final LandvelgerService landvelgerService;

    @Autowired
    public UtstedtA1Service(UtstedtA1Producer utstedtA1Producer,
                            BehandlingsresultatService behandlingsresultatService,
                            LandvelgerService landvelgerService) {
        this.utstedtA1Producer = utstedtA1Producer;
        this.behandlingsresultatService = behandlingsresultatService;
        this.landvelgerService = landvelgerService;
    }

    @Transactional(readOnly = true)
    public void sendMeldingOmUtstedtA1(Long behandlingID) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        if (behandlingsresultat.a1Produseres()) {
            log.info("Produserer melding om utstedt A1 for behandling {}", behandlingID);
            sendMeldingOmUtstedtA1(behandlingsresultat);
        } else {
            log.info("Melding om utstedt A1 blir ikke sendt for behandling {}", behandlingID);
        }
    }

    private void sendMeldingOmUtstedtA1(Behandlingsresultat behandlingsresultat) {
        final UtstedtA1Melding melding = lagMelding(behandlingsresultat);
        utstedtA1Producer.produserMelding(melding);
    }

    private UtstedtA1Melding lagMelding(Behandlingsresultat behandlingsresultat) {
        final Behandling behandling = behandlingsresultat.getBehandling();
        final Fagsak fagsak = behandling.getFagsak();

        final String saksnummer = fagsak.getSaksnummer();
        final Long behandlingID = behandling.getId();
        final String aktørID = fagsak.hentBrukerID();

        final Lovvalgsperiode lovvalgsperiode = behandlingsresultat.hentValidertLovvalgsperiode();
        final Lovvalgsbestemmelse artikkel = Lovvalgsbestemmelse.av(lovvalgsperiode.getBestemmelse());
        final String utsendtTilLand = hentUtsendtTilLand(behandlingID, lovvalgsperiode);

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

    private String hentUtsendtTilLand(Long behandlingID, Lovvalgsperiode lovvalgsperiode) {
        if (landSkalIkkeSendes(lovvalgsperiode)) {
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

    private boolean landSkalIkkeSendes(Lovvalgsperiode lovvalgsperiode) {
        return lovvalgsperiode.erArtikkel13()
            || (lovvalgsperiode.erArtikkel11() && !lovvalgsperiode.erArtikkel12());
    }
}
