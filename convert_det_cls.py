import sys, os
sys.path.insert(0, 'D:/Python310/Lib/site-packages')
from paddlelite.lite import Opt

models = [
    ('Y:/Temp/det_model/inference.pdmodel', 'Y:/Temp/det_out'),
    ('Y:/Temp/cls_model/inference.pdmodel', 'Y:/Temp/cls_out'),
]

for model_file, out_prefix in models:
    print(f'Converting: {model_file}')
    sys.stdout.flush()
    try:
        opt = Opt()
        opt.set_model_file(model_file)
        opt.set_model_type('naive_buffer')
        opt.set_valid_places('arm')
        opt.set_optimize_out(out_prefix)
        opt.run()
        nb_path = out_prefix + '.nb'
        if os.path.exists(nb_path):
            print(f'  OK: {os.path.getsize(nb_path)} bytes')
        else:
            print(f'  Failed: output not found')
    except Exception as e:
        print(f'  Error: {e}')
    sys.stdout.flush()
