// Run from OutlawEditor: node src/main/resources/mythos/mythos-editor/js/mythos_uncompressed.test.js
'use strict';

const assert = require('node:assert/strict');

// The production file defines pure units before its Blockly-dependent editor code.
// Stub only the globals needed to load that code; no DOM or npm dependencies.
const originalRename = function (name) { return name; };
global.Blockly = {
    Procedures: { rename: originalRename },
    FieldTextInput: class {}
};
require('./mythos_uncompressed.js');
const { MythosConvergence, MythosRename, MythosSerialize } = global;
assert.equal(typeof MythosConvergence.create, 'function');
assert.equal(typeof MythosRename.makeRenameListener, 'function');
assert.equal(typeof MythosSerialize.buildScriptXml, 'function');
assert.notEqual(Blockly.Procedures.rename, originalRename, 'legacy hook remains installed');

const sameSize = MythosConvergence.decide(960, 540, 960, 540);
assert.deepEqual(sameSize, { resize: false, converged: true, keepAlive: true });
assert.deepEqual(MythosConvergence.decide(960, 540, 960, 540), sameSize);
assert.deepEqual(MythosConvergence.decide(0, 0, 0, 0),
    { resize: false, converged: false, keepAlive: true });

// Fake sampler: each [untilMs, width, height] applies until that time.
function sampler(segments) {
    return function (now) {
        for (const [until, w, h] of segments) {
            if (now < until) return { w, h };
        }
        throw new Error('sampler ran past its timeline');
    };
}

// Faithfully models the old editor.html rAF-only retry loop: when parsing at
// 0x0, it polls until size appears or gives up after 150 ticks. There was no
// timer fallback, and WebKit need not dispatch a window resize on 0x0 -> real.
function oldDriver(sample, frameMs) {
    let tries = 0;
    for (let now = 0; ; now += frameMs) {
        const size = sample(now);
        if (size.w > 0 && size.h > 0) return { size, gaveUp: false };
        if (++tries >= 150) return { size: { w: 0, h: 0 }, gaveUp: true };
    }
}

// A small fake driver representing editor.html's syncPass: one size sample,
// decision, and apply-on-change. Run at either rAF or setInterval cadence.
function newDriver(sample, endMs, pollMs) {
    const decision = MythosConvergence.create();
    const applied = [];
    let reads = 0;
    for (let now = 0; now <= endMs; now += pollMs) {
        const size = sample(now);
        reads++;
        const result = decision.decide(size.w, size.h);
        assert.equal(result.keepAlive, true, 'polling must never give up');
        if (result.resize) {
            applied.push({ now, ...size });
            decision.reportSynced(size.w, size.h, true);
        }
    }
    return { decision, applied, reads };
}

const late = sampler([[5000, 0, 0], [Infinity, 1280, 720]]);
const red = oldDriver(late, 16);
assert.equal(red.gaveUp, true);
assert.deepEqual(red.size, { w: 0, h: 0 });
console.log('RED proven: old 150-tick loop misses size arriving at 5s (remains 0x0)');

const raf = newDriver(late, 8000, 16);
assert.deepEqual([raf.decision.lastW, raf.decision.lastH], [1280, 720]);
assert.equal(raf.decision.converged, true);
assert.equal(raf.applied.length, 1);

// JavaFX/WebKit can leave rAF unscheduled: the 500ms interval must independently
// converge even when the size arrives long after the former 150-tick deadline.
const veryLate = sampler([[30000, 0, 0], [Infinity, 1000, 600]]);
const intervalOnly = newDriver(veryLate, 32000, 500);
assert.deepEqual([intervalOnly.decision.lastW, intervalOnly.decision.lastH], [1000, 600]);
assert.equal(intervalOnly.decision.converged, true);
assert.equal(intervalOnly.applied.length, 1);
console.log('GREEN: rAF and interval-only polls converge for late (5s / 30s) sizes');

const changed = sampler([[4000, 1200, 800], [Infinity, 960, 640]]);
const reconverged = newDriver(changed, 8000, 500);
assert.deepEqual(reconverged.applied.map(({ w, h }) => [w, h]), [[1200, 800], [960, 640]]);
assert.deepEqual([reconverged.decision.lastW, reconverged.decision.lastH], [960, 640]);
console.log('GREEN: later container resize causes exactly one re-sync');

const stable = sampler([[Infinity, 960, 540]]);
const settled = newDriver(stable, 10000, 16);
assert.equal(settled.applied.length, 1, 'no repeated apply/render while size is stable');
assert.ok(settled.reads > 150, 'decision keeps running beyond former deadline');
const failed = MythosConvergence.create();
assert.equal(failed.decide(500, 400).resize, true);
failed.reportSynced(500, 400, false);
assert.equal(failed.decide(500, 400).resize, true, 'a failed svgResize is retried');
console.log('GREEN: stable size incurs zero further resizes; failed sync retries');

// The actual workspace listener factory is under test, not a duplicate predicate.
const calls = [];
const editor = { setFunctionName(name) { calls.push(name); } };
const blocks = {
    A: { id: 'A', type: 'procedures_defreturn', isTopBlock: () => true },
    B: { id: 'B', type: 'procedures_defreturn', isTopBlock: () => true },
    noReturn: { id: 'noReturn', type: 'procedures_defnoreturn', isTopBlock: () => true },
    caller: { type: 'procedures_callreturn', isTopBlock: () => true },
    arg: { type: 'procedures_mutatorarg', isTopBlock: () => false },
    nestedDef: { type: 'procedures_defreturn', isTopBlock: () => false }
};
const workspace = {
    getBlockById(id) { return blocks[id] || null; },
    addChangeListener(listener) { this.listener = listener; }
};
const mythos = { workspace, editor, rootBlockId: null };
workspace.addChangeListener(MythosRename.makeRenameListener(() => mythos, 'block_change'));
const change = (blockId, name, newValue) => ({
    type: 'block_change', element: 'field', name, blockId, newValue
});
mythos.rootBlockId = 'A';
workspace.listener(change('B', 'NAME', 'Second Function'));
assert.deepEqual(calls, [], 'renaming another top-level def must not rename the script');
mythos.rootBlockId = null;
workspace.listener(change('A', 'NAME', 'Before Import'));
assert.deepEqual(calls, [], 'no root identified yet must not rename the script');
mythos.rootBlockId = 'A';
workspace.listener(change('A', 'NAME', 'New Function'));
workspace.listener(change('caller', 'NAME', 'Caller Name'));
workspace.listener(change('arg', 'NAME', 'Argument Name'));
workspace.listener(change('A', 'PARAMS', 'arg'));
workspace.listener({ type: 'block_move', blockId: 'A' });
workspace.listener({ ...change('A', 'NAME', 'Move With Field'), type: 'block_move' });
workspace.listener(change('missing', 'NAME', 'Deleted'));
workspace.listener(change('nestedDef', 'NAME', 'Nested'));
workspace.listener(change('noReturn', 'NAME', 'Second Function'));
assert.deepEqual(calls, ['New Function'], 'another top-level def is not the script root');
delete blocks.A;
workspace.listener(change('B', 'NAME', 'After Root Deletion'));
assert.deepEqual(calls, ['New Function'], 'deleting root does not promote another def');
mythos.rootBlockId = 'noReturn';
workspace.listener(change('noReturn', 'NAME', 'New Imported Root'));
assert.deepEqual(calls, ['New Function', 'New Imported Root']);
mythos.editor = null;
assert.doesNotThrow(() => workspace.listener(change('noReturn', 'NAME', 'Bridge Not Ready')));
assert.deepEqual(calls, ['New Function', 'New Imported Root']);
console.log('GREEN: rename listener notifies only the imported root NAME commit; null bridge safe');

// Exercise the real import wiring, not just the predicate: extra top blocks
// are disposed before the surviving root id is stored, and reload clears it.
const imported = global.Mythos;
const importedBlocks = [];
const disposed = [];
const importWorkspace = {
    clear() {
        assert.equal(imported.rootBlockId, null, 'previous root is cleared before import events');
        importedBlocks.length = 0;
    },
    getTopBlocks() { return importedBlocks.slice(); },
    getBlockById(id) { return importedBlocks.find(block => block.id === id) || null; }
};
global.DOMParser = class { parseFromString() { return { documentElement: {} }; } };
Blockly.Xml = {
    domToWorkspace() {
        importedBlocks.push(
            { id: 'importedRoot', type: 'procedures_defreturn', isTopBlock: () => true },
            { id: 'extra', type: 'procedures_defreturn', isTopBlock: () => true,
                dispose() { disposed.push(this.id); importedBlocks.pop(); } }
        );
    }
};
imported.workspace = importWorkspace;
imported.addCustomVariables = () => {};
imported.rootBlockId = 'staleRoot';
imported.setScriptXml('<xml/>');
assert.deepEqual(disposed, ['extra']);
assert.equal(imported.rootBlockId, 'importedRoot');
importedBlocks.push({ id: 'newDef', type: 'procedures_defreturn', isTopBlock: () => true });
const importCalls = [];
imported.editor = { setFunctionName(name) { importCalls.push(name); } };
const importedListener = MythosRename.makeRenameListener(() => imported, 'block_change');
importedListener(change('newDef', 'NAME', 'Wrong Script'));
importedListener(change('importedRoot', 'NAME', 'Correct Script'));
assert.deepEqual(importCalls, ['Correct Script']);
importedBlocks.shift(); // deleting the root does not promote the later definition
importedListener(change('newDef', 'NAME', 'Still Wrong Script'));
assert.deepEqual(importCalls, ['Correct Script']);
console.log('GREEN: setScriptXml captures the surviving root; deleting it never promotes extras');

// The legacy namespace wrapper is dead on Blockly v12's field commit path,
// but it must also fail closed if anything invokes it directly in the future.
const originalLegacyBlock = { id: 'importedRoot' };
const otherLegacyBlock = { id: 'newDef' };
assert.equal(Blockly.Procedures.rename.call({ getSourceBlock: () => otherLegacyBlock }, 'Other'), 'Other');
assert.deepEqual(importCalls, ['Correct Script']);
assert.equal(Blockly.Procedures.rename.call({ getSourceBlock: () => originalLegacyBlock }, 'Root'), 'Root');
assert.deepEqual(importCalls, ['Correct Script', 'Root']);
imported.rootBlockId = null;
Blockly.Procedures.rename.call({ getSourceBlock: () => originalLegacyBlock }, 'Unset');
assert.deepEqual(importCalls, ['Correct Script', 'Root']);
console.log('GREEN: legacy rename wrapper also notifies only for the identified root');

// A tiny DOM-shaped serializer fixture: the legacy path's innerHTML is used
// verbatim; filtered paths clone the existing serialized children, never
// regenerate individual Blockly blocks.
function serializedChild(tag, id, markup) {
    return {
        nodeType: 1, nodeName: tag, markup,
        getAttribute(name) { return name === 'id' ? id : null; },
        cloneNode() { return serializedChild(tag, id, markup); }
    };
}
function serializedXml(children) {
    return {
        nodeType: 1, nodeName: 'xml', childNodes: children,
        get innerHTML() { return this.childNodes.map(child => child.markup).join(''); },
        cloneNode(deep) { return serializedXml(deep ? children.map(child => child.cloneNode(true)) : []); },
        appendChild(child) { this.childNodes.push(child); return child; }
    };
}
const vars = serializedChild('variables', null, '<variables><variable id="v">score</variable></variables>');
const root = serializedChild('block', 'A', '<block id="A"><field name="NAME">Hero</field></block>');
const second = serializedChild('block', 'B', '<block id="B"><field name="NAME">Other</field></block>');
const third = serializedChild('block', 'C', '<block id="C"/>');
const originalXml = serializedXml([vars, root, second]);
const filteredUnit = MythosSerialize.buildScriptXml(originalXml, 'A');
assert.notStrictEqual(filteredUnit.element, originalXml, 'filter must not mutate serializer output');
assert.equal(filteredUnit.element.innerHTML, vars.markup + root.markup);
assert.equal(originalXml.innerHTML, vars.markup + root.markup + second.markup);
assert.deepEqual(filteredUnit.report, {
    extraCount: 1, topBlockCount: 2, rootFound: true, legacy: false, warning: true
});
const singleOriginalXml = serializedXml([vars, root]);
const legacyUnit = MythosSerialize.buildScriptXml(singleOriginalXml, 'A');
assert.strictEqual(legacyUnit.element, singleOriginalXml, 'single root returns original DOM element');
assert.deepEqual(legacyUnit.report, {
    extraCount: 0, topBlockCount: 1, rootFound: true, legacy: true, warning: false
});
const missingUnit = MythosSerialize.buildScriptXml(serializedXml([vars, second]), 'A');
assert.equal(missingUnit.element.innerHTML, vars.markup);
assert.deepEqual(missingUnit.report, {
    extraCount: 1, topBlockCount: 1, rootFound: false, legacy: false, warning: true
});
let serialized, serializerCalls = 0;
Blockly.Xml.workspaceToDom = () => { serializerCalls++; return serialized; };
imported.workspace = {
    getBlockById(id) {
        return id === 'A' ? { getFieldValue(name) { return name === 'NAME' ? 'Hero' : null; } } : null;
    },
    getTopBlocks() { throw new Error('serialization should use the existing DOM, not requery Blockly'); }
};
const warnings = [], loggedWarnings = [];
imported.editor = {
    alertWarning(message) { warnings.push(message); },
    log(message) { loggedWarnings.push(message); }
};
imported.rootBlockId = 'A';
serialized = serializedXml([vars, root]);
const legacySingle = serialized.innerHTML;
assert.equal(imported.getScriptXml(), legacySingle, 'single root must be byte-identical');
assert.equal(serializerCalls, 1);
assert.deepEqual(warnings, []);

serialized = serializedXml([vars, root, second]);
const originalWithExtra = serialized.innerHTML;
assert.match(originalWithExtra, /<block id="B">/, 'the old serializer includes the second top block');
assert.equal(imported.getScriptXml(), vars.markup + root.markup, 'Apply must serialize only root');
assert.equal(serialized.innerHTML, originalWithExtra, 'filter must not mutate source DOM');
assert.equal(warnings.length, 1, 'one user-visible warning per multi-block Apply');
assert.match(warnings[0], /Hero.*2 top-level blocks.*1 extra/);
console.log('GREEN: Apply retains only the imported root and warns once');

serialized = serializedXml([vars, second, third]);
assert.equal(imported.getScriptXml(), vars.markup, 'deleted root must not serialize another def');
assert.equal(warnings.length, 2);
assert.match(warnings[1], /root block is missing; no changes were saved/);
serialized = serializedXml([second]);
assert.equal(imported.getScriptXml(), '', 'even one non-root block cannot replace a deleted root');
assert.equal(warnings.length, 3);

imported.rootBlockId = null;
serialized = serializedXml([root]);
assert.equal(imported.getScriptXml(), serialized.innerHTML, 'one block before import keeps legacy output');
assert.equal(warnings.length, 3);
serialized = serializedXml([vars, root, second]);
assert.equal(imported.getScriptXml(), vars.markup, 'multiple blocks without known root fail closed');
assert.equal(warnings.length, 4);
assert.deepEqual(loggedWarnings, []);
console.log('GREEN: missing/unset root never promotes extra blocks; single-block output unchanged');

delete imported.editor.alertWarning;
imported.rootBlockId = 'A';
serialized = serializedXml([root, second]);
assert.equal(imported.getScriptXml(), root.markup);
assert.equal(loggedWarnings.length, 1, 'older Java bridge falls back to one log');
console.log('GREEN: older Java bridge logs the warning when alertWarning is absent');
