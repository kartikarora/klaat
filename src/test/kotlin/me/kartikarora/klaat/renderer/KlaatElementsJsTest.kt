package me.kartikarora.klaat.renderer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KlaatElementsJsTest {

    companion object {
        @JvmStatic
        fun isNodeAvailable(): Boolean {
            return try {
                val process = ProcessBuilder("node", "--version").start()
                process.waitFor() == 0
            } catch (_: Exception) {
                false
            }
        }
    }

    @Test
    @EnabledIf("isNodeAvailable")
    fun `test klaat-elements js metadata API`() {
        val testScript = """
            let definedClasses = {};
            class MockStorage {
              constructor() { this.store = {}; }
              getItem(k) { return this.store[k] || null; }
              setItem(k, v) { this.store[k] = String(v); }
            }

            class MockElement {
              constructor(tag) {
                this.tagName = (tag || 'DIV').toUpperCase();
                this.attrs = {};
                this.children = [];
                this.innerHTML = '';
                this.style = {};
              }
              getAttribute(n) { return this.attrs[n] || null; }
              setAttribute(n, v) { this.attrs[n] = String(v); }
              removeAttribute(n) { delete this.attrs[n]; }
              hasAttribute(n) { return n in this.attrs; }
              querySelectorAll(sel) {
                if (sel === 'klaat-kodelab-step') return this.children.filter(c => c.tagName === 'KLAAT-KODELAB-STEP');
                return [];
              }
              querySelector(sel) { return null; }
              appendChild(c) { this.children.push(c); return c; }
              dispatchEvent() {}
            }

            const sandbox = {
              window: { localStorage: new MockStorage() },
              location: { hash: '', pathname: '/test' },
              history: { replaceState: () => {} },
              CustomEvent: class { constructor(n, opt) { this.name = n; this.detail = opt?.detail; } },
              document: {
                body: {
                  addEventListener: () => {},
                  dispatchEvent: () => {},
                  removeAttribute: () => {}
                }
              },
              HTMLElement: MockElement,
              customElements: {
                get: (n) => definedClasses[n] || null,
                define: (n, cls) => { definedClasses[n] = cls; }
              },
              console: console,
              parseInt: parseInt,
              isNaN: isNaN,
              Math: Math,
              Array: Array,
              String: String
            };

            const fs = require('fs');
            const vm = require('vm');
            const assert = require('assert');
            const code = fs.readFileSync('src/main/resources/assets/klaat-elements.js', 'utf8');
            const context = vm.createContext(sandbox);
            vm.runInContext(code, context);

            const KlaatKodelab = definedClasses['klaat-kodelab'];
            const KlaatKodelabStep = definedClasses['klaat-kodelab-step'];
            const codelab = new KlaatKodelab();

            // 1. Initial / empty state
            assert.strictEqual(codelab.totalSteps, 0);
            assert.strictEqual(codelab.getTotalSteps(), 0);
            assert.strictEqual(codelab.totalDuration, 0);
            assert.strictEqual(codelab.getTotalDuration(), 0);
            assert.strictEqual(codelab.currentStep, 0);
            assert.strictEqual(codelab.getCurrentStep(), 0);
            assert.strictEqual(codelab.progressPercent, 0);
            assert.strictEqual(codelab.getProgressPercent(), 0);
            assert.strictEqual(codelab.remainingTime, 0);
            assert.strictEqual(codelab.getRemainingTime(), 0);

            // 2. Add multiple steps with durations and excluded duration
            const s1 = new KlaatKodelabStep(); s1.setAttribute('duration', '5');
            const s2 = new KlaatKodelabStep(); s2.setAttribute('duration', '10');
            const s3 = new KlaatKodelabStep(); s3.setAttribute('duration', '15');
            const s4 = new KlaatKodelabStep(); s4.setAttribute('duration', '20'); s4.setAttribute('exclude-duration', '');

            codelab._steps = [s1, s2, s3, s4];

            // At step 0
            codelab._currentStep = 0;
            assert.strictEqual(codelab.totalSteps, 4);
            assert.strictEqual(codelab.getTotalSteps(), 4);
            assert.strictEqual(codelab.totalDuration, 30);
            assert.strictEqual(codelab.getTotalDuration(), 30);
            assert.strictEqual(codelab.currentStep, 0);
            assert.strictEqual(codelab.getCurrentStep(), 0);
            assert.strictEqual(codelab.progressPercent, 0);
            assert.strictEqual(codelab.getProgressPercent(), 0);
            assert.strictEqual(codelab.remainingTime, 30);
            assert.strictEqual(codelab.getRemainingTime(), 30);

            // At step 1
            codelab._currentStep = 1;
            assert.strictEqual(codelab.currentStep, 1);
            assert.strictEqual(codelab.getCurrentStep(), 1);
            assert.strictEqual(codelab.progressPercent, 25);
            assert.strictEqual(codelab.getProgressPercent(), 25);
            assert.strictEqual(codelab.remainingTime, 25);
            assert.strictEqual(codelab.getRemainingTime(), 25);

            // At step 2
            codelab._currentStep = 2;
            assert.strictEqual(codelab.currentStep, 2);
            assert.strictEqual(codelab.getCurrentStep(), 2);
            assert.strictEqual(codelab.progressPercent, 50);
            assert.strictEqual(codelab.getProgressPercent(), 50);
            assert.strictEqual(codelab.remainingTime, 15);
            assert.strictEqual(codelab.getRemainingTime(), 15);

            // At step 3 (last step, duration excluded)
            codelab._currentStep = 3;
            assert.strictEqual(codelab.currentStep, 3);
            assert.strictEqual(codelab.getCurrentStep(), 3);
            assert.strictEqual(codelab.progressPercent, 75);
            assert.strictEqual(codelab.getProgressPercent(), 75);
            assert.strictEqual(codelab.remainingTime, 0);
            assert.strictEqual(codelab.getRemainingTime(), 0);

            console.log('SUCCESS');
        """.trimIndent()

        val process = ProcessBuilder("node", "-e", testScript).start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        assertEquals(0, exitCode, "Node execution failed with error: $stderr")
        assertTrue(stdout.contains("SUCCESS"), "Expected output to contain SUCCESS, got: $stdout")
    }
}
