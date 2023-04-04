package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.FysiskDokument;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SedSomBrevService {
    private final EessiService eessiService;
    private final JoarkFasade joarkFasade;
    private final PersondataFasade persondataFasade;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final OppgaveFactory oppgaveFactory;

    public SedSomBrevService(EessiService eessiService,
                             JoarkFasade joarkFasade,
                             PersondataFasade persondataFasade,
                             UtenlandskMyndighetService utenlandskMyndighetService,
                             OppgaveFactory oppgaveFactory) {
        this.eessiService = eessiService;
        this.joarkFasade = joarkFasade;
        this.persondataFasade = persondataFasade;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.oppgaveFactory = oppgaveFactory;
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
        String institusjonID = utenlandskMyndighet.hentInstitusjonID();
        String brukerFnr = persondataFasade.hentFolkeregisterident(fagsak.hentBrukersAktørID());
        byte[] sedPdf = eessiService.genererSedPdf(behandling.getId(), sedType);
        var tema = oppgaveFactory.utledTema(behandling.getFagsak().getTema());

        OpprettJournalpost opprettJournalpost = OpprettJournalpost.lagJournalpostForSendingAvSedSomBrev(
            fagsak.getSaksnummer(), brukerFnr, sedType, sedPdf, institusjonID,
            utenlandskMyndighet.navn, mottakerland.getKode(), vedlegg, tema
        );
        return joarkFasade.opprettJournalpost(opprettJournalpost, true);
    }
}
