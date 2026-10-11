import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request

def main():
    keys = ['RENDER_DEPLOY_HOOK', 'APP_URL', 'EXPECTED_ENV', 'COMMIT_SHA']
    v = {k: os.environ.get(k, '').strip() for k in keys}
    if any(not x for x in v.values()):
        raise SystemExit('Configure o secret RENDER_DEPLOY_HOOK e a variável APP_URL no ambiente GitHub.')
    hook = v['RENDER_DEPLOY_HOOK']
    if urllib.parse.urlsplit(hook).hostname != 'api.render.com':
        raise SystemExit('Hook deve pertencer a api.render.com.')
    url = hook + ('&' if '?' in hook else '?') + urllib.parse.urlencode({'ref': v['COMMIT_SHA']})
    try:
        with urllib.request.urlopen(urllib.request.Request(url, method='POST'), timeout=60) as r:
            if r.status not in (200, 202):
                raise SystemExit('Deploy rejeitado.')
    except urllib.error.URLError:
        raise SystemExit('Falha ao disparar deploy; confira o secret. URL omitida.') from None
    base = v['APP_URL'].rstrip('/')
    deadline = time.monotonic() + 1500
    print('Deploy disparado; aguardando commit em ' + v['EXPECTED_ENV'], flush=True)
    while time.monotonic() < deadline:
        try:
            def get(path):
                with urllib.request.urlopen(base + path, timeout=30) as r:
                    return json.load(r)
            info = get('/api/ambiente')
            if info.get('ambiente') == v['EXPECTED_ENV'] and info.get('versao') == v['COMMIT_SHA']:
                if get('/actuator/health').get('status') == 'UP' and isinstance(get('/api/iniciativas'), list):
                    print('DEPLOY OK: ambiente, commit, saúde e API verificados.', flush=True)
                    return
        except (urllib.error.URLError, TimeoutError, ValueError, OSError):
            pass
        time.sleep(15)
    raise SystemExit('Tempo esgotado; consulte logs do Render.')

if __name__ == '__main__':
    main()
