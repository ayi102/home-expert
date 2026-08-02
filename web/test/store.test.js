import { test } from 'node:test';
import assert from 'node:assert/strict';
import { Store, MemoryBackend, ValidationError } from '../src/store.js';
import { makeEvent, makeList, makeListItem, makeChore } from '../src/contract.js';

function freshStore() {
  return new Store(new MemoryBackend());
}

test('add + all round-trips a record', () => {
  const s = freshStore();
  s.add('events', makeEvent({ title: 'Eid', start: '2026-08-01T09:00:00.000Z' }));
  const all = s.all('events');
  assert.equal(all.length, 1);
  assert.equal(all[0].title, 'Eid');
});

test('add rejects invalid records with ValidationError', () => {
  const s = freshStore();
  assert.throws(
    () => s.add('events', makeEvent({ title: '', start: 'bad' })),
    ValidationError,
  );
  assert.equal(s.all('events').length, 0);
});

test('update merges a patch and bumps updatedAt', async () => {
  const s = freshStore();
  const e = s.add('events', makeEvent({ title: 'Old', start: '2026-08-01T09:00:00.000Z' }));
  await new Promise((r) => setTimeout(r, 2));
  const updated = s.update('events', e.id, { title: 'New' });
  assert.equal(updated.title, 'New');
  assert.notEqual(updated.updatedAt, e.updatedAt);
});

test('update on missing id throws', () => {
  const s = freshStore();
  assert.throws(() => s.update('events', 'nope', { title: 'x' }));
});

test('toggleDone flips a chore', () => {
  const s = freshStore();
  const c = s.add('chores', makeChore({ title: 'Dishes' }));
  assert.equal(c.done, false);
  assert.equal(s.toggleDone('chores', c.id).done, true);
  assert.equal(s.toggleDone('chores', c.id).done, false);
});

test('removing a list cascades to its items', () => {
  const s = freshStore();
  const list = s.add('lists', makeList({ name: 'Groceries', kind: 'shopping' }));
  s.add('listItems', makeListItem({ listId: list.id, text: 'Milk' }));
  s.add('listItems', makeListItem({ listId: list.id, text: 'Eggs' }));
  s.add('listItems', makeListItem({ listId: 'other', text: 'Keep me' }));
  assert.equal(s.all('listItems').length, 3);

  s.remove('lists', list.id);
  assert.equal(s.all('lists').length, 0);
  const remaining = s.all('listItems');
  assert.equal(remaining.length, 1);
  assert.equal(remaining[0].text, 'Keep me');
});

test('subscribe fires on mutations and unsubscribe stops it', () => {
  const s = freshStore();
  const seen = [];
  const off = s.subscribe((c) => seen.push(c));
  s.add('chores', makeChore({ title: 'A' }));
  s.add('reminders', {
    id: 'rmd_x', type: 'reminder', text: 'B', remindAt: '2026-08-02T15:00:00.000Z',
    done: false, createdAt: '2026-01-01T00:00:00.000Z', updatedAt: '2026-01-01T00:00:00.000Z',
  });
  off();
  s.add('chores', makeChore({ title: 'C' }));
  assert.deepEqual(seen, ['chores', 'reminders']);
});

test('backend isolation: mutating returned array does not corrupt store', () => {
  const s = freshStore();
  s.add('chores', makeChore({ title: 'A' }));
  const rows = s.all('chores');
  rows.push({ id: 'sneaky' });
  assert.equal(s.all('chores').length, 1);
});
