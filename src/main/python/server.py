import gc
from typing import List, Optional

import torch
import torch.nn.functional as F
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from transformers import AutoModel, AutoTokenizer

app = FastAPI()

e5_model: Optional[SentenceTransformer] = None
bert_model = None
bert_tokenizer = None
loaded_mode: Optional[str] = None
loaded_model_name: Optional[str] = None

E5_MODEL_NAME = "intfloat/multilingual-e5-large"
BERTSCORE_MODEL_NAME = "xlm-roberta-large"


class LoadRequest(BaseModel):
    mode: str = "embedding"
    model: Optional[str] = None


class EmbedRequest(BaseModel):
    texts: List[str]


class ScorePair(BaseModel):
    candidate: str
    reference: str


class ScoreRequest(BaseModel):
    model: str = BERTSCORE_MODEL_NAME
    pairs: List[ScorePair]


@app.post("/load")
def load_model(req: Optional[LoadRequest] = None):
    global e5_model, bert_model, bert_tokenizer, loaded_mode, loaded_model_name

    req = req or LoadRequest()
    mode = req.mode
    model_name = req.model or (BERTSCORE_MODEL_NAME if mode == "bertscore" else E5_MODEL_NAME)

    if loaded_mode is not None:
        if loaded_mode == mode and loaded_model_name == model_name:
            return {"status": "already_loaded", "mode": loaded_mode, "model": loaded_model_name}
        raise HTTPException(
            status_code=409,
            detail=f"Model {loaded_model_name} is already loaded in mode {loaded_mode}. Call /unload first.",
        )

    if mode == "bertscore":
        bert_tokenizer = AutoTokenizer.from_pretrained(model_name)
        bert_model = AutoModel.from_pretrained(model_name).to("cuda")
        bert_model.eval()
    elif mode == "embedding":
        e5_model = SentenceTransformer(model_name, device="cuda")
    else:
        raise HTTPException(status_code=400, detail=f"Unknown mode: {mode}")

    loaded_mode = mode
    loaded_model_name = model_name

    return {
        "status": "loaded",
        "mode": loaded_mode,
        "model": loaded_model_name,
        "device": "cuda",
    }


@app.post("/embed")
def embed(req: EmbedRequest):
    global e5_model

    if loaded_mode != "embedding" or e5_model is None:
        raise HTTPException(status_code=409, detail="Embedding model not loaded. Call /load first.")

    embeddings = e5_model.encode(
        req.texts,
        normalize_embeddings=True,
        batch_size=32,
        show_progress_bar=False,
    )

    return {
        "dim": embeddings.shape[1],
        "count": len(req.texts),
        "embeddings": embeddings.tolist(),
    }


@app.post("/score")
def score(req: ScoreRequest):
    if loaded_mode != "bertscore" or bert_model is None or bert_tokenizer is None:
        raise HTTPException(status_code=409, detail="BERTScore model not loaded. Call /load first.")

    scores = [bertscore(pair.candidate, pair.reference) for pair in req.pairs]

    return {
        "model": loaded_model_name,
        "count": len(scores),
        "scores": scores,
    }


def bertscore(candidate: str, reference: str):
    candidate_embeddings = token_embeddings(candidate)
    reference_embeddings = token_embeddings(reference)

    if candidate_embeddings.numel() == 0 or reference_embeddings.numel() == 0:
        return {"precision": 0.0, "recall": 0.0, "f1": 0.0}

    similarities = candidate_embeddings @ reference_embeddings.T
    precision = similarities.max(dim=1).values.mean()
    recall = similarities.max(dim=0).values.mean()
    if precision + recall == 0:
        f1 = torch.tensor(0.0, device=precision.device)
    else:
        f1 = 2 * precision * recall / (precision + recall)

    return {
        "precision": float(precision.cpu()),
        "recall": float(recall.cpu()),
        "f1": float(f1.cpu()),
    }


def token_embeddings(text: str):
    encoded = bert_tokenizer(
        text,
        return_tensors="pt",
        truncation=True,
        max_length=512,
        return_special_tokens_mask=True,
    )
    encoded = {key: value.to("cuda") for key, value in encoded.items()}
    special_tokens_mask = encoded.pop("special_tokens_mask")

    with torch.no_grad():
        output = bert_model(**encoded)

    mask = (encoded["attention_mask"] == 1) & (special_tokens_mask == 0)
    embeddings = output.last_hidden_state[0][mask[0]]
    return F.normalize(embeddings, p=2, dim=1)


@app.post("/unload")
def unload_model(req: Optional[LoadRequest] = None):
    global e5_model, bert_model, bert_tokenizer, loaded_mode, loaded_model_name

    if loaded_mode is None:
        return {"status": "already_unloaded"}

    e5_model = None
    bert_model = None
    bert_tokenizer = None
    unloaded_mode = loaded_mode
    unloaded_model = loaded_model_name
    loaded_mode = None
    loaded_model_name = None

    gc.collect()

    if torch.cuda.is_available():
        torch.cuda.empty_cache()
        torch.cuda.ipc_collect()

    return {"status": "unloaded", "mode": unloaded_mode, "model": unloaded_model}


@app.get("/status")
def status():
    gpu = torch.cuda.is_available()

    result = {
        "loaded": loaded_mode is not None,
        "mode": loaded_mode,
        "model": loaded_model_name,
        "cuda_available": gpu,
    }

    if gpu:
        result["gpu_name"] = torch.cuda.get_device_name(0)
        result["allocated_mb"] = round(torch.cuda.memory_allocated(0) / 1024 / 1024, 1)
        result["reserved_mb"] = round(torch.cuda.memory_reserved(0) / 1024 / 1024, 1)

    return result
