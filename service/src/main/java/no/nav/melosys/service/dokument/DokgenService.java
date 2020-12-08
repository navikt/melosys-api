package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.dokgen.DokgenMalResolver;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DokgenService {

    private final DokgenConsumer dokgenConsumer;
    private final DokgenMalResolver dokgenMalResolver;
    private final JoarkFasade joarkFasade;
    private final KodeverkService kodeverkService;

    @Autowired
    public DokgenService(DokgenConsumer dokgenConsumer, DokgenMalResolver dokgenMalResolver, JoarkFasade joarkFasade, KodeverkService kodeverkService) {
        this.dokgenConsumer = dokgenConsumer;
        this.dokgenMalResolver = dokgenMalResolver;
        this.joarkFasade = joarkFasade;
        this.kodeverkService = kodeverkService;
    }

    public byte[] produserBrev(Produserbaredokumenter produserbartDokument, Behandling behandling) throws TekniskException, FunksjonellException {
        String malnavn = dokgenMalResolver.hentMalnavn(produserbartDokument);

        DokgenDto dokgenDto = dokgenMalResolver.mapBehandling(produserbartDokument, behandling, hentForsendelseMottattFraJournalpost(behandling));
        dokgenDto.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, dokgenDto.getPostnr(), LocalDate.now()));
        return lagPdf(malnavn, dokgenDto);
    }

    boolean erTilgjengeligDokgenmal(Produserbaredokumenter produserbartDokument) {
        Set<Produserbaredokumenter> tilgjengeligeMaler = dokgenMalResolver.utledTilgjengeligeMaler();
        return tilgjengeligeMaler.contains(produserbartDokument);
    }

    private byte[] lagPdf(String malNavn, DokgenDto dokgenDto) {
        return dokgenConsumer.lagPdf(malNavn, dokgenDto);
    }

    private Instant hentForsendelseMottattFraJournalpost(Behandling behandling) throws SikkerhetsbegrensningException, IntegrasjonException {
        return joarkFasade.hentInstantMottaksDatoForJournalpost(behandling.getInitierendeJournalpostId());
    }

    public String hentMalnavn(Produserbaredokumenter produserbartDokument) throws FunksjonellException {
        return dokgenMalResolver.hentMalnavn(produserbartDokument);
    }
}
