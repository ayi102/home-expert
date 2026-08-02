// Companion web app UI. Thin layer over the tested Store/contract. Works fully
// offline against localStorage now; swap the backend for Firestore later and
// this file is unchanged.

import { Store, BrowserBackend } from './store.js';
import {
  makeEvent, makeList, makeListItem, makeChore, makeReminder, RECURRENCES,
} from './contract.js';
import { CALC_METHODS, MADHABS, mergeHousehold, defaultHousehold } from './settings.js';

const store = new Store(new BrowserBackend());
const view = document.getElementById('view');
let currentTab = 'calendar';

// ---- tiny DOM helpers ------------------------------------------------------
function el(tag, attrs = {}, ...kids) {
  const node = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (k === 'class') node.className = v;
    else if (k === 'html') node.innerHTML = v;
    else if (k.startsWith('on') && typeof v === 'function') node.addEventListener(k.slice(2), v);
    else if (v != null) node.setAttribute(k, v);
  }
  for (const kid of kids.flat()) {
    if (kid == null) continue;
    node.append(kid.nodeType ? kid : document.createTextNode(String(kid)));
  }
  return node;
}

function section(title, ...children) {
  return el('section', { class: 'card' }, el('h2', {}, title), ...children);
}

function fmtDateTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
}

function toIso(localValue) {
  return localValue ? new Date(localValue).toISOString() : '';
}

// ---- config (settings) -----------------------------------------------------
function loadConfig() {
  try { return mergeHousehold(JSON.parse(localStorage.getItem('hd:config'))); }
  catch { return defaultHousehold(); }
}
function saveConfig(cfg) {
  localStorage.setItem('hd:config', JSON.stringify(cfg));
  document.getElementById('syncNote').textContent = 'Saved locally';
}

// ---- tabs ------------------------------------------------------------------
document.getElementById('tabs').addEventListener('click', (e) => {
  const btn = e.target.closest('button[data-tab]');
  if (!btn) return;
  currentTab = btn.dataset.tab;
  for (const b of document.querySelectorAll('#tabs button')) {
    b.classList.toggle('active', b.dataset.tab === currentTab);
  }
  render();
});

store.subscribe(() => render());

function render() {
  view.replaceChildren();
  switch (currentTab) {
    case 'calendar': return renderCalendar();
    case 'lists': return renderLists();
    case 'chores': return renderChores();
    case 'reminders': return renderReminders();
    case 'settings': return renderSettings();
  }
}

// ---- calendar --------------------------------------------------------------
function renderCalendar() {
  const title = el('input', { placeholder: 'Event title', id: 'evTitle' });
  const start = el('input', { type: 'datetime-local', id: 'evStart' });
  const end = el('input', { type: 'datetime-local', id: 'evEnd' });
  const addBtn = el('button', { class: 'primary', onclick: () => {
    const ev = makeEvent({
      title: title.value,
      start: toIso(start.value),
      end: end.value ? toIso(end.value) : null,
    });
    tryAdd('events', ev, [title, start]);
  } }, 'Add event');

  const form = section('New event',
    el('div', { class: 'form-row' }, labelled('Title', title)),
    el('div', { class: 'form-row' }, labelled('Start', start), labelled('End (optional)', end)),
    addBtn);

  const events = store.all('events').sort((a, b) => a.start.localeCompare(b.start));
  const list = section(`Upcoming (${events.length})`,
    events.length ? el('ul', { class: 'items' }, events.map(eventRow))
      : el('p', { class: 'empty' }, 'No events yet. New events also sync to your Google "Home" calendar once the cloud is connected.'));

  view.append(form, list);
}

function eventRow(ev) {
  return el('li', {},
    el('div', { class: 'grow' },
      el('strong', {}, ev.title),
      el('div', { class: 'sub' }, fmtDateTime(ev.start) + (ev.end ? ` – ${fmtDateTime(ev.end)}` : ''))),
    delBtn('events', ev.id));
}

// ---- lists -----------------------------------------------------------------
function renderLists() {
  const name = el('input', { placeholder: 'List name (e.g. Groceries)' });
  const add = el('button', { class: 'primary', onclick: () => {
    tryAdd('lists', makeList({ name: name.value, kind: 'shopping' }), [name]);
  } }, 'Add list');
  view.append(section('New list', el('div', { class: 'form-row' }, labelled('Name', name), add)));

  for (const list of store.all('lists')) {
    const items = store.all('listItems').filter((i) => i.listId === list.id);
    const itemInput = el('input', { placeholder: 'Add item…' });
    const addItem = () => tryAdd('listItems', makeListItem({ listId: list.id, text: itemInput.value }), [itemInput]);
    itemInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') addItem(); });

    view.append(section(list.name,
      el('ul', { class: 'items' }, items.map((it) => el('li', {},
        checkbox(it.done, () => store.toggleDone('listItems', it.id)),
        el('span', { class: it.done ? 'grow done' : 'grow' }, it.text),
        delBtn('listItems', it.id)))),
      el('div', { class: 'form-row' }, itemInput, el('button', { onclick: addItem }, 'Add'),
        el('button', { class: 'danger', onclick: () => store.remove('lists', list.id) }, 'Delete list')),
    ));
  }
}

// ---- chores ----------------------------------------------------------------
function renderChores() {
  const title = el('input', { placeholder: 'Chore (e.g. Take out trash)' });
  const assignee = el('input', { placeholder: 'Assignee (optional)' });
  const rec = el('select', {}, RECURRENCES.map((r) => el('option', { value: r }, r)));
  const add = el('button', { class: 'primary', onclick: () => {
    tryAdd('chores', makeChore({ title: title.value, assignee: assignee.value, recurrence: rec.value }), [title]);
  } }, 'Add chore');
  view.append(section('New chore',
    el('div', { class: 'form-row' }, labelled('Chore', title), labelled('Assignee', assignee), labelled('Repeat', rec)),
    add));

  const chores = store.all('chores');
  view.append(section(`Chores (${chores.length})`,
    chores.length ? el('ul', { class: 'items' }, chores.map((c) => el('li', {},
      checkbox(c.done, () => store.toggleDone('chores', c.id)),
      el('div', { class: c.done ? 'grow done' : 'grow' },
        el('strong', {}, c.title),
        el('div', { class: 'sub' }, [c.assignee, c.recurrence !== 'none' ? `repeats ${c.recurrence}` : null].filter(Boolean).join(' • '))),
      delBtn('chores', c.id))))
      : el('p', { class: 'empty' }, 'No chores yet.')));
}

// ---- reminders -------------------------------------------------------------
function renderReminders() {
  const text = el('input', { placeholder: 'Remind me to…' });
  const when = el('input', { type: 'datetime-local' });
  const add = el('button', { class: 'primary', onclick: () => {
    tryAdd('reminders', makeReminder({ text: text.value, remindAt: toIso(when.value) }), [text, when]);
  } }, 'Add reminder');
  view.append(section('New reminder',
    el('div', { class: 'form-row' }, labelled('Reminder', text), labelled('When', when)), add));

  const items = store.all('reminders').sort((a, b) => a.remindAt.localeCompare(b.remindAt));
  view.append(section(`Reminders (${items.length})`,
    items.length ? el('ul', { class: 'items' }, items.map((r) => el('li', {},
      checkbox(r.done, () => store.toggleDone('reminders', r.id)),
      el('div', { class: r.done ? 'grow done' : 'grow' },
        el('strong', {}, r.text), el('div', { class: 'sub' }, fmtDateTime(r.remindAt))),
      delBtn('reminders', r.id))))
      : el('p', { class: 'empty' }, 'No reminders yet.')));
}

// ---- settings --------------------------------------------------------------
function renderSettings() {
  const cfg = loadConfig();
  const method = el('select', {}, CALC_METHODS.map(([v, l]) => el('option', { value: v, ...(v === cfg.prayer.method ? { selected: '' } : {}) }, l)));
  const madhab = el('select', {}, MADHABS.map(([v, l]) => el('option', { value: v, ...(v === cfg.prayer.madhab ? { selected: '' } : {}) }, l)));
  const lat = el('input', { type: 'number', step: 'any', value: cfg.prayer.latitude });
  const lng = el('input', { type: 'number', step: 'any', value: cfg.prayer.longitude });
  const tz = el('input', { value: cfg.prayer.timeZoneId });
  const household = el('input', { value: cfg.general.householdName });

  const save = el('button', { class: 'primary', onclick: () => {
    cfg.prayer.method = method.value;
    cfg.prayer.madhab = madhab.value;
    cfg.prayer.latitude = parseFloat(lat.value);
    cfg.prayer.longitude = parseFloat(lng.value);
    cfg.prayer.timeZoneId = tz.value.trim();
    cfg.general.householdName = household.value.trim();
    saveConfig(cfg);
  } }, 'Save settings');

  view.append(
    section('Prayer settings',
      el('div', { class: 'form-row' }, labelled('Calculation method', method), labelled('Asr madhab', madhab)),
      el('div', { class: 'form-row' }, labelled('Latitude', lat), labelled('Longitude', lng), labelled('Time zone', tz)),
      el('p', { class: 'sub' }, 'Location is a placeholder until the tablet’s GPS sets it automatically.')),
    section('General', el('div', { class: 'form-row' }, labelled('Household name', household))),
    save);
}

// ---- shared widgets --------------------------------------------------------
function labelled(text, input) {
  return el('label', { class: 'field' }, el('span', {}, text), input);
}
function checkbox(checked, onToggle) {
  const c = el('input', { type: 'checkbox', class: 'chk' });
  c.checked = checked;
  c.addEventListener('change', onToggle);
  return c;
}
function delBtn(collection, id) {
  return el('button', { class: 'icon danger', title: 'Delete', onclick: () => store.remove(collection, id) }, '×');
}
function tryAdd(collection, record, toClear) {
  try {
    store.add(collection, record);
    for (const input of toClear) input.value = '';
  } catch (err) {
    alert(err.errors ? err.errors.join('\n') : err.message);
  }
}

render();
