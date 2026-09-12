// Run against a disposable local environment. Creates uniquely named test accounts and actual quests.
import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
const base=process.env.API_URL||'http://127.0.0.1:8080/api'
class Client {
  cookie=''; token=null
  async request(path,method='GET',body,expected=200,secure=true) {
    if(method!=='GET'&&secure&&!this.token) this.token=await this.request('/auth/csrf')
    const r=await fetch(base+path,{method,headers:{...(this.cookie?{Cookie:this.cookie}:{}),...(body?{'Content-Type':'application/json'}:{}),...(method!=='GET'&&secure&&this.token?{[this.token.headerName]:this.token.token}:{})},body:body?JSON.stringify(body):undefined})
    for(const cookie of r.headers.getSetCookie()) if(cookie.startsWith('SESSION='))this.cookie=cookie.split(';')[0]
    const data=await r.text()
    assert.equal(r.status,expected,method+' '+path+': '+data)
    if(path.startsWith('/auth/')&&method!=='GET')this.token=null
    return data?JSON.parse(data):null
  }
}
const a=new Client(), b=new Client(), outsider=new Client()
const email='api-'+randomUUID()+'@example.com',password='Aa9!'+randomUUID()
await a.request('/character','GET',undefined,401)
await a.request('/auth/signup','POST',{displayName:'API tester',email,password,timezone:'UTC'},403,false)
await a.request('/auth/csrf')
const old=a.cookie
await a.request('/auth/signup','POST',{displayName:'API tester',email,password,timezone:'UTC'},201)
assert.notEqual(a.cookie,old,'Signup rotates session')
let h=await a.request('/character')
assert.equal(h.progress.total,0);assert.equal(h.gold,0);assert.equal(h.streak,0)
await b.request('/auth/login','POST',{email,password:'wrong-password'},401)
await b.request('/auth/login','POST',{email,password})
await outsider.request('/auth/signup','POST',{displayName:'Other user',email:'other-'+randomUUID()+'@example.com',password,timezone:'UTC'},201)
let q=await a.request('/tasks','POST',{title:'  Review architecture  ',description:'Real API test quest',category:'CODING',difficulty:'MEDIUM',dueDate:null},201)
assert.equal(q.title,'Review architecture');assert.equal(q.rewardXp,25)
await outsider.request('/tasks/'+q.id,'GET',undefined,404)
await outsider.request('/tasks/'+q.id+'/complete','POST',undefined,404)
q=await a.request('/tasks/'+q.id,'PATCH',{title:'Review architecture carefully',description:'Updated',category:'STUDY',difficulty:'HARD',dueDate:null})
await a.request('/auth/csrf');await b.request('/auth/csrf')
const result=await Promise.all([a.request('/tasks/'+q.id+'/complete','POST'),b.request('/tasks/'+q.id+'/complete','POST')])
assert.equal(result.filter(r=>r.applied).length,1)
h=await b.request('/character');assert.equal(h.progress.total,50);assert.equal(h.gold,20)
const second=await a.request('/tasks','POST',{title:'Finish a second challenge',category:'EXERCISE',difficulty:'HARD'},201)
const level=await a.request('/tasks/'+second.id+'/complete','POST')
assert.equal(level.levelUp,true);assert.equal(level.character.progress.level,2)
await a.request('/tasks/'+q.id,'PATCH',{title:'Cannot change history',category:'STUDY',difficulty:'EASY'},409)
await a.request('/shop/1/purchase','POST',undefined,409)
const purchased=await Promise.all([a.request('/shop/4/purchase','POST'),b.request('/shop/4/purchase','POST')])
assert.equal(purchased.filter(r=>r.applied).length,1)
const owned=await a.request('/inventory');assert.equal(owned.length,1)
await outsider.request('/inventory/'+owned[0].inventoryId+'/equip','PATCH',undefined,404)
await a.request('/inventory/'+owned[0].inventoryId+'/equip','PATCH')
h=await b.request('/character');assert.equal(h.gold,10);assert.equal(h.cosmetics[0].id,4)
await a.request('/tasks/'+q.id,'DELETE',undefined,204)
const duplicate=await a.request('/tasks/'+q.id+'/complete','POST');assert.equal(duplicate.applied,false)
await a.request('/profile','PATCH',{displayName:'Updated adventurer',timezone:'America/New_York',avatar:'MAGE'})
assert.equal((await b.request('/character')).avatar,'MAGE')
assert.equal((await a.request('/activity?size=1')).totalPages,3)
await a.request('/auth/logout','POST',undefined,204)
await a.request('/auth/me','GET',undefined,401)
assert.equal((await b.request('/auth/me')).email,email,'Logout leaves independent session intact')
await b.request('/auth/logout','POST',undefined,204)
await outsider.request('/auth/logout','POST',undefined,204)
console.log('PASS: HTTP auth/CSRF/session rotation; CRUD; ownership; concurrent completion; level-up; purchases; equip; independent sessions; persistence; logout.')
