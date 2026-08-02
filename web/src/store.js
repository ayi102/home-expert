// Store: a small realtime-ish data layer with a pluggable backend.
//
// Today it ships a MemoryBackend (tests) and a BrowserBackend (localStorage) so
// the whole companion app WORKS and is testable before any cloud exists. When
// we wire Firebase, add a FirestoreBackend implementing the same 3 methods and
// swap it in — no UI changes. The Android app will read the same collections.

import { COLLECTIONS, validate } from './contract.js';

export class MemoryBackend {
  constructor() { this.data = Object.fromEntries(COLLECTIONS.map((c) => [c, []])); }
  read(collection) { return structuredClone(this.data[collection] ?? []); }
  write(collection, rows) { this.data[collection] = structuredClone(rows); }
}

export class BrowserBackend {
  constructor(storage = globalThis.localStorage, keyPrefix = 'hd:') {
    this.storage = storage;
    this.keyPrefix = keyPrefix;
  }
  read(collection) {
    const raw = this.storage.getItem(this.keyPrefix + collection);
    return raw ? JSON.parse(raw) : [];
  }
  write(collection, rows) {
    this.storage.setItem(this.keyPrefix + collection, JSON.stringify(rows));
  }
}

export class ValidationError extends Error {
  constructor(errors) {
    super(errors.join(' '));
    this.name = 'ValidationError';
    this.errors = errors;
  }
}

export class Store {
  constructor(backend = new MemoryBackend()) {
    this.backend = backend;
    this.listeners = new Set();
  }

  /** Subscribe to any change; returns an unsubscribe fn. Mimics realtime sync. */
  subscribe(fn) {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  _emit(collection) {
    for (const fn of this.listeners) fn(collection);
  }

  all(collection) {
    assertCollection(collection);
    return this.backend.read(collection);
  }

  get(collection, id) {
    return this.all(collection).find((r) => r.id === id) ?? null;
  }

  /** Insert a fully-formed record (from a contract factory). Throws if invalid. */
  add(collection, record) {
    assertCollection(collection);
    const errs = validate(collection, record);
    if (errs.length) throw new ValidationError(errs);
    const rows = this.backend.read(collection);
    rows.push(record);
    this.backend.write(collection, rows);
    this._emit(collection);
    return record;
  }

  /** Merge a patch into an existing record; bumps updatedAt. Throws if invalid. */
  update(collection, id, patch) {
    assertCollection(collection);
    const rows = this.backend.read(collection);
    const idx = rows.findIndex((r) => r.id === id);
    if (idx === -1) throw new Error(`No ${collection} record with id ${id}`);
    const updated = { ...rows[idx], ...patch, id, updatedAt: new Date().toISOString() };
    const errs = validate(collection, updated);
    if (errs.length) throw new ValidationError(errs);
    rows[idx] = updated;
    this.backend.write(collection, rows);
    this._emit(collection);
    return updated;
  }

  remove(collection, id) {
    assertCollection(collection);
    const rows = this.backend.read(collection).filter((r) => r.id !== id);
    this.backend.write(collection, rows);
    // Cascade: deleting a list removes its items.
    if (collection === 'lists') {
      const items = this.backend.read('listItems').filter((i) => i.listId !== id);
      this.backend.write('listItems', items);
      this._emit('listItems');
    }
    this._emit(collection);
  }

  toggleDone(collection, id) {
    const rec = this.get(collection, id);
    if (!rec) throw new Error(`No ${collection} record with id ${id}`);
    return this.update(collection, id, { done: !rec.done });
  }
}

function assertCollection(c) {
  if (!COLLECTIONS.includes(c)) throw new Error(`Unknown collection: ${c}`);
}
