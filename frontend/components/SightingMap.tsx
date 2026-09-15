import React from 'react'
import { ResponsiveContainer, ScatterChart, Scatter, XAxis, YAxis, CartesianGrid, Tooltip, ZAxis } from 'recharts'

export default function SightingMap({ points }:{points:any[]}){
  const safe = (points && points.length) ? points : []
  return (
    <div style={{ width: '100%', minWidth: 0 }}>
      <ResponsiveContainer width="100%" height={200}>
        <ScatterChart margin={{ top: 10, right: 12, bottom: 0, left: -18 }}>
          <CartesianGrid stroke="#CDC2A3" />
          <XAxis type="number" dataKey="lng" tick={{ fontSize: 10, fill: '#1C231E' }} stroke="#CDC2A3" tickLine={false} domain={["dataMin - 0.02","dataMax + 0.02"]} />
          <YAxis type="number" dataKey="lat" tick={{ fontSize: 10, fill: '#1C231E' }} stroke="#CDC2A3" tickLine={false} domain={["dataMin - 0.02","dataMax + 0.02"]} />
          <ZAxis type="number" dataKey="count" range={[40,180]} />
          <Tooltip cursor={{ strokeDasharray: '3 3' }} contentStyle={{ background: '#EAE3D0', border: '1px solid #1C231E', borderRadius: 2, fontSize: 12 }} />
          <Scatter data={safe.map(p=>({ x: p.lng, y: p.lat, lat: p.lat, lng: p.lng, count: p.count || 8 }))} fill="#E0A247" fillOpacity={0.85} />
        </ScatterChart>
      </ResponsiveContainer>
    </div>
  )
}
