package no.nav.melosys.service.dokument.brev;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.TemaFactory;
import no.nav.melosys.domain.arkiv.FysiskDokument;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Service;

@Service
public class SedSomBrevService {
    private final EessiService eessiService;
    private final JoarkFasade joarkFasade;
    private final PersondataFasade persondataFasade;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    public SedSomBrevService(EessiService eessiService,
                             JoarkFasade joarkFasade,
                             PersondataFasade persondataFasade,
                             UtenlandskMyndighetService utenlandskMyndighetService) {
        this.eessiService = eessiService;
        this.joarkFasade = joarkFasade;
        this.persondataFasade = persondataFasade;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    public String lagJournalpostForSendingAvSedSomBrev(SedType sedType,
                                                       Landkoder mottakerland,
                                                       Behandling behandling)
        {
        return lagJournalpostForSendingAvSedSomBrev(sedType, mottakerland, behandling, Collections.emptyList());
    }

    public String lagJournalpostForSendingAvSedSomBrev(SedType sedType,
                                                       Landkoder mottakerland,
                                                       Behandling behandling,
                                                       List<FysiskDokument> vedlegg) {
        var fagsak = behandling.getFagsak();
        var utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(mottakerland);
        String institusjonID = utenlandskMyndighetService.lagInstitusjonsId(utenlandskMyndighet);
        String brukerFnr = persondataFasade.hentFolkeregisterident(fagsak.hentBrukersAktørID());
        byte[] sedPdf = eessiService.genererSedPdf(behandling.getId(), sedType);
//        var tema = TemaFactory.fraBehandlingstema(behandling.getTema());
        var tema = OppgaveFactory.utledTema(behandling.getFagsak().getTema());

        OpprettJournalpost opprettJournalpost = OpprettJournalpost.lagJournalpostForSendingAvSedSomBrev(
            fagsak.getSaksnummer(), brukerFnr, sedType, sedPdf, institusjonID,
            utenlandskMyndighet.navn, mottakerland.getKode(), vedlegg, tema
        );
        return joarkFasade.opprettJournalpost(opprettJournalpost, true);
    }
}
