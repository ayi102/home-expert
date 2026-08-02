// Shared data contract for the Home Dashboard.
//
// This is the single source of truth for the shapes stored in the cloud DB and
// read by BOTH the companion web app and the Android tablet app. Keep the field
// names here in lock-step with the Kotlin data classes on the Android side.
//
// Pure, dependency-free, and unit-tested (see test/contract.test.js).

export const COLLECTIONS = ['events', 'lists', 'listItems', 'chores', 'reminders'];

/** Monotonic-ish unique id; good enough for a household-scale app. */
export function newId(prefix = 'id') {
  const rand = Math.random().toString(36).slice(2, 8);
  return `${prefix}_${Date.now().toString(36)}_${rand}`;
}

function nowIso() {
  return new Date().toISOString();
}

function isNonEmptyString(v) {
  return typeof v === 'string' && v.trim().length > 0;
}

function isIsoDate(v) {
  return typeof v === 'string' && !Number.isNaN(Date.parse(v));
}

// ---- Factories -------------------------------------------------------------
// Each factory fills defaults + timestamps. Validate separately with validate().

export function makeEvent({ title, start, end = null, allDay = false, notes = '', calendarId = 'home' }) {
  const ts = nowIso();
  return {
    id: newId('evt'), type: 'event',
    title: title ?? '', start: start ?? '', end, allDay, notes, calendarId,
    createdAt: ts, updatedAt: ts,
  };
}

export function makeList({ name, kind = 'custom' }) {
  const ts = nowIso();
  return { id: newId('lst'), type: 'list', name: name ?? '', kind, createdAt: ts, updatedAt: ts };
}

export function makeListItem({ listId, text, done = false }) {
  const ts = nowIso();
  return {
    id: newId('itm'), type: 'listItem',
    listId: listId ?? '', text: text ?? '', done: !!done,
    createdAt: ts, updatedAt: ts,
  };
}

export function makeChore({ title, assignee = '', dueDate = null, recurrence = 'none', done = false }) {
  const ts = nowIso();
  return {
    id: newId('chr'), type: 'chore',
    title: title ?? '', assignee, dueDate, recurrence, done: !!done,
    createdAt: ts, updatedAt: ts,
  };
}

export function makeReminder({ text, remindAt, done = false }) {
  const ts = nowIso();
  return {
    id: newId('rmd'), type: 'reminder',
    text: text ?? '', remindAt: remindAt ?? '', done: !!done,
    createdAt: ts, updatedAt: ts,
  };
}

export const RECURRENCES = ['none', 'daily', 'weekly', 'monthly'];

// ---- Validation ------------------------------------------------------------
// Returns an array of human-readable error strings; empty array === valid.

export function validate(collection, obj) {
  switch (collection) {
    case 'events': return validateEvent(obj);
    case 'lists': return validateList(obj);
    case 'listItems': return validateListItem(obj);
    case 'chores': return validateChore(obj);
    case 'reminders': return validateReminder(obj);
    default: return [`Unknown collection: ${collection}`];
  }
}

function validateEvent(e) {
  const errs = [];
  if (!isNonEmptyString(e.title)) errs.push('Event needs a title.');
  if (!isIsoDate(e.start)) errs.push('Event needs a valid start date/time.');
  if (e.end != null && !isIsoDate(e.end)) errs.push('Event end must be a valid date/time.');
  if (isIsoDate(e.start) && isIsoDate(e.end) && Date.parse(e.end) < Date.parse(e.start)) {
    errs.push('Event end cannot be before its start.');
  }
  return errs;
}

function validateList(l) {
  const errs = [];
  if (!isNonEmptyString(l.name)) errs.push('List needs a name.');
  return errs;
}

function validateListItem(i) {
  const errs = [];
  if (!isNonEmptyString(i.listId)) errs.push('List item must belong to a list.');
  if (!isNonEmptyString(i.text)) errs.push('List item needs text.');
  return errs;
}

function validateChore(c) {
  const errs = [];
  if (!isNonEmptyString(c.title)) errs.push('Chore needs a title.');
  if (!RECURRENCES.includes(c.recurrence)) errs.push(`Recurrence must be one of ${RECURRENCES.join(', ')}.`);
  if (c.dueDate != null && !isIsoDate(c.dueDate)) errs.push('Chore due date must be valid.');
  return errs;
}

function validateReminder(r) {
  const errs = [];
  if (!isNonEmptyString(r.text)) errs.push('Reminder needs text.');
  if (!isIsoDate(r.remindAt)) errs.push('Reminder needs a valid date/time.');
  return errs;
}
