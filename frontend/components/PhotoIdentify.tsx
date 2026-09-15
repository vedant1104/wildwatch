"use client"
import React, { useState } from 'react'
import { ImagePlus, Upload } from 'lucide-react'

export default function PhotoIdentify({ region, apiBase }:{ region:string, apiBase:string }){
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState<{identification?:string, regionNote?:string, error?:string}|null>(null)
  const handle = async (e:any)=>{
    const file = e.target.files?.[0]
    if(!file) return
    setBusy(true); setResult(null)
    const fd = new FormData(); fd.append('image', file); fd.append('region', region)
    try{
      const r = await fetch(`${apiBase}/api/photo/identify`, { method:'POST', body: fd })
      const data = await r.json()
      if(!r.ok){ setResult({ error: data.error || 'Upload failed' }) }
      else { setResult({ identification: data.identification, regionNote: data.regionNote }) }
    }catch(err){ setResult({ error: 'Failed to identify. Try again.' }) }
    setBusy(false)
  }
  return (
    <div>
      <label style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:8, border:'1px dashed #CDC2A3', borderRadius:8, padding:'18px 10px', cursor:'pointer', color:'#5F5A48', textAlign:'center' }}>
        <div style={{ display:'flex', justifyContent:'center' }}>
          <ImagePlus size={24} color="#2E4A27" />
        </div>
        <div style={{ fontSize: 14, fontWeight: 600, color: '#2E4A27' }}>Upload a wildlife photo</div>
        <input type="file" accept="image/*" onChange={handle} style={{ display:'none' }} />
      </label>
      {busy && <div style={{ marginTop:12 }}>Examining image…</div>}
      {result && result.error && <div style={{ marginTop:12, color:'#C6702B' }}>{result.error}</div>}
      {result && result.identification && (
        <div style={{ marginTop:12, borderTop:'1px solid #CDC2A3', paddingTop:10 }}>
          <div style={{ fontFamily:'Georgia, serif', fontStyle:'italic', fontSize:14 }}>{result.identification}</div>
          <div style={{ fontSize:11, color:'#E0A247', marginTop:4 }}>{result.regionNote}</div>
        </div>
      )}
    </div>
  )
}
