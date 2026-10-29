package com.vizja.lab3.controller;

import com.vizja.lab3.lib.BaseController;
import com.vizja.lab3.lib.http.Cookie;
import com.vizja.lab3.lib.http.HttpRequest;
import com.vizja.lab3.lib.http.HttpResponse;

/**
 * Serves the single-page HTML frontend for the task management application and
 * issues the visit-tracking cookies that the page's JavaScript reads.
 *
 * <p>Cookies set by the server on every {@code GET /}:
 * <ul>
 *   <li>{@code visits}    – number of times this browser has loaded the page (including this load)</li>
 *   <li>{@code lastVisit} – epoch-millis of this load; becomes "previous visit" on the next load</li>
 *   <li>{@code prevVisit} – epoch-millis of the previous load (omitted/expired on the first visit)</li>
 * </ul>
 * They are intentionally NOT HttpOnly so the frontend script can read them via
 * {@code document.cookie}. Values from incoming cookies are never trusted: they are
 * parsed defensively and clamped before use.
 */
public class HomeController extends BaseController {

    static final String COOKIE_VISITS = "visits";
    static final String COOKIE_LAST_VISIT = "lastVisit";
    static final String COOKIE_PREV_VISIT = "prevVisit";

    private static final int ONE_YEAR_SECONDS = 365 * 24 * 60 * 60;
    private static final long MAX_VISITS = 1_000_000L;

    @Override
    public void doGet(HttpRequest request, HttpResponse response) {
        long visits = Math.min(readLong(request, COOKIE_VISITS), MAX_VISITS) + 1;
        long previousVisit = readLong(request, COOKIE_LAST_VISIT);

        response.addCookie(persistentCookie(COOKIE_VISITS, String.valueOf(visits)));
        response.addCookie(persistentCookie(COOKIE_LAST_VISIT, String.valueOf(System.currentTimeMillis())));
        if (previousVisit > 0) {
            response.addCookie(persistentCookie(COOKIE_PREV_VISIT, String.valueOf(previousVisit)));
        } else {
            response.addCookie(new Cookie(COOKIE_PREV_VISIT, "").setMaxAge(0).setPath("/"));
        }

        response.setHeader("Content-Type", "text/html; charset=UTF-8");
        response.getWriter().print(HTML);
    }

    private static Cookie persistentCookie(String name, String value) {
        return new Cookie(name, value)
                .setPath("/")
                .setMaxAge(ONE_YEAR_SECONDS)
                .setSameSite(Cookie.SameSite.Lax);
    }

    /** Reads a numeric cookie; returns 0 if absent, malformed, or negative. */
    private static long readLong(HttpRequest request, String name) {
        return request.getCookie(name)
                .map(Cookie::getValue)
                .map(HomeController::parseNonNegative)
                .orElse(0L);
    }

    private static long parseNonNegative(String raw) {
        try {
            return Math.max(0L, Long.parseLong(raw));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    // -----------------------------------------------------------------------
    // Complete single-page frontend – no external dependencies
    // -----------------------------------------------------------------------
    private static final String HTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Task Manager</title>
                <style>
                    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

                    :root {
                        --bg:        #f5f6fa;
                        --surface:   #ffffff;
                        --border:    #e2e5ef;
                        --text:      #1e2230;
                        --muted:     #6b7280;
                        --accent:    #5b4ef8;
                        --accent-h:  #4338ca;
                        --danger:    #ef4444;
                        --danger-h:  #dc2626;
                        --todo-c:    #6b7280;
                        --prog-c:    #3b82f6;
                        --done-c:    #10b981;
                        --radius:    10px;
                        --shadow:    0 1px 4px rgba(0,0,0,.08), 0 4px 16px rgba(0,0,0,.06);
                    }

                    body { background: var(--bg); color: var(--text); font-family: system-ui, -apple-system, sans-serif; min-height: 100vh; }

                    /* ---- Header ---- */
                    .app-header {
                        background: var(--surface);
                        border-bottom: 1px solid var(--border);
                        padding: 16px 32px;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        position: sticky;
                        top: 0;
                        z-index: 100;
                        box-shadow: 0 1px 3px rgba(0,0,0,.06);
                    }
                    .app-header h1 { font-size: 1.25rem; font-weight: 700; letter-spacing: -.3px; }
                    .app-header h1 span { color: var(--accent); }

                    /* ---- Buttons ---- */
                    .btn {
                        display: inline-flex; align-items: center; gap: 6px;
                        padding: 8px 16px; border-radius: var(--radius);
                        font-size: .875rem; font-weight: 600; cursor: pointer;
                        border: none; transition: background .15s, transform .1s;
                    }
                    .btn:active { transform: scale(.97); }
                    .btn-primary { background: var(--accent); color: #fff; }
                    .btn-primary:hover { background: var(--accent-h); }
                    .btn-ghost { background: transparent; color: var(--muted); border: 1px solid var(--border); }
                    .btn-ghost:hover { background: var(--bg); color: var(--text); }
                    .btn-danger { background: var(--danger); color: #fff; }
                    .btn-danger:hover { background: var(--danger-h); }
                    .btn-icon { padding: 6px 8px; border-radius: 6px; font-size: .8rem; }

                    /* ---- Main layout ---- */
                    .main { max-width: 960px; margin: 0 auto; padding: 32px 24px; }

                    /* ---- Filter bar ---- */
                    .toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 24px; flex-wrap: wrap; }
                    .toolbar .spacer { flex: 1; }
                    .filter-btn {
                        padding: 6px 14px; border-radius: 20px; font-size: .8rem; font-weight: 600;
                        cursor: pointer; border: 1px solid var(--border); background: var(--surface);
                        color: var(--muted); transition: all .15s;
                    }
                    .filter-btn:hover { border-color: var(--accent); color: var(--accent); }
                    .filter-btn.active { background: var(--accent); border-color: var(--accent); color: #fff; }

                    /* ---- Stats ---- */
                    .stats { display: flex; gap: 12px; margin-bottom: 24px; flex-wrap: wrap; }
                    .stat-card {
                        background: var(--surface); border: 1px solid var(--border);
                        border-radius: var(--radius); padding: 14px 20px; flex: 1; min-width: 120px;
                    }
                    .stat-card .count { font-size: 1.75rem; font-weight: 700; }
                    .stat-card .label { font-size: .75rem; color: var(--muted); text-transform: uppercase; letter-spacing: .5px; margin-top: 2px; }
                    .stat-card.todo .count { color: var(--todo-c); }
                    .stat-card.prog .count { color: var(--prog-c); }
                    .stat-card.done .count { color: var(--done-c); }

                    /* ---- Task grid ---- */
                    .task-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }

                    .task-card {
                        background: var(--surface); border: 1px solid var(--border);
                        border-radius: var(--radius); padding: 18px;
                        box-shadow: var(--shadow); display: flex; flex-direction: column; gap: 10px;
                        transition: box-shadow .2s;
                    }
                    .task-card:hover { box-shadow: 0 4px 24px rgba(0,0,0,.12); }

                    .task-card-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 8px; }
                    .task-title { font-weight: 600; font-size: .95rem; flex: 1; line-height: 1.4; word-break: break-word; }
                    .task-desc { font-size: .82rem; color: var(--muted); line-height: 1.5; }
                    .task-card-bottom { display: flex; align-items: center; justify-content: space-between; margin-top: auto; }
                    .task-date { font-size: .73rem; color: var(--muted); }
                    .task-actions { display: flex; gap: 4px; }

                    /* ---- Status badge ---- */
                    .badge {
                        display: inline-block; padding: 3px 10px; border-radius: 20px;
                        font-size: .7rem; font-weight: 700; letter-spacing: .3px; text-transform: uppercase;
                    }
                    .badge-todo   { background: #f3f4f6; color: var(--todo-c); }
                    .badge-prog   { background: #eff6ff; color: var(--prog-c); }
                    .badge-done   { background: #ecfdf5; color: var(--done-c); }

                    /* ---- Empty state ---- */
                    .empty {
                        text-align: center; padding: 64px 24px; color: var(--muted);
                        background: var(--surface); border: 2px dashed var(--border);
                        border-radius: var(--radius); margin-top: 8px;
                    }
                    .empty .icon { font-size: 3rem; margin-bottom: 12px; }
                    .empty p { font-size: .9rem; }

                    /* ---- Modal ---- */
                    .overlay {
                        display: none; position: fixed; inset: 0; background: rgba(0,0,0,.4);
                        z-index: 200; align-items: center; justify-content: center;
                    }
                    .overlay.open { display: flex; }
                    .modal {
                        background: var(--surface); border-radius: 14px;
                        padding: 28px; width: 100%; max-width: 440px;
                        box-shadow: 0 20px 60px rgba(0,0,0,.25);
                        animation: slide-up .2s ease;
                    }
                    @keyframes slide-up { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
                    .modal h2 { font-size: 1.1rem; font-weight: 700; margin-bottom: 20px; }

                    .form-group { margin-bottom: 16px; }
                    .form-group label { display: block; font-size: .8rem; font-weight: 600; color: var(--muted); margin-bottom: 6px; text-transform: uppercase; letter-spacing: .4px; }
                    .form-group input,
                    .form-group textarea,
                    .form-group select {
                        width: 100%; padding: 10px 12px; border: 1px solid var(--border);
                        border-radius: 8px; font-size: .9rem; color: var(--text);
                        background: var(--bg); transition: border-color .15s;
                        font-family: inherit;
                    }
                    .form-group input:focus,
                    .form-group textarea:focus,
                    .form-group select:focus { outline: none; border-color: var(--accent); box-shadow: 0 0 0 3px rgba(91,78,248,.15); }
                    .form-group textarea { resize: vertical; min-height: 80px; }

                    .form-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 20px; }

                    /* ---- Toast ---- */
                    .toast-container { position: fixed; bottom: 24px; right: 24px; z-index: 300; display: flex; flex-direction: column; gap: 8px; }
                    .toast {
                        background: var(--text); color: #fff; padding: 12px 18px;
                        border-radius: 8px; font-size: .85rem; font-weight: 500;
                        animation: fade-in .2s ease; box-shadow: 0 4px 12px rgba(0,0,0,.2);
                    }
                    .toast.error { background: var(--danger); }

                    /* ---- Dark theme (preference stored in a client-side cookie) ---- */
                    [data-theme="dark"] {
                        --bg: #12141c; --surface: #1c1f2b; --border: #2e3345;
                        --text: #e6e8f0; --muted: #9aa1b5; --shadow: 0 1px 4px rgba(0,0,0,.5);
                    }
                    [data-theme="dark"] .badge-todo { background: #2a2e3d; }
                    [data-theme="dark"] .badge-prog { background: #1b2a47; }
                    [data-theme="dark"] .badge-done { background: #12332a; }
                    [data-theme="dark"] .toast { background: #e6e8f0; color: #12141c; }
                    [data-theme="dark"] .toast.error { background: var(--danger); color: #fff; }

                    /* ---- Welcome banner (driven by server-set cookies) ---- */
                    .welcome {
                        background: var(--surface); border: 1px solid var(--border); border-left: 4px solid var(--accent);
                        border-radius: var(--radius); padding: 14px 18px; margin-bottom: 20px;
                        display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap;
                    }
                    .welcome strong { color: var(--accent); }
                    .welcome .sub { font-size: .8rem; color: var(--muted); margin-top: 2px; }
                    .header-actions { display: flex; gap: 8px; }
                    @keyframes fade-in { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
                </style>
            </head>
            <body>

            <!-- ---- Header ---- -->
            <header class="app-header">
                <h1>Task <span>Manager</span></h1>
                <div class="header-actions">
                    <button class="btn btn-ghost" id="theme-btn" onclick="toggleTheme()">Dark mode</button>
                    <button class="btn btn-primary" onclick="openModal()">+ New Task</button>
                </div>
            </header>

            <!-- ---- Main ---- -->
            <main class="main">

                <!-- Welcome banner: filled from the cookies the server set on this page load -->
                <div class="welcome" id="welcome">
                    <div>
                        <div id="welcome-title"></div>
                        <div class="sub" id="welcome-sub"></div>
                    </div>
                    <button class="btn btn-ghost" onclick="resetVisitorData()">Reset visitor data</button>
                </div>

                <!-- Stats row -->
                <div class="stats" id="stats">
                    <div class="stat-card todo"><div class="count" id="cnt-todo">0</div><div class="label">To Do</div></div>
                    <div class="stat-card prog"><div class="count" id="cnt-prog">0</div><div class="label">In Progress</div></div>
                    <div class="stat-card done"><div class="count" id="cnt-done">0</div><div class="label">Done</div></div>
                </div>

                <!-- Filter + sort toolbar -->
                <div class="toolbar">
                    <button class="filter-btn active" data-f="ALL"       onclick="setFilter('ALL',this)">All</button>
                    <button class="filter-btn"         data-f="TODO"     onclick="setFilter('TODO',this)">To Do</button>
                    <button class="filter-btn"         data-f="IN_PROGRESS" onclick="setFilter('IN_PROGRESS',this)">In Progress</button>
                    <button class="filter-btn"         data-f="DONE"     onclick="setFilter('DONE',this)">Done</button>
                    <span class="spacer"></span>
                </div>

                <!-- Task cards -->
                <div class="task-grid" id="task-grid"></div>
            </main>

            <!-- ---- Add / Edit modal ---- -->
            <div class="overlay" id="overlay" onclick="handleOverlayClick(event)">
                <div class="modal">
                    <h2 id="modal-title">New Task</h2>
                    <form id="task-form" onsubmit="submitForm(event)">
                        <input type="hidden" id="f-id">
                        <div class="form-group">
                            <label>Title *</label>
                            <input id="f-title" type="text" placeholder="What needs to be done?" required>
                        </div>
                        <div class="form-group">
                            <label>Description</label>
                            <textarea id="f-desc" placeholder="Optional details..."></textarea>
                        </div>
                        <div class="form-group">
                            <label>Status</label>
                            <select id="f-status">
                                <option value="TODO">To Do</option>
                                <option value="IN_PROGRESS">In Progress</option>
                                <option value="DONE">Done</option>
                            </select>
                        </div>
                        <div class="form-actions">
                            <button type="button" class="btn btn-ghost" onclick="closeModal()">Cancel</button>
                            <button type="submit" class="btn btn-primary" id="submit-btn">Create</button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- ---- Toast container ---- -->
            <div class="toast-container" id="toasts"></div>

            <script>
                /* ------------------------------------------------------------------ */
                /* State                                                               */
                /* ------------------------------------------------------------------ */
                let allTasks = [];
                let activeFilter = 'ALL';

                /* ------------------------------------------------------------------ */
                /* Boot                                                                */
                /* ------------------------------------------------------------------ */
                initCookies();
                loadTasks();

                /* ------------------------------------------------------------------ */
                /* Cookies: read server-set cookies, read/write the client-set theme   */
                /* ------------------------------------------------------------------ */
                function getCookie(name) {
                    const pair = document.cookie.split('; ').find(c => c.startsWith(name + '='));
                    return pair ? decodeURIComponent(pair.substring(name.length + 1)) : null;
                }

                function setCookie(name, value, days) {
                    const maxAge = days * 24 * 60 * 60;
                    document.cookie = name + '=' + encodeURIComponent(value) + '; Max-Age=' + maxAge + '; Path=/; SameSite=Lax';
                }

                function deleteCookie(name) {
                    document.cookie = name + '=; Max-Age=0; Path=/';
                }

                function timeAgo(epochMs) {
                    const s = Math.max(0, Math.floor((Date.now() - epochMs) / 1000));
                    if (s < 60) return 'moments ago';
                    if (s < 3600) return Math.floor(s / 60) + ' minute(s) ago';
                    if (s < 86400) return Math.floor(s / 3600) + ' hour(s) ago';
                    return Math.floor(s / 86400) + ' day(s) ago';
                }

                function initCookies() {
                    applyTheme(getCookie('theme') === 'dark' ? 'dark' : 'light');

                    // visits / prevVisit were set by the server in this page's response headers
                    const visits = parseInt(getCookie('visits'), 10);
                    const prev = parseInt(getCookie('prevVisit'), 10);
                    const title = document.getElementById('welcome-title');
                    const sub = document.getElementById('welcome-sub');
                    if (!visits || visits <= 1) {
                        title.innerHTML = 'Welcome! <strong>First visit</strong> &mdash; the server just gave your browser a cookie.';
                        sub.textContent = 'Reload the page to see it remember you.';
                    } else {
                        title.innerHTML = 'Welcome back! This is visit <strong>#' + visits + '</strong>.';
                        sub.textContent = prev ? 'Your previous visit was ' + timeAgo(prev) + ' (' + new Date(prev).toLocaleString() + ').' : '';
                    }
                }

                function applyTheme(theme) {
                    document.documentElement.setAttribute('data-theme', theme);
                    document.getElementById('theme-btn').textContent = theme === 'dark' ? 'Light mode' : 'Dark mode';
                }

                function toggleTheme() {
                    const next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
                    setCookie('theme', next, 365);
                    applyTheme(next);
                }

                function resetVisitorData() {
                    ['visits', 'lastVisit', 'prevVisit', 'theme'].forEach(deleteCookie);
                    toast('Visitor cookies cleared. Reload to start fresh.');
                }

                /* ------------------------------------------------------------------ */
                /* API helpers                                                        */
                /* ------------------------------------------------------------------ */
                async function api(method, path, body) {
                    const opts = { method, headers: { 'Content-Type': 'application/json' } };
                    if (body !== undefined) opts.body = JSON.stringify(body);
                    const res = await fetch(path, opts);
                    if (res.status === 204) return null;
                    const data = await res.json();
                    if (!res.ok) {
                        throw new Error(data.error || `Request failed (${res.status})`);
                    }
                    return data;
                }

                /* ------------------------------------------------------------------ */
                /* Load & render                                                       */
                /* ------------------------------------------------------------------ */
                async function loadTasks() {
                    try {
                        allTasks = await api('GET', '/tasks');
                        if (!Array.isArray(allTasks)) allTasks = [];
                        render();
                    } catch (e) {
                        toast('Could not load tasks: ' + e.message, true);
                    }
                }

                function render() {
                    updateStats();
                    const grid = document.getElementById('task-grid');
                    const visible = (activeFilter === 'ALL')
                        ? allTasks
                        : allTasks.filter(t => t.status === activeFilter);

                    // Newest first
                    visible.sort((a, b) => b.id - a.id);

                    if (visible.length === 0) {
                        grid.innerHTML = `<div class="empty" style="grid-column:1/-1">
                            <div class="icon">&#x1F4CB;</div>
                            <p>${activeFilter === 'ALL' ? 'No tasks yet. Click <strong>+ New Task</strong> to get started.' : 'No tasks with this status.'}</p>
                        </div>`;
                        return;
                    }
                    grid.innerHTML = visible.map(taskCard).join('');
                }

                function taskCard(t) {
                    const badgeCls = { TODO: 'badge-todo', IN_PROGRESS: 'badge-prog', DONE: 'badge-done' }[t.status] || 'badge-todo';
                    const badgeLbl = { TODO: 'To Do', IN_PROGRESS: 'In Progress', DONE: 'Done' }[t.status] || t.status;
                    const date = t.createdAt ? t.createdAt.replace('T', ' ').substring(0, 16) : '';
                    const desc = t.description ? `<div class="task-desc">${escHtml(t.description)}</div>` : '';
                    return `<div class="task-card">
                        <div class="task-card-top">
                            <div class="task-title">${escHtml(t.title)}</div>
                            <span class="badge ${badgeCls}">${badgeLbl}</span>
                        </div>
                        ${desc}
                        <div class="task-card-bottom">
                            <div class="task-date">${date}</div>
                            <div class="task-actions">
                                <button class="btn btn-ghost btn-icon" title="Edit" onclick="editTask(${t.id})">&#9998;</button>
                                <button class="btn btn-danger btn-icon" title="Delete" onclick="deleteTask(${t.id})">&#128465;</button>
                            </div>
                        </div>
                    </div>`;
                }

                function updateStats() {
                    document.getElementById('cnt-todo').textContent = allTasks.filter(t => t.status === 'TODO').length;
                    document.getElementById('cnt-prog').textContent = allTasks.filter(t => t.status === 'IN_PROGRESS').length;
                    document.getElementById('cnt-done').textContent = allTasks.filter(t => t.status === 'DONE').length;
                }

                /* ------------------------------------------------------------------ */
                /* Filtering                                                           */
                /* ------------------------------------------------------------------ */
                function setFilter(f, btn) {
                    activeFilter = f;
                    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
                    btn.classList.add('active');
                    render();
                }

                /* ------------------------------------------------------------------ */
                /* Modal                                                               */
                /* ------------------------------------------------------------------ */
                function openModal(task) {
                    document.getElementById('modal-title').textContent = task ? 'Edit Task' : 'New Task';
                    document.getElementById('submit-btn').textContent = task ? 'Save Changes' : 'Create';
                    document.getElementById('f-id').value = task ? task.id : '';
                    document.getElementById('f-title').value = task ? task.title : '';
                    document.getElementById('f-desc').value = task ? task.description : '';
                    document.getElementById('f-status').value = task ? task.status : 'TODO';
                    document.getElementById('overlay').classList.add('open');
                    setTimeout(() => document.getElementById('f-title').focus(), 50);
                }

                function closeModal() {
                    document.getElementById('overlay').classList.remove('open');
                    document.getElementById('task-form').reset();
                }

                function handleOverlayClick(e) {
                    if (e.target === document.getElementById('overlay')) closeModal();
                }

                /* ------------------------------------------------------------------ */
                /* CRUD operations                                                     */
                /* ------------------------------------------------------------------ */
                async function submitForm(e) {
                    e.preventDefault();
                    const id = document.getElementById('f-id').value;
                    const payload = {
                        title:       document.getElementById('f-title').value.trim(),
                        description: document.getElementById('f-desc').value.trim(),
                        status:      document.getElementById('f-status').value
                    };

                    try {
                        if (id) {
                            const data = await api('PUT', '/tasks', { id: Number(id), ...payload });
                            if (data && data.error) { toast(data.error, true); return; }
                            toast('Task updated');
                        } else {
                            const data = await api('POST', '/tasks', payload);
                            if (data && data.error) { toast(data.error, true); return; }
                            toast('Task created');
                        }
                        closeModal();
                        await loadTasks();
                    } catch (err) {
                        toast('Request failed: ' + err.message, true);
                    }
                }

                function editTask(id) {
                    const task = allTasks.find(t => t.id === id);
                    if (task) openModal(task);
                }

                async function deleteTask(id) {
                    if (!confirm('Delete this task? This cannot be undone.')) return;
                    try {
                        const data = await api('DELETE', '/tasks', { id });
                        if (data && data.error) { toast(data.error, true); return; }
                        toast('Task deleted');
                        await loadTasks();
                    } catch (err) {
                        toast('Delete failed: ' + err.message, true);
                    }
                }

                /* ------------------------------------------------------------------ */
                /* Utilities                                                           */
                /* ------------------------------------------------------------------ */
                function escHtml(str) {
                    if (!str) return '';
                    return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
                }

                function toast(msg, isError) {
                    const el = document.createElement('div');
                    el.className = 'toast' + (isError ? ' error' : '');
                    el.textContent = msg;
                    document.getElementById('toasts').appendChild(el);
                    setTimeout(() => el.remove(), 3500);
                }
            </script>
            </body>
            </html>
            """;
}
