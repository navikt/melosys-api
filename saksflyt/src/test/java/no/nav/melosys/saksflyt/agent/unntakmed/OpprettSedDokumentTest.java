package no.nav.melosys.saksflyt.agent.unntakmed;

import no.nav.melosys.domain.*;
import no.nav.melosys.eessi.avro.MelosysEessiMelding;
import no.nav.melosys.eessi.avro.Periode;
import no.nav.melosys.eessi.avro.UtenlandskIdent;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class OpprettSedDokumentTest {

    @Mock
    private SaksopplysningRepository saksopplysningRepository;

    @InjectMocks
    private OpprettSedDokument opprettSedDokument;


    @Test
    public void utfoerSteg() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.SEDDOKUMENT,
            MelosysEessiMelding.newBuilder()
                .setArtikkel("12_2")
                .setAktoerId("11111111111")
                .setDokumentId("123sdf")
                .setGsakSaksnummer(111222432)
                .setJournalpostId("321432")
                .setLovvalgsland("SE")
                .setPeriode(Periode.newBuilder().setFom("12-12-2020").setTom("12-12-2022").build())
                .setRinaSaksnummer("111333")
                .setSedId("abcdef1")
                .setUtenlandskIdent(UtenlandskIdent.newBuilder().setIdent("2222222222").setLandkode("SE").build())
                .build()
        );
        prosessinstans.setBehandling(new Behandling());

        opprettSedDokument.utfør(prosessinstans);

        verify(saksopplysningRepository, times(1)).save(any(Saksopplysning.class));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.VALIDER_UNNTAK);
    }
}