"""Verify final pixels and write the requested local-only deliverable records."""
import difflib
import hashlib
import json
from pathlib import Path
import subprocess
from PIL import Image, ImageChops, ImageDraw, ImageStat

D = Path(__file__).resolve().parents[1]
S = D.parent / 'captures-fortnite-fix'


def load(path):
    return json.loads((D / path).read_text())


margins = load('evidence/margins.json')
smoke = load('evidence/commands-smoke.json')
final = load('evidence/commands-final.json')
inputs = load('evidence/unchanged-inputs.json')
original = json.loads((S / 'scenes/fortnite-a.json').read_text())
for record in smoke + final:
    assert record['exitCode'] == 0
    assert record['cpuMax'] == '400000 100000' and record['cpuWeight'] == '20'
    assert not Path(f"/proc/{record['pid']}").exists(), record
for name in ['fortnite-a', 'fortnite-b']:
    settings = load(f'evidence/{name}-input.json')
    assert settings['camera'] == original['camera']
    assert settings['sun'] == original['sun']
    assert settings['sky'] == original['sky']
    assert settings['chunkList'] == original['chunkList']
    assert settings['fancierTranslucency'] is False
    for key in original:
        if key not in {'name', 'spp', 'sppTarget', 'renderTime', 'world', 'materials'}:
            assert settings[key] == original[key], key
    for material in settings['materials'].values():
        assert material['ior'] == 1.0002930164337158
        assert material['specular'] == material['metalness'] == 0

empty = Image.open(D / 'renders/fortnite-empty-full.png').convert('RGB')
for row in margins:
    name = Path(row['path']).stem
    full = Image.open(D / 'renders' / (name + '-full.png')).convert('RGB')
    crop = Image.open(D.parent / row['path']).convert('RGB')
    assert ImageChops.difference(full.crop(row['cropFrom1600']), crop).getbbox() is None
    mask = ImageChops.difference(full, empty).convert('L').point(lambda p: 255 if p > 3 else 0)
    bounds = mask.crop(row['cropFrom1600']).getbbox()
    assert list(bounds) == row['contentBoundsExclusive']
    assert bounds[1] == crop.height - bounds[3] == 50
    assert crop.width == crop.height
    assert hashlib.sha256((D.parent / row['path']).read_bytes()).hexdigest() == row['sha256']

manifest = []
for name, gain, label in [('fortnite-a', 1.0, 'Flat-alpha holograms — neutral emission'), ('fortnite-b', .8, 'Flat-alpha holograms — softer emission')]:
    row = next(row for row in margins if Path(row['path']).stem == name)
    manifest.append({'project': 'FortniteInMinecraft', 'label': label, 'path': row['path'], 'method': 'Headless Chunky CPU path trace; scene-local flat-alpha/emissive hologram shader, native square crop.', 'source': 'captures-fortnite-fix/renders/fortnite-a.png; byte-identical approved void world, corrected mod hologram textures, and Minecraft 26.2 solid textures; exact Chunky 527cb4a PathTracer with local hologram-only shading branch.', 'notes': f'Hologram gain {gain}, original texture alpha 150/255, no reflected/scattered/refracted hologram paths. Same approved camera, directional sun, sky, geometry, and solid materials. Wood 48%, brick 50%, copper 50% built. {row["dimensions"][0]} square; exactly 50 px top and bottom margins.'})
(D / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
(D / 'blockers.json').write_text('[]\n')

before = next(row for row in smoke if row['name'] == 'probe-before')['shading']
after = {row['name']: row['shading'] for row in final if row['name'] in ['fortnite-a', 'fortnite-b']}
for row in after.values():
    assert all(int(row[key]) == 0 for key in ['hologramFresnel', 'hologramRefractions', 'hologramDiffuse'])
    assert int(row['panelHits']) > 0

diagnosis = {
    'rootCause': 'The approved adapter and material JSON used hologram IOR 1.0, but actual Chunky Air.INSTANCE.ior is 1.0002930164337158. Any IOR inequality sends a transmitted ray into PathTracer.doRefraction. Its Schlick-Fresnel reflection branch runs independently of both material.specular=0 and material.refractive=false. Thus literal IOR 1.0 is not optically matched to Chunky air. Fancier translucency was already disabled and was not the cause.',
    'evidenceBeforeEditing': {'materialProbe': 'logs/material-probe-1.log', 'runtimeBytecode': 'evidence/renderer-bytecode.txt', 'preEditDiagnosis': 'evidence/root-cause-before-edit.json', 'source': 'https://github.com/chunky-dev/chunky/blob/527cb4a/chunky/src/java/se/llbit/chunky/renderer/scene/PathTracer.java', 'frontFaceFresnelAtApprovedIsometricAngle': 0.013486637, 'withTextureTransparencyBranch': 0.005553321},
    'importantDistinction': 'Baseline instrumentation counted real Fresnel reflections but zero bent refracted rays: refractive=false already prevented ray bending. The glass-like appearance also included lit cubic faces and colored multilayer transmission. It would be inaccurate to claim the baseline had measured refraction, or that disabling fancier translucency fixed it.',
    'fix': 'Use the exact air IOR, retain specular=0, metalness=0, refractive=false and fancierTranslucency=false. A hologram-only branch replaces the lit/transmissive BSDF with linear-light source-over: alpha * textureRGB * emissionGain + (1-alpha) * straight-through scene radiance. No Fresnel, reflected radiance, refraction, normal-dependent lighting, or RGB filtering is sampled by holograms. Cubes, UVs, and source alpha are unchanged. Ordinary materials continue through the unmodified upstream paths.',
    'candidateDifference': 'Only hologram emissionGain differs: A=1.0, B=0.8. Both use the exact approved A camera, sun and sky, not the old B lighting variant.',
    'baseline320px40sppTarget32': before,
    'final1600px130sppTarget128': after,
    'measuredMargins': margins,
    'scenePreservation': inputs,
    'scope': 'All changes and generated files are confined to captures-fortnite-final. No mod repository, tracked portfolio file, commit, or remote changed.',
    'commands': {
        'cwd': str(D),
        'pillowEnvironment': 'PYTHONPATH=/nix/store/4v9j9wbzyhrlx9980ygbr812313mazy0-python3.13-pillow-12.3.0/lib/python3.13/site-packages',
        'smoke': 'systemd-run --user --scope --unit=fortnite-final-smoke -p CPUQuota=400% -p CPUWeight=20 python3 tools/render.py smoke',
        'final': 'systemd-run --user --scope --unit=fortnite-final-render -p CPUQuota=400% -p CPUWeight=20 python3 tools/render.py final',
        'verification': 'python3 tools/finalize.py',
        'exactJavacAndJavaArgv': ['evidence/material-probe-commands.json', 'evidence/commands-smoke.json', 'evidence/commands-final.json']
    },
    'startupCacheNote': 'On the initial render of each new scene name, Chunky reported a missing local .octree2 cache, then successfully loaded all 25 chunks from the byte-identical approved world and regenerated the cache. The original logs preserve this recovery. No missing texture errors. Final images and nonzero hologram path counts empirically verify the world was rendered.',
    'imageVerification': 'Both final PNGs were visually inspected. Native crop pixel identity and margins were independently recomputed here from the empty-world control. Contact sheet and approved/final close-up comparison are evidence-only derivatives, not candidate retouching.'
}
(D / 'diagnosis.json').write_text(json.dumps(diagnosis, indent=2) + '\n')

sheet = Image.new('RGB', (1236 * 2, 1280), '#1d2730')
draw = ImageDraw.Draw(sheet)
for column, name in enumerate(['fortnite-a', 'fortnite-b']):
    sheet.paste(Image.open(D / 'renders' / (name + '.png')).convert('RGB'), (column * 1236, 44))
    draw.text((column * 1236 + 24, 14), manifest[column]['label'], fill='white')
sheet.save(D / 'evidence/candidates-contact.png')
comparison = Image.new('RGB', (720 * 3, 560), '#1d2730')
draw = ImageDraw.Draw(comparison)
for column, (label, path) in enumerate([('Approved: Fresnel still active', S / 'renders/fortnite-a-full.png'), ('A: flat alpha, neutral emission', D / 'renders/fortnite-a-full.png'), ('B: flat alpha, softer emission', D / 'renders/fortnite-b-full.png')]):
    roi = Image.open(path).convert('RGB').crop((840, 330, 1200, 590))
    comparison.paste(roi.resize((720, 520), Image.Resampling.NEAREST), (column * 720, 40))
    draw.text((column * 720 + 12, 13), label, fill='white')
comparison.save(D / 'evidence/approved-flat-closeups.png')
(D / 'evidence/renderer-local.patch').write_text(''.join(difflib.unified_diff((D / 'evidence/PathTracer.original.java').read_text().splitlines(True), (D / 'tools/PathTracer.java').read_text().splitlines(True), fromfile='Chunky-527cb4a/PathTracer.java', tofile='scene-local/PathTracer.java')))

print(json.dumps({'manifest': manifest, 'verifiedMargins': margins, 'finalShading': after}, indent=2))
