package no.nav.melosys.integrasjon.joark;

import java.util.List;

import no.nav.melosys.domain.DokumentTittel;
import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.integrasjon.Fagsystem;
import no.nav.melosys.integrasjon.Konstanter;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.behandleinngaaendejournal.BehandleInngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.inngaaendejournal.InngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.journal.JournalConsumer;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringFerdigstillingIkkeMulig;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringJournalpostIkkeInngaaende;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringObjektIkkeFunnet;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringUgyldigInput;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostJournalpostIkkeInngaaende;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostObjektIkkeFunnet;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostOppdateringIkkeMulig;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostUgyldigInput;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.ArkivSak;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Avsender;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Dokumentkategori;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.FerdigstillJournalfoeringRequest;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.OppdaterJournalpostRequest;
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
    public void ferdigstillJournalføring(String journalpostId) throws SikkerhetsbegrensningException {
        FerdigstillJournalfoeringRequest request = new FerdigstillJournalfoeringRequest();
        request.setJournalpostId(journalpostId);
        request.setEnhetId(String.valueOf(Konstanter.MELOSYS_ENHET_ID));
        try {
            behandleInngåendeJournalConsumer.ferdigstillJournalfoering(request);
        } catch (FerdigstillJournalfoeringFerdigstillingIkkeMulig ferdigstillJournalfoeringFerdigstillingIkkeMulig) {
            throw new IntegrasjonException(ferdigstillJournalfoeringFerdigstillingIkkeMulig);
        } catch (FerdigstillJournalfoeringJournalpostIkkeInngaaende ferdigstillJournalfoeringJournalpostIkkeInngaaende) {
            throw new IntegrasjonException(ferdigstillJournalfoeringJournalpostIkkeInngaaende);
        } catch (FerdigstillJournalfoeringUgyldigInput ferdigstillJournalfoeringUgyldigInput) {
            throw new IntegrasjonException(ferdigstillJournalfoeringUgyldigInput);
        } catch (FerdigstillJournalfoeringSikkerhetsbegrensning ferdigstillJournalfoeringSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(ferdigstillJournalfoeringSikkerhetsbegrensning);
        } catch (FerdigstillJournalfoeringObjektIkkeFunnet ferdigstillJournalfoeringObjektIkkeFunnet) {
            throw new IntegrasjonException(ferdigstillJournalfoeringObjektIkkeFunnet);
        }
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
    public void oppdaterJounalpost(String journalpostId, String gsakSaksnummer, String brukerID, String avsenderID, String avsenderNavn, String tittel) throws SikkerhetsbegrensningException {
        OppdaterJournalpostRequest request = new OppdaterJournalpostRequest();
        no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.InngaaendeJournalpost journalpost =
                new no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.InngaaendeJournalpost();
        journalpost.setJournalpostId(journalpostId);

        ArkivSak arkivSak = new ArkivSak();
        arkivSak.setArkivSakId(gsakSaksnummer);
        arkivSak.setArkivSakSystem(Fagsystem.GSAK.getKode());
        journalpost.setArkivSak(arkivSak);

        no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Person bruker = new no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Person();
        bruker.setIdent(brukerID);
        journalpost.setBruker(bruker);

        Avsender avsender = new Avsender();
        avsender.setAvsenderId(avsenderID);
        avsender.setAvsenderNavn(avsenderNavn);
        journalpost.setAvsender(avsender);

        no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Dokumentinformasjon dokumentinfo =
            new no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Dokumentinformasjon();
        Dokumentkategori dokumentkategori = new Dokumentkategori();
        dokumentkategori.setValue(DokumentKategoriKode.IS.getKode()); //FIXME bare hvis det ikke er satt allerede
        dokumentinfo.setDokumentkategori(dokumentkategori);
        dokumentinfo.setTittel(tittel);
        journalpost.setHoveddokument(dokumentinfo);
        journalpost.setInnhold(tittel); // Innhold bruker titlen siden det ikke finnes andre grunnlag for det.

        request.setInngaaendeJournalpost(journalpost);
        try {
            behandleInngåendeJournalConsumer.oppdaterJournalpost(request);
        } catch (OppdaterJournalpostSikkerhetsbegrensning oppdaterJournalpostSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(oppdaterJournalpostSikkerhetsbegrensning);
        } catch (OppdaterJournalpostOppdateringIkkeMulig oppdaterJournalpostOppdateringIkkeMulig) {
            throw new IntegrasjonException(oppdaterJournalpostOppdateringIkkeMulig);
        } catch (OppdaterJournalpostUgyldigInput oppdaterJournalpostUgyldigInput) {
            throw new IntegrasjonException(oppdaterJournalpostUgyldigInput);
        } catch (OppdaterJournalpostJournalpostIkkeInngaaende oppdaterJournalpostJournalpostIkkeInngaaende) {
            throw new IntegrasjonException(oppdaterJournalpostJournalpostIkkeInngaaende);
        } catch (OppdaterJournalpostObjektIkkeFunnet oppdaterJournalpostObjektIkkeFunnet) {
            throw new IntegrasjonException(oppdaterJournalpostObjektIkkeFunnet);
        }
    }
}
