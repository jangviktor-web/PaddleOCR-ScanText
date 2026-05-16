import os, sys
sys.path.insert(0, 'D:/Python310/Lib/site-packages')
from paddlelite.lite import Opt

base = 'D:/AndroidProjects/paddleocr4android/app/src/main/assets/models'

# (source_pdmodel_dir, output_dir, output_name)
tasks = [
    ('/tmp/ch_PP-OCRv4_det_infer', f'{base}/ch_PP-OCRv4', 'det.nb'),
    ('/tmp/ch_PP-OCRv4_rec_infer', f'{base}/ch_PP-OCRv4', 'rec.nb'),
    ('/tmp/ch_ppocr_mobile_v2.0_cls_infer', f'{base}/ch_PP-OCRv4', 'cls.nb'),
    ('/tmp/japan_PP-OCRv3_rec_infer', f'{base}/japan_PP-OCRv3', 'rec.nb'),
    ('/tmp/korean_PP-OCRv3_rec_infer', f'{base}/korean_PP-OCRv3', 'rec.nb'),
    ('/tmp/chinese_cht_PP-OCRv3_rec_infer', f'{base}/chinese_cht_PP-OCRv3', 'rec.nb'),
]

# Copy shared det.nb and cls.nb to each language dir
import shutil
shared_dirs = ['japan_PP-OCRv3', 'korean_PP-OCRv3', 'chinese_cht_PP-OCRv3']

for src_dir, out_dir, out_name in tasks:
    os.makedirs(out_dir, exist_ok=True)
    if out_name == 'rec.nb':
        pdmodel = os.path.join(src_dir, 'inference.pdmodel')
        out_path = os.path.join(out_dir, out_name.replace('.nb', ''))
        print(f'Converting {os.path.basename(src_dir)} -> {out_name}...')
        sys.stdout.flush()
        try:
            opt = Opt()
            opt.set_model_file(pdmodel)
            opt.set_model_type('naive_buffer')
            opt.set_valid_places('arm')
            opt.set_optimize_out(out_path)
            opt.run()
            size = os.path.getsize(out_path + '.nb')
            print(f'  Done: {size} bytes')
        except Exception as e:
            print(f'  Error: {e}')
        sys.stdout.flush()

# Copy shared det.nb and cls.nb
det_src = f'{base}/ch_PP-OCRv4/det.nb'
cls_src = f'{base}/ch_PP-OCRv4/cls.nb'
for d in shared_dirs:
    dst_dir = f'{base}/{d}'
    if os.path.exists(det_src):
        shutil.copy2(det_src, os.path.join(dst_dir, 'det.nb'))
        print(f'Copied det.nb to {d}')
    if os.path.exists(cls_src):
        shutil.copy2(cls_src, os.path.join(dst_dir, 'cls.nb'))
        print(f'Copied cls.nb to {d}')

print('\nAll done!')
for d in os.listdir(base):
    dp = os.path.join(base, d)
    if os.path.isdir(dp):
        files = [f for f in os.listdir(dp) if f.endswith('.nb')]
        sizes = {f: os.path.getsize(os.path.join(dp, f)) for f in files}
        print(f'{d}: {sizes}')
