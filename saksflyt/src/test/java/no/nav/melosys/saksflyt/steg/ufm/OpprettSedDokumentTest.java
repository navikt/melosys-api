package no.nav.melosys.saksflyt.steg.ufm;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.sed.BucType;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Periode;
import no.nav.melosys.domain.eessi.melding.Statsborgerskap;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OpprettSedDokumentTest {

    @Mock
    private OpprettSedDokumentFelles opprettSedDokumentFelles;

    private OpprettSedDokument opprettSedDokument;

    @Before
    public void setup() {
        opprettSedDokument = new OpprettSedDokument(opprettSedDokumentFelles);
    }

    @Test
    public void utfoerSteg() throws Exception {

        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        sedDokument.setBucType(BucType.LA_BUC_04);
        sedDokument.setSedType(SedType.A009);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding());
        prosessinstans.setBehandling(new Behandling());

        opprettSedDokument.utfør(prosessinstans);

        verify(opprettSedDokumentFelles).opprettSedSaksopplysning(any(MelosysEessiMelding.class), any(Behandling.class));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_HENT_PERSON);
    }

    private MelosysEessiMelding hentMelosysEessiMelding() {
        MelosysEessiMelding melding = new MelosysEessiMelding();
        melding.setAktoerId("123");
        melding.setArtikkel("12_1");
        melding.setDokumentId("123321");
        melding.setGsakSaksnummer(432432L);
        melding.setJournalpostId("j123");
        melding.setLovvalgsland("SE");

        Periode periode = new Periode();
        periode.setFom(LocalDate.of(2012, 12, 12));
        periode.setTom(LocalDate.of(2012, 12, 13));
        melding.setPeriode(periode);

        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setLandkode("SE");

        melding.setRinaSaksnummer("r123");
        melding.setSedId("s123");
        melding.setStatsborgerskap(
            Collections.singletonList(statsborgerskap));
        melding.setSedType("A009");
        melding.setBucType("LA_BUC_04");
        return melding;
    }
}