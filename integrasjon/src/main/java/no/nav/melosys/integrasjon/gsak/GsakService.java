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
import no.nav.melosys.domain.kodeverk.Sakstyper;
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
                    .put(ENDRET_PERIODE, "ae0052")
                    .put(ANKE, "ae0046")
                    .put(KLAGE, "ae0058")
                    .put(UTL_MYND_UTPEKT_NORGE, "ae0112")
                    .put(NY_VURDERING, "ae0028")
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

        if (SOEKNAD == behandlingstype) {
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
        OppgaveDto oppgave = oppgaveConsumer.hentOppgave(oppgaveID);
        if (oppgave == null) {
            throw new IkkeFunnetException("Oppgave med ID " + oppgaveID + " er ikke funnet.");
        } else {
            oppgave.setStatus(OPPGAVE_STATUS_FERDIGSTILT);
            oppgaveConsumer.oppdaterOppgave(oppgave);
        }
    }

    @Override
    public List<Oppgave> finnUtildelteOppgaverEtterFrist(Set<Oppgavetyper> oppgavetyper, Set<Sakstyper> sakstyper, Set<Behandlingstyper> behandlingstyper, Set<Behandlingstema> behandlingstemaer)
        throws FunksjonellException, TekniskException {

        if (oppgavetyper.contains(Oppgavetyper.BEH_SAK_MK) && behandlingstemaer.contains(Behandlingstema.EU_EOS)) {
            //Byttet kode for EU/EØS 4.10.2019. Må fortsatt kunne plukke med gammelt tema
            behandlingstemaer.add(Behandlingstema.EU_EOS_GAMMEL_KODE);
        }

        OppgaveSearchRequest.Builder searchRequestBuilder = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medOppgaveTyper(oppgavetyper.stream().map(Oppgavetyper::getKode).toArray(String[]::new))
            .medBehandlingsTyper(behandlingstyper.stream().map(GsakService::hentFellesKode).toArray(String[]::new))
            .medBehandlingstema(behandlingstemaer.stream().map(Behandlingstema::getKode).toArray(String[]::new))
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .medTildeltRessurs(false);

        if (!oppgavetyper.equals(Collections.singleton(Oppgavetyper.JFR))) {
            searchRequestBuilder.medBehandlesAvApplikasjon(Fagsystem.MELOSYS.getKode());
        }

        List<OppgaveDto> oppgaver = oppgaveConsumer.hentOppgaveListe(searchRequestBuilder.build());

        return oppgaver.stream().map(GsakService::oppgaveMappingDtoTilDomain).collect(Collectors.toList());
    }

    @Override
    public Oppgave hentOppgave(String oppgaveId) throws FunksjonellException, TekniskException {
        OppgaveDto gsakOppgave = oppgaveConsumer.hentOppgave(oppgaveId);

        if (gsakOppgave == null) {
            throw new IkkeFunnetException("Oppgave med oppgaveID " + oppgaveId + " finnes ikke.");
        }
        return oppgaveMappingDtoTilDomain(gsakOppgave);
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
            oppgaveDto.setFristFerdigstillelse(oppgave.lagFristFerdigstillelse(idag));
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
        OppgaveDto oppgave = oppgaveConsumer.hentOppgave(oppgaveId);
        if (oppgave == null) {
            throw new IkkeFunnetException("Feil ved henting av oppgave for oppgaveID:" + oppgaveId);
        }
        oppgave.setTilordnetRessurs(null);
        oppgaveConsumer.oppdaterOppgave(oppgave);
    }

    @Override
    public void oppdaterOppgavePrioritet(String oppgaveId, PrioritetType prioritet) throws FunksjonellException, TekniskException {
        OppgaveDto oppgave = oppgaveConsumer.hentOppgave(oppgaveId);
        if (oppgave == null) {
            throw new IkkeFunnetException("Feil ved henting av oppgave for oppgaveID:" + oppgaveId);
        }
        oppgave.setPrioritet(prioritet.name());
        oppgaveConsumer.oppdaterOppgave(oppgave);
    }

    @Override
    public List<Oppgave> finnOppgaveListeMedAnsvarlig(String tilordnetRessurs) throws FunksjonellException, TekniskException {
        OppgaveSearchRequest.Builder oppgaveSearchRequestBuilder = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medTilordnetRessurs(tilordnetRessurs)
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN);

        return hentOppgaverAlleTyper(oppgaveSearchRequestBuilder);
    }

    private List<Oppgave> hentOppgaverAlleTyper(OppgaveSearchRequest.Builder oppgaveSearchRequestBuilder) throws FunksjonellException, TekniskException {
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
            .collect(Collectors.toList());
    }

    @Override
    public List<Oppgave> finnBehandlingsoppgaverMedBruker(String aktørId) throws FunksjonellException, TekniskException {
        OppgaveSearchRequest oppgaveSearchRequest = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medAktørId(aktørId)
            .medOppgaveTyper(new String[]{Oppgavetyper.BEH_SAK_MK.getKode()})
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .medBehandlesAvApplikasjon(Fagsystem.MELOSYS.getKode())
            .build();

        return oppgaveConsumer.hentOppgaveListe(oppgaveSearchRequest).stream()
            .filter(Objects::nonNull)
            .map(GsakService::oppgaveMappingDtoTilDomain)
            .collect(Collectors.toList());
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
    public List<Oppgave> finnOppgaverMedSaksnummer(String saksnummer) throws FunksjonellException, TekniskException {
        OppgaveSearchRequest oppgaveSearchRequest = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medSaksreferanse(new String[]{saksnummer})
            .medTema(new String[] {Tema.MED.getKode(), Tema.UFM.getKode()})
            .medOppgaveTyper(new String[]{Oppgavetyper.BEH_SAK_MK.getKode(), Oppgavetyper.VUR.getKode(), Oppgavetyper.BEH_SED.getKode()})
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .build();

        List<OppgaveDto> finnOppgaveListeResponse = oppgaveConsumer.hentOppgaveListe(oppgaveSearchRequest);
        return finnOppgaveListeResponse.stream()
            .filter(Objects::nonNull)
            .map(GsakService::oppgaveMappingDtoTilDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void tildelOppgave(String oppgaveId, String saksbehandlerID) throws FunksjonellException, TekniskException {
        OppgaveDto oppgave = oppgaveConsumer.hentOppgave(oppgaveId);
        if (oppgave == null) {
            throw new IkkeFunnetException(String.format("Feil ved henting av "
                + "oppgave %s for saksbehandler %s:", oppgaveId, saksbehandlerID));
        }
        oppgave.setTilordnetRessurs(saksbehandlerID);
        oppgaveConsumer.oppdaterOppgave(oppgave);
    }

    @Override
    public void oppdaterOppgave(Oppgave oppgave) throws FunksjonellException, TekniskException {
        OppgaveDto oppgaveDto = oppgaveMappingDomainTilDto(oppgave);

        oppgaveConsumer.oppdaterOppgave(oppgaveDto);
    }

    static Oppgave oppgaveMappingDtoTilDomain(OppgaveDto oppgaveDto) {
        Oppgave.Builder domainOppgaveBuilder = new Oppgave.Builder();
        String oppgaveId = oppgaveDto.getId();
        domainOppgaveBuilder
                .setAktivDato(oppgaveDto.getAktivDato())
                .setAktørId(oppgaveDto.getAktørId())
                .setBeskrivelse(oppgaveDto.getBeskrivelse())
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
                log.error("Fikk uventet behandlingstype: {} for OppgaveID: {}", oppgaveDto.getBehandlingstype(), oppgaveDto.getId());
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
