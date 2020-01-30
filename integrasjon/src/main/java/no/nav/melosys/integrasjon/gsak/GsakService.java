package no.nav.melosys.integrasjon.gsak;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableBiMap;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.domain.util.KodeverkUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.oppgave.OppgaveConsumer;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OpprettOppgaveDto;
import no.nav.melosys.integrasjon.gsak.sak.SakConsumer;
import no.nav.melosys.integrasjon.gsak.sak.dto.SakDto;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;

@Service
@Primary
public class GsakService implements GsakFasade {
    private static final Logger log = LoggerFactory.getLogger(GsakService.class);

    private static final String OPPGAVE_STATUS_FERDIGSTILT = "FERDIGSTILT";
    private static final String SORTERINGSFELT = "FRIST";
    private static final String OPPGAVE_STATUSKATEGORI_AAPEN = "AAPEN";

    private static final ImmutableBiMap<Behandlingstyper, String> BEHANDLINGSTYPE_FELLESKODE_MAP =
        ImmutableBiMap.<Behandlingstyper, String>builder()
            .put(SOEKNAD, "ae0034")
            .put(SOEKNAD_IKKE_YRKESAKTIV, "ae0238")
            .put(ENDRET_PERIODE, "ae0052")
            .put(ANKE, "ae0046")
            .put(KLAGE, "ae0058")
            .put(UTL_MYND_UTPEKT_NORGE, "ae0112")
            .put(NY_VURDERING, "ae0240")
            .put(UTL_MYND_UTPEKT_SEG_SELV, "ae0113")
            .put(ANMODNING_OM_UNNTAK_HOVEDREGEL, "ae0110")
            .put(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, "ae0111")
            .put(REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, "ae0235")
            .put(ØVRIGE_SED, "ukjent")
            .put(VURDER_TRYGDETID, "ae0236")
            .build();

    private final SakConsumer sakConsumer;

    private final OppgaveConsumer oppgaveConsumer;

    private static final EnumSet<Behandlingstyper> GYLDIGE_BEHANDLINGSTYPER_UFM = EnumSet.of(
        REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
        ANMODNING_OM_UNNTAK_HOVEDREGEL, UTL_MYND_UTPEKT_SEG_SELV
    );

    @Autowired
    public GsakService(SakConsumer sakConsumer, OppgaveConsumer oppgaveConsumer) {
        this.sakConsumer = sakConsumer;
        this.oppgaveConsumer = oppgaveConsumer;
    }

    @Override
    public Long opprettSak(String saksnummer, Behandlingstyper behandlingstype, String aktørId) throws FunksjonellException, TekniskException {
        SakDto sakDto = new SakDto();

        if (SOEKNAD == behandlingstype || SOEKNAD_IKKE_YRKESAKTIV == behandlingstype || VURDER_TRYGDETID == behandlingstype) {
            sakDto.setTema(Tema.MED.getKode());
        } else if (GYLDIGE_BEHANDLINGSTYPER_UFM.contains(behandlingstype)) {
            sakDto.setTema(Tema.UFM.getKode());
        } else {
            throw new TekniskException("Behandlingtype " + behandlingstype.getBeskrivelse() + " er ikke støttet.");
        }

        sakDto.setAktørId(aktørId);
        sakDto.setApplikasjon(Fagsystem.MELOSYS.getKode());
        sakDto.setSaksnummer(saksnummer);
        sakDto = sakConsumer.opprettSak(sakDto);

        if (sakDto.getId() == null) {
            log.error("Feil ved oppretting av sak i GSAK.");
            throw new IntegrasjonException("Feil ved oppretting av sak i GSAK.");
        }
        log.info("Sak opprettet i GSAK med saksnummer: {}", sakDto.getId());
        return sakDto.getId();
    }

    @Override
    public Tema hentTemaFraSak(Long gsakSaksnummer) throws FunksjonellException, TekniskException {
        return Tema.valueOf(sakConsumer.hentSak(gsakSaksnummer).getTema());
    }

    @Override
    public void ferdigstillOppgave(String oppgaveID) throws FunksjonellException, TekniskException {
        OppgaveDto oppgave = hentOppgaveDto(oppgaveID);
        oppgave.setStatus(OPPGAVE_STATUS_FERDIGSTILT);
        oppgaveConsumer.oppdaterOppgave(oppgave);
    }

    @Override
    public List<Oppgave> finnUtildelteOppgaverEtterFrist(Set<Oppgavetyper> oppgavetyper, Behandlingstyper behandlingstype, Behandlingstema behandlingstema)
        throws FunksjonellException, TekniskException {

        OppgaveSearchRequest.Builder searchRequestBuilder = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medOppgaveTyper(oppgavetyper.stream().map(Oppgavetyper::getKode).toArray(String[]::new))
            .medBehandlingsType(behandlingstype == null ? null : GsakService.hentFellesKode(behandlingstype))
            .medBehandlingstema(behandlingstema == null ? null : behandlingstema.getKode())
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medTildeltRessurs(false);

        if (!oppgavetyper.equals(Collections.singleton(Oppgavetyper.JFR))) {
            searchRequestBuilder.medBehandlesAvApplikasjon(Fagsystem.MELOSYS.getKode());
        }

        List<OppgaveDto> oppgaver = oppgaveConsumer.hentOppgaveListe(searchRequestBuilder.build());

        return oppgaver.stream().map(GsakService::oppgaveMappingDtoTilDomain).collect(Collectors.toList());
    }

    @Override
    public Oppgave hentOppgave(String oppgaveId) throws FunksjonellException, TekniskException {
        return oppgaveMappingDtoTilDomain(hentOppgaveDto(oppgaveId));
    }

    @Override
    public String opprettOppgave(Oppgave oppgave) throws FunksjonellException, TekniskException {
        LocalDate idag = LocalDate.now();
        OpprettOppgaveDto oppgaveDto = new OpprettOppgaveDto();
        oppgaveDto.setAktivDato(idag);
        oppgaveDto.setAktørId(oppgave.getAktørId());
        if (oppgave.getBehandlingstype() != null) {
            oppgaveDto.setBehandlingstype(hentFellesKode(oppgave.getBehandlingstype()));
        }
        if (oppgave.getBehandlingstema() != null) {
            oppgaveDto.setBehandlingstema(oppgave.getBehandlingstema().getKode());
        }
        oppgaveDto.setBeskrivelse(oppgave.getBeskrivelse());

        if (oppgave.getFristFerdigstillelse() != null) {
            oppgaveDto.setFristFerdigstillelse(oppgave.getFristFerdigstillelse());
        } else {
            throw new FunksjonellException("Frist ferdigstillelse er påkrevd for oppgave");
        }

        oppgaveDto.setJournalpostId(oppgave.getJournalpostId());
        oppgaveDto.setOppgavetype(oppgave.getOppgavetype().getKode());
        oppgaveDto.setPrioritet(oppgave.getPrioritet().toString());
        oppgaveDto.setSaksreferanse(oppgave.getSaksnummer());
        oppgaveDto.setTema(oppgave.getTema().getKode());
        oppgaveDto.setTildeltEnhetsnr(Integer.toString(MELOSYS_ENHET_ID));
        if (oppgave.getBehandlesAvApplikasjon() != Fagsystem.INTET) {
            oppgaveDto.setBehandlesAvApplikasjon(oppgave.getBehandlesAvApplikasjon().getKode());
        }

        oppgaveDto.setTilordnetRessurs(oppgave.getTilordnetRessurs());

        return oppgaveConsumer.opprettOppgave(oppgaveDto);
    }

    @Override
    public void leggTilbakeOppgave(String oppgaveId) throws FunksjonellException, TekniskException {
        OppgaveDto oppgave = hentOppgaveDto(oppgaveId);
        oppgave.setTilordnetRessurs(null);
        oppgaveConsumer.oppdaterOppgave(oppgave);
    }

    @Override
    public void tildelOppgave(String oppgaveId, String saksbehandler) throws FunksjonellException, TekniskException {
        oppdaterOppgave(oppgaveId, OppgaveOppdatering.builder().tilordnetRessurs(saksbehandler).build());
    }

    @Override
    public void oppdaterOppgave(String oppgaveID, OppgaveOppdatering oppgaveOppdatering) throws FunksjonellException, TekniskException {
        OppgaveDto oppgaveDto = hentOppgaveDto(oppgaveID);

        if (oppgaveOppdatering.getOppgavetype() != null) {
            oppgaveDto.setOppgavetype(oppgaveOppdatering.getOppgavetype().getKode());
        }
        if (oppgaveOppdatering.getTema() != null) {
            oppgaveDto.setTema(oppgaveOppdatering.getTema().getKode());
        }
        if (oppgaveOppdatering.getBehandlingstema() != null) {
            oppgaveDto.setBehandlingstema(oppgaveOppdatering.getBehandlingstema().getKode());
        }
        if (oppgaveOppdatering.getBehandlingstype() != null) {
            oppgaveDto.setBehandlingstype(hentFellesKode(oppgaveOppdatering.getBehandlingstype()));
        }
        if (oppgaveOppdatering.getBehandlesAvApplikasjon() != null && oppgaveOppdatering.getBehandlesAvApplikasjon() != Fagsystem.INTET) {
            oppgaveDto.setBehandlesAvApplikasjon(oppgaveOppdatering.getBehandlesAvApplikasjon().getKode());
        }
        if (StringUtils.isNotEmpty(oppgaveOppdatering.getSaksnummer())) {
            oppgaveDto.setSaksreferanse(oppgaveOppdatering.getSaksnummer());
        }

        if (StringUtils.isNotEmpty(oppgaveOppdatering.getBeskrivelse())) {
            if (StringUtils.isEmpty(oppgaveDto.getBeskrivelse())) {
                oppgaveDto.setBeskrivelse(oppgaveOppdatering.getBeskrivelse());
            } else {
                oppgaveDto.setBeskrivelse(StringUtils.joinWith("\n", oppgaveDto.getBeskrivelse(), oppgaveOppdatering.getBeskrivelse()));
            }
        }

        if (StringUtils.isNotEmpty(oppgaveOppdatering.getPrioritet())) {
            oppgaveDto.setPrioritet(oppgaveOppdatering.getPrioritet());
        }

        if (StringUtils.isNotEmpty(oppgaveOppdatering.getStatus())) {
            oppgaveDto.setStatus(oppgaveOppdatering.getStatus());
        }

        if (StringUtils.isNotEmpty(oppgaveOppdatering.getTilordnetRessurs())) {
            oppgaveDto.setTilordnetRessurs(oppgaveOppdatering.getTilordnetRessurs());
        }

        if (oppgaveOppdatering.getFristFerdigstillelse() != null) {
            oppgaveDto.setFristFerdigstillelse(oppgaveOppdatering.getFristFerdigstillelse());
        }

        oppgaveConsumer.oppdaterOppgave(oppgaveDto);
    }

    private OppgaveDto hentOppgaveDto(String oppgaveID) throws FunksjonellException, TekniskException {
        OppgaveDto oppgave = oppgaveConsumer.hentOppgave(oppgaveID);
        if (oppgave == null) {
            throw new IkkeFunnetException("Feil ved henting av oppgave for oppgaveID:" + oppgaveID);
        }

        return oppgave;
    }

    @Override
    public Set<Oppgave> finnOppgaveListeMedAnsvarlig(String tilordnetRessurs) throws FunksjonellException, TekniskException {
        OppgaveSearchRequest.Builder oppgaveSearchRequestBuilder = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medTilordnetRessurs(tilordnetRessurs)
            .medSorteringsfelt(SORTERINGSFELT)
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN);

        return hentOppgaverAlleTyper(oppgaveSearchRequestBuilder);
    }

    private Set<Oppgave> hentOppgaverAlleTyper(OppgaveSearchRequest.Builder oppgaveSearchRequestBuilder) throws FunksjonellException, TekniskException {
        // Henter oppgaver opprettet av melosys, hvor melosys har satt behandlesAvApplikasjon
        List<OppgaveDto> finnOppgaveListeResponse = oppgaveConsumer.hentOppgaveListe(
            oppgaveSearchRequestBuilder.medBehandlesAvApplikasjon(Fagsystem.MELOSYS.getKode()).build()
        );

        // Henter journalføringsoppgaver. Disse er ikke opprettet av Melosys
        List<OppgaveDto> finnJfrOppgaveListeResponse = oppgaveConsumer.hentOppgaveListe(
            oppgaveSearchRequestBuilder.medOppgaveTyper(new String[]{Oppgavetyper.JFR.getKode()})
                .medBehandlesAvApplikasjon(null).build()
        );

        return Stream.of(finnJfrOppgaveListeResponse, finnOppgaveListeResponse)
            .flatMap(Collection::stream)
            .map(GsakService::oppgaveMappingDtoTilDomain)
            .collect(Collectors.toSet());
    }

    @Override
    public Oppgave hentOppgaveMedSaksnummer(String saksnummer) throws TekniskException, FunksjonellException {
        List<Oppgave> oppgaver = finnOppgaverMedSaksnummer(saksnummer);

        if (!oppgaver.isEmpty()) {
            if (oppgaver.size() > 1) {
                throw new TekniskException("Det finnes flere aktive behandlingsoppgaver for sak " + saksnummer);
            }
            return oppgaver.get(0);
        } else {
            throw new IkkeFunnetException("Det finnes ingen aktive behandlingsoppgaver for sak " + saksnummer);
        }
    }

    @Override
    public List<Oppgave> finnOppgaverMedBrukerID(String aktørId) throws FunksjonellException, TekniskException {
        OppgaveSearchRequest oppgaveSearchRequest = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medAktørId(aktørId)
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .build();

        return oppgaveConsumer.hentOppgaveListe(oppgaveSearchRequest).stream()
            .map(GsakService::oppgaveMappingDtoTilDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Oppgave> finnOppgaverMedSaksnummer(String saksnummer) throws FunksjonellException, TekniskException {
        OppgaveSearchRequest oppgaveSearchRequest = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medSaksreferanse(new String[]{saksnummer})
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medOppgaveTyper(new String[]{Oppgavetyper.BEH_SAK_MK.getKode(), Oppgavetyper.VUR.getKode(), Oppgavetyper.BEH_SED.getKode()})
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .build();

        return oppgaveConsumer.hentOppgaveListe(oppgaveSearchRequest).stream()
            .map(GsakService::oppgaveMappingDtoTilDomain)
            .collect(Collectors.toList());
    }

    static Oppgave oppgaveMappingDtoTilDomain(OppgaveDto oppgaveDto) {
        Oppgave.Builder domainOppgaveBuilder = new Oppgave.Builder();
        String oppgaveId = oppgaveDto.getId();
        domainOppgaveBuilder
            .setAktivDato(oppgaveDto.getAktivDato())
            .setAktørId(oppgaveDto.getAktørId())
            .setBeskrivelse(oppgaveDto.getBeskrivelse())
            .setOpprettetTidspunkt(oppgaveDto.getOpprettetTidspunkt())
            .setFristFerdigstillelse(oppgaveDto.getFristFerdigstillelse())
            .setJournalpostId(oppgaveDto.getJournalpostId())
            .setOppgaveId(oppgaveId)
            .setSaksnummer(oppgaveDto.getSaksreferanse())
            .setStatus(oppgaveDto.getStatus())
            .setTemagruppe(oppgaveDto.getTemagruppe())
            .setTildeltEnhetsnr(oppgaveDto.getTildeltEnhetsnr())
            .setTilordnetRessurs(oppgaveDto.getTilordnetRessurs())
            .setVersjon(oppgaveDto.getVersjon())
            .setBehandlingstema(mapTilEnumFraKode(Behandlingstema.class, oppgaveDto.getBehandlingstema(), oppgaveId))
            .setTema(mapTilEnumFraKode(Tema.class, oppgaveDto.getTema(), oppgaveId))
            .setOppgavetype(mapTilEnumFraKode(no.nav.melosys.domain.kodeverk.Oppgavetyper.class, oppgaveDto.getOppgavetype(), oppgaveId))
            .setPrioritet(StringUtils.isNotEmpty(oppgaveDto.getPrioritet()) ? PrioritetType.valueOf(oppgaveDto.getPrioritet()) : null)
            .setBehandlesAvApplikasjon(mapTilEnumFraKode(Fagsystem.class, StringUtils.defaultString(oppgaveDto.getBehandlesAvApplikasjon()), oppgaveId));

        if (oppgaveDto.getBehandlingstype() != null) {
            try {
                domainOppgaveBuilder.setBehandlingstype(hentBehandlingstyper(oppgaveDto.getBehandlingstype()));
            } catch (IllegalArgumentException e) {
                log.warn("Fikk uventet behandlingstype: {} for OppgaveID: {}", oppgaveDto.getBehandlingstype(), oppgaveDto.getId());
            }
        }

        return domainOppgaveBuilder.build();
    }

    static OppgaveDto oppgaveMappingDomainTilDto(Oppgave oppgave) {
        OppgaveDto oppgaveDto = new OppgaveDto();
        oppgaveDto.setAktivDato(oppgave.getAktivDato());
        oppgaveDto.setAktørId(oppgave.getAktørId());
        if (oppgave.getBehandlesAvApplikasjon() != Fagsystem.INTET) {
            oppgaveDto.setBehandlesAvApplikasjon(oppgave.getBehandlesAvApplikasjon().getKode());
        }
        if (oppgave.getBehandlingstema() != null) {
            oppgaveDto.setBehandlingstema(oppgave.getBehandlingstema().getKode());
        }
        if (oppgave.getBehandlingstype() != null) {
            oppgaveDto.setBehandlingstype(hentFellesKode(oppgave.getBehandlingstype()));
        }
        oppgaveDto.setBeskrivelse(oppgave.getBeskrivelse());
        if (oppgave.getOpprettetTidspunkt() != null) {
            oppgaveDto.setOpprettetTidspunkt(oppgave.getOpprettetTidspunkt());
        }
        oppgaveDto.setFristFerdigstillelse(oppgave.getFristFerdigstillelse());
        oppgaveDto.setId(oppgave.getOppgaveId());
        oppgaveDto.setJournalpostId(oppgave.getJournalpostId());
        if (oppgave.getOppgavetype() != null) {
            oppgaveDto.setOppgavetype(oppgave.getOppgavetype().getKode());
        }
        if (oppgave.getPrioritet() != null) {
            oppgaveDto.setPrioritet(oppgave.getPrioritet().name());
        }
        oppgaveDto.setSaksreferanse(oppgave.getSaksnummer());
        oppgaveDto.setStatus(oppgave.getStatus());
        if (oppgave.getTema() != null) {
            oppgaveDto.setTema(oppgave.getTema().getKode());
        }
        oppgaveDto.setTemagruppe(oppgave.getTemagruppe());
        oppgaveDto.setTildeltEnhetsnr(oppgave.getTildeltEnhetsnr());
        oppgaveDto.setTilordnetRessurs(oppgave.getTilordnetRessurs());
        oppgaveDto.setVersjon(oppgave.getVersjon());

        return oppgaveDto;
    }

    /**
     * Henter koder fra felleskodeverk: Behandlingstyper.
     */
    static String hentFellesKode(Behandlingstyper behandlingstyper) {
        if (BEHANDLINGSTYPE_FELLESKODE_MAP.containsKey(behandlingstyper)) {
            return BEHANDLINGSTYPE_FELLESKODE_MAP.get(behandlingstyper);
        }
        throw new IllegalArgumentException(behandlingstyper + " er ikke implementert i felleskodeverk.");
    }

    /**
     * Mapper felleskodeverk til behandlingstyper.
     */
    static Behandlingstyper hentBehandlingstyper(String felleskode) {
        if (BEHANDLINGSTYPE_FELLESKODE_MAP.containsValue(felleskode)) {
            return BEHANDLINGSTYPE_FELLESKODE_MAP.inverse().get(felleskode);
        }
        throw new IllegalArgumentException(felleskode + " har ingen matchende behandlingstype.");
    }

    private static <K extends Kodeverk> K mapTilEnumFraKode(Class<K> clazz, String verdi, String oppgaveId) {
        if (verdi != null) {
            try {
                return KodeverkUtils.dekod(clazz, verdi);
            } catch (IkkeFunnetException e) {
                log.error("Fikk uventet {}: {} for OppgaveID: {}", clazz.getSimpleName(), verdi, oppgaveId);
            }
        }
        return null;
    }
}
