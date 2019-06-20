package no.nav.melosys.service.eessi;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EessiMottakService {

    private final ProsessinstansService prosessinstansService;

    @Autowired
    public EessiMottakService(ProsessinstansService prosessinstansService) {
        this.prosessinstansService = prosessinstansService;
    }

    @Transactional
    public void behandleMottattMelding(MelosysEessiMelding melosysEessiMelding) {
        Prosessinstans prosessinstans = opprettProsessinstans(melosysEessiMelding);
        prosessinstansService.lagre(prosessinstans);
    }

    private Prosessinstans opprettProsessinstans(MelosysEessiMelding melosysEessiMelding) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.MOTTAK_SED);
        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, melosysEessiMelding.getAktoerId());
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, melosysEessiMelding.getJournalpostId());
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, melosysEessiMelding.getDokumentId());
        prosessinstans.setData(ProsessDataKey.ER_ENDRING, melosysEessiMelding.getErEndring());
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, melosysEessiMelding.getGsakSaksnummer());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        return prosessinstans;
    }
}
