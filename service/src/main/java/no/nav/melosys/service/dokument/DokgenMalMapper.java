package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.dokgen.dto.*;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
public class DokgenMalMapper {

    private final KodeverkService kodeverkService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final EregFasade eregFasade;

    @Autowired
    public DokgenMalMapper(KodeverkService kodeverkService,
                           BehandlingsresultatService behandlingsresultatService,
                           @Qualifier("system") EregFasade eregFasade) {
        this.kodeverkService = kodeverkService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.eregFasade = eregFasade;
    }

    public DokgenDto mapBehandling(DokgenBrevbestilling brevbestilling) throws TekniskException, FunksjonellException {
        DokgenDto dto;
        switch (brevbestilling.getProduserbartdokument()) {
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID:
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD:
                dto = SaksbehandlingstidSoknad.av(brevbestilling);
                break;
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE:
                dto = SaksbehandlingstidKlage.av(brevbestilling);
                break;
            case MANGELBREV_BRUKER:
                dto = MangelbrevBruker.av(((MangelbrevBrevbestilling) brevbestilling).toBuilder()
                    .medVedtaksdato(hentVedtaksdato(brevbestilling.getBehandling().getId()))
                    .build());
                break;
            case MANGELBREV_ARBEIDSGIVER:
                MangelbrevBrevbestilling bestilling = (MangelbrevBrevbestilling) brevbestilling;
                dto = MangelbrevArbeidsgiver.av(bestilling.toBuilder()
                    .medVedtaksdato(hentVedtaksdato(brevbestilling.getBehandling().getId()))
                    .medFullmektigNavn(hentFullmektigNavn(brevbestilling.getBehandling().getFagsak()))
                    .build());
                break;
            default:
                throw new FunksjonellException(format("ProduserbartDokument %s er ikke støttet av melosys-dokgen", brevbestilling.getProduserbartdokument()));
        }

        dto.setPoststed(hentPoststed(dto.getPostnr()));
        return dto;
    }

    private String hentPoststed(String postnr) {
        return kodeverkService.dekod(FellesKodeverk.POSTNUMMER, postnr, LocalDate.now());
    }

    private Instant hentVedtaksdato(Long behandlingId) throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);
        return (behandlingsresultat != null && behandlingsresultat.harVedtak()) ?
            behandlingsresultat.getVedtakMetadata().getVedtaksdato() : null;
    }

    private String hentFullmektigNavn(Fagsak fagsak) throws IkkeFunnetException, IntegrasjonException {
        Optional<Aktoer> representant = fagsak.hentRepresentant(Representerer.BRUKER);
        if (representant.isPresent()) {
            return eregFasade.hentOrganisasjonNavn(representant.get().getOrgnr());
        }
        return null;
    }
}
