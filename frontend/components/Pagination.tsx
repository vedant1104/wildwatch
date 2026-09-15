import React from 'react'

type Props = {
  currentPage: number
  totalPages: number
  onPageChange: (p:number) => void
}

export default function Pagination({ currentPage, totalPages, onPageChange }: Props){
  if (totalPages <= 1) return null

  const pages: (number | '...')[] = []
  if (totalPages <= 7) {
    for (let i=1;i<=totalPages;i++) pages.push(i)
  } else {
    pages.push(1)
    const start = Math.max(2, currentPage - 2)
    const end = Math.min(totalPages - 1, currentPage + 2)
    if (start > 2) pages.push('...')
    for (let p = start; p <= end; p++) pages.push(p)
    if (end < totalPages - 1) pages.push('...')
    pages.push(totalPages)
  }

  return (
    <div style={{ display: 'flex', gap: 8, alignItems: 'center', justifyContent: 'center' }}>
      <button onClick={() => onPageChange(Math.max(1, currentPage - 1))} disabled={currentPage === 1} style={{ background: currentPage===1 ? '#F3EEE1' : '#2E4A27', color: currentPage===1 ? '#5F5A48' : '#F3EEE1', border: 'none', padding: '6px 10px', borderRadius: 6, cursor: currentPage===1 ? 'default' : 'pointer' }}>Previous</button>

      {pages.map((p, idx) => (
        p === '...'
          ? <div key={`e-${idx}`} style={{ padding: '6px 8px', color: '#5F5A48' }}>…</div>
          : <button key={p} onClick={() => onPageChange(p as number)} style={{ background: p===currentPage ? '#DCD3B4' : '#EAE3D0', color: '#1C231E', border: '1px solid #CDC2A3', padding: '6px 8px', borderRadius: 6, cursor: 'pointer' }}>{p}</button>
      ))}

      <button onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))} disabled={currentPage === totalPages} style={{ background: currentPage===totalPages ? '#F3EEE1' : '#2E4A27', color: currentPage===totalPages ? '#5F5A48' : '#F3EEE1', border: 'none', padding: '6px 10px', borderRadius: 6, cursor: currentPage===totalPages ? 'default' : 'pointer' }}>Next</button>
    </div>
  )
}
