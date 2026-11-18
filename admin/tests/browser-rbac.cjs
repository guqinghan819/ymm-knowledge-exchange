// Browser integration against isolated fixtures; never writes to the real backend.
const { spawn } = require('node:child_process')
const http = require('node:http')
const fs = require('node:fs')
const path = require('node:path')
const os = require('node:os')
const assert = require('node:assert/strict')
const { pathToFileURL } = require('node:url')

async function main() {
  const { adminPages } = await import(pathToFileURL(path.resolve('src/utils/authorization.js')))
  let keys = adminPages.map(p => p.permission), profileRoles = ['admin']
  let permissions = adminPages.map((p, i) => ({ id: String(i + 1), parentId: '0', name: ['仪表盘', '管理用户', '管理角色', '管理权限', '管理笔记', '管理频道', '管理话题'][i], type: 2, permissionKey: p.permission, menuUrl: p.path, menuIcon: '', status: 0, sort: i }))
  permissions[1].parentId = '1'
  let assigned = ['5'], userRoles = ['2'], mutations = [], createdPermission
  const articles = [
    { id: '101', title: '历史已通过图文', status: 1, type: 0 },
    { id: '102', title: '人工已通过视频', status: 1, type: 1, reviewMethod: 'MANUAL' },
    { id: '103', title: 'AI已通过图文', status: 1, type: 0, reviewMethod: 'AI' },
    { id: '104', title: '等待人工复核', status: 0, type: 0, reviewTaskStatus: 'HUMAN_REQUIRED' },
    { id: '105', title: '等待视频审核', status: 0, type: 1 },
    { id: '106', title: '等待AI审核', status: 0, type: 0, reviewTaskStatus: 'RUNNING' },
    { id: '107', title: '历史已下架', status: 3, type: 0 },
    { id: '108', title: '历史已删除', status: 2, type: 0 }
  ]
  const root = path.resolve(process.env.RBAC_BUILD_DIR || '.codex-build/rbac-check')
  const server = http.createServer(async (req, res) => {
    if (req.url.startsWith('/api/')) {
      let text = ''; for await (const chunk of req) text += chunk
      const body = text ? JSON.parse(text) : {}
      let data = null
      if (req.url.endsWith('/getLoginUser')) data = { id: '42', nickname: '测试管理员', permissionKeys: keys, roleKeys: profileRoles }
      else if (req.url.endsWith('/permission/get')) data = permissions
      else if (req.url.endsWith('/permission/create')) { createdPermission = body; permissions.push({ ...body, id: '100' }); mutations.push('create') }
      else if (req.url.endsWith('/permission/update')) { permissions = permissions.map(p => p.id === String(body.id) ? { ...body, id: String(body.id) } : p); mutations.push('update') }
      else if (req.url.endsWith('/permission/delete')) { permissions = permissions.filter(p => p.id !== String(body.id)); mutations.push('delete') }
      else if (req.url.endsWith('/role/admin/list')) data = [{ id: '2', roleName: '管理员', roleKey: 'admin', status: 0 }, { id: '3', roleName: '普通用户', roleKey: 'common_user', status: 0 }, { id: '4', roleName: '运营', roleKey: 'operator', status: 0 }]
      else if (req.url.endsWith('/permissions/get')) data = assigned
      else if (req.url.endsWith('/permissions/assign')) { assigned = body.permissionIds; mutations.push('permissions') }
      else if (req.url.endsWith('/user-role/admin/get')) data = userRoles
      else if (req.url.endsWith('/user-role/admin/assign')) { userRoles = body.roleIds; mutations.push('roles') }
      else if (req.url.endsWith('/getUserList')) data = { records: [{ id: '42', nickname: '测试管理员', ymmId: 'test', status: 0 }, { id: '43', nickname: '其他用户', ymmId: 'other', status: 0 }], total: 2 }
      else if (req.url.endsWith('/note/admin/list')) data = articles
      else if (req.url.startsWith('/api/note/note/admin/detail')) data = articles[0]
      res.writeHead(200, { 'Content-Type': 'application/json' }); return res.end(JSON.stringify({ success: true, data }))
    }
    const route = decodeURIComponent(req.url.split('?')[0]).replace(/^\/admin\/?/, '')
    let file = path.resolve(root, route)
    if (!file.startsWith(root + path.sep) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) file = path.join(root, 'index.html')
    const mime = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.png': 'image/png' }[path.extname(file)] || 'application/octet-stream'
    res.writeHead(200, { 'Content-Type': mime }); fs.createReadStream(file).pipe(res)
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  const origin = `http://127.0.0.1:${server.address().port}`
  const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'ymm-rbac-browser-'))
  const browser = spawn(process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', ['--headless=new', '--no-first-run', '--no-default-browser-check', '--remote-debugging-port=0', `--user-data-dir=${temp}`, 'about:blank'], { windowsHide: true, stdio: ['ignore', 'ignore', 'pipe'] })
  let ws
  try {
    const endpoint = await new Promise((resolve, reject) => {
      let log = ''; const timer = setTimeout(() => reject(new Error('Browser startup timed out')), 15000)
      browser.stderr.on('data', chunk => { log += chunk; const match = log.match(/DevTools listening on (ws:\/\/[^\s]+)/); if (match) { clearTimeout(timer); resolve(match[1]) } })
      browser.on('error', reject)
    })
    ws = new WebSocket(endpoint); await new Promise(resolve => ws.addEventListener('open', resolve, { once: true }))
    let id = 0; const pending = new Map(); const exceptions = []
    ws.addEventListener('message', event => { const m = JSON.parse(event.data); if (m.id) { const p = pending.get(m.id); pending.delete(m.id); m.error ? p.reject(new Error(JSON.stringify(m.error))) : p.resolve(m.result) } else if (m.method === 'Runtime.exceptionThrown') exceptions.push(m.params.exceptionDetails.text) })
    const send = (method, params = {}, sessionId) => new Promise((resolve, reject) => { const next = ++id; pending.set(next, { resolve, reject }); ws.send(JSON.stringify({ id: next, method, params, ...(sessionId ? { sessionId } : {}) })) })
    const { targetId } = await send('Target.createTarget', { url: 'about:blank' })
    const { sessionId } = await send('Target.attachToTarget', { targetId, flatten: true })
    const cdp = (method, params) => send(method, params, sessionId)
    await cdp('Runtime.enable'); await cdp('Page.enable'); await cdp('Network.enable')
    await cdp('Network.setCookie', { name: 'Ymm-Admin-Token', value: 'isolated-test-session', url: origin, path: '/' })
    const evaluate = async expression => { const r = await cdp('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (r.exceptionDetails) throw new Error(JSON.stringify(r.exceptionDetails)); return r.result.value }
    const wait = async expression => { for (let n = 0; n < 100; n++) { if (await evaluate(expression)) return; await new Promise(r => setTimeout(r, 100)) } throw new Error('Timeout: ' + expression + '\n' + await evaluate('document.body.innerText')) }
    const go = async route => { await cdp('Page.navigate', { url: origin + route }); await wait('document.readyState === "complete" && document.querySelector("#app")?.children.length > 0') }
    const click = label => evaluate(`(() => {const b=[...document.querySelectorAll('button')].find(b=>b.offsetParent && b.textContent.trim()===${JSON.stringify(label)}); if(!b)throw Error('missing button'); b.click()})()`)
    const input = (label, value) => evaluate(`(() => {const item=[...document.querySelectorAll('.el-dialog .el-form-item')].find(e=>e.innerText.includes(${JSON.stringify(label)})); const el=item.querySelector('input'); Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(el,${JSON.stringify(value)}); el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}))})()`)
    await go('/admin/user/permission'); await wait('document.body.innerText.includes("新增权限")')
    await click('新增权限'); await input('权限名称', '测试权限'); await input('权限标识', 'test:demo:manage')
    const radio = (label, value) => evaluate(`(() => {const item=[...document.querySelectorAll('.el-dialog .el-form-item')].find(e=>e.innerText.includes(${JSON.stringify(label)})); const el=[...item.querySelectorAll('.el-radio')].find(e=>e.textContent.trim()===${JSON.stringify(value)}); el.click()})()`)
    await radio('类型', '按钮'); await radio('状态', '禁用'); await click('保存')
    await wait('document.querySelector(".el-table")?.innerText.includes("测试权限")'); assert(mutations.includes('create')); assert.equal(createdPermission.type, 3); assert.equal(createdPermission.status, 1)
    await evaluate(`(()=>{const row=[...document.querySelectorAll('.el-table__row')].find(r=>r.innerText.includes('测试权限')); [...row.querySelectorAll('button')].find(b=>b.textContent.trim()==='编辑').click()})()`)
    await input('权限名称', '修改权限'); await click('保存'); await wait('document.querySelector(".el-table")?.innerText.includes("修改权限")')
    await evaluate(`(()=>{const row=[...document.querySelectorAll('.el-table__row')].find(r=>r.innerText.includes('修改权限')); [...row.querySelectorAll('button')].find(b=>b.textContent.trim()==='删除').click()})()`)
    await wait('Boolean(document.querySelector(".el-message-box"))'); await click('确定'); await wait('!document.querySelector(".el-table")?.innerText.includes("修改权限")'); assert(mutations.includes('delete'))
    console.log('PASS permission create/edit/delete dialogs')
    await go('/admin/user/role'); await wait('document.body.innerText.includes("分配权限")'); await click('分配权限'); await wait('document.querySelectorAll(".el-tree-node").length > 0')
    const treeCheck = name => evaluate(`(() => {const content=[...document.querySelectorAll('.el-dialog .el-tree-node__content')].find(e=>e.textContent.trim()===${JSON.stringify(name)}); content.querySelector('.el-checkbox').click()})()`)
    await treeCheck('仪表盘'); await treeCheck('管理用户'); await treeCheck('管理笔记'); await click('保存授权'); await wait('!document.querySelector(".el-overlay-dialog")?.offsetParent'); assert(mutations.includes('permissions')); assert.deepEqual([...assigned].sort(), ['1', '2'])
    await click('分配权限'); await wait('document.querySelectorAll(".el-dialog .el-checkbox.is-checked").length === 2'); await treeCheck('仪表盘'); await click('保存授权'); await wait('!document.querySelector(".el-overlay-dialog")?.offsetParent'); assert.deepEqual(assigned, ['2'])
    console.log('PASS role permission assignment and checked-state roundtrip')
    await go('/admin/user/list'); await wait('document.body.innerText.includes("分配角色")'); await click('分配角色'); await wait('Boolean(document.querySelector(".role-choice"))')
    const roleCheck = name => evaluate(`(() => {const choice=[...document.querySelectorAll('.role-choice')].find(e=>e.textContent.includes(${JSON.stringify(name)})); choice.querySelector('.el-checkbox').click()})()`)
    await roleCheck('普通用户'); await roleCheck('运营'); await roleCheck('管理员'); await click('保存角色'); await wait('!document.querySelector(".el-overlay-dialog")?.offsetParent'); assert(mutations.includes('roles')); assert.deepEqual([...userRoles].sort(), ['3', '4'])
    const ownButtons = await evaluate(`[...document.querySelectorAll('.el-table__row')].filter(r=>r.innerText.includes('测试管理员')).flatMap(r=>[...r.querySelectorAll('button')]).length`); assert.equal(ownButtons, 0)
    await click('分配角色'); await wait('document.querySelectorAll(".role-choice .el-checkbox.is-checked").length === 2'); await roleCheck('运营'); await roleCheck('普通用户'); await click('保存角色'); await wait('!document.querySelector(".el-overlay-dialog")?.offsetParent'); assert.deepEqual(userRoles, [])
    profileRoles = ['common_user']; await evaluate("window.dispatchEvent(new Event('admin-authorization-changed'))"); await wait('!document.body.innerText.includes("分配角色")'); assert.equal(await evaluate('document.querySelector(".custom-menu").innerText.includes("角色管理")'), false)
    await go('/admin/user/permission'); await wait('location.pathname === "/admin/forbidden"')
    console.log('PASS user role assignment')
    keys = ['admin:note:manage']; await go('/admin/login'); await wait('location.pathname === "/admin/content/article"'); await wait('Boolean(document.querySelector(".custom-menu"))')
    const menu = await evaluate('document.querySelector(".custom-menu").innerText'); assert(!menu.includes('角色管理')); assert(!menu.includes('权限管理')); assert(!menu.includes('仪表盘')); assert(menu.includes('文章管理'))
    await go('/admin/user/role'); await wait('location.pathname === "/admin/forbidden"')
    console.log('PASS operator landing, filtered menu and direct URL denial')
    await go('/admin/content/article'); await wait('document.querySelectorAll(".article-table .el-table__body-wrapper .el-table__row").length === 8')
    const reviews = await evaluate(`(() => {const column=[...document.querySelectorAll('.article-table .el-table__header-wrapper th')].findIndex(th=>th.innerText.trim()==='审核'); return [...document.querySelectorAll('.article-table .el-table__body-wrapper .el-table__row')].map(row=>row.querySelectorAll('td')[column].innerText.trim())})()`)
    assert.deepEqual(reviews, ['已通过（审核来源未记录）', '人工审核通过', 'AI审核通过', '待人工复核', '待人工审核', 'AI审核中', '审核来源未记录', '审核来源未记录'])
    await evaluate("[...document.querySelectorAll('.article-title-link')].find(b=>b.textContent==='历史已通过图文').click()")
    await wait('document.querySelector(".detail-body")?.innerText.includes("审核：已通过（审核来源未记录）")')
    assert.equal(await evaluate('document.querySelector(".detail-body").innerText.includes("待审核")'), false)
    console.log('PASS completed and pending article review labels in list and detail')
    keys = []; await go('/admin'); await wait('location.pathname === "/admin/forbidden"'); await click('退出登录'); await wait('location.pathname === "/admin/login"')
    assert.deepEqual(exceptions, []); console.log('PASS ordinary user denial and logout; no browser exceptions')
  } finally { if (ws) ws.close(); browser.kill(); await new Promise(resolve => server.close(resolve)) }
}
main().catch(error => { console.error(error); process.exitCode = 1 })
