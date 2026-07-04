/**
 * --- Alpine helper functions
 */

/**
 * Set an alpine.js property
 *
 * @function setProp
 * @param {string} key - Key of the data property
 * @param {*} prop - The data property
 * @param {string} selector - The jQuery selector to the root element
 */
const setProp = (key, prop, selector = '#root') => {
  $(selector).get(0).__x.$data[key] = prop;
};

/**
 * Get an alpine.js property
 *
 * @function getProp
 * @param {string} key - Key of the data property
 * @param {string} selector - The jQuery selector to the root element
 * @return {*} The data property
 */
const getProp = (key, selector = '#root') => {
  return $(selector).get(0).__x.$data[key];
};

/**
 * --- Theme related functions
 *  	Note: In the comments below we treat "theme" and "theme setting"
 *  		differently. A theme can have only two values, either "dark" or
 *  		"light", while a theme setting can have the third value "system".
 */

/**
 * Check if the system setting prefers dark theme.
 * 		from https://flaviocopes.com/javascript-detect-dark-mode/
 *
 * @function preferDarkMode
 * @return {bool}
 */
const preferDarkMode = () => {
  return (
    window.matchMedia &&
    window.matchMedia('(prefers-color-scheme: dark)').matches
  );
};

/**
 * Check whether a given string represents a valid theme setting
 *
 * @function validThemeSetting
 * @param {string} theme - The string representing the theme setting
 * @return {bool}
 */
const validThemeSetting = (theme) => {
  return ['dark', 'light', 'system'].indexOf(theme) >= 0;
};

/**
 * Load theme setting from local storage, or use 'light'
 *
 * @function loadThemeSetting
 * @return {string} A theme setting ('dark', 'light', or 'system')
 */
const loadThemeSetting = () => {
  let str = localStorage.getItem('theme');
  if (!str || !validThemeSetting(str)) str = 'system';
  return str;
};

/**
 * Load the current theme (not theme setting)
 *
 * @function loadTheme
 * @return {string} The current theme to use ('dark' or 'light')
 */
const loadTheme = () => {
  let setting = loadThemeSetting();
  if (setting === 'system') {
    setting = preferDarkMode() ? 'dark' : 'light';
  }
  return setting;
};

/**
 * Save a theme setting
 *
 * @function saveThemeSetting
 * @param {string} setting - A theme setting
 */
const saveThemeSetting = (setting) => {
  if (!validThemeSetting(setting)) setting = 'system';
  localStorage.setItem('theme', setting);
};

/**
 * Toggle the current theme. When the current theme setting is 'system', it
 *		will be changed to either 'light' or 'dark'
 *
 * @function toggleTheme
 */
const toggleTheme = () => {
  const theme = loadTheme();
  const newTheme = theme === 'dark' ? 'light' : 'dark';
  saveThemeSetting(newTheme);
  setTheme(newTheme);
};

/**
 * Apply a theme, or load a theme and then apply it
 *
 * @function setTheme
 * @param {string?} theme - (Optional) The theme to apply. When omitted, use
 * 		`loadTheme` to get a theme and apply it.
 */
const setTheme = (theme) => {
  if (!theme) theme = loadTheme();
  if (theme === 'dark') {
    $('html').css('background', 'rgb(20, 20, 20)');
    $('body').addClass('uk-light');
    $('.ui-widget-content').addClass('dark');
  } else {
    $('html').css('background', '');
    $('body').removeClass('uk-light');
    $('.ui-widget-content').removeClass('dark');
  }
};

// Firefox/Safari fallback for cross-document navigation transitions.
const hasNativeViewTransitions =
  typeof document !== 'undefined' &&
  typeof document.startViewTransition === 'function';

if (!hasNativeViewTransitions) {
  const root = document.documentElement;
  root.classList.add('vt-fallback', 'vt-enter');

  window.addEventListener('pageshow', () => {
    requestAnimationFrame(() => {
      root.classList.remove('vt-enter', 'vt-leave');
    });
  });

  let isLeaving = false;
  document.addEventListener('click', (event) => {
    if (event.defaultPrevented || event.button !== 0) return;
    if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;

    const anchor = event.target.closest('a[href]');
    if (!anchor) return;
    if (anchor.hasAttribute('download')) return;
    if (anchor.getAttribute('target') && anchor.getAttribute('target') !== '_self') return;
    if (anchor.hasAttribute('data-no-transition')) return;

    const href = anchor.getAttribute('href');
    if (!href || href.startsWith('javascript:') || href.startsWith('mailto:') || href.startsWith('tel:')) return;

    const url = new URL(anchor.href, window.location.href);
    if (url.origin !== window.location.origin) return;

    // Keep same-page hash jumps instant.
    if (
      url.pathname === window.location.pathname &&
      url.search === window.location.search &&
      url.hash
    ) {
      return;
    }

    event.preventDefault();
    if (isLeaving) return;
    isLeaving = true;
    root.classList.add('vt-leave');

    window.setTimeout(() => {
      window.location.assign(url.href);
    }, 180);
  });
}

// do it before document is ready to prevent the initial flash of white on
// 	most pages
setTheme();
$(() => {
  // hack for the reader page
  setTheme();

  // on system dark mode setting change
  if (window.matchMedia) {
    window
      .matchMedia('(prefers-color-scheme: dark)')
      .addEventListener('change', (event) => {
        if (loadThemeSetting() === 'system')
          setTheme(event.matches ? 'dark' : 'light');
      });
  }
});
