import React, { useEffect, useState, useRef, useMemo } from 'react'
import { Search, Loader2 } from 'lucide-react'
import Pagination from './Pagination'

type SpeciesItem = { scientificName: string, commonName?: string, observationCount?: number }

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'

export default function SpeciesCatalog({ species, onSelect, selected, error, region }: {
    species: SpeciesItem[],
    onSelect: (s: SpeciesItem) => void,
    selected: SpeciesItem | null,
    error?: string | null,
    region: string
}) {
    const [page, setPage] = useState(1)
    const perPage = 20

    const [query, setQuery] = useState('')
    const [searchResults, setSearchResults] = useState<SpeciesItem[] | null>(null)
    const [searching, setSearching] = useState(false)
    const [searchError, setSearchError] = useState<string | null>(null)
    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

    useEffect(() => {
        // Reset to first page when species list changes (region or search changed upstream)
        setPage(1)
    }, [species])

    useEffect(() => {
        if (debounceRef.current) clearTimeout(debounceRef.current)
        const q = query.trim()

        if (!q) {
            setSearchResults(null)
            setSearching(false)
            setSearchError(null)
            return
        }

        setSearching(true)
        setSearchError(null)
        debounceRef.current = setTimeout(() => {
            fetch(`${API_BASE_URL}/api/species/search?q=${encodeURIComponent(q)}&region=${encodeURIComponent(region)}`)
                .then((res) => {
                    if (!res.ok) throw new Error('Search failed')
                    return res.json()
                })
                .then((data: SpeciesItem[]) => {
                    setSearchResults(data)
                    setPage(1)
                })
                .catch(() => setSearchError('Search failed. Try again.'))
                .finally(() => setSearching(false))
        }, 300)

        return () => {
            if (debounceRef.current) clearTimeout(debounceRef.current)
        }
    }, [query, region])

    const isSearchMode = query.trim().length > 0
    const displayList = isSearchMode ? (searchResults ?? []) : species

    const total = displayList ? displayList.length : 0
    const totalPages = Math.max(1, Math.ceil(total / perPage))
    const start = (page - 1) * perPage
    const visible = useMemo(() => displayList ? displayList.slice(start, start + perPage) : [], [displayList, start])

    const activeError = isSearchMode ? searchError : error

    return (
        <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
                <Search size={14} color="#F3EEE1" />
                <input
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Search recorded species"
                    style={{
                        flex: 1,
                        background: '#EAE3D0',
                        border: '1px solid #CDC2A3',
                        borderRadius: 20,
                        padding: '7px 12px',
                        fontSize: 12.5,
                        color: '#1C231E',
                    }}
                />
                {searching && <Loader2 size={14} color="#F3EEE1" className="animate-spin" />}
            </div>

            <div style={{ border: '1px solid #1C231E', borderRadius: 10, overflow: 'hidden', background: '#EAE3D0' }}>
                {activeError ? (
                    <div style={{ padding: 12, color: '#C6702B' }}>{activeError}</div>
                ) : (
                    <div>
                        <div className="species-list" style={{ maxHeight: 320, overflowY: 'auto', background: '#EAE3D0' }}>
                            {isSearchMode && !searching && visible.length === 0 && (
                                <div style={{ padding: 20, fontSize: 13, color: '#5F5A48' }}>No species match that search.</div>
                            )}
                            {visible.map((s) => {
                                const common = s.commonName ? String(s.commonName).trim() : ''
                                const sci = s.scientificName ? String(s.scientificName).trim() : ''
                                const hasSubtitle = common.length > 0 && common !== sci
                                const padding = hasSubtitle ? '11px 14px' : '16px 14px'
                                return (
                                    <button key={s.scientificName} onClick={() => onSelect(s)} style={{ display: 'flex', justifyContent: 'space-between', width: '100%', padding, background: selected?.scientificName === s.scientificName ? '#DCD3B4' : '#EAE3D0', border: 'none', borderBottom: '1px solid #CDC2A3', cursor: 'pointer' }}>
                                        <div>
                                            <div style={{ fontFamily: 'Georgia, serif', fontStyle: 'italic', fontSize: 14 }}>{s.scientificName}</div>
                                            {hasSubtitle && <div style={{ fontSize: 11, color: '#5F5A48' }}>{s.commonName}</div>}
                                        </div>
                                        <div style={{ alignSelf: 'center', textAlign: 'right' }}>
                                            <div style={{ fontSize: 12, color: '#2E4A27', opacity: 0.95 }}>{(s.observationCount ?? 0).toLocaleString()}</div>
                                        </div>
                                    </button>
                                )
                            })}
                        </div>

                        {/* Pagination controls */}
                        {totalPages > 1 && (
                            <div style={{ padding: 10, borderTop: '1px solid #CDC2A3' }}>
                                <Pagination currentPage={page} totalPages={totalPages} onPageChange={(p) => setPage(p)} />
                            </div>
                        )}
                    </div>
                )}
            </div>
            <style jsx>{`
        .species-list::-webkit-scrollbar { width: 6px; }
        .species-list::-webkit-scrollbar-thumb { background: #2E4A27; border-radius: 3px; }
        .species-list::-webkit-scrollbar-track { background: transparent; }
      `}</style>
        </div>
    )
}