package no.nav.melosys.integrasjon.gsak;

import java.time.LocalDate;

import no.nav.melosys.domain.gsak.AktorType;
import no.nav.melosys.domain.gsak.Fagomrade;
import no.nav.melosys.domain.gsak.Oppgavetype;
import no.nav.melosys.domain.gsak.PrioritetType;
import no.nav.melosys.domain.gsak.Underkategori;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.BehandleOppgaveConsumer;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.BehandleOppgaveConsumerImpl;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.OpprettOppgaveRequest;
import no.nav.melosys.integrasjon.gsak.behandlesak.BehandleSakConsumer;
import no.nav.melosys.integrasjon.gsak.behandlesak.BehandleSakConsumerImpl;
import no.nav.melosys.integrasjon.gsak.oppgave.OppgaveConsumer;
import no.nav.melosys.integrasjon.gsak.oppgave.OppgaveConsumerImpl;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.BehandleOppgaveV1;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSOppgave;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSOpprettOppgaveRequest;
import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.BehandleSakV1;
import no.nav.tjeneste.virksomhet.oppgave.v3.binding.OppgaveV3;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class GsakServiceTest {

    private GsakService gsakService;

    @Before
    public void setUp() {
        BehandleSakV1 behandleSakV1port = mock(BehandleSakV1.class);
        OppgaveV3 oppgaveV3port = mock(OppgaveV3.class);
        BehandleOppgaveV1 behandleOppgaveV1port = mock(BehandleOppgaveV1.class);

        BehandleSakConsumer behandleSakConsumer = new BehandleSakConsumerImpl(behandleSakV1port);
        OppgaveConsumer oppgaveConsumer = new OppgaveConsumerImpl(oppgaveV3port);
        BehandleOppgaveConsumer behandleOppgaveConsumer = new BehandleOppgaveConsumerImpl(behandleOppgaveV1port);

        gsakService = new GsakService(behandleSakConsumer, oppgaveConsumer, behandleOppgaveConsumer);
    }

    @Test
    public void skal_mappe_alle_verdier_fra_mal_til_request() throws Exception {
        final LocalDate now = LocalDate.now();
        final OpprettOppgaveRequest mal = OpprettOppgaveRequest.builder()
                .medMottattDato(now.plusDays((long) (Math.random() * 6L)))
                .medDokumentId("dokumentId")
                .medAktørType(AktorType.PERSON)
                .medFnr("12345123456")
                .medUnderkategori(Underkategori.ANNET_MED)
                .medFagområde(Fagomrade.MED)
                .medSaksnummer("Saksnr")
                .medAktivTil(now.plusDays((long) (Math.random() * 6L)))
                .medAktivFra(now.plusDays((long) (Math.random() * 6L)))
                .medAnsvarligEnhetId("EnhetId")
                .medBeskrivelse("Beskrivelse")
                .medLest(true)
                .medOppgaveType(Oppgavetype.BEH_SAK_MED)
                .medPrioritetType(PrioritetType.NORM_MED)
                .medNormertBehandlingsTidInnen(now.plusDays((long) (Math.random() * 6L)))
                .build();

        final WSOpprettOppgaveRequest request = gsakService.convertToWSRequest(mal);
        final WSOppgave oppgave = request.getWsOppgave();

        assertThat(request.getOpprettetAvEnhetId()).isEqualTo(mal.getOpprettetAvEnhetId());
        assertThat(KonverteringsUtils.xmlGregorianCalendarToLocalDate(oppgave.getAktivFra())).isEqualTo(mal.getAktivFra().orElseGet(null));
        assertThat(KonverteringsUtils.xmlGregorianCalendarToLocalDate(oppgave.getAktivTil())).isEqualTo(mal.getAktivTil().orElseGet(null));
        assertThat(oppgave.getGjelderBruker().getIdent()).isEqualTo(mal.getFnr());
        assertThat(oppgave.getBeskrivelse()).isEqualTo(mal.getBeskrivelse());
        assertThat(oppgave.getFagomradeKode()).isEqualTo(mal.getFagområde().name());
        assertThat(KonverteringsUtils.xmlGregorianCalendarToLocalDate(oppgave.getMottattDato())).isEqualTo(mal.getMottattDato());
        assertThat(oppgave.getUnderkategoriKode()).isEqualTo(mal.getUnderkategoriKode().name());
        assertThat(KonverteringsUtils.xmlGregorianCalendarToLocalDate(oppgave.getNormDato())).isEqualTo(mal.getNormertBehandlingsTidInnen());
        assertThat(oppgave.getPrioritetKode()).isEqualTo(mal.getPrioritetType().name());
        assertThat(oppgave.getAnsvarligEnhetId()).isEqualTo(mal.getAnsvarligEnhetId());
        assertThat(oppgave.getDokumentId()).isEqualTo(mal.getDokumentId());
        assertThat(oppgave.getSaksnummer()).isEqualTo(mal.getSaksnummer());
        assertThat(oppgave.isLest()).isEqualTo(mal.isLest());
    }
}
