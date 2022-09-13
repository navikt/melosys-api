package no.nav.melosys.integrasjon.oppgave;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.domain.util.KodeverkUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumer;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveDto;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OpprettOppgaveDto;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;
import static no.nav.melosys.integrasjon.Konstanter.NAV_VIKEN_ENHET_ID;

@Service
@Primary
public class OppgaveFasadeImpl implements OppgaveFasade {
    private static final Logger log = LoggerFactory.getLogger(OppgaveFasadeImpl.class);

    private static final String OPPGAVE_STATUS_FEILREGISTRERT = "FEILREGISTRERT";
    private static final String OPPGAVE_STATUS_FERDIGSTILT = "FERDIGSTILT";
    private static final String SORTERINGSFELT = "FRIST";
    private static final String SORTERINGSREKKEFOLGE_DESC = "DESC";
    private static final String OPPGAVE_STATUSKATEGORI_AAPEN = "AAPEN";
    private static final String OPPGAVE_STATUSKATEGORI_AVSLUTTET = "AVSLUTTET";

    private final OppgaveConsumer oppgaveConsumer;

    public OppgaveFasadeImpl(OppgaveConsumer oppgaveConsumer) {
        this.oppgaveConsumer = oppgaveConsumer;
    }

    @Override
    public void feilregistrerOppgaver(Set<Oppgave> oppgaveSet) {
        for (var oppgave : oppgaveSet) {
            feilregistrerOppgave(oppgave);
        }
    }

    @Override
    public void ferdigstillOppgave(String oppgaveID) {
        OppgaveDto oppgave = hentOppgaveDto(oppgaveID);
        oppgave.setStatus(OPPGAVE_STATUS_FERDIGSTILT);
        oppgaveConsumer.oppdaterOppgave(oppgave);
    }

    @Override
    public List<Oppgave> finnUtildelteOppgaverEtterFrist(String behandlingstema) {
        OppgaveSearchRequest.Builder searchRequestBuilder = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medBehandlingstema(behandlingstema)
            .medOppgaveTyper(hentGyldigeOppgavetyper())
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medTildeltRessurs(false)
            .medBehandlesAvApplikasjon(Fagsystem.MELOSYS.getKode());

        List<OppgaveDto> oppgaver = oppgaveConsumer.hentOppgaveListe(searchRequestBuilder.build());

        return oppgaver.stream().map(OppgaveFasadeImpl::oppgaveMappingDtoTilDomain)
            .filter(erGyldigBehandlingsoppgave)
            .toList();
    }

    /**
     * @deprecated Fjernes med toggle melosys.oppgave.oppretting
     */
    @Deprecated
    @Override
    public List<Oppgave> finnUtildelteOppgaverEtterFrist(String behandlingstype, String behandlingstema) {
        OppgaveSearchRequest.Builder searchRequestBuilder = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medBehandlingsType(behandlingstype)
            .medBehandlingstema(behandlingstema)
            .medOppgaveTyper(hentGyldigeOppgavetyper())
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medTildeltRessurs(false)
            .medBehandlesAvApplikasjon(Fagsystem.MELOSYS.getKode());

        List<OppgaveDto> oppgaver = oppgaveConsumer.hentOppgaveListe(searchRequestBuilder.build());

        return oppgaver.stream().map(OppgaveFasadeImpl::oppgaveMappingDtoTilDomain)
            .filter(erGyldigBehandlingsoppgave)
            .toList();
    }

    @Override
    public Oppgave hentOppgave(String oppgaveId) {
        return oppgaveMappingDtoTilDomain(hentOppgaveDto(oppgaveId));
    }

    @Override
    public String opprettOppgave(Oppgave oppgave) {
        return opprettOppgave(oppgave, false);
    }

    @Override
    public String opprettSensitivOppgave(Oppgave oppgave) {
        return opprettOppgave(oppgave, true);
    }

    @Async
    public void feilregistrerOppgave(Oppgave oppgave) {
        try {
            var oppgaveDto = OppgaveDto.av(oppgave);
            oppgaveDto.setStatus(OPPGAVE_STATUS_FEILREGISTRERT);
            oppgaveConsumer.oppdaterOppgave(oppgaveDto);
            log.info("Oppgave {} er feilregistrert", oppgaveDto.getId());
        } catch (TekniskException e) {
            log.error("Feilregistrering av oppgave {} feilet", oppgave.getOppgaveId(), e);
        }
    }

    private String opprettOppgave(Oppgave oppgave, boolean erSensitiv) {
        LocalDate idag = LocalDate.now();
        OpprettOppgaveDto oppgaveDto = new OpprettOppgaveDto();
        oppgaveDto.setAktivDato(idag);
        oppgaveDto.setAktørId(oppgave.getAktørId());
        oppgaveDto.setOrgnr(oppgave.getOrgnr());
        oppgaveDto.setBehandlingstema(oppgave.getBehandlingstema());
        oppgaveDto.setBehandlingstype(oppgave.getBehandlingstype());
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
        oppgaveDto.setTildeltEnhetsnr(Integer.toString(erSensitiv ? NAV_VIKEN_ENHET_ID : MELOSYS_ENHET_ID));
        if (oppgave.getBehandlesAvApplikasjon() != Fagsystem.INTET) {
            oppgaveDto.setBehandlesAvApplikasjon(oppgave.getBehandlesAvApplikasjon().getKode());
        }

        oppgaveDto.setTilordnetRessurs(oppgave.getTilordnetRessurs());

        return oppgaveConsumer.opprettOppgave(oppgaveDto);
    }

    @Override
    public void leggTilbakeOppgave(String oppgaveId) {
        OppgaveDto oppgave = hentOppgaveDto(oppgaveId);
        oppgave.setTilordnetRessurs(null);
        oppgaveConsumer.oppdaterOppgave(oppgave);
    }

    @Override
    public void oppdaterOppgave(String oppgaveID, OppgaveOppdatering oppgaveOppdatering) {
        OppgaveDto oppgaveDto = hentOppgaveDto(oppgaveID);

        if (oppgaveOppdatering.getOppgavetype() != null) {
            oppgaveDto.setOppgavetype(oppgaveOppdatering.getOppgavetype().getKode());
        }
        if (oppgaveOppdatering.getTema() != null) {
            oppgaveDto.setTema(oppgaveOppdatering.getTema().getKode());
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

        if (oppgaveOppdatering.getBehandlingstema() != null) {
            oppgaveDto.setBehandlingstema(oppgaveOppdatering.getBehandlingstema());
        }

        if (oppgaveOppdatering.getBehandlingstype() != null) {
            oppgaveDto.setBehandlingstype(oppgaveOppdatering.getBehandlingstype());
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

    private OppgaveDto hentOppgaveDto(String oppgaveID) {
        OppgaveDto oppgave = oppgaveConsumer.hentOppgave(oppgaveID);
        if (oppgave == null) {
            throw new IkkeFunnetException("Feil ved henting av oppgave for oppgaveID:" + oppgaveID);
        }

        return oppgave;
    }

    @Override
    public Set<Oppgave> finnOppgaverMedAnsvarlig(String tilordnetRessurs) {
        OppgaveSearchRequest.Builder oppgaveSearchRequestBuilder = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medTilordnetRessurs(tilordnetRessurs)
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medOppgaveTyper(hentGyldigeOppgavetyper())
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN);

        return hentOppgaverAlleTyper(oppgaveSearchRequestBuilder);
    }

    private Set<Oppgave> hentOppgaverAlleTyper(OppgaveSearchRequest.Builder oppgaveSearchRequestBuilder) {
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
            .map(OppgaveFasadeImpl::oppgaveMappingDtoTilDomain)
            .collect(Collectors.toSet());
    }

    @Override
    public List<Oppgave> finnOppgaverMedAktørId(String aktørId) {
        OppgaveSearchRequest oppgaveSearchRequest = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medAktørId(aktørId)
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medOppgaveTyper(hentGyldigeOppgavetyper())
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .build();

        return oppgaveConsumer.hentOppgaveListe(oppgaveSearchRequest).stream()
            .map(OppgaveFasadeImpl::oppgaveMappingDtoTilDomain)
            .toList();
    }

    @Override
    public List<Oppgave> finnOppgaverMedOrgnr(String orgnr) {
        OppgaveSearchRequest oppgaveSearchRequest = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medOrgnr(orgnr)
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medOppgaveTyper(hentGyldigeOppgavetyper())
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .build();

        return oppgaveConsumer.hentOppgaveListe(oppgaveSearchRequest).stream()
            .map(OppgaveFasadeImpl::oppgaveMappingDtoTilDomain)
            .toList();
    }

    @Override
    public List<Oppgave> finnÅpneOppgaverMedJournalpostID(String journalpostID) {
        OppgaveSearchRequest oppgaveSearchRequest = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medJournalpostID(new String[]{journalpostID})
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medOppgaveTyper(new String[]{Oppgavetyper.BEH_SAK_MK.getKode(), Oppgavetyper.VUR.getKode(), Oppgavetyper.BEH_SED.getKode()})
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .build();

        return oppgaveConsumer.hentOppgaveListe(oppgaveSearchRequest).stream()
            .map(OppgaveFasadeImpl::oppgaveMappingDtoTilDomain)
            .toList();
    }

    @Override
    public List<Oppgave> finnÅpneOppgaverMedSaksnummer(String saksnummer) {
        OppgaveSearchRequest oppgaveSearchRequest = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medSaksreferanse(new String[]{saksnummer})
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medOppgaveTyper(new String[]{Oppgavetyper.BEH_SAK_MK.getKode(), Oppgavetyper.VUR.getKode(), Oppgavetyper.BEH_SED.getKode()})
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .build();

        return oppgaveConsumer.hentOppgaveListe(oppgaveSearchRequest).stream()
            .map(OppgaveFasadeImpl::oppgaveMappingDtoTilDomain)
            .toList();
    }

    @Override
    public List<Oppgave> finnAvsluttetOppgaverMedSaksnummer(String saksnummer) {
        OppgaveSearchRequest oppgaveSearchRequest = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medSaksreferanse(new String[]{saksnummer})
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medOppgaveTyper(new String[]{Oppgavetyper.BEH_SAK_MK.getKode(), Oppgavetyper.VUR.getKode(), Oppgavetyper.BEH_SED.getKode()})
            .medSorteringsfelt(SORTERINGSFELT)
            .medSorteringsrekkefolge(SORTERINGSREKKEFOLGE_DESC)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AVSLUTTET)
            .build();

        return oppgaveConsumer.hentOppgaveListe(oppgaveSearchRequest).stream()
            .map(OppgaveFasadeImpl::oppgaveMappingDtoTilDomain)
            .toList();
    }

    static Oppgave oppgaveMappingDtoTilDomain(OppgaveDto oppgaveDto) {
        Oppgave.Builder domainOppgaveBuilder = new Oppgave.Builder();
        String oppgaveId = oppgaveDto.getId();
        domainOppgaveBuilder
            .setAktivDato(oppgaveDto.getAktivDato())
            .setAktørId(oppgaveDto.getAktørId())
            .setOrgnr(oppgaveDto.getOrgnr())
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
            .setBehandlingstema(oppgaveDto.getBehandlingstema())
            .setTema(mapTilEnumFraKode(Tema.class, oppgaveDto.getTema(), oppgaveId))
            .setOppgavetype(mapTilEnumFraKode(Oppgavetyper.class, oppgaveDto.getOppgavetype(), oppgaveId))
            .setPrioritet(StringUtils.isNotEmpty(oppgaveDto.getPrioritet()) ? PrioritetType.valueOf(oppgaveDto.getPrioritet()) : null)
            .setBehandlesAvApplikasjon(mapTilEnumFraKode(Fagsystem.class, StringUtils.defaultString(oppgaveDto.getBehandlesAvApplikasjon()), oppgaveId));

        return domainOppgaveBuilder.build();
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

    private final Predicate<Oppgave> erGyldigBehandlingsoppgave
        = oppgave -> oppgave.getSaksnummer() != null;

    private static String[] hentGyldigeOppgavetyper() {
        return Stream.of(Oppgavetyper.values())
            .map(Oppgavetyper::getKode)
            .toArray(String[]::new);
    }
}
