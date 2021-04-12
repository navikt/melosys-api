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

    public byte[] produserUtkast(long behandlingId, BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {
        Produserbaredokumenter produserbartdokument = brevbestillingDto.getProduserbardokument();
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        Aktoer mottaker;
        if (hasText(brevbestillingDto.getOrgNr())) {
            mottaker = new Aktoer();
            mottaker.setRolle(brevbestillingDto.getMottaker());
            mottaker.setOrgnr(brevbestillingDto.getOrgNr());
        } else {
            mottaker = brevmottakerService.avklarMottakere(produserbartdokument,
                Mottaker.av(brevbestillingDto.getMottaker()), behandling, true, false).get(0);
        }

        DokgenBrevbestilling.Builder<?> brevbestilling = new DokgenBrevbestilling.Builder<>();

        if (List.of(MANGELBREV_ARBEIDSGIVER, MANGELBREV_BRUKER).contains(produserbartdokument)) {
            brevbestilling = new MangelbrevBrevbestilling.Builder()
                .medInnledningFritekst(brevbestillingDto.getInnledningFritekst())
                .medManglerInfoFritekst(brevbestillingDto.getManglerFritekst())
                .medKontaktperson(brevbestillingDto.getKontaktperson());
        }

        brevbestilling
            .medProduserbartdokument(produserbartdokument)
            .medBehandlingId(behandlingId)
            .medBestillKopi(true);

        return produserBrev(mottaker, brevbestilling.build());
    }

    public byte[] produserBrev(Aktoer mottaker, DokgenBrevbestilling brevbestilling) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(brevbestilling.getBehandlingId());
        String malnavn = dokumentproduksjonsInfoMapper.hentMalnavn(brevbestilling.getProduserbartdokument());
        String orgnr = mottaker != null ? mottaker.getOrgnr() : null;
        DokgenBrevbestilling.Builder<?> builder = brevbestilling.toBuilder();

        builder.medBehandling(behandling);

        if (hasText(orgnr)) {
            settOrganisasjonsOpplysninger(behandling, orgnr, builder);
        }

        settJournalpostOpplysninger(behandling, builder);

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(builder.build());

        return dokgenConsumer.lagPdf(malnavn, dokgenDto, brevbestilling.bestillKopi());
    }

    public void produserOgDistribuerBrev(long behandlingId, BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {
        Produserbaredokumenter produserbartDokument = brevbestillingDto.getProduserbardokument();
        Behandling behandling = behandlingService.hentBehandling(behandlingId);

        DokgenBrevbestilling.Builder<?> brevbestilling = new DokgenBrevbestilling.Builder<>();

        brevbestilling
            .medProduserbartdokument(produserbartDokument)
            .medBehandlingId(behandlingId);

        if (List.of(MANGELBREV_ARBEIDSGIVER, MANGELBREV_BRUKER).contains(produserbartDokument)) {
            brevbestilling = new MangelbrevBrevbestilling.Builder()
                .medInnledningFritekst(brevbestillingDto.getInnledningFritekst())
                .medManglerInfoFritekst(brevbestillingDto.getManglerFritekst())
                .medKontaktperson(brevbestillingDto.getKontaktperson());
        }

        List<Aktoer> mottakere = new ArrayList<>();
        if (hasText(brevbestillingDto.getOrgNr())) {
            Aktoer mottaker = new Aktoer();
            mottaker.setRolle(brevbestillingDto.getMottaker());
            mottaker.setOrgnr(brevbestillingDto.getOrgNr());
            mottakere.add(mottaker);
        } else {
            mottakere = brevmottakerService.avklarMottakere(produserbartDokument,
                Mottaker.av(brevbestillingDto.getMottaker()), behandling, false, false);
        }

        for (Aktoer aktoer : mottakere) {
            produserOgDistribuerBrev(behandling, aktoer, brevbestilling.build());
        }

        for (KopiMottaker kopiMottaker : brevbestillingDto.getKopiMottakere()) {
            Aktoer aktoer = new Aktoer();
            aktoer.setRolle(kopiMottaker.getRolle());
            aktoer.setOrgnr(kopiMottaker.getOrgnr());
            aktoer.setAktørId(kopiMottaker.getAktørId());
            brevbestilling.medBestillKopi(true);
            produserOgDistribuerBrev(behandling, aktoer, brevbestilling.build());
        }
    }

    private void produserOgDistribuerBrev(Behandling behandling, Aktoer mottaker, DokgenBrevbestilling brevbestilling) {
        prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(behandling, mottaker, brevbestilling);
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
