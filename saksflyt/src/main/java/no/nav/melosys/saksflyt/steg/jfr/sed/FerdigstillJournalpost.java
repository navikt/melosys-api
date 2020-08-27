package no.nav.melosys.saksflyt.steg.jfr.sed;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("SedMottakFerdigstillJournalpost")
public class FerdigstillJournalpost implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(FerdigstillJournalpost.class);

    private final JoarkFasade joarkFasade;
    private final TpsFasade tpsFasade;

    @Autowired
    public FerdigstillJournalpost(JoarkFasade joarkFasade, TpsFasade tpsFasade) {
        this.joarkFasade = joarkFasade;
        this.tpsFasade = tpsFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        ProsessSteg nesteSteg;
        if (prosessinstans.getType() == ProsessType.OPPRETT_NY_SAK) {
            prosessinstans.setSteg(ProsessSteg.SED_GENERELL_SAK_HENT_PERSON);
            return;
        } else if (prosessinstans.getType() != ProsessType.MOTTAK_SED_JOURNALFØRING) {
            nesteSteg = ProsessSteg.hentFørsteProsessStegForType(prosessinstans.getType());
        } else {
            nesteSteg = ProsessSteg.FERDIG;
        }

        String journalpostId = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        String brukerID = hentBrukerID(prosessinstans);
        Long arkivSakID = prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID, Long.class);
        String tittel = prosessinstans.getData(ProsessDataKey.HOVEDDOKUMENT_TITTEL);
        String dokumentID = prosessinstans.getData(ProsessDataKey.DOKUMENT_ID);
        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder()
            .medBrukerID(brukerID).medArkivSakID(arkivSakID).medHovedDokumentID(dokumentID).medTittel(tittel).build();
        joarkFasade.oppdaterJournalpost(journalpostId, journalpostOppdatering, true);
        log.info("Journalpost {} ferdigstilt for gsak-sak {}", journalpostId, prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID));
        prosessinstans.setSteg(nesteSteg);
    }

    private String hentBrukerID(Prosessinstans prosessinstans) throws IkkeFunnetException {
        String brukerID = prosessinstans.getData(ProsessDataKey.BRUKER_ID);
        if (StringUtils.isEmpty(brukerID)) {
            brukerID = tpsFasade.hentIdentForAktørId(prosessinstans.getData(ProsessDataKey.AKTØR_ID));
            prosessinstans.setData(ProsessDataKey.BRUKER_ID, brukerID);
        }

        return brukerID;
    }
}
