package no.nav.melosys.service.dokument.brev;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.arkiv.FysiskDokument;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SedSomBrevService {
    private final EessiService eessiService;
    private final JoarkFasade joarkFasade;
    private final PersondataFasade persondataFasade;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    public SedSomBrevService(@Qualifier("system") EessiService eessiService,
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
        throws MelosysException {
        return lagJournalpostForSendingAvSedSomBrev(sedType, mottakerland, behandling, Collections.emptyList());
    }

    public String lagJournalpostForSendingAvSedSomBrev(SedType sedType,
                                                       Landkoder mottakerland,
                                                       Behandling behandling,
                                                       List<FysiskDokument> vedlegg)
        throws MelosysException {
        Fagsak fagsak = behandling.getFagsak();
        UtenlandskMyndighet utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(mottakerland);
        String institusjonID = utenlandskMyndighetService.lagInstitusjonsId(utenlandskMyndighet);
        String brukerFnr = persondataFasade.hentFolkeregisterIdent(fagsak.hentBruker().getAktørId());
        byte[] sedPdf = eessiService.genererSedPdf(behandling.getId(), sedType);

        OpprettJournalpost opprettJournalpost = OpprettJournalpost.lagJournalpostForSendingAvSedSomBrev(
            fagsak.getGsakSaksnummer(), brukerFnr, sedType, sedPdf,
            institusjonID, utenlandskMyndighet.navn, mottakerland.getKode(), vedlegg
        );
        return joarkFasade.opprettJournalpost(opprettJournalpost, true);
    }
}
