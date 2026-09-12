export interface User { id: number; displayName: string; email: string }
export interface Progress { level: number; current: number; required: number; total: number }
export interface Item { id: number; name: string; slot: string; appearance: string; description: string; price: number; inventoryId: number | null; owned: boolean; equipped: boolean }
export interface Hero { displayName: string; avatar: string; progress: Progress; gold: number; streak: number; bestStreak: number; timezone: string; pendingTimezone: string | null; attributes: { type: string; progress: Progress }[]; cosmetics: Item[] }
export interface Quest { id: number; title: string; description: string; category: string; difficulty: string; dueDate: string | null; completed: boolean; archived: boolean; createdAt: string; updatedAt: string; completedAt: string | null; rewardXp: number; rewardGold: number }
export interface Activity { id: number; kind: string; description: string; xp: number; gold: number; createdAt: string }
export interface Page<T> { content: T[]; page: number; totalPages: number; totalElements: number }
export interface LeaderboardEntry { rank:number; userId:number; displayName:string; avatar:string; progress:Progress; weeklyXp:number; currentUser:boolean; projectedRewardXp:number }
export interface Leaderboard { weekStart:string; weekEnd:string; secondsRemaining:number; rankings:Page<LeaderboardEntry>; currentUser:LeaderboardEntry }
export class ApiError extends Error {
  status: number; fields: Record<string,string>
  constructor(status: number, message: string, fields: Record<string,string> = {}) { super(message); this.status=status; this.fields=fields }
}
let csrf: { token: string; headerName: string } | null = null
export async function api<T>(path: string, method='GET', body?: unknown): Promise<T> {
  try {
    if (method !== 'GET' && !csrf) {
      const result = await fetch('/api/auth/csrf', { credentials:'same-origin' })
      if (!result.ok) throw new ApiError(result.status, 'Unable to establish a secure connection. Please retry.')
      csrf = await result.json()
    }
    const response = await fetch('/api'+path, {
      method, credentials:'same-origin',
      headers: { ...(body ? { 'Content-Type':'application/json' } : {}), ...(method !== 'GET' && csrf ? { [csrf.headerName]: csrf.token } : {}) },
      body: body ? JSON.stringify(body) : undefined
    })
    if (path.startsWith('/auth/') && method !== 'GET' || response.status === 403) csrf=null
    if (!response.ok) {
      const error = await response.json().catch(()=>({}))
      const fallback=response.status>=500?'Cannot reach LevelForge right now. Check that the server is running, then retry.':'Something went wrong. Please try again.'
      const fieldDetails=Object.entries(error.fields || {}).map(([name,message])=>name+': '+message).join(' · ')
      throw new ApiError(response.status, fieldDetails || error.message || fallback, error.fields)
    }
    return response.status === 204 ? undefined as T : response.json()
  } catch (error) {
    if (error instanceof ApiError) throw error
    throw new ApiError(0, 'Cannot reach LevelForge. Check your connection and that the server is running, then retry.')
  }
}
export const label = (value: string) => value.charAt(0) + value.slice(1).toLowerCase()
export const categories = ['CODING','STUDY','EXERCISE','READING','MEDITATION','COMMUNICATION']
