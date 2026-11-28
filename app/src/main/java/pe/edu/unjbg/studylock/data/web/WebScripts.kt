package pe.edu.unjbg.studylock.data.web

/**
 * Fragmentos de JavaScript que la app inyecta en el WebView para iniciar sesión y leer la página.
 *
 * Están separados de [UnjbgWebSession] para poder revisarlos y probarlos sin un WebView.
 */
object WebScripts {

    /** Escapa una cadena de Kotlin para incrustarla como literal de JavaScript. */
    fun jsString(value: String): String = buildString {
        append('"')
        for (c in value) {
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
        }
        append('"')
    }

    /**
     * Rellena usuario y contraseña en el formulario visible y pulsa «Ingresar».
     *
     * Cubre tanto el Aula Virtual (Angular) como la Intranet (MVC): prueba varios selectores y
     * dispara los eventos `input`/`change` que Angular necesita para actualizar su modelo.
     */
    fun login(user: String, password: String): String = """
        (function(){
          function q(list){ for (var i=0;i<list.length;i++){ var el=document.querySelector(list[i]); if(el) return el; } return null; }
          var u = q(['input[ng-model="username"]','input[name="username"]','input[name="UserName"]','input#UserName','input[type="text"]']);
          var p = q(['input[ng-model="password"]','input[name="password"]','input[name="Password"]','input#Password','input[type="password"]']);
          if(!u || !p) return 'nofields';
          function set(el, val){
            el.focus(); el.value = val;
            el.dispatchEvent(new Event('input', {bubbles:true}));
            el.dispatchEvent(new Event('change', {bubbles:true}));
            try { if (window.angular) { var ng = angular.element(el); ng.triggerHandler('input'); ng.triggerHandler('change'); } } catch(e){}
            el.blur();
          }
          set(u, ${jsString(user)});
          set(p, ${jsString(password)});
          var b = q(['#m_login_signin_submit','button.button-submit','button[type="submit"]','form button','button']);
          if (b) { b.click(); return 'submitted'; }
          var f = document.querySelector('form');
          if (f) { f.submit(); return 'formsubmit'; }
          return 'nobutton';
        })()
    """.trimIndent()

    /**
     * Devuelve los enlaces de la página como `[{t: texto, h: href, k: origen}]`.
     *
     * Además de `<a href>`, se recogen los enlaces que usan las SPAs (Angular `routerLink`),
     * atributos `data-url`/`data-href`, URLs dentro de `onclick` y botones de descarga. Así el modo
     * inspección («Ver enlaces detectados») sirve aunque el portal no use enlaces clásicos.
     */
    fun collectLinks(): String = """
        (function(){
          var out = [];
          var seen = {};
          function add(text, href, kind, el){
            if (!href) return;
            href = String(href).trim();
            if (!href || href.charAt(0) === '#' || /^javascript:/i.test(href)) return;
            var t = (text || '').replace(/\s+/g,' ').trim();
            var key = href + '|' + t;
            if (seen[key]) return;
            seen[key] = 1;
            if (/^https?:\/\//i.test(href) === false && href.charAt(0) !== '/') {
              try { href = new URL(href, location.href).href; } catch(e){}
            } else if (href.charAt(0) === '/') {
              href = location.origin + href;
            }
            out.push({ t: t, h: href, k: kind, c: contextOf(el) });
          }
          function textOf(el){
            var t = (el.innerText || el.textContent || '').replace(/\s+/g,' ').trim();
            return t || el.getAttribute('title') || el.getAttribute('aria-label') || '';
          }
          function contextOf(el){
            if (!el || !el.parentElement) return '';
            var node = el.parentElement;
            var best = '';
            var fallback = '';
            var action = /^(ver curso|ir al curso|continuar|ingresar|abrir|omitir mis cursos|saltar al contenido|ver más información|ver mas informacion|más información|mas informacion|clic aquí|click aqui|omitir|saltar)\b/i;
            for (var i=0; node && i<5; i++, node = node.parentElement) {
              var t = (node.innerText || '').replace(/\s+/g,' ').trim();
              if (t.length < 4) continue;
              if (t.length > fallback.length) fallback = t;
              var meaningful = t.replace(action, '').trim();
              if (meaningful.length >= 8 && (!best || meaningful.length < best.length)) best = meaningful;
              if (best && best.length < 90) break;
            }
            return (best || fallback).slice(0, 160);
          }
          var anchors = document.querySelectorAll('a[href]');
          for (var i=0;i<anchors.length;i++) add(textOf(anchors[i]), anchors[i].href, 'a', anchors[i]);

          var routers = document.querySelectorAll('[routerLink],[routerlink],[data-url],[data-href]');
          for (var j=0;j<routers.length;j++){
            var el = routers[j];
            var url = el.getAttribute('routerLink') || el.getAttribute('routerlink') ||
                      el.getAttribute('data-url') || el.getAttribute('data-href');
            add(textOf(el), url, el.hasAttribute('routerLink') || el.hasAttribute('routerlink') ? 'routerLink' : 'data-url', el);
          }

          var clickables = document.querySelectorAll('[onclick],button,input[type="button"]');
          for (var k=0;k<clickables.length;k++){
            var c = clickables[k];
            var oc = c.getAttribute('onclick') || '';
            var m = oc.match(/(?:href|url|location|window\.open)\s*=?\s*\(?\s*['"]([^'"]+)['"]/i);
            if (!m) m = oc.match(/['"](\/[^'"]+\.(?:pdf|doc|docx|xls|xlsx|zip))['"]/i);
            if (m) add(textOf(c), m[1], c.tagName === 'BUTTON' || c.tagName === 'INPUT' ? 'button' : 'onclick', c);
          }
          return JSON.stringify(out);
        })()
    """.trimIndent()

    /**
     * Pide [url] con `fetch` **dentro de la página** (misma sesión y cookies) y devuelve el
     * resultado al puente [bridge] como `{ok, status, body}`.
     */
    fun fetchText(url: String, requestId: String, bridge: String): String = """
        (function(){
          var id = ${jsString(requestId)};
          try {
            fetch(${jsString(url)}, { credentials: 'include', headers: { 'Accept': 'application/json, text/plain, */*' } })
              .then(function(r){ return r.text().then(function(t){
                $bridge.onResult(id, JSON.stringify({ ok: true, status: r.status, body: t }));
              }); })
              .catch(function(e){ $bridge.onResult(id, JSON.stringify({ ok: false, status: 0, body: String(e) })); });
          } catch (e) {
            $bridge.onResult(id, JSON.stringify({ ok: false, status: 0, body: String(e) }));
          }
          return id;
        })()
    """.trimIndent()

    /** URL actual de la pestaña del WebView. */
    fun location(): String = "(function(){ return location.href; })()"

    /** `'true'` si la página sigue mostrando un campo de contraseña (login sin completar). */
    fun hasPasswordField(): String =
        "(function(){ return document.querySelector('input[type=password]') ? 'true' : 'false'; })()"

    /** Condición booleana: ya no hay campo de contraseña (login completado). */
    fun noPasswordField(): String =
        "!document.querySelector('input[type=password]')"

    /** Título del documento, para nombrar las secciones del árbol. */
    fun pageTitle(): String =
        "(function(){ return document.title ? document.title : ''; })()"

    /** Texto visible de la página (para depuración o extracción simple). */
    fun pageText(): String = "(function(){ return document.body ? document.body.innerText : ''; })()"
}
