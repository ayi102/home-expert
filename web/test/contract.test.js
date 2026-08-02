import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  makeEvent, makeList, makeListItem, makeChore, makeReminder, validate, newId,
} from '../src/contract.js';

test('newId is unique and prefixed', () => {
  const a = newId('evt');
  const b = newId('evt');
  assert.match(a, /^evt_/);
  assert.notEqual(a, b);
});

test('valid event passes validation', () => {
  const e = makeEvent({ title: 'Dentist', start: '2026-08-01T09:00:00.000Z' });
  assert.deepEqual(validate('events', e), []);
  assert.equal(e.type, 'event');
  assert.ok(e.createdAt && e.updatedAt);
});

test('event without title or start is rejected', () => {
  const e = makeEvent({ title: '', start: 'not-a-date' });
  const errs = validate('events', e);
  assert.equal(errs.length, 2);
});

test('event end before start is rejected', () => {
  const e = makeEvent({
    title: 'Backwards', start: '2026-08-01T10:00:00.000Z', end: '2026-08-01T09:00:00.000Z',
  });
  assert.ok(validate('events', e).some((m) => /end cannot be before/.test(m)));
});

test('list item must reference a list and have text', () => {
  assert.equal(validate('listItems', makeListItem({ listId: '', text: '' })).length, 2);
  assert.deepEqual(validate('listItems', makeListItem({ listId: 'lst_1', text: 'Milk' })), []);
});

test('chore recurrence is constrained', () => {
  const bad = makeChore({ title: 'Trash', recurrence: 'hourly' });
  assert.ok(validate('chores', bad).some((m) => /Recurrence must be/.test(m)));
  const good = makeChore({ title: 'Trash', recurrence: 'weekly' });
  assert.deepEqual(validate('chores', good), []);
});

test('reminder needs text and a valid time', () => {
  assert.equal(validate('reminders', makeReminder({ text: '', remindAt: 'x' })).length, 2);
  assert.deepEqual(
    validate('reminders', makeReminder({ text: 'Call plumber', remindAt: '2026-08-02T15:00:00.000Z' })),
    [],
  );
});

test('unknown collection reports an error', () => {
  assert.equal(validate('nope', {}).length, 1);
});

test('list factory defaults kind to custom', () => {
  assert.equal(makeList({ name: 'Groceries' }).kind, 'custom');
});
