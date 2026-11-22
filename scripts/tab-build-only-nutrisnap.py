import base64, datetime, os, pathlib, re, subprocess, sys, json, urllib.parse, urllib.request, zipfile, struct, hashlib
T=pathlib.Path(os.environ['RUNNER_TEMP']); C=T/'nutrisnap-recipient.pem'; P='com.charles.nutrisnap'; B=pathlib.Path('app/build/outputs/bundle/release/app-release.aab'); OUT=T/'nutrisnap-candidate.cms'; BT=T/'tab-bundletool-1.18.3.jar'

def run(a, **kw):
    kw.setdefault('timeout',60)
    return subprocess.run(a, check=True, capture_output=True, **kw).stdout

def need(k):
    v=os.environ.get(k,'')
    if not v.strip() or '\x00' in v: raise SystemExit(f'Required configuration is missing: {k}')
    return v

def setup():
    try:
        q=json.loads(base64.b64decode(need('REQUEST'),validate=True)); assert set(q)=={'pem','code','version'}
        code=q['code']; ver=q['version']; cert=q['pem']; assert type(code) is int and 100000000<=code<2100000000
        assert re.fullmatch(r'0\.2\.0\.\d{8}\.\d{4}',ver); ds=datetime.datetime.strptime(ver.removeprefix('0.2.0.'),'%Y%m%d.%H%M')
        assert code==int(ds.strftime('%y%j%H%M')) and re.fullmatch(r'-----BEGIN CERTIFICATE-----\n[A-Za-z0-9+/=\n]+-----END CERTIFICATE-----\n?',cert)
        C.write_text(cert); run(['openssl','x509','-in',str(C),'-noout','-checkend','900'])
        signer=run(['openssl','x509','-in',str(C),'-noout','-text']).decode(); assert 'Public-Key: (3072 bit)' in signer
        raw=base64.b64decode(''.join(need('GS_CLIENT_B64').split()),validate=True); gs=json.loads(raw)
        c=[i for i in gs['client'] if i.get('client_info',{}).get('android_client_info',{}).get('package_name')==P]
        assert len(c)==1 and c[0]['client_info'].get('mobilesdk_app_id') and gs['project_info'].get('project_id') not in (None,'','nutrisnap-qa','dummy','test'); assert 300<len(raw)<100000
        target=pathlib.Path('app/google-services.json'); assert not target.exists(); target.write_bytes(raw); target.chmod(0o600)
        with open(os.environ['GITHUB_ENV'],'a') as f:f.write(f'ANDROID_VERSION_CODE={code}\nANDROID_VERSION_NAME={ver}\n')
    except Exception: raise SystemExit('Invalid exact version, encryption recipient or exact NutriSnap release client configuration') from None
    print('Original package and production client configuration verified; version',code,ver)

def check_env():
    u=urllib.parse.urlsplit(need('GH_PROXY_URL'))
    if u.scheme!='https' or not u.hostname or u.username or u.password or u.query or u.fragment or u.hostname in ('localhost','127.0.0.1'):raise SystemExit('Invalid production reporting URL')
    need('GH_PROXY_SECRET')
    patterns={'ADMOB_APP_ID':r'ca-app-pub-[0-9]{16}~[0-9]{10}','ADMOB_BANNER_ID':r'ca-app-pub-[0-9]{16}/[0-9]{10}','ADMOB_INTERSTITIAL_ID':r'ca-app-pub-[0-9]{16}/[0-9]{10}'}
    for k,pat in patterns.items():
        v=need(k)
        if not re.fullmatch(pat,v) or v.startswith('ca-app-pub-3940256099942544'):raise SystemExit('Invalid production ad config: '+k)
    print('Reporting and ad settings match expected release configuration shapes')

def verify_bundle():
    assert B.is_file() and 10_000_000 < B.stat().st_size < 95_000_000
    url='https://github.com/google/bundletool/releases/download/1.18.3/bundletool-all-1.18.3.jar'
    with urllib.request.urlopen(url,timeout=25) as f: b=f.read(80_000_001)
    assert hashlib.sha256(b).hexdigest()=='a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29'; BT.write_bytes(b); del b
    base=['java','-jar',str(BT)]; run(base+['validate','--bundle='+str(B)])
    mc=run(base+['dump','manifest','--bundle='+str(B)]).decode()
    cfg=run(base+['dump','config','--bundle='+str(B)]).decode()
    assert 'package="'+P+'"' in mc and 'android:versionCode="'+need('ANDROID_VERSION_CODE')+'"' in mc and 'android:versionName="'+need('ANDROID_VERSION_NAME')+'"' in mc and 'android:targetSdkVersion="36"' in mc
    assert 'com.google.android.play.billingclient.version' in mc and 'android:value="9.1.0"' in mc and 'PAGE_ALIGNMENT_16K' in cfg
    with zipfile.ZipFile(B) as z:
        assert not any(re.fullmatch(r'META-INF/[^/]+\.(RSA|DSA|EC|SF)',n,re.I) for n in z.namelist())
        libs=[n for n in z.namelist() if re.match(r'^base/lib/[^/]+/[^/]+\.so$',n)]; assert {x.split('/')[2] for x in libs}=={'armeabi-v7a','arm64-v8a','x86','x86_64'}
        for ln in libs:
            if ln.split('/')[2] not in ('arm64-v8a','x86_64'): continue
            g=z.read(ln); assert g[:6]==b'\x7fELF\x02\x01'; off=struct.unpack_from('<Q',g,32)[0]; size,num=struct.unpack_from('<HH',g,54); assert size>=56 and 0<num<600 and off+num*size<=len(g)
            for i in range(num):
                ty,_,o,v,_,_,_,al=struct.unpack_from('<IIQQQQQQ',g,off+i*size)
                if ty==1: assert al>=16384 and (v-o)%16384==0
    print('Bundletool validated original package, SDK 36, Billing 9.1.0, and native 16 KiB alignment')

def sign():
    verify_bundle()
    kd=base64.b64decode(''.join(need('NUTRI_RELEASE_KEYSTORE_B64').split()),validate=True); assert 900<len(kd)<150000
    alias=need('NUTRI_RELEASE_KEY_ALIAS'); assert not alias.startswith('-') and len(alias)<150
    fd=os.memfd_create('nutrisnap-upload-keystore',os.MFD_CLOEXEC)
    try:
        os.write(fd,kd); del kd; os.lseek(fd,0,os.SEEK_SET); ks=f'/proc/self/fd/{fd}'
        cert=run(['keytool','-exportcert','-alias',alias,'-keystore',ks,'-storepass:env','NUTRI_RELEASE_STORE_PASS'],pass_fds=(fd,)); assert len(cert)>500
        run(['jarsigner','-keystore',ks,'-storepass:env','NUTRI_RELEASE_STORE_PASS','-keypass:env','NUTRI_RELEASE_KEY_PASS',str(B),alias],pass_fds=(fd,),timeout=90)
    finally: os.close(fd)
    v=run(['jarsigner','-verify',str(B)]).decode(); assert 'jar verified.' in v.lower()
    with zipfile.ZipFile(B) as z:
        assert sum(bool(re.fullmatch(r'META-INF/[^/]+\.(RSA|DSA|EC)',n,re.I)) for n in z.namelist())==1 and z.testzip() is None
    run(['openssl','cms','-encrypt','-binary','-aes-256-gcm','-in',str(B),'-outform','DER','-out',str(OUT),'-recip',str(C),'-keyopt','rsa_padding_mode:oaep','-keyopt','rsa_oaep_md:sha256'],timeout=90)
    assert OUT.stat().st_size > B.stat().st_size
    print('Signed SHA-256',hashlib.file_digest(B.open('rb'),'sha256').hexdigest())
    print('Upload certificate SHA-256',hashlib.sha256(cert).hexdigest())

if __name__=='__main__':
    try: {'setup':setup,'check-env':check_env,'sign-and-wrap':sign}[sys.argv[1]]()
    except (subprocess.CalledProcessError,subprocess.TimeoutExpired):raise SystemExit('Bounded release tool failed; no package was published') from None
