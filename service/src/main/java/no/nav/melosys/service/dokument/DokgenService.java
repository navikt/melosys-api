package no.nav.melosys.service.dokument;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.KopiMottaker;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.REPRESENTANT;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
import static org.springframework.util.StringUtils.hasText;

@Service
public class DokgenService {

    private final DokgenConsumer dokgenConsumer;
    private final DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper;
    private final JoarkFasade joarkFasade;
    private final DokgenMalMapper dokgenMalMapper;
    private final BehandlingService behandlingService;
    private final EregFasade eregFasade;
    private final KontaktopplysningService kontaktopplysningService;
    private final BrevmottakerService brevmottakerService;
    private final ProsessinstansService prosessinstansService;

    @Autowired
    public DokgenService(DokgenConsumer dokgenConsumer, DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper,
                         @Qualifier("system") JoarkFasade joarkFasade,
                         DokgenMalMapper dokgenMalMapper, BehandlingService behandlingService,
                         @Qualifier("system") EregFasade eregFasade,
                         KontaktopplysningService kontaktopplysningService,
                         BrevmottakerService brevmottakerService, ProsessinstansService prosessinstansService) {
        this.dokgenConsumer = dokgenConsumer;
        this.dokumentproduksjonsInfoMapper = dokumentproduksjonsInfoMapper;
        this.joarkFasade = joarkFasade;
        this.dokgenMalMapper = dokgenMalMapper;
        this.behandlingService = behandlingService;
        this.eregFasade = eregFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.brevmottakerService = brevmottakerService;
        this.prosessinstansService = prosessinstansService;
    }

    public byte[] produserUtkast(Produserbaredokumenter produserbartdokument, long behandlingId,
                                 String orgnr, BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        Aktoer mottaker = brevmottakerService.avklarMottakere(produserbartdokument, Mottaker.av(brevbestillingDto.getMottaker()), behandling, true, false).get(0);

        if (mottaker.erOrganisasjon() && mottaker.getRolle() == REPRESENTANT) {
            orgnr = mottaker.getOrgnr();
        }

        return produserBrev(produserbartdokument, behandlingId, orgnr, brevbestillingDto, true);
    }

    public byte[] produserBrev(Produserbaredokumenter produserbartdokument, long behandlingId,
                               String orgnr, BrevbestillingDto brevbestilling) throws FunksjonellException, TekniskException {
        return produserBrev(produserbartdokument, behandlingId, orgnr, brevbestilling, false);
    }

    public byte[] produserBrev(Produserbaredokumenter produserbartdokument, long behandlingId,
                               String orgnr, BrevbestillingDto brevbestillingDto, boolean bestillKopi) throws FunksjonellException, TekniskException {
        DokgenBrevbestilling.Builder<?> brevbestilling = new DokgenBrevbestilling.Builder<>();

        if (List.of(MANGELBREV_ARBEIDSGIVER, MANGELBREV_BRUKER).contains(produserbartdokument)) {
            brevbestilling = new MangelbrevBrevbestilling.Builder()
                .medInnledningFritekst(brevbestillingDto.getInnledningFritekst())
                .medManglerInfoFritekst(brevbestillingDto.getManglerFritekst())
                .medKontaktpersonNavn(brevbestillingDto.getKontaktpersonNavn());
        }

        brevbestilling
            .medProduserbartdokument(produserbartdokument)
            .medBehandlingId(behandlingId)
            .medBestillKopi(bestillKopi);

        if (hasText(orgnr)) {
            Aktoer mottaker = new Aktoer();
            mottaker.setOrgnr(orgnr);

            brevbestilling.medMottaker(mottaker);
        }


        return produserBrev(brevbestilling.build());
    }

    public byte[] produserBrev(DokgenBrevbestilling brevbestilling) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(brevbestilling.getBehandlingId());
        String malnavn = dokumentproduksjonsInfoMapper.hentMalnavn(brevbestilling.getProduserbartdokument());
        String orgnr = brevbestilling.getMottaker() != null ? brevbestilling.getMottaker().getOrgnr() : null;
        DokgenBrevbestilling.Builder<?> builder = brevbestilling.toBuilder();

        builder.medBehandling(behandling);

        if (hasText(orgnr)) {
            settOrganisasjonsOpplysninger(behandling, orgnr, builder);
        }

        settJournalpostOpplysninger(behandling, builder);

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(builder.build());

        return dokgenConsumer.lagPdf(malnavn, dokgenDto, brevbestilling.bestillKopi());
    }

    public void produserOgDistribuerBrev(Produserbaredokumenter produserbartDokument, long behandlingId,
                                         BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        List<Aktoer> mottakere = new ArrayList<>();
        if (hasText(brevbestillingDto.getOrgNr())) {
            Aktoer mottaker = new Aktoer();
            mottaker.setRolle(brevbestillingDto.getMottaker());
            mottaker.setOrgnr(brevbestillingDto.getOrgNr());
            mottakere.add(mottaker);
        } else {
            mottakere = brevmottakerService.avklarMottakere(produserbartDokument, Mottaker.av(brevbestillingDto.getMottaker()), behandling, false, false);
        }

        for (Aktoer aktoer : mottakere) {
            prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(produserbartDokument, behandling, aktoer, brevbestillingDto);
        }

        for (KopiMottaker kopiMottaker: brevbestillingDto.getKopiMottakere()) {
            Aktoer aktoer = new Aktoer();
            aktoer.setRolle(kopiMottaker.getRolle());
            aktoer.setOrgnr(kopiMottaker.getOrgnr());
            aktoer.setAktørId(kopiMottaker.getAktørId());
            prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(produserbartDokument, behandling, aktoer, brevbestillingDto, true);
        }
    }

    public DokumentproduksjonsInfo hentDokumentInfo(Produserbaredokumenter produserbartDokument) throws FunksjonellException {
        return dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(produserbartDokument);
    }

    boolean erTilgjengeligDokgenmal(Produserbaredokumenter produserbartDokument) {
        Set<Produserbaredokumenter> tilgjengeligeMaler = dokumentproduksjonsInfoMapper.utledTilgjengeligeMaler();
        return tilgjengeligeMaler.contains(produserbartDokument);
    }

    private void settOrganisasjonsOpplysninger(Behandling behandling, String orgnr,
                                               DokgenBrevbestilling.Builder<?> brevbestilling)
        throws IkkeFunnetException, IntegrasjonException {
        Kontaktopplysning kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), orgnr).orElse(null);
        String mottakerOrgnr = kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null ? kontaktopplysning.getKontaktOrgnr() : orgnr;
        brevbestilling
            .medOrg((OrganisasjonDokument) eregFasade.hentOrganisasjon(mottakerOrgnr).getDokument())
            .medKontaktopplysning(kontaktopplysning);
    }

    private void settJournalpostOpplysninger(Behandling behandling, DokgenBrevbestilling.Builder<?> brevbestilling) throws
        FunksjonellException, IntegrasjonException {
        Journalpost journalpost = joarkFasade.hentJournalpost(behandling.getInitierendeJournalpostId());
        brevbestilling
            .medForsendelseMottatt(journalpost.getForsendelseMottatt())
            .medAvsenderNavn(journalpost.getAvsenderNavn())
            .medAvsenderId(journalpost.getAvsenderId());
    }
}
