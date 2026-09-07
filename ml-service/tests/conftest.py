import sys
from pathlib import Path

# Add ml-service root to sys.path for test discovery
ML_SERVICE_DIR = Path(__file__).resolve().parent.parent
if str(ML_SERVICE_DIR) not in sys.path:
    sys.path.insert(0, str(ML_SERVICE_DIR))
