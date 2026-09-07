from typing import Any
from feature_engineering.url_features import extract_features, get_feature_names

class FeatureService:
    @staticmethod
    def get_features(url: str) -> dict[str, Any]:
        """Extracts complete feature dictionary and feature metadata for inspection."""
        features = extract_features(url)
        return {
            "url": url,
            "feature_count": len(features),
            "features": features
        }
