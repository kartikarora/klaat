/**
 * klaat-elements.js
 * Custom element implementations for the <klaat-kodelab-*> component suite.
 * Replaces Google's codelab-elements.js with a clean, brand-native implementation.
 * Requires no polyfills — targets modern browsers only (ES2020+, Web Components v1).
 */

'use strict';

/* --- Utilities ------------------------------------------------------------- */

/** Dispatch a CustomEvent on target and bubble to document.body */
function dispatchKlaatEvent(target, name, detail = {}) {
  const event = new CustomEvent(name, { detail, bubbles: true });
  target.dispatchEvent(event);
  // Also dispatch on body for analytics listeners
  if (target !== document.body) {
    document.body.dispatchEvent(new CustomEvent(name, { detail, bubbles: true }));
  }
}

/* --- KlaatKodelabAnalytics ------------------------------------------------ */

class KlaatKodelabAnalytics extends HTMLElement {
  static get observedAttributes() {
    return ['gaid', 'codelab-gaid', 'environment', 'category'];
  }

  connectedCallback() {
    this._initGA();
    this._bindEvents();
  }

  attributeChangedCallback() {
    this._initGA();
  }

  _getGaid() {
    return this.getAttribute('codelab-gaid') || this.getAttribute('gaid') || '';
  }

  _initGA() {
    const gaid = this._getGaid();
    if (!gaid) return;

    // Universal Analytics (ga)
    if (typeof window.ga === 'function') {
      window.ga('create', gaid, 'auto');
    }
  }

  _bindEvents() {
    const env = this.getAttribute('environment') || '';
    const cat = this.getAttribute('category') || '';

    document.body.addEventListener('klaat-kodelab-action', (e) => {
      const { category, action, label, value } = e.detail || {};
      if (typeof window.ga === 'function') {
        window.ga('send', 'event', category, action || '', label || '', value || 0, {
          dimension1: env,
          dimension2: cat,
        });
      }
      if (typeof window.gtag === 'function') {
        window.gtag('event', action || 'codelab_action', {
          event_category: category,
          event_label: label || '',
          value: value || 0,
        });
      }
    });

    document.body.addEventListener('klaat-kodelab-pageview', (e) => {
      const { page, title } = e.detail || {};
      if (typeof window.ga === 'function') {
        window.ga('send', 'pageview', { page: page || '', title: title || '', dimension1: env, dimension2: cat });
      }
      if (typeof window.gtag === 'function') {
        window.gtag('event', 'page_view', { page_title: title || '', page_location: page || '' });
      }
    });
  }
}

/* --- KlaatKodelabStep ----------------------------------------------------- */

class KlaatKodelabStep extends HTMLElement {
  static get observedAttributes() {
    return ['label', 'step', 'duration'];
  }

  get label() {
    return this.getAttribute('label') || '';
  }

  get duration() {
    const d = parseInt(this.getAttribute('duration'), 10);
    return isNaN(d) ? 0 : d;
  }
}

/* --- KlaatKodelab (main component) --------------------------------------- */

class KlaatKodelab extends HTMLElement {
  static get observedAttributes() {
    return [
      'title', 'codelab-title', 'environment', 'category',
      'feedback-link', 'selected', 'no-toolbar', 'no-arrows',
    ];
  }

  constructor() {
    super();
    this._steps = [];
    this._currentStep = -1;
    this._initialized = false;
    this._storage = (() => {
      try { return window.localStorage; } catch (e) { return null; }
    })();
  }

  connectedCallback() {
    if (!this._initialized) {
      this._init();
    }
    this._syncAnalytics();
    this._initialized = true;
  }

  attributeChangedCallback(name) {
    if (!this._initialized) return;
    switch (name) {
      case 'title':
        if (this.hasAttribute('title')) {
          const t = this.getAttribute('title');
          this.removeAttribute('title');
          this.setAttribute('codelab-title', t);
        }
        break;
      case 'codelab-title':
        this._updateTitle();
        break;
      case 'selected':
        this._onSelectedAttrChanged();
        break;
    }
  }

  // -- Initialisation --------------------------------------------------------

  _init() {
    // Collect step and footer data BEFORE modifying DOM
    const rawSteps = Array.from(this.querySelectorAll('klaat-kodelab-step'));
    const stepData = rawSteps.map(s => ({
      label: s.getAttribute('label') || '',
      duration: parseInt(s.getAttribute('duration'), 10) || 0,
      innerHTML: s.innerHTML
    }));

    const footerSlot = this.querySelector('[slot="footer"], .kodelab-footer, footer');
    const footerHtml = footerSlot ? footerSlot.innerHTML.trim() : (this.getAttribute('footer') || '');

    // Normalise title attribute
    if (this.hasAttribute('title')) {
      this.setAttribute('codelab-title', this.getAttribute('title'));
      this.removeAttribute('title');
    }

    const title = this.getAttribute('codelab-title') || '';
    const feedbackLink = this.getAttribute('feedback-link') || '';
    const homeUrl = this.getAttribute('home-url') || '../';
    const totalMins = stepData.reduce((sum, s) => sum + s.duration, 0);

    // Inject layout skeleton
    this.innerHTML = `
      <div id="kodelab-title">
        <div id="kodelab-nav-buttons">
          <a id="arrow-back" href="${this._escape(homeUrl)}" title="Back">
            <span class="material-symbols-outlined">arrow_back</span>
          </a>
          <a id="menu" title="Toggle navigation">
            <span class="material-symbols-outlined">menu</span>
          </a>
        </div>
        <h1>${this._escape(title)}</h1>
        <div id="time-remaining"${totalMins > 0 ? '' : ' style="display: none;"'}>
          <span class="material-symbols-outlined">schedule</span>
          <span id="time-remaining-label">${totalMins > 0 ? totalMins + ' min remaining' : ''}</span>
        </div>
      </div>
      <div id="drawer">
        <div class="steps"><ol></ol></div>
        <div class="metadata">
          ${totalMins > 0 ? '<span id="total-time">' + totalMins + ' min total</span>' : ''}
          ${feedbackLink ? '<a id="feedback-link" href="' + this._escape(feedbackLink) + '" target="_blank">Report a mistake</a>' : ''}
          ${footerHtml ? '<div class="kodelab-footer">' + footerHtml + '</div>' : ''}
        </div>
      </div>
      <div id="main">
        <div id="steps"></div>
        <div id="controls">
          <div id="fabs">
            <a id="previous-step" href="#">Back</a>
            <div class="spacer"></div>
            <a id="next-step" href="#">Next</a>
            <a id="done" href="#" hidden>Done</a>
          </div>
        </div>
      </div>
    `;

    // Cache refs
    this._drawerOl = this.querySelector('#drawer ol');
    this._stepsContainer = this.querySelector('#steps');
    this._prevBtn = this.querySelector('#previous-step');
    this._nextBtn = this.querySelector('#next-step');
    this._doneBtn = this.querySelector('#done');
    this._titleEl = this.querySelector('#kodelab-title h1');
    this._timeRemainingLabel = this.querySelector('#time-remaining-label');

    // Reconstruct steps and drawer items
    this._steps = [];
    stepData.forEach((data, i) => {
      const stepEl = document.createElement('klaat-kodelab-step');
      stepEl.setAttribute('label', data.label);
      if (data.duration > 0) stepEl.setAttribute('duration', String(data.duration));
      stepEl.setAttribute('step', String(i + 1));

      const instructions = document.createElement('div');
      instructions.className = 'instructions';
      const inner = document.createElement('div');
      inner.className = 'inner';
      inner.innerHTML = `<h2 class="step-title">${i + 1}. ${this._escape(data.label)}</h2>` + data.innerHTML;
      instructions.appendChild(inner);
      stepEl.appendChild(instructions);

      this._stepsContainer.appendChild(stepEl);
      this._steps.push(stepEl);

      // Drawer item
      const li = document.createElement('li');
      li.innerHTML = `<a href="#${i}"><div class="step">${this._escape(data.label)}</div></a>`;
      li.querySelector('a').addEventListener('click', (e) => {
        e.preventDefault();
        this.selectStep(i);
      });
      this._drawerOl.appendChild(li);
    });

    // Bind events
    this._bindEvents();

    // Restore progress or navigate via hash
    const id = this.getAttribute('id') || '';
    const savedStep = this._storage ? this._storage.getItem('progress_' + id) : null;
    const hashStep = location.hash ? parseInt(location.hash.substring(1), 10) : NaN;

    let startStep = 0;
    if (!isNaN(hashStep) && hashStep > 0) {
      startStep = hashStep;
    } else if (savedStep && savedStep !== '0') {
      startStep = parseInt(savedStep, 10) || 0;
    }

    this.selectStep(startStep, true);

    // Fire ready event
    dispatchKlaatEvent(this, 'klaat-kodelab-ready');
    this.setAttribute('klaat-kodelab-ready', '');
    document.body.removeAttribute('unresolved');
  }

  // -- Events ----------------------------------------------------------------

  _bindEvents() {
    this.querySelector('#arrow-back')?.addEventListener('click', (e) => {
      if (!this.hasAttribute('home-url') && window.history.length > 1 && document.referrer) {
        try {
          const refUrl = new URL(document.referrer);
          if (refUrl.origin === location.origin && refUrl.pathname !== location.pathname) {
            e.preventDefault();
            window.history.back();
          }
        } catch (_) {}
      }
    });

    this.querySelector('#menu')?.addEventListener('click', () => {
      this.toggleAttribute('drawer--open');
    });

    this.querySelector('#main')?.addEventListener('click', () => {
      if (this.hasAttribute('drawer--open')) {
        this.removeAttribute('drawer--open');
      }
    });

    this._prevBtn?.addEventListener('click', (e) => {
      e.preventDefault();
      this.selectStep(this._currentStep - 1);
    });

    this._nextBtn?.addEventListener('click', (e) => {
      e.preventDefault();
      this.selectStep(this._currentStep + 1);
    });

    this._doneBtn?.addEventListener('click', (e) => {
      e.preventDefault();
      dispatchKlaatEvent(this, 'klaat-kodelab-action', {
        category: 'codelab',
        action: 'complete',
        label: this.getAttribute('codelab-title') || '',
      });
    });

    window.addEventListener('hashchange', () => {
      const hash = parseInt(location.hash.substring(1), 10);
      if (!isNaN(hash)) this.selectStep(hash);
    });
  }

  // -- Step navigation -------------------------------------------------------

  selectStep(index, skipAnimation = false) {
    if (this._steps.length === 0) return;
    index = Math.max(0, Math.min(index, this._steps.length - 1));
    if (isNaN(index)) index = 0;

    const prev = this._currentStep;
    this._currentStep = index;

    // Update step visibility cleanly (show only selected step, reset scroll)
    this._steps.forEach((step, i) => {
      if (i === index) {
        step.setAttribute('selected', '');
        step.scrollTop = 0;
      } else {
        step.removeAttribute('selected');
        step.removeAttribute('animating');
      }
    });

    // Update drawer items
    const liItems = this._drawerOl?.querySelectorAll('li') || [];
    liItems.forEach((li, i) => {
      if (i <= index) li.setAttribute('completed', ''); else li.removeAttribute('completed');
      if (i === index) li.setAttribute('selected', ''); else li.removeAttribute('selected');
    });

    // Update remaining time
    let remainingMins = 0;
    for (let s = index; s < this._steps.length; s++) {
      const d = parseInt(this._steps[s].getAttribute('duration'), 10);
      remainingMins += isNaN(d) ? (this._steps[s].duration || 0) : d;
    }
    const timeRemainingContainer = this.querySelector('#time-remaining');
    if (timeRemainingContainer) {
      if (remainingMins > 0) {
        timeRemainingContainer.style.display = 'flex';
        if (this._timeRemainingLabel) {
          this._timeRemainingLabel.textContent = `${remainingMins} min remaining`;
        }
      } else {
        timeRemainingContainer.style.display = 'none';
      }
    }

    // Update nav buttons
    if (index === 0) {
      this._prevBtn?.setAttribute('disappear', '');
    } else {
      this._prevBtn?.removeAttribute('disappear');
    }

    if (index === this._steps.length - 1) {
      this._nextBtn?.setAttribute('hidden', '');
      this._doneBtn?.removeAttribute('hidden');
    } else {
      this._nextBtn?.removeAttribute('hidden');
      this._doneBtn?.setAttribute('hidden', '');
    }

    // Update URL hash
    if (location.hash !== '#' + index) {
      history.replaceState(null, '', '#' + index);
    }

    // Persist progress
    const id = this.getAttribute('id') || '';
    if (this._storage && id) {
      this._storage.setItem('progress_' + id, String(index));
    }

    // Fire analytics events
    const stepLabel = this._steps[index]?.label || '';
    dispatchKlaatEvent(this, 'klaat-kodelab-pageview', {
      page: location.pathname + '#' + index,
      title: stepLabel,
    });

    if (prev !== -1 && prev !== index) {
      dispatchKlaatEvent(this, 'klaat-kodelab-action', {
        category: 'codelab',
        action: 'navigate',
        label: stepLabel,
        value: index,
      });
    }
  }

  // -- Helpers ---------------------------------------------------------------

  _updateTitle() {
    const title = this.getAttribute('codelab-title') || '';
    if (this._titleEl) this._titleEl.textContent = title;
    document.title = title;
  }

  _onSelectedAttrChanged() {
    const idx = parseInt(this.getAttribute('selected'), 10);
    if (!isNaN(idx)) this.selectStep(idx);
  }

  _syncAnalytics() {
    const analytics = document.querySelector('klaat-kodelab-analytics');
    if (analytics) {
      const gaid = this.getAttribute('codelab-gaid');
      if (gaid) analytics.setAttribute('codelab-gaid', gaid);
      const env = this.getAttribute('environment');
      if (env) analytics.setAttribute('environment', env);
      const cat = this.getAttribute('category');
      if (cat) analytics.setAttribute('category', cat);
    }
  }

  _escape(str) {
    return (str || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  get steps() { return this._steps; }
}

/* --- KlaatKodelabSurvey --------------------------------------------------- */

class KlaatKodelabSurvey extends HTMLElement {
  connectedCallback() {
    if (this._initialized) return;
    this._initialized = true;

    const surveyId = this.getAttribute('survey-id') || '';
    this._storage = (() => {
      try { return window.localStorage; } catch (e) { return null; }
    })();

    // Restore saved selections
    const saved = this._loadSaved(surveyId);

    // Bind radio inputs
    this.querySelectorAll('input[type="radio"]').forEach((input) => {
      const name = input.getAttribute('name') || '';
      if (saved[name]) {
        // Find matching label text
        const label = input.closest('label');
        if (label && label.textContent.trim().includes(saved[name])) {
          input.checked = true;
        }
      }

      input.addEventListener('change', () => {
        const optionText = (input.closest('label')?.textContent || '').trim().replace(/^\s*/, '');
        this._saveSurvey(surveyId, name, optionText);
        dispatchKlaatEvent(this, 'klaat-kodelab-action', {
          category: 'survey',
          action: name.substring(0, 500),
          label: optionText.substring(0, 500),
        });
      });
    });
  }

  _loadSaved(surveyId) {
    if (!this._storage || !surveyId) return {};
    try {
      return JSON.parse(this._storage.getItem(surveyId) || '{}');
    } catch (e) {
      return {};
    }
  }

  _saveSurvey(surveyId, question, answer) {
    if (!this._storage || !surveyId) return;
    const data = this._loadSaved(surveyId);
    data[question] = answer;
    this._storage.setItem(surveyId, JSON.stringify(data));
  }
}

/* --- Register custom elements --------------------------------------------- */

try { customElements.define('klaat-kodelab', KlaatKodelab); }
catch (e) { console.warn('klaat-kodelab registration failed:', e); }

try { customElements.define('klaat-kodelab-step', KlaatKodelabStep); }
catch (e) { console.warn('klaat-kodelab-step registration failed:', e); }

try { customElements.define('klaat-kodelab-analytics', KlaatKodelabAnalytics); }
catch (e) { console.warn('klaat-kodelab-analytics registration failed:', e); }

try { customElements.define('klaat-kodelab-survey', KlaatKodelabSurvey); }
catch (e) { console.warn('klaat-kodelab-survey registration failed:', e); }
