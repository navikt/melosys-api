package no.nav.melosys.service.dokument;

import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.dokgen.DokgenMalResolver;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DokgenService {

    private final DokgenConsumer dokgenConsumer;
    private final DokgenMalResolver dokgenMalResolver;
    private final JoarkFasade joarkFasade;
    private final DokgenMalMapper dokgenMalMapper;

    @Autowired
    public DokgenService(DokgenConsumer dokgenConsumer, DokgenMalResolver dokgenMalResolver, JoarkFasade joarkFasade, DokgenMalMapper dokgenMalMapper) {
        this.dokgenConsumer = dokgenConsumer;
        this.dokgenMalResolver = dokgenMalResolver;
        this.joarkFasade = joarkFasade;
        this.dokgenMalMapper = dokgenMalMapper;
    }

    public byte[] produserBrev(DokgenBrevbestilling brevbestilling) throws FunksjonellException, TekniskException {
        String malnavn = dokgenMalResolver.hentMalnavn(brevbestilling.getProduserbartdokument());
        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(settJournalpostOpplysninger(brevbestilling.getBehandling(), brevbestilling));

        return dokgenConsumer.lagPdf(malnavn, dokgenDto, brevbestilling.bestillKopi());
    }

    public String hentMalnavn(Produserbaredokumenter produserbartDokument) throws FunksjonellException {
        return dokgenMalResolver.hentMalnavn(produserbartDokument);
    }

    boolean erTilgjengeligDokgenmal(Produserbaredokumenter produserbartDokument) {
        Set<Produserbaredokumenter> tilgjengeligeMaler = dokgenMalResolver.utledTilgjengeligeMaler();
        return tilgjengeligeMaler.contains(produserbartDokument);
    }

    private DokgenBrevbestilling settJournalpostOpplysninger(Behandling behandling, DokgenBrevbestilling brevbestilling) throws SikkerhetsbegrensningException, IntegrasjonException {
        Journalpost journalpost = joarkFasade.hentJournalpost(behandling.getInitierendeJournalpostId());
        DokgenBrevbestilling.Builder builder = brevbestilling.toBuilder();
        return builder
            .medForsendelseMottatt(journalpost.getForsendelseMottatt())
            .medAvsenderNavn(journalpost.getAvsenderNavn())
            .medAvsenderId(journalpost.getAvsenderId())
            .build();
    }
}
