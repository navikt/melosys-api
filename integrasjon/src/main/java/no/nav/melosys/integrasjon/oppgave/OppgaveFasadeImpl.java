package no.nav.melosys.integrasjon.oppgave;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;
import static no.nav.melosys.integrasjon.Konstanter.NAV_VIKEN_ENHET_ID;

@Service
@Primary
public class OppgaveFasadeImpl implements OppgaveFasade {
    private static final Logger log = LoggerFactory.getLogger(OppgaveFasadeImpl.class);

    private static final String OPPGAVE_STATUS_FERDIGSTILT = "FERDIGSTILT";
    private static final String SORTERINGSFELT = "FRIST";
    private static final String OPPGAVE_STATUSKATEGORI_AAPEN = "AAPEN";
    private static final String NY_VURDERING_BEHANDLINGTYPEKODE = "ae0240";
    private static final String ENDRET_PERIODE_BEHANDLINGSTYPEKODE = "ae0052";

    private static final String EUEOS_BEHANDLINGSTEMAKODE = "ab0424";
    private static final String EUEOS_BEHANDLINGSTEMAKODE_GAMMEL = "ab0390";


    private static final ImmutableMap<Behandlingstema, String> BEHANDLINGSTYPE_FELLESKODE_MAP =
        ImmutableMap.<Behandlingstema, String>builder()
            .put(UTSENDT_ARBEIDSTAKER, "ae0034")
            .put(UTSENDT_SELVSTENDIG, "ae0034")
            .put(IKKE_YRKESAKTIV, "ae0238")
            //.put(ENDRET_PERIODE, "ae0052")
            //.put(ANKE, "ae0046")
            //.put(KLAGE, "ae0058")
            .put(ARBEID_ETT_LAND_ØVRIG, "ae0243")
            .put(BESLUTNING_LOVVALG_NORGE, "ae0112")
            .put(BESLUTNING_LOVVALG_ANNET_LAND, "ae0113")
            .put(ANMODNING_OM_UNNTAK_HOVEDREGEL, "ae0110")
            .put(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, "ae0111")
            .put(REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, "ae0235")
            .put(ARBEID_FLERE_LAND, "ae0242")
            .put(ØVRIGE_SED_MED, "ae0254")
            .put(ØVRIGE_SED_UFM, "ae0254")
            .put(TRYGDETID, "ae0236")
            .build();


    private final OppgaveConsumer oppgaveConsumer;

    @Autowired
    public OppgaveFasadeImpl(OppgaveConsumer oppgaveConsumer) {
        this.oppgaveConsumer = oppgaveConsumer;
    }

    @Override
    public void ferdigstillOppgave(String oppgaveID) throws FunksjonellException, TekniskException {
        OppgaveDto oppgave = hentOppgaveDto(oppgaveID);
        oppgave.setStatus(OPPGAVE_STATUS_FERDIGSTILT);
        oppgaveConsumer.oppdaterOppgave(oppgave);
    }

    @Override
    public List<Oppgave> finnUtildelteOppgaverEtterFrist(Behandlingstema behandlingstema)
        throws FunksjonellException, TekniskException {

        OppgaveSearchRequest.Builder searchRequestBuilder = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medBehandlingsType(hentFellesKode(behandlingstema))
            .medBehandlingstema(EUEOS_BEHANDLINGSTEMAKODE)
            .medOppgaveTyper(hentGyldigeOppgavetyper())
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medTildeltRessurs(false);

        List<OppgaveDto> oppgaver = oppgaveConsumer.hentOppgaveListe(searchRequestBuilder.build());

        if (erBehandlingstypeUtsending(behandlingstema)) {

            oppgaver.addAll(oppgaveConsumer.hentOppgaveListe(
                searchRequestBuilder.medBehandlingstema(EUEOS_BEHANDLINGSTEMAKODE_GAMMEL).build()
            ));

            oppgaver.addAll(oppgaveConsumer.hentOppgaveListe(
                searchRequestBuilder
                    .medBehandlingstema(EUEOS_BEHANDLINGSTEMAKODE)
                    .medBehandlingsType(NY_VURDERING_BEHANDLINGTYPEKODE)
                    .build()
            ));

            oppgaver.addAll(oppgaveConsumer.hentOppgaveListe(
                searchRequestBuilder
                    .medBehandlingstema(EUEOS_BEHANDLINGSTEMAKODE)
                    .medBehandlingsType(ENDRET_PERIODE_BEHANDLINGSTYPEKODE)
                    .build()
            ));
        }

        return oppgaver.stream().map(OppgaveFasadeImpl::oppgaveMappingDtoTilDomain)
            .filter(erGyldigJournalføringEllerBehandlingsoppgave)
            .collect(Collectors.toList());
    }

    private boolean erBehandlingstypeUtsending(Behandlingstema behandlingstema) {
        return behandlingstema == UTSENDT_ARBEIDSTAKER || behandlingstema == UTSENDT_SELVSTENDIG;
    }

    @Override
    public Oppgave hentOppgave(String oppgaveId) throws FunksjonellException, TekniskException {
        return oppgaveMappingDtoTilDomain(hentOppgaveDto(oppgaveId));
    }

    @Override
    public String opprettOppgave(Oppgave oppgave) throws FunksjonellException, TekniskException {
        return opprettOppgave(oppgave, false);
    }

    @Override
    public String opprettSensitivOppgave(Oppgave oppgave) throws FunksjonellException, TekniskException {
        return opprettOppgave(oppgave, true);
    }

    private String opprettOppgave(Oppgave oppgave, boolean erSensitiv) throws FunksjonellException, TekniskException {
        LocalDate idag = LocalDate.now();
        OpprettOppgaveDto oppgaveDto = new OpprettOppgaveDto();
        oppgaveDto.setAktivDato(idag);
        oppgaveDto.setAktørId(oppgave.getAktørId());
        if (oppgave.getBehandlingstype() != null) {
            oppgaveDto.setBehandlingstype(hentFellesKode(oppgave.getBehandlingstema()));
        }
        if (oppgave.getBehandlingstema() != null) {
            oppgaveDto.setBehandlingstema(EUEOS_BEHANDLINGSTEMAKODE);
            //oppgaveDto.setBehandlingstema(oppgave.getBehandlingstema().getKode()); todo: mapping til nytt kodeverk når det kommer..
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
        oppgaveDto.setTildeltEnhetsnr(Integer.toString(erSensitiv ? NAV_VIKEN_ENHET_ID : MELOSYS_ENHET_ID));
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
    public void oppdaterOppgave(String oppgaveID, OppgaveOppdatering oppgaveOppdatering) throws FunksjonellException, TekniskException {
        OppgaveDto oppgaveDto = hentOppgaveDto(oppgaveID);

        if (oppgaveOppdatering.getOppgavetype() != null) {
            oppgaveDto.setOppgavetype(oppgaveOppdatering.getOppgavetype().getKode());
        }
        if (oppgaveOppdatering.getTema() != null) {
            oppgaveDto.setTema(oppgaveOppdatering.getTema().getKode());
        }
        if (oppgaveOppdatering.getBehandlingstema() != null) {
            oppgaveDto.setBehandlingstype(hentFellesKode(oppgaveOppdatering.getBehandlingstema()));
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
    public Set<Oppgave> finnOppgaverMedAnsvarlig(String tilordnetRessurs) throws FunksjonellException, TekniskException {
        OppgaveSearchRequest.Builder oppgaveSearchRequestBuilder = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medTilordnetRessurs(tilordnetRessurs)
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medOppgaveTyper(hentGyldigeOppgavetyper())
            .medSorteringsfelt(SORTERINGSFELT)
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
            .map(OppgaveFasadeImpl::oppgaveMappingDtoTilDomain)
            .collect(Collectors.toSet());
    }

    @Override
    public List<Oppgave> finnOppgaverMedBrukerID(String aktørId) throws FunksjonellException, TekniskException {
        OppgaveSearchRequest oppgaveSearchRequest = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medAktørId(aktørId)
            .medTema(new String[]{Tema.MED.getKode(), Tema.UFM.getKode()})
            .medOppgaveTyper(hentGyldigeOppgavetyper())
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .build();

        return oppgaveConsumer.hentOppgaveListe(oppgaveSearchRequest).stream()
            .map(OppgaveFasadeImpl::oppgaveMappingDtoTilDomain)
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
            .map(OppgaveFasadeImpl::oppgaveMappingDtoTilDomain)
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
            /*todo, ikke bruke lenger - kan ikke mappes tilbake hvis vi bruker melosys behandlingstema?
            .setBehandlingstema(mapTilEnumFraKode(OppgaveBehandlingstema.class, oppgaveDto.getBehandlingstema(), oppgaveId))*/
            .setTema(mapTilEnumFraKode(Tema.class, oppgaveDto.getTema(), oppgaveId))
            .setOppgavetype(mapTilEnumFraKode(Oppgavetyper.class, oppgaveDto.getOppgavetype(), oppgaveId))
            .setPrioritet(StringUtils.isNotEmpty(oppgaveDto.getPrioritet()) ? PrioritetType.valueOf(oppgaveDto.getPrioritet()) : null)
            .setBehandlesAvApplikasjon(mapTilEnumFraKode(Fagsystem.class, StringUtils.defaultString(oppgaveDto.getBehandlesAvApplikasjon()), oppgaveId));

        return domainOppgaveBuilder.build();
    }

    /**
     * Henter koder fra felleskodeverk: Behandlingstyper.
     */
    private static String hentFellesKode(Behandlingstema behandlingstema) {
        if (BEHANDLINGSTYPE_FELLESKODE_MAP.containsKey(behandlingstema)) {
            return BEHANDLINGSTYPE_FELLESKODE_MAP.get(behandlingstema);
        }
        throw new IllegalArgumentException(behandlingstema + " er ikke implementert i felleskodeverk.");
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

    private final Predicate<Oppgave> erGyldigJournalføringEllerBehandlingsoppgave
        = oppgave -> oppgave.getOppgavetype() == Oppgavetyper.JFR || oppgave.getSaksnummer() != null;

    private static String[] hentGyldigeOppgavetyper() {
        return Stream.of(Oppgavetyper.values())
            .map(Oppgavetyper::getKode)
            .toArray(String[]::new);
    }
}
