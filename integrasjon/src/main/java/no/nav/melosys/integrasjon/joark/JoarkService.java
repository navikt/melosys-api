package no.nav.melosys.integrasjon.joark;

import java.util.List;

import no.nav.melosys.domain.DokumentTittel;
import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.behandleinngaaendejournal.BehandleInngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.inngaaendejournal.InngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.journal.JournalConsumer;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.HentJournalpostJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.HentJournalpostJournalpostIkkeInngaaende;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.HentJournalpostSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.HentJournalpostUgyldigInput;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Aktoer;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Dokumentinformasjon;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.InngaaendeJournalpost;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Organisasjon;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Person;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostRequest;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostResponse;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentDokumentIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Variantformater;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JoarkService implements JoarkFasade {

    private BehandleInngaaendeJournalConsumer behandleInngåendeJournalConsumer;
    private InngaaendeJournalConsumer inngåendeJournalConsumer;
    private JournalConsumer journalConsumer;

    @Autowired
    public JoarkService(BehandleInngaaendeJournalConsumer behandleInngåendeJournal, InngaaendeJournalConsumer inngåendeJournal, JournalConsumer journal) {
        this.behandleInngåendeJournalConsumer = behandleInngåendeJournal;
        this.inngåendeJournalConsumer = inngåendeJournal;
        this.journalConsumer = journal;
    }

    @Override
    public Journalpost hentJournalpost(String journalpostID) throws SikkerhetsbegrensningException {
        HentJournalpostRequest request = new HentJournalpostRequest();
        request.setJournalpostId(journalpostID);

        HentJournalpostResponse hentJournalpostResponse;
        try {
            hentJournalpostResponse = inngåendeJournalConsumer.hentJournalpost(request);
        } catch (HentJournalpostJournalpostIkkeFunnet hentJournalpostJournalpostIkkeFunnet) {
            throw new IntegrasjonException(hentJournalpostJournalpostIkkeFunnet);
        } catch (HentJournalpostJournalpostIkkeInngaaende hentJournalpostJournalpostIkkeInngaaende) {
            throw new IntegrasjonException(hentJournalpostJournalpostIkkeInngaaende);
        } catch (HentJournalpostSikkerhetsbegrensning hentJournalpostSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(hentJournalpostSikkerhetsbegrensning);
        } catch (HentJournalpostUgyldigInput hentJournalpostUgyldigInput) {
            throw new IntegrasjonException(hentJournalpostUgyldigInput);
        }

        InngaaendeJournalpost inngaaendeJournalpost = hentJournalpostResponse.getInngaaendeJournalpost();
        Journalpost journalpost = new Journalpost(journalpostID);
        List<Aktoer> brukerListe = inngaaendeJournalpost.getBrukerListe();
        if (!brukerListe.isEmpty()) {
            // FIXME Vi antar foreløpig at det er bare en bruker. Vi trenger en avklaring.
            if (brukerListe.size() > 1) {
                throw new IntegrasjonException("Det finnes flere brukere i journalpost " + journalpostID); 
            } else {
                Aktoer aktoer = brukerListe.get(0);
                if (aktoer instanceof Person) {
                    Person p = (Person) aktoer;
                    journalpost.setBrukerId(p.getIdent());
                } else if (aktoer instanceof Organisasjon) {
                    throw new IntegrasjonException("Organisasjon i brukerlisten er ikke støttet");
                } else {
                    throw new IntegrasjonException(aktoer.getClass().getSimpleName() + " i brukerlisten er ikke støttet");
                }
            }
        }
        journalpost.setAvsenderId(inngaaendeJournalpost.getAvsenderId());
        if (inngaaendeJournalpost.getArkivSak() != null) {
            journalpost.setArkivSakId(inngaaendeJournalpost.getArkivSak().getArkivSakId());
            journalpost.setArkivSakSystem(inngaaendeJournalpost.getArkivSak().getArkivSakSystem());
        }
        if (inngaaendeJournalpost.getForsendelseMottatt() != null) {
            journalpost.setForsendelseMottatt(KonverteringsUtils.xmlGregorianCalendarToLocalDateTime(inngaaendeJournalpost.getForsendelseMottatt()));
        }
        Dokumentinformasjon hoveddokument = inngaaendeJournalpost.getHoveddokument();
        journalpost.setHoveddokumentId(hoveddokument.getDokumentId());
        // FIXME mangler opplysninger (tittel og vedleggstitler) fra https://jira.adeo.no/browse/PK-51497
        journalpost.setHoveddokumentTittel(DokumentTittel.SØKNAD_MEDLEMSSKAP.getBeskrivelse());

        return journalpost;
    }

    @Override
    public byte[] hentDokument(String journalPostID, String dokumentID) throws SikkerhetsbegrensningException {
        HentDokumentRequest request = new HentDokumentRequest();
        request.setDokumentId(dokumentID);
        request.setJournalpostId(journalPostID);

        Variantformater variantformat = new Variantformater();
        variantformat.setValue(Variantformat.ARKIV.toString());
        request.setVariantformat(variantformat);

        HentDokumentResponse hentDokumentResponse;
        try {
            hentDokumentResponse = journalConsumer.hentDokument(request);
        } catch (HentDokumentDokumentIkkeFunnet hentDokumentDokumentIkkeFunnet) {
            throw new IntegrasjonException(hentDokumentDokumentIkkeFunnet);
        } catch (HentDokumentSikkerhetsbegrensning hentDokumentSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(hentDokumentSikkerhetsbegrensning);
        } catch (HentDokumentJournalpostIkkeFunnet hentDokumentJournalpostIkkeFunnet) {
            throw new IntegrasjonException(hentDokumentJournalpostIkkeFunnet);
        }
        return hentDokumentResponse.getDokument();
    }
}
