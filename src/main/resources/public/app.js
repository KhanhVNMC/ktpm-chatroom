var PERMS = [
  ['SEND_MESSAGES', 1], ['EDIT_MESSAGES', 2], ['DELETE_MESSAGES', 4], ['KICK_MEMBERS', 8],
  ['BAN_MEMBERS', 16], ['MANAGE_ROOM', 32], ['MUTE_MEMBERS', 64], ['GRANT_PERMISSIONS', 128]
];
var P = { DELETE: 4, KICK: 8, BAN: 16, MANAGE: 32, MUTE: 64, GRANT: 128 };
var MUTED = 1, BANNED = 2;

var S = {
  session: localStorage.getItem('session'),
  me: null,
  room: null,        // current room uuid
  overview: null,
  members: [],
  myBits: 0,
  inRoom: false,
  oldest: null,      // uuid of the oldest loaded message, for "load older"
  searchQ: null,
  unread: {},
  mineIds: {},
  es: null,
  permTarget: null,
  recorder: null
};

function $(id) { return document.getElementById(id); }

/** tiny element builder, text goes in as text nodes so nothing is ever parsed as html */
function h(tag, attrs) {
  var el = document.createElement(tag);
  for (var k in (attrs || {})) {
    if (k === 'onclick') el.onclick = attrs[k];
    else if (k === 'class') el.className = attrs[k];
    else el.setAttribute(k, attrs[k]);
  }
  for (var i = 2; i < arguments.length; i++) {
    var c = arguments[i];
    if (c === null || c === undefined || c === false) continue;
    el.appendChild(typeof c === 'object' ? c : document.createTextNode(String(c)));
  }
  return el;
}
function btn(label, fn) { return h('button', { onclick: fn }, label); }
function clear(el) { while (el.firstChild) el.removeChild(el.firstChild); }
function show(id, on) { $(id).classList.toggle('hidden', !on); }

function log(s) {
  var d = $('log');
  d.textContent += s + '\n';
  d.scrollTop = d.scrollHeight;
}

async function api(method, path, body, extra) {
  var headers = {};
  var payload;
  if (extra && extra.raw !== undefined) {
    payload = extra.raw;
    if (extra.name) headers['X-File-Name'] = extra.name;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }
  if (S.session) headers['Authorization'] = 'Bearer ' + S.session;
  log('> ' + method + ' ' + path + (body !== undefined ? ' ' + JSON.stringify(body).slice(0, 200) : ''));
  var res, text;
  try {
    res = await fetch(path, { method: method, headers: headers, body: payload });
    text = await res.text();
  } catch (e) {
    log('< NETWORK ERROR ' + e);
    return { ok: false, status: 0, data: null };
  }
  var data = null;
  try { data = text ? JSON.parse(text) : null; } catch (e) { data = text; }
  log('< ' + res.status + ' ' + (typeof data === 'string' ? data : JSON.stringify(data)).slice(0, 300));
  if (!res.ok) alertErr(data, res.status);
  return { ok: res.ok, status: res.status, data: data };
}

function alertErr(data, status) {
  var msg = data && data.error_message ? data.error_message : ('HTTP ' + status);
  $('banner').style.display = 'block';
  $('banner').className = 'err';
  $('banner').textContent = msg;
  clearTimeout(alertErr.t);
  alertErr.t = setTimeout(function () { renderBanner(); }, 4000);
}

function renderBanner() {
  var b = $('banner');
  b.className = '';
  clear(b);
  if (S.searchQ) {
    b.style.display = 'block';
    b.appendChild(document.createTextNode('search results for "' + S.searchQ + '" '));
    b.appendChild(btn('exit search', function () { S.searchQ = null; $('searchQ').value = ''; renderBanner(); loadPage(true); }));
  } else {
    b.style.display = 'none';
  }
}

function avatarEl(url, name, cls) {
  var d = h('span', { class: 'av ' + (cls || '') });
  if (url) d.appendChild(h('img', { src: url + (url.indexOf('?') < 0 ? '?t=' + Date.now() : '') }));
  else d.appendChild(document.createTextNode((name || '?').charAt(0).toUpperCase()));
  return d;
}

function fileToB64(file) {
  return new Promise(function (resolve, reject) {
    var r = new FileReader();
    r.onload = function () { resolve(String(r.result).split(',')[1] || ''); };
    r.onerror = reject;
    r.readAsDataURL(file);
  });
}
function fileBytes(file) { return file.arrayBuffer(); }

/* ---------------- auth / profile ---------------- */

async function register() {
  var r = await api('POST', '/api/auth/register', {
    username: $('username').value, password: $('password').value,
    display_name: $('displayName').value || undefined
  });
  if (r.ok) login();
}

async function login() {
  var r = await api('POST', '/api/auth/login', { username: $('username').value, password: $('password').value });
  if (!r.ok) return;
  S.session = r.data.session_id;
  localStorage.setItem('session', S.session);
  await boot();
}

async function logout() {
  await api('POST', '/api/auth/logout');
  dropSession();
}

function dropSession() {
  S.session = null; S.me = null; S.room = null;
  localStorage.removeItem('session');
  if (S.es) { S.es.close(); S.es = null; }
  renderMe();
  clear($('mine'));
  S.mineIds = {};
  closeRoom();
  loadPublic();
}

async function boot() {
  if (!S.session) { renderMe(); loadPublic(); return; }
  var r = await api('GET', '/api/auth/me');
  if (!r.ok) { dropSession(); return; }
  S.me = r.data;
  renderMe();
  connectSSE();
  refreshLists();
  if (S.room) openRoom(S.room);
}

function renderMe() {
  show('guest', !S.me);
  show('user', !!S.me);
  show('roomtools', !!S.me);
  show('moretools', !!S.me);
  if (!S.me) return;
  var a = $('meAvatar');
  clear(a);
  if (S.me.avatar) a.appendChild(h('img', { src: S.me.avatar + '?t=' + Date.now() }));
  else a.appendChild(document.createTextNode(S.me.display_name.charAt(0).toUpperCase()));
  $('meName').textContent = S.me.display_name;
  $('meSub').textContent = '@' + S.me.username + ' - ' + S.me.presence + (S.me.status_text ? ' - ' + S.me.status_text : '');
  $('mePresence').value = S.me.presence;
  $('meDisplay').value = S.me.display_name;
  $('meStatus').value = S.me.status_text || '';
}

async function saveProfile() {
  var r = await api('PATCH', '/api/user/me', {
    display_name: $('meDisplay').value, presence: $('mePresence').value, status_text: $('meStatus').value
  });
  if (r.ok) { S.me = r.data; renderMe(); }
}

async function uploadAvatar() {
  var f = $('meFile').files[0];
  if (!f) return;
  var up = await api('POST', '/api/assets', undefined, { raw: await fileBytes(f), name: f.name });
  if (!up.ok) return;
  var r = await api('PATCH', '/api/user/me', { avatar: up.data.asset_uuid });
  if (r.ok) { S.me = r.data; renderMe(); }
}

async function removeAvatar() {
  var r = await api('PATCH', '/api/user/me', { avatar: null });
  if (r.ok) { S.me = r.data; renderMe(); }
}

/* ---------------- room lists ---------------- */

function roomRow(room, withJoin) {
  var id = room.roomUUID;
  var joinable = withJoin && S.me && !S.mineIds[id];
  var row = h('div', { class: 'row click' + (id === S.room ? ' active' : ''), onclick: function () { openRoom(id); } },
    avatarEl(room.avatar, room.name),
    h('span', { class: 'name' }, room.name || id),
    S.unread[id] ? h('span', { class: 'badge' }, S.unread[id]) : null,
    h('span', { class: 'small' }, room.members),
    (joinable ? h('button', { onclick: function (e) { e.stopPropagation(); quickJoin(id); } }, 'join') : null));
  row.title = (room.motd || '') + '\n' + id;
  return row;
}

async function loadMine() {
  if (!S.me) return;
  var r = await api('GET', '/api/chatrooms/mine?limit=100');
  if (!r.ok) return;
  var box = $('mine');
  clear(box);
  S.mineIds = {};
  r.data.rooms.forEach(function (room) { S.mineIds[room.roomUUID] = true; box.appendChild(roomRow(room)); });
}

async function loadPublic() {
  var r = await api('GET', '/api/chatrooms/listing?limit=100');
  if (!r.ok) return;
  var box = $('pub');
  clear(box);
  r.data.public_rooms.forEach(function (room) { box.appendChild(roomRow(room, true)); });
}

async function refreshLists() {
  await loadMine(); // public list needs to know which rooms are already mine
  await loadPublic();
}

async function quickJoin(id) {
  var r = await api('POST', '/api/chatrooms/' + id + '/join', {});
  if (!r.ok) return;
  await refreshLists();
  openRoom(id);
}

async function createRoom() {
  var r = await api('POST', '/api/chatrooms', {
    name: $('newName').value, motd: $('newMotd').value,
    is_public: $('newPublic').checked, invite_code: $('newCode').value || undefined
  });
  if (!r.ok) return;
  refreshLists();
  openRoom(r.data.room_uuid);
}

async function previewInvite() {
  var code = $('joinCode').value.trim();
  if (!code) return;
  var r = await api('GET', '/api/chatrooms/invite/' + encodeURIComponent(code));
  if (r.ok) alert(r.data.name + '\n' + r.data.motd + '\nmembers: ' + r.data.member_count + '\nowner: ' + r.data.owner);
}

async function joinInvite() {
  var code = $('joinCode').value.trim();
  if (!code) return;
  var r = await api('POST', '/api/chatrooms/invite/' + encodeURIComponent(code) + '/join');
  if (!r.ok) return;
  $('joinCode').value = '';
  refreshLists();
  openRoom(r.data.room_uuid);
}

/* ---------------- current room ---------------- */

function closeRoom() {
  S.room = null; S.overview = null; S.members = []; S.myBits = 0; S.inRoom = false; S.searchQ = null;
  $('roomTitle').textContent = 'no room selected';
  $('roomMotd').textContent = '';
  clear($('messages'));
  clear($('members'));
  $('roomInfo').textContent = 'open a room';
  show('composer', false); show('joinbar', false); show('manage', false); show('leaveBox', false); show('permBox', false);
  renderBanner();
}

async function openRoom(id) {
  S.room = id; S.searchQ = null; S.oldest = null; S.permTarget = null;
  delete S.unread[id];
  clear($('messages'));
  show('permBox', false);
  renderBanner();
  refreshLists();
  await loadOverview();
  if (S.room !== id) return;
  if (!S.overview) { $('roomTitle').textContent = 'cannot open this room'; return; }
  await loadMembers();
  await loadPage(true);
}

async function loadOverview() {
  var r = await api('GET', '/api/chatrooms/' + S.room + '/overview');
  S.overview = r.ok ? r.data : null;
  renderRoomInfo();
}

async function loadMembers() {
  var m = null;
  if (S.me) {
    var r = await api('GET', '/api/chatrooms/' + S.room + '/members');
    if (r.ok) m = r.data.members;
  }
  S.myBits = 0; S.inRoom = false;
  if (m) {
    S.members = m;
    var mine = m.filter(function (x) { return S.me && x.uuid === S.me.uuid; })[0];
    S.inRoom = !!mine && mine.is_in_room;
    S.myBits = mine ? mine.permission_bits : 0;
  } else {
    S.members = S.overview ? S.overview.members : []; // not a member: only the public summary
  }
  renderMembers();
  renderRoomInfo();
}

function isOwner() { return !!(S.me && S.overview && S.overview.owner_uuid === S.me.uuid); }
function can(bit) { return isOwner() || (S.myBits & bit) !== 0; }

function renderRoomInfo() {
  var o = S.overview;
  var box = $('roomInfo');
  clear(box);
  if (!o) { $('roomTitle').textContent = 'cannot open this room'; show('manage', false); show('leaveBox', false); return; }
  $('roomTitle').textContent = o.name;
  $('roomMotd').textContent = o.motd;
  box.appendChild(h('div', { class: 'row' }, avatarEl(o.avatar, o.name, 'big'),
    h('div', {}, h('b', {}, o.name), h('br'), o.is_public ? 'public' : 'private', ' - ', o.member_count, ' members', h('br'),
      'owner: ', o.owner, h('br'), 'created: ', new Date(o.createdAt).toLocaleString())));
  if (o.invite_code) {
    box.appendChild(h('div', {}, 'invite code: ', h('b', {}, o.invite_code), h('br'),
      'link: ', h('code', {}, location.origin + '/api/chatrooms/invite/' + o.invite_code)));
  }
  $('editName').value = o.name;
  $('editMotd').value = o.motd;
  show('manage', S.inRoom && can(P.MANAGE));
  $('inviteBox').style.display = o.is_public ? 'none' : '';
  show('leaveBox', S.inRoom);
  show('delBtn', isOwner());
  show('leaveBtn', !isOwner());
  show('composer', S.inRoom);
  show('joinbar', !S.inRoom);
  $('joinMsg').textContent = !S.me ? 'log in to join this room.' : (o.is_public ? 'you are viewing this room, not a member yet.' : 'private room, enter the invite code to join.');
  $('roomCode').style.display = (S.me && !o.is_public) ? '' : 'none';
  $('joinBtn').style.display = S.me ? '' : 'none';
}

function renderMembers() {
  var box = $('members');
  clear(box);
  var banned = [];
  S.members.forEach(function (m) {
    if (m.is_in_room === false) { banned.push(m); return; }
    box.appendChild(memberRow(m));
  });
  if (banned.length) {
    box.appendChild(h('h4', {}, 'banned'));
    banned.forEach(function (m) { box.appendChild(memberRow(m)); });
  }
}

function memberRow(m) {
  var ownerRow = S.overview && m.uuid === S.overview.owner_uuid;
  var self = S.me && m.uuid === S.me.uuid;
  var inRoom = m.is_in_room !== false;
  var row = h('div', { class: 'member' },
    h('div', { class: 'row' }, avatarEl(m.avatar, m.display_name),
      h('span', { class: 'name' }, m.display_name),
      ownerRow ? h('span', { class: 'badge' }, 'owner') : null,
      (m.status_bits & MUTED) ? h('span', { class: 'badge' }, 'muted') : null,
      !inRoom ? h('span', { class: 'badge' }, 'banned') : null,
      inRoom ? h('span', { class: 'small' }, m.presence) : null),
    m.status_text ? h('div', { class: 'small' }, m.status_text) : null);
  // moderation buttons, the server enforces the real rules (cant touch owner/admins etc)
  if (S.inRoom && m.permission_bits !== undefined && !ownerRow && !self) {
    var b = h('div', { class: 'btns' });
    var uid = m.uuid;
    if (inRoom && can(P.KICK)) b.appendChild(btn('kick', function () { moderate(uid, 'kick'); }));
    if (inRoom && can(P.BAN)) b.appendChild(btn('ban', function () { moderate(uid, 'ban'); }));
    if (!inRoom && can(P.BAN)) b.appendChild(btn('unban', function () { moderate(uid, 'unban'); }));
    if (inRoom && can(P.MUTE)) {
      b.appendChild((m.status_bits & MUTED)
        ? btn('unmute', function () { moderate(uid, 'unmute'); })
        : btn('mute', function () { moderate(uid, 'mute'); }));
    }
    if (inRoom && can(P.GRANT)) b.appendChild(btn('perms', function () { openPerms(m); }));
    if (inRoom && isOwner()) b.appendChild(btn('make owner', function () { transfer(uid, m.display_name); }));
    row.appendChild(b);
  }
  return row;
}

async function moderate(uid, action) {
  var reason = (action === 'kick' || action === 'ban') ? prompt('reason (optional)') : null;
  var r = await api('POST', '/api/chatrooms/' + S.room + '/members/' + uid + '/' + action, reason ? { reason: reason } : {});
  if (r.ok) { loadMembers(); loadOverview(); }
}

async function transfer(uid, name) {
  if (!confirm('transfer ownership to ' + name + '?')) return;
  var r = await api('POST', '/api/chatrooms/' + S.room + '/transfer-ownership', { new_owner: uid });
  if (r.ok) { loadOverview().then(loadMembers); }
}

function openPerms(m) {
  S.permTarget = m.uuid;
  $('permWho').textContent = m.display_name;
  var box = $('permChecks');
  clear(box);
  PERMS.forEach(function (p) {
    var cb = h('input', { type: 'checkbox', 'data-bit': p[1] });
    cb.checked = (m.permission_bits & p[1]) !== 0;
    box.appendChild(h('div', {}, h('label', {}, cb, ' ' + p[0])));
  });
  show('permBox', true);
}

function closePerms() { S.permTarget = null; show('permBox', false); }

async function applyPerms() {
  var bits = 0;
  Array.prototype.forEach.call($('permChecks').querySelectorAll('input'), function (cb) {
    if (cb.checked) bits |= parseInt(cb.getAttribute('data-bit'), 10);
  });
  var r = await api('PUT', '/api/chatrooms/' + S.room + '/members/' + S.permTarget + '/permissions', { permission_bits: bits });
  if (r.ok) { closePerms(); loadMembers(); }
}

async function joinCurrent() {
  var code = $('roomCode').value.trim();
  var r = await api('POST', '/api/chatrooms/' + S.room + '/join', code ? { room_invite_code: code } : {});
  if (!r.ok) return;
  $('roomCode').value = '';
  refreshLists();
  openRoom(S.room);
}

async function leaveRoom() {
  var r = await api('POST', '/api/chatrooms/' + S.room + '/leave', {
    silent_leave: $('silent').checked, reason: $('reason').value || undefined
  });
  if (!r.ok) return;
  refreshLists();
  openRoom(S.room); // public rooms stay viewable, private ones show "cannot open"
}

async function deleteRoom() {
  if (!confirm('delete this room for everyone?')) return;
  var r = await api('DELETE', '/api/chatrooms/' + S.room);
  if (r.ok) { closeRoom(); refreshLists(); }
}

async function saveRoom() {
  var r = await api('PATCH', '/api/chatrooms/' + S.room, { name: $('editName').value, motd: $('editMotd').value });
  if (r.ok) { loadOverview(); refreshLists(); }
}

async function setRoomAvatar() {
  var f = $('roomFile').files[0];
  if (!f) return;
  var r = await api('PUT', '/api/chatrooms/' + S.room + '/avatar', undefined, { raw: await fileBytes(f), name: f.name });
  if (r.ok) { loadOverview(); refreshLists(); }
}

async function delRoomAvatar() {
  var r = await api('DELETE', '/api/chatrooms/' + S.room + '/avatar');
  if (r.ok) { loadOverview(); refreshLists(); }
}

async function resetCode() {
  var code = $('customCode').value.trim();
  var r = await api('POST', '/api/chatrooms/' + S.room + '/invite-code', code ? { invite_code: code } : {});
  if (r.ok) { $('customCode').value = ''; loadOverview(); }
}

/* ---------------- messages ---------------- */

function attachmentEl(url) {
  // the url has no extension so just try img -> video (also plays audio) -> plain link
  var box = h('div', { class: 'att' });
  var img = h('img', { src: url });
  img.onerror = function () {
    var vid = h('video', { src: url, controls: 'controls' });
    vid.onerror = function () { box.replaceChild(h('a', { href: url, target: '_blank' }, 'attachment ' + url.split('/').pop().slice(0, 8)), vid); };
    box.replaceChild(vid, img);
  };
  box.appendChild(img);
  return box;
}

function msgEl(m) {
  var mine = S.me && m.senderUUID === S.me.uuid;
  var acts = h('span', { class: 'acts' });
  if (mine) acts.appendChild(btn('edit', function () { editMessage(m); }));
  if (mine || can(P.DELETE)) acts.appendChild(btn('delete', function () { deleteMessage(m); }));
  var el = h('div', { class: 'msg', 'data-id': m.chatMessageUUID },
    h('div', { class: 'meta' }, h('b', {}, m.sender), ' ', new Date(m.unixTimestamp).toLocaleString(), S.me ? acts : null),
    h('div', { class: 'content' }, m.content));
  (m.attachments || []).forEach(function (u) { el.appendChild(attachmentEl(u)); });
  return el;
}

function findMsg(id) { return $('messages').querySelector('[data-id="' + id + '"]'); }

function addMessage(m) {
  if (findMsg(m.chatMessageUUID)) return;
  var box = $('messages');
  var atBottom = box.scrollHeight - box.scrollTop - box.clientHeight < 40;
  box.appendChild(msgEl(m));
  if (atBottom) box.scrollTop = box.scrollHeight;
}

function updateMessage(m) {
  var old = findMsg(m.chatMessageUUID);
  if (old) old.parentNode.replaceChild(msgEl(m), old);
}

function removeMessage(id) {
  var old = findMsg(id);
  if (old) old.parentNode.removeChild(old);
}

// reset = (re)load newest page, otherwise prepend the page before S.oldest
async function loadPage(reset) {
  if (!S.room) return;
  var box = $('messages');
  var q = S.searchQ ? 'search?q=' + encodeURIComponent(S.searchQ) + '&' : 'history?';
  var path = '/api/chatrooms/' + S.room + '/' + q + 'limit=30';
  if (!reset && S.oldest) path += '&before_message=' + S.oldest;
  var r = await api('GET', path);
  if (!r.ok) return;
  var list = r.data.messages; // old -> new
  if (reset) { clear(box); S.oldest = null; }
  if (list.length) S.oldest = list[0].chatMessageUUID;
  if (reset) {
    list.forEach(function (m) { box.appendChild(msgEl(m)); });
    box.insertBefore(olderBtn(), box.firstChild);
    box.scrollTop = box.scrollHeight;
  } else {
    var anchor = box.querySelector('.msg');
    var before = box.scrollHeight;
    list.forEach(function (m) { box.insertBefore(msgEl(m), anchor); });
    box.scrollTop = box.scrollHeight - before;
    if (!list.length) log('no older messages');
  }
}

function olderBtn() {
  var b = h('div', { class: 'small', id: 'olderRow' }, btn('load older', function () { loadPage(false); }));
  return b;
}

function search() {
  var q = $('searchQ').value.trim();
  if (!q || !S.room) return;
  S.searchQ = q;
  renderBanner();
  loadPage(true);
}

async function sendMessage() {
  var input = $('content');
  var files = $('files').files;
  var uploaded = [];
  for (var i = 0; i < files.length; i++) {
    uploaded.push({ attachment: await fileToB64(files[i]), file_name: files[i].name });
  }
  if (!input.value && !uploaded.length) return;
  var r = await api('POST', '/api/chatrooms/' + S.room + '/messages', {
    message_content: input.value || undefined, uploaded_files: uploaded.length ? uploaded : undefined
  });
  if (!r.ok) return;
  input.value = '';
  $('files').value = '';
  if (!S.searchQ) addMessage(r.data);
}

async function editMessage(m) {
  var text = prompt('new content', m.content);
  if (text === null) return;
  var keep = (m.attachments || []).length ? confirm('keep the attachments? (cancel = drop them)') : false;
  var r = await api('PATCH', '/api/chatrooms/' + S.room + '/messages/' + m.chatMessageUUID, { new_content: text, keep_attachments: keep });
  if (r.ok) updateMessage(r.data);
}

async function deleteMessage(m) {
  var r = await api('DELETE', '/api/chatrooms/' + S.room + '/messages/' + m.chatMessageUUID);
  if (r.ok) removeMessage(m.chatMessageUUID);
}

/* voice message: record with the mic and attach it as a file */
async function toggleRecord() {
  if (S.recorder) { S.recorder.stop(); return; }
  var stream;
  try { stream = await navigator.mediaDevices.getUserMedia({ audio: true }); }
  catch (e) { alert('no mic: ' + e); return; }
  var chunks = [];
  S.recorder = new MediaRecorder(stream);
  S.recorder.ondataavailable = function (e) { chunks.push(e.data); };
  S.recorder.onstop = function () {
    stream.getTracks().forEach(function (t) { t.stop(); });
    var file = new File(chunks, 'voice.webm', { type: 'audio/webm' });
    var dt = new DataTransfer();
    dt.items.add(file);
    $('files').files = dt.files;
    S.recorder = null;
    $('recBtn').textContent = 'rec voice';
  };
  S.recorder.start();
  $('recBtn').textContent = 'stop rec';
}

/* ---------------- realtime (SSE) ---------------- */

function connectSSE() {
  if (S.es) S.es.close();
  // EventSource cant set headers, hence ?session_id=
  S.es = new EventSource('/api/events?session_id=' + encodeURIComponent(S.session));
  S.es.onmessage = function (e) {
    var ev = JSON.parse(e.data);
    var payload = JSON.parse(ev.payload); // payload is a json string inside the json
    var d = $('events');
    d.textContent += ev.type + ' ' + ev.payload.slice(0, 160) + '\n';
    d.scrollTop = d.scrollHeight;
    onEvent(ev.type, payload);
  };
  S.es.onerror = function () { log('SSE error / reconnecting'); };
}

function onEvent(type, p) {
  switch (type) {
    case 'CONNECTED': return;
    case 'MESSAGE_CREATED':
      if (p.roomUUID === S.room) { if (!S.searchQ) addMessage(p); }
      else { S.unread[p.roomUUID] = (S.unread[p.roomUUID] || 0) + 1; loadMine(); }
      return;
    case 'MESSAGE_EDITED': if (p.roomUUID === S.room) updateMessage(p); return;
    case 'MESSAGE_DELETED': if (p.room_uuid === S.room) removeMessage(p.message_uuid); return;
    case 'USER_UPDATED':
      if (S.me && p.uuid === S.me.uuid) { S.me = p; renderMe(); }
      if (S.room) loadMembers();
      return;
    case 'ROOM_DELETED':
      refreshLists();
      if (p.room_uuid === S.room) { closeRoom(); alert('this room was deleted'); }
      return;
    case 'ROOM_UPDATED':
      refreshLists();
      if (p.roomUUID === S.room) loadOverview();
      return;
  }
  // MEMBER_* and OWNER_TRANSFERRED, all carry room_uuid
  var self = S.me && (p.user_uuid === S.me.uuid || (p.user && p.user.uuid === S.me.uuid));
  if (p.room_uuid !== S.room) { if (self) refreshLists(); return; }
  if (self && (type === 'MEMBER_KICKED' || type === 'MEMBER_BANNED')) {
    alert(type === 'MEMBER_BANNED' ? 'you got banned from this room' : 'you got kicked from this room');
    refreshLists();
    openRoom(S.room);
    return;
  }
  loadOverview().then(loadMembers);
  loadMine();
}

boot();
