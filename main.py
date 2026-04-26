from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import re
import time

app = FastAPI()

# ==============================
# MODELS (input validation)
# ==============================

class ToxicityRequest(BaseModel):
    text: str


class SummarizeRequest(BaseModel):
    messages: List[str]


# ==============================
# CONFIG
# ==============================

TOXIC_WORDS = {"bad", "hate", "stupid"}
MAX_SUMMARY_MESSAGES = 10


# ==============================
# UTILITIES
# ==============================

def extract_words(text: str):
    return re.findall(r"\b\w+\b", text.lower())


def is_toxic_text(text: str) -> bool:
    words = extract_words(text)
    return any(word in TOXIC_WORDS for word in words)


def clean_messages(messages: List[str]) -> List[str]:
    """Remove duplicates while preserving order"""
    seen = set()
    result = []

    for msg in messages:
        if not isinstance(msg, str):
            continue

        msg = msg.strip()
        if not msg:
            continue

        if msg not in seen:
            seen.add(msg)
            result.append(msg)

    return result


# ==============================
# TOXICITY ENDPOINT
# ==============================

@app.post("/toxicity")
async def toxicity(req: ToxicityRequest):
    start = time.time()

    try:
        text = req.text.strip()

        if not text:
            return {"toxic": False}

        toxic = is_toxic_text(text)

        print(f"[AI][TOXIC] text='{text[:30]}' result={toxic} time={round((time.time()-start)*1000)}ms")

        return {"toxic": toxic}

    except Exception as e:
        print(f"[AI][ERROR][TOXIC] {str(e)}")
        return {"toxic": False}


# ==============================
# SUMMARIZATION ENDPOINT
# ==============================

@app.post("/summarize")
async def summarize(req: SummarizeRequest):
    start = time.time()

    try:
        messages = clean_messages(req.messages)

        if not messages:
            return {"summary": "No messages to summarize."}

        # limit messages for safety
        messages = messages[-MAX_SUMMARY_MESSAGES:]

        # simple summarization strategy
        # (can be replaced with ML later)
        summary = " | ".join(messages[:3])

        print(f"[AI][SUMMARY] count={len(messages)} time={round((time.time()-start)*1000)}ms")

        return {"summary": summary}

    except Exception as e:
        print(f"[AI][ERROR][SUMMARY] {str(e)}")
        return {"summary": "⚠️ Summary failed."}


# ==============================
# HEALTH CHECK (useful for tests)
# ==============================

@app.get("/health")
async def health():
    return {"status": "ok"}