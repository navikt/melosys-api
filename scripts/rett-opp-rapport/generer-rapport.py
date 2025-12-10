#!/usr/bin/env python3
"""
Genererer detaljert HTML-rapport for MELOSYS-7711 Feil 1 Del 2.
"""

import json
import csv
from collections import defaultdict
from datetime import datetime

# Les JSON-data
with open('feil1-del2-0-2723.json', 'r') as f:
    json_data = json.load(f)

# Les CSV-data og grupper per saksnummer
behandlinger_per_sak = defaultdict(list)
with open('feil1-fagsak-behandlinger.csv', 'r') as f:
    reader = csv.reader(f)
    for row in reader:
        if len(row) >= 9:
            saksnummer = row[0]
            behandlinger_per_sak[saksnummer].append({
                'saksnummer': row[0],
                'gsakSaksnummer': row[1],
                'saksstatus': row[2],
                'behandlingId': row[3],
                'behandlingType': row[4],
                'behandlingTema': row[5],
                'behandlingStatus': row[6],
                'registrertDato': row[7],
                'resultat': row[8]
            })

# Kategoribeskrivelser
KATEGORI_INFO = {
    'DEL2A_RETTET': {
        'badge': 'badge-del2a',
        'color': '#FFF3CD',
        'border': '#856404',
        'title': 'DEL2A: Siste behandling er REGISTRERT_UNNTAK',
        'aksjon': 'MEDL på HENLEGGELSE-behandlinger settes til AVVI. Saksstatus endres IKKE.',
        'forklaring': '''
            <p><strong>Hvorfor denne kategorien?</strong></p>
            <ul>
                <li>Saken har <strong>flere enn én behandling</strong></li>
                <li>Den <strong>siste behandlingen</strong> har resultat <code>REGISTRERT_UNNTAK</code></li>
                <li>Dette betyr at brukeren har fått registrert et gyldig unntak <em>etter</em> at den opprinnelige A003 ble ugyldiggjort</li>
            </ul>
            <p><strong>Hvorfor kan vi fikse automatisk?</strong></p>
            <ul>
                <li>Antall A003 i EESSI = antall behandlinger i Melosys (balansert)</li>
                <li>Vi vet sikkert hvilken MEDL-periode som hører til hvilken behandling</li>
                <li>HENLEGGELSE-behandlingers MEDL skal være AVVI (avvist)</li>
                <li>REGISTRERT_UNNTAK-behandlingens MEDL skal forbli gyldig</li>
            </ul>
        '''
    },
    'DEL2B_RETTET': {
        'badge': 'badge-del2b',
        'color': '#E8DAEF',
        'border': '#634689',
        'title': 'DEL2B: Alle behandlinger er HENLEGGELSE',
        'aksjon': 'Alle MEDL-perioder settes til AVVI. Saksstatus endres til ANNULLERT.',
        'forklaring': '''
            <p><strong>Hvorfor denne kategorien?</strong></p>
            <ul>
                <li>Saken har <strong>flere enn én behandling</strong></li>
                <li><strong>Alle behandlinger</strong> har resultat <code>HENLEGGELSE</code></li>
                <li>Dette betyr at alle A003-er er blitt ugyldiggjort</li>
            </ul>
            <p><strong>Hvorfor kan vi fikse automatisk?</strong></p>
            <ul>
                <li>Antall A003 = antall behandlinger (balansert)</li>
                <li>Ingen gyldige perioder skal finnes - alle skal være AVVI</li>
                <li>Saksstatus skal være ANNULLERT siden ingen gyldige beslutninger finnes</li>
            </ul>
        '''
    },
    'DEL2_ANNET': {
        'badge': 'badge-gray',
        'color': '#FFF3E0',
        'border': '#FF9100',
        'title': 'DEL2_ANNET: Blandet resultat - krever manuell vurdering',
        'aksjon': 'Manuell vurdering påkrevd. Automatisk retting er IKKE mulig.',
        'forklaring': '''
            <p><strong>Hvorfor denne kategorien?</strong></p>
            <ul>
                <li>Saken har flere behandlinger med <strong>ulike resultater</strong></li>
                <li>Siste behandling er verken <code>HENLEGGELSE</code> eller <code>REGISTRERT_UNNTAK</code></li>
                <li>Eksempler: <code>FASTSATT_LOVVALGSLAND</code>, <code>FERDIGBEHANDLET</code></li>
            </ul>
            <p><strong>Hvorfor kan vi IKKE fikse automatisk?</strong></p>
            <ul>
                <li>Ukjent kombinasjon av resultater</li>
                <li>Trenger manuell analyse for å avgjøre korrekt handling</li>
            </ul>
        '''
    },
    'IKKE_DEL2_KRITERIE': {
        'badge': 'badge-unsafe',
        'color': '#FFEBEE',
        'border': '#BA3A26',
        'title': 'UNSAFE: Kun 1 behandling, men flere A003',
        'aksjon': 'KAN IKKE fikses automatisk. Manuell vurdering påkrevd.',
        'forklaring': '''
            <p><strong>Hvorfor denne kategorien?</strong></p>
            <ul>
                <li>Saken har <strong>kun 1 behandling</strong> i Melosys</li>
                <li>Men det finnes <strong>flere A003 SEDer</strong> i EESSI</li>
                <li>Dette betyr at en A003 kom inn <em>uten</em> å opprette behandling</li>
            </ul>
            <p><strong>Hvorfor kan vi IKKE fikse automatisk?</strong></p>
            <ul>
                <li><span style="color:#BA3A26;font-weight:bold;">A003-ubalanse:</span> Antall A003 > antall behandlinger</li>
                <li>Vi vet ikke hvilken A003 som er gyldig</li>
                <li>En A003 kan ha kommet etter X008 og dermed ikke opprettet behandling</li>
                <li>Krever manuell sjekk i EESSI for å forstå sekvensen</li>
            </ul>
        '''
    },
    'UNSAFE_A003_UBALANSE': {
        'badge': 'badge-unsafe',
        'color': '#FFEBEE',
        'border': '#BA3A26',
        'title': 'UNSAFE: A003-ubalanse (flere A003 enn behandlinger)',
        'aksjon': 'KAN IKKE fikses automatisk. Manuell vurdering påkrevd.',
        'forklaring': '''
            <p><strong>Hvorfor denne kategorien?</strong></p>
            <ul>
                <li>Antall A003 SEDer i EESSI er <strong>høyere</strong> enn antall behandlinger i Melosys</li>
                <li>Dette betyr at minst én A003 ikke opprettet behandling</li>
            </ul>
            <p><strong>Hvorfor kan vi IKKE fikse automatisk?</strong></p>
            <ul>
                <li><span style="color:#BA3A26;font-weight:bold;">Kritisk ubalanse:</span> Vi kan ikke matche A003 til behandlinger</li>
                <li>En A003 som kom etter X008 vil ikke opprette behandling (pga. <code>ArbeidFlereLandSedRuter</code>)</li>
                <li>Den "ekstra" A003-en kan være gyldig og saken kan være korrekt</li>
                <li>Krever manuell analyse i EESSI</li>
            </ul>
        '''
    },
    'IKKE_INVALIDERT_I_EESSI': {
        'badge': 'badge-gray',
        'color': '#E3F2FD',
        'border': '#1976D2',
        'title': 'IKKE INVALIDERT: Ingen X008/X006 funnet i EESSI',
        'aksjon': 'Sjekk manuelt i EESSI om A003 faktisk er ugyldiggjort.',
        'forklaring': '''
            <p><strong>Hvorfor denne kategorien?</strong></p>
            <ul>
                <li>Saken ble identifisert som kandidat basert på database-data</li>
                <li>Men vi fant <strong>ingen X008 eller X006</strong> i EESSI som ugyldiggjør A003</li>
            </ul>
            <p><strong>Hvorfor kan vi IKKE fikse automatisk?</strong></p>
            <ul>
                <li>Uten bevis på ugyldiggjøring fra EESSI kan vi ikke vite om saken er feil</li>
                <li>Mulige årsaker:
                    <ul>
                        <li>A003 ble aldri ugyldiggjort (saken kan være korrekt)</li>
                        <li>X008/X006 kom via annen kanal</li>
                        <li>EESSI-data er ufullstendig</li>
                    </ul>
                </li>
            </ul>
        '''
    }
}

# Statistikk
stats = defaultdict(lambda: {'saker': 0, 'behandlinger': 0})
alle_saker = []

for saksnummer, behandlinger in json_data.items():
    # Ta første behandling for å få saksinfo (utfall er likt for alle)
    first = behandlinger[0]
    utfall = first.get('utfall', 'UKJENT')

    stats[utfall]['saker'] += 1
    stats[utfall]['behandlinger'] += len(behandlinger)

    # Hent alle behandlinger fra CSV
    csv_behandlinger = sorted(
        behandlinger_per_sak.get(saksnummer, []),
        key=lambda x: x['registrertDato']
    )

    # Tell resultater
    henleggelser = sum(1 for b in csv_behandlinger if b['resultat'] == 'HENLEGGELSE')
    reg_unntak = sum(1 for b in csv_behandlinger if b['resultat'] == 'REGISTRERT_UNNTAK')

    alle_saker.append({
        'saksnummer': saksnummer,
        'gsakSaksnummer': first.get('gsakSaksnummer'),
        'saksstatus': first.get('saksstatus'),
        'utfall': utfall,
        'antallA003': first.get('antallA003', 0),
        'antallBehandlinger': first.get('antallBehandlinger', 0),
        'feilmelding': first.get('feilmelding'),
        'medlPerioder': first.get('medlPerioder', []),
        'behandlinger': csv_behandlinger,
        'henleggelser': henleggelser,
        'regUnntak': reg_unntak,
        'rettetOpp': first.get('rettetOpp', False)
    })

# Sorter saker
alle_saker.sort(key=lambda x: (
    0 if x['utfall'] == 'DEL2A_RETTET' else
    1 if x['utfall'] == 'DEL2B_RETTET' else
    2 if x['utfall'] == 'DEL2_ANNET' else
    3 if x['utfall'] == 'IKKE_INVALIDERT_I_EESSI' else
    4 if x['utfall'] == 'UNSAFE_A003_UBALANSE' else
    5 if x['utfall'] == 'IKKE_DEL2_KRITERIE' else 6,
    x['saksnummer']
))

# Generer HTML
html = '''<!DOCTYPE html>
<html lang="no">
<head>
    <meta charset="UTF-8">
    <title>MELOSYS-7711 Feil 1 Del 2 - Detaljert rapport</title>
    <style>
        :root {
            --nav-blue: #0067C5;
            --nav-red: #BA3A26;
            --nav-green: #06893A;
            --nav-orange: #FF9100;
            --nav-purple: #634689;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; background: #F5F5F5; line-height: 1.6; }
        .container { max-width: 1800px; margin: 0 auto; padding: 20px; }
        h1 { color: var(--nav-blue); margin-bottom: 5px; }
        h2 { margin: 30px 0 15px; border-bottom: 2px solid var(--nav-blue); padding-bottom: 5px; }
        .subtitle { color: #666; font-size: 16px; }
        .timestamp { color: #999; font-size: 14px; margin-bottom: 30px; }

        .summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 15px; margin-bottom: 30px; }
        .summary-card { background: white; border-radius: 8px; padding: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); border-left: 4px solid var(--nav-blue); }
        .summary-card h3 { font-size: 11px; color: #666; text-transform: uppercase; }
        .big-number { font-size: 32px; font-weight: bold; color: var(--nav-blue); }
        .card-del2a { border-left-color: var(--nav-orange); } .card-del2a .big-number { color: var(--nav-orange); }
        .card-del2b { border-left-color: var(--nav-purple); } .card-del2b .big-number { color: var(--nav-purple); }
        .card-unsafe { border-left-color: var(--nav-red); } .card-unsafe .big-number { color: var(--nav-red); }
        .card-success { border-left-color: var(--nav-green); } .card-success .big-number { color: var(--nav-green); }

        .badge { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: bold; }
        .badge-del2a { background: #FFF3CD; color: #856404; }
        .badge-del2b { background: #E8DAEF; color: var(--nav-purple); }
        .badge-unsafe { background: #FFDDD8; color: var(--nav-red); }
        .badge-gray { background: #E9ECEF; color: #666; }

        .filter-section { background: white; padding: 15px; border-radius: 8px; margin-bottom: 20px; display: flex; gap: 15px; flex-wrap: wrap; align-items: center; }
        .filter-group { display: flex; align-items: center; gap: 8px; }
        .filter-group label { font-weight: 600; font-size: 13px; }
        .filter-group select, .filter-group input { padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
        .btn { padding: 8px 16px; border-radius: 4px; cursor: pointer; border: none; }
        .btn-primary { background: var(--nav-blue); color: white; }
        .btn-secondary { background: #E9ECEF; }
        .result-count { margin-left: auto; font-size: 14px; color: #666; }

        .table-container { background: white; border-radius: 8px; overflow: hidden; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        th { background: var(--nav-blue); color: white; padding: 12px; text-align: left; position: sticky; top: 0; }
        td { padding: 12px; border-bottom: 1px solid #eee; }
        tr:hover { background: #f8f9fa; }

        .toggle-btn { background: var(--nav-blue); color: white; border: none; padding: 4px 12px; border-radius: 4px; cursor: pointer; font-size: 12px; }
        .expand-row { display: none; background: #FAFAFA; }
        .expand-row.visible { display: table-row; }
        .expand-content { padding: 20px; }

        .kategori-box { border-radius: 8px; padding: 15px; margin-bottom: 15px; }
        .kategori-box h5 { margin: 0 0 8px 0; }
        .kategori-box ul { margin: 10px 0; padding-left: 20px; }
        .kategori-box li { margin: 5px 0; }

        .expand-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
        .expand-section h5 { color: var(--nav-blue); font-size: 12px; text-transform: uppercase; margin-bottom: 8px; }
        .expand-section p { margin: 4px 0; }
        .label { color: #666; }

        .detail-table { width: 100%; font-size: 12px; border-collapse: collapse; margin-top: 10px; }
        .detail-table th { background: #f0f0f0; color: #333; padding: 8px; text-align: left; position: static; }
        .detail-table td { padding: 8px; border-bottom: 1px solid #eee; }

        .forklaring-box { background: #f8f9fa; border: 1px solid #dee2e6; border-radius: 8px; padding: 15px; margin-top: 15px; font-size: 13px; }
        .forklaring-box h5 { color: var(--nav-blue); margin-bottom: 10px; }

        code { background: #e9ecef; padding: 2px 6px; border-radius: 3px; font-size: 12px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>MELOSYS-7711: Feil 1 Del 2 - Detaljert rapport</h1>
        <p class="subtitle">A003 saker ugyldiggjort med X008/X006 - saker med flere behandlinger</p>
        <p class="timestamp">Generert: ''' + datetime.now().strftime('%Y-%m-%d %H:%M') + ''' | Jira: <a href="https://jira.nav.no/browse/MELOSYS-7752">MELOSYS-7752</a></p>

        <div class="summary-grid">
            <div class="summary-card"><h3>Totalt</h3><div class="big-number">''' + f"{len(alle_saker):,}".replace(",", " ") + '''</div></div>
            <div class="summary-card card-del2a"><h3>Del 2a</h3><div class="big-number">''' + f"{stats['DEL2A_RETTET']['saker']:,}".replace(",", " ") + '''</div><p style="font-size:11px;color:#666;">Siste=REGISTRERT_UNNTAK</p></div>
            <div class="summary-card card-del2b"><h3>Del 2b</h3><div class="big-number">''' + str(stats['DEL2B_RETTET']['saker']) + '''</div><p style="font-size:11px;color:#666;">Alle=HENLEGGELSE</p></div>
            <div class="summary-card card-unsafe"><h3>UNSAFE</h3><div class="big-number">''' + f"{stats['UNSAFE_A003_UBALANSE']['saker'] + stats['IKKE_DEL2_KRITERIE']['saker']:,}".replace(",", " ") + '''</div><p style="font-size:11px;color:#666;">A003 > behandlinger</p></div>
            <div class="summary-card"><h3>Annet</h3><div class="big-number">''' + str(stats['DEL2_ANNET']['saker']) + '''</div></div>
            <div class="summary-card"><h3>Ikke invalidert</h3><div class="big-number">''' + str(stats['IKKE_INVALIDERT_I_EESSI']['saker']) + '''</div></div>
        </div>

        <div class="filter-section">
            <div class="filter-group">
                <label>Kategori:</label>
                <select id="filter-kat" onchange="applyFilters()">
                    <option value="">Alle</option>
                    <option value="DEL2A_RETTET">Del 2a (''' + str(stats['DEL2A_RETTET']['saker']) + ''')</option>
                    <option value="DEL2B_RETTET">Del 2b (''' + str(stats['DEL2B_RETTET']['saker']) + ''')</option>
                    <option value="DEL2_ANNET">Annet (''' + str(stats['DEL2_ANNET']['saker']) + ''')</option>
                    <option value="UNSAFE">UNSAFE (''' + str(stats['UNSAFE_A003_UBALANSE']['saker'] + stats['IKKE_DEL2_KRITERIE']['saker']) + ''')</option>
                    <option value="IKKE_INVALIDERT_I_EESSI">Ikke invalidert (''' + str(stats['IKKE_INVALIDERT_I_EESSI']['saker']) + ''')</option>
                </select>
            </div>
            <div class="filter-group">
                <label>Søk:</label>
                <input type="text" id="search" placeholder="Saksnummer..." onkeyup="applyFilters()">
            </div>
            <button class="btn btn-secondary" onclick="resetFilters()">Nullstill</button>
            <button class="btn btn-primary" onclick="expandAll()">Utvid alle synlige</button>
            <button class="btn btn-secondary" onclick="collapseAll()">Lukk alle</button>
            <span class="result-count">Viser <strong id="count">''' + str(len(alle_saker)) + '''</strong> av ''' + str(len(alle_saker)) + '''</span>
        </div>

        <div class="table-container">
            <table id="tbl">
                <thead>
                    <tr>
                        <th>Saksnummer</th>
                        <th>Kategori</th>
                        <th>Ant. Beh</th>
                        <th>Ant. A003</th>
                        <th>Henleggelser</th>
                        <th>Reg. Unntak</th>
                        <th>Feilmelding</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
'''

# Generer rader for hver sak
for idx, sak in enumerate(alle_saker):
    utfall = sak['utfall']
    info = KATEGORI_INFO.get(utfall, KATEGORI_INFO['DEL2_ANNET'])

    # Bestem badge klasse
    badge_class = info['badge']

    # Kort utfall-tekst for tabellen
    utfall_kort = utfall.replace('_RETTET', '').replace('_I_EESSI', '').replace('IKKE_DEL2_KRITERIE', 'UNSAFE')

    html += f'''
                    <tr data-kat="{utfall}" data-sak="{sak['saksnummer']}" data-idx="{idx}">
                        <td><strong>{sak['saksnummer']}</strong></td>
                        <td><span class="badge {badge_class}">{utfall_kort}</span></td>
                        <td>{sak['antallBehandlinger']}</td>
                        <td>{sak['antallA003']}</td>
                        <td>{sak['henleggelser']}</td>
                        <td>{sak['regUnntak']}</td>
                        <td style="font-size:11px;max-width:300px;overflow:hidden;text-overflow:ellipsis;">{sak['feilmelding'] or '-'}</td>
                        <td><button class="toggle-btn" onclick="toggle({idx})">Vis detaljer</button></td>
                    </tr>
                    <tr id="exp-{idx}" class="expand-row" data-kat="{utfall}">
                        <td colspan="8">
                            <div class="expand-content">
                                <div class="kategori-box" style="background:{info['color']};border:2px solid {info['border']};">
                                    <h5 style="color:{info['border']};">{info['title']}</h5>
                                    <p><strong>Aksjon:</strong> {info['aksjon']}</p>
                                    <p style="margin-top:8px;font-size:12px;">
                                        A003 i EESSI: <strong>{sak['antallA003']}</strong> |
                                        Behandlinger: <strong>{sak['antallBehandlinger']}</strong> |
                                        Henleggelser: <strong>{sak['henleggelser']}</strong> |
                                        Registrert unntak: <strong>{sak['regUnntak']}</strong>
                                    </p>
                                </div>

                                <div class="expand-grid">
                                    <div class="expand-section">
                                        <h5>📋 Saksinfo</h5>
                                        <p><span class="label">Saksnummer:</span> {sak['saksnummer']}</p>
                                        <p><span class="label">GSAK:</span> {sak['gsakSaksnummer']}</p>
                                        <p><span class="label">Saksstatus nå:</span> <strong>{sak['saksstatus']}</strong></p>
                                        <p><span class="label">Saksstatus-aksjon:</span> {'<span style="color:var(--nav-green);">Ingen endring</span>' if utfall == 'DEL2A_RETTET' else '<span style="color:var(--nav-red);font-weight:bold;">→ ANNULLERT</span>' if utfall == 'DEL2B_RETTET' else '<span style="color:#666;">Manuell vurdering</span>'}</p>
                                    </div>
                                    <div class="expand-section">
                                        <h5>📄 MEDL-perioder</h5>
'''

    if sak['medlPerioder']:
        has_status = any(periode.get('status') for periode in sak['medlPerioder'])
        if not has_status:
            html += '''<p style="font-size:11px;color:#999;margin-bottom:8px;">NB: Kjør ny dry-run for å få MEDL-status.</p>'''

        for periode in sak['medlPerioder']:
            status = periode.get('status', '?')
            skal_rettes = periode.get('skalRettes', True)
            hoppet_over = periode.get('hoppetOverGrunn', '')

            status_color = '#06893A' if status == 'AVST' else '#BA3A26' if status in ['GYLD', 'ENDL'] else '#666'
            status_display = f'<span style="color:{status_color};font-weight:bold;">{status}</span>' if status != '?' else '?'

            html += f'''
                                        <div style="background:#f8f9fa;padding:8px;border-radius:4px;margin-bottom:5px;">
                                            <p><span class="label">MEDL ID:</span> {periode.get('medlPeriodeId', '?')}</p>
                                            <p><span class="label">Periode:</span> {periode.get('fom', '?')} - {periode.get('tom', '?')}</p>
                                            <p><span class="label">Status:</span> {status_display}</p>
'''
            if not skal_rettes and hoppet_over:
                html += f'''<p style="color:var(--nav-green);"><strong>✓ Hoppet over:</strong> {hoppet_over}</p>'''
            elif skal_rettes and status and status != '?':
                html += f'''<p style="color:var(--nav-red);"><strong>⚠️ Skal rettes:</strong> {status} → AVST</p>'''
            html += '''</div>'''
    else:
        html += '''<p style="color:#999;">Ingen MEDL-perioder funnet i rapport</p>'''

    html += '''
                                    </div>
                                </div>
'''

    # Behandlinger tabell
    if sak['behandlinger']:
        html += '''
                                <div style="margin-top:20px;">
                                    <h5 style="color:var(--nav-blue);margin-bottom:10px;">🗂️ Behandlinger på fagsak (''' + str(len(sak['behandlinger'])) + ''')</h5>
                                    <table class="detail-table">
                                        <thead><tr><th>ID</th><th>Type</th><th>Resultat</th><th>Dato</th><th>MEDL-aksjon</th></tr></thead>
                                        <tbody>
'''
        for beh in sak['behandlinger']:
            resultat_color = 'var(--nav-red)' if beh['resultat'] == 'HENLEGGELSE' else 'var(--nav-green)' if beh['resultat'] == 'REGISTRERT_UNNTAK' else '#666'
            bg_color = '#FFDDD8' if beh['resultat'] == 'HENLEGGELSE' else '#D4EDDA' if beh['resultat'] == 'REGISTRERT_UNNTAK' else 'white'

            # Bestem aksjon basert på resultat og utfall
            if beh['resultat'] == 'HENLEGGELSE':
                if utfall in ['DEL2A_RETTET', 'DEL2B_RETTET']:
                    aksjon = '<span style="color:var(--nav-red);font-weight:bold;">⚠️ MEDL → AVVI</span>'
                else:
                    aksjon = '<span style="color:#666;">❓ Manuell vurdering</span>'
            elif beh['resultat'] == 'REGISTRERT_UNNTAK':
                aksjon = '<span style="color:var(--nav-green);">✅ Behold gyldig</span>'
            else:
                aksjon = '<span style="color:#666;">-</span>'

            html += f'''
                                            <tr style="background:{bg_color};">
                                                <td>{beh['behandlingId']}</td>
                                                <td>{beh['behandlingType']}</td>
                                                <td style="color:{resultat_color};font-weight:bold;">{beh['resultat']}</td>
                                                <td>{beh['registrertDato'][:10]}</td>
                                                <td>{aksjon}</td>
                                            </tr>
'''
        html += '''
                                        </tbody>
                                    </table>
                                </div>
'''

    # Forklaring
    html += f'''
                                <div class="forklaring-box">
                                    <h5>💡 Hvorfor denne kategorien?</h5>
                                    {info['forklaring']}
                                </div>
                            </div>
                        </td>
                    </tr>
'''

html += '''
                </tbody>
            </table>
        </div>
    </div>

    <script>
        function toggle(idx) {
            const row = document.getElementById('exp-' + idx);
            row.classList.toggle('visible');
            const btn = row.previousElementSibling.querySelector('.toggle-btn');
            btn.textContent = row.classList.contains('visible') ? 'Skjul' : 'Vis detaljer';
        }

        function applyFilters() {
            const kat = document.getElementById('filter-kat').value;
            const search = document.getElementById('search').value.toLowerCase();
            const rows = document.querySelectorAll('#tbl tbody tr[data-idx]');
            let count = 0;

            rows.forEach((row) => {
                const rowKat = row.dataset.kat;
                const rowSak = row.dataset.sak.toLowerCase();
                const idx = row.dataset.idx;
                const expRow = document.getElementById('exp-' + idx);

                let showKat = !kat;
                if (kat === 'UNSAFE') {
                    showKat = rowKat === 'UNSAFE_A003_UBALANSE' || rowKat === 'IKKE_DEL2_KRITERIE';
                } else if (kat) {
                    showKat = rowKat === kat;
                }

                const showSearch = !search || rowSak.includes(search);
                const show = showKat && showSearch;

                row.style.display = show ? 'table-row' : 'none';
                if (expRow) {
                    if (!show) {
                        expRow.classList.remove('visible');
                        row.querySelector('.toggle-btn').textContent = 'Vis detaljer';
                    }
                }
                if (show) count++;
            });

            document.getElementById('count').textContent = count;
        }

        function resetFilters() {
            document.getElementById('filter-kat').value = '';
            document.getElementById('search').value = '';
            applyFilters();
        }

        function expandAll() {
            const rows = document.querySelectorAll('#tbl tbody tr[data-idx]');
            rows.forEach((row) => {
                if (row.style.display !== 'none') {
                    const idx = row.dataset.idx;
                    const expRow = document.getElementById('exp-' + idx);
                    if (expRow) {
                        expRow.classList.add('visible');
                        row.querySelector('.toggle-btn').textContent = 'Skjul';
                    }
                }
            });
        }

        function collapseAll() {
            document.querySelectorAll('.expand-row').forEach(row => {
                row.classList.remove('visible');
            });
            document.querySelectorAll('.toggle-btn').forEach(btn => {
                btn.textContent = 'Vis detaljer';
            });
        }
    </script>
</body>
</html>
'''

# Skriv til fil
with open('MELOSYS-7711-feil1-del2-detaljert.html', 'w') as f:
    f.write(html)

print(f"Rapport generert: MELOSYS-7711-feil1-del2-detaljert.html")
print(f"Totalt {len(alle_saker)} saker")
for utfall, data in sorted(stats.items()):
    print(f"  {utfall}: {data['saker']} saker, {data['behandlinger']} behandlinger")
