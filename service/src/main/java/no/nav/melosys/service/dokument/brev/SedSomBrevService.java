package no.nav.melosys.service.dokument.brev;

import java.util.Collections;
import java.util.List;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.FysiskDokument;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.TemaFactory.fraBehandlingstema;
import static no.nav.melosys.service.oppgave.OppgaveFactory.utledTema;

@Service
public class SedSomBrevService {
    private final EessiService eessiService;
    private final JoarkFasade joarkFasade;
    private final PersondataFasade persondataFasade;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final Unleash unleash;


    public SedSomBrevService(EessiService eessiService,
                             JoarkFasade joarkFasade,
                             PersondataFasade persondataFasade,
                             UtenlandskMyndighetService utenlandskMyndighetService, Unleash unleash) {
        this.eessiService = eessiService;
        this.joarkFasade = joarkFasade;
        this.persondataFasade = persondataFasade;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.unleash = unleash;
    }

    public String lagJournalpostForSendingAvSedSomBrev(SedType sedType,
                                                       Land_iso2 mottakerland,
                                                       Behandling behandling)
        {
        return lagJournalpostForSendingAvSedSomBrev(sedType, mottakerland, behandling, Collections.emptyList());
    }

    public String lagJournalpostForSendingAvSedSomBrev(SedType sedType,
                                                       Land_iso2 mottakerland,
                                                       Behandling behandling,
                                                       List<FysiskDokument> vedlegg) {
        var fagsak = behandling.getFagsak();
        var utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(mottakerland);
        String institusjonID = utenlandskMyndighetService.lagInstitusjonsId(utenlandskMyndighet);
        String brukerFnr = persondataFasade.hentFolkeregisterident(fagsak.hentBrukersAktørID());
        byte[] sedPdf = eessiService.genererSedPdf(behandling.getId(), sedType);
        var tema = unleash.isEnabled("melosys.behandle_alle_saker")
            ? utledTema(behandling.getFagsak().getTema())
            : fraBehandlingstema(behandling.getTema());

        OpprettJournalpost opprettJournalpost = OpprettJournalpost.lagJournalpostForSendingAvSedSomBrev(
            fagsak.getSaksnummer(), brukerFnr, sedType, sedPdf, institusjonID,
            utenlandskMyndighet.navn, mottakerland.getKode(), vedlegg, tema
        );
        return joarkFasade.opprettJournalpost(opprettJournalpost, true);
    }
}
