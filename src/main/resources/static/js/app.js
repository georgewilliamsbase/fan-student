// Фронтенд для REST API fun-student (Spring Boot + JWT)

const API = '/api/v1';
const ROLE_NAMES = { SUPER_ADMIN: 'Суперадмин', ADMIN: 'Админ', TEACHER: 'Преподаватель' };

// Сессия (токен хранится в localStorage)
const session = {
    get token() { return localStorage.getItem('token'); },
    get roles() { return JSON.parse(localStorage.getItem('roles') || '[]'); },
    get email() { return localStorage.getItem('email') || ''; },
    get expiresAt() { return Number(localStorage.getItem('expiresAt') || 0); },

    save(auth, email) {
        localStorage.setItem('token', auth.token);
        // сервер присылает роли вида "ROLE_ADMIN" - убираем префикс
        localStorage.setItem('roles', JSON.stringify((auth.roles || []).map(r => r.replace('ROLE_', ''))));
        localStorage.setItem('email', email);
        localStorage.setItem('expiresAt', String(Date.now() + (auth.expiresIn || 3600) * 1000));
    },
    clear() { ['token', 'roles', 'email', 'expiresAt'].forEach(k => localStorage.removeItem(k)); },
    isValid() { return !!this.token && Date.now() < this.expiresAt; },
    has(...roles) { return roles.some(r => this.roles.includes(r)); }
};

// Запросы к API
async function api(path, { method = 'GET', body } = {}) {
    const headers = { 'Content-Type': 'application/json' };
    if (session.token) headers['Authorization'] = 'Bearer ' + session.token;

    let res;
    try {
        res = await fetch(API + path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
    } catch {
        throw new Error('Сервер недоступен');
    }

    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = text; }

    if (res.status === 401) {
        logout('Сессия истекла, войдите снова');
        throw new Error('Не авторизован');
    }
    if (!res.ok) {
        let msg = (data && (data.message || data.error)) || '';
        if (res.status === 403) msg = 'Недостаточно прав для этого действия';
        else if (res.status === 404 && !msg) msg = 'Не найдено';
        else if (!msg) msg = 'Ошибка сервера (' + res.status + ')';
        throw new Error(msg);
    }
    return data;
}

// Утилиты
const $ = sel => document.querySelector(sel);
const $$ = sel => document.querySelectorAll(sel);
function show(node, visible = true) { node.classList.toggle('hidden', !visible); }

function el(tag, attrs = {}, ...children) {
    const node = document.createElement(tag);
    for (const [k, v] of Object.entries(attrs)) {
        if (k === 'class') node.className = v;
        else if (k.startsWith('on')) node.addEventListener(k.slice(2), v);
        else node.setAttribute(k, v);
    }
    for (const c of children) node.append(c instanceof Node ? c : document.createTextNode(c ?? ''));
    return node;
}

// всплывающие уведомления в стиле lombard-project
function toast(text, ok = true) {
    const t = el('div', { class: 'toast' + (ok ? '' : ' error') }, text);
    $('#toasts').append(t);
    setTimeout(() => { t.classList.add('hide'); setTimeout(() => t.remove(), 300); }, 3500);
}

function roleBadges(roles) {
    return el('span', { class: 'badges' },
        ...roles.map(r => el('span', { class: 'status-badge role-' + r }, ROLE_NAMES[r] || r)));
}

// Тема (светлая / темная)
const SUN = '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/></svg>';
const MOON = '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>';

function applyTheme(theme) {
    localStorage.setItem('app_theme', theme);
    document.body.classList.toggle('theme-dark', theme === 'dark');
    document.documentElement.classList.toggle('pre-dark', theme === 'dark');
    $$('.theme-btn').forEach(b => b.classList.toggle('selected', b.dataset.theme === theme));
}

$$('.theme-switch').forEach(box => {
    for (const [theme, icon, title] of [['light', SUN, 'Светлая'], ['dark', MOON, 'Темная']]) {
        const b = el('button', { type: 'button', class: 'theme-btn', title, 'data-theme': theme });
        b.innerHTML = icon;
        b.addEventListener('click', () => applyTheme(theme));
        box.append(b);
    }
});
applyTheme(localStorage.getItem('app_theme') || 'light');

// Окно подтверждения
function confirmDialog(text) {
    return new Promise(resolve => {
        $('#confirm-text').textContent = text;
        $('#confirm').classList.add('active');
        const done = result => {
            $('#confirm').classList.remove('active');
            $('#confirm-ok').onclick = $('#confirm-cancel').onclick = null;
            resolve(result);
        };
        $('#confirm-ok').onclick = () => done(true);
        $('#confirm-cancel').onclick = () => done(false);
    });
}

// Модальное окно с формой
// fields: {name,label,type,value,required,readonly} | {type:'roles',value} | {type:'view',rows}
function openModal(title, fields, onSubmit, submitText = 'Сохранить') {
    $('#modal-title').textContent = title;
    const form = $('#modal-form');
    form.replaceChildren();

    for (const f of fields) {
        if (f.type === 'roles') {
            const box = el('div', { class: 'checks' }, el('label', {}, 'Роли'));
            for (const role of Object.keys(ROLE_NAMES)) {
                const cb = el('input', { type: 'checkbox', name: 'roles', value: role });
                cb.checked = (f.value || []).includes(role);
                box.append(el('label', { class: 'check' }, cb, ROLE_NAMES[role] + ' (' + role + ')'));
            }
            form.append(box);
        } else if (f.type === 'view') {
            const dl = el('dl', { class: 'details' });
            for (const [k, v] of f.rows) dl.append(el('div', { class: 'details-row' }, el('dt', {}, k), el('dd', {}, v || '-')));
            form.append(dl);
        } else {
            const input = el('input', { type: f.type || 'text', name: f.name, placeholder: f.placeholder || '' });
            input.value = f.value ?? '';
            if (f.required) input.required = true;
            if (f.readonly) input.readOnly = true;
            form.append(el('div', { class: 'form-group' }, el('label', {}, f.label), input));
        }
    }

    const error = el('div', { class: 'error hidden' });
    const buttons = el('div', { class: 'confirm-actions' },
        el('button', { type: 'button', class: 'confirm-btn cancel', onclick: closeModal }, onSubmit ? 'Отмена' : 'Закрыть'));
    if (onSubmit) buttons.append(el('button', { type: 'submit', class: 'confirm-btn ok' }, submitText));
    form.append(error, buttons);

    form.onsubmit = async e => {
        e.preventDefault();
        if (!onSubmit) return;
        const data = Object.fromEntries(new FormData(form));
        data.roles = [...form.querySelectorAll('input[name=roles]:checked')].map(c => c.value);
        try {
            await onSubmit(data);
            closeModal();
        } catch (err) {
            error.textContent = err.message;
            show(error);
        }
    };

    $('#modal').classList.add('active');
    const first = form.querySelector('input:not([readonly]):not([type=checkbox])');
    if (first) first.focus();
}

function closeModal() { $('#modal').classList.remove('active'); }
$('#modal').addEventListener('click', e => { if (e.target.id === 'modal') closeModal(); });
document.addEventListener('keydown', e => {
    if (e.key !== 'Escape') return;
    closeModal();
    if ($('#confirm').classList.contains('active')) $('#confirm-cancel').click();
});

// Вход / выход
$('#login-form').addEventListener('submit', async e => {
    e.preventDefault();
    const email = $('#login-email').value.trim();
    const password = $('#login-password').value;
    const errorBox = $('#login-error');
    show(errorBox, false);
    try {
        let res;
        try {
            res = await fetch(API + '/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
        } catch { throw new Error('Сервер недоступен'); }
        if (!res.ok) throw new Error('Неверный логин или пароль');
        session.save(await res.json(), email);
        $('#login-password').value = '';
        startApp();
        toast('Добро пожаловать, ' + email);
    } catch (err) {
        errorBox.textContent = err.message;
        show(errorBox);
    }
});

$('#logout-btn').addEventListener('click', () => logout());

function logout(reason) {
    session.clear();
    closeModal();
    show($('#app-view'), false);
    show($('#login-view'));
    show($('#login-switchers'));
    show($('#login-error'), !!reason);
    if (reason) $('#login-error').textContent = reason;
}

// Навигация
const canStudents = () => session.has('ADMIN', 'TEACHER');
const canUsers = () => session.has('SUPER_ADMIN');

$$('.sidebar-nav a[data-tab]').forEach(a => a.addEventListener('click', () => openTab(a.dataset.tab)));

$$('.tile[data-go]').forEach(t => t.addEventListener('click', () => {
    const go = t.dataset.go;
    if (go === 'logout') logout();
    else if (go === 'add-student') { openTab('students'); addStudent(); }
    else if (go === 'add-user') { openTab('users'); addUser(); }
    else openTab(go);
}));

function openTab(name) {
    $$('.sidebar-nav a[data-tab]').forEach(a => a.classList.toggle('active', a.dataset.tab === name));
    show($('#home-tab'), name === 'home');
    show($('#students-tab'), name === 'students');
    show($('#users-tab'), name === 'users');
    if (name === 'students') loadStudents();
    if (name === 'users') loadUsers();
}

function startApp() {
    show($('#login-view'), false);
    show($('#login-switchers'), false);
    show($('#app-view'));
    $('#who-email').textContent = session.email;
    $('#who-roles').replaceChildren(roleBadges(session.roles));

    $$('.role-students').forEach(n => show(n, canStudents()));
    $$('.role-users').forEach(n => show(n, canUsers()));
    $$('.admin-only').forEach(n => show(n, session.has('ADMIN')));
    show($('#no-access'), !canStudents() && !canUsers());

    openTab('home');
}

//  Студенты
let studentsCache = [];

async function loadStudents() {
    try {
        const data = await api('/student');
        studentsCache = (data && data.response) || [];
        $('#students-count').textContent = '(' + studentsCache.length + ')';
        renderStudents();
    } catch (err) { toast(err.message, false); }
}

function renderStudents() {
    const q = $('#students-search').value.trim().toLowerCase();
    const list = studentsCache.filter(s =>
        !q || String(s.id).includes(q) || (s.name || '').toLowerCase().includes(q) || (s.surname || '').toLowerCase().includes(q));
    show($('#students-empty'), list.length === 0);
    show($('#students-body').closest('.table-wrap'), list.length > 0);

    const isAdmin = session.has('ADMIN');
    $('#students-body').replaceChildren(...list.map(s => {
        const actions = el('div', { class: 'row-actions' },
            el('button', { class: 'action-btn issue', onclick: () => viewStudent(s.id) }, 'Подробнее'));
        if (isAdmin) {
            actions.append(
                el('button', { class: 'action-btn approve', onclick: () => editStudent(s.id) }, 'Редактировать'),
                el('button', { class: 'action-btn reject', onclick: () => deleteStudent(s) }, 'Удалить'));
        }
        return el('tr', {},
            el('td', {}, el('b', { class: 'text-accent' }, '#' + s.id)),
            el('td', {}, s.name), el('td', {}, s.surname), el('td', {}, actions));
    }));
}
$('#students-search').addEventListener('input', renderStudents);

async function viewStudent(id) {
    try {
        const s = await api('/student/' + id);
        openModal('Студент #' + s.id, [{
            type: 'view', rows: [['Имя', s.name], ['Фамилия', s.surname], ['Телефон', s.phoneNumber], ['Email', s.email]]
        }], null);
    } catch (err) { toast(err.message, false); }
}

function addStudent() {
    openModal('Новый студент', [
        { name: 'name', label: 'Имя', required: true, placeholder: 'Введите имя' },
        { name: 'surname', label: 'Фамилия', required: true, placeholder: 'Введите фамилию' },
        { name: 'phone', label: 'Телефон', type: 'tel', placeholder: '+7 (___) ___-__-__' },
        { name: 'email', label: 'Email', type: 'email', placeholder: 'student@example.com' }
    ], async d => {
        // StudentRequest ждет телефон в поле phone_number
        await api('/student/save', {
            method: 'POST',
            body: { name: d.name, surname: d.surname, phone_number: d.phone, email: d.email }
        });
        toast('Студент добавлен');
        loadStudents();
    }, 'Добавить');
}
$('#add-student-btn').addEventListener('click', addStudent);

async function editStudent(id) {
    try {
        const s = await api('/student/' + id);
        // по API можно менять только телефон и email
        openModal('Редактировать студента #' + s.id, [
            { name: 'fio', label: 'Студент', value: s.name + ' ' + s.surname, readonly: true },
            { name: 'phoneNumber', label: 'Телефон', type: 'tel', value: s.phoneNumber },
            { name: 'email', label: 'Email', type: 'email', value: s.email }
        ], async d => {
            await api('/student/' + id, { method: 'PUT', body: { phoneNumber: d.phoneNumber, email: d.email } });
            toast('Данные студента обновлены');
            loadStudents();
        });
    } catch (err) { toast(err.message, false); }
}

async function deleteStudent(s) {
    if (!await confirmDialog('Удалить студента ' + s.name + ' ' + s.surname + '?')) return;
    try {
        await api('/student/' + s.id, { method: 'DELETE' });
        toast('Студент удален');
        loadStudents();
    } catch (err) { toast(err.message, false); }
}

//  Пользователи (только SUPER_ADMIN)
async function loadUsers() {
    try {
        const data = await api('/users');
        const list = (data && data.responses) || [];
        $('#users-count').textContent = '(' + list.length + ')';
        show($('#users-empty'), list.length === 0);

        $('#users-body').replaceChildren(...list.map(u => {
            const actions = el('div', { class: 'row-actions' },
                el('button', { class: 'action-btn approve', onclick: () => editUser(u) }, 'Роли'));
            if (u.email !== session.email) {
                actions.append(el('button', { class: 'action-btn reject', onclick: () => deleteUser(u) }, 'Удалить'));
            }
            return el('tr', {},
                el('td', {}, el('b', { class: 'text-accent' }, '#' + u.id)),
                el('td', {}, u.email), el('td', {}, roleBadges(u.roles || [])), el('td', {}, actions));
        }));
    } catch (err) { toast(err.message, false); }
}

function addUser() {
    openModal('Новый пользователь', [
        { name: 'email', label: 'Email', type: 'email', required: true, placeholder: 'user@example.com' },
        { name: 'password', label: 'Пароль', type: 'password', required: true, placeholder: 'Введите пароль' },
        { type: 'roles', value: ['TEACHER'] }
    ], async d => {
        if (d.roles.length === 0) throw new Error('Выберите хотя бы одну роль');
        await api('/users/save', { method: 'POST', body: { email: d.email, password: d.password, roles: d.roles } });
        toast('Пользователь создан');
        loadUsers();
    }, 'Создать');
}
$('#add-user-btn').addEventListener('click', addUser);

function editUser(u) {
    openModal('Роли пользователя', [
        { name: 'email', label: 'Email', value: u.email, readonly: true },
        { type: 'roles', value: u.roles }
    ], async d => {
        if (d.roles.length === 0) throw new Error('Выберите хотя бы одну роль');
        await api('/users/' + u.id, { method: 'PUT', body: { email: u.email, roles: d.roles } });
        toast('Роли обновлены');
        loadUsers();
    });
}

async function deleteUser(u) {
    if (!await confirmDialog('Удалить пользователя ' + u.email + '?')) return;
    try {
        await api('/users/' + u.id, { method: 'DELETE' });
        toast('Пользователь удален');
        loadUsers();
    } catch (err) { toast(err.message, false); }
}

// Старт
if (session.isValid()) startApp();
else logout();
