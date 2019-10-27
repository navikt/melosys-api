package no.nav.melosys.service.saksflyt;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.metrics.MetrikkerNavn;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.journalforing.dto.DokumentDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringDto;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import static no.nav.melosys.domain.util.SoeknadUtils.hentPeriode;
import static no.nav.melosys.domain.util.SoeknadUtils.hentSøknadsland;

@Service
public class ProsessinstansService {
    private static final Logger logger = LoggerFactory.getLogger(ProsessinstansService.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ProsessinstansRepository prosessinstansRepo;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    private final Counter prosessinstanserOpprettet = Metrics.counter(MetrikkerNavn.PROSESSINSTANSER_OPPRETTET);

    @Autowired
    public ProsessinstansService(ApplicationEventPublisher applicationEventPublisher,
                                 ProsessinstansRepository prosessinstansRepo,
                                 UtenlandskMyndighetService utenlandskMyndighetService) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.prosessinstansRepo = prosessinstansRepo;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    public Prosessinstans lagJournalføringProsessinstans(ProsessType type, JournalfoeringDto journalfoeringDto) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(type);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);

        if (StringUtils.isNotEmpty(journalfoeringDto.getBehandlingstypeKode())) {
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.valueOf(journalfoeringDto.getBehandlingstypeKode()));
        }
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalfoeringDto.getJournalpostID());
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, journalfoeringDto.getDokumentID());
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, journalfoeringDto.getOppgaveID());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, journalfoeringDto.getBrukerID());

        final String avsenderID = journalfoeringDto.getAvsenderID();
        if (erAvsenderErUtenlandskTrygemyndighet(avsenderID)) {
            prosessinstans.setData(ProsessDataKey.AVSENDER_ID, lagInstitusjonsId(avsenderID));
        } else {
            prosessinstans.setData(ProsessDataKey.AVSENDER_ID, avsenderID);
        }
        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, journalfoeringDto.getAvsenderNavn());
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, journalfoeringDto.getHoveddokumentTittel());
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, journalfoeringDto.isSkalTilordnes());
        prosessinstans.setData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, skalSendesForvaltningsmelding(journalfoeringDto));

        if (!CollectionUtils.isEmpty(journalfoeringDto.getVedlegg())) {
            final String hovedDokumentID = journalfoeringDto.getDokumentID();
            prosessinstans.setData(ProsessDataKey.LOGISKE_VEDLEGG_TITLER,
                journalfoeringDto.getVedlegg().stream().filter(v -> v.erLogiskVedlegg(hovedDokumentID)).map(DokumentDto::getTittel).collect(Collectors.toList()));
            prosessinstans.setData(ProsessDataKey.FYSISKE_VEDLEGG,
                journalfoeringDto.getVedlegg().stream().filter(v -> v.erFysiskVedlegg(hovedDokumentID)).collect(Collectors.toMap(DokumentDto::getDokumentID, DokumentDto::getTittel)));
        }

        return prosessinstans;
    }

    private static boolean erAvsenderErUtenlandskTrygemyndighet(String avsenderID) {
        return Arrays.stream(Landkoder.values()).anyMatch(l -> l.getKode().equals(avsenderID));
    }

    private String lagInstitusjonsId(String avsenderID) {
        try {
            return utenlandskMyndighetService.lagInstitusjonsId(Landkoder.valueOf(avsenderID));
        } catch (TekniskException e) {
            logger.warn(e.getMessage());
            logger.warn("Bruker {}: som avsenderID", avsenderID);
            return avsenderID + ":";
        }
    }

    private static boolean skalSendesForvaltningsmelding(JournalfoeringDto journalfoeringDto) {
        return journalfoeringDto.isIkkeSendForvaltingsmelding() != null && !journalfoeringDto.isIkkeSendForvaltingsmelding();
    }

    public boolean erUnderOppfriskning(Long behandlingID) {
        return prosessinstansRepo.findByTypeAndBehandling_IdAndStegIsNotAndStegIsNot(ProsessType.OPPFRISKNING, behandlingID, ProsessSteg.FEILET_MASKINELT, ProsessSteg.FERDIG).isPresent();
    }

    public boolean harAktivProsessinstans(Long behandlingID) {
        return prosessinstansRepo.findByBehandling_IdAndStegIsNotAndStegIsNot(behandlingID, ProsessSteg.FEILET_MASKINELT, ProsessSteg.FERDIG).isPresent();
    }

    public void lagre(Prosessinstans prosessinstans) {
        lagre(prosessinstans, SubjectHandler.getInstance().getUserID());
        prosessinstanserOpprettet.increment();
    }

    void lagre(Prosessinstans prosessinstans, String saksbehandler) {
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);
        if (saksbehandler != null) {
            prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
        }

        prosessinstansRepo.save(prosessinstans);
        applicationEventPublisher.publishEvent(new ProsessinstansOpprettetEvent(prosessinstans));

        logger.info("Saksbehandler={} har opprettet prosessinstans {} av type {}.", saksbehandler, prosessinstans.getId(), prosessinstans.getType());
    }

    public void opprettProsessinstansAnmodningOmUnntak(Behandling behandling) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);
        prosessinstans.setSteg(ProsessSteg.AOU_VALIDERING);
        prosessinstans.setBehandling(behandling);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansAnmodningOmUnntakMottakSvar(Behandling behandling) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_SVAR);
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_SVAR_SEND_SED);
        prosessinstans.setBehandling(behandling);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansHenleggSak(Behandling behandling, Henleggelsesgrunner begrunnelseKode, String fritekst) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.HENLEGG_SAK);

        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, SubjectHandler.getInstance().getUserID());
        prosessinstans.setData(ProsessDataKey.BEGRUNNELSEKODE, begrunnelseKode);
        if (begrunnelseKode == Henleggelsesgrunner.ANNET) {
            prosessinstans.setData(ProsessDataKey.FRITEKST, fritekst);
        }

        prosessinstans.setSteg(no.nav.melosys.domain.ProsessSteg.HS_OPPDATER_RESULTAT);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansIverksettVedtak(Behandling behandling, Behandlingsresultattyper behandlingsresultatType) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setSteg(ProsessSteg.IV_VALIDERING);
        prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, behandlingsresultatType.getKode());
        prosessinstans.setBehandling(behandling);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansMangelbrev(Behandling behandling, Aktoersroller mottaker, BrevData brevData) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.MANGELBREV);
        prosessinstans.setSteg(ProsessSteg.MANGELBREV);
        prosessinstans.setData(ProsessDataKey.MOTTAKER, mottaker);
        prosessinstans.setData(ProsessDataKey.BREVDATA, brevData);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansOppfriskning(Behandling behandling, String aktørID, String brukerID, SoeknadDokument søknadDokument) {
        Prosessinstans nyprosessinstans = new Prosessinstans();
        nyprosessinstans.setBehandling(behandling);
        nyprosessinstans.setType(ProsessType.OPPFRISKNING);

        nyprosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);
        nyprosessinstans.setData(ProsessDataKey.BRUKER_ID, brukerID);

        nyprosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, hentPeriode(søknadDokument));
        nyprosessinstans.setData(ProsessDataKey.SØKNADSLAND, hentSøknadsland(søknadDokument));

        nyprosessinstans.setSteg(ProsessSteg.JFR_HENT_PERS_OPPL);

        lagre(nyprosessinstans);
    }

    public void opprettProsessinstansForkortPeriode(Behandling behandling, Endretperiode endretperiode) {
        Prosessinstans nyprosessinstans = new Prosessinstans();
        nyprosessinstans.setData(ProsessDataKey.BEGRUNNELSEKODE, endretperiode);
        nyprosessinstans.setBehandling(behandling);
        nyprosessinstans.setType(ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE);
        nyprosessinstans.setSteg(ProsessSteg.IV_FORKORT_PERIODE);

        lagre(nyprosessinstans);
    }

    public void opprettProsessinstansGodkjennUnntaksperiode(Behandling behandling) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.REGISTRERING_UNNTAK);
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPDATER_MEDL);
        lagre(prosessinstans);
    }

    public void opprettProsessinstansUnntaksperiodeAvvist(Behandling behandling, Collection<Ikke_godkjent_begrunnelser> begrunnelser, String begrunnelseFritekst) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.REGISTRERING_UNNTAK);
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_PERIODE_IKKE_GODKJENT);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSER, begrunnelser);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST, begrunnelseFritekst);
        lagre(prosessinstans);
    }

    public void opprettProsessinstansUnntaksperiodeUnderAvklaring(Behandling behandling) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.REGISTRERING_UNNTAK);
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_UNDER_AVKLARING);
        prosessinstans.setBehandling(behandling);
        lagre(prosessinstans);
    }

    public void opprettProsessinstansForvaltningsmelding(Behandling behandling) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.FORVALTNINGSMELDING_SEND);
        prosessinstans.setSteg(ProsessSteg.SEND_FORVALTNINGSMELDING);
        prosessinstans.setBehandling(behandling);
        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansSedMottak(MelosysEessiMelding melosysEessiMelding) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.MOTTAK_SED);
        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_RUTING);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, melosysEessiMelding.getJournalpostId());
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, melosysEessiMelding.getDokumentId());
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, melosysEessiMelding.getErEndring());
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, melosysEessiMelding.getGsakSaksnummer());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, melosysEessiMelding.getAktoerId());
        lagre(prosessinstans);
    }

    public void opprettProsessinstansSedMottak(String journalpostID, String brukerID) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.MOTTAK_SED);
        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_HENT_EESSI_MELDING);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostID);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, brukerID);
        lagre(prosessinstans);
    }

    public void opprettProsessinstansGenerellSedBehandling(JournalfoeringDto journalfoeringDto) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.SED_GENERELL_SAK);
        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_HENT_EESSI_MELDING);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalfoeringDto.getJournalpostID());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, journalfoeringDto.getBrukerID());
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.valueOf(journalfoeringDto.getBehandlingstypeKode()));
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, journalfoeringDto.isSkalTilordnes());
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, journalfoeringDto.getHoveddokumentTittel());

        lagre(prosessinstans);
    }

    public void opprettProsessinstansVideresendSoknad(Behandling behandling) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.VIDERESEND_SOKNAD);
        prosessinstans.setSteg(ProsessSteg.VS_OPPDATER_RESULTAT);
        prosessinstans.setBehandling(behandling);

        lagre(prosessinstans);
    }
}
