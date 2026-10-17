import fs from 'node:fs/promises'
import crypto from 'node:crypto'
import assert from 'node:assert/strict'
const root=new URL('../',import.meta.url)
const secret=JSON.parse(await fs.readFile(new URL('.local/mesh/secrets.json',root),'utf8'))
const base='http://127.0.0.1:9092'
async function request(path,data,token){
 const response=await fetch(base+path,{method:data===undefined?'GET':'POST',headers:{'Content-Type':'application/json',...(token?{Authorization:`Bearer ${token}`}:{})},...(data===undefined?{}:{body:JSON.stringify(data)})})
 return {status:response.status,body:await response.json()}
}
async function ok(path,data,token){const r=await request(path,data,token);assert.equal(r.body.code,'200',path+': '+r.body.msg);return r.body.data}
const publicKey=await ok('/user/public-key')
const encrypted=crypto.publicEncrypt({key:`-----BEGIN PUBLIC KEY-----\n${publicKey}\n-----END PUBLIC KEY-----`,padding:crypto.constants.RSA_PKCS1_PADDING},Buffer.from(secret.adminPassword)).toString('base64')
const admin=await ok('/user/login',{username:'admin@mesh.local',password:encrypted})
const token=admin.accessToken
assert.equal((await request('/mesh/content',{title:'unauthorized'})).status,401)
assert.equal(await ok('/mesh/public/session'),null)
assert.equal((await ok('/mesh/public/session',undefined,token)).userId,'mesh-admin')
const suffix=Date.now()
const id=await ok('/mesh/content',{kind:'qa',title:`联调问题 ${suffix}`,content:'<p>仅用于独立环境的自动验证</p>',tags:['联调'],images:[],anonymous:true},token)
assert.equal(typeof id,'string')
const guest=await ok('/mesh/public/content/'+id)
assert.equal(guest.locked,true);assert.equal(guest.content,'');assert.equal(guest.authorId,undefined)
const own=await ok('/mesh/public/content/'+id,undefined,token)
assert.equal(own.authorName,'匿名用户');assert.equal(own.authorId,undefined);assert.equal(own.own,true)
const answer=await ok(`/mesh/questions/${id}/answers`,{content:'<p>测试回答</p><script>alert(1)</script>',anonymous:false},token)
let replies=await ok(`/mesh/public/questions/${id}/answers`,undefined,token)
assert.equal(replies.length,1);assert.ok(!replies[0].content.includes('<script'))
await ok(`/mesh/answers/${answer}/interaction`,{kind:'accept',enabled:true},token)
await ok(`/mesh/questions/${id}/interaction`,{kind:'same',enabled:true},token)
await ok(`/mesh/questions/${id}/interaction`,{kind:'same',enabled:true},token)
await ok('/like/do?targetId='+id,{},token)
await ok('/favorite/do?targetId='+id,{},token)
const changed=await ok('/mesh/public/content/'+id,undefined,token)
assert.equal(changed.sameCount,1);assert.equal(changed.likeCount,1);assert.equal(changed.favoriteCount,1)
await ok(`/mesh/questions/${id}/interaction`,{kind:'dislike',enabled:true},token)
await ok(`/mesh/questions/${id}/interaction`,{kind:'dislike',enabled:true},token)
assert.ok((await ok('/mesh/public/content/'+id,undefined,token)).disliked)
await ok(`/mesh/questions/${id}/interaction`,{kind:'dislike',enabled:false},token)
assert.ok(!(await ok('/mesh/public/content/'+id,undefined,token)).disliked)
assert.equal((await request(`/mesh/questions/${id}/interaction`,{kind:'unknown',enabled:true},token)).body.code,'400')
replies=await ok(`/mesh/public/questions/${id}/answers`,undefined,token);assert.ok(replies[0].accepted)
const feed=await ok('/mesh/public/content?kind=qa&tag='+encodeURIComponent('联调')+'&size=2&current=1',undefined,token)
assert.ok(feed.records.some(p=>p.id===id));assert.equal(feed.size,2)
await ok('/post/delete?postId='+id,{},token)
assert.equal((await request('/mesh/public/content/'+id)).body.code,'404')
console.log('PASS: independent login/session, guest gate, anonymous content, publishing, sanitized answers, acceptance, idempotent same-question, legacy likes/favorites, filtering, deletion visibility.')
