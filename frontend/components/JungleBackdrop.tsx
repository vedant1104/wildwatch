import React from 'react'

function treeCluster(x: number, y: number, scale: number, dark: boolean): JSX.Element {
    return (
        <g transform={`translate(${x},${y}) scale(${scale})`}>
            <rect x="-6" y="0" width="12" height="70" fill={BARK} />
            <ellipse cx="0" cy="-15" rx="62" ry="48" fill={dark ? CANOPY_MID : CANOPY_LIGHT} />
            <ellipse cx="-40" cy="10" rx="38" ry="32" fill={CANOPY_DARK} />
            <ellipse cx="42" cy="5" rx="34" ry="30" fill={CANOPY_VIVID} opacity="0.9" />
        </g>
    );
}

function JungleScene() {
    return (
        <svg viewBox="0 0 1000 1400" width="100%" height="100%" preserveAspectRatio="xMidYMin slice" style={{ position: "absolute", inset: 0 }}>
            <defs>
                <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={SKY_DEEP} />
                    <stop offset="30%" stopColor="#1E4038" />
                    <stop offset="100%" stopColor={CANOPY_DARK} />
                </linearGradient>
            </defs>
            <rect width="1000" height="1400" fill="url(#sky)" />
            <circle cx="770" cy="110" r="66" fill={AMBER} opacity="0.8" />

            <path d="M0,240 Q150,190 320,230 T650,220 T1000,240 L1000,1400 L0,1400 Z" fill={CANOPY_DARK} opacity="0.7" />

            <path d="M0,330 C220,290 260,370 480,340 C700,310 760,390 1000,350 C900,470 780,430 680,520 C560,610 640,700 520,760 C420,810 460,900 360,940 L0,940 Z" fill={RIVER} opacity="0.9" />
            <path d="M0,342 C220,308 260,378 480,352 C700,324 760,394 1000,362" fill="none" stroke={RIVER_LIGHT} strokeWidth="4" opacity="0.55" />

            <path d="M0,300 Q120,250 260,290 T520,270 T780,290 T1000,270 L1000,1400 L0,1400 Z" fill={CANOPY_MID} />

            {treeCluster(70, 210, 1.1, true)}
            {treeCluster(230, 260, 0.8, false)}
            {treeCluster(900, 190, 1.2, true)}
            {treeCluster(770, 300, 0.75, false)}
            {treeCluster(40, 480, 1.0, false)}
            {treeCluster(940, 520, 1.05, true)}
            {treeCluster(150, 650, 0.9, true)}
            {treeCluster(860, 700, 0.85, false)}
            {treeCluster(50, 850, 1.15, false)}
            {treeCluster(920, 880, 0.95, true)}

            <g transform="translate(160,410) scale(1.5)" fill={TIGER}>
                <ellipse cx="30" cy="18" rx="34" ry="16" />
                <circle cx="62" cy="8" r="13" />
                <rect x="53" y="-3" width="4" height="8" fill={TIGER} />
                <rect x="67" y="-3" width="4" height="8" fill={TIGER} />
                <path d="M0,18 Q-14,10 -22,-2" stroke={TIGER} strokeWidth="6" fill="none" strokeLinecap="round" />
                <rect x="6" y="26" width="6" height="16" fill={TIGER} />
                <rect x="22" y="28" width="6" height="16" fill={TIGER} />
                <rect x="40" y="28" width="6" height="16" fill={TIGER} />
                <rect x="52" y="26" width="6" height="16" fill={TIGER} />
                <g stroke={INK} strokeWidth="2.5" opacity="0.85">
                    <path d="M14,6 l0,14" /><path d="M24,4 l0,16" /><path d="M34,4 l0,16" /><path d="M44,6 l0,14" />
                    <path d="M58,2 l4,6" /><path d="M66,3 l4,5" />
                </g>
            </g>

            <g transform="translate(250,255) scale(0.9)" fill={INK} opacity="0.85">
                <circle cx="0" cy="0" r="10" />
                <ellipse cx="0" cy="16" rx="8" ry="12" />
                <path d="M-6,24 Q-16,34 -8,42" stroke={INK} strokeWidth="3" fill="none" />
            </g>

            {([[700, 140], [620, 190], [820, 230], [500, 160]] as [number, number][]).map(([bx, by], i: number) => (
                <g key={i} transform={`translate(${bx},${by})`} fill="none" stroke={INK} strokeWidth="3" opacity="0.75">
                    <path d="M0,0 Q9,-9 18,0" />
                    <path d="M18,0 Q27,-9 36,0" />
                </g>
            ))}

            <g transform="translate(560,570)" fill={RIVER_LIGHT} opacity="0.9">
                <ellipse cx="0" cy="0" rx="14" ry="7" />
                <path d="M-14,0 L-24,-7 L-24,7 Z" />
            </g>

            <path d="M0,940 Q250,900 500,935 T1000,920 L1000,1400 L0,1400 Z" fill={CANOPY_DARK} />
            {([[80, 960], [220, 990], [400, 955], [620, 1000], [800, 970], [940, 1005]] as [number, number][]).map(([bx, by], i: number) => (
                <ellipse key={i} cx={bx} cy={by} rx="60" ry="34" fill={CANOPY_VIVID} opacity="0.5" />
            ))}

        </svg>
    );
}

const CREAM = "#F3EEE1";
const INK = "#1C231E";
const SKY_DEEP = "#12282D";
const AMBER = "#E0A247";
const RIVER = "#2F7A78";
const RIVER_LIGHT = "#6FB2AB";
const CANOPY_DARK = "#1B2E18";
const CANOPY_MID = "#2E4A27";
const CANOPY_LIGHT = "#4C7038";
const CANOPY_VIVID = "#5C8A3A";
const BARK = "#5B3A29";
const TIGER = "#C6702B";

export default JungleScene;
