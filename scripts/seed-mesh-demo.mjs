// Development-only examples. Never imports reference-site users or content.
import fs from 'node:fs/promises'
import crypto from 'node:crypto'
const root=new URL('../',import.meta.url),checkpoint=new URL('.local/mesh/demo-seed.json',root)
try{await fs.access(checkpoint);console.log('Demo already seeded');process.exit(0)}catch{}
const secret=JSON.parse(await fs.readFile(new URL('.local/mesh/secrets.json',root),'utf8')),base='http://127.0.0.1:9091'
let token=''
async function request(path,data){const r=await fetch(base+path,{method:data===undefined?'GET':'POST',headers:{'Content-Type':'application/json',...(token?{Authorization:`Bearer ${token}`}:{})},...(data===undefined?{}:{body:JSON.stringify(data)})});const body=await r.json();if(!['200','3000'].includes(body.code))throw Error(path+': '+body.msg);return body}
const key=(await request('/user/public-key')).data
const encrypted=crypto.publicEncrypt({key:`-----BEGIN PUBLIC KEY-----\n${key}\n-----END PUBLIC KEY-----`,padding:crypto.constants.RSA_PKCS1_PADDING},Buffer.from(secret.adminPassword)).toString('base64')
token=(await request('/user/login',{username:'admin@mesh.local',password:encrypted})).data.accessToken
const saved={courses:[],posts:[]}
for(const [code,name,subtitle,subject,level] of [
 ['DEMO1001','Introduction to computing','计算基础 · 演示课程','Computer Science','UG'],
 ['DEMO2002','Data structures and algorithms','数据结构与算法 · 演示课程','Computer Science','UG'],
 ['DEMO3003','Design thinking','设计思维 · 演示课程','Design','UG'],
 ['DEMO6001','Applied machine learning','应用机器学习 · 演示课程','Computer Science','PG'],
 ['DEMO6002','Business analytics','商业分析 · 演示课程','Business','PG'],
 ['DEMO6003','Urban studies','城市研究 · 演示课程','Social Sciences','PG']
])saved.courses.push((await request('/console/mesh/courses',{code,name,subtitle,subject,level,credits:6,description:'独立开发环境的演示数据，用于体验课程评价、资料、小组与问答。'})).data)
for(const p of [
 {kind:'qa',title:'新学期选课，大家会怎样安排课程时间？',content:'欢迎交流选课思路、课表安排和学习经验。\n\n这是一条开发环境示例，点击可体验回答、同问、收藏和蹲帖。',tags:['选课','港大']},
 {kind:'qa',title:'第一次来港大，有哪些校园生活小建议？',content:'想了解图书馆、自习空间和校园出行的小技巧。欢迎分享你的经验。\n\n此内容为演示样例。',tags:['新生','校园']},
 {kind:'qa',title:'一起交流课程学习方法',content:'可以在课程详情中参与课程、创建学习小组并提出问题。此内容为演示样例。',tags:['这课咋样','学习'],courseId:saved.courses[0]},
 {kind:'job',category:'求职经验',company:'面试准备',role:'经验分享',title:'如何准备你的第一次实习面试',content:'从梳理项目经历、练习表达开始。此内容为开发环境示例。',tags:['实习']},
 {kind:'job',category:'行业资讯',company:'行业观察',title:'从校园项目到职业方向',content:'记录兴趣与项目经验，寻找适合自己的方向。此内容为演示样例。',tags:['求职']},
 {kind:'job',category:'简历',company:'简历互评',title:'一起完善项目经历的表达',content:'欢迎分享你的简历准备思路。此内容为演示样例。',tags:['校招']},
 {kind:'rental',rentalType:'roommate',title:'西营盘附近寻找合租伙伴',content:'希望找到作息规律、爱干净的同学。这是一条演示信息，不是真实房源需求。',region:'西营盘',budgetMin:4500,budgetMax:6500,habits:['不吸烟','爱干净'],tags:[]},
 {kind:'rental',rentalType:'wanted',title:'寻找通勤方便的小房间',content:'考虑坚尼地城及周边地区。这是一条演示信息，不是真实租房需求。',region:'坚尼地城',budgetMin:5000,budgetMax:7000,tags:[]}
]){const r=await request('/mesh/content',{images:[],...p});saved.posts.push(r.data);if(r.code==='3000')await request('/console/mesh/content/status',{kind:'post',id:r.data,status:1,reason:'仅限本地开发环境的演示样例'})}
await fs.writeFile(checkpoint,JSON.stringify(saved,null,2));console.log(`Seeded ${saved.courses.length} courses and ${saved.posts.length} demonstration posts in the new development database.`)
