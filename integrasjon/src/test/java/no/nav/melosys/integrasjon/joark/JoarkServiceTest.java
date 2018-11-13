package no.nav.melosys.integrasjon.joark;

import java.time.LocalDate;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.melosys.domain.arkiv.JournalfoeringMangel;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.joark.journal.JournalConsumer;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.DokumentInformasjonMangler;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Journalfoeringsbehov;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.JournalpostMangler;
import no.nav.tjeneste.virksomhet.journal.v3.HentKjerneJournalpostListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.HentKjerneJournalpostListeUgyldigInput;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Journalposttyper;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.ArkivSak;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.DetaljertDokumentinformasjon;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JoarkServiceTest {

    private JoarkService joarkService;

    @Mock
    private JournalConsumer journalConsumer;

    @Before
    public void setUp() {
        this.joarkService = new JoarkService(null, null, journalConsumer);
    }

    @Test
    public void hentKjerneJournalpostListe() throws SikkerhetsbegrensningException, IntegrasjonException, HentKjerneJournalpostListeUgyldigInput, HentKjerneJournalpostListeSikkerhetsbegrensning, DatatypeConfigurationException {
        Long arkivSakID = Long.valueOf(1);
        HentKjerneJournalpostListeResponse response = new HentKjerneJournalpostListeResponse();
        no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.Journalpost journalpost = new no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.Journalpost();
        ArkivSak arkivSak = new ArkivSak();
        arkivSak.setArkivSakId("123");
        journalpost.setGjelderArkivSak(arkivSak);
        journalpost.setForsendelseMottatt(KonverteringsUtils.localDateToXMLGregorianCalendar(LocalDate.now()));
        journalpost.setForsendelseJournalfoert(KonverteringsUtils.localDateToXMLGregorianCalendar(LocalDate.now().plusDays(2)));
        DetaljertDokumentinformasjon dokumentInfo = new DetaljertDokumentinformasjon();
        String dokID = "dokID_1";
        String tittel = "Tittel 1";
        dokumentInfo.setDokumentId(dokID);
        dokumentInfo.setTittel(tittel);
        journalpost.setHoveddokument(dokumentInfo);
        Journalposttyper type = new Journalposttyper();
        type.setValue("I");
        journalpost.setJournalposttype(type);
        response.getJournalpostListe().add(journalpost);
        when(journalConsumer.hentKjerneJournalpostListe(any())).thenReturn(response);

        List<Journalpost> journalpostListe = joarkService.hentKjerneJournalpostListe(arkivSakID);
        assertThat(journalpostListe.size()).isEqualTo(1);

        Journalpost journalpost1 = journalpostListe.get(0);
        assertThat(journalpost1.getArkivSakId()).isEqualTo("123");
        assertThat(journalpost1.getHoveddokument().getDokumentId()).isEqualTo(dokID);
        assertThat(journalpost1.getHoveddokument().getTittel()).isEqualTo(tittel);
        assertThat(journalpost1.getJournalposttype()).isEqualTo(Journalposttype.INN);
        assertThat(journalpost1.getForsendelseMottatt()).isNotNull();
        assertThat(journalpost1.getForsendelseJournalfoert()).isNotNull();
    }


    @Test
    public void konverterTilJournalfoeringmangler() {
        JournalpostMangler input = new JournalpostMangler();
        input.setArkivSak(Journalfoeringsbehov.MANGLER);
        input.setAvsenderId(Journalfoeringsbehov.MANGLER_IKKE);
        input.setAvsenderNavn(Journalfoeringsbehov.MANGLER);
        input.setBruker(Journalfoeringsbehov.MANGLER);
        input.setForsendelseInnsendt(Journalfoeringsbehov.MANGLER_IKKE);
        DokumentInformasjonMangler dokumentInformasjonMangler = new DokumentInformasjonMangler();
        dokumentInformasjonMangler.setDokumentkategori(Journalfoeringsbehov.MANGLER);
        dokumentInformasjonMangler.setTittel(Journalfoeringsbehov.MANGLER_IKKE);
        input.setHoveddokument(dokumentInformasjonMangler);
        input.setInnhold(Journalfoeringsbehov.MANGLER);
        input.setTema(Journalfoeringsbehov.MANGLER);

        List<JournalfoeringMangel> journalfoeringMangler = joarkService.konverterTilJournalfoeringmangler(input);

        assertThat(journalfoeringMangler).isNotNull();
        assertThat(journalfoeringMangler).isNotEmpty();
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.ARKIVSAK);
        assertThat(journalfoeringMangler).doesNotContain(JournalfoeringMangel.AVSENDERID);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.AVSENDERNAVN);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.BRUKER);
        assertThat(journalfoeringMangler).doesNotContain(JournalfoeringMangel.FORSENDELSEINNSENDT);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.HOVEDDOKUMENT_KATEGORI);
        assertThat(journalfoeringMangler).doesNotContain(JournalfoeringMangel.HOVEDDOKUMENT_TITTEL);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.INNHOLD);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.TEMA);
    }
}