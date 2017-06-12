package no.nav.melosys.integrasjon.joark.journal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentDokumentIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentJournalpostListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v2.informasjon.Fagsystemer;
import no.nav.tjeneste.virksomhet.journal.v2.informasjon.JournalFiltrering;
import no.nav.tjeneste.virksomhet.journal.v2.informasjon.JournalfoertDokumentInfo;
import no.nav.tjeneste.virksomhet.journal.v2.informasjon.Journalpost;
import no.nav.tjeneste.virksomhet.journal.v2.informasjon.Sak;
import no.nav.tjeneste.virksomhet.journal.v2.informasjon.SoekeFilter;
import no.nav.tjeneste.virksomhet.journal.v2.informasjon.Variantformater;
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentRequest;
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentResponse;
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentJournalpostListeRequest;
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentJournalpostListeResponse;

@RunWith(SpringRunner.class)
public class JournalConsumerTestIT extends Gen3WsProxyServiceITBase {
    private static final Logger LOG = LoggerFactory.getLogger(JournalConsumerTestIT.class);
    private static final String FAGSYSTEM_AO01 = "AO01";
    private static final String KJENT_SAK = "12345d6";
    private static final String DOKUMENT_ID = "123";
    private static final String JOURNALPOST_ID = "456";
    private static final String INNGÅENDE_DOKUMENT = "I";
    private static final String FAGSYSTEM_ØVRIG = "OVR";
    private static final String SAK_SOM_IKKE_FINNES = "798";
    private static final String VARIANTFORMAT_ARKIV = "ARKIV";
    private JournalConsumer journalConsumer;

    @Autowired
    private JournalConsumerConfig consumerConfig;

    @Before
    public void setup() throws Exception {
        JournalConsumerProducer consumerProducer = new JournalConsumerProducer();
        consumerProducer.setConfig(consumerConfig);
        journalConsumer = consumerProducer.journalConsumer();
    }

    @Test
    public void test_hentJournalpostListe_tom() throws HentJournalpostListeSikkerhetsbegrensning {
        HentJournalpostListeRequest request = new HentJournalpostListeRequest();
        SoekeFilter soekeFilter = new SoekeFilter();
        soekeFilter.setJournalFiltrering(JournalFiltrering.INGEN);
        soekeFilter.setJournalpostType(INNGÅENDE_DOKUMENT);
        request.setSoekeFilter(soekeFilter);
        Sak sak = new Sak();
        Fagsystemer fagsystem = new Fagsystemer();
        fagsystem.setValue(FAGSYSTEM_ØVRIG);
        sak.setFagsystem(fagsystem);
        sak.setSakId(SAK_SOM_IKKE_FINNES);
        request.getSakListe().add(sak);

        HentJournalpostListeResponse response = journalConsumer.hentJournalpostListe(request);
        assertNotNull(response.getJournalpostListe());
        assertTrue(response.getJournalpostListe().isEmpty());
    }

    @Test
    public void test_hentDokument_ukjentID() throws Exception {
        HentDokumentRequest request = new HentDokumentRequest();
        request.setDokumentId(DOKUMENT_ID);
        request.setJournalpostId(JOURNALPOST_ID);
        Variantformater variantFormat = new Variantformater();
        variantFormat.setValue(VARIANTFORMAT_ARKIV);
        request.setVariantformat(variantFormat);
        try {
            HentDokumentResponse response = journalConsumer.hentDokument(request);
            assertNotNull(response.getDokument());
            fail("Ventet HentDokumentDokumentIkkeFunnet exception");
        } catch (HentDokumentDokumentIkkeFunnet somForventet) {
            LOG.info("HentDokumentDokumentIkkeFunnet kastet som forventet.");
        }
    }

    @Test
    public void test_hent_journalpostliste_ukjent_sak() throws HentJournalpostListeSikkerhetsbegrensning {
        HentJournalpostListeRequest hentJournalpostListeRequest = new HentJournalpostListeRequest();

        Sak sak = new Sak();
        Fagsystemer fagsystemer = new Fagsystemer();
        fagsystemer.setValue("OVR"); // TODO: opprett egen fagsystemId for vedtaksløsningen. Se:
                                     // http://stash.devillo.no/projects/BOAF/repos/joark/browse/layers/domain/nav-domain-dok-joark-java/src/main/java/no/nav/domain/dok/joark/codestable/FagsystemCode.java
        sak.setFagsystem(fagsystemer);
        sak.setSakId("123");
        hentJournalpostListeRequest.getSakListe().add(sak);

        HentJournalpostListeResponse response = journalConsumer.hentJournalpostListe(hentJournalpostListeRequest);
        LOG.info("HentJournalpostListeResponse: " + response);
    }

    @Ignore
    @Test
    public void test_hent_soknads_xml() throws Exception {

        HentDokumentRequest hentDokumentRequest = new HentDokumentRequest();
        hentDokumentRequest.setDokumentId("393894382");
        hentDokumentRequest.setJournalpostId("389426442");
        Variantformater variantFormat = new Variantformater();
        variantFormat.setValue("ARKIV");
        hentDokumentRequest.setVariantformat(variantFormat);

        HentDokumentResponse hentDokumentResponse = journalConsumer.hentDokument(hentDokumentRequest);

        assertThat(hentDokumentResponse.getDokument(), is(notNullValue()));
        LOG.info("Dokument : " + new String(hentDokumentResponse.getDokument()));
    }

    @Test
    public void test_hent_dokument_fra_kjent_sak() throws Exception {
        HentJournalpostListeRequest hentJournalpostListeRequest = new HentJournalpostListeRequest();
        Sak sak = new Sak();
        Fagsystemer fagsystemer = new Fagsystemer();
        fagsystemer.setValue(FAGSYSTEM_AO01);
        sak.setFagsystem(fagsystemer);
        sak.setSakId(KJENT_SAK);
        hentJournalpostListeRequest.getSakListe().add(sak);

        HentJournalpostListeResponse hentJournalpostListeResponse = journalConsumer.hentJournalpostListe(hentJournalpostListeRequest);

        assertThat(hentJournalpostListeResponse.getJournalpostListe().isEmpty(), is(false));
        Journalpost journalpost = hentJournalpostListeResponse.getJournalpostListe().get(0);

        HentDokumentRequest hentDokumentRequest = new HentDokumentRequest();
        assertThat(journalpost.getDokumentinfoRelasjonListe().isEmpty(), is(false));
        JournalfoertDokumentInfo journalfoertDokumentInfo = journalpost.getDokumentinfoRelasjonListe().get(0).getJournalfoertDokument();
        hentDokumentRequest.setDokumentId(journalfoertDokumentInfo.getDokumentId());
        hentDokumentRequest.setJournalpostId(journalpost.getJournalpostId());
        Variantformater variantFormat = new Variantformater();
        variantFormat.setValue(journalfoertDokumentInfo.getBeskriverInnholdListe().get(0).getVariantformat().getValue());
        hentDokumentRequest.setVariantformat(variantFormat);

        HentDokumentResponse hentDokumentResponse = journalConsumer.hentDokument(hentDokumentRequest);

        assertThat(hentDokumentResponse.getDokument(), is(notNullValue()));
        LOG.info("Dokument : " + Arrays.toString(hentDokumentResponse.getDokument()));
    }

}