package me.kartikarora.klaat.renderer

import me.kartikarora.klaat.core.Codelab
import me.kartikarora.klaat.core.Node
import me.kartikarora.klaat.core.Step
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
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
    fun `test inline codelab runtime metadata API`(@TempDir tempDir: Path) {
        val codelab = Codelab(
            id = "runtime-test",
            title = "Runtime Test",
            steps = listOf(Step("Intro", listOf(Node.Text("Hello")), duration = 5))
        )
        HtmlRenderer().render(codelab, tempDir.toFile())
        val html = File(tempDir.toFile(), "runtime-test/index.html").readText()

        // Extract the inline runtime <script> block
        val scriptMatch = Regex("""<script>\s*([\s\S]*?)</script>""").find(html)
        assertTrue(scriptMatch != null, "Rendered HTML should contain inline runtime script")
        val runtimeScript = scriptMatch.groupValues[1]
        val scriptFile = File(tempDir.toFile(), "runtime.js")
        scriptFile.writeText(runtimeScript)

        val testScript = """
            const fs = require('fs');
            const vm = require('vm');
            const assert = require('assert');

            class MockStorage {
              constructor() { this.store = {}; }
              getItem(k) { return this.store[k] || null; }
              setItem(k, v) { this.store[k] = String(v); }
            }

            class MockElement {
              constructor(attrs = {}) {
                this.attrs = { ...attrs };
                this.style = {};
                this.classList = { add() {}, remove() {}, toggle() {}, contains() { return false; } };
              }
              getAttribute(n) { return n in this.attrs ? this.attrs[n] : null; }
              setAttribute(n, v) { this.attrs[n] = String(v); }
              removeAttribute(n) { delete this.attrs[n]; }
              hasAttribute(n) { return n in this.attrs; }
              addEventListener() {}
              querySelector() { return null; }
              querySelectorAll() { return []; }
            }

            const mockWindow = {
              localStorage: new MockStorage(),
              addEventListener: () => {},
              history: { length: 1, back: () => {}, replaceState: () => {} },
              location: { hash: '', pathname: '/test', origin: 'http://localhost' },
              scrollTo: () => {}
            };

            const sandbox = {
              window: mockWindow,
              location: mockWindow.location,
              history: mockWindow.history,
              document: {
                body: new MockElement(),
                referrer: '',
                getElementById: () => null,
                querySelectorAll: () => [],
                addEventListener: () => {}
              },
              console: console,
              parseInt: parseInt,
              isNaN: isNaN,
              Math: Math,
              Array: Array,
              String: String,
              setTimeout: setTimeout
            };

            const code = fs.readFileSync(${"\""}${scriptFile.absolutePath.replace("\\", "\\\\")}${"\""}, 'utf8');
            const context = vm.createContext(sandbox);
            vm.runInContext(code, context);

            const createController = mockWindow.createKlaatCodelabController;
            assert.strictEqual(typeof createController, 'function', 'createKlaatCodelabController should be exported');

            // 1. Initial / empty state
            const empty = createController([]);
            assert.strictEqual(empty.totalSteps, 0);
            assert.strictEqual(empty.getTotalSteps(), 0);
            assert.strictEqual(empty.totalDuration, 0);
            assert.strictEqual(empty.getTotalDuration(), 0);
            assert.strictEqual(empty.currentStep, 0);
            assert.strictEqual(empty.getCurrentStep(), 0);
            assert.strictEqual(empty.progressPercent, 0);
            assert.strictEqual(empty.getProgressPercent(), 0);
            assert.strictEqual(empty.remainingTime, 0);
            assert.strictEqual(empty.getRemainingTime(), 0);

            // 2. Multiple steps with durations and excluded duration
            const s1 = new MockElement({ 'data-duration': '5', 'data-label': 'Step 1' });
            const s2 = new MockElement({ 'data-duration': '10', 'data-label': 'Step 2' });
            const s3 = new MockElement({ 'data-duration': '15', 'data-label': 'Step 3' });
            const s4 = new MockElement({ 'data-duration': '20', 'data-exclude-duration': 'true', 'data-label': 'Step 4' });

            const ctrl = createController([s1, s2, s3, s4]);

            // At step 0
            ctrl.selectStep(0, true);
            assert.strictEqual(ctrl.totalSteps, 4);
            assert.strictEqual(ctrl.getTotalSteps(), 4);
            assert.strictEqual(ctrl.totalDuration, 30);
            assert.strictEqual(ctrl.getTotalDuration(), 30);
            assert.strictEqual(ctrl.currentStep, 0);
            assert.strictEqual(ctrl.progressPercent, 0);
            assert.strictEqual(ctrl.remainingTime, 30);

            // At step 1
            ctrl.selectStep(1);
            assert.strictEqual(ctrl.currentStep, 1);
            assert.strictEqual(ctrl.progressPercent, 25);
            assert.strictEqual(ctrl.remainingTime, 25);

            // At step 2
            ctrl.selectStep(2);
            assert.strictEqual(ctrl.currentStep, 2);
            assert.strictEqual(ctrl.progressPercent, 50);
            assert.strictEqual(ctrl.remainingTime, 15);

            // At step 3 (duration excluded)
            ctrl.selectStep(3);
            assert.strictEqual(ctrl.currentStep, 3);
            assert.strictEqual(ctrl.progressPercent, 75);
            assert.strictEqual(ctrl.remainingTime, 0);

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
