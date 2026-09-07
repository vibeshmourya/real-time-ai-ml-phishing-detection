# PhishShield Dataset Directory

This directory contains datasets used for training and evaluating PhishShield's machine learning classifiers.

## Directory Structure

```
data/
├── raw/
│   ├── phishing_dataset.csv     # Raw dataset (CSV format)
│   └── README.md
└── processed/                   # Cleaned/intermediate feature matrices (if generated)
```

## Dataset Format

The dataset must be a CSV file containing at least two columns:
1. `url` - Full web address string (e.g., `https://example.com/login`)
2. `label` - Binary classification label:
   - `0`, `legitimate`, `benign`, or `safe` for benign URLs
   - `1`, `phishing`, `malicious`, or `bad` for phishing URLs

### Sample Schema
```csv
url,label
https://www.google.com/search?q=cybersecurity,0
https://en.wikipedia.org/wiki/Phishing,0
http://secure-login-hdfc-netbanking.tk/verify,1
http://192.168.1.100/account-update/login.php,1
```

## Training Command

To execute training with this dataset:
```bash
python training/train.py
```
