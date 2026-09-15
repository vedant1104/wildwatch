import React, { useRef, useEffect } from 'react'

export default function VineTrendChart({ trend, years, color, title, subtitle }: { trend: number[], years: number[], color: string, title?: string, subtitle?: string }) {
    if (!trend || !years || trend.length === 0) {
        return <div style={{ padding: 20, fontSize: 13, color: "#5F5A48" }}>No trend data available for this species yet.</div>
    }

    // Single-point summary display
    if (trend.length === 1) {
        const count = trend[0]
        const showSubtitle = subtitle && title && subtitle.trim() !== '' && subtitle.trim() !== title.trim()
        return (
            <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
                <div>
                    <div style={{ fontFamily: 'Georgia, serif', fontStyle: 'italic', fontSize: 18 }}>{title}</div>
                    {showSubtitle && <div style={{ fontSize: 13, color: '#5F5A48' }}>{subtitle}</div>}
                </div>
                <div style={{ marginLeft: 'auto', textAlign: 'right' }}>
                    <div style={{ fontSize: 36, fontFamily: 'Georgia, serif', color: '#2E4A27' }}>{count.toLocaleString()}</div>
                    <div style={{ fontSize: 12, color: '#5F5A48' }}>observations recorded</div>
                </div>
            </div>
        )
    }

    // Two or more points: render vine chart
    // layout padding: reserve left space for y-axis and top space for count labels
    const w = 520, h = 240
    const padLeft = 56
    const padRight = 22
    const padTop = 28
    const padBottom = 28
    const max = Math.max(...trend)
    const min = Math.min(...trend)
    const pts = trend.map((v, i) => {
        const x = padLeft + (i * (w - padLeft - padRight)) / (trend.length - 1 || 1)
        const y = padTop + ((max - v) / (max - min || 1)) * (h - padTop - padBottom)
        return [x, y]
    })
    const d = pts.reduce((acc, [x, y], i) => i === 0 ? `M${x},${y}` : ((): any => { const [px, py] = pts[i - 1]; const mx = (px + x) / 2; return acc + ` C${mx},${py} ${mx},${y} ${x},${y}` })(), '')
    const areaD = `${d} L${pts[pts.length - 1][0]},${h - padBottom} L${pts[0][0]},${h - padBottom} Z`
    const ref = useRef<SVGPathElement | null>(null)
    useEffect(() => {
        if (ref.current) {
            const len = ref.current.getTotalLength();
            ref.current.style.strokeDasharray = `${len}`
            ref.current.style.strokeDashoffset = `${len}`
            ref.current.getBoundingClientRect()
            ref.current.style.transition = 'stroke-dashoffset 1.2s ease-out'
            ref.current.style.strokeDashoffset = '0'
        }
    }, [d])

    // Prepare y-axis ticks (min, two intermediates, max)
    const ticks = (() => {
        if (max === min) return [min]
        const step = (max - min) / 3
        return [min, Math.round(min + step), Math.round(min + step * 2), max]
    })()
    const labelY = h - padBottom + 16

    return (
        <div style={{ position: 'relative' }}>
            <svg viewBox={`0 0 ${w} ${h}`} width="100%" height={h}>
                <defs>
                    <linearGradient id="vineFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor={color} stopOpacity="0.45" />
                        <stop offset="100%" stopColor={color} stopOpacity="0.02" />
                    </linearGradient>
                </defs>
                {/* y-axis line */}
                <line x1={padLeft} y1={padTop} x2={padLeft} y2={h - padBottom} stroke="#CFCAC0" strokeWidth={1} />
                {/* y-axis ticks and labels */}
                {ticks.map((tv, idx) => {
                    const ty = padTop + ((max - tv) / (max - min || 1)) * (h - padTop - padBottom)
                    return (
                        <g key={idx}>
                            <line x1={padLeft - 6} y1={ty} x2={padLeft} y2={ty} stroke="#CFCAC0" strokeWidth={1} />
                            <text x={padLeft - 8} y={ty + 4} fontSize={11} fill={'#1C231E'} textAnchor="end">{tv.toLocaleString()}</text>
                        </g>
                    )
                })}
                <path d={areaD} fill="url(#vineFill)" />
                <path ref={ref} d={d} fill="none" stroke={color} strokeWidth={4.5} strokeLinecap="round" />
                {pts.map(([x, y], i) => (
                    <g key={i}>
                        <circle cx={x} cy={y} r={6} fill={i === pts.length - 1 ? '#E0A247' : color} stroke="#F3EEE1" strokeWidth={2} />
                        {/* count label above each point */}
                        <text x={x} y={y - 10} fontSize={11} fill={'#1C231E'} textAnchor="middle">{trend[i].toLocaleString()}</text>
                        {/* year label below each point */}
                        {years && years[i] != null && (
                            <text x={x} y={labelY} fontSize={11} fill={'#1C231E'} textAnchor="middle">{years[i]}</text>
                        )}
                    </g>
                ))}
            </svg>
        </div>
    )
}
