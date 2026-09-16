import React, { useEffect, useState } from 'react'
import Pagination from './Pagination'

type SpeciesItem = { scientificName: string, commonName?: string, observationCount?: number }

export default function SpeciesCatalog({ species, onSelect, selected, error }:{ species:SpeciesItem[], onSelect:(s:SpeciesItem)=>void, selected:SpeciesItem | null, error?:string | null }){
  const [page, setPage] = useState(1)
  const perPage = 20

  useEffect(()=>{
    // Reset to first page when species list changes (region or search changed upstream)
    setPage(1)
  }, [species])

  const total = species ? species.length : 0
  const totalPages = Math.max(1, Math.ceil(total / perPage))
  const start = (page - 1) * perPage
  const visible = species ? species.slice(start, start + perPage) : []

  return (
    <div>
      <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
        <div style={{ fontSize: 12, letterSpacing: '0.04em', color: '#F3EEE1' }}>Recorded species</div>
      </div>
      <div style={{ border: '1px solid #1C231E', borderRadius: 10, overflow: 'hidden', background: '#EAE3D0' }}>
        {error ? (
          <div style={{ padding: 12, color: '#C6702B' }}>{error}</div>
        ) : (
          <div>
            {visible.map((s)=> {
              const common = s.commonName ? String(s.commonName).trim() : ''
              const sci = s.scientificName ? String(s.scientificName).trim() : ''
              const hasSubtitle = common.length > 0 && common !== sci
              const padding = hasSubtitle ? '11px 14px' : '16px 14px'
              return (
                <button key={s.scientificName} onClick={()=>onSelect(s)} style={{ display:'flex', justifyContent:'space-between', width:'100%', padding, background: selected?.scientificName===s.scientificName? '#DCD3B4' : '#EAE3D0', border:'none', borderBottom:'1px solid #CDC2A3', cursor:'pointer' }}>
                  <div>
                    <div style={{ fontFamily: 'Georgia, serif', fontStyle:'italic', fontSize:14 }}>{s.scientificName}</div>
                    {hasSubtitle && <div style={{ fontSize:11, color:'#5F5A48' }}>{s.commonName}</div>}
                  </div>
                  <div style={{ alignSelf: 'center', textAlign: 'right' }}>
                    <div style={{ fontSize:12, color: '#2E4A27', opacity: 0.95 }}>{(s.observationCount ?? 0).toLocaleString()}</div>
                  </div>
                </button>
              )
            })}

            {/* Pagination controls */}
            <div style={{ padding: 10, borderTop: '1px solid #CDC2A3' }}>
              {/* Reusable pagination component */}
              <Pagination currentPage={page} totalPages={totalPages} onPageChange={(p)=>setPage(p)} />
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
