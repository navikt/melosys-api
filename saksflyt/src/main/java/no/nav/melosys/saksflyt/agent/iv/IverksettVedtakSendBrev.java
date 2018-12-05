package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevDataByggerA1;
import no.nav.melosys.service.dokument.brev.BrevDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.Dokumenttype.ATTEST_A1;
import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.GSAK_AVSLUTT_OPPGAVE;
import static no.nav.melosys.domain.ProsessSteg.IV_SEND_BREV;

/**
 * Sende ulike brev basert på lovvalgsbestemmelse.
 * <p>
 * Transisjoner:
 * ProsessType.IVERKSETT_VEDTAK
 *  IV_SEND_BREV -> GSAK_AVSLUTT_OPPGAVE eller FEILET_MASKINELT hvis feil
 */
@Component
public class IverksettVedtakSendBrev extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(IverksettVedtakSendBrev.class);

    private final DokumentSystemService dokumentService;

    private final BrevDataByggerA1 brevDataByggerA1;

    @Autowired
    public IverksettVedtakSendBrev(DokumentSystemService dokumentService, BrevDataByggerA1 brevDataByggerA1) {
        this.dokumentService = dokumentService;
        this.brevDataByggerA1 = brevDataByggerA1;

        log.info("IverksetteVedtakSendBrev initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_SEND_BREV;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        ProsessType prosessType = prosessinstans.getType();
        long behandlingId = prosessinstans.getBehandling().getId();
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);

        if (ProsessType.IVERKSETT_VEDTAK == prosessType) {
            BrevDataDto brevDataDto = brevDataByggerA1.lag(behandlingId, saksbehandler);
            dokumentService.produserDokument(behandlingId, ATTEST_A1, brevDataDto);
            prosessinstans.setSteg(GSAK_AVSLUTT_OPPGAVE);
        } else {
            String feilmelding = "Ukjent prosess type: " + prosessType;
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
        }
    }
}
