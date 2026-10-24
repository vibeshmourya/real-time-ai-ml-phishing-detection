import datetime
import logging
import re
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
from urllib.parse import urlparse, urlunparse
import pandas as pd

logger = logging.getLogger("phishshield.ml.ingest")

# Representative hard negatives (legitimate banking, gov, educational, and e-commerce domains)
# with complex query strings, subdomains, and hyphenated names that naive heuristics misclassify
CURATED_HARD_NEGATIVES = [
    "https://retail.onlinesbi.sbi/retail/login.htm",
    "https://netbanking.hdfcbank.com/netbanking/",
    "https://www.icicibank.com/personal-banking/insta-banking/internet-banking",
    "https://incometax.gov.in/iec/foportal/",
    "https://cybercrime.gov.in/Webform/Crime_AuthoLogin.aspx",
    "https://passportindia.gov.in/AppOnlineProject/welcomeLink",
    "https://www.irctc.co.in/nget/train-search",
    "https://uidai.gov.in/my-aadhaar/get-aadhaar.html",
    "https://accounts.google.com/signin/v2/identifier?service=mail",
    "https://login.microsoftonline.com/common/oauth2/authorize",
    "https://github.com/login",
    "https://www.amazon.in/ap/signin?openid.pape.max_auth_age=0",
    "https://paytm.com/recharge",
    "https://www.phonepe.com/en/",
    "https://ceir.sancharsaathi.gov.in/Device/CeirUserRequest",
    "https://unifiedportal-mem.epfindia.gov.in/memberinterface/",
    "https://www.canarabank.com/pages/net-banking",
    "https://bankofbaroda.in/personal-banking/digital-products/instant-banking/baroda-connect-internet-banking",
    "https://axisbank.com/bank-smart/internet-banking",
    "https://punjabnationalbank.in/internet-banking.html"
]

def normalize_url(raw_url: str) -> Optional[str]:
    """
    Standardizes a URL for deduplication and feature extraction:
    - Strips leading/trailing whitespace
    - Ensures lowercased scheme and host
    - Strips fragment identifiers (#...)
    - Strips trailing slash if path is empty
    """
    if not raw_url or not isinstance(raw_url, str):
        return None
        
    cleaned = raw_url.strip()
    if not cleaned or len(cleaned) < 4:
        return None
        
    # Prepend scheme if missing
    if not cleaned.startswith(("http://", "https://")):
        cleaned = "http://" + cleaned
        
    try:
        parsed = urlparse(cleaned)
        if not parsed.netloc:
            return None
            
        scheme = parsed.scheme.lower()
        netloc = parsed.netloc.lower()
        path = parsed.path or "/"
        
        # Remove default ports
        if netloc.endswith(":80") and scheme == "http":
            netloc = netloc[:-3]
        elif netloc.endswith(":443") and scheme == "https":
            netloc = netloc[:-4]
            
        # Reconstruct without fragment
        normalized = urlunparse((scheme, netloc, path, parsed.params, parsed.query, ""))
        return normalized
    except Exception as e:
        logger.debug("URL normalization failed for '%s': %s", raw_url, e)
        return None

class PhishingFeedIngester:
    """
    Configurable, license-compliant data ingestion pipeline for phishing and benign URL feeds.
    Does not violate third-party terms or bypass anti-scraping controls.
    Supports local snapshot parsing and authorized public community feeds.
    """
    def __init__(self, output_dir: Optional[str] = None):
        self.output_dir = Path(output_dir or Path(__file__).resolve().parent.parent / "data" / "processed")
        self.output_dir.mkdir(parents=True, exist_ok=True)

    def load_feed_from_file_or_data(
        self,
        data_source: Any,
        source_name: str,
        is_phishing: bool,
        url_column: str = "url"
    ) -> List[Dict[str, Any]]:
        """
        Parses a list of URLs, a raw text file (one URL per line), or a CSV file.
        Returns a list of standardized record dicts with provenance.
        """
        records = []
        now_iso = datetime.datetime.now(datetime.timezone.utc).isoformat()
        label = 1 if is_phishing else 0
        
        if isinstance(data_source, list):
            for item in data_source:
                norm = normalize_url(str(item))
                if norm:
                    records.append({
                        "url": norm,
                        "label": label,
                        "source": source_name,
                        "collected_at": now_iso
                    })
        elif isinstance(data_source, (str, Path)):
            p = Path(data_source)
            if not p.exists():
                logger.warning("Source file not found: %s", p)
                return []
                
            if p.suffix.lower() == ".csv":
                df = pd.read_csv(p)
                col = url_column if url_column in df.columns else df.columns[0]
                for raw in df[col].dropna():
                    norm = normalize_url(str(raw))
                    if norm:
                        records.append({
                            "url": norm,
                            "label": label,
                            "source": source_name,
                            "collected_at": now_iso
                        })
            else:
                with open(p, "r", encoding="utf-8", errors="ignore") as f:
                    for line in f:
                        line = line.strip()
                        if line and not line.startswith("#"):
                            norm = normalize_url(line)
                            if norm:
                                records.append({
                                    "url": norm,
                                    "label": label,
                                    "source": source_name,
                                    "collected_at": now_iso
                                })
                                
        logger.info("Ingested %d valid records from source '%s'", len(records), source_name)
        return records

    def build_curated_dataset(
        self,
        phishing_sources: List[Tuple[Any, str]],
        benign_sources: List[Tuple[Any, str]],
        include_hard_negatives: bool = True
    ) -> Tuple[pd.DataFrame, Dict[str, Any]]:
        """
        Combines multiple phishing and benign sources, deduplicates, incorporates hard negatives,
        and generates class balance and provenance metrics.
        """
        all_records: List[Dict[str, Any]] = []

        # Ingest phishing sources
        for source_data, source_name in phishing_sources:
            all_records.extend(self.load_feed_from_file_or_data(source_data, source_name, is_phishing=True))

        # Ingest benign sources
        for source_data, source_name in benign_sources:
            all_records.extend(self.load_feed_from_file_or_data(source_data, source_name, is_phishing=False))

        # Incorporate hard negatives if requested
        if include_hard_negatives:
            hard_neg_records = self.load_feed_from_file_or_data(
                CURATED_HARD_NEGATIVES,
                source_name="curated_hard_negatives_banking_gov",
                is_phishing=False
            )
            all_records.extend(hard_neg_records)

        if not all_records:
            return pd.DataFrame(columns=["url", "label", "source", "collected_at"]), {}

        df = pd.DataFrame(all_records)
        raw_count = len(df)

        # Deduplicate strictly on normalized URL
        # In case of conflicting labels for the exact same URL, keep the phishing label (safer)
        df = df.sort_values(by="label", ascending=False).drop_duplicates(subset=["url"], keep="first").reset_index(drop=True)
        dedup_count = len(df)

        legitimate_count = int((df["label"] == 0).sum())
        phishing_count = int((df["label"] == 1).sum())

        provenance = {
            "created_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
            "raw_samples": raw_count,
            "deduplicated_samples": dedup_count,
            "duplicates_removed": raw_count - dedup_count,
            "legitimate_count": legitimate_count,
            "phishing_count": phishing_count,
            "class_ratio": round(phishing_count / max(1, legitimate_count), 3),
            "sources": df["source"].value_counts().to_dict()
        }

        logger.info(
            "Dataset compiled: Total=%d (Legitimate=%d, Phishing=%d, Ratio=%.2f)",
            dedup_count, legitimate_count, phishing_count, provenance["class_ratio"]
        )
        return df, provenance

    def export_dataset(self, df: pd.DataFrame, prefix: str = "dataset") -> Path:
        """
        Saves the processed dataset to CSV with an ISO-timestamped filename.
        """
        timestamp = datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%d_%H%M%S")
        out_file = self.output_dir / f"{prefix}_{timestamp}.csv"
        df.to_csv(out_file, index=False)
        logger.info("Saved curated dataset to %s", out_file)
        return out_file
