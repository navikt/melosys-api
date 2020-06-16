package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SedSomBrevService {
    private final EessiService eessiService;
    private final JoarkFasade joarkFasade;
    private final TpsFasade tpsFasade;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    public SedSomBrevService(@Qualifier("system") EessiService eessiService,
                             JoarkFasade joarkFasade,
                             TpsFasade tpsFasade,
                             UtenlandskMyndighetService utenlandskMyndighetService) {
        this.eessiService = eessiService;
        this.joarkFasade = joarkFasade;
        this.tpsFasade = tpsFasade;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    public String lagJournalpostForSendingAvSedSomBrev(SedType sedType, Behandling behandling, Landkoder land)
        throws MelosysException {
        Fagsak fagsak = behandling.getFagsak();
        UtenlandskMyndighet utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(land);
        String institusjonsId = utenlandskMyndighetService.lagInstitusjonsId(utenlandskMyndighet);
        String fnr = tpsFasade.hentIdentForAktørId(fagsak.hentBruker().getAktørId());
        byte[] pdf = eessiService.genererSedPdf(behandling.getId(), sedType);

        OpprettJournalpost opprettJournalpost = OpprettJournalpost.lagJournalpostForSendingAvSedSomBrev(
            fagsak.getGsakSaksnummer(), fnr, sedType, pdf,
            institusjonsId, utenlandskMyndighet.navn, land.getKode(), null
        );
        return joarkFasade.opprettJournalpost(opprettJournalpost, true);
    }
}
