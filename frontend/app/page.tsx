"use client"
import React, { useEffect, useState } from 'react'
import JungleBackdrop from '../components/JungleBackdrop'
import VineTrendChart from '../components/VineTrendChart'
import SightingMap from '../components/SightingMap'
import SpeciesCatalog from '../components/SpeciesCatalog'
import PhotoIdentify from '../components/PhotoIdentify'
import AskNaturalist from '../components/AskNaturalist'

const REGIONS = [
    { id: 'Karnataka', lat: 12.97, lng: 77.59 },
    { id: 'Kerala', lat: 10.85, lng: 76.27 },
    { id: 'Tamil Nadu', lat: 11.1271, lng: 78.6569 },
    { id: 'Maharashtra', lat: 19.076, lng: 72.8777 }
]

const API = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'

export default function Page() {
    const [region, setRegion] = useState(REGIONS[0].id)
    const [speciesList, setSpeciesList] = useState<any[]>([])
    const [selected, setSelected] = useState<any>(null)
    const [trend, setTrend] = useState<{ years: number[], trend: number[] } | null>(null)
    const [sightings, setSightings] = useState<any[]>([])
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        setError(null)
        fetch(`${API}/api/species?region=${encodeURIComponent(region)}`)
            .then(r => r.ok ? r.json() : Promise.reject('Failed'))
            .then(data => setSpeciesList(data))
            .catch(() => setError("Couldn't load species for this region. Try again."))

        const regionDef = REGIONS.find(r => r.id === region)!
        fetch(`${API}/api/observations/nearby?lat=${regionDef.lat}&lng=${regionDef.lng}&radiusKm=50&region=${encodeURIComponent(region)}`)
            .then(r => r.ok ? r.json() : Promise.reject('Failed'))
            .then(data => setSightings(data))
            .catch(() => { })
    }, [region])

    useEffect(() => {
        if (!selected) return
        setTrend(null)
        fetch(`${API}/api/species/${encodeURIComponent(selected.scientificName)}/trend?region=${encodeURIComponent(region)}`)
            .then(r => r.ok ? r.json() : Promise.reject('Failed'))
            .then((data: any[]) => {
                // Expecting data like [{ year: 2024, count: 342 }, ...]
                if (!Array.isArray(data) || data.length === 0) {
                    setTrend(null)
                    return
                }
                const years = data.map(d => d.year)
                const trendArr = data.map(d => d.count)
                setTrend({ years, trend: trendArr })
            })
            .catch(() => setTrend(null))
    }, [selected, region])

    return (
        <div style={{ position: 'relative', minHeight: '100vh', overflowX: 'hidden' }}>
            <div style={{ position: 'fixed', inset: 0, zIndex: 0 }}>
                <JungleBackdrop />
            </div>
            <div style={{ position: 'relative', zIndex: 2, maxWidth: 1040, margin: '0 auto', padding: '40px 24px 56px' }}>
                <div style={{ textAlign: 'center', marginBottom: 30 }}>
                    <div style={{ fontSize: 12, letterSpacing: '0.15em', color: '#F3EEE1', opacity: 0.9 }}>FIELD RECORDS · {region.toUpperCase()}</div>
                    <h1 style={{ fontFamily: 'Georgia, serif', fontSize: 52, color: '#F3EEE1', margin: '6px 0 0' }}>WildWatch</h1>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
                    <select value={region} onChange={(e) => setRegion(e.target.value)} style={{ background: '#EAE3D0', borderRadius: 20, padding: '6px 14px' }}>
                        {REGIONS.map(r => <option key={r.id} value={r.id}>{r.id}</option>)}
                    </select>
                </div>

                {/* Selected species trend card placed near the top, before the catalog/map grid */}
                <div style={{ marginBottom: 28 }}>
                    {selected && trend && (
                        <div style={{ background: '#EAE3D0', padding: 18, borderRadius: 10, marginBottom: 18 }}>
                            <h3 style={{ fontFamily: 'Georgia, serif', fontStyle: 'italic' }}>{selected.scientificName}</h3>
                            <VineTrendChart trend={trend.trend} years={trend.years} color="#2E4A27" title={selected.scientificName} subtitle={selected.commonName} />
                        </div>
                    )}
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1.1fr) minmax(0, 0.9fr)', gap: 24, marginBottom: 28 }}>
                    <div>
                        <SpeciesCatalog species={speciesList} onSelect={setSelected} selected={selected} error={error} region={region} />          </div>

                    <div>
                        <div style={{ fontSize: 12, letterSpacing: '0.04em', color: '#F3EEE1', marginBottom: 10 }}>Recent sighting locations</div>
                        <div style={{ border: '1px solid #1C231E', borderRadius: 10, padding: 10, background: '#EAE3D0' }}>
                            <SightingMap points={sightings} />
                        </div>
                    </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) minmax(0, 1fr)', gap: 24, alignItems: 'start' }}>
                    <div style={{ border: '1px solid #1C231E', borderRadius: 10, padding: 18, background: '#EAE3D0' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
                            <div style={{ fontSize: 12, color: '#2E4A27' }}>Identify from a photo</div>
                        </div>
                        <PhotoIdentify region={region} apiBase={API} />
                    </div>

                    <div style={{ border: '1px solid #1C231E', borderRadius: 10, padding: 18, background: '#EAE3D0' }}>
                        <AskNaturalist region={region} apiBase={API} />
                    </div>
                </div>

                <div style={{ marginTop: 28, fontSize: 11, color: '#F3EEE1', opacity: 0.75, borderTop: '1px solid rgba(255,255,255,0.2)', paddingTop: 14 }}>
                    Preview shown with live API where available. Backend must allow CORS from http://localhost:3000 for local dev.
                </div>

                {/* Duplicate trend card intentionally removed; single card lives at the top */}

            </div>
        </div>
    )
}
