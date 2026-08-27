package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

/**
 * Kastes når en skarp kjøring avvises av en sikkerhetssele. Controlleren gjør dette om til 400.
 */
class VedtaksmetadataFiksAvvist(melding: String) : RuntimeException(melding)

/**
 * Datafiks for MELOSYS-8174 (Q4a/Q4b i fiksplanen): setter inn manglende rader i `vedtak_metadata`
 * for avsluttede behandlinger som blokkerer skattepliktig årsavregning.
 *
 * `ÅrsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning` kaller
 * `hentVedtakMetadata()` ubetinget i filter/sortering, så én rad uten vedtaksmetadata velter hele
 * saken med «vedtakMetadata er påkrevd for Behandlingsresultat» før den blir faglig vurdert.
 *
 * Fiksen kjøres med native SQL, ikke via JPA, av to grunner:
 *  - `registrert_av`/`endret_av` må bli [PATCH_MARKØR] slik at fiksen kan rulles tilbake ([angre]).
 *    JPA-auditing ville satt saksbehandler/«MELOSYS» i stedet.
 *  - `vedtak_dato` skal være proxyen `behandlingsresultat.endret_dato`, ikke `Instant.now()` som
 *    `Behandlingsresultat.settVedtakMetadata` bruker.
 *
 * Innsettingen er idempotent (`NOT EXISTS`), så en utilsiktet ny kjøring gir null nye rader.
 *
 * Q-numrene i denne fila (Q4a, Q4b, Q6a) kommer fra arbeidsøkta med fag, der fiksen først ble
 * kjørt som løse SQL-spørringer mot prod. Se `vedtaksmetadata_fiksplan.md` i samme pakke for hva
 * numrene betyr, spørringene slik de ble kjørt for hånd, og forbeholdene rundt proxy-datoen.
 */
@Component
class VedtaksmetadataFiksService {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    /** Q4a — read-only preview av nøyaktig hvilke rader [utfør] vil sette inn. */
    @Transactional(readOnly = true)
    fun forhåndsvis(
        saksnummer: List<String>,
        tillatSorteringsendring: List<String> = emptyList(),
    ): VedtaksmetadataFiksResultat {
        val kandidater = hentKandidater(saksnummer)
        val påvirkning = sorteringspåvirkning(saksnummer)
        return VedtaksmetadataFiksResultat(
            skarp = false,
            saksnummer = saksnummer,
            antallRaderFunnet = kandidater.size,
            antallRaderInnsatt = 0,
            rader = kandidater,
            utenMetadataPerSak = tellUtenMetadata(saksnummer),
            ukjentBehType = kandidater.filterNot { it.behType in KJENTE_BEH_TYPER }.map { it.behandlingsresultatId },
            sorteringspåvirkning = påvirkning,
            saksnummerUtenKandidater = finnSaksnummerUtenKandidater(saksnummer, kandidater),
            kvitteringerUtenTreff = finnKvitteringerUtenTreff(tillatSorteringsendring, påvirkning),
        )
    }

    /**
     * Q4b — datafiksen. Endrer prod.
     *
     * Tre sikkerhetsseler, alle bevisste etter review 20.08:
     *  - tomt scope gjør ingenting i stedet for å falle tilbake på en default,
     *  - kandidatantallet må ligge innenfor [maksAntallRader] (samme rolle som `maksAntall` på `/run`),
     *  - alle kandidater må ha en `beh_type` vi vet hvilken vedtakstype hører til; ellers ville
     *    ELSE-grenen i SQL-en stilltiende skrevet ENDRINGSVEDTAK på f.eks. en KLAGE,
     *  - ingen sak der patchen tar nyeste-plassen i vedtaksdato-sorteringen (se
     *    [sorteringspåvirkning]) — det bytter hvilken behandling avgiftsgrunnlaget hentes fra, og
     *    må enten løses med ekte vedtaksdato eller kvitteres ut i [tillatSorteringsendring].
     *
     * [tillatSorteringsendring] er en liste med saksnummer, ikke et av/på-flagg. Prod-populasjonen
     * er nettopp formen der ett av flere saksnummer i samme kall kaprer nyeste-plassen: et flagg
     * ville tvunget operatøren til å slå av selen for hele kallet — også for sakene som faktisk
     * skulle vurderes manuelt — eller til å kjøre én sak av gangen. Her kvitteres saken ut, ikke
     * selen.
     */
    @Transactional
    fun utfør(
        saksnummer: List<String>,
        maksAntallRader: Int,
        tillatSorteringsendring: List<String> = emptyList(),
    ): VedtaksmetadataFiksResultat {
        if (saksnummer.isEmpty()) throw VedtaksmetadataFiksAvvist("Skarp kjøring krever eksplisitt saksnummer-liste")

        val kandidater = hentKandidater(saksnummer)
        if (kandidater.size > maksAntallRader) {
            throw VedtaksmetadataFiksAvvist(
                "Fant ${kandidater.size} rader, men maksAntallRader er $maksAntallRader. " +
                    "Kjør preview først, og hev maksAntallRader eksplisitt hvis tallet er riktig."
            )
        }
        // Etter maksAntallRader-selen, så den mer presise meldingen vinner i det vanlige tilfellet.
        // Denne fanger kun kjøringer der operatøren har hevet taket over Oracle-grensen.
        if (kandidater.size > MAKS_UTTRYKK_I_IN) {
            throw VedtaksmetadataFiksAvvist(
                "Fant ${kandidater.size} kandidater, men INSERT-en binder dem i en IN-liste, og Oracle " +
                    "tar maks $MAKS_UTTRYKK_I_IN uttrykk (ORA-01795). Del kjøringen opp i flere kall med " +
                    "færre saksnummer om gangen."
            )
        }
        val ukjente = kandidater.filterNot { it.behType in KJENTE_BEH_TYPER }
        if (ukjente.isNotEmpty()) {
            throw VedtaksmetadataFiksAvvist(
                "Kandidater med ukjent beh_type (vedtakstype kan ikke utledes trygt): " +
                    ukjente.joinToString { "${it.behandlingsresultatId}=${it.behType}" }
            )
        }

        // Måles her: etter de andre selene (ingen bortkastede spørringer på en kjøring som avvises),
        // men før INSERT-en — etterpå er patch-radene ekte rader, og «før»-bildet kan ikke gjenskapes.
        val påvirkning = sorteringspåvirkning(saksnummer)
        // Ett felt avgjør. patchenVinnerNyeste er sant hvis og bare hvis patchen legger seg på eller
        // over den nyeste datoen som kan være et ekte vedtak — se sorteringspåvirkning for hvorfor
        // det underlaget er det eneste selen måler mot.
        val kaprer = påvirkning.filter { it.patchenVinnerNyeste }
        val ikkeKvittert = kaprer.filterNot { it.saksnummer in tillatSorteringsendring }
        val kvitteringerUtenTreff = finnKvitteringerUtenTreff(tillatSorteringsendring, påvirkning)
        if (ikkeKvittert.isNotEmpty()) {
            throw VedtaksmetadataFiksAvvist(
                "Patchen ville tatt nyeste-plassen i vedtaksdato-sorteringen for " +
                    ikkeKvittert.joinToString { "${it.saksnummer} (${it.nyesteKandidatDato} mot ${it.nyesteSammenlignbareDato})" } +
                    ". Da bytter ÅrsavregningService hvilken behandling avgiftsgrunnlaget hentes fra. " +
                    "Sett ekte vedtaksdato manuelt, eller list saksnummeret i tillatSorteringsendring " +
                    "hvis endringen er vurdert og ønsket." +
                    if (kvitteringerUtenTreff.isEmpty()) "" else
                        " Merk at disse kvitteringene ikke traff noen sak som kaprer: $kvitteringerUtenTreff."
            )
        }

        // WARN er reservert hendelsen som faktisk fortjener oppmerksomhet: selen slo ut, og operatøren
        // kvitterte den ut. Det er den ene linja man trenger for å rekonstruere hvorfor en behandling
        // byttet plass som nyeste.
        kaprer.forEach {
            log.warn {
                "Datafiks $PATCH_MARKØR: sak ${it.saksnummer} patches selv om behandlingsresultat " +
                    "${it.nyesteKandidatId} (${it.nyesteKandidatDato}) tar nyeste-plassen fra " +
                    "${it.nyesteSammenlignbareId} (${it.nyesteSammenlignbareDato}) — kvittert ut i " +
                    "tillatSorteringsendring."
            }
        }

        // Resten av rekonstruksjonssporet på INFO. Patchen kan bli nyeste rad uten at selen slo ut —
        // typisk fordi den kun fortrenger en tidligere patch — og en sak helt uten vedtaksmetadata
        // treffer denne grenen hver eneste gang, så den hører ikke hjemme på WARN.
        påvirkning.filter { it.patchenBlirNyesteIHeleSaken && !it.patchenVinnerNyeste }.forEach {
            log.info {
                "Datafiks $PATCH_MARKØR: sak ${it.saksnummer} — behandlingsresultat ${it.nyesteKandidatId} " +
                    "(${it.nyesteKandidatDato}) blir nyeste rad i saken, men fortrenger ingen dato som " +
                    "kan være ekte (nyeste sammenlignbare: ${it.nyesteSammenlignbareDato ?: "ingen"})."
            }
        }

        log.info {
            "Datafiks $PATCH_MARKØR: setter inn ${kandidater.size} rader i vedtak_metadata for saker $saksnummer " +
                "(behandlingsresultat ${kandidater.map { it.behandlingsresultatId }})"
        }

        // Bundet til ID-ene fra forhåndsvisningen, ikke til kandidatfilteret på nytt (Copilot-review
        // 25.08): alle selene over er evaluert på nøyaktig disse radene, og Oracle er READ COMMITTED,
        // så et statement som revaluerte filteret ville fått sitt eget snapshot og kunne skrive en
        // kandidat som dukket opp etterpå uten å ha passert noen sele. Uten bindingen kan `avvik`
        // dessuten forbli false selv om mengden er en annen; nå er `antallInnsatt != kandidater.size`
        // eksakt. NB: selve kappløpet er ikke dekket av en regresjonstest — det ville krevd en
        // transaksjonssøm midt i metoden. Tom kandidatliste er derimot dekket: idempotens-ITen kjører
        // skarp en gang til og treffer `IN ()`-grenen.
        val antallInnsatt = entityManager.createNativeQuery(INSERT_SQL)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
            .setParameter("ider", kandidater.map { it.behandlingsresultatId })
            .setParameter("markoer", PATCH_MARKØR)
            .executeUpdate()

        val avvik = antallInnsatt != kandidater.size
        if (avvik) {
            log.warn {
                "Datafiks $PATCH_MARKØR: satte inn $antallInnsatt rader, men forhåndsvisningen viste " +
                    "${kandidater.size}. Noe har endret dataene under kjøringen — kontroller før du går videre."
            }
        } else {
            log.info { "Datafiks $PATCH_MARKØR: satte inn $antallInnsatt rader" }
        }

        return VedtaksmetadataFiksResultat(
            skarp = true,
            saksnummer = saksnummer,
            antallRaderFunnet = kandidater.size,
            antallRaderInnsatt = antallInnsatt,
            rader = kandidater,
            utenMetadataPerSak = tellUtenMetadata(saksnummer),
            avvik = avvik,
            sorteringspåvirkning = påvirkning,
            saksnummerUtenKandidater = finnSaksnummerUtenKandidater(saksnummer, kandidater),
            kvitteringerUtenTreff = kvitteringerUtenTreff,
        )
    }

    /**
     * Angreknappen. Sletter kun rader der BÅDE `registrert_av` og `endret_av` er [PATCH_MARKØR].
     *
     * `registrert_av` alene er ikke nok: den er `@CreatedBy` og settes kun ved insert, så en rad som
     * senere har fått en ekte vedtaksdato skrevet av en saksbehandler ville blitt slettet med
     * saksbehandlerens data. Slike rader telles i `antallSomIkkeKanAngres` og røres ikke.
     *
     * Tom `saksnummer`-liste betyr «alle markerte rader»; `skarp = false` viser hva som ville blitt slettet.
     *
     * Skarp kjøring uten scope krever [bekreftAlle]. Selen speiler den i [utfør], som avviser tomt
     * scope: uten den ruller et `{"skarp": true}` der `saksnummer` er glemt tilbake ALLE patch-rader
     * i basen, også fikser fra tidligere kjøringer — og hver slettet rad gjeninnfører nøyaktig
     * 8174-krasjen på saken den fikset. Muligheten beholdes, fordi «rull tilbake alt» er den
     * riktige nødbryteren, men den skal være et bevisst valg og ikke en default.
     */
    @Transactional
    fun angre(saksnummer: List<String>, skarp: Boolean, bekreftAlle: Boolean = false): VedtaksmetadataAngreResultat {
        if (skarp && saksnummer.isEmpty() && !bekreftAlle) {
            throw VedtaksmetadataFiksAvvist(
                "Skarp angre uten saksnummer sletter alle rader merket $PATCH_MARKØR, også fra tidligere " +
                    "kjøringer. Angi saksnummer, eller send bekreftAlle=true hvis du faktisk vil rulle tilbake alt."
            )
        }

        val kandidater = hentAngreKandidater(saksnummer)
        val kanIkkeAngres = tellSomIkkeKanAngres(saksnummer)

        if (!skarp) {
            return VedtaksmetadataAngreResultat(
                skarp = false,
                saksnummer = saksnummer,
                antallRaderFunnet = kandidater.size,
                antallSlettet = 0,
                rader = kandidater,
                antallSomIkkeKanAngres = kanIkkeAngres,
            )
        }

        log.info {
            "Datafiks $PATCH_MARKØR: ruller tilbake ${kandidater.size} rader " +
                "(scope=${saksnummer.ifEmpty { listOf("ALLE") }}, urørt fordi de er endret etterpå: $kanIkkeAngres)"
        }

        val sql = if (saksnummer.isEmpty()) ANGRE_SQL else ANGRE_SQL + ANGRE_SAKSFILTER
        val query = entityManager.createNativeQuery(sql).setParameter("markoer", PATCH_MARKØR)
        if (saksnummer.isNotEmpty()) query.setParameter("saksnummer", saksnummer)
        val antallSlettet = query.executeUpdate()

        log.info { "Datafiks $PATCH_MARKØR: slettet $antallSlettet rader" }

        return VedtaksmetadataAngreResultat(
            skarp = true,
            saksnummer = saksnummer,
            antallRaderFunnet = kandidater.size,
            antallSlettet = antallSlettet,
            rader = kandidater,
            antallSomIkkeKanAngres = kanIkkeAngres,
            avvik = antallSlettet != kandidater.size,
        )
    }

    /**
     * Saksnummer i requesten som ikke ga én eneste kandidatrad.
     *
     * Uten dette feltet er en skrivefeil usynlig: sender du fem saker og får «4 rader innsatt»,
     * kan du ikke se fra svaret at én av dem var feilskrevet og aldri ble rørt. Feltet skiller ikke
     * mellom «saken finnes ikke» og «saken har ingen defekte rader» — begge deler er verdt et blikk
     * når du trodde saken skulle fikses.
     */
    private fun finnSaksnummerUtenKandidater(saksnummer: List<String>, kandidater: List<VedtaksmetadataFiksRad>): List<String> {
        val truffet = kandidater.map { it.saksnummer }.toSet()
        return saksnummer.filterNot { it in truffet }
    }

    /**
     * Kvitteringer i `tillatSorteringsendring` som ikke tilsvarer noen sak der selen faktisk slår ut.
     *
     * Nesten alltid en skrivefeil, og ellers usynlig: operatøren ser bare at kjøringen ble avvist, og
     * kan ikke lese ut av svaret hvilken oppføring som var feil. Beregnes også i forhåndsvisningen —
     * en skrivefeil-detektor som først fyrer på kjøringen som endrer prod er lite verdt.
     */
    private fun finnKvitteringerUtenTreff(
        tillatSorteringsendring: List<String>,
        påvirkning: List<SorteringspåvirkningRad>,
    ): List<String> {
        val kaprer = påvirkning.filter { it.patchenVinnerNyeste }.map { it.saksnummer }.toSet()
        return tillatSorteringsendring.distinct().filterNot { it in kaprer }
    }

    @Suppress("UNCHECKED_CAST")
    private fun hentKandidater(saksnummer: List<String>): List<VedtaksmetadataFiksRad> {
        if (saksnummer.isEmpty()) return emptyList()

        val rader = entityManager.createNativeQuery(KANDIDAT_SQL)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
            .resultList as List<Array<Any?>>

        return rader.map { rad ->
            VedtaksmetadataFiksRad(
                saksnummer = rad[0] as String,
                behandlingsresultatId = (rad[1] as Number).toLong(),
                behType = rad[2] as String?,
                resultatType = rad[3] as String?,
                blirVedtakDato = rad[4] as String?,
                blirKlagefrist = rad[5] as String?,
                blirVedtakType = rad[6] as String,
            )
        }
    }

    /**
     * Viser hva patchen gjør med vedtaksdato-sorteringen, per sak.
     *
     * `vedtak_dato` er ikke dekorasjon: `ÅrsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning`
     * sorterer på den og plukker den siste som `sisteBehandlingsresultatMedAvgift` — altså behandlingen
     * avgiftsgrunnlaget hentes fra. Fiksen bruker `endret_dato` som proxy, og den er `@LastModifiedDate`:
     * alltid ≥ ekte vedtaksdato. Patchede rader ser derfor systematisk nyere ut enn de er.
     *
     * **Én mengde styrer én beslutning.** Selen finnes for å hindre at en oppdiktet dato fortrenger et
     * vedtak som kan være ekte, og sammenligner derfor kun mot [Datoopphav.EKTE] og
     * [Datoopphav.PATCHET_ENDRET] — datoene som *kan* stamme fra et vedtak. Rader vi selv satte inn og
     * som ingen har rørt siden er beviselig vår egen proxy; de rapporteres, men styrer ingenting.
     *
     * Det var nettopp blandingen som gjorde de tre foregående rundene med fikser til nye feil: ett felt
     * målte over alle rader, et annet over bare de ekte, og selen var en `&&` av begge. Nå er
     * [SorteringspåvirkningRad.patchenVinnerNyeste] alene nok til å avgjøre, og de øvrige feltene er
     * rapport.
     *
     * Merk hva dette *ikke* er: ingen simulering av avgiftsgrunnlaget. Den faktiske utvelgelsen filtrerer
     * også på år og periodeoverlapp, og sammenligningen her er global maks mot global maks. `true` er
     * derfor en pålitelig grunn til å stoppe, mens `false` ikke er et frikjenn: taper patchen mot en rad
     * som årsfilteret luker bort, kan den fortsatt vinne der det teller. Derfor listes alle datoene.
     *
     * Saker uten kandidater er ikke med i lista — send N saksnummer og du kan få M ≤ N rader. Bruk
     * [VedtaksmetadataFiksResultat.utenMetadataPerSak] for å krysskontrollere.
     */
    private fun sorteringspåvirkning(saksnummer: List<String>): List<SorteringspåvirkningRad> {
        if (saksnummer.isEmpty()) return emptyList()

        val eksisterendePerSak = raderPerSak(EKSISTERENDE_NYESTE_SQL, saksnummer, medMarkoer = true)
        val kandidaterPerSak = raderPerSak(PATCH_NYESTE_SQL, saksnummer, medMarkoer = false)

        return kandidaterPerSak.keys.sorted().map { sak ->
            val eksisterende = eksisterendePerSak[sak].orEmpty()
            val nyesteKandidat = kandidaterPerSak.getValue(sak).first()

            fun datoer(opphav: Datoopphav) =
                eksisterende.filter { it.opphav == opphav }.mapNotNull { it.visning }

            // Grunnlaget selen måler mot: alt som kan være en ekte vedtaksdato. Rader uten dato er
            // ikke med — ÅrsavregningService sorterer dem først i stigende rekkefølge, altså som de
            // eldste, så de vinner aldri .lastOrNull() og er ikke noe sammenligningsgrunnlag.
            val nyesteSammenlignbare = eksisterende
                .filter { it.opphav != Datoopphav.PATCHET_URØRT && it.sortering != null }
                .maxByOrNull { it.sortering!! }

            SorteringspåvirkningRad(
                saksnummer = sak,
                nyesteKandidatId = nyesteKandidat.behandlingsresultatId,
                nyesteKandidatDato = nyesteKandidat.visning,
                nyesteSammenlignbareId = nyesteSammenlignbare?.behandlingsresultatId,
                nyesteSammenlignbareDato = nyesteSammenlignbare?.visning,
                // >= og ikke >: ved eksakt likt tidsstempel er utfallet i den ekte sorteringen
                // vilkårlig (stabil sortering på behandlingsrekkefølgen), og et myntkast skal
                // flagges, ikke rapporteres som «patchen taper».
                patchenVinnerNyeste = nyesteSammenlignbare != null &&
                    requireNotNull(nyesteKandidat.sortering) >= nyesteSammenlignbare.sortering!!,
                // Uavhengig av selen: havner patchen øverst blant ALLE rader, endrer kjøringen
                // hvilken behandling som er nyest — også når den kun fortrenger en tidligere patch.
                // Logges alltid, slik at en kjøring kan rekonstrueres i ettertid.
                patchenBlirNyesteIHeleSaken = eksisterende
                    .mapNotNull { it.sortering }
                    .maxOrNull()
                    ?.let { requireNotNull(nyesteKandidat.sortering) >= it } ?: true,
                ekteDatoer = datoer(Datoopphav.EKTE),
                usikreDatoer = datoer(Datoopphav.PATCHET_ENDRET),
                patchedeDatoer = datoer(Datoopphav.PATCHET_URØRT),
                antallUdaterteRader = eksisterende.count { it.sortering == null },
            )
        }
    }

    /**
     * Alle rader per sak, nyest først.
     *
     * `sorteringspåvirkning` plukker [SorteringspåvirkningRad.nyesteSammenlignbareId] med
     * `maxByOrNull` og er ikke avhengig av rekkefølgen, men kandidatspørringen er: den tar `.first()`
     * for å finne raden patchen skriver. Datolistene i rapporten arver også rekkefølgen. Fjernes
     * `ORDER BY` fra en av spørringene, ryker begge deler stille.
     */
    @Suppress("UNCHECKED_CAST")
    private fun raderPerSak(sql: String, saksnummer: List<String>, medMarkoer: Boolean): Map<String, List<SortertRad>> {
        val query = entityManager.createNativeQuery(sql)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
        if (medMarkoer) query.setParameter("markoer", PATCH_MARKØR)

        return (query.resultList as List<Array<Any?>>)
            .map { rad ->
                SortertRad(
                    saksnummer = rad[0] as String,
                    behandlingsresultatId = (rad[1] as Number).toLong(),
                    visning = rad[2] as String?,
                    sortering = rad[3] as String?,
                    opphav = Datoopphav.av((rad[4] as Number).toInt()),
                )
            }
            .groupBy { it.saksnummer }
    }

    /**
     * Hvor datoen på en `vedtak_metadata`-rad kommer fra — det eneste vi kan lese ut av auditfeltene.
     *
     * `registrert_av` er `@CreatedBy` og settes kun ved insert, så den forteller pålitelig om raden ble
     * laget av denne fiksen. `endret_av` er `@LastModifiedBy` og flyttes av *enhver* senere skriving —
     * ikke bare av at noen setter en ekte vedtaksdato. Derfor kan vi skille «beviselig vår proxy» fra
     * «noen har rørt den siden», men ikke avgjøre om det som ble skrevet faktisk var vedtaksdatoen.
     *
     * Den upresisheten er ufarlig så lenge den ikke styrer om prod endres: [PATCHET_ENDRET] regnes med i
     * seleunderlaget, altså konservativt, og rapporteres for seg slik at operatøren ser forskjellen.
     */
    private enum class Datoopphav {
        /** Raden ble ikke laget av fiksen. Datoen er en ekte vedtaksdato. */
        EKTE,

        /** Laget av fiksen og urørt siden. Datoen er beviselig vår egen proxy. */
        PATCHET_URØRT,

        /** Laget av fiksen, men skrevet til siden. Kan være korrigert til en ekte dato — vi vet ikke. */
        PATCHET_ENDRET,
        ;

        companion object {
            fun av(kode: Int) = when (kode) {
                0 -> EKTE
                1 -> PATCHET_URØRT
                else -> PATCHET_ENDRET
            }
        }
    }

    /**
     * [visning] er lesbar for operatøren; [sortering] har mikrosekunder og er den som sammenlignes.
     * Begge er null når raden ikke har vedtaksdato — kolonnen er nullbar.
     */
    private data class SortertRad(
        val saksnummer: String,
        val behandlingsresultatId: Long,
        val visning: String?,
        val sortering: String?,
        val opphav: Datoopphav,
    )

    private fun hentAngreKandidater(saksnummer: List<String>): List<VedtaksmetadataAngreRad> {
        val sql = if (saksnummer.isEmpty()) ANGRE_KANDIDAT_SQL else ANGRE_KANDIDAT_SQL + ANGRE_KANDIDAT_SAKSFILTER
        val query = entityManager.createNativeQuery(sql).setParameter("markoer", PATCH_MARKØR)
        if (saksnummer.isNotEmpty()) query.setParameter("saksnummer", saksnummer)

        return (query.resultList as List<Array<Any?>>).map { rad ->
            VedtaksmetadataAngreRad(
                saksnummer = rad[0] as String,
                behandlingsresultatId = (rad[1] as Number).toLong(),
                vedtakDato = rad[2] as String?,
                vedtakType = rad[3] as String?,
            )
        }
    }

    private fun tellSomIkkeKanAngres(saksnummer: List<String>): Int {
        val sql = if (saksnummer.isEmpty()) KAN_IKKE_ANGRES_SQL else KAN_IKKE_ANGRES_SQL + ANGRE_KANDIDAT_SAKSFILTER
        val query = entityManager.createNativeQuery(sql).setParameter("markoer", PATCH_MARKØR)
        if (saksnummer.isNotEmpty()) query.setParameter("saksnummer", saksnummer)
        return (query.singleResult as Number).toInt()
    }

    /** Q6a — hvor mange defekte rader står igjen per sak. Tom etter en vellykket skarp kjøring. */
    @Suppress("UNCHECKED_CAST")
    private fun tellUtenMetadata(saksnummer: List<String>): Map<String, Int> {
        if (saksnummer.isEmpty()) return emptyMap()

        val rader = entityManager.createNativeQuery(ETTERKONTROLL_SQL)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
            .resultList as List<Array<Any?>>

        return rader.associate { rad -> (rad[0] as String) to (rad[1] as Number).toInt() }
    }

    companion object {
        /**
         * Skrives i registrert_av/endret_av og er nøkkelen angreknappen sletter på.
         *
         * Verdien er en del av datakontrakten mot prod — den står i radene fiksen allerede har satt
         * inn, og i den manuelle Q4b-SQL-en i fiksplanen. Konstanten kan hete hva som helst; verdien
         * kan ikke endres. Bind-parameteren i spørringene heter fortsatt `:markoer`: det er et
         * SQL-navn, ikke en Kotlin-identifikator, og der holder vi oss til ASCII.
         */
        const val PATCH_MARKØR = "MELOSYS-8174-PATCH"

        /**
         * Sakene fra fiksplanen 18.08.2026 som fag har bekreftet har trygdeavgift til Nav for 2024.
         * Brukes kun som default i preview — skarp kjøring krever eksplisitt liste.
         * MEL-409394 er tatt ut av scope; den er avklart separat (år-løs årsavregning, ikke
         * manglende vedtaksmetadata).
         */
        val STANDARD_SAKER = listOf("MEL-448193", "MEL-545776", "MEL-632908")

        /** Maks antall saksnummer per kall. Holder oss også godt unna [MAKS_UTTRYKK_I_IN]. */
        const val MAKS_ANTALL_SAKER = 25

        /**
         * Oracles tak på antall uttrykk i en IN-liste (ORA-01795).
         *
         * `MAKS_ANTALL_SAKER` holder `IN (:saksnummer)` trygt under grensen, men `INSERT_SQL` binder
         * også kandidat-IDene i `IN (:ider)`, og den lista er like lang som antall kandidatrader —
         * ikke antall saker. Én sak med over tusen defekte behandlingsresultat er nok. Uten selen
         * feiler INSERT-en med ORA-01795 og gir 500 (verifisert i IT) midt i det skrittet som endrer
         * prod, i stedet for en 400 operatøren kan handle på.
         */
        const val MAKS_UTTRYKK_I_IN = 1000

        /** Default tak på antall rader en skarp kjøring får sette inn. Kan heves eksplisitt i requesten. */
        const val STANDARD_MAKS_ANTALL_RADER = 10

        /**
         * Behandlingstyper vi trygt kan utlede vedtakstype for: FØRSTEGANG gir FØRSTEGANGSVEDTAK,
         * NY_VURDERING og ENDRET_PERIODE gir ENDRINGSVEDTAK (samme mønster som Flyway-patchen V7.6_04).
         * Andre typer — KLAGE, ANKE, SATSENDRING … — har egne vedtakstyper i kodeverket og avvises.
         */
        val KJENTE_BEH_TYPER = listOf("FØRSTEGANG", "NY_VURDERING", "ENDRET_PERIODE")

        /** Resultattypene ÅrsavregningService slår opp på — det er kun disse som kan velte en sak. */
        private val RESULTATTYPER = listOf("FASTSATT_TRYGDEAVGIFT", "FASTSATT_LOVVALGSLAND", "MEDLEM_I_FOLKETRYGDEN")

        /** Delt av [KANDIDAT_SQL] og [INSERT_SQL], slik at forhåndsvisningen treffer nøyaktig de samme radene. */
        private const val KANDIDAT_WHERE = """
            WHERE b.status = 'AVSLUTTET'
              AND br.resultat_type IN (:resultattyper)
              AND NOT EXISTS (SELECT 1 FROM vedtak_metadata vm WHERE vm.behandlingsresultat_id = br.behandling_id)
              AND b.saksnummer IN (:saksnummer)
        """

        /**
         * vedtak_type utledes av beh_type — kodeverkskoden er FØRSTEGANG, ikke FØRSTEGANGSBEHANDLING.
         *
         * Merk at beh_type ikke *bestemmer* vedtakstypen: den sendes separat i FattVedtakRequest, og
         * kodeverket har også KORRIGERT_VEDTAK, OMGJØRINGSVEDTAK og OPPHØRSVEDTAK — en NY_VURDERING kan
         * altså ha vært et korrigert vedtak. Utledningen er bevisst likevel, av tre grunner: feltet
         * leses ikke av ÅrsavregningService (kun som etikett i BehandlingsresultatDto), Flyway-patchen
         * V7.6_04 gjorde nøyaktig samme utledning for en strukturelt lik populasjon, og alternativet
         * NULL er verre — A1TypeUtstedelse.av() switcher på enumen uten null-gren. Vi bytter altså en
         * mulig feil etikett på tre rader mot en ny NPE-flate. Er raden dessuten aldri fattet som
         * vedtak (se statussiden 25.08), finnes det uansett ingen riktig verdi å skrive.
         */
        private const val VEDTAK_TYPE_CASE =
            "CASE WHEN b.beh_type = 'FØRSTEGANG' THEN 'FØRSTEGANGSVEDTAK' ELSE 'ENDRINGSVEDTAK' END"

        /**
         * vedtak_dato er en proxy: den ekte vedtaksdatoen finnes ikke lenger, så vi bruker
         * behandlingsresultat.endret_dato. Klagefrist +42 dager følger Flyway-patchen V7.6_04.
         * Datoene formateres i SQL for å slippe JDBC-typemapping i rapporten.
         */
        private const val KANDIDAT_SQL = """
            SELECT b.saksnummer,
                   br.behandling_id,
                   b.beh_type,
                   br.resultat_type,
                   TO_CHAR(br.endret_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   TO_CHAR(TRUNC(CAST(br.endret_dato AS DATE)) + 42, 'YYYY-MM-DD'),
                   $VEDTAK_TYPE_CASE
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
            ORDER BY b.saksnummer, br.endret_dato
        """

        private const val INSERT_SQL = """
            INSERT INTO vedtak_metadata
                (behandlingsresultat_id, vedtak_dato, vedtak_klagefrist, vedtak_type,
                 registrert_dato, endret_dato, registrert_av, endret_av)
            SELECT br.behandling_id,
                   br.endret_dato,
                   TRUNC(CAST(br.endret_dato AS DATE)) + 42,
                   $VEDTAK_TYPE_CASE,
                   SYSTIMESTAMP, SYSTIMESTAMP, :markoer, :markoer
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
              AND br.behandling_id IN (:ider)
        """

        /**
         * Radene som allerede har vedtaksmetadata — klokka de nye patch-radene sammenlignes mot.
         * Nyest først. Siste kolonne er en opphavskode, ikke et ja/nei: 0 = raden ble ikke laget av
         * fiksen, 1 = laget av fiksen og urørt siden, 2 = laget av fiksen men skrevet til etterpå.
         * Skillet mellom 0 og 2 på den ene siden og 1 på den andre er hele poenget — se [Datoopphav].
         *
         * `vedtak_dato` er nullbar, og Oracle sorterer NULL først i `DESC`. Det som faktisk hindrer
         * at en udatert rad kaprer «nyeste»-plassen er filteret i [sorteringspåvirkning] (og
         * nullbar lesing i [raderPerSak]); `NULLS LAST` her gjør SQL-ens egen rekkefølge riktig i
         * tillegg, slik at [SorteringspåvirkningRad.ekteDatoer] listes nyest først. Tolkningen
         * følger `ÅrsavregningService`, der `sortedBy` legger null først i stigende rekkefølge —
         * altså som den eldste. `behandling_id` som sekundærnøkkel gjør rekkefølgen deterministisk.
         *
         * Lesbar dato til rapporten, og en med mikrosekunder til sammenligningen.
         * Uten mikrosekundene svarer rapporten «patchen vinner ikke» når de to ligger i samme
         * sekund — og `vedtak_dato` er en TIMESTAMP som ÅrsavregningService sammenligner i full
         * oppløsning.
         */
        private const val EKSISTERENDE_NYESTE_SQL = """
            SELECT b.saksnummer,
                   br.behandling_id,
                   TO_CHAR(vm.vedtak_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   TO_CHAR(vm.vedtak_dato, 'YYYY-MM-DD HH24:MI:SS.FF6'),
                   CASE
                       WHEN vm.registrert_av IS NULL OR vm.registrert_av <> :markoer THEN 0
                       WHEN vm.endret_av = :markoer THEN 1
                       ELSE 2
                   END
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = br.behandling_id
            WHERE b.status = 'AVSLUTTET'
              AND br.resultat_type IN (:resultattyper)
              AND b.saksnummer IN (:saksnummer)
            ORDER BY b.saksnummer, vm.vedtak_dato DESC NULLS LAST, br.behandling_id DESC
        """

        /** Kandidatene, med datoen patchen faktisk ville skrevet. Nyest først. */
        private const val PATCH_NYESTE_SQL = """
            SELECT b.saksnummer,
                   br.behandling_id,
                   TO_CHAR(br.endret_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   TO_CHAR(br.endret_dato, 'YYYY-MM-DD HH24:MI:SS.FF6'),
                   0
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
            ORDER BY b.saksnummer, br.endret_dato DESC, br.behandling_id DESC
        """

        private const val ETTERKONTROLL_SQL = """
            SELECT b.saksnummer, COUNT(*)
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
            GROUP BY b.saksnummer
        """

        /** Kun rader som fortsatt er urørte: endret_av flyttes av enhver senere skriving (@LastModifiedBy). */
        private const val ANGRE_KANDIDAT_SQL = """
            SELECT b.saksnummer,
                   vm.behandlingsresultat_id,
                   TO_CHAR(vm.vedtak_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   vm.vedtak_type
            FROM vedtak_metadata vm
            JOIN behandling b ON b.id = vm.behandlingsresultat_id
            WHERE vm.registrert_av = :markoer
              AND vm.endret_av = :markoer
        """
        private const val ANGRE_KANDIDAT_SAKSFILTER = " AND b.saksnummer IN (:saksnummer)"

        /**
         * Patch-rader som ikke lenger er urørte, og som [ANGRE_SQL] derfor lar stå.
         *
         * To årsaker, og feltnavnet nevner ingen av dem med vilje: enten har noen skrevet ekte data
         * oppå (`endret_av` er en saksbehandler), eller så er markøren tømt (`endret_av` er NULL).
         * Et navn som `antallEndretEtterpå` ville sendt operatøren på jakt etter hvem som endret
         * raden, og i NULL-tilfellet finnes det ingen. Det operatøren trenger å vite er at raden
         * ikke ruller tilbake automatisk.
         *
         * `endret_av` er nullbar, og i Oracle er både `= :markoer` og `<> :markoer` UNKNOWN mot
         * NULL. Uten NULL-grenen her faller en patch-rad med NULL `endret_av` ut av BÅDE
         * angre-kandidatene og denne tellingen: rollbacken blir ufullstendig, og svaret ser ut som
         * «ingenting å angre» i stedet for «én rad kunne ikke rulles tilbake».
         */
        private const val KAN_IKKE_ANGRES_SQL = """
            SELECT COUNT(*)
            FROM vedtak_metadata vm
            JOIN behandling b ON b.id = vm.behandlingsresultat_id
            WHERE vm.registrert_av = :markoer
              AND (vm.endret_av IS NULL OR vm.endret_av <> :markoer)
        """

        private const val ANGRE_SQL = """
            DELETE FROM vedtak_metadata
            WHERE registrert_av = :markoer
              AND endret_av = :markoer
        """
        private const val ANGRE_SAKSFILTER = """
              AND behandlingsresultat_id IN (SELECT b.id FROM behandling b WHERE b.saksnummer IN (:saksnummer))
        """
    }
}

data class VedtaksmetadataFiksRad(
    val saksnummer: String,
    val behandlingsresultatId: Long,
    val behType: String?,
    val resultatType: String?,
    val blirVedtakDato: String?,
    val blirKlagefrist: String?,
    val blirVedtakType: String,
)

/**
 * Hva patchen gjør med vedtaksdato-sorteringen for én sak.
 *
 * Feltene er bevisst uten overlapp: [patchenVinnerNyeste] er den ENESTE som avgjør om skarp kjøring
 * blokkeres. De tre datolistene er rapport, og summen av dem er alle daterte rader `ÅrsavregningService`
 * ser i saken — altså avsluttede behandlinger med resultattype i `RESULTATTYPER`, samme avgrensning som
 * `hentGjeldendeBehandlingsresultaterForÅrsavregning`. En vedtaksmetadata-rad på en åpen behandling er
 * med vilje ikke med, for den påvirker ikke utvelgelsen. Rader uten `vedtak_dato` har ingen dato å
 * liste — se [antallUdaterteRader].
 *
 * At to felt målte over hvert sitt underlag og ble kombinert i selen er nøyaktig det som gjorde tre
 * runder med fikser til nye feil.
 */
data class SorteringspåvirkningRad(
    val saksnummer: String,
    /** Raden patchen vil skrive — den nyeste av kandidatene i saken, med datoen den får. */
    val nyesteKandidatId: Long,
    val nyesteKandidatDato: String?,
    /**
     * Nyeste daterte rad som *kan* være et ekte vedtak ([ekteDatoer] og [usikreDatoer] under).
     * Null betyr at saken ikke har noen slik rad — da er det ingenting å fortrenge, og selen slipper
     * kjøringen gjennom. Det er det tryggeste tilfellet, ikke det farligste: alle datoene i saken
     * kommer da fra samme klokke, så den interne rekkefølgen er konsistent.
     */
    val nyesteSammenlignbareId: Long?,
    val nyesteSammenlignbareDato: String?,
    /**
     * Patchen legger seg på eller over [nyesteSammenlignbareDato], altså kan den fortrenge et vedtak
     * som er ekte. True også ved eksakt likt tidsstempel — da er utfallet vilkårlig, og det skal
     * flagges. **Blokkerer skarp kjøring med mindre saksnummeret er kvittert ut.**
     */
    val patchenVinnerNyeste: Boolean,
    /**
     * Patchen blir nyeste rad i saken sett mot ALLE rader, også de vi selv satte inn tidligere.
     * Blokkerer ikke — fortrenger den bare en tidligere patch, er det ingen ekte dato som ryker — men
     * kjøringen endrer likevel hvilken behandling `ÅrsavregningService` regner som nyest, så den logges.
     */
    val patchenBlirNyesteIHeleSaken: Boolean,
    /**
     * Datoer fra rader denne fiksen ikke har laget.
     *
     * Merk hva det *ikke* garanterer: Flyway-patchen `V7.6_04__patch_vedtak_metadata_endret_periode`
     * skrev `vedtak_dato = behandlingsresultat.endret_dato` — nøyaktig samme proxy som vi bruker — med
     * `registrert_av` satt til saksbehandlerens ident i stedet for en markør. Rader derfra (BEH_TYPE
     * `ENDRET_PERIODE`, resultattype `FASTSATT_LOVVALGSLAND`) havner derfor her og ser ekte ut. Vi kan
     * ikke skille dem fra ekte vedtaksdatoer, og forsøker ikke: å telle dem med er den konservative
     * retningen for selen. Men på en slik rad er «sett ekte vedtaksdato manuelt» fortsatt ugjort.
     */
    val ekteDatoer: List<String> = emptyList(),
    /**
     * Datoer fra rader fiksen laget, men som noen har skrevet til siden. Kan være korrigert til en
     * ekte vedtaksdato — `endret_av` er `@LastModifiedBy` og flyttes av enhver skriving, så vi kan
     * ikke vite det. Regnes med i seleunderlaget, altså konservativt.
     */
    val usikreDatoer: List<String> = emptyList(),
    /** Datoer fra rader fiksen laget og ingen har rørt. Beviselig vår egen proxy — styrer ingenting. */
    val patchedeDatoer: List<String> = emptyList(),
    /**
     * Rader som har vedtaksmetadata, men uten `vedtak_dato`. De har ingen dato å liste i de tre
     * listene over, og teller ikke som sammenligningsgrunnlag — `ÅrsavregningService` sorterer dem
     * først, altså som de eldste, så de vinner aldri. Feltet finnes fordi `nyesteSammenlignbareId:
     * null` og tre tomme lister ellers leses som «saken har ingen vedtaksmetadata», når den kan ha
     * flere.
     */
    val antallUdaterteRader: Int = 0,
)

data class VedtaksmetadataFiksResultat(
    val skarp: Boolean,
    val saksnummer: List<String>,
    val antallRaderFunnet: Int,
    val antallRaderInnsatt: Int,
    /** Kandidatene fra preview-spørringen. Ved `avvik = true` er dette ikke det samme som det som ble skrevet. */
    val rader: List<VedtaksmetadataFiksRad>,
    /** Q6a-etterkontrollen, per sak: rader uten vedtaksmetadata slik det står nå. Tom = ingen igjen. */
    val utenMetadataPerSak: Map<String, Int>,
    /** Kandidater der vedtakstypen ikke kan utledes trygt. Blokkerer skarp kjøring. */
    val ukjentBehType: List<Long> = emptyList(),
    /**
     * Saksnummer fra requesten som ikke ga én eneste kandidatrad — enten fordi saken ikke finnes
     * (skrivefeil), eller fordi den ikke har defekte rader. Blokkerer ikke, men skal ses på når du
     * trodde saken skulle fikses.
     */
    val saksnummerUtenKandidater: List<String> = emptyList(),
    /**
     * Saksnummer sendt i `tillatSorteringsendring` som ikke tilsvarer noen sak der patchen faktisk
     * kaprer nyeste-plassen — typisk en skrivefeil. Blokkerer ikke, men en kvittering som ikke traff
     * noe er verdt et blikk før du kjører videre.
     */
    val kvitteringerUtenTreff: List<String> = emptyList(),
    /** True hvis antall innsatte rader ikke stemmer med forhåndsvisningen. */
    val avvik: Boolean = false,
    /** Per sak: bytter patchen ut hvilken behandling som er nyest i vedtaksdato-sorteringen? */
    val sorteringspåvirkning: List<SorteringspåvirkningRad> = emptyList(),
    val markoer: String = VedtaksmetadataFiksService.PATCH_MARKØR,
    val angreEndepunkt: String = "POST /admin/aarsavregninger/saker/skattepliktige/vedtaksmetadata-fiks/angre",
)

data class VedtaksmetadataAngreRad(
    val saksnummer: String,
    val behandlingsresultatId: Long,
    val vedtakDato: String?,
    val vedtakType: String?,
)

data class VedtaksmetadataAngreResultat(
    val skarp: Boolean,
    /** Tom liste = alle markerte rader, uansett sak. */
    val saksnummer: List<String>,
    val antallRaderFunnet: Int,
    val antallSlettet: Int,
    val rader: List<VedtaksmetadataAngreRad>,
    /** Patch-rader som er endret etterpå (ekte data skrevet oppå) — disse røres aldri. */
    val antallSomIkkeKanAngres: Int,
    val avvik: Boolean = false,
    val markoer: String = VedtaksmetadataFiksService.PATCH_MARKØR,
)
