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

    /** Devuelve todos los enlaces de la página como `[{t: texto, h: href}]`. */
    fun collectLinks(): String = """
        (function(){
          var out = [];
          var anchors = document.querySelectorAll('a[href]');
          for (var i=0;i<anchors.length;i++){
            var a = anchors[i];
            if (!a.href) continue;
            var t = (a.textContent || '').replace(/\s+/g,' ').trim();
            out.push({ t: t, h: a.href });
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
