import { BookOpen, Shield, Sun } from 'lucide-react'
import type { Item } from './api'
import { useId } from 'react'
export function Badge({ appearance }: { appearance: string }) {
  const Icon = appearance === 'book' ? BookOpen : appearance === 'shield' ? Shield : Sun
  return <span className="badge-insignia"><Icon size={18}/></span>
}
export function Character({ avatar='RANGER', cosmetics=[], small=false }: { avatar?: string; cosmetics?: Item[]; small?: boolean }) {
  const uniqueId=useId().replace(/:/g,'')
  const frame = cosmetics.find(i=>i.slot==='FRAME')?.appearance || 'default'
  const cloak = avatar==='MAGE'?'#706483':avatar==='KNIGHT'?'#788d9a':'#547b6d'
  return <div className={'portrait frame-'+frame+(small?' small':'')} role="img" aria-label={labelAvatar(avatar)+' character, '+frame+' frame'}>
    <svg viewBox="0 0 320 340" preserveAspectRatio="xMidYMid slice" fill="none" aria-hidden="true">
      <defs><radialGradient id={uniqueId+'sky'}><stop stopColor="#394c43"/><stop offset="1" stopColor="#172124"/></radialGradient><linearGradient id={uniqueId+'cape'} x2="1" y2="1"><stop stopColor={cloak}/><stop offset="1" stopColor="#182f2d"/></linearGradient></defs>
      <rect width="320" height="340" fill={'url(#'+uniqueId+'sky)'}/>
      <circle cx="165" cy="117" r="88" stroke="#a7b09a" strokeOpacity=".16"/><circle cx="165" cy="117" r="74" stroke="#a7b09a" strokeOpacity=".12"/>
      <path d="M0 246 40 146 78 249 102 174 152 277 225 168 256 247 292 123 320 231V340H0Z" fill="#101d1c" opacity=".8"/>
      <path d="m37 83 2-9 2 9 9 2-9 2-2 9-2-9-9-2Zm222-28 1-6 2 6 6 2-6 1-2 6-1-6-6-1Z" fill="#c4b47f"/>
      <path d="M66 340 87 226Q103 202 129 194H195Q230 207 240 237L262 340Z" fill={'url(#'+uniqueId+'cape)'} stroke="#8ca18b" strokeOpacity=".35"/>
      <path d="m127 199 36 81 34-81-17-20h-36Z" fill="#202b2c"/><path d="m150 173-4 33 18 16 18-18-5-30" fill="#c79873"/>
      <path d="M125 115Q123 69 164 68Q207 69 201 121L190 164Q165 196 138 164Z" fill="#d4ab83"/>
      <path d="M123 131Q108 88 134 72Q165 43 192 76Q218 95 202 134L189 103Q160 105 146 84Q135 119 123 131" fill="#34302c"/>
      <path d="m137 131 12-1m25 0 12 1" stroke="#403a31" strokeWidth="3" strokeLinecap="round"/><path d="m160 137-3 13 8 1m-13 12q11 6 21-1" stroke="#9a6e54" strokeWidth="2" strokeLinecap="round"/>
      <path d="M128 194 103 227 153 266 138 215m58-21 22 33-48 40 17-50" fill={cloak} stroke="#91a68a" strokeOpacity=".4"/>
      <path d="m95 277 130 14-4 21-131-14" fill="#544838"/><rect x="146" y="282" width="24" height="24" rx="3" fill="#baa36a"/><rect x="151" y="287" width="14" height="14" rx="1" fill="#4b5144"/>
      <path d="m220 300 25-160 8 1-18 161" fill="#897353"/><path d="m242 145-7-24 18-29 10 32-13 23" fill="#a7b2aa"/><path d="m246 102 0 35" stroke="#d4d5b5"/>
      <circle cx="165" cy="232" r="9" fill="#c1a973"/><path d="m161 232 4-5 4 5-4 5Z" fill="#556b58"/>
      {avatar==='MAGE'&&<path d="m107 104 57-89 48 91q-53-19-105-2" fill="#706483" stroke="#b0a2c1"/>}
      {avatar==='KNIGHT'&&<path d="M123 119V95q43-62 81 0v25l-18-12-21-28-22 29Z" fill="#879b9f" stroke="#c5cccc"/>}
      <path d="M0 326q160-27 320 0v14H0Z" fill="#111a1b" opacity=".5"/>
    </svg>
    <span className="portrait-corner tl"/><span className="portrait-corner br"/>
  </div>
}
function labelAvatar(value: string) { return value.charAt(0)+value.slice(1).toLowerCase() }
