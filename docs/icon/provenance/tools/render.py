"""Reproduce the approved scene with a hologram-only flat-alpha shader."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
from collections import Counter
from PIL import Image, ImageChops
import anvil

D = Path(__file__).resolve().parents[1]
S = D.parent / 'captures-fortnite-fix'
JAVA = '/nix/store/b7hzgfqk2jfcbncp0vpabf73j0riqgq4-openjdk-25.0.4.1+1/bin/java'
CP = ':'.join(str(p) for p in sorted((D / 'lib').glob('*.jar')))
RECORDS = []


def run(command, name):
    print('RUN', name, flush=True)
    record = {'name': name, 'argv': command}
    RECORDS.append(record)
    with (D / 'logs' / (name + '.log')).open('w') as log:
        process = subprocess.Popen(command, cwd=D, stdout=log, stderr=subprocess.STDOUT)
        record['pid'] = process.pid
        time.sleep(.3)
        proc = Path(f'/proc/{process.pid}/cgroup')
        if proc.exists():
            cgroup = proc.read_text().strip().split('::')[-1]
            record['cgroup'] = cgroup
            group = Path('/sys/fs/cgroup' + cgroup)
            record['cpuMax'] = (group / 'cpu.max').read_text().strip()
            record['cpuWeight'] = (group / 'cpu.weight').read_text().strip()
            quota, period = record['cpuMax'].split()
            if quota == 'max' or int(quota) / int(period) > 4 or record['cpuWeight'] != '20':
                process.terminate()
                process.wait()
                raise RuntimeError('Uncapped process: ' + json.dumps(record))
        code = process.wait(timeout=2400)
        record['exitCode'] = code
        if code:
            raise RuntimeError(f'{name}: exit {code}; see log')
    text = (D / 'logs' / (name + '.log')).read_text()
    shading = re.search(r'FIM_SHADING flatPanels=(\w+) panelHits=(\d+) hologramFresnel=(\d+) hologramRefractions=(\d+) hologramDiffuse=(\d+)', text)
    if shading:
        record['shading'] = dict(zip(['flatPanels', 'panelHits', 'hologramFresnel', 'hologramRefractions', 'hologramDiffuse'], shading.groups()))
        print('SHADING', record['shading'], flush=True)
    (D / 'evidence' / ('commands-' + sys.argv[1] + '.json')).write_text(json.dumps(RECORDS, indent=2) + '\n')
    return record


def prepare():
    blocks = anvil.world_blocks(D / 'worlds/fortnite')
    assert blocks == anvil.world_blocks(S / 'worlds/fortnite')
    assert not anvil.world_blocks(D / 'worlds/empty')
    census = Counter(blocks.values())
    assert census == {'minecraft:oak_planks': 12, 'fortniteinminecraft:build_hologram_wood': 13, 'minecraft:bricks': 10, 'fortniteinminecraft:build_hologram_stone': 10, 'minecraft:waxed_copper_block': 8, 'fortniteinminecraft:build_hologram_metal': 8}
    hashes = {}
    for folder in ['worlds', 'textures', 'lib']:
        for path in (D / folder).rglob('*'):
            if path.is_file():
                relative = path.relative_to(D)
                assert path.read_bytes() == (S / relative).read_bytes(), str(relative)
                hashes[str(relative)] = hashlib.sha256(path.read_bytes()).hexdigest()
    (D / 'evidence/unchanged-inputs.json').write_text(json.dumps({'allCopiedInputsByteIdentical': True, 'census': census, 'solidFractions': {'wood': '12/25 = 48%', 'brick': '10/20 = 50%', 'copper': '8/16 = 50%'}, 'voidOnly': True, 'sha256': hashes}, indent=2) + '\n')
    run([str(Path(JAVA).parent / 'javac'), '-cp', CP, '-d', str(D / 'classes'), str(D / 'tools/FortniteChunky.java'), str(D / 'tools/PathTracer.java')], 'compile-adapter-' + sys.argv[1])


def render(name, size=1600, spp=128, gain=1.0, baseline=False, empty=False):
    source = json.loads((S / 'scenes/fortnite-a.json').read_text())
    data = json.loads(json.dumps(source))
    data.update(name=name, width=size, height=size, spp=0, sppTarget=spp, renderTime=0)
    data['world']['path'] = str(D / 'worlds' / ('empty' if empty else 'fortnite'))
    if not baseline:
        for material in data['materials'].values():
            material.update(ior=1.0002930164337158, specular=0.0, metalness=0.0, emittance=gain)
    for key in source:
        if key not in {'name', 'width', 'height', 'spp', 'sppTarget', 'renderTime', 'world', 'materials'}:
            assert data[key] == source[key], key
    path = D / 'scenes' / (name + '.json')
    path.write_text(json.dumps(data, indent=2) + '\n')
    shutil.copy2(path, D / 'evidence' / (name + '-input.json'))
    command = [JAVA, '-Xmx4G', '-XX:ActiveProcessorCount=4', '-Djava.awt.headless=true', f'-Dchunky.home={D}/chunky-home', f'-Dfim.textures={D}/textures', f'-Dfim.baseline={str(baseline).lower()}', '-cp', str(D / 'classes') + ':' + CP, 'FortniteChunky', '-texture', str(D / 'textures/real-fortnite.zip'), '-render', str(path), '-reload-chunks', '-f', '-target', str(spp), '-threads', '4']
    record = run(command, name)
    snapshot = max((D / 'scenes/snapshots').glob(name + '-*.png'), key=lambda p: p.stat().st_mtime)
    shutil.copy2(snapshot, D / 'renders' / (name + '-full.png'))
    counts = record['shading']
    if baseline:
        assert int(counts['hologramFresnel']) > 0
        assert int(counts['hologramDiffuse']) > 0
    else:
        assert all(int(counts[key]) == 0 for key in ['hologramFresnel', 'hologramRefractions', 'hologramDiffuse']), counts
        if not empty:
            assert int(counts['panelHits']) > 0


def crop(name):
    image = Image.open(D / 'renders' / (name + '-full.png')).convert('RGB')
    empty = Image.open(D / 'renders/fortnite-empty-full.png').convert('RGB')
    mask = ImageChops.difference(image, empty).convert('L').point(lambda p: 255 if p > 3 else 0)
    x0, y0, x1, y1 = mask.getbbox()
    side = y1 - y0 + 100
    left = round((x0 + x1 - side) / 2)
    top = y0 - 50
    rectangle = (left, top, left + side, top + side)
    assert 0 <= left and rectangle[2] <= image.width and 0 <= top and rectangle[3] <= image.height
    cropped = image.crop(rectangle)
    target = D / 'renders' / (name + '.png')
    cropped.save(target)
    bounds = mask.crop(rectangle).getbbox()
    assert cropped.width == cropped.height and bounds[1] == 50 and side - bounds[3] == 50
    return {'path': str(target.relative_to(D.parent)), 'dimensions': cropped.size, 'contentBoundsExclusive': bounds, 'topMarginPx': 50, 'bottomMarginPx': 50, 'cropFrom1600': rectangle, 'method': 'Luminance difference >3 from separately rendered identical empty-world control; native crop only, no scaling, padding, or pixel retouching.', 'sha256': hashlib.sha256(target.read_bytes()).hexdigest()}


if __name__ == '__main__':
    print('READY capped headless Fortnite render', flush=True)
    prepare()
    if sys.argv[1] == 'smoke':
        render('probe-before', size=320, spp=32, baseline=True)
        render('probe-after-a', size=320, spp=32, gain=1.0)
        render('probe-after-b', size=320, spp=32, gain=.8)
    elif sys.argv[1] == 'final':
        render('fortnite-empty', spp=8, empty=True)
        crops = []
        for name, gain in [('fortnite-a', 1.0), ('fortnite-b', .8)]:
            render(name, gain=gain)
            crops.append(crop(name))
        (D / 'evidence/margins.json').write_text(json.dumps(crops, indent=2) + '\n')
        print(json.dumps(crops, indent=2), flush=True)
    else:
        raise ValueError('Expected smoke or final')
    print('COMPLETE', sys.argv[1], flush=True)
