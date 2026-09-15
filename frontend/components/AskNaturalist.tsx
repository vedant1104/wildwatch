"use client"
import React, { useEffect, useRef, useState } from 'react'
import ReactMarkdown from 'react-markdown'
import { Leaf } from 'lucide-react'

export default function AskNaturalist({ region, apiBase }:{ region:string, apiBase:string }){
  const [q, setQ] = useState('')
  const [log, setLog] = useState<{role:'assistant'|'user', text:string}[]>([{role:'assistant', text:'Ask about wildlife recorded in this region.'}])
  const [busy, setBusy] = useState(false)
  const containerRef = useRef<HTMLDivElement|null>(null)
  const bottomRef = useRef<HTMLDivElement|null>(null)

  useEffect(()=>{
    // auto-scroll to bottom
    if(bottomRef.current){ bottomRef.current.scrollIntoView({ behavior: 'smooth', block: 'end' }) }
  }, [log, busy])

  const ask = async ()=>{
    const question = q.trim(); if(!question) return
    setLog(l=>[...l,{role:'user', text:question}]); setQ(''); setBusy(true)
    try{
      const r = await fetch(`${apiBase}/api/qa/ask`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ question, region }) })
      const data = await r.json()
      if(r.ok){ setLog(l=>[...l,{role:'assistant', text: data.answer}]) }
      else { setLog(l=>[...l,{role:'assistant', text: 'Could not answer right now.'}]) }
    }catch(err){ setLog(l=>[...l,{role:'assistant', text: 'Error contacting service.'}]) }
    setBusy(false)
  }

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:8 }}>
      <div ref={containerRef} style={{ flex:1, display:'flex', flexDirection:'column', gap:8, height:280, overflowY:'auto', padding:8 }}>
        {log.map((m,i)=> (
          <div key={i} style={{ display:'flex', gap:8, justifyContent: m.role==='user' ? 'flex-end' : 'flex-start' }}>
            {m.role === 'assistant' && (
              <div style={{ width:28, display:'flex', alignItems:'flex-end' }}>
                <Leaf size={16} color="#2E4A27" />
              </div>
            )}

            <div style={{
              maxWidth: '78%',
              background: m.role==='user' ? '#2E4A27' : '#EAE3D0',
              color: m.role==='user' ? '#F3EEE1' : '#1C231E',
              padding: '10px 12px',
              borderRadius: m.role==='user' ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
              fontFamily: m.role==='user' ? 'system-ui, sans-serif' : 'Georgia, serif',
              fontStyle: m.role==='assistant' ? 'italic' : 'normal',
              fontSize: 13,
              lineHeight: 1.4
            }}>
              {m.role === 'assistant' ? (
                <ReactMarkdown>{m.text}</ReactMarkdown>
              ) : (
                <div style={{ fontWeight: 600 }}>{m.text}</div>
              )}
            </div>

            {m.role === 'user' && <div style={{ width:28 }} />}
          </div>
        ))}

        {busy && (
          <div style={{ display:'flex', gap:8, justifyContent: 'flex-start' }}>
            <div style={{ width:28, display:'flex', alignItems:'flex-end' }}>
              <Leaf size={16} color="#2E4A27" />
            </div>
            <div style={{ background:'#EAE3D0', color:'#1C231E', padding:'10px 12px', borderRadius:'16px 16px 16px 4px', fontFamily:'Georgia, serif', fontStyle:'italic' }}>
              <TypingDots />
            </div>
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      <div style={{ display:'flex', gap:8, alignItems:'center' }}>
        <input value={q} onChange={e=>setQ(e.target.value)} placeholder="What's commonly seen near Bengaluru?" style={{ flex:1, border:'1px solid #CDC2A3', borderRadius:999, padding:'10px 14px', fontSize:13 }} />
        <button onClick={ask} disabled={q.trim().length===0} style={{ background: q.trim().length===0 ? '#A9B49E' : '#2E4A27', color:'#F3EEE1', border:'none', borderRadius:'50%', width:40, height:40, display:'flex', alignItems:'center', justifyContent:'center', cursor: q.trim().length===0 ? 'default' : 'pointer' }}>
          →
        </button>
      </div>
    </div>
  )
}

function TypingDots(){
  return (
    <div style={{ display:'flex', gap:6, alignItems:'center' }}>
      <span className="dot" style={dotStyle(0)} />
      <span className="dot" style={dotStyle(150)} />
      <span className="dot" style={dotStyle(300)} />
      <style>{`
        @keyframes pulse { 0% { transform: translateY(0); opacity: 0.6 } 50% { transform: translateY(-4px); opacity: 1 } 100% { transform: translateY(0); opacity: 0.6 } }
      `}</style>
    </div>
  )
}

function dotStyle(delay:number){
  return {
    display:'inline-block', width:8, height:8, borderRadius:99, background:'#2E4A27', animation: `pulse 900ms ${delay}ms infinite ease-in-out`
  } as React.CSSProperties
}
