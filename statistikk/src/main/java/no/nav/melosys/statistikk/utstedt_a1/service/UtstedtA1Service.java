package no.nav.melosys.statistikk.utstedt_a1.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.UtstedtA1Producer;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.A1TypeUtstedelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Lovvalgsbestemmelse;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Periode;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UtstedtA1Service {
    private final UtstedtA1Producer utstedtA1Producer;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final BrevmottakerService brevmottakerService;

    @Autowired
    public UtstedtA1Service(UtstedtA1Producer utstedtA1Producer,
                            BehandlingService behandlingService,
                            BehandlingsresultatService behandlingsresultatService,
                            BrevmottakerService brevmottakerService) {
        this.utstedtA1Producer = utstedtA1Producer;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.brevmottakerService = brevmottakerService;
    }

    public UtstedtA1Melding sendMeldingOmUtstedtA1(Long behandlingID) throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        return sendMeldingOmUtstedtA1(behandling, behandlingsresultat);
    }

    public UtstedtA1Melding sendMeldingOmUtstedtA1(Behandling behandling, Behandlingsresultat behandlingsresultat) throws TekniskException, FunksjonellException {
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
        final String utsendtTilLand = hentUtsendtTilLand(behandling, lovvalgsperiode.erArtikkel13());

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

    private String hentUtsendtTilLand(Behandling behandling, boolean erArt13) throws FunksjonellException, TekniskException {
        List<Aktoer> mottakere = brevmottakerService.avklarMottakere(Produserbaredokumenter.ATTEST_A1, Mottaker.av(Aktoersroller.MYNDIGHET), behandling);
        if (erArt13) {
            return null;
        }

        if( mottakere.size() > 1) {
            throw new FunksjonellException("Finner flere enn én mottaker av A1 for behandling " + behandling.getId());
        }

        return mottakere.stream().findFirst()
            .orElseThrow(() -> new FunksjonellException("Finner ingen gyldige mottakere av A1 for behandling " + behandling.getId()))
            .hentMyndighetLandkode().getKode();
    }
}
