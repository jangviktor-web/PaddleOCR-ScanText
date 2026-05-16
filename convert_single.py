import sys
sys.path.insert(0, 'D:/Python310/Lib/site-packages')
from paddlelite.lite import Opt

print("Testing Opt tool...")
sys.stdout.flush()

try:
    opt = Opt()
    print("Opt created")
    sys.stdout.flush()
    opt.set_model_file('/tmp/ch_PP-OCRv4_det_infer/inference.pdmodel')
    print("Model file set")
    sys.stdout.flush()
    opt.set_model_type('naive_buffer')
    print("Model type set")
    sys.stdout.flush()
    opt.set_valid_places('arm')
    print("Valid places set")
    sys.stdout.flush()
    opt.set_optimize_out('/tmp/test_det')
    print("Output set")
    sys.stdout.flush()
    opt.run()
    print("Run completed!")
    sys.stdout.flush()
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()
    sys.stdout.flush()
