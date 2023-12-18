package no.nav.melosys.service.brev.bestilling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

@Component
public class HentMuligeProduserbaredokumenterService {

    private final BehandlingService behandlingService;

    public HentMuligeProduserbaredokumenterService(BehandlingService behandlingService) {
        this.behandlingService = behandlingService;
    }

    @Transactional
    public List<Produserbaredokumenter> hentMuligeProduserbaredokumenter(long behandlingId, Mottakerroller mottakerroller) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        Fagsak fagsak = behandling.getFagsak();

        if (behandling.erInaktiv()) {
            return emptyList();
        }

        return switch (mottakerroller) {
            case BRUKER -> {
                List<Produserbaredokumenter> brevmaler = new ArrayList<>();
                if (fagsak.getTema() == Sakstemaer.MEDLEMSKAP_LOVVALG && (behandling.erFørstegangsvurdering() || behandling.erNyVurdering())) {
                    brevmaler.add(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
                }
                if (!behandling.erManglendeInnbetalingTrygdeavgift()) {
                    brevmaler.add(MANGELBREV_BRUKER);
                }
                brevmaler.add(GENERELT_FRITEKSTBREV_BRUKER);
                yield brevmaler;
            }
            case VIRKSOMHET -> Collections.singletonList(GENERELT_FRITEKSTBREV_VIRKSOMHET);
            case ARBEIDSGIVER -> behandling.erManglendeInnbetalingTrygdeavgift()
                ? List.of(GENERELT_FRITEKSTBREV_ARBEIDSGIVER)
                : List.of(MANGELBREV_ARBEIDSGIVER, GENERELT_FRITEKSTBREV_ARBEIDSGIVER);
            case ANNEN_ORGANISASJON -> (fagsak.getHovedpartRolle() == Aktoersroller.VIRKSOMHET || behandling.erManglendeInnbetalingTrygdeavgift())
                ? Collections.singletonList(GENERELT_FRITEKSTBREV_VIRKSOMHET)
                : List.of(MANGELBREV_ARBEIDSGIVER, GENERELT_FRITEKSTBREV_ARBEIDSGIVER);
            case UTENLANDSK_TRYGDEMYNDIGHET -> Collections.singletonList(UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV);
            case NORSK_MYNDIGHET -> Collections.singletonList(FRITEKSTBREV);
            default -> throw new FunksjonellException("Mottakerrollen " + mottakerroller + " kan ikke sende brev gjennom brevmenyen");
        };
    }
}
